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
package com.joliciel.talismane.posTagger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.lexicon.LexicalEntry;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.Tokeniser;

/**
 * An interface for evaluating a given pos tagger.
 * 
 * @author Assaf Urieli
 *
 */
public class PosTaggerEvaluator {
	private static final Logger LOG = LoggerFactory.getLogger(PosTaggerEvaluator.class);

	private final PosTagger posTagger;
	private Tokeniser tokeniser;
	private boolean propagateBeam = false;
	private int sentenceCount = 0;

	private List<PosTagEvaluationObserver> observers = new ArrayList<PosTagEvaluationObserver>();

	/**
	 */
	public PosTaggerEvaluator(PosTagger posTagger) {
		this.posTagger = posTagger;
	}

	/**
	 * Evaluate a given pos tagger.
	 * 
	 * @param corpusReader
	 *            for reading manually tagged tokens from a corpus
	 */
	public void evaluate(PosTagAnnotatedCorpusReader corpusReader) {
		int sentenceIndex = 0;
		while (corpusReader.hasNextPosTagSequence()) {
			PosTagSequence realPosTagSequence = corpusReader.nextPosTagSequence();

			List<TokenSequence> tokenSequences = null;
			List<PosTagSequence> guessedSequences = null;

			TokenSequence tokenSequence = realPosTagSequence.getTokenSequence();
			PosTagSequence guessedSequence = null;

			if (this.tokeniser != null) {
				tokenSequences = tokeniser.tokenise(tokenSequence.getSentence());

				tokenSequence = tokenSequences.get(0);
				if (!propagateBeam) {
					tokenSequences = new ArrayList<TokenSequence>();
					tokenSequences.add(tokenSequence);
				}
			} else {
				tokenSequences = new ArrayList<TokenSequence>();
				tokenSequences.add(tokenSequence);
			}

			if (posTagger instanceof NonDeterministicPosTagger) {
				NonDeterministicPosTagger nonDeterministicPosTagger = (NonDeterministicPosTagger) posTagger;
				guessedSequences = nonDeterministicPosTagger.tagSentence(tokenSequences);
				guessedSequence = guessedSequences.get(0);
			} else {
				guessedSequence = posTagger.tagSentence(tokenSequence);
			}

			if (LOG.isDebugEnabled()) {
				StringBuilder stringBuilder = new StringBuilder();
				for (PosTaggedToken posTaggedToken : guessedSequence) {
					Set<String> lemmas = new TreeSet<String>();
					stringBuilder.append(posTaggedToken.getToken().getOriginalText());
					stringBuilder.append("[" + posTaggedToken.getTag());

					List<LexicalEntry> entries = posTaggedToken.getLexicalEntries();
					boolean dropCurrentWord = false;
					if (entries.size() > 1)
						dropCurrentWord = true;
					for (LexicalEntry entry : posTaggedToken.getLexicalEntries()) {
						if (!lemmas.contains(entry.getLemma())) {
							if (dropCurrentWord && posTaggedToken.getToken().getText().equals(entry.getLemma())) {
								dropCurrentWord = false;
								continue;
							}
							stringBuilder.append("|" + entry.getLemma());
							// stringBuilder.append("/" + entry.getCategory());
							stringBuilder.append("/" + entry.getMorphology());
							lemmas.add(entry.getLemma());
						}
					}
					stringBuilder.append("] ");
				}
				LOG.debug(stringBuilder.toString());
			}

			for (PosTagEvaluationObserver observer : this.observers) {
				observer.onNextPosTagSequence(realPosTagSequence, guessedSequences);
			}

			sentenceIndex++;
			if (sentenceCount > 0 && sentenceIndex == sentenceCount)
				break;
		} // next sentence

		for (PosTagEvaluationObserver observer : this.observers) {
			observer.onEvaluationComplete();
		}
	}

	public PosTagger getPosTagger() {
		return posTagger;
	}

	/**
	 * A tokeniser to tokenise the sentences brought back by the corpus reader,
	 * rather than automatically using their existing tokenisation.
	 */
	public Tokeniser getTokeniser() {
		return tokeniser;
	}

	public void setTokeniser(Tokeniser tokeniser) {
		this.tokeniser = tokeniser;
	}

	/**
	 * Should the pos tagger take the tokeniser's full beam as it's input, or
	 * only the best guess.
	 */
	public boolean isPropagateBeam() {
		return propagateBeam;
	}

	public void setPropagateBeam(boolean propagateBeam) {
		this.propagateBeam = propagateBeam;
	}

	public List<PosTagEvaluationObserver> getObservers() {
		return observers;
	}

	public void setObservers(List<PosTagEvaluationObserver> observers) {
		this.observers = observers;
	}

	public void addObserver(PosTagEvaluationObserver observer) {
		this.observers.add(observer);
	}

	/**
	 * If set, will limit the maximum number of sentences that will be
	 * evaluated. Default is 0 = all sentences.
	 */
	public int getSentenceCount() {
		return sentenceCount;
	}

	public void setSentenceCount(int sentenceCount) {
		this.sentenceCount = sentenceCount;
	}

}
