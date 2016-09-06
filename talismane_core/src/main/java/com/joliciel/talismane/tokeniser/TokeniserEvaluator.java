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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An interface for evaluating a given tokeniser.
 * 
 * @author Assaf Urieli
 *
 */
public class TokeniserEvaluator {
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(TokeniserEvaluator.class);
	private final Tokeniser tokeniser;
	private int sentenceCount = 0;

	private final List<TokenEvaluationObserver> observers = new ArrayList<>();

	public TokeniserEvaluator(Tokeniser tokeniser) {
		this.tokeniser = tokeniser;
	}

	/**
	 * Evaluate a given tokeniser.
	 * 
	 * @param corpusReader
	 *            for reading manually separated tokens from a corpus
	 */
	public void evaluate(TokeniserAnnotatedCorpusReader corpusReader) {
		int sentenceIndex = 0;
		while (corpusReader.hasNextTokenSequence()) {
			TokenSequence realSequence = corpusReader.nextTokenSequence();
			String sentence = realSequence.getText();

			List<TokenisedAtomicTokenSequence> guessedAtomicSequences = this.getGuessedAtomicSequences(sentence);

			for (TokenEvaluationObserver observer : observers) {
				observer.onNextTokenSequence(realSequence, guessedAtomicSequences);
			}
			sentenceIndex++;
			if (sentenceCount > 0 && sentenceIndex == sentenceCount)
				break;
		} // next sentence

		for (TokenEvaluationObserver observer : observers) {
			observer.onEvaluationComplete();
		}
	}

	private List<TokenisedAtomicTokenSequence> getGuessedAtomicSequences(String sentence) {
		List<TokenisedAtomicTokenSequence> guessedAtomicSequences = tokeniser.tokeniseWithDecisions(sentence);

		// TODO: we'd like to evaluate the effect of propagation on tokenising.
		// To do this, we either need to copy the entire processing chain from
		// TalismaneImpl here
		// or refactor so we can call it from within here.
		// A better method might be to implement the evaluator as a
		// PosTagSequenceProcessor, ParseConfigurationProcessor, etc.
		// and use:
		// ParseConfiguration.getPosTagSequence().getTokenSequence().getUnderlyingAtomicTokenSequence();
		// but in this case, we'd need to somehow retain the "realSequence" for
		// comparison

		return guessedAtomicSequences;
	}

	public Tokeniser getTokeniser() {
		return tokeniser;
	}

	public int getSentenceCount() {
		return sentenceCount;
	}

	public void setSentenceCount(int sentenceCount) {
		this.sentenceCount = sentenceCount;
	}

	public void addObserver(TokenEvaluationObserver observer) {
		this.observers.add(observer);
	}
}
