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
package com.joliciel.talismane.parser;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.filters.Sentence;
import com.joliciel.talismane.posTagger.NonDeterministicPosTagger;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTagger;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.Tokeniser;

class ParserEvaluatorImpl implements ParserEvaluator {
	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(ParserEvaluatorImpl.class);
	private Parser parser;
	private PosTagger posTagger;
	private Tokeniser tokeniser;
	private boolean propagateBeam = true;
	private int sentenceCount = 0;
	
	private ParserServiceInternal parserServiceInternal;
	private List<ParseEvaluationObserver> observers = new ArrayList<ParseEvaluationObserver>();
	
	@Override
	public void evaluate(
			ParserAnnotatedCorpusReader corpusReader) {
		int sentenceIndex = 0;
		while (corpusReader.hasNextConfiguration()) {
			ParseConfiguration realConfiguration = corpusReader.nextConfiguration();
			
			List<PosTagSequence> posTagSequences = null;
			List<TokenSequence> tokenSequences = null;
			if (tokeniser!=null) {
				if (posTagger==null)
					throw new TalismaneException("Cannot evaluate with tokeniser but no pos-tagger");
				
				Sentence sentence = realConfiguration.getPosTagSequence().getTokenSequence().getSentence();
				
				tokenSequences = tokeniser.tokenise(sentence);
				
				if (!propagateBeam) {
					TokenSequence tokenSequence = tokenSequences.get(0);
					tokenSequences = new ArrayList<TokenSequence>();
					tokenSequences.add(tokenSequence);
				}
			} else {
				tokenSequences = new ArrayList<TokenSequence>();
				PosTagSequence posTagSequence = realConfiguration.getPosTagSequence().clonePosTagSequence();
				posTagSequence.removeRoot();
				tokenSequences.add(posTagSequence.getTokenSequence());
			}
			
			if (posTagger!=null) {
				if (posTagger instanceof NonDeterministicPosTagger) {
					NonDeterministicPosTagger nonDeterministicPosTagger = (NonDeterministicPosTagger) posTagger;
					posTagSequences = nonDeterministicPosTagger.tagSentence(tokenSequences);
					if (!propagateBeam) {
						PosTagSequence posTagSequence = posTagSequences.get(0);
						posTagSequences = new ArrayList<PosTagSequence>();
						posTagSequences.add(posTagSequence);
					}
				} else {
					posTagSequences = new ArrayList<PosTagSequence>();
					PosTagSequence posTagSequence = posTagger.tagSentence(tokenSequences.get(0));
					posTagSequences.add(posTagSequence);
				}
			} else {
				PosTagSequence posTagSequence = realConfiguration.getPosTagSequence();
				posTagSequences = new ArrayList<PosTagSequence>();
				posTagSequences.add(posTagSequence);				
			}
			
			List<ParseConfiguration> guessedConfigurations = null;
			if (parser instanceof NonDeterministicParser) {
				NonDeterministicParser nonDeterministicParser = (NonDeterministicParser) parser;
				guessedConfigurations = nonDeterministicParser.parseSentence(posTagSequences);
			} else {
				ParseConfiguration bestGuess = parser.parseSentence(posTagSequences.get(0));
				guessedConfigurations = new ArrayList<ParseConfiguration>();
				guessedConfigurations.add(bestGuess);
			}
			
			for (ParseEvaluationObserver observer : this.observers) {
				observer.onNextParseConfiguration(realConfiguration, guessedConfigurations);
			}
			sentenceIndex++;
			if (sentenceCount>0 && sentenceIndex==sentenceCount)
				break;
		} // next sentence
		
		for (ParseEvaluationObserver observer : this.observers) {
			observer.onEvaluationComplete();
		}

	}

	public Parser getParser() {
		return parser;
	}

	public void setParser(Parser parser) {
		this.parser = parser;
	}

	@Override
	public PosTagger getPosTagger() {
		return posTagger;
	}

	@Override
	public void setPosTagger(PosTagger posTagger) {
		this.posTagger = posTagger;
	}

	@Override
	public Tokeniser getTokeniser() {
		return tokeniser;
	}

	@Override
	public void setTokeniser(Tokeniser tokeniser) {
		this.tokeniser = tokeniser;
	}

	public ParserServiceInternal getParserServiceInternal() {
		return parserServiceInternal;
	}

	public void setParserServiceInternal(ParserServiceInternal parserServiceInternal) {
		this.parserServiceInternal = parserServiceInternal;
	}

	public List<ParseEvaluationObserver> getObservers() {
		return observers;
	}

	public void setObservers(List<ParseEvaluationObserver> observers) {
		this.observers = observers;
	}

	public void addObserver(ParseEvaluationObserver observer) {
		this.observers.add(observer);
	}

	@Override
	public boolean isPropagateBeam() {
		return propagateBeam;
	}

	@Override
	public void setPropagateBeam(boolean propagateBeam) {
		this.propagateBeam = propagateBeam;
	}

	@Override
	public int getSentenceCount() {
		return sentenceCount;
	}

	@Override
	public void setSentenceCount(int sentenceCount) {
		this.sentenceCount = sentenceCount;
	}
	
	
}
