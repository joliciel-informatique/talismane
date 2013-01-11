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

import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.patterns.TokenMatch;
import com.joliciel.talismane.tokeniser.patterns.TokenPattern;

/**
 * Returns the index of the first token matching this pattern.
 * @author Assaf Urieli
 *
 */
public class CurrentPatternIndex extends AbstractTokenFeature<Integer> implements IntegerFeature<TokenWrapper> {
	TokenPattern tokeniserPattern;
	
	public CurrentPatternIndex(TokenPattern tokeniserPattern) {
		this.tokeniserPattern = tokeniserPattern;
		this.setName(super.getName() + "[pattern<" + this.tokeniserPattern + ">]");
	}
	
	@Override
	public FeatureResult<Integer> checkInternal(TokenWrapper tokenWrapper) {
		Token token = tokenWrapper.getToken();
		FeatureResult<Integer> result = null;
		TokenMatch theMatch = null;
		for (TokenMatch tokenMatch : token.getMatches()) {
			if (tokenMatch.getPattern().equals(tokeniserPattern)&&tokenMatch.getIndex()==tokeniserPattern.getIndexesToTest().get(0)) {
				theMatch = tokenMatch;
				break;
			}
		}
		if (theMatch!=null) {
			// note - if a match is found, this is actually the second token in the pattern
			// therefore, we want the index of the first token in the pattern.
			int indexWithWhiteSpace = token.getIndexWithWhiteSpace() - theMatch.getIndex();
			Token firstToken = token.getTokenSequence().listWithWhiteSpace().get(indexWithWhiteSpace);
			int patternIndex = firstToken.getIndex();
			
			result = this.generateResult(patternIndex);
		} // the current token matches the tokeniserPattern at it's first test index
		
		return result;
	}
}
