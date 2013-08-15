///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2013 Assaf Urieli
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
package com.joliciel.talismane.machineLearning.perceptron;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.DecisionFactory;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.Outcome;
import com.joliciel.talismane.machineLearning.features.FeatureResult;

class PerceptronDecisionMaker<T extends Outcome> implements DecisionMaker<T> {
	private DecisionFactory<T> decisionFactory;
	private PerceptronModelParameters modelParameters;
	
	public PerceptronDecisionMaker(PerceptronModelParameters params, DecisionFactory<T> decisionFactory) {
		super();
		this.modelParameters = params;
		this.decisionFactory = decisionFactory;
	}

	@Override
	public List<Decision<T>> decide(List<FeatureResult<?>> featureResults) {
		List<Integer> featureIndexList = new ArrayList<Integer>();
		List<Double> featureValueList = new ArrayList<Double>();
		modelParameters.prepareData(featureResults, featureIndexList, featureValueList);
		
		double[] results = this.predict(featureIndexList, featureValueList);
		
		int i = 0;
		TreeSet<Decision<T>> outcomeSet = new TreeSet<Decision<T>>();
		for (String outcome : modelParameters.getOutcomes()) {
			Decision<T> decision = this.decisionFactory.createDecision(outcome, results[i++]);
			outcomeSet.add(decision);
		}

		List<Decision<T>> decisions = new ArrayList<Decision<T>>(outcomeSet);

		return decisions;

	}

	public double[] predict(List<Integer> featureIndexList, List<Double> featureValueList) {
		return this.predict(featureIndexList, featureValueList, true);
	}
	
	public double[] predict(List<Integer> featureIndexList, List<Double> featureValueList, boolean normalise) {
		double[] results = new double[modelParameters.getOutcomeCount()];
		for (int i=0; i<featureIndexList.size();i++) {
			int featureIndex = featureIndexList.get(i);
			double value = featureValueList.get(i);
			
			for (int j=0;j<results.length;j++) {
				double[] classWeights = modelParameters.getFeatureWeights()[featureIndex];
				double weight = classWeights[j];
				results[j] += value * weight;
			}			
		}

		if (normalise) {
			//e^(x/absmax)/sum(e^(x/absmax))
			// where x/absmax is in [-1,1]
			// e^(x/absmax) is in [1/e,e]
			
			double absoluteMax = 1;

			for (int i=0;i<results.length;i++) {
				if (Math.abs(results[i]) > absoluteMax)
					absoluteMax = Math.abs(results[i]);
			}

			double total = 0.0;
			for (int i=0;i<results.length;i++) {
				results[i] = Math.exp(results[i]/absoluteMax);
				total += results[i];
			}

			for (int i=0;i<results.length;i++) {
				results[i] /= total;
			}
		}
		return results;
	}
	
	@Override
	public DecisionFactory<T> getDecisionFactory() {
		return this.decisionFactory;
	}

	@Override
	public void setDecisionFactory(DecisionFactory<T> decisionFactory) {
		this.decisionFactory = decisionFactory;
	}

	public PerceptronModelParameters getModelParameters() {
		return modelParameters;
	}

	
}
