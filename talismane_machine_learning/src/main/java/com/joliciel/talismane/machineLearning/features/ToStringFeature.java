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
 * Converts a non-string feature to a string feature. If the feature result is
 * null, will return null (rather than the string "null").
 * 
 * @author Assaf Urieli
 */
public class ToStringFeature<T> extends AbstractCachableFeature<T, String>implements StringFeature<T> {
	Feature<T, ?> featureToString;

	public ToStringFeature(Feature<T, ?> feature1) {
		super();
		this.featureToString = feature1;
		this.setName(super.getName() + "(" + feature1.getName() + ")");
	}

	@Override
	public FeatureResult<String> checkInternal(T context, RuntimeEnvironment env) {
		FeatureResult<String> featureResult = null;

		FeatureResult<?> result1 = featureToString.check(context, env);

		if (result1 != null) {
			featureResult = this.generateResult(result1.getOutcome().toString());
		}
		return featureResult;
	}

	public Feature<T, ?> getFeatureToString() {
		return featureToString;
	}

	public void setFeatureToString(Feature<T, ?> featureToString) {
		this.featureToString = featureToString;
	}

}
