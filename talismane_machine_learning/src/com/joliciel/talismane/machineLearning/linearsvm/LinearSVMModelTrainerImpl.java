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

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.procedure.TObjectIntProcedure;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
	private boolean oneVsRest = false;
	private boolean balanceEventCounts = false;
	
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
			
			TObjectIntMap<String> featureIndexMap = new TObjectIntHashMap<String>(1000, 0.75f, -1);
			TIntIntMap featureCountMap = new TIntIntHashMap();
			TObjectIntMap<String> outcomeIndexMap = new TObjectIntHashMap<String>(100, 0.75f, -1);
			
			List<Feature[]> fullFeatureList = new ArrayList<Feature[]>();
			TIntList outcomeList = new TIntArrayList();
			
			while (corpusEventStream.hasNext()) {
				ClassificationEvent corpusEvent = corpusEventStream.next();
				int outcomeIndex = outcomeIndexMap.get(corpusEvent.getClassification());
				if (outcomeIndex<0) {
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
							this.addFeatureResult(featureName, value, featureList, featureIndexMap, featureCountMap, countingInfo);
						}
	
					} else {
						double value = 1.0;
						if (featureResult.getOutcome() instanceof Double)
						{
							@SuppressWarnings("unchecked")
							FeatureResult<Double> doubleResult = (FeatureResult<Double>) featureResult;
							value = doubleResult.getOutcome().doubleValue();
						}
						this.addFeatureResult(featureResult.getTrainingName(), value, featureList, featureIndexMap, featureCountMap, countingInfo);
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
				if (numEvents % 1000 == 0) {
					LOG.debug("Processed " + numEvents + " events.");
				}
			}
						
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
			
			final String[] outcomeArray2 = new String[outcomeIndexMap.size()];
			outcomeIndexMap.forEachEntry(new TObjectIntProcedure<String>() {
				@Override
				public boolean execute(String key, int value) {
					outcomeArray2[value] = key;
					return true;
				}
			});
			
			List<String> outcomes = new ArrayList<String>(outcomeIndexMap.size());
			for (String outcome : outcomeArray2)
				outcomes.add(outcome);

			if (oneVsRest) {
				// find outcomes representing multiple classes
				TIntSet multiClassOutcomes = new TIntHashSet();
				TIntObjectMap<TIntSet> outcomeComponentMap = new TIntObjectHashMap<TIntSet>();
				List<String> atomicOutcomes = new ArrayList<String>();
				TObjectIntMap<String> atomicOutcomeIndexes = new TObjectIntHashMap<String>();
				TIntIntMap oldIndexNewIndexMap = new TIntIntHashMap();
				
				// store all atomic outcomes in one data structures
				for (int j=0; j<outcomes.size(); j++) {
					String outcome = outcomes.get(j);
					if (outcome.indexOf('\t')<0) {
						int newIndex = atomicOutcomes.size();
						atomicOutcomeIndexes.put(outcome, newIndex);
						oldIndexNewIndexMap.put(j, newIndex);
						atomicOutcomes.add(outcome);
					}
				}
				
				// process all compound outcomes and store their components
				// when required, add their components to the atomic outcome data structures
				for (int j=0; j<outcomes.size(); j++) {
					String outcome = outcomes.get(j);
					if (outcome.indexOf('\t')>=0) {
						multiClassOutcomes.add(j);
						TIntSet myComponentOutcomes = new TIntHashSet();
						outcomeComponentMap.put(j, myComponentOutcomes);
						String[] parts = outcome.split("\t", -1);
						for (String part : parts) {
							int outcomeIndex = outcomeIndexMap.get(part);
							int newIndex = 0;
							if (outcomeIndex<0) {
								outcomeIndex = currentOutcomeIndex++;
								outcomeIndexMap.put(part, outcomeIndex);
								newIndex = atomicOutcomes.size();
								atomicOutcomeIndexes.put(part, newIndex);
								oldIndexNewIndexMap.put(outcomeIndex, newIndex);
								atomicOutcomes.add(part);
							} else {
								newIndex = oldIndexNewIndexMap.get(outcomeIndex);
							}
							myComponentOutcomes.add(newIndex);
						}
						
					}
				}
				
				LinearSVMOneVsRestModel<T> linearSVMModel = new LinearSVMOneVsRestModel<T>(descriptors, decisionFactory);
				linearSVMModel.setFeatureIndexMap(featureIndexMap);
				
				linearSVMModel.setOutcomes(atomicOutcomes);
				linearSVMModel.addModelAttribute("solver", this.getSolverType().name());
				linearSVMModel.addModelAttribute("cutoff", "" + this.getCutoff());
				linearSVMModel.addModelAttribute("c", "" + this.getConstraintViolationCost());
				linearSVMModel.addModelAttribute("eps", "" + this.getEpsilon());
				linearSVMModel.addModelAttribute("oneVsRest", "" + this.isOneVsRest());
				
				linearSVMModel.getModelAttributes().putAll(corpusEventStream.getAttributes());
				// build one 1-vs-All model per outcome
				for (int j=0; j<atomicOutcomes.size(); j++) {
					String outcome = atomicOutcomes.get(j);
					LOG.info("Building model for outcome: " + outcome);
					
					// create an outcome array with 1 for the current outcome and 0 for all others
					double[] outcomeArray = new double[numEvents];
					i = 0;
					TIntIterator outcomeIterator = outcomeList.iterator();
					int myOutcomeCount = 0;
					while (outcomeIterator.hasNext()) {
						boolean isMyOutcome = false;
						int originalOutcomeIndex = outcomeIterator.next();
						if (multiClassOutcomes.contains(originalOutcomeIndex)) {
							if (outcomeComponentMap.get(originalOutcomeIndex).contains(j))
								isMyOutcome = true;
						} else {
							if (oldIndexNewIndexMap.get(originalOutcomeIndex)==j)
								isMyOutcome = true;
						}
						int myOutcome = (isMyOutcome ? 1 : 0);
						if (myOutcome==1) myOutcomeCount++;
						outcomeArray[i++] = myOutcome;
					}
					
					LOG.debug("Found " + myOutcomeCount + " out of " + numEvents + " outcomes of type: " + outcome);
					
					double[] myOutcomeArray = outcomeArray;
					Feature[][] myFeatureMatrix = featureMatrix;
					if (balanceEventCounts) {
						// we start with the truncated proportion of false events to true events
						// we want these approximately balanced
						// we only balance up, never balance down
						int otherCount = numEvents - myOutcomeCount;
						int proportion = otherCount / myOutcomeCount;
						if (proportion > 1) {
							LOG.debug("Balancing events for " + outcome + " by " + proportion);
							int newSize = otherCount + myOutcomeCount * proportion;
							myOutcomeArray = new double[newSize];
							myFeatureMatrix = new Feature[newSize][];
							int l=0;
							for (int k=0; k<outcomeArray.length; k++) {
								double myOutcome = outcomeArray[k];
								Feature[] myFeatures = featureMatrix[k];
								if (myOutcome==0) {
									myOutcomeArray[l] = myOutcome;
									myFeatureMatrix[l] = myFeatures;
									l++;
								} else {
									for (int m=0; m<proportion; m++) {
										myOutcomeArray[l] = myOutcome;
										myFeatureMatrix[l] = myFeatures;
										l++;
									}
								} // is it the right outcome or not?
							} // next outcome in original array
						} // requires balancing?
					} // balance event counts?
					
					Problem problem = new Problem();
					
					//		problem.l = ... // number of training examples
					//		problem.n = ... // number of features
					//		problem.x = ... // feature nodes - note: must be ordered by index
					//		problem.y = ... // target values
					
					problem.l = numEvents; // number of training examples
					problem.n = countingInfo.currentFeatureIndex; // number of features
					problem.x = myFeatureMatrix; // feature nodes - note: must be ordered by index
					problem.y = myOutcomeArray; // target values
					
					Parameter parameter = new Parameter(solver, this.constraintViolationCost, this.epsilon);
					Model model = null;
					MONITOR.startTask("train");
					try {
						model = Linear.train(problem, parameter);
					} finally {
						MONITOR.endTask();
					}
					
					linearSVMModel.addModel(model);
				}
				
				return linearSVMModel;
			} else {
				double[] outcomeArray = new double[numEvents];
				i = 0;
				TIntIterator outcomeIterator = outcomeList.iterator();
				while (outcomeIterator.hasNext())
					outcomeArray[i++] = outcomeIterator.next();
	
				Problem problem = new Problem();
				
				//		problem.l = ... // number of training examples
				//		problem.n = ... // number of features
				//		problem.x = ... // feature nodes - note: must be ordered by index
				//		problem.y = ... // target values
				
				problem.l = numEvents; // number of training examples
				problem.n = countingInfo.currentFeatureIndex; // number of features
				problem.x = featureMatrix; // feature nodes - note: must be ordered by index
				problem.y = outcomeArray; // target values
		
				Parameter parameter = new Parameter(solver, this.constraintViolationCost, this.epsilon);
				Model model = null;
				MONITOR.startTask("train");
				try {
					model = Linear.train(problem, parameter);
				} finally {
					MONITOR.endTask();
				}
				
				LinearSVMModel<T> linearSVMModel = new LinearSVMModel<T>(model, descriptors, decisionFactory);
				linearSVMModel.setFeatureIndexMap(featureIndexMap);
								
				linearSVMModel.setOutcomes(outcomes);
				linearSVMModel.addModelAttribute("solver", this.getSolverType().name());
				linearSVMModel.addModelAttribute("cutoff", "" + this.getCutoff());
				linearSVMModel.addModelAttribute("c", "" + this.getConstraintViolationCost());
				linearSVMModel.addModelAttribute("eps", "" + this.getEpsilon());
				linearSVMModel.addModelAttribute("oneVsRest", "" + this.isOneVsRest());
				
				linearSVMModel.getModelAttributes().putAll(corpusEventStream.getAttributes());
		
				return linearSVMModel;
			}
		} finally {
			MONITOR.endTask();
		}
	}

	void addFeatureResult(String featureName, double value,
			Map<Integer,Feature> featureList,
			TObjectIntMap<String> featureIndexMap,
			TIntIntMap featureCountMap,
			CountingInfo countingInfo) {
		
		int featureIndex = featureIndexMap.get(featureName);
		if (featureIndex<0) {
			featureIndex = countingInfo.currentFeatureIndex++;
			featureIndexMap.put(featureName, featureIndex);
		}
		
		if (cutoff>1) {
			int featureCount = featureCountMap.get(featureIndex)+1;
			if (featureCount==cutoff)
				countingInfo.featureCountOverCutoff++;
			featureCountMap.put(featureIndex, featureCount);
		}

		FeatureNode featureNode = new FeatureNode(featureIndex, value);
		featureList.put(featureIndex,featureNode);		
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
	public boolean isOneVsRest() {
		return oneVsRest;
	}

	@Override
	public void setOneVsRest(boolean oneVsRest) {
		this.oneVsRest = oneVsRest;
	}
	
	@Override
	public boolean isBalanceEventCounts() {
		return balanceEventCounts;
	}

	@Override
	public void setBalanceEventCounts(boolean balanceEventCounts) {
		this.balanceEventCounts = balanceEventCounts;
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
				case OneVsRest:
					this.setOneVsRest((Boolean) value);
					break;
				case BalanceEventCounts:
					this.setBalanceEventCounts((Boolean) value);
					break;
				default:
					throw new JolicielException("Unknown parameter type: " + modelParameter);
				}
			}
		}
	}



}
