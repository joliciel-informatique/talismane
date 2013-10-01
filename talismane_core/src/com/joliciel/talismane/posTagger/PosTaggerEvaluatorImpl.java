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
package com.joliciel.talismane.posTagger;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.lexicon.LexicalEntry;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.Tokeniser;
import com.joliciel.talismane.utils.PerformanceMonitor;

class PosTaggerEvaluatorImpl implements PosTaggerEvaluator {
	private static final Log LOG = LogFactory.getLog(PosTaggerEvaluatorImpl.class);
	private static final PerformanceMonitor MONITOR = PerformanceMonitor.getMonitor(PosTaggerEvaluatorImpl.class);

	private PosTagger posTagger;
	private Tokeniser tokeniser;
	private boolean propagateBeam = false;
	private int sentenceCount = 0;
	
	private List<PosTagEvaluationObserver> observers = new ArrayList<PosTagEvaluationObserver>();
	
	/**
	 */
	public PosTaggerEvaluatorImpl(PosTagger posTagger) {
		this.posTagger = posTagger;
	}

	@Override
	public void evaluate(PosTagAnnotatedCorpusReader corpusReader) {
		int sentenceIndex = 0;
		while (corpusReader.hasNextPosTagSequence()) {
			PosTagSequence realPosTagSequence = corpusReader.nextPosTagSequence();
			
			List<TokenSequence> tokenSequences = null;
			List<PosTagSequence> guessedSequences = null;
			
			TokenSequence tokenSequence = realPosTagSequence.getTokenSequence();
			PosTagSequence guessedSequence = null;
			
			if (this.tokeniser!=null) {
				MONITOR.startTask("tokenise");
				try {
					tokenSequences = tokeniser.tokenise(tokenSequence.getText());
				} finally {
					MONITOR.endTask("tokenise");
				}
				
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
				MONITOR.startTask("posTag");
				try {
					guessedSequences = nonDeterministicPosTagger.tagSentence(tokenSequences);
				} finally {
					MONITOR.endTask("posTag");
				}
				guessedSequence = guessedSequences.get(0);
			} else {
				try {
					guessedSequence = posTagger.tagSentence(tokenSequence);
				} finally {
					MONITOR.endTask("posTag");
				}
			}
			
			if (LOG.isDebugEnabled()) {
				StringBuilder stringBuilder = new StringBuilder();
				for (PosTaggedToken posTaggedToken : guessedSequence) {
					Set<String> lemmas = new TreeSet<String>();
					stringBuilder.append(posTaggedToken.getToken().getOriginalText());
					stringBuilder.append("[" + posTaggedToken.getTag());
					
					List<LexicalEntry> entries = posTaggedToken.getLexicalEntries();
					boolean dropCurrentWord = false;
					if (entries.size()>1) 
						dropCurrentWord = true;
					for (LexicalEntry entry : posTaggedToken.getLexicalEntries()) {
						if (!lemmas.contains(entry.getLemma())) {
							if (dropCurrentWord&&posTaggedToken.getToken().getText().equals(entry.getLemma())) {
								dropCurrentWord = false;
								continue;
							}
							stringBuilder.append("|" + entry.getLemma());
//							stringBuilder.append("/" + entry.getCategory());
							stringBuilder.append("/" + entry.getMorphology());
							lemmas.add(entry.getLemma());
						}
					}
					stringBuilder.append("] ");
				}
				LOG.debug(stringBuilder.toString());
			}
			
			MONITOR.startTask("observe");
			try {
				for (PosTagEvaluationObserver observer : this.observers) {
					observer.onNextPosTagSequence(realPosTagSequence, guessedSequences);
				}
			} finally {
				MONITOR.endTask("observe");
			}
			
			sentenceIndex++;
			if (sentenceCount>0 && sentenceIndex==sentenceCount)
				break;
		} // next sentence
		
		MONITOR.startTask("onEvaluationComplete");
		try {
			for (PosTagEvaluationObserver observer : this.observers) {
				observer.onEvaluationComplete();
			}
		} finally {
			MONITOR.endTask("onEvaluationComplete");
		}
	}

	public PosTagger getPosTagger() {
		return posTagger;
	}

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
	
	@Override
	public boolean isPropagateBeam() {
		return propagateBeam;
	}

	@Override
	public void setPropagateBeam(boolean propagateBeam) {
		this.propagateBeam = propagateBeam;
	}

	public List<PosTagEvaluationObserver> getObservers() {
		return observers;
	}

	public void setObservers(List<PosTagEvaluationObserver> observers) {
		this.observers = observers;
	}
	
	@Override
	public void addObserver(PosTagEvaluationObserver observer) {
		this.observers.add(observer);
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
