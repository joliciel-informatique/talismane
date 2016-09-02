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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.utils.PerformanceMonitor;
import com.joliciel.talismane.utils.WeightedOutcome;

/**
 * Detect the language of a text.
 * 
 * @author Assaf Urieli
 *
 */
public class LanguageDetector {
	private static final Logger LOG = LoggerFactory.getLogger(LanguageDetector.class);
	private static final PerformanceMonitor MONITOR = PerformanceMonitor.getMonitor(LanguageDetector.class);

	private final DecisionMaker decisionMaker;
	private final Set<LanguageDetectorFeature<?>> features;

	/**
	 * Construct a language detector from a decision maker and set of features.
	 */
	public LanguageDetector(DecisionMaker decisionMaker, Set<LanguageDetectorFeature<?>> features) {
		this.decisionMaker = decisionMaker;
		this.features = features;
	}

	/**
	 * Construct a language detector for an existing model.
	 */
	public LanguageDetector(ClassificationModel languageModel) {
		this(languageModel.getDecisionMaker(), (new LanguageDetectorFeatureFactory()).getFeatureSet(languageModel.getFeatureDescriptors()));
	}

	/**
	 * Return a probability distribution of languages for a given text.
	 */
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
				RuntimeEnvironment env = new RuntimeEnvironment();
				FeatureResult<?> featureResult = feature.check(text, env);
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

			List<WeightedOutcome<Locale>> results = new ArrayList<WeightedOutcome<Locale>>();
			for (Decision decision : decisions) {
				Locale locale = Locale.forLanguageTag(decision.getOutcome());
				results.add(new WeightedOutcome<Locale>(locale, decision.getProbability()));
			}

			return results;
		} finally {
			MONITOR.endTask();
		}
	}

	public DecisionMaker getDecisionMaker() {
		return decisionMaker;
	}

	public Set<LanguageDetectorFeature<?>> getFeatures() {
		return features;
	}
}
