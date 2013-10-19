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
import com.joliciel.talismane.machineLearning.features.StringFeature;
import com.joliciel.talismane.tokeniser.Token;

/**
 * Returns true if token word is any one of the words provided.<br/>
 * @author Assaf Urieli
 *
 */
public final class WordFeature extends AbstractTokenFeature<Boolean> implements BooleanFeature<TokenWrapper> {
	StringFeature<TokenWrapper>[] words = null;

	public WordFeature(StringFeature<TokenWrapper>... words) {
		this.words = words;
		String name = "Word(";
		boolean firstWord = true;
		for (StringFeature<TokenWrapper> word : words) {
			if (!firstWord) name += ",";
			name += word.getName();
			firstWord = false;
		}
		name += ")";
		this.setName(name);
	}
	
	public WordFeature(TokenAddressFunction<TokenWrapper> addressFunction, StringFeature<TokenWrapper>... words) {
		this(words);
		this.setAddressFunction(addressFunction);
	}
	
	@Override
	public FeatureResult<Boolean> checkInternal(TokenWrapper tokenWrapper, RuntimeEnvironment env) {
		TokenWrapper innerWrapper = this.getToken(tokenWrapper, env);
		if (innerWrapper==null)
			return null;
		Token token = innerWrapper.getToken();
		FeatureResult<Boolean> result = null;

		boolean matches = false;
		for (StringFeature<TokenWrapper> word : words) {
			FeatureResult<String> wordResult = word.check(innerWrapper, env);
			if (wordResult!=null) {
				String wordText = wordResult.getOutcome();
				if (wordText.equals(token.getText())) {
					matches = true;
					break;
				}
			}
		}
		result = this.generateResult(matches);
		
		return result;
	}

	public StringFeature<TokenWrapper>[] getWords() {
		return words;
	}
}
