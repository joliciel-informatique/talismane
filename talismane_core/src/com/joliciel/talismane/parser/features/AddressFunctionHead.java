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

import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.posTagger.PosTaggedToken;

/**
 * Retrieves the head (or governor) of the reference token.
 * @author Assaf Urieli
 *
 */
public class AddressFunctionHead extends AbstractAddressFunction {
	private AddressFunction addressFunction;
	
	public AddressFunctionHead(AddressFunction addressFunction) {
		super();
		this.addressFunction = addressFunction;
		this.setName("Head(" + addressFunction.getName() + ")");
	}

	@Override
	public FeatureResult<PosTaggedToken> checkInternal(ParseConfigurationWrapper wrapper) {
		ParseConfiguration configuration = wrapper.getParseConfiguration();
		PosTaggedToken resultToken = null;
		FeatureResult<PosTaggedToken> addressResult = addressFunction.check(configuration);
		if (addressResult!=null) {
			PosTaggedToken referenceToken = addressResult.getOutcome();
			resultToken = configuration.getHead(referenceToken);
		}

		FeatureResult<PosTaggedToken> featureResult = null;
		if (resultToken!=null)
			featureResult = this.generateResult(resultToken);
		return featureResult;
	}
}
