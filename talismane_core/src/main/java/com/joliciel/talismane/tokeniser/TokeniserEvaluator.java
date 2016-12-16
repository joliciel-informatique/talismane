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
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.rawText.Sentence;
import com.typesafe.config.Config;

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
	private final TokeniserAnnotatedCorpusReader corpusReader;

	private final List<TokenEvaluationObserver> observers;

	public TokeniserEvaluator(Reader evalReader, File outDir, TalismaneSession session)
			throws IOException, ClassNotFoundException, ReflectiveOperationException {
		Config config = session.getConfig();
		this.tokeniser = Tokeniser.getInstance(session);
		this.observers = TokenEvaluationObserver.getTokenEvaluationObservers(outDir, session);

		Config tokeniserConfig = config.getConfig("talismane.core.tokeniser");

		this.corpusReader = TokeniserAnnotatedCorpusReader.getCorpusReader(evalReader, tokeniserConfig.getConfig("input"), session);
	}

	/**
	 * 
	 * @param tokeniser
	 *            the tokeniser to evaluate
	 * @param corpusReader
	 *            for reading manually separated tokens from a corpus
	 */
	public TokeniserEvaluator(Tokeniser tokeniser, TokeniserAnnotatedCorpusReader corpusReader) {
		this.tokeniser = tokeniser;
		this.observers = new ArrayList<>();
		this.corpusReader = corpusReader;
	}

	/**
	 * Evaluate a given tokeniser.
	 */
	public void evaluate() {
		while (corpusReader.hasNextTokenSequence()) {
			TokenSequence realSequence = corpusReader.nextTokenSequence();
			Sentence sentence = realSequence.getSentence();

			List<TokenisedAtomicTokenSequence> guessedAtomicSequences = tokeniser.tokeniseWithDecisions(sentence);

			for (TokenEvaluationObserver observer : observers) {
				observer.onNextTokenSequence(realSequence, guessedAtomicSequences);
			}
		} // next sentence

		for (TokenEvaluationObserver observer : observers) {
			observer.onEvaluationComplete();
		}
	}

	public Tokeniser getTokeniser() {
		return tokeniser;
	}

	public void addObserver(TokenEvaluationObserver observer) {
		this.observers.add(observer);
	}
}
