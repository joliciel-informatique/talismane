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
import com.joliciel.talismane.utils.features.Feature;
import com.joliciel.talismane.utils.features.FeatureResult;
import com.joliciel.talismane.utils.features.IntegerFeature;

/**
 * Allows to apply any other TokenFeature to a token
 * offset from the TokeniserPattern containing the present token.<br/>
 * This allows us to find the word preceding a given compound candidate, or following a given compound candidate.<br/>
 * Returns null if the offset goes outside the token sequence.<br/>
 * @author Assaf Urieli
 *
 */
public class TokeniserPatternOffsetFeature<Y> extends AbstractTokenFeature<Y> {
	TokenPattern tokeniserPattern;
	Feature<TokenWrapper,Y> tokenFeature;
	IntegerFeature<TokenWrapper> offsetFeature;
	
	public TokeniserPatternOffsetFeature(TokenPattern tokeniserPattern, IntegerFeature<TokenWrapper> offsetFeature, Feature<TokenWrapper,Y> tokenFeature) {
		this.tokeniserPattern = tokeniserPattern;
		this.tokenFeature = tokenFeature;
		this.offsetFeature = offsetFeature;
		this.setName(tokenFeature.getName() + "[pattern<" + this.tokeniserPattern + ">offset<" + this.offsetFeature.getName() + ">]");
	}
	
	@Override
	public FeatureResult<Y> checkInternal(TokenWrapper tokenWrapper) {
		Token token = tokenWrapper.getToken();
		FeatureResult<Y> result = null;
		boolean foundMatch = false;
		int testIndex = tokeniserPattern.getIndexesToTest().get(0);
		for (TokenMatch tokenMatch : token.getMatches()) {
			if (tokenMatch.getPattern().equals(tokeniserPattern)&&tokenMatch.getIndex()==testIndex) {
				foundMatch = true;
				break;
			}
		}
		if (foundMatch) {
			// Only continue if the current token matched this pattern at this index
			FeatureResult<Integer> offsetResult = offsetFeature.check(tokenWrapper);
			if (offsetResult!=null) {
				int offset = offsetResult.getOutcome();
			
				Token offsetToken = null;
				if (offset==0)
					offsetToken = token;
				else  {
					// baseIndex should be the last non-whitespace word in the pattern if offset > 0
					// or the first non-whitespace word in the pattern if offset < 0
					int baseIndex = 0;
					int j = token.getIndexWithWhiteSpace() - testIndex;
					for (int i=0; i<tokeniserPattern.getTokenCount(); i++) {
						if (j>=0&&j<token.getTokenSequence().listWithWhiteSpace().size()) {
							Token tokenInPattern = token.getTokenSequence().listWithWhiteSpace().get(j);
							if (!tokenInPattern.isWhiteSpace()) {
								baseIndex = tokenInPattern.getIndex();
								if (offset<0) {
									break;
								}
							}
						}
						j++;
					}
	
					int offsetIndex = baseIndex + offset;
					if (offsetIndex>=0 && offsetIndex<token.getTokenSequence().size()) {
						offsetToken = token.getTokenSequence().get(offsetIndex);
					}
				}
				if (offsetToken!=null) {
					FeatureResult<Y> originalResult = tokenFeature.check(offsetToken);
					if (originalResult!=null)
						result = this.generateResult(originalResult.getOutcome());
				} // we have an offset token
			} // we have an offset result
		} // the current token matches the tokeniserPattern at the testIndex
		
		return result;
	}

	@Override
	public boolean isDynamic() {
		return true;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class<? extends Feature> getFeatureType() {
		return tokenFeature.getFeatureType();
	}

}
