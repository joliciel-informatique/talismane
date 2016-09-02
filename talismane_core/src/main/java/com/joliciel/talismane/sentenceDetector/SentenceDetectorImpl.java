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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.sentenceDetector.features.SentenceDetectorFeature;
import com.joliciel.talismane.tokeniser.filters.TokenFilter;
import com.joliciel.talismane.tokeniser.filters.TokenPlaceholder;
import com.joliciel.talismane.utils.PerformanceMonitor;

class SentenceDetectorImpl implements SentenceDetector {
	private static final Logger LOG = LoggerFactory.getLogger(SentenceDetectorImpl.class);
	private static final PerformanceMonitor MONITOR = PerformanceMonitor.getMonitor(SentenceDetectorImpl.class);

	private DecisionMaker decisionMaker;
	private Set<SentenceDetectorFeature<?>> features;
	private List<TokenFilter> preTokeniserFilters = new ArrayList<TokenFilter>();

	private SentenceDetectorService sentenceDetectorService;

	public SentenceDetectorImpl(DecisionMaker decisionMaker, Set<SentenceDetectorFeature<?>> features) {
		super();
		this.decisionMaker = decisionMaker;
		this.features = features;
	}

	@Override
	public List<Integer> detectSentences(String prevText, String text, String moreText) {
		MONITOR.startTask("detectSentences");
		try {
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
				PossibleSentenceBoundary boundary = this.sentenceDetectorService.getPossibleSentenceBoundary(context, possibleBoundary);
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
		} finally {
			MONITOR.endTask();
		}
	}

	public DecisionMaker getDecisionMaker() {
		return decisionMaker;
	}

	public Set<SentenceDetectorFeature<?>> getFeatures() {
		return features;
	}

	public SentenceDetectorService getSentenceDetectorService() {
		return sentenceDetectorService;
	}

	public void setSentenceDetectorService(SentenceDetectorService sentenceDetectorService) {
		this.sentenceDetectorService = sentenceDetectorService;
	}

	@Override
	public List<TokenFilter> getTokenFilters() {
		return preTokeniserFilters;
	}

	@Override
	public void addTokenFilter(TokenFilter filter) {
		this.preTokeniserFilters.add(filter);
	}

}
