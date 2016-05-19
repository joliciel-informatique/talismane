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
package com.joliciel.talismane.machineLearning.linearsvm;

import gnu.trove.map.TObjectIntMap;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.machineLearning.ClassificationSolution;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.GeometricMeanScoringStrategy;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.machineLearning.ScoringStrategy;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;

class LinearSVMDecisionMaker implements DecisionMaker {
	private static final Logger LOG = LoggerFactory.getLogger(LinearSVMDecisionMaker.class);
	
	private MachineLearningService machineLearningService;
	
	private Model model;
	TObjectIntMap<String> featureIndexMap = null;
	List<String> outcomes = null;
	private transient ScoringStrategy<ClassificationSolution> scoringStrategy = null;
	
	public LinearSVMDecisionMaker(Model model,
			TObjectIntMap<String> featureIndexMap,
			List<String> outcomes) {
		super();
		this.model = model;
		this.featureIndexMap = featureIndexMap;
		this.outcomes = outcomes;
	}

	@Override
	public List<Decision> decide(List<FeatureResult<?>> featureResults) {
		List<Feature> featureList = LinearSVMUtils.prepareData(featureResults, featureIndexMap);
		
		List<Decision> decisions = null;

		if (featureList.size()==0) {
			LOG.info("No features for current context.");
			TreeSet<Decision> outcomeSet = new TreeSet<Decision>();
			double uniformProb = 1 / outcomes.size();
			for (String outcome : outcomes) {
				Decision decision = machineLearningService.createDecision(outcome, uniformProb);
				outcomeSet.add(decision);
			}
			decisions = new ArrayList<Decision>(outcomeSet);
		} else {
			Feature[] instance = new Feature[1];
			instance = featureList.toArray(instance);
			
			double[] probabilities = new double[model.getLabels().length];
			Linear.predictProbability(model, instance, probabilities);
	
			TreeSet<Decision> outcomeSet = new TreeSet<Decision>();
			for (int i = 0; i<model.getLabels().length; i++) {
				Decision decision = machineLearningService.createDecision(outcomes.get(i), probabilities[i]);
				outcomeSet.add(decision);
			}
			decisions = new ArrayList<Decision>(outcomeSet);
		}

		return decisions;

	}


	@Override
	public ScoringStrategy<ClassificationSolution> getDefaultScoringStrategy() {
		if (scoringStrategy==null)
			scoringStrategy = new GeometricMeanScoringStrategy();
		return scoringStrategy;
	}

	public MachineLearningService getMachineLearningService() {
		return machineLearningService;
	}

	public void setMachineLearningService(
			MachineLearningService machineLearningService) {
		this.machineLearningService = machineLearningService;
	}
	
	
}
