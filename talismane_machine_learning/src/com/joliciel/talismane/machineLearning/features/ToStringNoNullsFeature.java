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
 * Converts a non-string feature to a string feature.
 * If the feature result is null, will return null (rather than the string "null").
 * @author Assaf Urieli
 *
 * @param <T>
 */
public class ToStringNoNullsFeature<T> extends AbstractCachableFeature<T, String> implements StringFeature<T> {
	Feature<T,?> feature1;
	
	public ToStringNoNullsFeature(Feature<T,?> feature1) {
		super();
		this.feature1 = feature1;
		this.setName(super.getName() + "(" + feature1.getName() + ")");
	}

	@Override
	public FeatureResult<String> checkInternal(T context, RuntimeEnvironment env) {
		FeatureResult<String> featureResult = null;
		
		FeatureResult<?> result1 = feature1.check(context, env);
		
		if (result1!=null) {
			featureResult = this.generateResult(result1.getOutcome().toString());
		}
		return featureResult;
	}
}
