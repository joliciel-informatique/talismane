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

import java.util.List;

import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.posTagger.PosTaggedToken;

/**
 * Retrieves the nth dependent of the reference token.
 * @author Assaf Urieli
 *
 */
public class AddressFunctionDep extends AbstractAddressFunction {
	private AddressFunction addressFunction;
	private IntegerFeature<ParseConfiguration> indexFeature;
	
	public AddressFunctionDep(AddressFunction addressFunction, IntegerFeature<ParseConfiguration> indexFeature) {
		super();
		this.addressFunction = addressFunction;
		this.indexFeature = indexFeature;
		this.setName("Dep(" + addressFunction.getName() + "," + indexFeature.getName() + ")");
	}

	@Override
	public FeatureResult<PosTaggedToken> checkInternal(ParseConfigurationWrapper wrapper) {
		ParseConfiguration configuration = wrapper.getParseConfiguration();
		PosTaggedToken resultToken = null;
		FeatureResult<PosTaggedToken> addressResult = addressFunction.check(configuration);
		FeatureResult<Integer> indexResult = indexFeature.check(configuration);
		if (addressResult!=null && indexResult!=null) {
			int index = indexResult.getOutcome();
			PosTaggedToken referenceToken = addressResult.getOutcome();
			if (referenceToken!=null) {
				List<PosTaggedToken> dependents = configuration.getDependents(referenceToken);
				if (dependents.size()>index)
					resultToken = dependents.get(index);
			}
		}

		FeatureResult<PosTaggedToken> featureResult = null;
		if (resultToken!=null)
			featureResult = this.generateResult(resultToken);
		return featureResult;
	}
}