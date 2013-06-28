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

import com.joliciel.talismane.machineLearning.features.BooleanFeature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.sentenceDetector.PossibleSentenceBoundary;
import com.joliciel.talismane.tokeniser.Token;

/**
 * Returns true if the current token is "." and the previous token is a capital letter,
 * false otherwise.
 * @author Assaf Urieli
 *
 */
public final class InitialsFeature extends AbstractSentenceDetectorFeature<Boolean> implements BooleanFeature<PossibleSentenceBoundary> {	
	@Override
	public FeatureResult<Boolean> checkInternal(PossibleSentenceBoundary context, RuntimeEnvironment env) {
		FeatureResult<Boolean> result = null;
		
		if (context.getBoundaryString().equals(".")) {
			int tokenIndex = context.getTokenIndexWithWhitespace();

			Token previousToken = null;
			if (tokenIndex>0)
				previousToken = context.getTokenSequence().listWithWhiteSpace().get(tokenIndex-1);
			
			String isInitial = null;
			
			if (previousToken!=null&&Character.isUpperCase(previousToken.getOriginalText().charAt(0))) {
				if (previousToken.getOriginalText().length()==1)
					isInitial = "true";
			}
			
			if (isInitial!=null) {
				result = this.generateResult(true);
			}
			
		}
		return result;
	}

}
