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
 * If the condition returns true, return null, else return the result of the feature provided.
 * @author Assaf Urieli
 *
 * @param <T>
 */
public class NullIfBooleanFeature<T> extends AbstractCachableFeature<T,Boolean> implements
		BooleanFeature<T> {
	private BooleanFeature<T> condition;
	private BooleanFeature<T> resultFeature;
	
	public NullIfBooleanFeature(BooleanFeature<T> condition, BooleanFeature<T> resultFeature) {
		super();
		this.condition = condition;
		this.resultFeature = resultFeature;
		this.setName("NullIf(" + condition.getName() + "," + resultFeature.getName() + ")");
	}

	@Override
	protected FeatureResult<Boolean> checkInternal(T context) {
		FeatureResult<Boolean> featureResult = null;
		
		FeatureResult<Boolean> conditionResult = condition.check(context);
		if (conditionResult!=null) {
			boolean conditionOutcome = conditionResult.getOutcome();
			if (!conditionOutcome) {
				FeatureResult<Boolean> thenFeatureResult = resultFeature.check(context);
				if (thenFeatureResult!=null) {
					boolean result = thenFeatureResult.getOutcome();
					featureResult = this.generateResult(result);
				}
			}
		}
		
		return featureResult;
		
	}

	public BooleanFeature<T> getCondition() {
		return condition;
	}

	public BooleanFeature<T> getResultFeature() {
		return resultFeature;
	}
}
