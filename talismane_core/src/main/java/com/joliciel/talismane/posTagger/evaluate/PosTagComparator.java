package com.joliciel.talismane.posTagger.evaluate;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.posTagger.PosTagAnnotatedCorpusReader;
import com.joliciel.talismane.posTagger.PosTagSequence;
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

  public PosTagComparator(Reader referenceReader, Reader evalReader, File outDir, String sessionId)
      throws IOException, ClassNotFoundException, ReflectiveOperationException, TalismaneException {
    Config config = ConfigFactory.load();
    Config posTaggerConfig = config.getConfig("talismane.core." + sessionId + ".pos-tagger");

    this.referenceCorpusReader = PosTagAnnotatedCorpusReader.getCorpusReader(referenceReader, posTaggerConfig.getConfig("input"), sessionId);

    this.evaluationCorpusReader = PosTagAnnotatedCorpusReader.getCorpusReader(evalReader, posTaggerConfig.getConfig("evaluate"), sessionId);

    this.observers = PosTagEvaluationObserver.getObservers(outDir, sessionId);
  }

  /**
   * 
   * @param referenceCorpusReader
   *          for reading manually tagged tokens from a reference corpus
   * 
   * @param evaluationCorpusReader
   *          for reading manually tagged tokens from another pos-tagged corpus
   */
  public PosTagComparator(PosTagAnnotatedCorpusReader referenceCorpusReader, PosTagAnnotatedCorpusReader evaluationCorpusReader) {
    this.referenceCorpusReader = referenceCorpusReader;
    this.evaluationCorpusReader = evaluationCorpusReader;
    this.observers = new ArrayList<>();
  }

  /**
   * Evaluate the evaluation corpus against the reference corpus.
   * 
   * @throws TalismaneException
   * @throws IOException
   */
  public void evaluate() throws TalismaneException, IOException {
    while (referenceCorpusReader.hasNextSentence()) {
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
