package com.joliciel.talismane.posTagger;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An interface for comparing two pos-tagged corpora, one of which is considered
 * a reference.
 * 
 * @author Assaf Urieli
 *
 */
public class PosTagComparator {
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(PosTagComparator.class);
	private int sentenceCount = 0;

	private List<PosTagEvaluationObserver> observers = new ArrayList<PosTagEvaluationObserver>();

	/**
	 * Evaluate the evaluation corpus against the reference corpus.
	 * 
	 * @param evaluationCorpusReader
	 *            for reading manually tagged tokens from a corpus
	 */
	public void evaluate(PosTagAnnotatedCorpusReader referenceCorpusReader, PosTagAnnotatedCorpusReader evaluationCorpusReader) {
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
			if (sentenceCount > 0 && sentenceIndex == sentenceCount)
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
