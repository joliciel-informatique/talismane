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

import java.util.Map;

import com.joliciel.talismane.machineLearning.features.Feature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.machineLearning.features.StringFeature;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.patterns.TokenPattern;

/**
 * Allows to apply any other TokenFeature to a token
 * offset from the TokeniserPattern containing the present token.<br/>
 * This allows us to find the word preceding a given compound candidate, or following a given compound candidate.<br/>
 * Returns null if the offset goes outside the token sequence.<br/>
 * @author Assaf Urieli
 *
 */
public class TokeniserPatternOffsetFeature<Y> extends AbstractTokenFeature<Y> {
	StringFeature<TokenWrapper> tokenPatternFeature;
	Feature<TokenWrapper,Y> tokenFeature;
	IntegerFeature<TokenWrapper> offsetFeature;
	private Map<String,TokenPattern> patternMap;
	
	public TokeniserPatternOffsetFeature(StringFeature<TokenWrapper> tokenPatternFeature, IntegerFeature<TokenWrapper> offsetFeature, Feature<TokenWrapper,Y> tokenFeature) {
		this.tokenPatternFeature = tokenPatternFeature;
		this.tokenFeature = tokenFeature;
		this.offsetFeature = offsetFeature;
		this.setName(tokenFeature.getName() + "(" + this.tokenPatternFeature.getName() + "," + this.offsetFeature.getName() + ")");
	}
	
	@Override
	public FeatureResult<Y> checkInternal(TokenWrapper tokenWrapper, RuntimeEnvironment env) {
		Token token = tokenWrapper.getToken();
		FeatureResult<Y> result = null;
		
		FeatureResult<String> tokenPatternResult = tokenPatternFeature.check(tokenWrapper, env);
		if (tokenPatternResult!=null) {
			TokenPattern tokenPattern = this.patternMap.get(tokenPatternResult.getOutcome());
			int testIndex = tokenPattern.getIndexesToTest().get(0);
		
			// Only continue if the current token matched this pattern at this index
			FeatureResult<Integer> offsetResult = offsetFeature.check(tokenWrapper, env);
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
					for (int i=0; i<tokenPattern.getTokenCount(); i++) {
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
					FeatureResult<Y> originalResult = tokenFeature.check(offsetToken, env);
					if (originalResult!=null)
						result = this.generateResult(originalResult.getOutcome());
				} // we have an offset token
			} // we have an offset result
		}
		return result;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class<? extends Feature> getFeatureType() {
		return tokenFeature.getFeatureType();
	}

	public Map<String, TokenPattern> getPatternMap() {
		return patternMap;
	}

	public void setPatternMap(Map<String, TokenPattern> patternMap) {
		this.patternMap = patternMap;
	}

}
