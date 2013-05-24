///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2013 Assaf Urieli
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
 * A stand-in for a StringCollectionFeature at runtime, which reads one of the collection values
 * that has been stored in the context via HasRuntimeCollectionSupport.
 * @author Assaf Urieli
 *
 * @param <T>
 */
class StringCollectionFeatureProxy<T> extends AbstractFeature<T, String> implements StringFeature<T> {
	private StringCollectionFeature<T> stringCollectionFeature;
	
	public StringCollectionFeatureProxy(StringCollectionFeature<T> stringCollectionFeature) {
		super();
		this.stringCollectionFeature = stringCollectionFeature;
		this.setName(stringCollectionFeature.getName());
	}

	@Override
	public FeatureResult<String> check(T context, RuntimeEnvironment env) {
		FeatureResult<String> result = null;
		String outcome = (String) env.getValue(stringCollectionFeature.getName());
		if (outcome!=null) {
			result = this.generateResult(outcome);
		}
		return result;
	}

	public StringCollectionFeature<T> getStringCollectionFeature() {
		return stringCollectionFeature;
	}
	
}
