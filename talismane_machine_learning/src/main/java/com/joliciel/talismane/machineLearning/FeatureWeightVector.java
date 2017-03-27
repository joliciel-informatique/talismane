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
package com.joliciel.talismane.machineLearning;

import java.util.List;

import com.joliciel.talismane.machineLearning.features.FeatureResult;

/**
 * In global learning systems, stores the weight associated with each feature
 * @author Assaf Urieli
 */
public interface FeatureWeightVector {
  /**
   * Get the weight associated with a particular feature result.
   */
  double getWeight(FeatureResult<?> featureResult);
  
  /**
   * Add the feature vector provided to the current weight vector.
   */
  void addVector(List<FeatureResult<?>> featureResults);
  
  /**
   * Subtract the feature vector provided from the current weight vector.
   */
  void subtractVector(List<FeatureResult<?>> featureResults);
}
