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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.utils.ConfigUtils;
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

	public TokeniserEvaluator(TalismaneSession session) throws IOException, ClassNotFoundException, ReflectiveOperationException {
		Config config = session.getConfig();
		this.tokeniser = Tokeniser.getInstance(session);
		this.observers = TokenEvaluationObserver.getTokenEvaluationObservers(session);

		Config tokeniserConfig = config.getConfig("talismane.core.tokeniser");
		InputStream evalFile = ConfigUtils.getFileFromConfig(config, "talismane.core.tokeniser.evaluate.eval-file");
		Reader evalReader = new BufferedReader(new InputStreamReader(evalFile, session.getInputCharset()));
		this.corpusReader = TokeniserAnnotatedCorpusReader.getCorpusReader(evalReader, tokeniserConfig.getConfig("input"), session);
	}

	public TokeniserEvaluator(Tokeniser tokeniser, TokeniserAnnotatedCorpusReader corpusReader) {
		this.tokeniser = tokeniser;
		this.observers = new ArrayList<>();
		this.corpusReader = corpusReader;
	}

	/**
	 * Evaluate a given tokeniser.
	 * 
	 * @param corpusReader
	 *            for reading manually separated tokens from a corpus
	 */
	public void evaluate() {
		while (corpusReader.hasNextTokenSequence()) {
			TokenSequence realSequence = corpusReader.nextTokenSequence();
			String sentence = realSequence.getSentence().getText();

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
