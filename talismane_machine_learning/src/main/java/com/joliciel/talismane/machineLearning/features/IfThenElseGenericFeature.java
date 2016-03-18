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
 * Mimics an in-then-else structure - if condition is true return thenFeature result, else return elseFeature result.
 * @author Assaf Urieli
 *
 */
public class IfThenElseGenericFeature<T,Y> extends AbstractCachableFeature<T,Y> {
	private BooleanFeature<T> condition;
	private Feature<T,Y> thenFeature;
	private Feature<T,Y> elseFeature;
	
	public IfThenElseGenericFeature(BooleanFeature<T> condition, Feature<T,Y> thenFeature, Feature<T,Y> elseFeature) {
		super();
		this.condition = condition;
		this.thenFeature = thenFeature;
		this.elseFeature = elseFeature;
		this.setName("IfThenElse(" + condition.getName() + "," + thenFeature.getName() + "," + elseFeature.getName() + ")");
	}

	@Override
	protected FeatureResult<Y> checkInternal(T context, RuntimeEnvironment env) {
		FeatureResult<Y> featureResult = null;
		
		FeatureResult<Boolean> conditionResult = condition.check(context, env);
		if (conditionResult!=null) {
			boolean conditionOutcome = conditionResult.getOutcome();
			if (conditionOutcome) {
				FeatureResult<Y> thenFeatureResult = thenFeature.check(context, env);
				if (thenFeatureResult!=null) {
					Y result = thenFeatureResult.getOutcome();
					featureResult = this.generateResult(result);
				}
			} else {
				FeatureResult<Y> elseFeatureResult = elseFeature.check(context, env);
				if (elseFeatureResult!=null) {
					Y result = elseFeatureResult.getOutcome();
					featureResult = this.generateResult(result);
				}
			}
		}
		
		
		return featureResult;
		
	}

	public BooleanFeature<T> getCondition() {
		return condition;
	}

	public Feature<T,Y> getThenFeature() {
		return thenFeature;
	}

	public Feature<T,Y> getElseFeature() {
		return elseFeature;
	}

	public void setCondition(BooleanFeature<T> condition) {
		this.condition = condition;
	}

	public void setThenFeature(Feature<T,Y> thenFeature) {
		this.thenFeature = thenFeature;
	}

	public void setElseFeature(Feature<T,Y> elseFeature) {
		this.elseFeature = elseFeature;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class<? extends Feature> getFeatureType() {
		return thenFeature.getFeatureType();
	}

}
