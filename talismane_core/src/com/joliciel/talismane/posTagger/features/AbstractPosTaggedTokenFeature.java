///////////////////////////////////////////////////////////////////////////////
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
package com.joliciel.talismane.posTagger.features;

import com.joliciel.talismane.machineLearning.features.AbstractCachableFeature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;

/**
 * An Abstract base class for features applied to a given pos-tagged token.
 * @author Assaf Urieli
 *
 */
public abstract class AbstractPosTaggedTokenFeature<T> extends AbstractCachableFeature<PosTaggedTokenWrapper,T> implements PosTaggedTokenFeature<T> {

	@Override
	protected final FeatureResult<T> checkInCache(PosTaggedTokenWrapper context) {
		if (context.getPosTaggedToken()!=null)
			return context.getPosTaggedToken().getResultFromCache(this);
		return null;
	}

	@Override
	protected final void putInCache(PosTaggedTokenWrapper context,
			FeatureResult<T> featureResult) {
		if (context.getPosTaggedToken()!=null)
			context.getPosTaggedToken().putResultInCache(this, featureResult);
	}

}
