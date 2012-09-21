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
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.posTagger.PosTaggedToken;

/**
 * Retrieves the token offset from the current token by <i>n</i> (in the linear sentence),
 * where <i>n</i> can be negative (before the current token) or positive (after the current token).<br/>
 * The "current token" is returned by the address function.
 * @author Assaf Urieli
 *
 */
public class AddressFunctionOffset extends AbstractAddressFunction {
	private AddressFunction addressFunction;
	private IntegerFeature<ParseConfiguration> offsetFeature;
	
	public AddressFunctionOffset(AddressFunction addressFunction, IntegerFeature<ParseConfiguration> offsetFeature) {
		super();
		this.addressFunction = addressFunction;
		this.offsetFeature = offsetFeature;
		this.setName("Offset(" + addressFunction.getName() + "," + offsetFeature.getName() + ")");
	}

	@Override
	public FeatureResult<PosTaggedToken> checkInternal(ParseConfiguration configuration) {
		PosTaggedToken resultToken = null;
		FeatureResult<PosTaggedToken> addressResult = addressFunction.check(configuration);
		FeatureResult<Integer> offsetResult = offsetFeature.check(configuration);
		if (addressResult!=null && offsetResult!=null) {
			int offset = offsetResult.getOutcome();
			PosTaggedToken referenceToken = addressResult.getOutcome();
			
			int refIndex = referenceToken.getToken().getIndex();
			int index = refIndex + offset;
			if (index>=0 && index<configuration.getPosTagSequence().size()) {
				resultToken = configuration.getPosTagSequence().get(index);
			}
		}

		FeatureResult<PosTaggedToken> featureResult = null;
		if (resultToken!=null)
			featureResult = this.generateResult(resultToken);
		return featureResult;
	}
}
