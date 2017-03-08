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
package com.joliciel.talismane.parser.features;

import java.util.List;

import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.posTagger.features.PosTaggedTokenAddressFunction;
import com.joliciel.talismane.posTagger.features.PosTaggedTokenWrapper;

/**
 * Retrieves the right-most right-hand dependent of the reference token.
 * 
 * @author Assaf Urieli
 *
 */
public final class AddressFunctionRDep extends AbstractAddressFunction {
	private PosTaggedTokenAddressFunction<ParseConfigurationWrapper> addressFunction;

	public AddressFunctionRDep(PosTaggedTokenAddressFunction<ParseConfigurationWrapper> addressFunction) {
		super();
		this.addressFunction = addressFunction;
		this.setName("RDep(" + addressFunction.getName() + ")");
	}

	@Override
	public FeatureResult<PosTaggedTokenWrapper> check(ParseConfigurationWrapper wrapper, RuntimeEnvironment env) {
		ParseConfiguration configuration = wrapper.getParseConfiguration();
		PosTaggedToken resultToken = null;
		FeatureResult<PosTaggedTokenWrapper> addressResult = addressFunction.check(wrapper, env);
		if (addressResult != null) {
			PosTaggedToken referenceToken = addressResult.getOutcome().getPosTaggedToken();
			List<PosTaggedToken> rightDependents = configuration.getRightDependents(referenceToken);
			if (rightDependents.size() > 0)
				resultToken = rightDependents.get(rightDependents.size() - 1);
		}

		FeatureResult<PosTaggedTokenWrapper> featureResult = null;
		if (resultToken != null)
			featureResult = this.generateResult(resultToken);
		return featureResult;
	}
}
