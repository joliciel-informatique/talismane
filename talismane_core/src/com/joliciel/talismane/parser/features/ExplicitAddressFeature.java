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

import com.joliciel.talismane.machineLearning.features.AbstractCachableFeature;
import com.joliciel.talismane.machineLearning.features.Feature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.posTagger.features.PosTaggedTokenFeature;

/**
 * Used when the address of the pos-tagged token for a given ParseConfigurationAddressFeature
 * or PosTaggedTokenFeature is provided explicitly in the feature descriptor
 * (as opposed to being provided implicitly via the wrapping function).
 * @author Assaf Urieli
 *
 */
public class ExplicitAddressFeature<T> extends AbstractCachableFeature<ParseConfigurationWrapper,T>
	implements ParseConfigurationFeature<T> {

	private AddressFunction addressFunction;
	private ParseConfigurationAddressFeature<T> parseConfigurationAddressFeature = null;
	private PosTaggedTokenFeature<T> posTaggedTokenFeature = null;
	private boolean isParseConfigurationAddress = false;
	
	// keeping this at instance level is for performance reasons only
	// it is safe, as only one configuration is tested at a time
//	private ParseConfigurationAddress parseConfigurationAddress = new ParseConfigurationAddress();
	
	public ExplicitAddressFeature(PosTaggedTokenFeature<T> posTaggedTokenFeature, AddressFunction addressFunction) {
		super();
		this.addressFunction = addressFunction;
//		parseConfigurationAddress.setAddressFunction(addressFunction);
		this.posTaggedTokenFeature = posTaggedTokenFeature;
		this.setNameFromWrappedFeature(this.posTaggedTokenFeature.getName());
		isParseConfigurationAddress = false;
	}
	
	public ExplicitAddressFeature(ParseConfigurationAddressFeature<T> parseConfigurationAddressFeature, AddressFunction addressFunction) {
		super();
		this.addressFunction = addressFunction;
//		parseConfigurationAddress.setAddressFunction(addressFunction);
		this.parseConfigurationAddressFeature = parseConfigurationAddressFeature;
		this.setNameFromWrappedFeature(this.parseConfigurationAddressFeature.getName());
		isParseConfigurationAddress = true;
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
	public FeatureResult<T> checkInternal(ParseConfigurationWrapper wrapper, RuntimeEnvironment env) {
		ParseConfiguration configuration = wrapper.getParseConfiguration();
		
		ParseConfigurationAddress parseConfigurationAddress = new ParseConfigurationAddress(env);
		parseConfigurationAddress.setAddressFunction(addressFunction);
		parseConfigurationAddress.setParseConfiguration(configuration);
		
		FeatureResult<T> result = null;
		FeatureResult<T> internalResult = null;
		if (isParseConfigurationAddress)
			internalResult = this.parseConfigurationAddressFeature.check(parseConfigurationAddress, env);
		else
			internalResult = this.posTaggedTokenFeature.check(parseConfigurationAddress, env);
		
		if (internalResult!=null) {
			result = this.generateResult(internalResult.getOutcome());
		}
		return result;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class<? extends Feature> getFeatureType() {
		if (isParseConfigurationAddress)
			return parseConfigurationAddressFeature.getFeatureType();
		else
			return posTaggedTokenFeature.getFeatureType();
	}


	@Override
	protected FeatureResult<T> checkInCache(ParseConfigurationWrapper context, RuntimeEnvironment env) {
		return context.getParseConfiguration().getResultFromCache(this, env);
	}


	@Override
	protected void putInCache(ParseConfigurationWrapper context,
			FeatureResult<T> featureResult, RuntimeEnvironment env) {
		context.getParseConfiguration().putResultInCache(this, featureResult, env);
	}	
	
	
}
