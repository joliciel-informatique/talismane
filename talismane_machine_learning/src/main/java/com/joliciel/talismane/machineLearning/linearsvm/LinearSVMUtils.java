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

import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.utils.WeightedOutcome;

import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;

class LinearSVMUtils {
  public static List<Feature> prepareData(List<FeatureResult<?>> featureResults, TObjectIntMap<String> featureIndexMap) {
    List<Feature> featureList = new ArrayList<Feature>(featureResults.size());
    for (FeatureResult<?> featureResult : featureResults) {
      if (featureResult.getOutcome() instanceof List) {
        @SuppressWarnings("unchecked")
        FeatureResult<List<WeightedOutcome<String>>> stringCollectionResult = (FeatureResult<List<WeightedOutcome<String>>>) featureResult;
        for (WeightedOutcome<String> stringOutcome : stringCollectionResult.getOutcome()) {
          int index = featureIndexMap.get(featureResult.getTrainingName() + "|" + featureResult.getTrainingOutcome(stringOutcome.getOutcome()));
          if (index >= 0) {
            double value = stringOutcome.getWeight();
            FeatureNode featureNode = new FeatureNode(index, value);
            featureList.add(featureNode);
          }
        }
      } else {
        double value = 1.0;

        if (featureResult.getOutcome() instanceof Double) {
          @SuppressWarnings("unchecked")
          FeatureResult<Double> doubleResult = (FeatureResult<Double>) featureResult;
          value = doubleResult.getOutcome().doubleValue();
        }
        int index = featureIndexMap.get(featureResult.getTrainingName());
        if (index >= 0) {
          // we only need to bother adding features which existed in the
          // training set
          FeatureNode featureNode = new FeatureNode(index, value);
          featureList.add(featureNode);
        }
      }
    }
    return featureList;
  }
}
