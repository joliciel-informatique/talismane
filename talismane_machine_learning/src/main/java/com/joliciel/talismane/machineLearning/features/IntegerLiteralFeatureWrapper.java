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
package com.joliciel.talismane.machineLearning.features;

import com.joliciel.talismane.TalismaneException;

/**
 * Returns the wrapped literal.
 * 
 * @author Assaf Urieli
 *
 */
public class IntegerLiteralFeatureWrapper<T> extends AbstractFeature<T, Integer>implements IntegerFeature<T> {
  private IntegerFeature<T> integerLiteralFeature;

  public IntegerLiteralFeatureWrapper(IntegerFeature<T> integerLiteralFeature) {
    super();
    this.integerLiteralFeature = integerLiteralFeature;
    this.setName(integerLiteralFeature.getName());
  }

  @Override
  public FeatureResult<Integer> check(T context, RuntimeEnvironment env) throws TalismaneException {
    FeatureResult<Integer> featureResult = null;

    FeatureResult<Integer> literalResult = integerLiteralFeature.check(context, env);

    if (literalResult != null) {
      int result = literalResult.getOutcome();
      featureResult = this.generateResult(result);
    }

    return featureResult;

  }

  public IntegerFeature<T> getIntegerLiteralFeature() {
    return integerLiteralFeature;
  }

  public void setIntegerLiteralFeature(IntegerFeature<T> integerLiteralFeature) {
    this.integerLiteralFeature = integerLiteralFeature;
  }

}
