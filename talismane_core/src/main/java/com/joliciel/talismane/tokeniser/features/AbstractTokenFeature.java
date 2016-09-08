///////////////////////////////////////////////////////////////////////////////
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
package com.joliciel.talismane.tokeniser.features;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.machineLearning.features.AbstractCachableFeature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;

/**
 * An Abstract base class for intrinsic features.
 * @author Assaf Urieli
 *
 */
public abstract class AbstractTokenFeature<Y> extends AbstractCachableFeature<TokenWrapper,Y> implements TokenFeature<Y> {
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(AbstractTokenFeature.class);
	private TokenAddressFunction<TokenWrapper> addressFunction;
	
	@Override
	protected final FeatureResult<Y> checkInCache(TokenWrapper context, RuntimeEnvironment env) {
		return context.getToken().getResultFromCache(this, env);
	}

	@Override
	protected final void putInCache(TokenWrapper context, FeatureResult<Y> result, RuntimeEnvironment env) {
		context.getToken().putResultInCache(this, result, env);
	}

	protected TokenAddressFunction<TokenWrapper> getAddressFunction() {
		return addressFunction;
	}

	protected void setAddressFunction(TokenAddressFunction<TokenWrapper> addressFunction) {
		this.addressFunction = addressFunction;
		String name = this.getName();
		if (name.endsWith(")")) {
			name = name.substring(0, name.length()-1) + "," + addressFunction.getName() + ")";
		} else {
			name = name + "(" + addressFunction.getName() + ")";
		}
		this.setName(name);
	}

	protected TokenWrapper getToken(TokenWrapper tokenWrapper, RuntimeEnvironment env) {
		if (this.addressFunction==null) {
			return tokenWrapper;
		} else {
			FeatureResult<TokenWrapper> tokenResult = addressFunction.check(tokenWrapper, env);
			if (tokenResult==null)
				return null;
			return tokenResult.getOutcome();
		}
	}
	
}
