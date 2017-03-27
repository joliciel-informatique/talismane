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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.machineLearning.ClassificationEvent;
import com.joliciel.talismane.machineLearning.ClassificationEventStream;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.sentenceDetector.features.SentenceDetectorFeature;

/**
 * An event stream for sentence detector training/evaluation.
 * 
 * @author Assaf Urieli
 *
 */
public class SentenceDetectorEventStream implements ClassificationEventStream {
  private static final Logger LOG = LoggerFactory.getLogger(SentenceDetectorEventStream.class);

  private final SentenceDetectorAnnotatedCorpusReader corpusReader;
  private final Set<SentenceDetectorFeature<?>> features;
  private final TalismaneSession talismaneSession;

  private String currentSentence;
  private String previousSentence = ". ";
  private int currentIndex = 0;
  private List<Integer> possibleBoundaries = new ArrayList<Integer>();
  private int realBoundary = -1;
  private LinkedList<String> sentences = new LinkedList<String>();
  int minCharactersAfterBoundary = 50;

  public SentenceDetectorEventStream(SentenceDetectorAnnotatedCorpusReader corpusReader, Set<SentenceDetectorFeature<?>> features,
      TalismaneSession talismaneSession) {
    this.corpusReader = corpusReader;
    this.features = features;
    this.talismaneSession = talismaneSession;
  }

  @Override
  public ClassificationEvent next() throws TalismaneException, IOException {
    ClassificationEvent event = null;
    if (this.hasNext()) {
      int possibleBoundary = possibleBoundaries.get(currentIndex++);

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
          moreText += sentences.get(sentenceIndex);
        else
          moreText += " " + sentences.get(sentenceIndex);

        sentenceIndex++;
      }
      String text = previousSentence + currentSentence + moreText;

      PossibleSentenceBoundary boundary = new PossibleSentenceBoundary(text, possibleBoundary, talismaneSession);
      LOG.debug("next event, boundary: " + boundary);

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
      String classification = SentenceDetectorOutcome.IS_NOT_BOUNDARY.name();
      if (possibleBoundary == realBoundary)
        classification = SentenceDetectorOutcome.IS_BOUNDARY.name();

      event = new ClassificationEvent(featureResults, classification);

      if (currentIndex == possibleBoundaries.size()) {
        if (currentSentence.endsWith(" "))
          previousSentence = currentSentence;
        else
          previousSentence = currentSentence + " ";
        currentSentence = null;
      }
    }
    return event;
  }

  @Override
  public boolean hasNext() throws TalismaneException, IOException {
    while (currentSentence == null) {
      currentIndex = 0;
      possibleBoundaries = new ArrayList<Integer>();
      if (!sentences.isEmpty()) {
        currentSentence = sentences.poll();
      } else if (corpusReader.hasNextSentence()) {
        currentSentence = corpusReader.nextSentence().getText().toString();
      } else {
        break;
      }
      if (currentSentence != null) {
        Matcher matcher = SentenceDetector.POSSIBLE_BOUNDARIES.matcher(currentSentence);

        while (matcher.find()) {
          possibleBoundaries.add(previousSentence.length() + matcher.start());
        }
        realBoundary = previousSentence.length() + currentSentence.length() - 1;
      }
      if (possibleBoundaries.size() == 0) {
        if (currentSentence.endsWith(" "))
          previousSentence = currentSentence;
        else
          previousSentence = currentSentence + " ";
        currentSentence = null;
      }
    }

    return (currentSentence != null);
  }

  @Override
  public Map<String, String> getAttributes() {
    Map<String, String> attributes = new LinkedHashMap<String, String>();
    attributes.put("eventStream", this.getClass().getSimpleName());
    attributes.put("corpusReader", corpusReader.getClass().getSimpleName());

    return attributes;
  }

}
