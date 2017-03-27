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
package com.joliciel.talismane.sentenceDetector.features;

import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.machineLearning.features.StringFeature;
import com.joliciel.talismane.sentenceDetector.PossibleSentenceBoundary;

/**
 * Returns "YES" if the current sentence break is between a "(" and ")"
 * character without any intervening characters ".", "?" or "!". Returns "OPEN"
 * if a parenthesis has been open but not closed. Return "CLOSE" if a
 * parenthesis has not been opened but has been closed.
 * 
 * @author Assaf Urieli
 *
 */
public final class InParenthesesFeature extends AbstractSentenceDetectorFeature<String>implements StringFeature<PossibleSentenceBoundary> {
  @Override
  public FeatureResult<String> checkInternal(PossibleSentenceBoundary context, RuntimeEnvironment env) {
    FeatureResult<String> result = null;

    boolean openParenthesis = false;
    for (int i = context.getIndex() - 1; i >= 0; i--) {
      char c = context.getText().charAt(i);
      if (c == ')' || c == '.' || c == '!' || c == '?')
        break;
      if (c == '(') {
        openParenthesis = true;
        break;
      }
    }

    boolean closeParenthesis = false;
    for (int i = context.getIndex() + 1; i < context.getText().length(); i++) {
      char c = context.getText().charAt(i);
      if (c == '(' || c == '.' || c == '!' || c == '?')
        break;
      if (c == ')') {
        closeParenthesis = true;
        break;
      }
    }

    String resultString = null;
    if (openParenthesis && closeParenthesis)
      resultString = "YES";
    else if (openParenthesis)
      resultString = "OPEN";
    else if (closeParenthesis)
      resultString = "CLOSE";

    if (resultString != null)
      result = this.generateResult(resultString);

    return result;
  }

}
