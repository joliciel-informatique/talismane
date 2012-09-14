///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
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

import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.regex.Matcher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.filters.TextFilter;
import com.joliciel.talismane.sentenceDetector.features.SentenceDetectorFeature;
import com.joliciel.talismane.sentenceDetector.features.SentenceDetectorFeatureService;
import com.joliciel.talismane.utils.DecisionMaker;
import com.joliciel.talismane.utils.features.FeatureResult;
import com.joliciel.talismane.utils.util.PerformanceMonitor;
import com.joliciel.talismane.utils.util.WeightedOutcome;

public class SentenceDetectorImpl implements SentenceDetector {
	private static final Log LOG = LogFactory.getLog(SentenceDetectorImpl.class);
	private DecisionMaker decisionMaker;
	private Set<SentenceDetectorFeature<?>> features;
	private List<TextFilter> textFilters = new ArrayList<TextFilter>();
	private String hardStop = "\n";
	
	private SentenceDetectorService sentenceDetectorService;
	private SentenceDetectorFeatureService sentenceDetectorFeatureService;
	
	public SentenceDetectorImpl(DecisionMaker decisionMaker,
			Set<SentenceDetectorFeature<?>> features) {
		super();
		this.decisionMaker = decisionMaker;
		this.features = features;
	}
	
	@Override
	public List<Integer> detectSentences(String prevText, String text, String moreText) {
		PerformanceMonitor.startTask("SentenceDetectorImpl.detectSentences");
		try {
			String context = prevText + text + moreText;

			Matcher matcher = SentenceDetector.POSSIBLE_BOUNDARIES.matcher(text);
			List<Integer> possibleBoundaries = new ArrayList<Integer>();
			List<Integer> guessedBoundaries = new ArrayList<Integer>();
			
			while (matcher.find()) {
				possibleBoundaries.add(prevText.length() + matcher.start());
			}
	
			for (int possibleBoundary : possibleBoundaries) {
				PossibleSentenceBoundary boundary = this.sentenceDetectorService.getPossibleSentenceBoundary(context, possibleBoundary);
				if (LOG.isTraceEnabled()) {
					LOG.trace("Testing boundary: " + boundary);
					LOG.trace(" at position: " + possibleBoundary);
				}
				if (boundary.getBoundaryString().equals(hardStop)) {
					LOG.trace("Is hard stop");
					guessedBoundaries.add(possibleBoundary);
				} else {
					List<FeatureResult<?>> featureResults = new ArrayList<FeatureResult<?>>();
					for (SentenceDetectorFeature<?> feature : features) {
						FeatureResult<?> featureResult = feature.check(boundary);
						if (featureResult!=null)
							featureResults.add(featureResult);
					}
					if (LOG.isTraceEnabled()) {
						for (FeatureResult<?> result : featureResults) {
							LOG.trace(result.getName() + ": " + result.getOutcome());
						}
					}
					
					List<WeightedOutcome<String>> weightedOutcomes = this.decisionMaker.decide(featureResults);
					List<WeightedOutcome<SentenceDetectorDecision>> decisions = new ArrayList<WeightedOutcome<SentenceDetectorDecision>>();
					for (WeightedOutcome<String> weightedOutcome : weightedOutcomes) {
						SentenceDetectorDecision decision = SentenceDetectorDecision.valueOf(weightedOutcome.getOutcome());
						WeightedOutcome<SentenceDetectorDecision> weightedDecision = new WeightedOutcome<SentenceDetectorDecision>(decision, weightedOutcome.getWeight());
						decisions.add(weightedDecision);
						if (LOG.isTraceEnabled()) {
							LOG.trace(weightedDecision.getOutcome() + ": " + weightedDecision.getWeight());
						}
					}
					if (decisions.get(0).getOutcome().equals(SentenceDetectorDecision.IS_BOUNDARY)) {
						guessedBoundaries.add(possibleBoundary);
						if (LOG.isTraceEnabled()) {
							LOG.trace("Adding boundary: " + possibleBoundary);
						}
					}
				}
			}
			return guessedBoundaries;
		} finally {
			PerformanceMonitor.endTask("SentenceDetectorImpl.detectSentences");
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

	@Override
	public List<TextFilter> getTextFilters() {
		return textFilters;
	}

	@Override
	public void setTextFilters(List<TextFilter> textFilters) {
		this.textFilters = textFilters;
	}

	@Override
	public void addTextFilter(TextFilter textFilter) {
		this.textFilters.add(textFilter);
	}

}
