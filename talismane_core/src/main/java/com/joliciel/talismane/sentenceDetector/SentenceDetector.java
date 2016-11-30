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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.Annotation;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.filters.RollingTextBlock;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.ExternalResource;
import com.joliciel.talismane.machineLearning.ExternalResourceFinder;
import com.joliciel.talismane.machineLearning.MachineLearningModelFactory;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.sentenceDetector.features.SentenceDetectorFeature;
import com.joliciel.talismane.sentenceDetector.features.SentenceDetectorFeatureParser;
import com.joliciel.talismane.tokeniser.filters.TokenPlaceholder;
import com.joliciel.talismane.utils.ConfigUtils;
import com.typesafe.config.Config;

/**
 * Detect sentence boundaries within a textual block. The linefeed character
 * will always be considered to be a sentence boundary. Therefore, filters prior
 * to the sentence detector should remove any arbitrary linefeeds, and insert
 * linefeeds instead of any other patterns indicating a sentence boundary (e.g.
 * XML tags).
 * 
 * @author Assaf Urieli
 *
 */
public class SentenceDetector {
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

	public static SentenceDetector getInstance(TalismaneSession session) throws IOException {
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

	/**
	 * Detect sentences within a particular textual block, given the previous
	 * and next textual blocks.
	 * 
	 * @param textBlock
	 *            the text block in which we want to detect sentences
	 * @return a List of integers marking the index of the last character in
	 *         each sentence within the current textual block. The index is
	 *         relative to the current block only (textBlock.getText()), not the
	 *         full context (prevText + text + nextText).
	 */
	public List<Integer> detectSentences(RollingTextBlock textBlock) {
		String context = textBlock.getPrevText() + textBlock.getText() + textBlock.getNextText();

		List<Annotation<TokenPlaceholder>> placeholders = textBlock.getAnnotations(TokenPlaceholder.class);
		List<Annotation<TokenPlaceholder>> newPlaceholders = new ArrayList<>();

		Annotation<TokenPlaceholder> lastPlaceholder = null;
		for (Annotation<TokenPlaceholder> placeholder : placeholders) {
			// take the first placeholder at this start index only
			// thus declaration order is the order at which they're
			// applied
			if (lastPlaceholder == null || placeholder.getStart() > lastPlaceholder.getStart()) {
				newPlaceholders.add(placeholder);
			}
			lastPlaceholder = placeholder;
		}
		placeholders = newPlaceholders;

		Matcher matcher = SentenceDetector.POSSIBLE_BOUNDARIES.matcher(textBlock.getCurrentText());
		Set<Integer> possibleBoundaries = new HashSet<Integer>();
		List<Integer> guessedBoundaries = new ArrayList<Integer>();

		while (matcher.find()) {
			// Only add possible boundaries if they're not inside a
			// placeholder
			// Note that we allow boundaries at the last position of the
			// placeholder (placeholder.getEndIndex()-1)
			boolean inPlaceholder = false;
			int position = textBlock.getPrevText().length() + matcher.start();
			for (Annotation<TokenPlaceholder> placeholder : placeholders) {
				int endPos = placeholder.getEnd();
				if (placeholder.getData().isPossibleSentenceBoundary()) {
					endPos -= 1;
				}
				if (placeholder.getStart() <= position && position < endPos) {
					inPlaceholder = true;
					break;
				}
			}
			if (!inPlaceholder)
				possibleBoundaries.add(position);
		}

		for (int possibleBoundary : possibleBoundaries) {
			PossibleSentenceBoundary boundary = new PossibleSentenceBoundary(context, possibleBoundary, session);
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
				guessedBoundaries.add(possibleBoundary - textBlock.getPrevText().length());
				if (LOG.isTraceEnabled()) {
					LOG.trace("Adding boundary: " + possibleBoundary);
				}
			}
		} // have we a possible boundary at this position?

		return guessedBoundaries;
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
