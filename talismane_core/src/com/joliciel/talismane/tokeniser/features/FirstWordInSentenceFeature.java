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
import com.joliciel.talismane.tokeniser.TokenSequence;

/**
 * Returns true if this is the first word in the sentence.<br/>
 * Will skip initial punctuation (e.g. quotation marks) or numbered lists, returning true for the
 * first word following such constructs.
 * @author Assaf Urieli
 *
 */
public class FirstWordInSentenceFeature extends AbstractTokenFeature<Boolean> implements BooleanFeature<TokenWrapper> {	
	public FirstWordInSentenceFeature() {
	}
	
	public FirstWordInSentenceFeature(TokenAddressFunction<TokenWrapper> addressFunction) {
		this.setAddressFunction(addressFunction);
	}
	
	@Override
	public FeatureResult<Boolean> checkInternal(TokenWrapper tokenWrapper, RuntimeEnvironment env) {
		TokenWrapper innerWrapper = this.getToken(tokenWrapper, env);
		if (innerWrapper==null)
			return null;
		Token token = innerWrapper.getToken();
		
		FeatureResult<Boolean> result = null;
		
		boolean firstWord = (token.getIndex()==0);
		if (!firstWord) {
			TokenSequence tokenSequence = token.getTokenSequence();
			int startIndex = 0;
			String word0 = "";
			String word1 = "";
			String word2 = "";
			if (tokenSequence.size()>0) word0 = tokenSequence.get(0).getText();
			if (tokenSequence.size()>1) word1 = tokenSequence.get(1).getText();
			if (tokenSequence.size()>2) word2 = tokenSequence.get(2).getText();
			
			boolean word0IsInteger = false;
			try {
				Integer.parseInt(word0);
				word0IsInteger = true;
			} catch (NumberFormatException nfe) {
				word0IsInteger = false;
			}
			
			boolean word1IsInteger = false;
			try {
				Integer.parseInt(word1);
				word1IsInteger = true;
			} catch (NumberFormatException nfe) {
				word1IsInteger = false;
			}
			
			if (word0.equals("\"")||word0.equals("-")||word0.equals("--")||word0.equals("—")||word0.equals("*")||word0.equals("(")) {
				startIndex = 1;
			} else if ((word0IsInteger||word0.length()==1)
					&& (word1.equals(")")||word1.equals("."))) {
				startIndex = 2;
			} else if (word0.equals("(")
					&& (word1IsInteger||word1.length()==1)
					&& word2.equals(")")) {
				startIndex = 3;
			}
			firstWord = (token.getIndex()==startIndex);
			
			result = this.generateResult(firstWord);
		} // have token
		
		return result;
	}
}
