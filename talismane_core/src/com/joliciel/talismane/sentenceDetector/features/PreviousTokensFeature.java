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

import com.joliciel.talismane.sentenceDetector.PossibleSentenceBoundary;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.utils.features.FeatureResult;
import com.joliciel.talismane.utils.features.IntegerFeature;
import com.joliciel.talismane.utils.features.StringFeature;

public class PreviousTokensFeature extends AbstractSentenceDetectorFeature<String> implements StringFeature<PossibleSentenceBoundary> {
	IntegerFeature<PossibleSentenceBoundary> nFeature;
	
	public PreviousTokensFeature(IntegerFeature<PossibleSentenceBoundary> nFeature) {
		this.nFeature = nFeature;
		this.setName(super.getName() + "(" + nFeature.getName() + ")");
	}
	
	@Override
	public FeatureResult<String> checkInternal(PossibleSentenceBoundary context) {
		FeatureResult<String> result = null;
		
		FeatureResult<Integer> nResult = nFeature.check(context);
		if (nResult!=null) {
			int n = nResult.getOutcome();
			int tokenIndex = context.getTokenIndexWithWhitespace();
			String tokenString = "";
			for (int i=0;i<=n;i++) {
				int relativeIndex = tokenIndex - i;
				if (relativeIndex>=0) {
					Token token = context.getTokenSequence().listWithWhiteSpace().get(relativeIndex);
					tokenString = token.getOriginalText() + tokenString;
				} else {
					tokenString = "[[START]]" + tokenString;
				}
			}
			result = this.generateResult(tokenString);
		} // have n

		return result;
	}
}
