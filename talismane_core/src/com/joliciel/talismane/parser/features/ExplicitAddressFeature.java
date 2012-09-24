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
import com.joliciel.talismane.parser.ParseConfiguration;

/**
 * Passes an explicit address to a ParseConfigurationAddressFeature.
 * @author Assaf Urieli
 *
 */
public class ExplicitAddressFeature<T> extends AbstractCachableFeature<ParseConfiguration,T>
	implements ParseConfigurationFeature<T> {
	@SuppressWarnings("unused")
	private AddressFunction addressFunction;
	private ParseConfigurationAddressFeature<T> parseConfigurationAddressFeature = null;
	
	// keeping this at instance level is for performance reasons only
	// it is safe, as only one configuration is tested at a time
	private ParseConfigurationAddress parseConfigurationAddress = new ParseConfigurationAddress();
	
	public ExplicitAddressFeature(ParseConfigurationAddressFeature<T> parseConfigurationAddressFeature, AddressFunction addressFunction) {
		super();
		this.addressFunction = addressFunction;
		this.parseConfigurationAddressFeature = parseConfigurationAddressFeature;
		String originalName = parseConfigurationAddressFeature.getName();
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
		parseConfigurationAddress.setAddressFunction(addressFunction);
	}

	
	@Override
	public FeatureResult<T> checkInternal(ParseConfiguration configuration) {
		parseConfigurationAddress.setParseConfiguration(configuration);
		
		FeatureResult<T> internalResult = this.parseConfigurationAddressFeature.check(parseConfigurationAddress);
		FeatureResult<T> result = null;
		if (internalResult!=null) {
			result = this.generateResult(internalResult.getOutcome());
		}
		return result;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class<? extends Feature> getFeatureType() {
		return parseConfigurationAddressFeature.getFeatureType();
	}


	@Override
	protected FeatureResult<T> checkInCache(ParseConfiguration context) {
		return context.getResultFromCache(this);
	}


	@Override
	protected void putInCache(ParseConfiguration context,
			FeatureResult<T> featureResult) {
		context.putResultInCache(this, featureResult);
	}	
	
	
}
