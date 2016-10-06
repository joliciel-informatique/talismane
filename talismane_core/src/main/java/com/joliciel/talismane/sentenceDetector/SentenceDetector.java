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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.ExternalResource;
import com.joliciel.talismane.machineLearning.ExternalResourceFinder;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.sentenceDetector.features.SentenceDetectorFeature;
import com.joliciel.talismane.sentenceDetector.features.SentenceDetectorFeatureParser;
import com.joliciel.talismane.tokeniser.filters.TokenFilter;
import com.joliciel.talismane.tokeniser.filters.TokenPlaceholder;

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

	private final DecisionMaker decisionMaker;
	private final Set<SentenceDetectorFeature<?>> features;
	private final TalismaneSession talismaneSession;

	private final List<TokenFilter> preTokeniserFilters;

	public SentenceDetector(DecisionMaker decisionMaker, Set<SentenceDetectorFeature<?>> features, TalismaneSession talismaneSession) {
		this.decisionMaker = decisionMaker;
		this.features = features;
		this.talismaneSession = talismaneSession;
		this.preTokeniserFilters = new ArrayList<TokenFilter>();
	}

	public SentenceDetector(ClassificationModel sentenceModel, TalismaneSession talismaneSession) {
		this.talismaneSession = talismaneSession;

		SentenceDetectorFeatureParser parser = new SentenceDetectorFeatureParser(talismaneSession);

		Collection<ExternalResource<?>> externalResources = sentenceModel.getExternalResources();
		if (externalResources != null) {
			ExternalResourceFinder externalResourceFinder = parser.getExternalResourceFinder();

			for (ExternalResource<?> externalResource : externalResources) {
				externalResourceFinder.addExternalResource(externalResource);
			}
		}

		this.features = parser.getFeatureSet(sentenceModel.getFeatureDescriptors());
		this.decisionMaker = sentenceModel.getDecisionMaker();
		this.preTokeniserFilters = new ArrayList<TokenFilter>();
	}

	SentenceDetector(SentenceDetector sentenceDetector) {
		this.talismaneSession = sentenceDetector.talismaneSession;
		this.features = new HashSet<>(sentenceDetector.features);
		this.decisionMaker = sentenceDetector.decisionMaker;
		this.preTokeniserFilters = new ArrayList<>(sentenceDetector.preTokeniserFilters);
	}

	/**
	 * Detect sentences within a particular textual block, given the previous
	 * and next textual blocks.
	 * 
	 * @param prevText
	 *            the previous textual block
	 * @param text
	 *            the current textual block
	 * @param moreText
	 *            the following textual block
	 * @return a List of integers marking the index of the last character in
	 *         each sentence within the current textual block. The index is
	 *         relative to the current block only (text), not the full context
	 *         (prevText + text + nextText).
	 */
	public List<Integer> detectSentences(String prevText, String text, String moreText) {
		String context = prevText + text + moreText;

		// we only want one placeholder per start index - the first one that
		// gets added
		Map<Integer, TokenPlaceholder> placeholderMap = new HashMap<Integer, TokenPlaceholder>();
		for (TokenFilter filter : this.preTokeniserFilters) {
			List<TokenPlaceholder> myPlaceholders = filter.apply(context);
			for (TokenPlaceholder placeholder : myPlaceholders) {
				if (!placeholderMap.containsKey(placeholder.getStartIndex())) {
					placeholderMap.put(placeholder.getStartIndex(), placeholder);
				}
			}
		}

		Matcher matcher = SentenceDetector.POSSIBLE_BOUNDARIES.matcher(text);
		Set<Integer> possibleBoundaries = new HashSet<Integer>();
		List<Integer> guessedBoundaries = new ArrayList<Integer>();

		while (matcher.find()) {
			// Only add possible boundaries if they're not inside a
			// placeholder
			// Note that we allow boundaries at the last position of the
			// placeholder (placeholder.getEndIndex()-1)
			boolean inPlaceholder = false;
			int position = prevText.length() + matcher.start();
			for (TokenPlaceholder placeholder : placeholderMap.values()) {
				int endPos = placeholder.getEndIndex();
				if (placeholder.isPossibleSentenceBoundary()) {
					endPos -= 1;
				}
				if (placeholder.getStartIndex() <= position && position < endPos) {
					inPlaceholder = true;
					break;
				}
			}
			if (!inPlaceholder)
				possibleBoundaries.add(position);
		}

		for (int possibleBoundary : possibleBoundaries) {
			PossibleSentenceBoundary boundary = new PossibleSentenceBoundary(context, possibleBoundary, talismaneSession);
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
				guessedBoundaries.add(possibleBoundary - prevText.length());
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

	/**
	 * Token filters mark certain portions of the raw text as entire tokens - a
	 * sentence break will never be detected inside such a token.
	 */
	public List<TokenFilter> getTokenFilters() {
		return preTokeniserFilters;
	}

	public void addTokenFilter(TokenFilter filter) {
		this.preTokeniserFilters.add(filter);
	}

	public SentenceDetector cloneSentenceDetector() {
		return new SentenceDetector(this);
	}
}
