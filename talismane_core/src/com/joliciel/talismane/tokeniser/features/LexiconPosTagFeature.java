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
package com.joliciel.talismane.tokeniser.features;


import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.machineLearning.features.BooleanFeature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.machineLearning.features.StringFeature;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.tokeniser.Token;

/**
 * Returns true if the token has a lexical entry for the PosTag provided.
 * @author Assaf Urieli
 *
 */
public class LexiconPosTagFeature extends AbstractTokenFeature<Boolean> implements BooleanFeature<TokenWrapper> {
	StringFeature<TokenWrapper> posTagFeature;
	
	/**
	 * 
	 * @param posTag the PosTag we're testing for
	 */
	public LexiconPosTagFeature(StringFeature<TokenWrapper> posTagFeature) {
		this.posTagFeature = posTagFeature;
		this.setName(super.getName() + "(" + this.posTagFeature.getName() + ")");
	}
	
	@Override
	public FeatureResult<Boolean> checkInternal(TokenWrapper tokenWrapper, RuntimeEnvironment env) {
		Token token = tokenWrapper.getToken();
		FeatureResult<Boolean> result = null;

		FeatureResult<String> posTagResult = posTagFeature.check(tokenWrapper, env);
		if (posTagResult!=null) {
			PosTag posTag = TalismaneSession.getPosTagSet().getPosTag(posTagResult.getOutcome());
			boolean hasPosTag = (token.getPossiblePosTags().contains(posTag));
			result = this.generateResult(hasPosTag);
		}
		
		return result;
	}

}
