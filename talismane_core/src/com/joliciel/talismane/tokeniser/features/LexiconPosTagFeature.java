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


import com.joliciel.talismane.machineLearning.features.BooleanFeature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenWrapper;

/**
 * Returns true if the token has a lexical entry for the PosTag provided.
 * @author Assaf Urieli
 *
 */
public class LexiconPosTagFeature extends AbstractTokenFeature<Boolean> implements BooleanFeature<TokenWrapper> {
	private PosTag posTag;
	
	/**
	 * 
	 * @param posTag the PosTag we're testing for
	 */
	public LexiconPosTagFeature(PosTag posTag) {
		this.posTag = posTag;
		this.setName(super.getName() + "(" + this.posTag.getCode() + ")");
	}
	
	@Override
	public FeatureResult<Boolean> checkInternal(TokenWrapper tokenWrapper) {
		Token token = tokenWrapper.getToken();
		FeatureResult<Boolean> result = null;

		boolean hasPosTag = (token.getPossiblePosTags().contains(posTag));
		result = this.generateResult(hasPosTag);
		
		return result;
	}

}
