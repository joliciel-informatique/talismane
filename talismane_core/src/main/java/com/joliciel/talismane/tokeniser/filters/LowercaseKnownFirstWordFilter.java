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
package com.joliciel.talismane.tokeniser.filters;

import java.util.Set;
import java.util.regex.Pattern;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.Tokeniser;

/**
 * Transforms a word into lower-case if it is a known word in the lexicon, and
 * if it is at the start of a sentence or immediately following any punctuation
 * mark except those in {@link #noUppercasePunctuationRegex}.
 * 
 * @author Assaf Urieli
 *
 */
public class LowercaseKnownFirstWordFilter implements TokenFilter {
  public static final String noUppercasePunctuationRegex = "[,]";

  private static final Pattern noUppercasePunctuation = Pattern.compile(noUppercasePunctuationRegex);
  private final String sessionId;

  public LowercaseKnownFirstWordFilter(String sessionId) {
    this.sessionId = sessionId;
  }

  @Override
  public void apply(TokenSequence tokenSequence) {
    int startIndex = 0;
    if (tokenSequence.isWithRoot())
      startIndex += 1;

    boolean lowerCaseNextWord = true;
    int index = -1;
    for (Token token : tokenSequence) {
      index++;
      if (index < startIndex)
        continue;

      if (token.getText().length() == 0)
        continue;

      if (lowerCaseNextWord) {
        char firstChar = token.getText().charAt(0);
        if (Character.isUpperCase(firstChar)) {
          Set<String> possibleWords = TalismaneSession.get(sessionId).getDiacriticizer().diacriticize(token.getText());
          if (possibleWords.size() > 0)
            token.setText(possibleWords.iterator().next());
        } // next word starts with an upper-case
        lowerCaseNextWord = false;
      } // should we lower-case the next word?
      if (Tokeniser.getTokenSeparators(sessionId).matcher(token.getText()).matches() && !noUppercasePunctuation.matcher(token.getText()).matches()) {
        lowerCaseNextWord = true;
      }
    } // next token
  }

}
