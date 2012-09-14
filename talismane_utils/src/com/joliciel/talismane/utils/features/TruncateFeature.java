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
 * Truncates a double down to an integer.
 * @author Assaf Urieli
 *
 * @param <T>
 */
class TruncateFeature<T> extends AbstractFeature<T, Integer> implements
		IntegerFeature<T> {
	private DoubleFeature<T> doubleFeature;
	
	public TruncateFeature(DoubleFeature<T> doubleFeature) {
		super();
		this.doubleFeature = doubleFeature;
		this.setName(this.doubleFeature.getName());
	}

	@Override
	public FeatureResult<Integer> check(T context) {
		FeatureResult<Integer> featureResult = null;
		
		FeatureResult<Double> doubleResult = doubleFeature.check(context);
		if (doubleResult!=null) {
			int intResult = doubleResult.getOutcome().intValue();
			featureResult = this.generateResult(intResult);
		}
		return featureResult;
	}

	public DoubleFeature<T> getDoubleFeature() {
		return doubleFeature;
	}
}
