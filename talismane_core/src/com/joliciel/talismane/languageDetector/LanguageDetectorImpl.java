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
package com.joliciel.talismane.languageDetector;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.ArrayList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.FeatureService;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.utils.PerformanceMonitor;
import com.joliciel.talismane.utils.WeightedOutcome;

class LanguageDetectorImpl implements LanguageDetector {
	private static final Log LOG = LogFactory.getLog(LanguageDetectorImpl.class);
	private static final PerformanceMonitor MONITOR = PerformanceMonitor.getMonitor(LanguageDetectorImpl.class);

	private DecisionMaker<LanguageOutcome> decisionMaker;
	private Set<LanguageDetectorFeature<?>> features;
	
	private LanguageDetectorService languageDetectorService;
	private FeatureService featureService;
	
	public LanguageDetectorImpl(DecisionMaker<LanguageOutcome> decisionMaker,
			Set<LanguageDetectorFeature<?>> features) {
		super();
		this.decisionMaker = decisionMaker;
		this.features = features;
	}
	
	@Override
	public List<WeightedOutcome<Locale>> detectLanguages(String text) {
		MONITOR.startTask("detectLanguages");
		try {
			
			if (LOG.isTraceEnabled()) {
				LOG.trace("Testing text: " + text);
			}
			
			text = text.toLowerCase(Locale.ENGLISH);
			text = Normalizer.normalize(text, Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

			List<FeatureResult<?>> featureResults = new ArrayList<FeatureResult<?>>();
			for (LanguageDetectorFeature<?> feature : features) {
				RuntimeEnvironment env = this.featureService.getRuntimeEnvironment();
				FeatureResult<?> featureResult = feature.check(text, env);
				if (featureResult!=null)
					featureResults.add(featureResult);
			}
			if (LOG.isTraceEnabled()) {
				for (FeatureResult<?> result : featureResults) {
					LOG.trace(result.toString());
				}
			}
			
			List<Decision<LanguageOutcome>> decisions = this.decisionMaker.decide(featureResults);
			if (LOG.isTraceEnabled()) {
				for (Decision<LanguageOutcome> decision : decisions) {
					LOG.trace(decision.getCode() + ": " + decision.getProbability());
				}
			}
			
			List<WeightedOutcome<Locale>> results = new ArrayList<WeightedOutcome<Locale>>();
			for (Decision<LanguageOutcome> decision : decisions) {
				Locale locale = Locale.forLanguageTag(decision.getOutcome().getCode());
				results.add(new WeightedOutcome<Locale>(locale, decision.getProbability()));
			}
			
			return results;
		} finally {
			MONITOR.endTask();
		}
	}
	
	public DecisionMaker<LanguageOutcome> getDecisionMaker() {
		return decisionMaker;
	}

	public Set<LanguageDetectorFeature<?>> getFeatures() {
		return features;
	}

	public LanguageDetectorService getLanguageDetectorService() {
		return languageDetectorService;
	}

	public void setLanguageDetectorService(
			LanguageDetectorService languageDetectorService) {
		this.languageDetectorService = languageDetectorService;
	}

	public FeatureService getFeatureService() {
		return featureService;
	}

	public void setFeatureService(FeatureService featureService) {
		this.featureService = featureService;
	}
	
	
}
