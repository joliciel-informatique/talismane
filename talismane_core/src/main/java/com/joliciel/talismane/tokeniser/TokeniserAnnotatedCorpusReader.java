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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.AnnotatedCorpusReader;
import com.joliciel.talismane.Annotator;
import com.joliciel.talismane.NeedsTalismaneSession;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.tokeniser.filters.TokenFilter;
import com.joliciel.talismane.tokeniser.filters.TokenFilterFactory;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilterFactory;
import com.joliciel.talismane.utils.ConfigUtils;
import com.typesafe.config.Config;

/**
 * An interface for reading tokenized sentences from a corpus.
 * 
 * @author Assaf Urieli
 *
 */
public abstract class TokeniserAnnotatedCorpusReader implements AnnotatedCorpusReader {
	protected final List<TokenSequenceFilter> tokenSequenceFilters = new ArrayList<>();
	protected final List<Annotator> preAnnotators = new ArrayList<>();

	public static final Logger LOG = LoggerFactory.getLogger(TokeniserAnnotatedCorpusReader.class);

	public TokeniserAnnotatedCorpusReader(Reader reader, Config config, TalismaneSession session) {

	}

	/**
	 * Is there another sentence to be read?
	 */
	public abstract boolean hasNextTokenSequence();

	/***
	 * Reads the next token sequence from the corpus.
	 */
	public abstract TokenSequence nextTokenSequence();

	/**
	 * These filters will be applied to each token sequence returned by the
	 * corpus prior to being returned.
	 */
	public void addTokenSequenceFilter(TokenSequenceFilter tokenSequenceFilter) {
		this.tokenSequenceFilters.add(tokenSequenceFilter);
	}

	/**
	 * These annotators will not be used to detect tokens, as token boundaries
	 * are provided by the corpus. They will, on the other hand, be used to
	 * replace token text.
	 */
	public void addPreAnnotator(Annotator annotator) {
		this.preAnnotators.add(annotator);
	}

	/**
	 * @see #addTokenSequenceFilter(TokenSequenceFilter)
	 */
	public List<TokenSequenceFilter> getTokenSequenceFilters() {
		return this.tokenSequenceFilters;
	}

	/**
	 * #see {@link #addPreAnnotator(Annotator)}
	 */
	public List<Annotator> getPreAnnotators() {
		return this.preAnnotators;
	}

	/**
	 * It is assumed the configuration will contain the following keys:
	 * <ul>
	 * <li>corpus-reader: the sub-class to construct</li>
	 * <li>pre-annotators: annotators to apply prior to tokenising</li>
	 * <li>post-annotators: annotators to apply after tokenising</li>
	 * </ul>
	 * 
	 * @param config
	 *            the specific configuration section from which we're building a
	 *            reader
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws ReflectiveOperationException
	 */
	public static TokeniserAnnotatedCorpusReader getCorpusReader(Reader reader, Config config, TalismaneSession session)
			throws IOException, ClassNotFoundException, ReflectiveOperationException {
		final Logger LOG = LoggerFactory.getLogger(TokeniserAnnotatedCorpusReader.class);

		String className = config.getString("corpus-reader");

		@SuppressWarnings("unchecked")
		Class<? extends TokeniserAnnotatedCorpusReader> clazz = (Class<? extends TokeniserAnnotatedCorpusReader>) Class.forName(className);
		Constructor<? extends TokeniserAnnotatedCorpusReader> cons = clazz.getConstructor(Reader.class, Config.class, TalismaneSession.class);

		TokeniserAnnotatedCorpusReader corpusReader = cons.newInstance(reader, config, session);

		corpusReader.setMaxSentenceCount(config.getInt("sentence-count"));
		corpusReader.setStartSentence(config.getInt("start-sentence"));
		int crossValidationSize = config.getInt("cross-validation.fold-count");
		if (crossValidationSize > 0)
			corpusReader.setCrossValidationSize(crossValidationSize);
		int includeIndex = -1;
		if (config.hasPath("cross-validation.include-index"))
			includeIndex = config.getInt("cross-validation.include-index");
		if (includeIndex >= 0)
			corpusReader.setIncludeIndex(includeIndex);
		int excludeIndex = -1;
		if (config.hasPath("cross-validation.exclude-index"))
			excludeIndex = config.getInt("cross-validation.exclude-index");
		if (excludeIndex >= 0)
			corpusReader.setExcludeIndex(excludeIndex);

		List<String> tokenFilterDescriptors = new ArrayList<>();
		TokenFilterFactory tokenFilterFactory = TokenFilterFactory.getInstance(session);

		LOG.debug("pre-annotators");
		String configPath = "pre-annotators";
		List<String> tokenFilterPaths = config.getStringList(configPath);
		for (String path : tokenFilterPaths) {
			LOG.debug("From: " + path);
			InputStream tokenFilterFile = ConfigUtils.getFile(config, configPath, path);
			try (Scanner scanner = new Scanner(tokenFilterFile, "UTF-8")) {
				List<TokenFilter> myFilters = tokenFilterFactory.readTokenFilters(scanner, path, tokenFilterDescriptors);
				for (TokenFilter tokenFilter : myFilters) {
					corpusReader.addPreAnnotator(tokenFilter);
				}
			}
		}

		List<String> tokenSequenceFilterDescriptors = new ArrayList<>();
		List<TokenSequenceFilter> tokenSequenceFilters = new ArrayList<>();
		TokenSequenceFilterFactory tokenSequenceFilterFactory = TokenSequenceFilterFactory.getInstance(session);

		LOG.debug("post-annotators");
		configPath = "post-annotators";
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

		for (TokenSequenceFilter tokenSequenceFilter : tokenSequenceFilters) {
			corpusReader.addTokenSequenceFilter(tokenSequenceFilter);
		}
		return corpusReader;
	}
}
