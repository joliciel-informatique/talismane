///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2014 Joliciel Informatique
//
//This file is part of Jochre.
//
//Jochre is free software: you can redistribute it and/or modify
//it under the terms of the GNU Affero General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//Jochre is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Affero General Public License for more details.
//
//You should have received a copy of the GNU Affero General Public License
//along with Jochre.  If not, see <http://www.gnu.org/licenses/>.
//////////////////////////////////////////////////////////////////////////////
package com.joliciel.talismane.machineLearning.features;

import com.joliciel.talismane.TalismaneException;

/**
 * Takes a feature with a value from 0 to 1, and converts it to a graduated
 * value of 0, 1/n, 2/n, 3/n, ..., 1
 * 
 * @author Assaf Urieli
 *
 */
public class GraduateFeature<T> extends AbstractCachableFeature<T, Double>implements DoubleFeature<T> {
  private DoubleFeature<T> valueFeature;
  private IntegerFeature<T> nFeature;

  public GraduateFeature(DoubleFeature<T> valueFeature, IntegerFeature<T> nFeature) {
    super();
    this.valueFeature = valueFeature;
    this.nFeature = nFeature;
    this.setName(this.getName() + "(" + valueFeature.getName() + "," + nFeature.getName() + ")");
  }

  @Override
  public FeatureResult<Double> checkInternal(T context, RuntimeEnvironment env) throws TalismaneException {
    FeatureResult<Double> rawResult = valueFeature.check(context, env);
    FeatureResult<Integer> nResult = nFeature.check(context, env);
    FeatureResult<Double> result = null;
    if (rawResult != null && nResult != null) {
      double weight = rawResult.getOutcome();
      int n = nResult.getOutcome();
      double graduatedWeight = (1.0 / n) * Math.round(weight * (n - 1));
      result = this.generateResult(graduatedWeight);
    }
    return result;
  }

  public DoubleFeature<T> getValueFeature() {
    return valueFeature;
  }

  public void setValueFeature(DoubleFeature<T> valueFeature) {
    this.valueFeature = valueFeature;
  }

  public IntegerFeature<T> getnFeature() {
    return nFeature;
  }

  public void setnFeature(IntegerFeature<T> nFeature) {
    this.nFeature = nFeature;
  }

}
