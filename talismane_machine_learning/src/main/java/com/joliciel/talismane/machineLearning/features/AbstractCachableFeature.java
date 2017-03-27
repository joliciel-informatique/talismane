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
 * In addition to AbstractFeature, allows us to cache the feature result in the
 * context to avoid multiple checking, and logs performance.
 * 
 * @author Assaf Urieli
 *
 */
public abstract class AbstractCachableFeature<T, Y> extends AbstractFeature<T, Y> {
  public AbstractCachableFeature() {
    super();
  }

  @Override
  public final FeatureResult<Y> check(T context, RuntimeEnvironment env) throws TalismaneException {
    FeatureResult<Y> featureResult = this.checkInCache(context, env);
    if (featureResult == null) {
      featureResult = this.checkInternal(context, env);
      this.putInCache(context, featureResult, env);
    } else if (featureResult.getOutcome() == null) {
      // a NullFeatureResult was stored in the cache
      featureResult = null;
    }
    return featureResult;
  }

  /**
   * Override if this feature result should be cached within the context to
   * avoid checking multiple times.
   */
  protected FeatureResult<Y> checkInCache(T context, RuntimeEnvironment env) throws TalismaneException {
    if (context instanceof HasFeatureCache) {
      return ((HasFeatureCache) context).getResultFromCache(this, env);
    }
    return null;
  }

  /**
   * Override if this feature result should be cached within the context to
   * avoid checking multiple times.
   * 
   * @throws TalismaneException
   */
  protected void putInCache(T context, FeatureResult<Y> featureResult, RuntimeEnvironment env) throws TalismaneException {
    if (context instanceof HasFeatureCache) {
      if (featureResult == null)
        featureResult = new NullFeatureResult<Y>();
      ((HasFeatureCache) context).putResultInCache(this, featureResult, env);
    }
  }

  protected abstract FeatureResult<Y> checkInternal(T context, RuntimeEnvironment env) throws TalismaneException;
}
