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
package com.joliciel.talismane.utils.maxent;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import com.joliciel.talismane.utils.DecisionMaker;
import com.joliciel.talismane.utils.features.FeatureResult;
import com.joliciel.talismane.utils.util.WeightedOutcome;

import opennlp.model.MaxentModel;

public class MaxentDecisionMaker implements DecisionMaker {
	private MaxentModel model;
	
	public MaxentDecisionMaker(MaxentModel model) {
		super();
		this.model = model;
	}

	@Override
	public List<WeightedOutcome<String>> decide(List<FeatureResult<?>> featureResults) {
		List<String> contextList = new ArrayList<String>();
		List<Float> weightList = new ArrayList<Float>();
		for (FeatureResult<?> featureResult : featureResults) {
			if (featureResult!=null) {
				float weight = 1;
				if (featureResult.getOutcome() instanceof Double)
				{
					@SuppressWarnings("unchecked")
					FeatureResult<Double> doubleResult = (FeatureResult<Double>) featureResult;
					weight = doubleResult.getOutcome().floatValue();
				}
				contextList.add(featureResult.getName());
				weightList.add(weight);
			}
		}
		
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
		
		TreeSet<WeightedOutcome<String>> outcomeSet = new TreeSet<WeightedOutcome<String>>();
		for (i = 0; i<probs.length; i++) {
			WeightedOutcome<String> weightedOutcome = new WeightedOutcome<String>(outcomes[i], probs[i]);
			outcomeSet.add(weightedOutcome);
		}
		
		List<WeightedOutcome<String>> weightedOutcomes = new ArrayList<WeightedOutcome<String>>(outcomeSet);
		
		
		return weightedOutcomes;
	}
}
