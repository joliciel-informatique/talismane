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
 * If the wrapped boolean feature returns null, will convert it to a false.
 * @author Assaf Urieli
 *
 * @param <T>
 */
public class NullToFalseFeature<T> extends AbstractCachableFeature<T, Boolean> implements BooleanFeature<T> {
	BooleanFeature<T> wrappedFeature;
	
	public NullToFalseFeature(BooleanFeature<T> feature1) {
		super();
		this.wrappedFeature = feature1;
		this.setName(feature1.getName() + "°");
	}

	@Override
	public FeatureResult<Boolean> checkInternal(T context, RuntimeEnvironment env) {
		FeatureResult<Boolean> featureResult = null;
		
		FeatureResult<Boolean> result1 = wrappedFeature.check(context, env);
		
		if (result1!=null) {
			featureResult = this.generateResult(result1.getOutcome());
		} else {
			featureResult = this.generateResult(false);
		}
		return featureResult;
	}

	public BooleanFeature<T> getWrappedFeature() {
		return wrappedFeature;
	}
	
	
}
