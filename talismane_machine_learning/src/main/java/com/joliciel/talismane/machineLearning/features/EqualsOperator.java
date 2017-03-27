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
 * Returns operand1 == operand2. For double values, an error margin of 0.0001 is
 * allowed.
 * 
 * Note: because of enormous usage of this features, it has been split into four
 * separate features, to gain an infinitesimal amount of performance.
 * 
 * @author Assaf Urieli
 *
 */
public class EqualsOperator<T> extends AbstractCachableFeature<T, Boolean>implements BooleanFeature<T> {
  private Feature<T, ?> operand1;
  private Feature<T, ?> operand2;
  private double sigma = 0.0001;
  private Class<?> operandType;

  public EqualsOperator(DoubleFeature<T> operand1, DoubleFeature<T> operand2) {
    super();
    this.operand1 = operand1;
    this.operand2 = operand2;
    this.operandType = Double.class;
    this.setName(operand1.getName() + "==" + operand2.getName());
  }

  public EqualsOperator(IntegerFeature<T> operand1, IntegerFeature<T> operand2) {
    super();
    this.operand1 = operand1;
    this.operand2 = operand2;
    this.operandType = Integer.class;
    this.setName(operand1.getName() + "==" + operand2.getName());
  }

  public EqualsOperator(StringFeature<T> operand1, StringFeature<T> operand2) {
    super();
    this.operand1 = operand1;
    this.operand2 = operand2;
    this.operandType = String.class;
    this.setName(operand1.getName() + "==" + operand2.getName());
  }

  public EqualsOperator(BooleanFeature<T> operand1, BooleanFeature<T> operand2) {
    super();
    this.operand1 = operand1;
    this.operand2 = operand2;
    this.operandType = Boolean.class;
    this.setName(operand1.getName() + "==" + operand2.getName());
  }

  @SuppressWarnings("unchecked")
  @Override
  protected FeatureResult<Boolean> checkInternal(T context, RuntimeEnvironment env) throws TalismaneException {
    FeatureResult<Boolean> featureResult = null;

    if (operandType.equals(Double.class)) {
      FeatureResult<Double> operand1Result = (FeatureResult<Double>) operand1.check(context, env);
      FeatureResult<Double> operand2Result = (FeatureResult<Double>) operand2.check(context, env);

      if (operand1Result != null && operand2Result != null) {
        double diff = Math.abs(operand1Result.getOutcome() - operand2Result.getOutcome());
        boolean result = diff <= sigma;
        featureResult = this.generateResult(result);
      }
    } else if (operandType.equals(Integer.class)) {
      FeatureResult<Integer> operand1Result = (FeatureResult<Integer>) operand1.check(context, env);
      FeatureResult<Integer> operand2Result = (FeatureResult<Integer>) operand2.check(context, env);

      if (operand1Result != null && operand2Result != null) {
        boolean result = operand1Result.getOutcome() == operand2Result.getOutcome();
        featureResult = this.generateResult(result);
      }
    } else if (operandType.equals(String.class)) {
      FeatureResult<String> operand1Result = (FeatureResult<String>) operand1.check(context, env);
      FeatureResult<String> operand2Result = (FeatureResult<String>) operand2.check(context, env);

      if (operand1Result != null && operand2Result != null) {
        boolean result = operand1Result.getOutcome().equals(operand2Result.getOutcome());
        featureResult = this.generateResult(result);
      }
    } else if (operandType.equals(Boolean.class)) {
      FeatureResult<Boolean> operand1Result = (FeatureResult<Boolean>) operand1.check(context, env);
      FeatureResult<Boolean> operand2Result = (FeatureResult<Boolean>) operand2.check(context, env);

      if (operand1Result != null && operand2Result != null) {
        boolean result = operand1Result.getOutcome().equals(operand2Result.getOutcome());
        featureResult = this.generateResult(result);
      }
    }

    return featureResult;

  }

  public Feature<T, ?> getOperand1() {
    return operand1;
  }

  public Feature<T, ?> getOperand2() {
    return operand2;
  }

  public void setOperand1(Feature<T, ?> operand1) {
    this.operand1 = operand1;
  }

  public void setOperand2(Feature<T, ?> operand2) {
    this.operand2 = operand2;
  }

}
