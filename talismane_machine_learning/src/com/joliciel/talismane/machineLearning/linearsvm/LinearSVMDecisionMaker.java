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
package com.joliciel.talismane.machineLearning.linearsvm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.DecisionFactory;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.Outcome;
import com.joliciel.talismane.machineLearning.features.FeatureResult;

import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;

class LinearSVMDecisionMaker<T extends Outcome> implements DecisionMaker<T> {
	private static final Log LOG = LogFactory.getLog(LinearSVMDecisionMaker.class);
	private DecisionFactory<T> decisionFactory;
	private Model model;
	Map<String, Integer> featureIndexMap = null;
	List<String> outcomes = null;
	
	public LinearSVMDecisionMaker(Model model,
			Map<String, Integer> featureIndexMap,
			List<String> outcomes) {
		super();
		this.model = model;
		this.featureIndexMap = featureIndexMap;
		this.outcomes = outcomes;
	}

	@Override
	public List<Decision<T>> decide(List<FeatureResult<?>> featureResults) {
		List<Feature> featureList = new ArrayList<Feature>(featureResults.size());
		for (int i=0; i<featureResults.size(); i++) {
			FeatureResult<?> featureResult = featureResults.get(i);
			double value = 1.0;
			if (featureResult.getOutcome() instanceof Double)
			{
				@SuppressWarnings("unchecked")
				FeatureResult<Double> doubleResult = (FeatureResult<Double>) featureResult;
				value = doubleResult.getOutcome().doubleValue();
			}
			Integer index = featureIndexMap.get(featureResult.getTrainingName());
			if (index!=null) {
				// we only need to bother adding features which existed in the training set
				FeatureNode featureNode = new FeatureNode(index.intValue(), value);
				featureList.add(featureNode);
			}
		}
		List<Decision<T>> decisions = null;

		if (featureList.size()==0) {
			LOG.info("No features for current context.");
			TreeSet<Decision<T>> outcomeSet = new TreeSet<Decision<T>>();
			double uniformProb = 1 / outcomes.size();
			for (String outcome : outcomes) {
				Decision<T> decision = decisionFactory.createDecision(outcome, uniformProb);
				outcomeSet.add(decision);
			}
			decisions = new ArrayList<Decision<T>>(outcomeSet);
		} else {
			Feature[] instance = new Feature[1];
			instance = featureList.toArray(instance);
			
			double[] probabilities = new double[model.getLabels().length];
			Linear.predictProbability(model, instance, probabilities);
	
			TreeSet<Decision<T>> outcomeSet = new TreeSet<Decision<T>>();
			for (int i = 0; i<model.getLabels().length; i++) {
				Decision<T> decision = decisionFactory.createDecision(outcomes.get(i), probabilities[i]);
				outcomeSet.add(decision);
			}
			decisions = new ArrayList<Decision<T>>(outcomeSet);
		}

		return decisions;

	}

	public DecisionFactory<T> getDecisionFactory() {
		return decisionFactory;
	}

	public void setDecisionFactory(DecisionFactory<T> decisionFactory) {
		this.decisionFactory = decisionFactory;
	}

	
}
