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

/**
 * If the condition returns true, return null, else return the result of the
 * feature provided.
 * 
 * @author Assaf Urieli
 *
 */
public class NullIfIntegerFeature<T> extends AbstractCachableFeature<T, Integer>implements IntegerFeature<T> {
	private BooleanFeature<T> condition;
	private IntegerFeature<T> resultFeature;

	public NullIfIntegerFeature(BooleanFeature<T> condition, IntegerFeature<T> resultFeature) {
		super();
		this.condition = condition;
		this.resultFeature = resultFeature;
		this.setName("NullIf(" + condition.getName() + "," + resultFeature.getName() + ")");
	}

	@Override
	protected FeatureResult<Integer> checkInternal(T context, RuntimeEnvironment env) {
		FeatureResult<Integer> featureResult = null;

		FeatureResult<Boolean> conditionResult = condition.check(context, env);
		if (conditionResult != null) {
			boolean conditionOutcome = conditionResult.getOutcome();
			if (!conditionOutcome) {
				FeatureResult<Integer> thenFeatureResult = resultFeature.check(context, env);
				if (thenFeatureResult != null) {
					int result = thenFeatureResult.getOutcome();
					featureResult = this.generateResult(result);
				}
			}
		}

		return featureResult;

	}

	public BooleanFeature<T> getCondition() {
		return condition;
	}

	public IntegerFeature<T> getResultFeature() {
		return resultFeature;
	}

	public void setCondition(BooleanFeature<T> condition) {
		this.condition = condition;
	}

	public void setResultFeature(IntegerFeature<T> resultFeature) {
		this.resultFeature = resultFeature;
	}

}
