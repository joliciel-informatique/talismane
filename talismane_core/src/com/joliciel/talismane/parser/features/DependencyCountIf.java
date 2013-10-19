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

import com.joliciel.talismane.machineLearning.features.BooleanFeature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.posTagger.features.PosTaggedTokenAddressFunction;
import com.joliciel.talismane.posTagger.features.PosTaggedTokenWrapper;

/**
 * Returns the number of dependents already matching a certain criterion.
 * @author Assaf Urieli
 *
 */
public final class DependencyCountIf extends AbstractParseConfigurationFeature<Integer> implements IntegerFeature<ParseConfigurationWrapper> {
	private PosTaggedTokenAddressFunction<ParseConfigurationWrapper> addressFunction;
	private BooleanFeature<ParseConfigurationAddress> criterion;
	
	public DependencyCountIf(PosTaggedTokenAddressFunction<ParseConfigurationWrapper> addressFunction,
			BooleanFeature<ParseConfigurationAddress> criterion) {
		super();
		this.addressFunction = addressFunction;
		this.criterion = criterion;
		this.setName(super.getName() + "(" + this.addressFunction.getName() + "," + this.criterion.getName() + ")");
	}

	@Override
	protected FeatureResult<Integer> checkInternal(ParseConfigurationWrapper wrapper, RuntimeEnvironment env) {
		ParseConfiguration configuration = wrapper.getParseConfiguration();		
		FeatureResult<PosTaggedTokenWrapper> tokenResult = addressFunction.check(wrapper, env);
		FeatureResult<Integer> featureResult = null;
		if (tokenResult!=null) {
			PosTaggedToken posTaggedToken = tokenResult.getOutcome().getPosTaggedToken();
			int countMatching = 0;
			for (PosTaggedToken dependent : configuration.getDependents(posTaggedToken)) {
				ParseConfigurationAddress parseConfigurationAddress = new ParseConfigurationAddress(env);
				parseConfigurationAddress.setParseConfiguration(configuration);
				parseConfigurationAddress.setPosTaggedToken(dependent);
				FeatureResult<Boolean> criterionResult = criterion.check(parseConfigurationAddress, env);
				if (criterionResult!=null && criterionResult.getOutcome())
					countMatching++;
			}
			featureResult = this.generateResult(countMatching);
		}
		return featureResult;
	}

}
