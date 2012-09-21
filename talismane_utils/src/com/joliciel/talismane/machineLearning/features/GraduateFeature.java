///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
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
 * Takes a feature with a value from 0 to 1, and converts it to a graduated value
 * of 0, 1/n, 2/n, 3/n, ..., 1
 * @author Assaf Urieli
 *
 */
public class GraduateFeature<T> extends AbstractCachableFeature<T,Double> implements DoubleFeature<T> {
	private DoubleFeature<T> feature = null;
	private int n;
	
	public GraduateFeature(DoubleFeature<T> feature, int n) {
		super();
		this.feature = feature;
		this.n = n;
		this.setName(this.feature.getName() + "{graduated_" + n + "}");
	}


	@Override
	public FeatureResult<Double> checkInternal(T context) {
		FeatureResult<Double> rawOutcome = feature.check(context);
		FeatureResult<Double> outcome = null;
		if (rawOutcome!=null) {
			double weight = rawOutcome.getOutcome();
			double graduatedWeight = (1.0/(double)n) * Math.round(weight * (double) (n-1));
			outcome = this.generateResult(graduatedWeight);
		}
		return outcome;
	}


}
