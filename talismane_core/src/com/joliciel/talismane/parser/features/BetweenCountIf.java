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
import com.joliciel.talismane.machineLearning.features.IntegerLiteralFeature;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.posTagger.PosTaggedToken;

/**
 * Returns the number of pos-tagged tokens between two pos-tagged tokens (and not including them) matching a certain criterion.
 * @author Assaf Urieli
 *
 */
public class BetweenCountIf extends AbstractParseConfigurationFeature<Integer> implements IntegerFeature<ParseConfigurationWrapper> {
	private AddressFunction addressFunction1;
	private AddressFunction addressFunction2;
	private BooleanFeature<ParseConfigurationAddress> criterion;
	
	public BetweenCountIf(AddressFunction addressFunction1, AddressFunction addressFunction2,
			BooleanFeature<ParseConfigurationAddress> criterion) {
		super();
		this.addressFunction1 = addressFunction1;
		this.addressFunction2 = addressFunction2;
		this.criterion = criterion;
		this.setName(super.getName() + "(" + this.addressFunction1.getName() + "," + this.addressFunction2.getName() + "," + this.criterion.getName() + ")");
	}

	@Override
	public FeatureResult<Integer> checkInternal(ParseConfigurationWrapper wrapper) {
		ParseConfiguration configuration = wrapper.getParseConfiguration();
		FeatureResult<PosTaggedToken> tokenResult1 = addressFunction1.check(configuration);
		FeatureResult<PosTaggedToken> tokenResult2 = addressFunction2.check(configuration);
		FeatureResult<Integer> featureResult = null;
		if (tokenResult1!=null && tokenResult2!=null) {
			PosTaggedToken posTaggedToken1 = tokenResult1.getOutcome();
			PosTaggedToken posTaggedToken2 = tokenResult2.getOutcome();
			int index1 = posTaggedToken1.getToken().getIndex();
			int index2 = posTaggedToken2.getToken().getIndex();
			
			int minIndex = index1 < index2 ? index1 : index2;
			int maxIndex = index1 >= index2 ? index1 : index2;
			
			int countMatching = 0;
			
			for (int i=minIndex+1; i<maxIndex; i++) {
				IntegerFeature<ParseConfiguration> indexFeature = new IntegerLiteralFeature<ParseConfiguration>(i);
				AddressFunction indexFunction = new AddressFunctionSequence(indexFeature);
				ParseConfigurationAddress parseConfigurationAddress = new ParseConfigurationAddress(configuration, indexFunction);
				FeatureResult<Boolean> criterionResult = criterion.check(parseConfigurationAddress);
				if (criterionResult!=null && criterionResult.getOutcome())
					countMatching++;
			}
			featureResult = this.generateResult(countMatching);
		}
		return featureResult;
	}

}
