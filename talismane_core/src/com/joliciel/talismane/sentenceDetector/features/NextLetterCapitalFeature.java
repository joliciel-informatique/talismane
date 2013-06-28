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

import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.machineLearning.features.StringFeature;
import com.joliciel.talismane.sentenceDetector.PossibleSentenceBoundary;
import com.joliciel.talismane.tokeniser.Token;

/**
 * In the following descriptions, the current boundary is surrounded by square brackets.<br/>
 * Returns "CapitalAfterInitial" for any pattern like: W[.] Shakespeare<br/>
 * Returns "CapitalAfterQuote" for any pattern like: blah di blah[.]" Hello <i>or</i> blah di blah[.] "Hello<br/>
 * Returns "CapitalAfterDash" for any pattern like: blah di blah[.] - Hello<br/>
 * Returns "true" for any other pattern like: "blah di blah[.] Hello<br/>
 * Returns "false" otherwise.<br/>
 * Note that there MUST be whitespace between the separator and the capital letter for it to be considered a capital letter.
 * @author Assaf Urieli
 *
 */
public final class NextLetterCapitalFeature extends AbstractSentenceDetectorFeature<String> implements StringFeature<PossibleSentenceBoundary> {
	@Override
	public FeatureResult<String> checkInternal(PossibleSentenceBoundary context, RuntimeEnvironment env) {
		FeatureResult<String> result = null;
		
		int tokenIndex = context.getTokenIndexWithWhitespace();
		
		boolean isInitial = false;
		if (context.getBoundaryString().equals(".")) {
			Token previousToken = null;
			if (tokenIndex>0)
				previousToken = context.getTokenSequence().listWithWhiteSpace().get(tokenIndex-1);
			
			if (previousToken!=null&&Character.isUpperCase(previousToken.getOriginalText().charAt(0))) {
				if (previousToken.getOriginalText().length()<2)
					isInitial = true;
			}
		}
		
		boolean hasWhiteSpace = false;
		boolean hasQuote = false;
		boolean hasDash = false;
		boolean nextLetterCapital = false;
		if (tokenIndex>=0) {
			for (int i=tokenIndex+1;i<context.getTokenSequence().listWithWhiteSpace().size();i++) {
				Token token = context.getTokenSequence().listWithWhiteSpace().get(i);
				if (token.isWhiteSpace()) {
					hasWhiteSpace = true;
				} else if (token.getText().equals("\"")||token.getText().equals("“")||token.getText().equals("„")||token.getText().equals("‟")||token.getText().equals("″")) {
					hasQuote = true;
					if (hasDash)
						break;
				} else if (token.getText().equals("-")) {
					hasDash = true;
					if (hasQuote)
						break;
				} else if (token.isSeparator()) {
					nextLetterCapital = false;
					break;
				} else {
					nextLetterCapital = (Character.isUpperCase(token.getOriginalText().charAt(0)));
					break;
				}
			}
		}
		
		nextLetterCapital = nextLetterCapital & hasWhiteSpace;
		if (nextLetterCapital&&isInitial)
			result = this.generateResult("CapitalAfterInitial");
		else if (nextLetterCapital&&hasQuote)
			result = this.generateResult("CapitalAfterQuote");
		else if (nextLetterCapital&&hasDash)
			result = this.generateResult("CapitalAfterDash");
		else if (nextLetterCapital)
			result = this.generateResult("true");
		else
			result = this.generateResult("false");
		return result;
	}

}
