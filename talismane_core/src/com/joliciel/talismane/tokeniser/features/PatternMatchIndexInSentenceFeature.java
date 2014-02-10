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

import com.joliciel.talismane.machineLearning.features.AbstractCachableFeature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.patterns.TokenPatternMatch;

/**
 * Returns the index of the first token matching this pattern.
 * @author Assaf Urieli
 *
 */
public final class PatternMatchIndexInSentenceFeature extends AbstractCachableFeature<TokenPatternMatch,Integer> implements IntegerFeature<TokenPatternMatch> {
	public PatternMatchIndexInSentenceFeature() {
	}
	
	@Override
	public FeatureResult<Integer> checkInternal(TokenPatternMatch tokenPatternMatch, RuntimeEnvironment env) {
		FeatureResult<Integer> result = null;
		Token token = tokenPatternMatch.getToken();
		
		// note - if a match is found, this is actually the second token in the pattern
		// therefore, we want the index of the first token in the pattern.
		int indexWithWhiteSpace = token.getIndexWithWhiteSpace() - tokenPatternMatch.getIndex();
		Token firstToken = token.getTokenSequence().listWithWhiteSpace().get(indexWithWhiteSpace);
		int patternIndex = firstToken.getIndex();
		
		result = this.generateResult(patternIndex);

		return result;
	}
}
