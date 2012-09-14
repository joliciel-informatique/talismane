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
package com.joliciel.talismane.utils.features;

/**
 * Combines two boolean features using a boolean OR.
 * If any feature returns null, will return a null.
 * @author Assaf Urieli
 *
 * @param <T>
 */
public class OrFeature<T> extends AbstractCachableFeature<T, Boolean> implements BooleanFeature<T> {
	BooleanFeature<T>[] booleanFeatures;
	
	public OrFeature(BooleanFeature<T>... booleanFeatures) {
		super();
		this.booleanFeatures = booleanFeatures;
		String name = "Or(";
		boolean firstFeature = true;
		for (BooleanFeature<T> booleanFeature : booleanFeatures) {
			if (!firstFeature)
				 name += ",";
			name += booleanFeature.getName();
			firstFeature = false;
		}
		name += ")";
		this.setName(name);
	}

	@Override
	public FeatureResult<Boolean> checkInternal(T context) {
		FeatureResult<Boolean> featureResult = null;
		
		boolean hasNull = false;
		boolean booleanResult = false;
		for (BooleanFeature<T> booleanFeature : booleanFeatures) {
			FeatureResult<Boolean> result = booleanFeature.check(context);
			if (result==null) {
				hasNull = true;
				break;
			}
			booleanResult = booleanResult || result.getOutcome();
			// not breaking out as soon as we hit a true,
			// since if any single feature returns a null, we want to return a null
		}
		
		if (!hasNull) {
			featureResult = this.generateResult(booleanResult);
		}
		return featureResult;
	}

	public BooleanFeature<T>[] getBooleanFeatures() {
		return booleanFeatures;
	}
}
