///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
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

/**
 * Returns operand1 == operand2, with an error margin of 0.0001 is allowed.
 * 
 * @author Assaf Urieli
 *
 * @param <T>
 */
public class EqualsOperatorForDouble<T> extends AbstractCachableFeature<T,Boolean> implements
		BooleanFeature<T> {
	private DoubleFeature<T> operand1;
	private DoubleFeature<T> operand2;
	private double sigma = 0.0001;
	
	public EqualsOperatorForDouble(DoubleFeature<T> operand1, DoubleFeature<T> operand2) {
		super();
		this.operand1 = operand1;
		this.operand2 = operand2;
		this.setName(operand1.getName() + "==" + operand2.getName());
	}
	
	@Override
	protected FeatureResult<Boolean> checkInternal(T context, RuntimeEnvironment env) {
		FeatureResult<Boolean> featureResult = null;
		
		FeatureResult<Double> operand1Result = operand1.check(context, env);
		if (operand1Result!=null) {
			FeatureResult<Double> operand2Result = operand2.check(context, env);
			
			if (operand2Result!=null) {
				double diff = Math.abs(operand1Result.getOutcome() - operand2Result.getOutcome());
				boolean result = diff <= sigma;
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
}
