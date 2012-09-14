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

import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenWrapper;
import com.joliciel.talismane.tokeniser.patterns.TokenMatch;
import com.joliciel.talismane.tokeniser.patterns.TokenPattern;
import com.joliciel.talismane.utils.features.FeatureResult;
import com.joliciel.talismane.utils.features.StringFeature;

/**
 * Returns the actual text of the tokens matching the current pattern.
 * @author Assaf Urieli
 *
 */
public class CurrentPatternWordForm extends AbstractTokenFeature<String> implements StringFeature<TokenWrapper> {
	TokenPattern tokeniserPattern;
	
	public CurrentPatternWordForm(TokenPattern tokeniserPattern) {
		this.tokeniserPattern = tokeniserPattern;
		this.setName(super.getName() + "[pattern<" + this.tokeniserPattern + ">]");
	}
	
	@Override
	public FeatureResult<String> checkInternal(TokenWrapper tokenWrapper) {
		Token token = tokenWrapper.getToken();
		FeatureResult<String> result = null;
		TokenMatch theMatch = null;
		for (TokenMatch tokenMatch : token.getMatches()) {
			if (tokenMatch.getPattern().equals(tokeniserPattern)&&tokenMatch.getIndex()==tokeniserPattern.getIndexesToTest().get(0)) {
				theMatch = tokenMatch;
				break;
			}
		}
		if (theMatch!=null) {
			String unigram = "";
			for (int i = 0; i<tokeniserPattern.getTokenCount();i++) {
				int index = token.getIndexWithWhiteSpace() - theMatch.getIndex() + i;
				Token aToken = token.getTokenSequence().listWithWhiteSpace().get(index);
				unigram += aToken.getText();
			}
			result = this.generateResult(unigram);
		} // the current token matches the tokeniserPattern at it's first test index
		
		return result;
	}

	@Override
	public boolean isDynamic() {
		return true;
	}
}
