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
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.tokeniser.Token;

/**
 * Returns true if this is the last word in the sentence (including punctuation).
 * @author Assaf Urieli
 *
 */
public final class LastWordInSentenceFeature extends AbstractTokenFeature<Boolean> implements BooleanFeature<TokenWrapper> {	
	public LastWordInSentenceFeature() {
	}
	
	public LastWordInSentenceFeature(TokenAddressFunction<TokenWrapper> addressFunction) {
		this.setAddressFunction(addressFunction);
	}
	
	@Override
	public FeatureResult<Boolean> checkInternal(TokenWrapper tokenWrapper, RuntimeEnvironment env) {
		TokenWrapper innerWrapper = this.getToken(tokenWrapper, env);
		if (innerWrapper==null)
			return null;
		Token token = innerWrapper.getToken();
		FeatureResult<Boolean> result = null;
		
		boolean lastWord = (token.getIndex()==token.getTokenSequence().size()-1);
		result = this.generateResult(lastWord);
		
		return result;
	}
}
