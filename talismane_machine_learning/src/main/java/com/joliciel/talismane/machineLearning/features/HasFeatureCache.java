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

import com.joliciel.talismane.TalismaneException;

/**
 * Any context implementing this interface has a feature cache in which to cache
 * features.
 * 
 * @author Assaf Urieli
 */
public interface HasFeatureCache {
	/**
	 * Get a particular feature result from the cache.
	 * 
	 * @throws TalismaneException
	 */
	public <T, Y> FeatureResult<Y> getResultFromCache(Feature<T, Y> feature, RuntimeEnvironment env) throws TalismaneException;

	/**
	 * Place a feature result in the cache.
	 * 
	 * @throws TalismaneException
	 */
	public <T, Y> void putResultInCache(Feature<T, Y> feature, FeatureResult<Y> featureResult, RuntimeEnvironment env) throws TalismaneException;
}
