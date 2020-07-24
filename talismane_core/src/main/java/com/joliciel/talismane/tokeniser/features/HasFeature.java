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
package com.joliciel.talismane.tokeniser.features;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.machineLearning.features.BooleanFeature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;

/**
 * Returns true if the token refered to by address meets the boolean criterion.
 * 
 * @author Assaf Urieli
 *
 */
public final class HasFeature extends AbstractTokenFeature<Boolean> implements BooleanFeature<TokenWrapper> {
  BooleanFeature<TokenWrapper> criterion;

  public HasFeature(TokenAddressFunction<TokenWrapper> addressFunction, BooleanFeature<TokenWrapper> criterion) {
    this.criterion = criterion;
    this.setName(super.getName() + "(" + this.criterion.getName() + ")");

    this.setAddressFunction(addressFunction);
  }

  @Override
  public FeatureResult<Boolean> checkInternal(TokenWrapper tokenWrapper, RuntimeEnvironment env) throws TalismaneException {
    TokenWrapper innerWrapper = this.getToken(tokenWrapper, env);
    if (innerWrapper == null)
      return null;
    FeatureResult<Boolean> result = null;

    FeatureResult<Boolean> criterionResult = criterion.check(innerWrapper, env);
    if (criterionResult != null) {
      result = this.generateResult(criterionResult.getOutcome());
    }

    return result;
  }

}
