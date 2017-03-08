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
 * Converts an integer feature to a double feature
 * 
 * @author Assaf Urieli
 *
 */
public class IntegerToDoubleFeature<T> extends AbstractFeature<T, Double>implements DoubleFeature<T> {
	private IntegerFeature<T> integerFeature;

	public IntegerToDoubleFeature(IntegerFeature<T> integerFeature) {
		super();
		this.integerFeature = integerFeature;
		this.setName("IntToDouble(" + this.integerFeature.getName() + ")");
		this.addArgument(integerFeature);
	}

	@Override
	public FeatureResult<Double> check(T context, RuntimeEnvironment env) {
		FeatureResult<Double> featureResult = null;

		FeatureResult<Integer> integerResult = integerFeature.check(context, env);
		if (integerResult != null) {
			featureResult = this.generateResult(integerResult.getOutcome().doubleValue());
		}
		return featureResult;
	}

	public IntegerFeature<T> getIntegerFeature() {
		return integerFeature;
	}

	public void setIntegerFeature(IntegerFeature<T> integerFeature) {
		this.integerFeature = integerFeature;
	}

}
