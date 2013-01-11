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

import com.joliciel.talismane.machineLearning.features.AbstractCachableFeature;
import com.joliciel.talismane.machineLearning.features.Feature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.tokeniser.Token;

/**
 * Used to apply a {@link TokenFeature} to a specific token referenced by a {@link TokenAddressFunction},
 * instead of applying it to the current token.
 * @author Assaf Urieli
 *
 */
public class TokenReferenceFeature<T> extends AbstractCachableFeature<TokenWrapper,T>
	implements TokenFeature<T> {

	private TokenAddressFunction addressFunction;
	private TokenFeature<T> tokenFeature = null;
	
	public TokenReferenceFeature(TokenAddressFunction addressFunction, TokenFeature<T> tokenFeature) {
		super();
		this.addressFunction = addressFunction;
		this.tokenFeature = tokenFeature;
		this.setNameFromWrappedFeature(this.tokenFeature.getName());
	}
	
	private void setNameFromWrappedFeature(String originalName) {
		int openParenIndex = originalName.indexOf('(');
		int closeParenIndex = originalName.indexOf(')');
		String newName = "";
		if (openParenIndex>=0) {
			newName = originalName.substring(0, openParenIndex) + "(" + addressFunction.getName();
			if (closeParenIndex-openParenIndex > 1)
				newName += ",";
			newName += originalName.substring(openParenIndex+1);
		} else {
			newName = originalName + "(" + addressFunction.getName() + ")";
		}
		this.setName(newName);		
	}
	
	@Override
	public FeatureResult<T> checkInternal(TokenWrapper wrapper) {
		FeatureResult<T> result = null;
		FeatureResult<T> internalResult = null;
		
		FeatureResult<Token> tokenResult = addressFunction.check(wrapper);
		if (tokenResult!=null) {
			Token referencedToken = tokenResult.getOutcome();
			internalResult = this.tokenFeature.check(referencedToken);
			
			if (internalResult!=null) {
				result = this.generateResult(internalResult.getOutcome());
			}
		}
		return result;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class<? extends Feature> getFeatureType() {
		return tokenFeature.getFeatureType();
	}

	@Override
	protected FeatureResult<T> checkInCache(TokenWrapper context) {
		return context.getToken().getResultFromCache(this);
	}

	@Override
	protected void putInCache(TokenWrapper context,
			FeatureResult<T> featureResult) {
		context.getToken().putResultInCache(this, featureResult);
	}	
	
	
}
