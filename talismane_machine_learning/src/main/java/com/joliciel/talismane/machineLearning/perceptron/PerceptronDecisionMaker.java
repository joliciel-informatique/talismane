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
package com.joliciel.talismane.machineLearning.perceptron;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import com.joliciel.talismane.machineLearning.AdditiveScoringStrategy;
import com.joliciel.talismane.machineLearning.ClassificationSolution;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.GeometricMeanScoringStrategy;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.machineLearning.ScoringStrategy;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.perceptron.PerceptronService.PerceptronScoring;
import com.joliciel.talismane.utils.JolicielException;

class PerceptronDecisionMaker implements DecisionMaker {
	private PerceptronModelParameters modelParameters;
	private transient ScoringStrategy<ClassificationSolution> scoringStrategy = null;
	private transient PerceptronScoring perceptronScoring = null;

	private MachineLearningService machineLearningService;

	public PerceptronDecisionMaker(PerceptronModelParameters params, PerceptronScoring perceptronScoring) {
		super();
		this.modelParameters = params;
		this.perceptronScoring = perceptronScoring;
	}

	@Override
	public List<Decision> decide(List<FeatureResult<?>> featureResults) {
		List<Integer> featureIndexList = new ArrayList<Integer>();
		List<Double> featureValueList = new ArrayList<Double>();
		modelParameters.prepareData(featureResults, featureIndexList, featureValueList);

		double[] results = this.predict(featureIndexList, featureValueList);
		double[] probs = new double[results.length];

		if (this.getPerceptronScoring() == PerceptronScoring.normalisedExponential) {
			// e^(x/absmax)/sum(e^(x/absmax))
			// where x/absmax is in [-1,1]
			// e^(x/absmax) is in [1/e,e]
			double absoluteMax = 1;
			for (int i = 0; i < results.length; i++) {
				if (Math.abs(results[i]) > absoluteMax)
					absoluteMax = Math.abs(results[i]);
			}

			double total = 0.0;
			for (int i = 0; i < results.length; i++) {
				probs[i] = Math.exp(results[i] / absoluteMax);
				total += probs[i];
			}

			for (int i = 0; i < probs.length; i++) {
				probs[i] /= total;
			}
		} else {
			// make all results >= 1
			double min = Double.MAX_VALUE;
			for (int i = 0; i < results.length; i++) {
				if (results[i] < min)
					min = results[i];
			}

			if (min < 0) {
				for (int i = 0; i < results.length; i++) {
					probs[i] = (results[i] - min) + 1;
				}
			}

			// then divide by total to get a probability distribution
			double total = 0.0;
			for (int i = 0; i < probs.length; i++) {
				total += probs[i];
			}

			for (int i = 0; i < probs.length; i++) {
				probs[i] /= total;
			}
		}

		int i = 0;
		TreeSet<Decision> outcomeSet = new TreeSet<Decision>();
		for (String outcome : modelParameters.getOutcomes()) {
			Decision decision = this.machineLearningService.createDecision(outcome, results[i], probs[i]);
			outcomeSet.add(decision);
			i++;
		}

		List<Decision> decisions = new ArrayList<Decision>(outcomeSet);

		return decisions;

	}

	public double[] predict(List<Integer> featureIndexList, List<Double> featureValueList) {
		double[] results = new double[modelParameters.getOutcomeCount()];
		for (int i = 0; i < featureIndexList.size(); i++) {
			int featureIndex = featureIndexList.get(i);
			double value = featureValueList.get(i);

			for (int j = 0; j < results.length; j++) {
				double[] classWeights = modelParameters.getFeatureWeights()[featureIndex];
				double weight = classWeights[j];
				results[j] += value * weight;
			}
		}

		return results;
	}

	public PerceptronModelParameters getModelParameters() {
		return modelParameters;
	}

	@Override
	public ScoringStrategy<ClassificationSolution> getDefaultScoringStrategy() {
		if (scoringStrategy == null) {
			if (this.getPerceptronScoring() == PerceptronScoring.normalisedLinear) {
				scoringStrategy = new GeometricMeanScoringStrategy();
			} else if (this.getPerceptronScoring() == PerceptronScoring.normalisedExponential) {
				scoringStrategy = new GeometricMeanScoringStrategy();
			} else if (this.getPerceptronScoring() == PerceptronScoring.additive) {
				scoringStrategy = new AdditiveScoringStrategy();
			} else {
				throw new JolicielException("Unknown perceptron scoring strategy: " + this.getPerceptronScoring());
			}
		}
		return scoringStrategy;
	}

	public PerceptronScoring getPerceptronScoring() {
		return perceptronScoring;
	}

	public MachineLearningService getMachineLearningService() {
		return machineLearningService;
	}

	public void setMachineLearningService(MachineLearningService machineLearningService) {
		this.machineLearningService = machineLearningService;
	}

}
