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
 * If the condition returns true, return null, else return the result of the feature provided.
 * @author Assaf Urieli
 *
 * @param <T>
 */
public class NullIfGenericFeature<T,Y> extends AbstractCachableFeature<T,Y> implements
		Feature<T,Y> {
	private BooleanFeature<T> condition;
	private Feature<T,Y> resultFeature;
	
	public NullIfGenericFeature(BooleanFeature<T> condition, Feature<T,Y> resultFeature) {
		super();
		this.condition = condition;
		this.resultFeature = resultFeature;
		this.setName("NullIf(" + condition.getName() + "," + resultFeature.getName() + ")");
	}

	@Override
	protected FeatureResult<Y> checkInternal(T context, RuntimeEnvironment env) {
		FeatureResult<Y> featureResult = null;
		
		FeatureResult<Boolean> conditionResult = condition.check(context, env);
		if (conditionResult!=null) {
			boolean conditionOutcome = conditionResult.getOutcome();
			if (!conditionOutcome) {
				FeatureResult<Y> thenFeatureResult = resultFeature.check(context, env);
				if (thenFeatureResult!=null) {
					Y result = thenFeatureResult.getOutcome();
					featureResult = this.generateResult(result);
				}
			}
		}
		
		return featureResult;
		
	}
	
	public BooleanFeature<T> getCondition() {
		return condition;
	}

	public Feature<T,Y> getResultFeature() {
		return resultFeature;
	}

	public void setCondition(BooleanFeature<T> condition) {
		this.condition = condition;
	}

	public void setResultFeature(Feature<T,Y> resultFeature) {
		this.resultFeature = resultFeature;
	}
	

	@SuppressWarnings("rawtypes")
	@Override
	public Class<? extends Feature> getFeatureType() {
		return resultFeature.getFeatureType();
	}

}
