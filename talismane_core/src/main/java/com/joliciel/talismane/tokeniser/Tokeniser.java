///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2014 Joliciel Informatique
//
//This file is part of Talismane.
//
//Talismane is free software: you can redistribute it and/or modify
//it under the terms of the GNU Affero General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//Talismane is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Affero General Public License for more details.
//
//You should have received a copy of the GNU Affero General Public License
//along with Talismane.  If not, see <http://www.gnu.org/licenses/>.
//////////////////////////////////////////////////////////////////////////////
package com.joliciel.talismane.tokeniser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.Annotator;
import com.joliciel.talismane.NeedsTalismaneSession;
import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.filters.Sentence;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.ClassificationObserver;
import com.joliciel.talismane.machineLearning.MachineLearningModelFactory;
import com.joliciel.talismane.posTagger.filters.PosTagSequenceFilterFactory;
import com.joliciel.talismane.tokeniser.filters.TokenFilter;
import com.joliciel.talismane.tokeniser.filters.TokenFilterFactory;
import com.joliciel.talismane.tokeniser.filters.TokenPlaceholder;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilterFactory;
import com.joliciel.talismane.tokeniser.patterns.PatternTokeniser;
import com.joliciel.talismane.utils.ConfigUtils;
import com.typesafe.config.Config;

/**
 * A Tokeniser splits a sentence up into tokens (parsing units).
 * 
 * @author Assaf Urieli
 *
 */
public abstract class Tokeniser {
	public static enum TokeniserType {
		simple,
		pattern
	};

	/**
	 * A list of possible separators for tokens.
	 */
	public static final Pattern SEPARATORS = Pattern.compile("[\\s\\p{Punct}«»_‒–—―‛“”„‟′″‴‹›‘’‚*\ufeff]", Pattern.UNICODE_CHARACTER_CLASS);

	private static final Logger LOG = LoggerFactory.getLogger(Tokeniser.class);

	private static final Map<String, ClassificationModel> modelMap = new HashMap<>();
	private static final Map<String, Tokeniser> tokeniserMap = new HashMap<>();

	private final List<TokenSequenceFilter> tokenSequenceFilters;
	private final List<Annotator> preAnnotators;
	private final TalismaneSession talismaneSession;

	public Tokeniser(TalismaneSession talismaneSession) {
		this.talismaneSession = talismaneSession;
		this.preAnnotators = new ArrayList<>();
		this.tokenSequenceFilters = new ArrayList<>();
	}

	protected Tokeniser(Tokeniser tokeniser) {
		this.talismaneSession = tokeniser.talismaneSession;
		this.preAnnotators = new ArrayList<>(tokeniser.preAnnotators);
		this.tokenSequenceFilters = new ArrayList<>(tokeniser.tokenSequenceFilters);
	}

	/**
	 * Similar to {@link #tokenise(String)}, but returns only the best token
	 * sequence.
	 */

	public TokenSequence tokeniseText(String text) {
		List<TokenSequence> tokenSequences = this.tokenise(text);
		return tokenSequences.get(0);
	}

	/**
	 * Similar to {@link #tokeniseWithDecisions(String)}, but returns the token
	 * sequences inferred from the decisions, rather than the list of decisions
	 * themselves.
	 */

	public List<TokenSequence> tokenise(String text) {
		Sentence sentence = new Sentence(text, talismaneSession);
		return this.tokenise(sentence);
	}

	/**
	 * Similar to {@link #tokenise(Sentence)}, but returns only the best token
	 * sequence.
	 */

	public TokenSequence tokeniseSentence(Sentence sentence) {
		List<TokenSequence> tokenSequences = this.tokenise(sentence);
		return tokenSequences.get(0);
	}

	/**
	 * Similar to {@link #tokeniseWithDecisions(Sentence)}, but returns the
	 * token sequences inferred from the decisions, rather than the list of
	 * decisions themselves.
	 */

	public List<TokenSequence> tokenise(Sentence sentence) {
		List<TokenisedAtomicTokenSequence> decisionSequences = this.tokeniseWithDecisions(sentence);
		List<TokenSequence> tokenSequences = new ArrayList<TokenSequence>();
		for (TokenisedAtomicTokenSequence decisionSequence : decisionSequences) {
			tokenSequences.add(decisionSequence.inferTokenSequence());
		}
		return tokenSequences;
	}

	/**
	 * Tokenise a given sentence. More specifically, return up to N most likely
	 * tokeniser decision sequences, each of which breaks up the sentence into a
	 * different a list of tokens. Note: we assume duplicate white-space has
	 * already been removed from the sentence prior to calling the tokenise
	 * method, e.g. multiple spaces have been replaced by a single space.
	 * 
	 * @param text
	 *            the sentence to be tokenised
	 * @return a List of up to <i>n</i> TokeniserDecisionTagSequence, ordered
	 *         from most probable to least probable
	 */

	public List<TokenisedAtomicTokenSequence> tokeniseWithDecisions(String text) {
		Sentence sentence = new Sentence(text, talismaneSession);
		return this.tokeniseWithDecisions(sentence);
	}

	/**
	 * Similar to {@link #tokeniseWithDecisions(String)}, but the text to be
	 * tokenised is contained within a Sentence object.
	 */

	public List<TokenisedAtomicTokenSequence> tokeniseWithDecisions(Sentence sentence) {
		// annotate the sentence for pre token filters
		for (Annotator annotator : this.preAnnotators) {
			annotator.annotate(sentence);
			if (LOG.isTraceEnabled()) {
				LOG.trace("TokenFilter: " + annotator);
				LOG.trace("annotations: " + sentence.getAnnotations());
			}
		}

		// Initially, separate the sentence into tokens using the separators
		// provided
		TokenSequence tokenSequence = new TokenSequence(sentence, Tokeniser.SEPARATORS, this.talismaneSession);

		// apply any pre-processing filters that have been added
		for (TokenSequenceFilter tokenSequenceFilter : this.tokenSequenceFilters) {
			tokenSequenceFilter.apply(tokenSequence);
		}

		List<TokenisedAtomicTokenSequence> sequences = this.tokeniseInternal(tokenSequence, sentence);

		LOG.debug("####Final token sequences:");
		int j = 1;
		for (TokenisedAtomicTokenSequence sequence : sequences) {
			TokenSequence newTokenSequence = sequence.inferTokenSequence();
			if (LOG.isDebugEnabled()) {
				LOG.debug("Token sequence " + (j++));
				LOG.debug("Atomic sequence: " + sequence);
				LOG.debug("Resulting sequence: " + newTokenSequence);
			}
			// need to re-apply the pre-processing filters, because the
			// tokens are all new
			// Question: why can't we conserve the initial tokens when they
			// haven't changed at all?
			// Answer: because the tokenSequence and index in the sequence
			// is referenced by the token.
			// Question: should we create a separate class, Token and
			// TokenInSequence,
			// one with index & sequence access & one without?
			for (TokenSequenceFilter tokenSequenceFilter : this.tokenSequenceFilters) {
				tokenSequenceFilter.apply(newTokenSequence);
			}
			if (LOG.isDebugEnabled()) {
				LOG.debug("After filters: " + newTokenSequence);
			}
		}

		return sequences;
	}

	protected abstract List<TokenisedAtomicTokenSequence> tokeniseInternal(TokenSequence initialSequence, Sentence sentence);

	/**
	 * Filters to be applied to the atomic token sequences, prior to tokenising.
	 * These filters will either add empty tokens at given places, or change the
	 * token text. Note that these filters will be applied to the token
	 * sequences produced by the tokeniser as well.
	 */
	public List<TokenSequenceFilter> getTokenSequenceFilters() {
		return tokenSequenceFilters;
	}

	/**
	 * See {@link #getTokenSequenceFilters()}.
	 */
	public void addTokenSequenceFilter(TokenSequenceFilter tokenSequenceFilter) {
		this.tokenSequenceFilters.add(tokenSequenceFilter);
	}

	public void addObserver(ClassificationObserver observer) {
		// nothing to do here
	}

	/**
	 * Annotators that will be applied before tokenising.<br/>
	 * The Tokeniser must recognise the following annotations:<br/>
	 * <ul>
	 * <li>{@link TokenPlaceholder}: will get replaced by a token.</li>
	 * <li>{@link TokenAttribute}: will add attributes to all tokens contained
	 * within its span.</li>
	 * </ul>
	 */
	public List<Annotator> getPreAnnotators() {
		return preAnnotators;
	}

	/**
	 * Adds an annotator at the end of the current list returned by
	 * {@link #getPreAnnotators()}.
	 */
	public void addPreAnnotator(Annotator filter) {
		if (LOG.isTraceEnabled())
			LOG.trace("Added filter: " + filter.toString());
		this.preAnnotators.add(filter);
	}

	protected TalismaneSession getTalismaneSession() {
		return talismaneSession;
	}

	public abstract Tokeniser cloneTokeniser();

	/**
	 * Build a tokeniser using the configuration provided.
	 * 
	 * @param session
	 *            current session
	 * @return a tokeniser to be used - each call returns a separate tokeniser
	 * @throws IOException
	 *             if problems occurred reading the model
	 */
	public static Tokeniser getInstance(TalismaneSession session) throws IOException {
		Tokeniser tokeniser = null;
		if (session.getSessionId() != null)
			tokeniser = tokeniserMap.get(session.getSessionId());
		if (tokeniser == null) {
			Config config = session.getConfig();
			Config tokeniserConfig = config.getConfig("talismane.core.tokeniser");
			TokeniserType tokeniserType = TokeniserType.valueOf(tokeniserConfig.getString("type"));

			ClassificationModel tokeniserModel = null;
			if (tokeniserType == TokeniserType.simple) {
				tokeniser = new SimpleTokeniser(session);
			} else if (tokeniserType == TokeniserType.pattern) {
				int beamWidth = tokeniserConfig.getInt("beam-width");
				LOG.debug("Getting tokeniser model");

				String configPath = "talismane.core.tokeniser.model";
				String modelFilePath = config.getString(configPath);
				tokeniserModel = modelMap.get(modelFilePath);
				if (tokeniserModel == null) {
					InputStream tokeniserModelFile = ConfigUtils.getFileFromConfig(config, configPath);
					MachineLearningModelFactory factory = new MachineLearningModelFactory();
					tokeniserModel = factory.getClassificationModel(new ZipInputStream(tokeniserModelFile));
					modelMap.put(modelFilePath, tokeniserModel);
				}

				tokeniser = new PatternTokeniser(tokeniserModel, beamWidth, session);

				boolean includeDetails = tokeniserConfig.getBoolean("output.include-details");
				if (includeDetails) {
					String detailsFilePath = session.getBaseName() + "_tokeniser_details.txt";
					File detailsFile = new File(session.getOutDir(), detailsFilePath);
					detailsFile.delete();
					ClassificationObserver observer = tokeniserModel.getDetailedAnalysisObserver(detailsFile);
					tokeniser.addObserver(observer);
				}
			} else {
				throw new TalismaneException("Unknown tokeniserType: " + tokeniserType);
			}

			List<String> tokenFilterDescriptors = new ArrayList<>();
			TokenFilterFactory tokenFilterFactory = TokenFilterFactory.getInstance(session);

			LOG.debug("tokenFilters");
			String configPath = "talismane.core.tokeniser.pre-annotators";
			List<String> tokenFilterPaths = config.getStringList(configPath);
			for (String path : tokenFilterPaths) {
				LOG.debug("From: " + path);
				InputStream tokenFilterFile = ConfigUtils.getFile(config, configPath, path);
				try (Scanner scanner = new Scanner(tokenFilterFile, "UTF-8")) {
					List<TokenFilter> myFilters = tokenFilterFactory.readTokenFilters(scanner, path, tokenFilterDescriptors);
					for (TokenFilter tokenFilter : myFilters) {
						tokeniser.addPreAnnotator(tokenFilter);
					}
				}
			}

			if (tokeniserModel != null) {
				LOG.debug("From model");
				List<String> modelDescriptors = tokeniserModel.getDescriptors().get(TokenFilterFactory.TOKEN_FILTER_DESCRIPTOR_KEY);
				String modelDescriptorString = "";
				if (modelDescriptors != null) {
					for (String descriptor : modelDescriptors) {
						modelDescriptorString += descriptor + "\n";
					}
				}
				try (Scanner scanner = new Scanner(modelDescriptorString)) {
					List<TokenFilter> myFilters = tokenFilterFactory.readTokenFilters(scanner, tokenFilterDescriptors);
					for (TokenFilter tokenFilter : myFilters) {
						tokeniser.addPreAnnotator(tokenFilter);
					}
				}
			}

			List<String> tokenSequenceFilterDescriptors = new ArrayList<>();
			List<TokenSequenceFilter> tokenSequenceFilters = new ArrayList<>();
			TokenSequenceFilterFactory tokenSequenceFilterFactory = TokenSequenceFilterFactory.getInstance(session);

			configPath = "talismane.core.tokeniser.post-annotators";
			List<String> tokenSequenceFilterPaths = config.getStringList(configPath);
			for (String path : tokenSequenceFilterPaths) {
				LOG.debug("From: " + path);
				InputStream tokenFilterFile = ConfigUtils.getFile(config, configPath, path);
				try (Scanner scanner = new Scanner(tokenFilterFile, "UTF-8")) {
					while (scanner.hasNextLine()) {
						String descriptor = scanner.nextLine();
						LOG.debug(descriptor);
						tokenSequenceFilterDescriptors.add(descriptor);
						if (descriptor.length() > 0 && !descriptor.startsWith("#")) {
							TokenSequenceFilter tokenSequenceFilter = tokenSequenceFilterFactory.getTokenSequenceFilter(descriptor);
							if (tokenSequenceFilter instanceof NeedsTalismaneSession)
								((NeedsTalismaneSession) tokenSequenceFilter).setTalismaneSession(session);
							tokenSequenceFilters.add(tokenSequenceFilter);
						}
					}
				}
			}

			if (tokeniserModel != null) {
				LOG.debug("From model");
				List<String> modelDescriptors = tokeniserModel.getDescriptors().get(PosTagSequenceFilterFactory.POSTAG_PREPROCESSING_FILTER_DESCRIPTOR_KEY);

				if (modelDescriptors != null) {
					for (String descriptor : modelDescriptors) {
						LOG.debug(descriptor);
						tokenSequenceFilterDescriptors.add(descriptor);
						if (descriptor.length() > 0 && !descriptor.startsWith("#")) {
							TokenSequenceFilter tokenSequenceFilter = tokenSequenceFilterFactory.getTokenSequenceFilter(descriptor);
							if (tokenSequenceFilter instanceof NeedsTalismaneSession)
								((NeedsTalismaneSession) tokenSequenceFilter).setTalismaneSession(session);
							tokenSequenceFilters.add(tokenSequenceFilter);
						}
					}
				}
			}

			for (TokenSequenceFilter tokenSequenceFilter : tokenSequenceFilters) {
				tokeniser.addTokenSequenceFilter(tokenSequenceFilter);
			}
		}

		return tokeniser.cloneTokeniser();
	}
}
