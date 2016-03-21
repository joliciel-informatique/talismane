package com.joliciel.talismane.posTagger;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class PosTagComparatorImpl implements PosTagComparator {
	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(PosTaggerEvaluatorImpl.class);
	private int sentenceCount = 0;
	
	private List<PosTagEvaluationObserver> observers = new ArrayList<PosTagEvaluationObserver>();

	@Override
	public void evaluate(PosTagAnnotatedCorpusReader referenceCorpusReader,
			PosTagAnnotatedCorpusReader evaluationCorpusReader) {
		int sentenceIndex = 0;
		while (referenceCorpusReader.hasNextPosTagSequence()) {
			PosTagSequence realPosTagSequence = referenceCorpusReader.nextPosTagSequence();
			PosTagSequence guessedPosTagSequence = evaluationCorpusReader.nextPosTagSequence();
			
			List<PosTagSequence> guessedSequences = new ArrayList<PosTagSequence>();
			guessedSequences.add(guessedPosTagSequence);
			for (PosTagEvaluationObserver observer : this.observers) {
				observer.onNextPosTagSequence(realPosTagSequence, guessedSequences);
			}
			sentenceIndex++;
			if (sentenceCount>0 && sentenceIndex==sentenceCount)
				break;
		}
		
		for (PosTagEvaluationObserver observer : this.observers) {
			observer.onEvaluationComplete();
		}
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
