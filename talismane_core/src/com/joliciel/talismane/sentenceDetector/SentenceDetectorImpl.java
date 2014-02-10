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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.regex.Matcher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.FeatureService;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.sentenceDetector.features.SentenceDetectorFeature;
import com.joliciel.talismane.sentenceDetector.features.SentenceDetectorFeatureService;
import com.joliciel.talismane.tokeniser.filters.TokenFilter;
import com.joliciel.talismane.tokeniser.filters.TokenPlaceholder;
import com.joliciel.talismane.utils.PerformanceMonitor;

class SentenceDetectorImpl implements SentenceDetector {
	private static final Log LOG = LogFactory.getLog(SentenceDetectorImpl.class);
	private static final PerformanceMonitor MONITOR = PerformanceMonitor.getMonitor(SentenceDetectorImpl.class);

	private DecisionMaker<SentenceDetectorOutcome> decisionMaker;
	private Set<SentenceDetectorFeature<?>> features;
	private List<TokenFilter> preTokeniserFilters = new ArrayList<TokenFilter>();
	
	private SentenceDetectorService sentenceDetectorService;
	private SentenceDetectorFeatureService sentenceDetectorFeatureService;
	private FeatureService featureService;
	
	public SentenceDetectorImpl(DecisionMaker<SentenceDetectorOutcome> decisionMaker,
			Set<SentenceDetectorFeature<?>> features) {
		super();
		this.decisionMaker = decisionMaker;
		this.features = features;
	}
	
	@Override
	public List<Integer> detectSentences(String prevText, String text, String moreText) {
		MONITOR.startTask("detectSentences");
		try {
			String context = prevText + text + moreText;

			// we only want one placeholder per start index - the first one that gets added
			Map<Integer,TokenPlaceholder> placeholderMap = new HashMap<Integer, TokenPlaceholder>();
			for (TokenFilter filter : this.preTokeniserFilters) {
				Set<TokenPlaceholder> myPlaceholders = filter.apply(context);
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
				// only add possible boundaries if they're not inside a placeholder
				boolean inPlaceholder = false;
				int position = prevText.length() + matcher.start();
				for (TokenPlaceholder placeholder : placeholderMap.values()) {
					if (placeholder.getStartIndex()<=position && position<placeholder.getEndIndex()) {
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
					RuntimeEnvironment env = this.featureService.getRuntimeEnvironment();
					FeatureResult<?> featureResult = feature.check(boundary, env);
					if (featureResult!=null)
						featureResults.add(featureResult);
				}
				if (LOG.isTraceEnabled()) {
					for (FeatureResult<?> result : featureResults) {
						LOG.trace(result.toString());
					}
				}
				
				List<Decision<SentenceDetectorOutcome>> decisions = this.decisionMaker.decide(featureResults);
				if (LOG.isTraceEnabled()) {
					for (Decision<SentenceDetectorOutcome> decision : decisions) {
						LOG.trace(decision.getCode() + ": " + decision.getProbability());
					}
				}
				
				if (decisions.get(0).getOutcome().equals(SentenceDetectorOutcome.IS_BOUNDARY)) {
					guessedBoundaries.add(possibleBoundary - prevText.length());
					if (LOG.isTraceEnabled()) {
						LOG.trace("Adding boundary: " + possibleBoundary);
					}
				}
			} // have we a possible boundary at this position?
			
			return guessedBoundaries;
		} finally {
			MONITOR.endTask("detectSentences");
		}
	}
	
	public DecisionMaker<SentenceDetectorOutcome> getDecisionMaker() {
		return decisionMaker;
	}

	public Set<SentenceDetectorFeature<?>> getFeatures() {
		return features;
	}

	public SentenceDetectorService getSentenceDetectorService() {
		return sentenceDetectorService;
	}

	public void setSentenceDetectorService(
			SentenceDetectorService sentenceDetectorService) {
		this.sentenceDetectorService = sentenceDetectorService;
	}

	public SentenceDetectorFeatureService getSentenceDetectorFeatureService() {
		return sentenceDetectorFeatureService;
	}

	public void setSentenceDetectorFeatureService(
			SentenceDetectorFeatureService sentenceDetectorFeatureService) {
		this.sentenceDetectorFeatureService = sentenceDetectorFeatureService;
	}

	public List<TokenFilter> getTokenFilters() {
		return preTokeniserFilters;
	}

	public void addTokenFilter(TokenFilter filter) {
		this.preTokeniserFilters.add(filter);
	}

	public FeatureService getFeatureService() {
		return featureService;
	}

	public void setFeatureService(FeatureService featureService) {
		this.featureService = featureService;
	}
	
	
}
