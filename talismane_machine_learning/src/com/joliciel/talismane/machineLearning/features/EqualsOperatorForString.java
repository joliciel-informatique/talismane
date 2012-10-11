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
 * Returns operand1 == operand2 for two strings.
 * 
 * @author Assaf Urieli
 *
 * @param <T>
 */
public class EqualsOperatorForString<T> extends AbstractCachableFeature<T,Boolean> implements
		BooleanFeature<T> {
	private StringFeature<T> operand1;
	private StringFeature<T> operand2;
	
	public EqualsOperatorForString(StringFeature<T> operand1, StringFeature<T> operand2) {
		super();
		this.operand1 = operand1;
		this.operand2 = operand2;
		this.setName(operand1.getName() + "==" + operand2.getName());
	}

	@Override
	protected FeatureResult<Boolean> checkInternal(T context) {
		FeatureResult<Boolean> featureResult = null;
		
		FeatureResult<String> operand1Result = operand1.check(context);
		if (operand1Result!=null) {
			FeatureResult<String> operand2Result = operand2.check(context);
			if (operand2Result!=null) {
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
}
