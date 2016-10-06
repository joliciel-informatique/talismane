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
package com.joliciel.talismane.parser;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.filters.Sentence;
import com.joliciel.talismane.posTagger.NonDeterministicPosTagger;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTagger;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.Tokeniser;

/**
 * Evaluate a parser.
 * 
 * @author Assaf Urieli
 *
 */
public class ParserEvaluator {
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(ParserEvaluator.class);
	private Parser parser;
	private PosTagger posTagger;
	private Tokeniser tokeniser;
	private boolean propagateBeam = true;
	private int sentenceCount = 0;

	private List<ParseEvaluationObserver> observers = new ArrayList<ParseEvaluationObserver>();

	public void evaluate(ParserAnnotatedCorpusReader corpusReader) {
		int sentenceIndex = 0;
		while (corpusReader.hasNextConfiguration()) {
			ParseConfiguration realConfiguration = corpusReader.nextConfiguration();

			List<PosTagSequence> posTagSequences = null;
			List<TokenSequence> tokenSequences = null;
			if (tokeniser != null) {
				if (posTagger == null)
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

			if (posTagger != null) {
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
					PosTagSequence posTagSequence = null;
					posTagSequence = posTagger.tagSentence(tokenSequences.get(0));
					posTagSequences.add(posTagSequence);
				}
			} else {
				PosTagSequence posTagSequence = realConfiguration.getPosTagSequence();
				posTagSequences = new ArrayList<PosTagSequence>();
				posTagSequences.add(posTagSequence);
			}

			for (ParseEvaluationObserver observer : this.observers) {
				observer.onParseStart(realConfiguration, posTagSequences);
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
				observer.onParseEnd(realConfiguration, guessedConfigurations);
			}

			sentenceIndex++;
			if (sentenceCount > 0 && sentenceIndex == sentenceCount)
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

	/**
	 * If provided, will apply pos-tagging as part of the evaluation.
	 */
	public PosTagger getPosTagger() {
		return posTagger;
	}

	public void setPosTagger(PosTagger posTagger) {
		this.posTagger = posTagger;
	}

	/**
	 * If provided, will apply tokenisation as part of the evaluation. If
	 * provided, a pos-tagger must be provided as well.
	 */
	public Tokeniser getTokeniser() {
		return tokeniser;
	}

	public void setTokeniser(Tokeniser tokeniser) {
		this.tokeniser = tokeniser;
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

	/**
	 * Should the beam be propagated from one module to the next, e.g. from the
	 * pos-tagger to the parser.
	 */
	public boolean isPropagateBeam() {
		return propagateBeam;
	}

	public void setPropagateBeam(boolean propagateBeam) {
		this.propagateBeam = propagateBeam;
	}

	/**
	 * The maximum number of sentences to evaluate. Default is 0, which means
	 * all.
	 */
	public int getSentenceCount() {
		return sentenceCount;
	}

	public void setSentenceCount(int sentenceCount) {
		this.sentenceCount = sentenceCount;
	}

}
