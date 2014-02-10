///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2014 Joliciel Informatique
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

import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.machineLearning.features.StringFeature;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.patterns.TokenPatternMatch;
import com.joliciel.talismane.tokeniser.patterns.TokenPattern;

/**
 * Returns the actual text of the tokens matching the current pattern.
 * @author Assaf Urieli
 *
 */
public final class PatternWordFormFeature extends AbstractTokenFeature<String> implements StringFeature<TokenWrapper> {
	StringFeature<TokenWrapper> tokenPatternFeature;
	private Map<String,TokenPattern> patternMap;

	public PatternWordFormFeature(StringFeature<TokenWrapper> tokenPatternFeature) {
		this.tokenPatternFeature = tokenPatternFeature;
		this.setName(super.getName() + "(" + this.tokenPatternFeature.getName() + ")");
	}
	
	@Override
	public FeatureResult<String> checkInternal(TokenWrapper tokenWrapper, RuntimeEnvironment env) {
		Token token = tokenWrapper.getToken();
		FeatureResult<String> result = null;
		FeatureResult<String> tokenPatternResult = tokenPatternFeature.check(tokenWrapper, env);
		if (tokenPatternResult!=null) {
			// If we have a token pattern, then this is the first token to be tested in that pattern
			TokenPattern tokenPattern = this.patternMap.get(tokenPatternResult.getOutcome());

			TokenPatternMatch theMatch = null;
			for (TokenPatternMatch tokenMatch : token.getMatches(tokenPattern)) {
				if (tokenMatch.getPattern().equals(tokenPattern)&&tokenMatch.getIndex()==tokenPattern.getIndexesToTest().get(0)) {
					theMatch = tokenMatch;
					break;
				}
			}
			if (theMatch!=null) {
				String unigram = "";
				for (int i = 0; i<tokenPattern.getTokenCount();i++) {
					int index = token.getIndexWithWhiteSpace() - theMatch.getIndex() + i;
					Token aToken = token.getTokenSequence().listWithWhiteSpace().get(index);
					unigram += aToken.getText();
				}
				result = this.generateResult(unigram);
			} // the current token matches the tokeniserPattern at it's first test index
		}
		return result;
	}

	public Map<String, TokenPattern> getPatternMap() {
		return patternMap;
	}

	public void setPatternMap(Map<String, TokenPattern> patternMap) {
		this.patternMap = patternMap;
	}
	
	
}
