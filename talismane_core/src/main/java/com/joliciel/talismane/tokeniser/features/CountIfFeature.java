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
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.tokeniser.Token;

/**
 * Counts the tokens within a certain range matching a certain criterion.<br>
 * The range is given in absolute indexes.<br>
 * If start &gt; end, returns null.<br>
 * If no end is provided, assumes it should go till end of sentence.<br>
 * 
 * @author Assaf Urieli
 *
 */
public final class CountIfFeature extends AbstractTokenFeature<Integer> implements IntegerFeature<TokenWrapper> {
  private BooleanFeature<TokenWrapper> criterion;
  private IntegerFeature<TokenWrapper> startIndexFeature = null;
  private IntegerFeature<TokenWrapper> endIndexFeature = null;

  public CountIfFeature(BooleanFeature<TokenWrapper> criterion, IntegerFeature<TokenWrapper> startIndexFeature) {
    this.criterion = criterion;
    this.startIndexFeature = startIndexFeature;
    this.setName(super.getName() + "(" + criterion.getName() + "," + startIndexFeature.getName() + ")");
  }

  public CountIfFeature(TokenAddressFunction<TokenWrapper> addressFunction, BooleanFeature<TokenWrapper> criterion,
      IntegerFeature<TokenWrapper> startIndexFeature) {
    this(criterion, startIndexFeature);
    this.setAddressFunction(addressFunction);
  }

  public CountIfFeature(BooleanFeature<TokenWrapper> criterion, IntegerFeature<TokenWrapper> startIndexFeature, IntegerFeature<TokenWrapper> endIndexFeature) {
    this.criterion = criterion;
    this.startIndexFeature = startIndexFeature;
    this.endIndexFeature = endIndexFeature;
    this.setName(super.getName() + "(" + criterion.getName() + "," + startIndexFeature.getName() + "," + endIndexFeature.getName() + ")");
  }

  public CountIfFeature(TokenAddressFunction<TokenWrapper> addressFunction, BooleanFeature<TokenWrapper> criterion,
      IntegerFeature<TokenWrapper> startIndexFeature, IntegerFeature<TokenWrapper> endIndexFeature) {
    this(criterion, startIndexFeature, endIndexFeature);
    this.setAddressFunction(addressFunction);
  }

  @Override
  public FeatureResult<Integer> checkInternal(TokenWrapper tokenWrapper, RuntimeEnvironment env) throws TalismaneException {
    TokenWrapper innerWrapper = this.getToken(tokenWrapper, env);
    if (innerWrapper == null)
      return null;
    Token token = innerWrapper.getToken();

    FeatureResult<Integer> featureResult = null;

    int startIndex = 0;
    int endIndex = token.getTokenSequence().size();

    FeatureResult<Integer> startIndexResult = startIndexFeature.check(innerWrapper, env);
    if (startIndexResult != null) {
      startIndex = startIndexResult.getOutcome();
    } else {
      return null;
    }

    if (endIndexFeature != null) {
      FeatureResult<Integer> endIndexResult = endIndexFeature.check(innerWrapper, env);
      if (endIndexResult != null) {
        endIndex = endIndexResult.getOutcome();
      } else {
        return null;
      }
    }

    if (endIndex < startIndex)
      return null;

    if (startIndex <= 0)
      startIndex = 0;

    int count = 0;
    for (int i = startIndex; i < token.getTokenSequence().size() && i <= endIndex; i++) {
      Token oneToken = token.getTokenSequence().get(i);
      FeatureResult<Boolean> criterionResult = this.criterion.check(oneToken, env);
      if (criterionResult != null && criterionResult.getOutcome()) {
        count++;
      }
    }

    featureResult = this.generateResult(count);

    return featureResult;
  }
}
