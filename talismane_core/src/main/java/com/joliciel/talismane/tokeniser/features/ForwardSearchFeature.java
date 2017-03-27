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
 * Returns the first token following this one which matches a certain criterion,
 * or null if no such token is found.<br/>
 * The user may optionally provide absolute indexes to limit the search. By
 * default, the start index is the current token index+1, and the end index is
 * the end of sentence.<br/>
 * 
 * @author Assaf Urieli
 *
 */
public final class ForwardSearchFeature extends AbstractTokenAddressFunction {
  private BooleanFeature<TokenWrapper> criterion;
  private BooleanFeature<TokenWrapper> stopCriterion;
  private IntegerFeature<TokenWrapper> startIndexFeature = null;
  private IntegerFeature<TokenWrapper> endIndexFeature = null;

  public ForwardSearchFeature(BooleanFeature<TokenWrapper> criterion) {
    this.criterion = criterion;
    this.setName(super.getName() + "(" + criterion.getName() + ")");
  }

  public ForwardSearchFeature(TokenAddressFunction<TokenWrapper> addressFunction, BooleanFeature<TokenWrapper> criterion) {
    this(criterion);
    this.setAddressFunction(addressFunction);
  }

  public ForwardSearchFeature(BooleanFeature<TokenWrapper> criterion, IntegerFeature<TokenWrapper> startIndexFeature) {
    this.criterion = criterion;
    this.startIndexFeature = startIndexFeature;
    this.setName(super.getName() + "(" + criterion.getName() + "," + startIndexFeature.getName() + ")");
  }

  public ForwardSearchFeature(TokenAddressFunction<TokenWrapper> addressFunction, BooleanFeature<TokenWrapper> criterion,
      IntegerFeature<TokenWrapper> startIndexFeature) {
    this(criterion, startIndexFeature);
    this.setAddressFunction(addressFunction);
  }

  public ForwardSearchFeature(BooleanFeature<TokenWrapper> criterion, IntegerFeature<TokenWrapper> startIndexFeature,
      IntegerFeature<TokenWrapper> endIndexFeature) {
    this.criterion = criterion;
    this.startIndexFeature = startIndexFeature;
    this.endIndexFeature = endIndexFeature;
    this.setName(super.getName() + "(" + criterion.getName() + "," + startIndexFeature.getName() + "," + endIndexFeature.getName() + ")");
  }

  public ForwardSearchFeature(TokenAddressFunction<TokenWrapper> addressFunction, BooleanFeature<TokenWrapper> criterion,
      IntegerFeature<TokenWrapper> startIndexFeature, IntegerFeature<TokenWrapper> endIndexFeature) {
    this(criterion, startIndexFeature, endIndexFeature);
    this.setAddressFunction(addressFunction);
  }

  public ForwardSearchFeature(BooleanFeature<TokenWrapper> criterion, BooleanFeature<TokenWrapper> stopCriterion,
      IntegerFeature<TokenWrapper> startIndexFeature, IntegerFeature<TokenWrapper> endIndexFeature) {
    this.criterion = criterion;
    this.stopCriterion = stopCriterion;
    this.startIndexFeature = startIndexFeature;
    this.endIndexFeature = endIndexFeature;
    this.setName(super.getName() + "(" + criterion.getName() + "," + stopCriterion.getName() + "," + startIndexFeature.getName() + ","
        + endIndexFeature.getName() + ")");
  }

  public ForwardSearchFeature(TokenAddressFunction<TokenWrapper> addressFunction, BooleanFeature<TokenWrapper> criterion,
      BooleanFeature<TokenWrapper> stopCriterion, IntegerFeature<TokenWrapper> startIndexFeature, IntegerFeature<TokenWrapper> endIndexFeature) {
    this(criterion, stopCriterion, startIndexFeature, endIndexFeature);
    this.setAddressFunction(addressFunction);
  }

  @Override
  public FeatureResult<TokenWrapper> checkInternal(TokenWrapper tokenWrapper, RuntimeEnvironment env) throws TalismaneException {
    TokenWrapper innerWrapper = this.getToken(tokenWrapper, env);
    if (innerWrapper == null)
      return null;
    Token token = innerWrapper.getToken();

    FeatureResult<TokenWrapper> featureResult = null;

    int startIndex = token.getIndex() + 1;
    int endIndex = token.getTokenSequence().size();

    if (startIndexFeature != null) {
      FeatureResult<Integer> startIndexResult = startIndexFeature.check(innerWrapper, env);
      if (startIndexResult != null) {
        startIndex = startIndexResult.getOutcome();
      } else {
        return null;
      }
    }

    if (endIndexFeature != null) {
      FeatureResult<Integer> endIndexResult = endIndexFeature.check(innerWrapper, env);
      if (endIndexResult != null) {
        endIndex = endIndexResult.getOutcome();
      } else {
        return null;
      }
    }

    if (startIndex >= token.getTokenSequence().size())
      return null;
    if (endIndex < 0)
      return null;

    if (endIndex < startIndex)
      return null;

    if (startIndex < 0)
      startIndex = 0;

    Token matchingToken = null;
    for (int i = startIndex; i < token.getTokenSequence().size() && i <= endIndex; i++) {
      Token oneToken = token.getTokenSequence().get(i);
      FeatureResult<Boolean> criterionResult = this.criterion.check(oneToken, env);
      if (criterionResult != null && criterionResult.getOutcome()) {
        matchingToken = oneToken;
        break;
      }
      if (stopCriterion != null) {
        FeatureResult<Boolean> stopCriterionResult = this.stopCriterion.check(oneToken, env);
        if (stopCriterionResult != null && stopCriterionResult.getOutcome()) {
          break;
        }
      }
    }
    if (matchingToken != null) {
      featureResult = this.generateResult(matchingToken);
    }

    return featureResult;
  }
}
