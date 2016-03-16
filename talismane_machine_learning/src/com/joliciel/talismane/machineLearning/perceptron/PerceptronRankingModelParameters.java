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

import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.procedure.TIntDoubleProcedure;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.machineLearning.FeatureWeightVector;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.utils.WeightedOutcome;

/**
 * Store parameters for perceptron ranking models.
 * Weights are a vector of length |features|.
 * @author Assaf Urieli
 *
 */
class PerceptronRankingModelParameters implements Serializable, FeatureWeightVector {
	private static final Log LOG = LogFactory.getLog(PerceptronRankingModelParameters.class);
	private static final long serialVersionUID = 1L;
	private int featureCount = 0;
	
	private TObjectIntMap<String> featureIndexes = new TObjectIntHashMap<String>(1000, 0.7f, -1);
	private double[] weightVector;
	private transient TIntDoubleMap featureWeights = null;
	private transient TIntObjectMap<String> reverseLookup = new TIntObjectHashMap<String>(1000, 0.7f);
	
	public PerceptronRankingModelParameters() { }
	
	public int getFeatureIndex(String featureName) {
		return featureIndexes.get(featureName);
	}
	
	public int getOrCreateFeatureIndex(String featureName) {
		int featureIndex = featureIndexes.get(featureName);
		if (featureIndex<0) {
			featureIndex = featureCount++;
			featureIndexes.put(featureName, featureIndex);
			if (LOG.isTraceEnabled())
				reverseLookup.put(featureIndex, featureName);
		}
		return featureIndex;
	}
	
	public void initialiseVector() {
		featureWeights = new TIntDoubleHashMap(1000, 0.7f, -1, 0.0);
	}
	
	public void finaliseVector() {
		final double[] weightVector = new double[featureCount];
		featureWeights.forEachEntry(
			new TIntDoubleProcedure() {	
				@Override
				public boolean execute(int key, double value) {
					weightVector[key] = value;
					return true;
				}
			}
		);
		this.weightVector = weightVector;
		this.featureWeights = null;
	}

	public int getFeatureCount() {
		return featureCount;
	}

	/**
	 * Prepare the feature index list and weight list, based on the feature results provided.
	 * If a feature is not in the model, leave it out.
	 * @param featureResults the results to analyse
	 * @param featureIndexList the list of feature indexes to populate
	 * @param featureValueList the list of feature values to populate
	 */
	public void prepareData(List<FeatureResult<?>> featureResults, List<Integer> featureIndexList, List<Double> featureValueList) {
		this.prepareData(featureResults, featureIndexList, featureValueList, false);
	}
	
	/**
	 * Prepare the feature index list and weight list, based on the feature results provided.
	 * @param create If true and a feature is not in the model, create it. Otherwise leave it out.
	 */
	public void prepareData(List<FeatureResult<?>> featureResults, List<Integer> featureIndexList, List<Double> featureValueList, boolean create) {
		for (FeatureResult<?> featureResult : featureResults) {
			if (featureResult!=null) {
				if (featureResult.getOutcome() instanceof List) {
					@SuppressWarnings("unchecked")
					FeatureResult<List<WeightedOutcome<String>>> stringCollectionResult = (FeatureResult<List<WeightedOutcome<String>>>) featureResult;
					for (WeightedOutcome<String> stringOutcome : stringCollectionResult.getOutcome()) {
						String featureName = featureResult.getTrainingName() + "|" + featureResult.getTrainingOutcome(stringOutcome.getOutcome());
						int featureIndex = -1;
						if (create) {
							featureIndex = this.getOrCreateFeatureIndex(featureName);
						} else {
							featureIndex = this.getFeatureIndex(featureName);
						}
						if (featureIndex>=0) {
							featureIndexList.add(featureIndex);
							featureValueList.add(stringOutcome.getWeight());
						}
					}
				} else {
					double value = 1;
					if (featureResult.getOutcome() instanceof Double)
					{
						@SuppressWarnings("unchecked")
						FeatureResult<Double> doubleResult = (FeatureResult<Double>) featureResult;
						value = doubleResult.getOutcome();
					}
					String featureName = featureResult.getTrainingName();
					int featureIndex = -1;
					if (create) {
						featureIndex = this.getOrCreateFeatureIndex(featureName);
					} else {
						featureIndex = this.getFeatureIndex(featureName);
					}
					if (featureIndex>=0) {
						featureIndexList.add(featureIndex);
						featureValueList.add(value);
					}
				}
			}
		}
	}


	@Override
	public double getWeight(FeatureResult<?> featureResult) {
		double totalWeight = 0.0;
		List<Integer> featureIndexes = new ArrayList<Integer>();
		List<Double> featureValues = new ArrayList<Double>();
		List<FeatureResult<?>> featureResults = new ArrayList<FeatureResult<?>>(1);
		featureResults.add(featureResult);
		this.prepareData(featureResults, featureIndexes, featureValues);
		for (int i=0; i<featureIndexes.size(); i++) {
			if (weightVector!=null)
				totalWeight += weightVector[featureIndexes.get(i)] * featureValues.get(i);
			else
				totalWeight += featureWeights.get(featureIndexes.get(i)) * featureValues.get(i);
		}
		return totalWeight;
	}


	@Override
	public void addVector(List<FeatureResult<?>> featureResults) {
		if (LOG.isTraceEnabled())
			LOG.trace("Adding vector");

		List<Integer> featureIndexes = new ArrayList<Integer>();
		List<Double> featureValues = new ArrayList<Double>();
		this.prepareData(featureResults, featureIndexes, featureValues, true);
		for (int i=0; i<featureIndexes.size(); i++) {
			featureWeights.put(featureIndexes.get(i), featureWeights.get(featureIndexes.get(i)) + featureValues.get(i));
			if (LOG.isTraceEnabled()) {
				LOG.trace(reverseLookup.get(featureIndexes.get(i)) + ": " + featureWeights.get(featureIndexes.get(i)));
			}
		}
	}


	@Override
	public void subtractVector(List<FeatureResult<?>> featureResults) {
		if (LOG.isTraceEnabled())
			LOG.trace("Subtracting vector");

		List<Integer> featureIndexes = new ArrayList<Integer>();
		List<Double> featureValues = new ArrayList<Double>();
		this.prepareData(featureResults, featureIndexes, featureValues, true);
		for (int i=0; i<featureIndexes.size(); i++) {
			featureWeights.put(featureIndexes.get(i), featureWeights.get(featureIndexes.get(i)) - featureValues.get(i));
			if (LOG.isTraceEnabled()) {
				LOG.trace(reverseLookup.get(featureIndexes.get(i)) + ": " + featureWeights.get(featureIndexes.get(i)));
			}
		}
	}

	public TIntDoubleMap getFeatureWeights() {
		return featureWeights;
	}
	
}
