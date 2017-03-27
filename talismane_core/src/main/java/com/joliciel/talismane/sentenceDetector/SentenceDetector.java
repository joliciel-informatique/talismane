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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.AnnotatedText;
import com.joliciel.talismane.Annotation;
import com.joliciel.talismane.Annotator;
import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.ExternalResource;
import com.joliciel.talismane.machineLearning.ExternalResourceFinder;
import com.joliciel.talismane.machineLearning.MachineLearningModelFactory;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.rawText.RawTextMarker.RawTextNoSentenceBreakMarker;
import com.joliciel.talismane.rawText.RawTextMarker.RawTextSentenceBreakMarker;
import com.joliciel.talismane.sentenceDetector.features.SentenceDetectorFeature;
import com.joliciel.talismane.sentenceDetector.features.SentenceDetectorFeatureParser;
import com.joliciel.talismane.utils.ConfigUtils;
import com.typesafe.config.Config;

/**
 * Detects sentence boundaries within an annotated text. <br/>
 * 
 * 
 * @author Assaf Urieli
 *
 */
public class SentenceDetector implements Annotator<AnnotatedText> {
  /**
   * A list of possible sentence-end boundaries.
   */
  public static final Pattern POSSIBLE_BOUNDARIES = Pattern.compile("[\\.\\?\\!\"\\)\\]\\}»—―”″\n]");

  private static final Logger LOG = LoggerFactory.getLogger(SentenceDetector.class);

  private static final Map<String, ClassificationModel> modelMap = new HashMap<>();
  private static final Map<String, SentenceDetector> sentenceDetectorMap = new HashMap<>();

  private final DecisionMaker decisionMaker;
  private final Set<SentenceDetectorFeature<?>> features;
  private final TalismaneSession session;

  public static SentenceDetector getInstance(TalismaneSession session) throws IOException, ClassNotFoundException {
    SentenceDetector sentenceDetector = null;
    if (session.getSessionId() != null)
      sentenceDetector = sentenceDetectorMap.get(session.getSessionId());
    if (sentenceDetector == null) {
      Config config = session.getConfig();

      String configPath = "talismane.core.sentence-detector.model";
      String modelFilePath = config.getString(configPath);
      ClassificationModel sentenceModel = modelMap.get(modelFilePath);
      if (sentenceModel == null) {
        InputStream modelFile = ConfigUtils.getFileFromConfig(config, configPath);
        MachineLearningModelFactory factory = new MachineLearningModelFactory();
        sentenceModel = factory.getClassificationModel(new ZipInputStream(modelFile));
        modelMap.put(modelFilePath, sentenceModel);
      }

      sentenceDetector = new SentenceDetector(sentenceModel, session);

      if (session.getSessionId() != null)
        sentenceDetectorMap.put(session.getSessionId(), sentenceDetector);
    }
    return sentenceDetector.cloneSentenceDetector();
  }

  public SentenceDetector(DecisionMaker decisionMaker, Set<SentenceDetectorFeature<?>> features, TalismaneSession session) {
    this.decisionMaker = decisionMaker;
    this.features = features;
    this.session = session;
  }

  public SentenceDetector(ClassificationModel sentenceModel, TalismaneSession session) {
    this.session = session;

    SentenceDetectorFeatureParser parser = new SentenceDetectorFeatureParser(session);

    Collection<ExternalResource<?>> externalResources = sentenceModel.getExternalResources();
    if (externalResources != null) {
      ExternalResourceFinder externalResourceFinder = parser.getExternalResourceFinder();

      for (ExternalResource<?> externalResource : externalResources) {
        externalResourceFinder.addExternalResource(externalResource);
      }
    }

    this.features = parser.getFeatureSet(sentenceModel.getFeatureDescriptors());
    this.decisionMaker = sentenceModel.getDecisionMaker();
  }

  SentenceDetector(SentenceDetector sentenceDetector) {
    this.session = sentenceDetector.session;
    this.features = new HashSet<>(sentenceDetector.features);
    this.decisionMaker = sentenceDetector.decisionMaker;
  }

  @Override
  public void annotate(AnnotatedText annotatedText, String... labels) throws TalismaneException {
    this.detectSentences(annotatedText, labels);
  }

  /**
   * Detect sentences within an annotated text. Sentences are added in the form
   * of an Annotation around a {@link SentenceBoundary}, with the start position
   * (relative to the start of the annotated text) at the start of the sentence
   * and the end position immediately after the end of the sentence. <br/>
   * <br/>
   * Sentence boundaries will not be detected within any annotation of type
   * {@link RawTextNoSentenceBreakMarker}, nor will they be detected before or
   * after the {@link AnnotatedText#getAnalysisStart()} and
   * {@link AnnotatedText#getAnalysisEnd()} respectively. <br/>
   * <br/>
   * If the text contained existing {@link SentenceBoundary} annotations before
   * analysis start, the first sentence will begin where the last existing
   * annotation ended. Otherwise, the first boundary will begin at position 0.
   * <br/>
   * <br/>
   * If the text's analysis end is equal to the text length, it is assumed that
   * the text end is a sentence boundary. In this case, an additional sentence
   * is added starting at the final detected boundary and ending at text end.
   * 
   * @param text
   *          the annotated text in which we need to detect sentences.
   * @return in addition to the annotations added, we return a List of integers
   *         marking the end position of each sentence boundary.
   */
  public List<Integer> detectSentences(AnnotatedText text, String... labels) throws TalismaneException {
    LOG.debug("detectSentences");

    List<Annotation<RawTextNoSentenceBreakMarker>> noSentenceBreakMarkers = text.getAnnotations(RawTextNoSentenceBreakMarker.class);

    Matcher matcher = SentenceDetector.POSSIBLE_BOUNDARIES.matcher(text.getText());
    List<Integer> possibleBoundaries = new ArrayList<Integer>();
    while (matcher.find()) {
      if (matcher.start() >= text.getAnalysisStart() && matcher.start() < text.getAnalysisEnd()) {
        boolean noSentences = false;
        int position = matcher.start();
        for (Annotation<RawTextNoSentenceBreakMarker> noSentenceBreakMarker : noSentenceBreakMarkers) {
          if (noSentenceBreakMarker.getStart() <= position && position < noSentenceBreakMarker.getEnd()) {
            noSentences = true;
            break;
          }
        }
        if (!noSentences)
          possibleBoundaries.add(position);
      }
    }

    // collect all deterministic sentence boundaries
    List<Annotation<RawTextSentenceBreakMarker>> sentenceBreakMarkers = text.getAnnotations(RawTextSentenceBreakMarker.class);
    Set<Integer> guessedBoundaries = new TreeSet<>(
        sentenceBreakMarkers.stream().filter(f -> f.getEnd() >= text.getAnalysisStart()).map(f -> f.getEnd()).collect(Collectors.toList()));

    List<PossibleSentenceBoundary> boundaries = new ArrayList<>();
    for (int possibleBoundary : possibleBoundaries) {
      PossibleSentenceBoundary boundary = new PossibleSentenceBoundary(text.getText(), possibleBoundary, session);
      if (LOG.isTraceEnabled()) {
        LOG.trace("Testing boundary: " + boundary);
        LOG.trace(" at position: " + possibleBoundary);
      }

      List<FeatureResult<?>> featureResults = new ArrayList<FeatureResult<?>>();
      for (SentenceDetectorFeature<?> feature : features) {
        RuntimeEnvironment env = new RuntimeEnvironment();
        FeatureResult<?> featureResult = feature.check(boundary, env);
        if (featureResult != null)
          featureResults.add(featureResult);
      }
      if (LOG.isTraceEnabled()) {
        for (FeatureResult<?> result : featureResults) {
          LOG.trace(result.toString());
        }
      }

      List<Decision> decisions = this.decisionMaker.decide(featureResults);
      if (LOG.isTraceEnabled()) {
        for (Decision decision : decisions) {
          LOG.trace(decision.getOutcome() + ": " + decision.getProbability());
        }
      }

      if (decisions.get(0).getOutcome().equals(SentenceDetectorOutcome.IS_BOUNDARY.name())) {
        if (LOG.isTraceEnabled()) {
          LOG.trace("Adding boundary: " + possibleBoundary + 1);
        }

        guessedBoundaries.add(possibleBoundary + 1);
        boundaries.add(boundary);
      }
    } // have we a possible boundary at this position?

    if (LOG.isTraceEnabled()) {
      LOG.trace("context: " + text.getText().toString().replace('\n', '¶').replace('\r', '¶'));

      for (PossibleSentenceBoundary boundary : boundaries)
        LOG.trace("boundary: " + boundary.toString());
    }
    if (LOG.isDebugEnabled())
      LOG.debug("guessedBoundaries : " + guessedBoundaries.toString());

    List<Annotation<SentenceBoundary>> newBoundaries = new ArrayList<>();
    int lastBoundary = 0;
    List<Annotation<SentenceBoundary>> existingBoundaries = text.getAnnotations(SentenceBoundary.class);
    if (existingBoundaries.size() > 0) {
      lastBoundary = existingBoundaries.get(existingBoundaries.size() - 1).getEnd();
    }
    for (int guessedBoundary : guessedBoundaries) {
      if (guessedBoundary > lastBoundary) {
        Annotation<SentenceBoundary> sentenceBoundary = new Annotation<>(lastBoundary, guessedBoundary, new SentenceBoundary(), labels);
        newBoundaries.add(sentenceBoundary);
        if (LOG.isTraceEnabled()) {
          LOG.trace("Added boundary: " + sentenceBoundary);
        }
        lastBoundary = guessedBoundary;
      }
    }
    if (text.getAnalysisEnd() == text.getText().length()) {
      if (text.getAnalysisEnd() > lastBoundary) {
        Annotation<SentenceBoundary> sentenceBoundary = new Annotation<>(lastBoundary, text.getAnalysisEnd(), new SentenceBoundary(), labels);
        newBoundaries.add(sentenceBoundary);
        if (LOG.isTraceEnabled()) {
          LOG.trace("Added final boundary: " + sentenceBoundary);
        }
      }
    }
    text.addAnnotations(newBoundaries);

    return new ArrayList<>(guessedBoundaries);
  }

  public DecisionMaker getDecisionMaker() {
    return decisionMaker;
  }

  public Set<SentenceDetectorFeature<?>> getFeatures() {
    return features;
  }

  public SentenceDetector cloneSentenceDetector() {
    return new SentenceDetector(this);
  }
}
