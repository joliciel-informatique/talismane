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
package com.joliciel.talismane.tokeniser.features;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.tokeniser.TokenWrapper;
import com.joliciel.talismane.utils.features.AbstractCachableFeature;
import com.joliciel.talismane.utils.features.FeatureResult;

/**
 * An Abstract base class for intrinsic features.
 * @author Assaf Urieli
 *
 */
public abstract class AbstractTokenFeature<Y> extends AbstractCachableFeature<TokenWrapper,Y> implements TokenFeature<Y> {
	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(AbstractTokenFeature.class);

	@Override
	protected FeatureResult<Y> checkInCache(TokenWrapper context) {
		return context.getToken().getResultFromCache(this);
	}

	@Override
	protected void putInCache(TokenWrapper context, FeatureResult<Y> result) {
		context.getToken().putResultInCache(this, result);
	}

	@Override
	public boolean isDynamic() {
		return false;
	}

}
