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
 * Returns true if the first string feature starts with any of the other string features.
 * @author Assaf Urieli
 *
 * @param <T>
 */
public class StartsWithFeature<T> extends AbstractCachableFeature<T, Boolean> implements
		BooleanFeature<T> {
	StringFeature<T>[] stringFeatures;
	
	public StartsWithFeature(StringFeature<T>... stringFeatures) {
		super();
		this.stringFeatures = stringFeatures;
		String name = this.getName() + "(";
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
	public FeatureResult<Boolean> checkInternal(T context, RuntimeEnvironment env) {
		FeatureResult<Boolean> featureResult = null;
		
		String string = null;
		boolean startsWith = false;
		boolean firstFeature = true;
		
		for (StringFeature<T> stringFeature : stringFeatures) {
			FeatureResult<String> result = stringFeature.check(context, env);
			if (result==null) {
				if (firstFeature)
					break;
				else
					continue;
			}
			if (firstFeature) {
				string = result.getOutcome();
			} else if (string.startsWith(result.getOutcome())) {
				startsWith = true;
				break;
			}
			firstFeature = false;
		}
		
		if (string!=null)
			featureResult = this.generateResult(startsWith);
		return featureResult;
	}


	public StringFeature<T>[] getStringFeatures() {
		return stringFeatures;
	}
}
