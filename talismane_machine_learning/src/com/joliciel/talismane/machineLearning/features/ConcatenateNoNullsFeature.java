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
 * Merges two or more string features by concatenating their results and adding a | in between.
 * If any of the results is null, returns a null.
 * @author Assaf Urieli
 *
 * @param <T>
 */
public class ConcatenateNoNullsFeature<T> extends AbstractCachableFeature<T, String> implements
		StringFeature<T> {
	StringFeature<T>[] stringFeatures;
	
	public ConcatenateNoNullsFeature(StringFeature<T>... stringFeatures) {
		super();
		this.stringFeatures = stringFeatures;
		String name = "ConcatNoNulls(";
		boolean firstFeature = true;
		for (StringFeature<T> stringFeature : stringFeatures) {
			if (!firstFeature)
				name += ",";
			name += stringFeature.getName();
			firstFeature = false;
		}
		name += ")";
		this.setName(name);
	}

	@Override
	public FeatureResult<String> checkInternal(T context) {
		FeatureResult<String> featureResult = null;
		
		StringBuilder sb = new StringBuilder();
		boolean firstFeature = true;
		boolean hasNull = false;
		for (StringFeature<T> stringFeature : stringFeatures) {
			if (!firstFeature)
				sb.append("|");
			FeatureResult<String> result = stringFeature.check(context);
			if (result==null) {
				hasNull = true;
				break;
			}
			
			sb.append(result.getOutcome());
			firstFeature = false;
		}
		
		if (!hasNull)
			featureResult = this.generateResult(sb.toString());
		return featureResult;
	}

	public StringFeature<T>[] getStringFeatures() {
		return stringFeatures;
	}

}
