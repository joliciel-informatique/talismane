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
package com.joliciel.talismane.utils.features;

/**
 * Mimics an in-then-else structure - if condition is true return thenFeature result, else retunr elseFeature result.
 * @author Assaf Urieli
 *
 * @param <T>
 */
public class IfThenElseStringFeature<T> extends AbstractCachableFeature<T,String> implements
		StringFeature<T> {
	private BooleanFeature<T> condition;
	private StringFeature<T> thenFeature;
	private StringFeature<T> elseFeature;
	
	public IfThenElseStringFeature(BooleanFeature<T> condition, StringFeature<T> thenFeature, StringFeature<T> elseFeature) {
		super();
		this.condition = condition;
		this.thenFeature = thenFeature;
		this.elseFeature = elseFeature;
		this.setName("IfThenElse(" + condition.getName() + "," + thenFeature.getName() + "," + elseFeature.getName() + ")");
	}

	@Override
	protected FeatureResult<String> checkInternal(T context) {
		FeatureResult<String> featureResult = null;
		
		FeatureResult<Boolean> conditionResult = condition.check(context);
		if (conditionResult!=null) {
			boolean conditionOutcome = conditionResult.getOutcome();
			if (conditionOutcome) {
				FeatureResult<String> thenFeatureResult = thenFeature.check(context);
				if (thenFeatureResult!=null) {
					String result = thenFeatureResult.getOutcome();
					featureResult = this.generateResult(result);
				}
			} else {
				FeatureResult<String> elseFeatureResult = elseFeature.check(context);
				if (elseFeatureResult!=null) {
					String result = elseFeatureResult.getOutcome();
					featureResult = this.generateResult(result);
				}
			}
		}
		
		
		return featureResult;
		
	}

	public BooleanFeature<T> getCondition() {
		return condition;
	}

	public StringFeature<T> getThenFeature() {
		return thenFeature;
	}

	public StringFeature<T> getElseFeature() {
		return elseFeature;
	}

}
