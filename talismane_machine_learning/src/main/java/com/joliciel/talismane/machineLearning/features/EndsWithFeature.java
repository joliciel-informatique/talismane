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
 * Returns true if the first string feature ends with any of the other string
 * features.
 * 
 * @author Assaf Urieli
 *
 */
public class EndsWithFeature<T> extends AbstractCachableFeature<T, Boolean>implements BooleanFeature<T> {
  StringFeature<T>[] stringFeatures;

  @SafeVarargs
  public EndsWithFeature(StringFeature<T>... stringFeatures) {
    super();
    this.stringFeatures = stringFeatures;
    String name = this.getName() + "(";
    boolean firstFeature = true;
    for (StringFeature<T> stringFeature : stringFeatures) {
      if (!firstFeature)
        name += ",";
      name += stringFeature.getName();
      firstFeature = false;
    }
    name += ")";
    this.setName(name);
  }

  @Override
  public FeatureResult<Boolean> checkInternal(T context, RuntimeEnvironment env) throws TalismaneException {
    FeatureResult<Boolean> featureResult = null;

    String string = null;
    boolean endsWith = false;
    boolean firstFeature = true;

    for (StringFeature<T> stringFeature : stringFeatures) {
      FeatureResult<String> result = stringFeature.check(context, env);
      if (result == null) {
        if (firstFeature)
          break;
        else
          continue;
      }
      if (firstFeature) {
        string = result.getOutcome();
      } else if (string.endsWith(result.getOutcome())) {
        endsWith = true;
        break;
      }
      firstFeature = false;
    }

    if (string != null)
      featureResult = this.generateResult(endsWith);
    return featureResult;
  }

  public StringFeature<T>[] getStringFeatures() {
    return stringFeatures;
  }
}
