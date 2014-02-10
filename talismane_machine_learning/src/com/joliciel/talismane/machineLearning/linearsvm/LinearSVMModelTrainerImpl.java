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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.ClassificationEvent;
import com.joliciel.talismane.machineLearning.ClassificationEventStream;
import com.joliciel.talismane.machineLearning.DecisionFactory;
import com.joliciel.talismane.machineLearning.MachineLearningModel;
import com.joliciel.talismane.machineLearning.Outcome;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.utils.JolicielException;
import com.joliciel.talismane.utils.PerformanceMonitor;
import com.joliciel.talismane.utils.WeightedOutcome;

import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;

class LinearSVMModelTrainerImpl<T extends Outcome> implements LinearSVMModelTrainer<T> {
	private static final Log LOG = LogFactory.getLog(LinearSVMModelTrainerImpl.class);
	private static final PerformanceMonitor MONITOR = PerformanceMonitor.getMonitor(LinearSVMModelTrainerImpl.class);
	
	private int cutoff = 1;
	private double constraintViolationCost = 1.0;
	private double epsilon = 0.01;
	private LinearSVMSolverType solverType = LinearSVMSolverType.L2R_LR;

	@Override
	public ClassificationModel<T> trainModel(
			ClassificationEventStream corpusEventStream,
			DecisionFactory<T> decisionFactory, List<String> featureDescriptors) {
		Map<String,List<String>> descriptors = new HashMap<String, List<String>>();
		descriptors.put(MachineLearningModel.FEATURE_DESCRIPTOR_KEY, featureDescriptors);
		return this.trainModel(corpusEventStream, decisionFactory, descriptors);
	}

	@Override
	public ClassificationModel<T> trainModel(
			ClassificationEventStream corpusEventStream,
			DecisionFactory<T> decisionFactory,
			Map<String, List<String>> descriptors) {
		MONITOR.startTask("trainModel");
		try {
			// Note: since we want a probabilistic classifier, our options here are limited to logistic regression:
			// L2R_LR: L2-regularized logistic regression (primal)
			// L1R_LR: L1-regularized logistic regression
			// L2R_LR_DUAL: L2-regularized logistic regression (dual)
			SolverType solver = SolverType.valueOf(this.solverType.name()); 
			if (!solver.isLogisticRegressionSolver())
				throw new JolicielException("To get a probability distribution of outcomes, only logistic regression solvers are supported.");
				
			int numEvents = 0;
			int currentOutcomeIndex = 0;
			int maxFeatureCount = 0;
			CountingInfo countingInfo = new CountingInfo();
			
			Map<String, Integer> featureIndexMap = new HashMap<String, Integer>();
			Map<Integer, Integer> featureCountMap = new HashMap<Integer, Integer>();
			Map<String, Integer> outcomeIndexMap = new HashMap<String, Integer>();
			
			List<Feature[]> fullFeatureList = new ArrayList<Feature[]>();
			List<Integer> outcomeList = new ArrayList<Integer>();
			
			while (corpusEventStream.hasNext()) {
				ClassificationEvent corpusEvent = corpusEventStream.next();
				Integer outcomeIndex = outcomeIndexMap.get(corpusEvent.getClassification());
				if (outcomeIndex==null) {
					outcomeIndex = currentOutcomeIndex++;
					outcomeIndexMap.put(corpusEvent.getClassification(), outcomeIndex);
				}
				outcomeList.add(outcomeIndex);
				Map<Integer,Feature> featureList = new TreeMap<Integer,Feature>();
				for (FeatureResult<?> featureResult : corpusEvent.getFeatureResults()) {
					if (featureResult.getOutcome() instanceof List) {
						@SuppressWarnings("unchecked")
						FeatureResult<List<WeightedOutcome<String>>> stringCollectionResult = (FeatureResult<List<WeightedOutcome<String>>>) featureResult;
						for (WeightedOutcome<String> stringOutcome : stringCollectionResult.getOutcome()) {
							String featureName = featureResult.getTrainingName()+ "|" + featureResult.getTrainingOutcome(stringOutcome.getOutcome());
							double value = stringOutcome.getWeight();
							this.addFeatureResult(featureName, value, featureList, featureIndexMap, featureCountMap, outcomeIndexMap, countingInfo);
						}
	
					} else {
						double value = 1.0;
						if (featureResult.getOutcome() instanceof Double)
						{
							@SuppressWarnings("unchecked")
							FeatureResult<Double> doubleResult = (FeatureResult<Double>) featureResult;
							value = doubleResult.getOutcome().doubleValue();
						}
						this.addFeatureResult(featureResult.getTrainingName(), value, featureList, featureIndexMap, featureCountMap, outcomeIndexMap, countingInfo);
					}
				}
				if (featureList.size()>maxFeatureCount)
					maxFeatureCount = featureList.size();
				
				// convert to array immediately, to avoid double storage
				int j = 0;
				Feature[] featureArray = new Feature[featureList.size()];
				for (Feature feature : featureList.values()) {
					featureArray[j] = feature;
					j++;
				}
				fullFeatureList.add(featureArray);
				numEvents++;
			}
			
			Problem problem = new Problem();
			
	//		problem.l = ... // number of training examples
	//		problem.n = ... // number of features
	//		problem.x = ... // feature nodes - note: must be ordered by index
	//		problem.y = ... // target values
	
			problem.l = numEvents; // number of training examples
			problem.n = countingInfo.currentFeatureIndex; // number of features
			
			Feature[][] featureMatrix = new Feature[numEvents][];
			int i = 0;
			for (Feature[] featureArray : fullFeatureList) {
				featureMatrix[i] = featureArray;
				i++;
			}
			fullFeatureList = null;
			
			LOG.debug("Event count: " + numEvents);
			LOG.debug("Feature count: " + featureIndexMap.size());
			// apply the cutoff
			if (cutoff>1) {
				LOG.debug("Feature count (after cutoff): " + countingInfo.featureCountOverCutoff);
				for (i=0; i<featureMatrix.length; i++) {
					Feature[] featureArray = featureMatrix[i];
					List<Feature> featureList = new ArrayList<Feature>(featureArray.length);
					for (int j=0; j<featureArray.length; j++) {
						Feature feature = featureArray[j];
						int featureCount = featureCountMap.get(feature.getIndex());
						if (featureCount>=cutoff)
							featureList.add(feature);
					}
					Feature[] newFeatureArray = new Feature[featureList.size()];
					int j = 0;
					for (Feature feature : featureList)
						newFeatureArray[j++] = feature;
					// try to force a garbage collect without being too explicit about it
					featureMatrix[i] = null;
					featureArray = null;
					featureMatrix[i] = newFeatureArray;
				}
			}
			
			problem.x = featureMatrix; // feature nodes
			
			double[] outcomeArray = new double[numEvents];
			i = 0;
			for (Integer outcome : outcomeList)
				outcomeArray[i++] = outcome;
			problem.y = outcomeArray;
	
			Parameter parameter = new Parameter(solver, this.constraintViolationCost, this.epsilon);
			Model model = null;
			MONITOR.startTask("train");
			try {
				model = Linear.train(problem, parameter);
			} finally {
				MONITOR.endTask("train");
			}
			
			LinearSVMModel<T> linearSVMModel = new LinearSVMModel<T>(model, descriptors, decisionFactory);
			linearSVMModel.setFeatureIndexMap(featureIndexMap);
			
			String[] outcomeArray2 = new String[outcomeIndexMap.size()];
			for (Entry<String,Integer> outcomeMapEntry : outcomeIndexMap.entrySet()) {
				outcomeArray2[outcomeMapEntry.getValue()] = outcomeMapEntry.getKey();
			}
			List<String> outcomes = new ArrayList<String>(outcomeIndexMap.size());
			for (String outcome : outcomeArray2)
				outcomes.add(outcome);
			
			linearSVMModel.setOutcomes(outcomes);
			linearSVMModel.addModelAttribute("solver", this.getSolverType().name());
			linearSVMModel.addModelAttribute("cutoff", "" + this.getCutoff());
			linearSVMModel.addModelAttribute("c", "" + this.getConstraintViolationCost());
			linearSVMModel.addModelAttribute("eps", "" + this.getEpsilon());
			
			linearSVMModel.getModelAttributes().putAll(corpusEventStream.getAttributes());
	
			return linearSVMModel;
		} finally {
			MONITOR.endTask("trainModel");
		}
	}

	void addFeatureResult(String featureName, double value,
			Map<Integer,Feature> featureList,
			Map<String, Integer> featureIndexMap,
			Map<Integer, Integer> featureCountMap,
			Map<String, Integer> outcomeIndexMap,
			CountingInfo countingInfo) {
		Integer featureIndex = featureIndexMap.get(featureName);
		if (featureIndex==null) {
			featureIndex = countingInfo.currentFeatureIndex++;
			featureIndexMap.put(featureName, featureIndex);
		}
		
		if (cutoff>1) {
			Integer featureCountObj = featureCountMap.get(featureIndex);
			int featureCount = 0;
			if (featureCountObj!=null) {
				featureCount = featureCountObj.intValue();
			}
			featureCount = featureCount + 1;
			if (featureCount==cutoff)
				countingInfo.featureCountOverCutoff++;
			featureCountMap.put(featureIndex, featureCount);
		}

		FeatureNode featureNode = new FeatureNode(featureIndex.intValue(), value);
		featureList.put(featureIndex.intValue(),featureNode);		
	}
	
	private static class CountingInfo {
		public int currentFeatureIndex = 1;
		public int featureCountOverCutoff = 0;
	}
	
	public int getCutoff() {
		return cutoff;
	}

	public void setCutoff(int cutoff) {
		this.cutoff = cutoff;
	}

	public double getConstraintViolationCost() {
		return constraintViolationCost;
	}

	public void setConstraintViolationCost(double constraintViolationCost) {
		this.constraintViolationCost = constraintViolationCost;
	}

	public double getEpsilon() {
		return epsilon;
	}

	public void setEpsilon(double epsilon) {
		this.epsilon = epsilon;
	}

	public LinearSVMSolverType getSolverType() {
		return solverType;
	}

	public void setSolverType(LinearSVMSolverType solverType) {
		this.solverType = solverType;
	}

	@Override
	public void setParameters(Map<String, Object> parameters) {
		if (parameters!=null) {
			for (String parameter : parameters.keySet()) {
				LinearSVMModelParameter modelParameter = LinearSVMModelParameter.valueOf(parameter);
				Object value = parameters.get(parameter);
				if (!modelParameter.getParameterType().isAssignableFrom(value.getClass())) {
					throw new JolicielException("Parameter of wrong type: " + parameter + ". Expected: " + modelParameter.getParameterType().getSimpleName());
				}
				switch (modelParameter) {
				case Cutoff:
					this.setCutoff((Integer)value);
					break;
				case ConstraintViolationCost:
					this.setConstraintViolationCost((Double)value);
					break;
				case Epsilon:
					this.setEpsilon((Double)value);
					break;
				case SolverType:
					this.setSolverType((LinearSVMSolverType)value);
					break;
				default:
					throw new JolicielException("Unknown parameter type: " + modelParameter);
				}
			}
		}
	}



}
