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
import com.joliciel.talismane.machineLearning.features.StringFeature;
import com.joliciel.talismane.tokeniser.TaggedToken;
import com.joliciel.talismane.tokeniser.TokeniserOutcome;
import com.joliciel.talismane.tokeniser.patterns.TokenMatch;
import com.joliciel.talismane.tokeniser.patterns.TokenPattern;

/**
 * Gives the previous tokeniser decision for the atomic token just preceding the one indicated by a given index in the given pattern.<br/>
 * Useful for ensuring that inner-pattern decisions are always respected (unless two patterns overlap in the same sequence),
 * thus ensuring that multi-token compound words are either made compound as a whole, or not at all.
 * @author Assaf Urieli
 *
 */
public class InsidePatternNgramFeature extends AbstractTokeniserContextFeature<String> implements StringFeature<TokeniserContext> {
	TokenPattern tokeniserPattern;
	IntegerFeature<TokeniserContext> testIndexFeature;
	
	/**
	 * No argument constructor for simplicity's sake - since InsidePatternNgram only applies to patterns with more than 2 atomic tokens,
	 * and we don't want the feature parser crashing if it's called for a pattern with nothing to test.
	 */
	public InsidePatternNgramFeature() { }
	
	public InsidePatternNgramFeature(TokenPattern tokeniserPattern, IntegerFeature<TokeniserContext> testIndexFeature) {
		this.tokeniserPattern = tokeniserPattern;
		this.testIndexFeature = testIndexFeature;
		this.setName(super.getName() + "[pattern<" + this.tokeniserPattern + ">index<" + this.testIndexFeature.getName() + ">]");
	}
	
	@Override
	public FeatureResult<String> checkInternal(TokeniserContext tokeniserContext) {
		if (this.tokeniserPattern==null)
			return null;
		
		FeatureResult<String> result = null;
		
		FeatureResult<Integer> testIndexResult = testIndexFeature.check(tokeniserContext);
		if (testIndexResult!=null) {
			int testIndex = testIndexResult.getOutcome();

			boolean foundMatch = false;
			for (TokenMatch tokenMatch : tokeniserContext.getToken().getMatches()) {
				if (tokenMatch.getPattern().equals(tokeniserPattern)&&tokenMatch.getIndex()==testIndex) {
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
}
