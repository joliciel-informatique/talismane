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
package com.joliciel.talismane.sentenceDetector.features;

import java.util.regex.Pattern;

import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.machineLearning.features.StringFeature;
import com.joliciel.talismane.sentenceDetector.PossibleSentenceBoundary;
import com.joliciel.talismane.tokeniser.Token;

/**
 * Examines the atomic tokens from <i>n</i> before the boundary to <i>n</i> after the boundary.<br/>
 * For each token, if it is whitespace, adds " " to the result.<br/>
 * If it is a separator, adds the original separator to the result.<br/>
 * If it is a capitalised word, adds, "W", "Wo" or "Word", depending on whether the word is 1 letter, 2 letters, or more.<br/>
 * Otherwise adds "word" to the result.<br/>
 * @author Assaf Urieli
 *
 */
public class SurroundingsFeature extends AbstractSentenceDetectorFeature<String> implements StringFeature<PossibleSentenceBoundary> {
	Pattern number = Pattern.compile("\\d+");
	IntegerFeature<PossibleSentenceBoundary> nFeature;

	public SurroundingsFeature(IntegerFeature<PossibleSentenceBoundary> nFeature) {
		this.nFeature = nFeature;
		this.setName(super.getName() + "(" + nFeature.getName() + ")");
	}
	
	@Override
	public FeatureResult<String> checkInternal(PossibleSentenceBoundary context, RuntimeEnvironment env) {
		FeatureResult<String> result = null;
		
		FeatureResult<Integer> nResult = nFeature.check(context, env);
		if (nResult!=null) {
			int n = nResult.getOutcome();
	
			int tokenIndex = context.getTokenIndexWithWhitespace();
			String tokenString = "";
			int maxToken = context.getTokenSequence().listWithWhiteSpace().size();
			for (int i=tokenIndex-n;i<=tokenIndex+n;i++) {
				Token token = null;
				String categoryString = null;
				if (i>=0&&i<maxToken) {
					token = context.getTokenSequence().listWithWhiteSpace().get(i);
					categoryString = this.getCategoryString(token);
				} else {
					if (i==-1||i==maxToken) {
						categoryString = " ";
					} else if (i==maxToken+1) {
						categoryString = "Word";
					} else if ((0-i)%2==0||(maxToken-i)%2==1) {
						categoryString = "word";
					} else {
						categoryString = " ";
					}
				}
				tokenString += categoryString;
			}
			
			String resultString = tokenString;
			result = this.generateResult(resultString);
		} // have n

		return result;
	}

	private String getCategoryString(Token token) {
		String categoryString = "";
		if (token==null) 
			categoryString = " ";
		else if (token.isWhiteSpace())
			categoryString = " ";
		else if (token.isSeparator())
			categoryString = token.getOriginalText();
		else if (number.matcher(token.getText()).matches())
			categoryString = "1";
		else if (Character.isUpperCase(token.getOriginalText().charAt(0))) {
			if (token.getOriginalText().length()==1)
				categoryString = "W";
			else if (token.getOriginalText().length()==2)
				categoryString = "Wo";
			else
				categoryString = "Word";
		} else
			categoryString = "word";	
		return categoryString;
	}
	
	
}
