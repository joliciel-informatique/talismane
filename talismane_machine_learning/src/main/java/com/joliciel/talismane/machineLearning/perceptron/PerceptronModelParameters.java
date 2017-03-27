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

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.procedure.TObjectIntProcedure;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.utils.WeightedOutcome;

/**
 * Perceptron classification model parameters. Weights are a matrix of feature x
 * label.
 * 
 * @author Assaf Urieli
 *
 */
class PerceptronModelParameters implements Serializable {
  private static final long serialVersionUID = 1L;

  private List<String> outcomes = new ArrayList<String>();
  private int featureCount = 0;
  private int outcomeCount = 0;

  private TObjectIntMap<String> outcomeIndexes = new TObjectIntHashMap<String>(10, 0.7f, -1);
  private TObjectIntMap<String> featureIndexes = new TObjectIntHashMap<String>(1000, 0.7f, -1);
  private double[][] featureWeights;
  private int[] featureCounts;

  public PerceptronModelParameters() {
  }

  public PerceptronModelParameters clone() {
    PerceptronModelParameters params = new PerceptronModelParameters(this);
    return params;
  }

  private PerceptronModelParameters(PerceptronModelParameters params) {
    // need to perform deep clone for feature weights
    this.featureWeights = new double[params.getFeatureWeights().length][];
    for (int i = 0; i < params.getFeatureWeights().length; i++) {
      this.featureWeights[i] = params.getFeatureWeights()[i].clone();
    }
    // all the rest can be a shallow copy since it won't change
    this.outcomeIndexes = params.getOutcomeIndexes();
    this.featureIndexes = params.getFeatureIndexes();
    this.featureCounts = params.getFeatureCounts();
    this.outcomes = params.getOutcomes();
    this.featureCount = params.getFeatureCount();
    this.outcomeCount = params.getOutcomeCount();
  }

  public int[] initialise(PerceptronModelParameters oldParams, int cutoff) {
    int[] newIndexes = new int[oldParams.featureCount];
    this.outcomes = oldParams.outcomes;
    this.outcomeIndexes = oldParams.outcomeIndexes;
    this.outcomeCount = oldParams.outcomeCount;
    final String[] featureNames = new String[oldParams.featureCount];
    oldParams.featureIndexes.forEachEntry(new TObjectIntProcedure<String>() {

      @Override
      public boolean execute(String key, int value) {
        featureNames[value] = key;
        return true;
      }
    });
    int i = 0;
    for (int count : oldParams.featureCounts) {
      if (count >= cutoff) {
        int newIndex = this.getOrCreateFeatureIndex(featureNames[i]);
        newIndexes[i] = newIndex;
      } else {
        newIndexes[i] = -1;
      }
      i++;
    }

    return newIndexes;
  }

  public int getOutcomeIndex(String outcome) {
    return outcomeIndexes.get(outcome);
  }

  public int getOrCreateOutcomeIndex(String outcome) {
    int outcomeIndex = outcomeIndexes.get(outcome);
    if (outcomeIndex < 0) {
      outcomeIndex = outcomeCount++;
      outcomeIndexes.put(outcome, outcomeIndex);
      outcomes.add(outcome);
    }
    return outcomeIndex;
  }

  public int getFeatureIndex(String featureName) {
    return featureIndexes.get(featureName);
  }

  public int getOrCreateFeatureIndex(String featureName) {
    int featureIndex = featureIndexes.get(featureName);
    if (featureIndex < 0) {
      featureIndex = featureCount++;
      featureIndexes.put(featureName, featureIndex);
    }
    return featureIndex;
  }

  public void initialiseCounts() {
    featureCounts = new int[featureCount];
  }

  public void initialiseWeights() {
    featureWeights = new double[featureCount][outcomeCount];
  }

  public double[][] getFeatureWeights() {
    return featureWeights;
  }

  public int[] getFeatureCounts() {
    return featureCounts;
  }

  public int getFeatureCount() {
    return featureCount;
  }

  public int getOutcomeCount() {
    return outcomeCount;
  }

  public List<String> getOutcomes() {
    return outcomes;
  }

  public TObjectIntMap<String> getOutcomeIndexes() {
    return outcomeIndexes;
  }

  public TObjectIntMap<String> getFeatureIndexes() {
    return featureIndexes;
  }

  /**
   * Prepare the feature index list and weight list, based on the feature
   * results provided. If a feature is not in the model, leave it out.
   * 
   * @param featureResults
   *          the results to analyse
   * @param featureIndexList
   *          the list of feature indexes to populate
   * @param featureValueList
   *          the list of feature values to populate
   */
  public void prepareData(List<FeatureResult<?>> featureResults, List<Integer> featureIndexList, List<Double> featureValueList) {
    this.prepareData(featureResults, featureIndexList, featureValueList, false);
  }

  /**
   * Prepare the feature index list and weight list, based on the feature
   * results provided.
   * 
   * @param create
   *          If true and a feature is not in the model, create it. Otherwise
   *          leave it out.
   */
  public void prepareData(List<FeatureResult<?>> featureResults, List<Integer> featureIndexList, List<Double> featureValueList, boolean create) {
    for (FeatureResult<?> featureResult : featureResults) {
      if (featureResult != null) {
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
            if (featureIndex >= 0) {
              featureIndexList.add(featureIndex);
              featureValueList.add(stringOutcome.getWeight());
            }
          }
        } else {
          double value = 1;
          if (featureResult.getOutcome() instanceof Double) {
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
          if (featureIndex >= 0) {
            featureIndexList.add(featureIndex);
            featureValueList.add(value);
          }

        }
      }
    }
  }

}
