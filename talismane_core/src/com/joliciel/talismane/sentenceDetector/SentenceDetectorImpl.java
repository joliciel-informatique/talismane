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
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.sentenceDetector.features.SentenceDetectorFeature;
import com.joliciel.talismane.sentenceDetector.features.SentenceDetectorFeatureService;
import com.joliciel.talismane.utils.PerformanceMonitor;

public class SentenceDetectorImpl implements SentenceDetector {
	private static final Log LOG = LogFactory.getLog(SentenceDetectorImpl.class);
	private DecisionMaker<SentenceDetectorOutcome> decisionMaker;
	private Set<SentenceDetectorFeature<?>> features;
	private List<TextFilter> textFilters = new ArrayList<TextFilter>();
	private String hardStop = "\n";
	
	private SentenceDetectorService sentenceDetectorService;
	private SentenceDetectorFeatureService sentenceDetectorFeatureService;
	
	public SentenceDetectorImpl(DecisionMaker<SentenceDetectorOutcome> decisionMaker,
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
					
					List<Decision<SentenceDetectorOutcome>> decisions = this.decisionMaker.decide(featureResults);
					if (LOG.isTraceEnabled()) {
						for (Decision<SentenceDetectorOutcome> decision : decisions) {
							LOG.trace(decision.getCode() + ": " + decision.getProbability());
						}
					}
					
					if (decisions.get(0).getOutcome().equals(SentenceDetectorOutcome.IS_BOUNDARY)) {
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
