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
 * Returns true if a feature is null, false otherwise.
 * 
 * @author Assaf Urieli
 *
 */
public class IsNullFeature<T> extends AbstractCachableFeature<T, Boolean>implements BooleanFeature<T> {
	Feature<T, ?> testFeature;

	public IsNullFeature(Feature<T, ?> testFeature) {
		super();
		this.testFeature = testFeature;
		this.setName("IsNull(" + testFeature.getName() + ")");
	}

	@Override
	public FeatureResult<Boolean> checkInternal(T context, RuntimeEnvironment env) throws TalismaneException {
		FeatureResult<Boolean> featureResult = null;

		FeatureResult<?> result1 = testFeature.check(context, env);
		featureResult = this.generateResult(result1 == null);
		return featureResult;
	}

	public Feature<T, ?> getTestFeature() {
		return testFeature;
	}

	public void setTestFeature(Feature<T, ?> testFeature) {
		this.testFeature = testFeature;
	}

}
