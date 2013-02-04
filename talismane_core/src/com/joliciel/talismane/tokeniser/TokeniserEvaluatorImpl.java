///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
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

import java.util.List;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class TokeniserEvaluatorImpl implements TokeniserEvaluator {
	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(TokeniserEvaluatorImpl.class);
	Tokeniser tokeniser;

	private List<TokenEvaluationObserver> observers = new ArrayList<TokenEvaluationObserver>();
	
	@Override
	public void evaluate(
			TokeniserAnnotatedCorpusReader corpusReader) {		
		while (corpusReader.hasNextTokenSequence()) {
			TokenSequence realSequence = corpusReader.nextTokenSequence();
			String sentence = realSequence.getText();
				
			List<TokenisedAtomicTokenSequence> guessedAtomicSequences = this.getGuessedAtomicSequences(sentence);
			
			for (TokenEvaluationObserver observer : observers) {
				observer.onNextTokenSequence(realSequence, guessedAtomicSequences);
			}
		} // next sentence
		
		for (TokenEvaluationObserver observer : observers) {
			observer.onEvaluationComplete();
		}
	}
	
	private List<TokenisedAtomicTokenSequence> getGuessedAtomicSequences(String sentence) {
		List<TokenisedAtomicTokenSequence> guessedAtomicSequences = tokeniser.tokeniseWithDecisions(sentence);

		// TODO: we'd like to evaluate the effect of propagation on tokenising.
		// To do this, we either need to copy the entire processing chain from TalismaneImpl here
		// or refactor so we can call it from within here.
		// A better method might be to implement the evaluator as a PosTagSequenceProcessor, ParseConfigurationProcessor, etc.
		// and use: ParseConfiguration.getPosTagSequence().getTokenSequence().getUnderlyingAtomicTokenSequence();
		// but in this case, we'd need to somehow retain the "realSequence" for comparison

		return guessedAtomicSequences;
	}

	public Tokeniser getTokeniser() {
		return tokeniser;
	}

	public void setTokeniser(Tokeniser tokeniser) {
		this.tokeniser = tokeniser;
	}


	@Override
	public void addObserver(TokenEvaluationObserver observer) {
		this.observers.add(observer);
	}
}
