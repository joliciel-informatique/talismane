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
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.utils.WeightedOutcome;

/**
 * A StringCollectionFeature returning all of the postags for the current token in the lexicon.
 * @author Assaf Urieli
 *
 */
public final class LexiconPosTagsFeature extends AbstractTokenFeature<List<WeightedOutcome<String>>> implements StringCollectionFeature<TokenWrapper> {

	public LexiconPosTagsFeature() {}
	
	public LexiconPosTagsFeature(TokenAddressFunction<TokenWrapper> addressFunction) {
		this.setAddressFunction(addressFunction);
	}

	@Override
	public FeatureResult<List<WeightedOutcome<String>>> checkInternal(
			TokenWrapper tokenWrapper, RuntimeEnvironment env) {
		TokenWrapper innerWrapper = this.getToken(tokenWrapper, env);
		if (innerWrapper==null)
			return null;
		Token token = innerWrapper.getToken();
		FeatureResult<List<WeightedOutcome<String>>> result = null;
		List<WeightedOutcome<String>> resultList = new ArrayList<WeightedOutcome<String>>();

		for (PosTag posTag : token.getPossiblePosTags()) {
			resultList.add(new WeightedOutcome<String>(posTag.getCode(), 1.0));
		}

		if (resultList.size()>0)
			result = this.generateResult(resultList);
		
		return result;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class<? extends Feature> getFeatureType() {
		return StringCollectionFeature.class;
	}
}
