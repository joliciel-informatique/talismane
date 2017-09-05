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
package com.joliciel.talismane.sentenceDetector;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.AnnotatedText;
import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.stats.FScoreCalculator;
import com.joliciel.talismane.utils.StringUtils;
import com.typesafe.config.Config;

/**
 * An interface for evaluating a given sentence detector.
 * 
 * @author Assaf Urieli
 *
 */
public class SentenceDetectorEvaluator {
  private static final Logger LOG = LoggerFactory.getLogger(SentenceDetectorEvaluator.class);
  private final SentenceDetector sentenceDetector;
  private final SentenceDetectorAnnotatedCorpusReader corpusReader;
  private final Writer errorWriter;
  private final int minCharactersAfterBoundary = 50;

  private static final int NUM_CHARS = 30;

  public SentenceDetectorEvaluator(Reader evalReader, File outDir, TalismaneSession session)
      throws IOException, ClassNotFoundException, ReflectiveOperationException {
    if (outDir != null)
      outDir.mkdirs();

    Config config = session.getConfig();
    this.sentenceDetector = SentenceDetector.getInstance(session);
    Config sentenceConfig = config.getConfig("talismane.core." + session.getId() + ".sentence-detector");
    this.corpusReader = SentenceDetectorAnnotatedCorpusReader.getCorpusReader(evalReader, sentenceConfig.getConfig("input"), session);

    File sentenceErrorFile = new File(outDir, session.getBaseName() + "_errors.txt");
    this.errorWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(sentenceErrorFile, false), "UTF8"));
  }

  /**
   * 
   * @param sentenceDetector
   * @param corpusReader
   *          for reading manually separated sentences from a corpus
   * @param errorWriter
   * @param session
   */
  public SentenceDetectorEvaluator(SentenceDetector sentenceDetector, SentenceDetectorAnnotatedCorpusReader corpusReader, Writer errorWriter,
      TalismaneSession session) {
    this.sentenceDetector = sentenceDetector;
    this.corpusReader = corpusReader;
    this.errorWriter = errorWriter;
  }

  /**
   * Evaluate a given sentence detector.
   * 
   * @return an f-score calculator for this sentence detector
   * @throws TalismaneException
   * @throws IOException
   */
  public FScoreCalculator<SentenceDetectorOutcome> evaluate() throws TalismaneException, IOException {
    FScoreCalculator<SentenceDetectorOutcome> fScoreCalculator = new FScoreCalculator<SentenceDetectorOutcome>();

    // add f-score per tagger module, to see how we do for each boundary
    // character
    Map<String, FScoreCalculator<SentenceDetectorOutcome>> taggerFScoreCalculators = new TreeMap<String, FScoreCalculator<SentenceDetectorOutcome>>();
    Map<String, List<String>> errorMap = new TreeMap<String, List<String>>();

    LinkedList<String> sentences = new LinkedList<String>();
    String sentence = null;
    String previousSentence = ". ";
    if (corpusReader.hasNextSentence())
      sentence = corpusReader.nextSentence().getText().toString();

    sentences.add(sentence);

    while (!sentences.isEmpty()) {
      sentence = sentences.poll();
      LOG.debug("Sentence: " + sentence);

      String moreText = "";
      int sentenceIndex = 0;
      while (moreText.length() < minCharactersAfterBoundary) {
        String nextSentence = "";
        if (sentenceIndex < sentences.size()) {
          nextSentence = sentences.get(sentenceIndex);
        } else if (corpusReader.hasNextSentence()) {
          nextSentence = corpusReader.nextSentence().getText().toString();

          sentences.add(nextSentence);
        } else {
          break;
        }
        if (nextSentence.startsWith(" ") || nextSentence.startsWith("\n"))
          moreText += nextSentence;
        else
          moreText += " " + nextSentence;
        sentenceIndex++;
      }

      String text = previousSentence + sentence + moreText;
      AnnotatedText annotatedText = new AnnotatedText(text, previousSentence.length(), previousSentence.length() + sentence.length(), new ArrayList<>());

      Matcher matcher = SentenceDetector.POSSIBLE_BOUNDARIES.matcher(text);
      List<Integer> possibleBoundaries = new ArrayList<Integer>();

      while (matcher.find()) {
        if (matcher.start() >= annotatedText.getAnalysisStart() && matcher.start() < annotatedText.getAnalysisEnd())
          possibleBoundaries.add(matcher.start());
      }

      int realBoundary = previousSentence.length() + sentence.length();
      if (!possibleBoundaries.contains(realBoundary))
        possibleBoundaries.add(realBoundary);

      List<Integer> guessedBoundaries = this.sentenceDetector.detectSentences(annotatedText);
      for (int possibleBoundary : possibleBoundaries) {
        SentenceDetectorOutcome expected = SentenceDetectorOutcome.IS_NOT_BOUNDARY;
        SentenceDetectorOutcome guessed = SentenceDetectorOutcome.IS_NOT_BOUNDARY;
        if (possibleBoundary == realBoundary)
          expected = SentenceDetectorOutcome.IS_BOUNDARY;
        if (guessedBoundaries.contains(possibleBoundary))
          guessed = SentenceDetectorOutcome.IS_BOUNDARY;
        fScoreCalculator.increment(expected, guessed);

        String boundaryCharacter = "" + text.charAt(possibleBoundary - 1);
        Matcher boundaryMatcher = SentenceDetector.POSSIBLE_BOUNDARIES.matcher(boundaryCharacter);
        if (!boundaryMatcher.matches())
          boundaryCharacter = "OTHER";
        FScoreCalculator<SentenceDetectorOutcome> taggerFScoreCalculator = taggerFScoreCalculators.get(boundaryCharacter);
        if (taggerFScoreCalculator == null) {
          taggerFScoreCalculator = new FScoreCalculator<SentenceDetectorOutcome>();
          taggerFScoreCalculators.put(boundaryCharacter, taggerFScoreCalculator);
        }
        taggerFScoreCalculator.increment(expected, guessed);

        if (!expected.equals(guessed)) {
          int start1 = possibleBoundary - NUM_CHARS;
          int end1 = possibleBoundary + NUM_CHARS;

          if (start1 < 0)
            start1 = 0;
          String startString = text.substring(start1, possibleBoundary - 1);
          startString = StringUtils.padLeft(startString, NUM_CHARS);

          String middleString = "" + text.charAt(possibleBoundary - 1);
          if (end1 >= text.length())
            end1 = text.length() - 1;
          String endString = "";
          if (end1 >= 0 && possibleBoundary < text.length())
            endString = text.substring(possibleBoundary, end1);

          String testText = startString + "[" + middleString + "]" + endString;
          testText = testText.replace('\n', '¶');

          String error = "Guessed " + guessed + ", Expected " + expected + ". Text: " + testText;
          LOG.debug(error);
          List<String> errors = errorMap.get(boundaryCharacter);
          if (errors == null) {
            errors = new ArrayList<String>();
            errorMap.put(boundaryCharacter, errors);
          }
          errors.add(error);
        } // have error
      } // next possible boundary
      if (sentence.endsWith(" "))
        previousSentence = sentence;
      else
        previousSentence = sentence + " ";
    } // next sentence

    for (String boundaryCharacter : taggerFScoreCalculators.keySet()) {
      LOG.debug("###### Boundary " + boundaryCharacter);
      FScoreCalculator<SentenceDetectorOutcome> taggerFScoreCalculator = taggerFScoreCalculators.get(boundaryCharacter);
      LOG.debug("###### Boundary " + boundaryCharacter + ": f-score = " + taggerFScoreCalculator.getTotalFScore());
    } // f-scores

    if (errorWriter != null) {
      for (String boundaryCharacter : taggerFScoreCalculators.keySet()) {
        FScoreCalculator<SentenceDetectorOutcome> taggerFScoreCalculator = taggerFScoreCalculators.get(boundaryCharacter);
        errorWriter.write("###### Tagger " + boundaryCharacter + ": f-score = " + taggerFScoreCalculator.getTotalFScore() + "\n");
        errorWriter.write("Total " + (taggerFScoreCalculator.getTotalTruePositiveCount() + taggerFScoreCalculator.getTotalFalseNegativeCount()) + "\n");
        errorWriter.write("True + " + taggerFScoreCalculator.getTotalTruePositiveCount() + "\n");
        errorWriter.write("False- " + taggerFScoreCalculator.getTotalFalseNegativeCount() + "\n");
        errorWriter.write("False+ " + taggerFScoreCalculator.getTotalFalsePositiveCount() + "\n");
        for (SentenceDetectorOutcome outcome : taggerFScoreCalculator.getOutcomeSet()) {
          errorWriter.write(
              outcome + " total  " + (taggerFScoreCalculator.getTruePositiveCount(outcome) + taggerFScoreCalculator.getFalseNegativeCount(outcome)) + "\n");
          errorWriter.write(outcome + " true + " + (taggerFScoreCalculator.getTruePositiveCount(outcome)) + "\n");
          errorWriter.write(outcome + " false- " + (taggerFScoreCalculator.getFalseNegativeCount(outcome)) + "\n");
          errorWriter.write(outcome + " false+ " + (taggerFScoreCalculator.getFalsePositiveCount(outcome)) + "\n");
          errorWriter.write(outcome + " precis " + (taggerFScoreCalculator.getPrecision(outcome)) + "\n");
          errorWriter.write(outcome + " recall " + (taggerFScoreCalculator.getRecall(outcome)) + "\n");
          errorWriter.write(outcome + " fscore " + (taggerFScoreCalculator.getFScore(outcome)) + "\n");
        }

        List<String> errors = errorMap.get(boundaryCharacter);
        if (errors != null) {
          for (String error : errors) {
            errorWriter.write(error + "\n");
          }
        }
        errorWriter.flush();
      } // next boundary character
      errorWriter.close();
    } // have error writer
    return fScoreCalculator;
  }

  public Writer getErrorWriter() {
    return errorWriter;
  }
}
