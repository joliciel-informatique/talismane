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
import com.joliciel.talismane.utils.features.BooleanFeature;
import com.joliciel.talismane.utils.features.FeatureResult;

/**
 * Returns true if current token is the FIRST TOKEN in a sequence of tokens matching a given pattern.<br/>
 * Returns null (not false!) otherwise.
 * @author Assaf Urieli
 *
 */
public class CurrentPatternFeature extends AbstractTokenFeature<Boolean> implements BooleanFeature<TokenWrapper> {
	TokenPattern tokeniserPattern;
	
	public CurrentPatternFeature(TokenPattern tokeniserPattern) {
		this.tokeniserPattern = tokeniserPattern;
		this.setName(super.getName() + "[pattern<" + this.tokeniserPattern + ">]");
	}
	
	@Override
	public FeatureResult<Boolean> checkInternal(TokenWrapper tokenWrapper) {
		Token token = tokenWrapper.getToken();
		FeatureResult<Boolean> result = null;
		boolean foundMatch = false;
		for (TokenMatch tokenMatch : token.getMatches()) {
			if (tokenMatch.getPattern().equals(tokeniserPattern)&&tokenMatch.getIndex()==tokeniserPattern.getIndexesToTest().get(0)) {
				foundMatch = true;
				break;
			}
		}
		if (foundMatch) {
			result = this.generateResult(true);
		} // the current token matches the tokeniserPattern at it's first test index
		
		return result;
	}

	@Override
	public boolean isDynamic() {
		return true;
	}
}
