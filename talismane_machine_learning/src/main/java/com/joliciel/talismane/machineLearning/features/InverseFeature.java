///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2014 Joliciel Informatique
//
//This file is part of Jochre.
//
//Jochre is free software: you can redistribute it and/or modify
//it under the terms of the GNU Affero General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//Jochre is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Affero General Public License for more details.
//
//You should have received a copy of the GNU Affero General Public License
//along with Jochre.  If not, see <http://www.gnu.org/licenses/>.
//////////////////////////////////////////////////////////////////////////////
package com.joliciel.talismane.machineLearning.features;

/**
 * Inverts a normalised double feature (whose values go from 0 to 1), giving
 * 1-result. If the result is &lt; 0, returns 0.
 * 
 * @author Assaf Urieli
 *
 */
public class InverseFeature<T> extends AbstractCachableFeature<T, Double>implements DoubleFeature<T> {

	Feature<T, Double> valueFeature;

	public InverseFeature(Feature<T, Double> feature) {
		super();
		this.valueFeature = feature;
		this.setName(this.getName() + "(" + this.valueFeature.getName() + ")");
	}

	@Override
	public FeatureResult<Double> checkInternal(T context, RuntimeEnvironment env) {
		FeatureResult<Double> rawOutcome = valueFeature.check(context, env);
		FeatureResult<Double> outcome = null;
		if (rawOutcome != null) {
			double weight = rawOutcome.getOutcome();
			double inverseWeight = 1 - weight;
			if (inverseWeight < 0)
				inverseWeight = 0;
			outcome = this.generateResult(inverseWeight);
		}
		return outcome;
	}

	public Feature<T, Double> getValueFeature() {
		return valueFeature;
	}

	public void setValueFeature(Feature<T, Double> valueFeature) {
		this.valueFeature = valueFeature;
	}

}
