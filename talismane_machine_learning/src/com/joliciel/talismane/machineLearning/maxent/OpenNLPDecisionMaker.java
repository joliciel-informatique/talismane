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
package com.joliciel.talismane.machineLearning.maxent;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.DecisionFactory;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.Outcome;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.utils.WeightedOutcome;

import opennlp.model.MaxentModel;

class OpenNLPDecisionMaker<T extends Outcome> implements DecisionMaker<T> {
	private MaxentModel model;
	private DecisionFactory<T> decisionFactory;
	
	public OpenNLPDecisionMaker(MaxentModel model) {
		super();
		this.model = model;
	}
	
	@Override
	public List<Decision<T>> decide(List<FeatureResult<?>> featureResults) {
		List<String> contextList = new ArrayList<String>();
		List<Float> weightList = new ArrayList<Float>();
		OpenNLPDecisionMaker.prepareData(featureResults, contextList, weightList);
		
		String[] contexts = new String[contextList.size()];
		float[] weights = new float[weightList.size()];
		
		int i = 0;
		for (String context : contextList) {
			contexts[i++] = context;
		}
		i = 0;
		for (Float weight : weightList) {
			weights[i++]  = weight;
		}
		
		double[] probs = model.eval(contexts, weights);
		
		
		String[] outcomes = new String[probs.length];
		for (i=0;i<probs.length;i++)
			outcomes[i]=model.getOutcome(i);
		
		TreeSet<Decision<T>> outcomeSet = new TreeSet<Decision<T>>();
		for (i = 0; i<probs.length; i++) {
			Decision<T> decision = decisionFactory.createDecision(outcomes[i], probs[i]);
			outcomeSet.add(decision);
		}
		
		List<Decision<T>> decisions = new ArrayList<Decision<T>>(outcomeSet);

		return decisions;
	}

	static void prepareData(List<FeatureResult<?>> featureResults, List<String> contextList, List<Float> weightList) {
		for (FeatureResult<?> featureResult : featureResults) {
			if (featureResult!=null) {
				if (featureResult.getOutcome() instanceof List) {
					@SuppressWarnings("unchecked")
					FeatureResult<List<WeightedOutcome<String>>> stringCollectionResult = (FeatureResult<List<WeightedOutcome<String>>>) featureResult;
					for (WeightedOutcome<String> stringOutcome : stringCollectionResult.getOutcome()) {
						contextList.add(featureResult.getTrainingName() + "|" + featureResult.getTrainingOutcome(stringOutcome.getOutcome()));
						weightList.add(((Double)stringOutcome.getWeight()).floatValue());
					}
				} else {
					float weight = 1;
					if (featureResult.getOutcome() instanceof Double)
					{
						@SuppressWarnings("unchecked")
						FeatureResult<Double> doubleResult = (FeatureResult<Double>) featureResult;
						weight = doubleResult.getOutcome().floatValue();
					}
					contextList.add(featureResult.getTrainingName());
					weightList.add(weight);
				}
			}
		}
	}
	
	@Override
	public DecisionFactory<T> getDecisionFactory() {
		return this.decisionFactory;
	}

	@Override
	public void setDecisionFactory(DecisionFactory<T> decisionFactory) {
		this.decisionFactory = decisionFactory;
	}
}
