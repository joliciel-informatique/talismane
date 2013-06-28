///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2013 Assaf Urieli
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
package com.joliciel.talismane.tokeniser.features;

import java.util.ArrayList;
import java.util.List;

import com.joliciel.talismane.machineLearning.features.Feature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.machineLearning.features.StringCollectionFeature;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.patterns.TokenMatch;
import com.joliciel.talismane.utils.WeightedOutcome;

/**
 * A StringCollectionFeature returning all of the patterns in which the current token
 * is not in position 0.
 * @author Assaf Urieli
 *
 */
public final class TokeniserPatternsAndIndexesFeature extends AbstractTokenFeature<List<WeightedOutcome<String>>> implements StringCollectionFeature<TokenWrapper> {

	@Override
	public FeatureResult<List<WeightedOutcome<String>>> checkInternal(
			TokenWrapper tokenWrapper, RuntimeEnvironment env) {
		Token token = tokenWrapper.getToken();
		List<WeightedOutcome<String>> resultList = new ArrayList<WeightedOutcome<String>>();
		for (TokenMatch tokenMatch : token.getMatches()) {
			if (tokenMatch.getIndex()!=tokenMatch.getPattern().getIndexesToTest().get(0)) {
				resultList.add(new WeightedOutcome<String>(tokenMatch.getPattern().getName() + "¤" + tokenMatch.getIndex(), 1.0));
			}
		}
		
		return this.generateResult(resultList);
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public Class<? extends Feature> getFeatureType() {
		return StringCollectionFeature.class;
	}
}
