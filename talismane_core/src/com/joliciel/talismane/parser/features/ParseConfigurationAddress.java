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
package com.joliciel.talismane.parser.features;

import com.joliciel.talismane.machineLearning.features.Feature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.HasFeatureCache;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.posTagger.features.PosTaggedTokenWrapper;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.features.TokenWrapper;

/**
 * A simple container for a ParseConfiguration + an address function.
 * @author Assaf Urieli
 *
 */
public final class ParseConfigurationAddress implements ParseConfigurationWrapper, PosTaggedTokenWrapper, TokenWrapper, HasFeatureCache {
	private ParseConfiguration parseConfiguration;
	private AddressFunction addressFunction;
	private PosTaggedToken posTaggedToken = null;
	private boolean posTaggedTokenRetrieved = false;
	public ParseConfigurationAddress() {
		
	}
	
	public ParseConfigurationAddress(ParseConfiguration parseConfiguration,
			AddressFunction addressFunction) {
		super();
		this.parseConfiguration = parseConfiguration;
		this.addressFunction = addressFunction;
	}
	public ParseConfiguration getParseConfiguration() {
		return parseConfiguration;
	}
	public AddressFunction getAddressFunction() {
		return addressFunction;
	}

	public void setParseConfiguration(ParseConfiguration parseConfiguration) {
		this.parseConfiguration = parseConfiguration;
	}

	public void setAddressFunction(AddressFunction addressFunction) {
		this.addressFunction = addressFunction;
	}

	@Override
	public PosTaggedToken getPosTaggedToken() {
		if (!posTaggedTokenRetrieved) {
			FeatureResult<PosTaggedToken> featureResult = this.addressFunction.check(this.parseConfiguration);
			if (featureResult!=null)
				posTaggedToken = featureResult.getOutcome();
			posTaggedTokenRetrieved = true;
		}
		return posTaggedToken;
	}

	public void setPosTaggedToken(PosTaggedToken posTaggedToken) {
		this.posTaggedToken = posTaggedToken;
		this.posTaggedTokenRetrieved = true;
	}

	@Override
	public <T,Y> FeatureResult<Y> getResultFromCache(
			Feature<T, Y> feature) {
		PosTaggedToken posTaggedToken = this.getPosTaggedToken();
		if (posTaggedToken!=null)
			return posTaggedToken.getResultFromCache(feature);
		return null;
	}

	@Override
	public <T,Y> void putResultInCache(
			Feature<T, Y> feature,
			FeatureResult<Y> featureResult) {
		PosTaggedToken posTaggedToken = this.getPosTaggedToken();
		if (posTaggedToken!=null)
			posTaggedToken.putResultInCache(feature, featureResult);
	}

	@Override
	public Token getToken() {
		PosTaggedToken posTaggedToken = this.getPosTaggedToken();
		Token token = null;
		if (posTaggedToken!=null)
			token = posTaggedToken.getToken();
		return token;
	}
	
	
}
