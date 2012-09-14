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

import com.joliciel.talismane.utils.util.PerformanceMonitor;

/**
 * In addition to AbstractFeature, allows us to cache the feature result in the context
 * to avoid multiple checking, and logs performance.
 * @author Assaf Urieli
 *
 * @param <T>
 * @param <Y>
 */
public abstract class AbstractCachableFeature<T,Y> extends AbstractFeature<T, Y> implements Feature<T,Y>, Comparable<Feature<T,?>> {
	public AbstractCachableFeature() {
		super();
	}

	@Override
	public final FeatureResult<Y> check(T context) {
		PerformanceMonitor.startTask(logName);
		try {
			FeatureResult<Y> featureResult = this.checkInCache(context);
			if (featureResult==null) {
				featureResult = this.checkInternal(context);
				this.putInCache(context, featureResult);
			}
			return featureResult;		
		} finally {
			PerformanceMonitor.endTask(logName);
		}

	}


	/**
	 * Override if this feature result should be cached within the context
	 * to avoid checking multiple times.
	 * @param context
	 * @return
	 */
	protected FeatureResult<Y> checkInCache(T context) {
		return null;
	}
	
	/**
	 * Override if this feature result should be cached within the context
	 * to avoid checking multiple times.
	 * @param context
	 * @return
	 */	
	protected void putInCache(T context, FeatureResult<Y> featureResult) {
	}

	protected abstract FeatureResult<Y> checkInternal(T context);
	
	
}