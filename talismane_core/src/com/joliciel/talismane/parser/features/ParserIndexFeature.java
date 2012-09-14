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
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.utils.features.FeatureResult;
import com.joliciel.talismane.utils.features.IntegerFeature;

/**
 * The index of a given token in the token sequence, referenced by address.
 * @author Assaf Urieli
 *
 */
public class ParserIndexFeature extends AbstractParseConfigurationAddressFeature<Integer> implements IntegerFeature<ParseConfigurationAddress> {
	public ParserIndexFeature() {
		super();
		this.setName(super.getName());
	}

	@Override
	public FeatureResult<Integer> checkInternal(ParseConfigurationAddress parseConfigurationAddress) {
		ParseConfiguration configuration = parseConfigurationAddress.getParseConfiguration();
		AddressFunction addressFunction = parseConfigurationAddress.getAddressFunction();
		FeatureResult<PosTaggedToken> tokenResult = addressFunction.check(configuration);
		FeatureResult<Integer> featureResult = null;
		if (tokenResult!=null) {
			PosTaggedToken posTaggedToken = tokenResult.getOutcome();
			Token token = posTaggedToken.getToken();
			int index = token.getIndex();
			featureResult = this.generateResult(index);
		}
		return featureResult;
	}
	
	
}
