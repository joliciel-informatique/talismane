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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.machineLearning.ClassificationSolution;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.DecisionFactory;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.GeometricMeanScoringStrategy;
import com.joliciel.talismane.machineLearning.Outcome;
import com.joliciel.talismane.machineLearning.ScoringStrategy;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;

class LinearSVMOneVsRestDecisionMaker<T extends Outcome> implements DecisionMaker<T> {
	private static final Log LOG = LogFactory.getLog(LinearSVMOneVsRestDecisionMaker.class);
	private DecisionFactory<T> decisionFactory;
	private List<Model> models;
	TObjectIntMap<String> featureIndexMap = null;
	List<String> outcomes = null;
	private transient ScoringStrategy<ClassificationSolution<T>> scoringStrategy = null;
	
	public LinearSVMOneVsRestDecisionMaker(List<Model> models,
			TObjectIntMap<String> featureIndexMap,
			List<String> outcomes) {
		super();
		this.models = models;
		this.featureIndexMap = featureIndexMap;
		this.outcomes = outcomes;
	}

	@Override
	public List<Decision<T>> decide(List<FeatureResult<?>> featureResults) {
		List<Feature> featureList = LinearSVMUtils.prepareData(featureResults, featureIndexMap);
		
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
			
			TreeSet<Decision<T>> outcomeSet = new TreeSet<Decision<T>>();
			
			for (Model model : models) {
				double[] probabilities = new double[2];
				Linear.predictProbability(model, instance, probabilities);
				
				Decision<T> decision = decisionFactory.createDecision(outcomes.get(0), probabilities[0]);
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

	@Override
	public ScoringStrategy<ClassificationSolution<T>> getDefaultScoringStrategy() {
		if (scoringStrategy==null)
			scoringStrategy = new GeometricMeanScoringStrategy<T>();
		return scoringStrategy;
	}
}
