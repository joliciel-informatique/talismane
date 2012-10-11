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

import com.joliciel.talismane.utils.PerformanceMonitor;

/**
 * In addition to AbstractFeature, logs performance.
 * @author Assaf Urieli
 *
 * @param <T>
 * @param <Y>
 */
public abstract class AbstractMonitorableFeature<T,Y> extends AbstractFeature<T, Y> implements Feature<T,Y>, Comparable<Feature<T,?>> {
	public AbstractMonitorableFeature() {
		super();
	}

	@Override
	public final FeatureResult<Y> check(T context) {
		PerformanceMonitor.startTask(logName);
		try {
			return this.checkInternal(context);
		} finally {
			PerformanceMonitor.endTask(logName);
		}
	}

	protected abstract FeatureResult<Y> checkInternal(T context);

}