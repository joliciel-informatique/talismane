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

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.machineLearning.features.BooleanFeature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.talismane.machineLearning.features.IntegerLiteralFeature;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.posTagger.features.PosTaggedTokenAddressFunction;
import com.joliciel.talismane.posTagger.features.PosTaggedTokenWrapper;

/**
 * Returns the number of pos-tagged tokens between two pos-tagged tokens (and
 * not including them) matching a certain criterion.
 * 
 * @author Assaf Urieli
 *
 */
public final class BetweenCountIf extends AbstractParseConfigurationFeature<Integer>implements IntegerFeature<ParseConfigurationWrapper> {
	private PosTaggedTokenAddressFunction<ParseConfigurationWrapper> addressFunction1;
	private PosTaggedTokenAddressFunction<ParseConfigurationWrapper> addressFunction2;
	private BooleanFeature<ParseConfigurationAddress> criterion;

	public BetweenCountIf(PosTaggedTokenAddressFunction<ParseConfigurationWrapper> addressFunction1,
			PosTaggedTokenAddressFunction<ParseConfigurationWrapper> addressFunction2, BooleanFeature<ParseConfigurationAddress> criterion) {
		super();
		this.addressFunction1 = addressFunction1;
		this.addressFunction2 = addressFunction2;
		this.criterion = criterion;
		this.setName(super.getName() + "(" + this.addressFunction1.getName() + "," + this.addressFunction2.getName() + "," + this.criterion.getName() + ")");
	}

	@Override
	public FeatureResult<Integer> check(ParseConfigurationWrapper wrapper, RuntimeEnvironment env) throws TalismaneException {
		ParseConfiguration configuration = wrapper.getParseConfiguration();
		FeatureResult<PosTaggedTokenWrapper> tokenResult1 = addressFunction1.check(wrapper, env);
		FeatureResult<PosTaggedTokenWrapper> tokenResult2 = addressFunction2.check(wrapper, env);
		FeatureResult<Integer> featureResult = null;
		if (tokenResult1 != null && tokenResult2 != null) {
			PosTaggedToken posTaggedToken1 = tokenResult1.getOutcome().getPosTaggedToken();
			PosTaggedToken posTaggedToken2 = tokenResult2.getOutcome().getPosTaggedToken();
			int index1 = posTaggedToken1.getToken().getIndex();
			int index2 = posTaggedToken2.getToken().getIndex();

			int minIndex = index1 < index2 ? index1 : index2;
			int maxIndex = index1 >= index2 ? index1 : index2;

			int countMatching = 0;

			for (int i = minIndex + 1; i < maxIndex; i++) {
				IntegerFeature<ParseConfigurationWrapper> indexFeature = new IntegerLiteralFeature<ParseConfigurationWrapper>(i);
				PosTaggedTokenAddressFunction<ParseConfigurationWrapper> indexFunction = new AddressFunctionSequence(indexFeature);
				ParseConfigurationAddress parseConfigurationAddress = new ParseConfigurationAddress(configuration, indexFunction, env);
				FeatureResult<Boolean> criterionResult = criterion.check(parseConfigurationAddress, env);
				if (criterionResult != null && criterionResult.getOutcome())
					countMatching++;
			}
			featureResult = this.generateResult(countMatching);
		}
		return featureResult;
	}

}
