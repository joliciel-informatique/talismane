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

import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.utils.features.FeatureResult;
import com.joliciel.talismane.utils.features.StringFeature;

/**
 * The actual text of a given token, referenced by address.
 * @author Assaf Urieli
 *
 */
public class ParserLexicalFormFeature extends AbstractParseConfigurationAddressFeature<String> implements StringFeature<ParseConfigurationAddress> {
	public ParserLexicalFormFeature() {
		super();
		this.setName(super.getName());
	}

	@Override
	public FeatureResult<String> checkInternal(ParseConfigurationAddress parseConfigurationAddress) {
		ParseConfiguration configuration = parseConfigurationAddress.getParseConfiguration();
		AddressFunction addressFunction = parseConfigurationAddress.getAddressFunction();
		FeatureResult<PosTaggedToken> tokenResult = addressFunction.check(configuration);
		FeatureResult<String> featureResult = null;
		if (tokenResult!=null) {
			PosTaggedToken posTaggedToken = tokenResult.getOutcome();
			featureResult = this.generateResult(posTaggedToken.getToken().getText());
		}
		return featureResult;
	}
	
	
}
