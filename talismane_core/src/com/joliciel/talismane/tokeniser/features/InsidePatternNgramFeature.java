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

import com.joliciel.talismane.tokeniser.TaggedToken;
import com.joliciel.talismane.tokeniser.TokeniserDecision;
import com.joliciel.talismane.tokeniser.patterns.TokenMatch;
import com.joliciel.talismane.tokeniser.patterns.TokenPattern;
import com.joliciel.talismane.utils.features.FeatureResult;
import com.joliciel.talismane.utils.features.IntegerFeature;
import com.joliciel.talismane.utils.features.StringFeature;

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
	
	public InsidePatternNgramFeature(TokenPattern tokeniserPattern, IntegerFeature<TokeniserContext> testIndexFeature) {
		this.tokeniserPattern = tokeniserPattern;
		this.testIndexFeature = testIndexFeature;
		this.setName(super.getName() + "[pattern<" + this.tokeniserPattern + ">index<" + this.testIndexFeature.getName() + ">]");
	}
	
	@Override
	public FeatureResult<String> checkInternal(TokeniserContext tokeniserContext) {
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
				TaggedToken<TokeniserDecision> lastDecision = null;
				for (int i=tokeniserContext.getHistory().size()-1; i>=0; i--) {
					TaggedToken<TokeniserDecision> decision = tokeniserContext.getHistory().get(i);
					if (!decision.getTag().equals(TokeniserDecision.NOT_APPLICABLE)) {
						lastDecision = decision;
						break;
					}
				}
				if (lastDecision!=null)
					result = this.generateResult(lastDecision.getTag().toString());
			}
		} // have test index
		
		return result;
	}
}
