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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.machineLearning.ClassificationObserver;
import com.joliciel.talismane.rawText.Sentence;
import com.joliciel.talismane.sentenceAnnotators.TokenPlaceholder;
import com.joliciel.talismane.tokeniser.patterns.PatternTokeniser;
import com.typesafe.config.Config;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

/**
 * A Tokeniser splits a sentence up into tokens (parsing units).<br/>
 * <br/>
 * The Tokeniser must recognise the following annotations in the sentence
 * provided:<br/>
 * <ul>
 * <li>{@link TokenPlaceholder}: will get replaced by a token.</li>
 * <li>{@link TokenAttribute}: will add attributes to all tokens contained
 * within its span.</li>
 * </ul>
 * 
 * @author Assaf Urieli
 *
 */
public abstract class Tokeniser {
	public static enum TokeniserType {
		simple,
		pattern
	};

	private static final Logger LOG = LoggerFactory.getLogger(Tokeniser.class);

	private static final Map<String, Tokeniser> tokeniserMap = new HashMap<>();
	private static final Map<String, Pattern> tokenSeparatorMap = new HashMap<>();

	private final TalismaneSession session;

	public Tokeniser(TalismaneSession session) {
		this.session = session;
	}

	protected Tokeniser(Tokeniser tokeniser) {
		this.session = tokeniser.session;
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
		Sentence sentence = new Sentence(text, session);
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
		Sentence sentence = new Sentence(text, session);
		return this.tokeniseWithDecisions(sentence);
	}

	/**
	 * Similar to {@link #tokeniseWithDecisions(String)}, but the text to be
	 * tokenised is contained within a Sentence object.
	 */

	public List<TokenisedAtomicTokenSequence> tokeniseWithDecisions(Sentence sentence) {
		// Initially, separate the sentence into tokens using the separators
		// provided
		TokenSequence tokenSequence = new TokenSequence(sentence, this.session);
		tokenSequence.findDefaultTokens();

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
		}

		return sequences;
	}

	protected abstract List<TokenisedAtomicTokenSequence> tokeniseInternal(TokenSequence initialSequence, Sentence sentence);

	public void addObserver(ClassificationObserver observer) {
		// nothing to do here
	}

	protected TalismaneSession getTalismaneSession() {
		return session;
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

			if (tokeniserType == TokeniserType.simple) {
				tokeniser = new SimpleTokeniser(session);
			} else if (tokeniserType == TokeniserType.pattern) {
				tokeniser = new PatternTokeniser(session);
			} else {
				throw new TalismaneException("Unknown tokeniserType: " + tokeniserType);
			}

			if (session.getSessionId() != null)
				tokeniserMap.put(session.getSessionId(), tokeniser);
		}

		return tokeniser.cloneTokeniser();
	}

	/**
	 * A pattern matching default separators for tokens.
	 */
	public static Pattern getTokenSeparators(TalismaneSession session) {
		Pattern tokenSeparators = tokenSeparatorMap.get(session.getSessionId());
		if (tokenSeparators == null) {
			Config config = session.getConfig();
			String separatorRegex = config.getString("talismane.core.tokeniser.separators");
			tokenSeparators = Pattern.compile(separatorRegex, Pattern.UNICODE_CHARACTER_CLASS);
			tokenSeparatorMap.put(session.getSessionId(), tokenSeparators);
		}
		return tokenSeparators;
	}

	/**
	 * For a given text, returns a list of strings which are guaranteed not to
	 * overlap any token boundaries, except on the unlikely occurrence when a
	 * token placeholder cut a token in the middle of two non-separators.
	 */
	public static List<String> bruteForceTokenise(CharSequence text, TalismaneSession session) {
		List<String> tokens = new ArrayList<>();
		Pattern separatorPattern = Tokeniser.getTokenSeparators(session);
		Matcher matcher = separatorPattern.matcher(text);
		TIntSet separatorMatches = new TIntHashSet();
		while (matcher.find())
			separatorMatches.add(matcher.start());

		int currentPos = 0;
		for (int i = 0; i < text.length(); i++) {
			if (separatorMatches.contains(i)) {
				if (i > currentPos)
					tokens.add(text.subSequence(currentPos, i).toString());
				tokens.add(text.subSequence(i, i + 1).toString());
				currentPos = i + 1;
			}
		}

		if (currentPos < text.length())
			tokens.add(text.subSequence(currentPos, text.length()).toString());

		return tokens;
	}
}
