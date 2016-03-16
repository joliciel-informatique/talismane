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

import com.joliciel.talismane.utils.PerformanceMonitor;

/**
 * In addition to AbstractFeature, logs performance.
 * @author Assaf Urieli
 *
 */
public abstract class AbstractMonitorableFeature<T,Y> extends AbstractFeature<T, Y> implements Feature<T,Y>, Comparable<Feature<T,?>> {
	public AbstractMonitorableFeature() {
		super();
	}

	@Override
	public final FeatureResult<Y> check(T context, RuntimeEnvironment env) {
		PerformanceMonitor monitor = PerformanceMonitor.getMonitor(this.getClass());
		monitor.startTask("check");
		try {
			return this.checkInternal(context, env);
		} finally {
			monitor.endTask();
		}
	}

	protected abstract FeatureResult<Y> checkInternal(T context, RuntimeEnvironment env);

}