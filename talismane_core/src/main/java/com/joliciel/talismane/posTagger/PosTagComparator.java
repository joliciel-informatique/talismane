package com.joliciel.talismane.posTagger;

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
 * An interface for comparing two pos-tagged corpora, one of which is considered
 * a reference.
 * 
 * @author Assaf Urieli
 *
 */
public class PosTagComparator {
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(PosTagComparator.class);
	private final PosTagAnnotatedCorpusReader referenceCorpusReader;
	private final PosTagAnnotatedCorpusReader evaluationCorpusReader;

	private final List<PosTagEvaluationObserver> observers;

	public PosTagComparator(TalismaneSession session) throws IOException, ClassNotFoundException, ReflectiveOperationException {
		Config config = session.getConfig();
		Config posTaggerConfig = config.getConfig("talismane.core.pos-tagger");

		this.referenceCorpusReader = PosTagAnnotatedCorpusReader.getCorpusReader(session.getTrainingReader(), posTaggerConfig.getConfig("input"), session);

		InputStream evalFile = ConfigUtils.getFileFromConfig(config, "talismane.core.pos-tagger.evaluate.eval-file");
		Reader evalReader = new BufferedReader(new InputStreamReader(evalFile, session.getInputCharset()));
		this.evaluationCorpusReader = PosTagAnnotatedCorpusReader.getCorpusReader(evalReader, posTaggerConfig.getConfig("evaluate"), session);

		this.observers = PosTagEvaluationObserver.getObservers(session);
	}

	public PosTagComparator(PosTagAnnotatedCorpusReader referenceCorpusReader, PosTagAnnotatedCorpusReader evaluationCorpusReader) {
		this.referenceCorpusReader = referenceCorpusReader;
		this.evaluationCorpusReader = evaluationCorpusReader;
		this.observers = new ArrayList<>();
	}

	/**
	 * Evaluate the evaluation corpus against the reference corpus.
	 * 
	 * @param evaluationCorpusReader
	 *            for reading manually tagged tokens from a corpus
	 */
	public void evaluate() {
		while (referenceCorpusReader.hasNextPosTagSequence()) {
			PosTagSequence realPosTagSequence = referenceCorpusReader.nextPosTagSequence();
			PosTagSequence guessedPosTagSequence = evaluationCorpusReader.nextPosTagSequence();

			List<PosTagSequence> guessedSequences = new ArrayList<PosTagSequence>();
			guessedSequences.add(guessedPosTagSequence);
			for (PosTagEvaluationObserver observer : this.observers) {
				observer.onNextPosTagSequence(realPosTagSequence, guessedSequences);
			}
		}

		for (PosTagEvaluationObserver observer : this.observers) {
			observer.onEvaluationComplete();
		}
	}

	public List<PosTagEvaluationObserver> getObservers() {
		return observers;
	}

	public void addObserver(PosTagEvaluationObserver observer) {
		this.observers.add(observer);
	}
}
