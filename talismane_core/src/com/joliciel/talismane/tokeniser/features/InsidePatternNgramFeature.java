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

import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.machineLearning.features.StringFeature;
import com.joliciel.talismane.tokeniser.TaggedToken;
import com.joliciel.talismane.tokeniser.TokeniserOutcome;
import com.joliciel.talismane.tokeniser.patterns.TokenPatternMatch;
import com.joliciel.talismane.tokeniser.patterns.TokenPattern;

/**
 * Gives the previous tokeniser decision for the atomic token just preceding the one indicated by a given index in the given pattern.<br/>
 * Useful for ensuring that inner-pattern decisions are always respected (unless two patterns overlap in the same sequence),
 * thus ensuring that multi-token compound words are either made compound as a whole, or not at all.
 * @author Assaf Urieli
 *
 */
public final class InsidePatternNgramFeature extends AbstractTokeniserContextFeature<String> implements StringFeature<TokeniserContext> {
	StringFeature<TokenWrapper> tokenPatternFeature;
	IntegerFeature<TokeniserContext> testIndexFeature;
	StringFeature<TokenWrapper> tokenPatternAndIndexFeature;
	
	private Map<String,TokenPattern> patternMap;
	
	/**
	 * No argument constructor for simplicity's sake - since InsidePatternNgram only applies to patterns with more than 2 atomic tokens,
	 * and we don't want the feature parser crashing if it's called for a pattern with nothing to test.
	 */
	public InsidePatternNgramFeature() { }
	
	/**
	 * This single argument version is a bit of a hack, but it was the simplest way to send
	 * both the pattern and the related index in one go.<br/>
	 * The tokenPatternAndIndexFeature should thus include the pattern name, followed by the character ¤,
	 * followed by the index number.
	 * @param tokenPatternFeature
	 * @param testIndexFeature
	 */
	public InsidePatternNgramFeature(StringFeature<TokenWrapper> tokenPatternAndIndexFeature) {
		this.tokenPatternAndIndexFeature = tokenPatternAndIndexFeature;
		this.setName(super.getName() + "(" + this.tokenPatternAndIndexFeature.getName() + ")");
	}	
	/**
	 * @param tokenPatternFeature
	 * @param testIndexFeature
	 */
	public InsidePatternNgramFeature(StringFeature<TokenWrapper> tokenPatternFeature, IntegerFeature<TokeniserContext> testIndexFeature) {
		this.tokenPatternFeature = tokenPatternFeature;
		this.testIndexFeature = testIndexFeature;
		this.setName(super.getName() + "(" + this.tokenPatternFeature.getName() + "," + this.testIndexFeature.getName() + ")");
	}
	
	@Override
	public FeatureResult<String> checkInternal(TokeniserContext tokeniserContext, RuntimeEnvironment env) {
		if (this.tokenPatternFeature==null && this.tokenPatternAndIndexFeature==null)
			return null;
		
		FeatureResult<String> result = null;
		TokenPattern tokenPattern = null;
		int testIndex = -1;
		
		if (tokenPatternAndIndexFeature!=null) {
			FeatureResult<String> tokenPatternAndIndexResult = tokenPatternAndIndexFeature.check(tokeniserContext, env);
			if (tokenPatternAndIndexResult!=null) {
				String tokenPatternAndIndex = tokenPatternAndIndexResult.getOutcome();
				String[] parts = tokenPatternAndIndex.split("¤");
				tokenPattern = this.patternMap.get(parts[0]);
				testIndex = Integer.parseInt(parts[1]);
			}
			
		} else {
			FeatureResult<Integer> testIndexResult = testIndexFeature.check(tokeniserContext, env);
			FeatureResult<String> tokenPatternResult = tokenPatternFeature.check(tokeniserContext, env);
			if (tokenPatternResult!=null) {
				tokenPattern = this.patternMap.get(tokenPatternResult.getOutcome());
			}
			if (testIndexResult!=null)
				testIndex = testIndexResult.getOutcome();
		}

		if (testIndex>=0 && tokenPattern!=null) {
			boolean foundMatch = false;
			for (TokenPatternMatch tokenMatch : tokeniserContext.getToken().getMatches(tokenPattern)) {
				if (tokenMatch.getPattern().equals(tokenPattern)&&tokenMatch.getIndex()==testIndex) {
					foundMatch = true;
					break;
				}
			}
			if (foundMatch) {
				TaggedToken<TokeniserOutcome> lastDecision = null;
				if (tokeniserContext.getHistory().size()>0) {
					TaggedToken<TokeniserOutcome> decision = tokeniserContext.getHistory().get(tokeniserContext.getHistory().size()-1);
					lastDecision = decision;
				}
				if (lastDecision!=null)
					result = this.generateResult(lastDecision.getTag().toString());
			}
		} // have test index
		
		return result;
	}

	public Map<String, TokenPattern> getPatternMap() {
		return patternMap;
	}

	public void setPatternMap(Map<String, TokenPattern> patternMap) {
		this.patternMap = patternMap;
	}
	
	
}
