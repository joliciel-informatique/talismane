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
package com.joliciel.talismane.sentenceAnnotators;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.NeedsTalismaneSession;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.tokeniser.Tokeniser;

/**
 * Transforms a word into lower-case if it is a known word in the lexicon, and
 * if it is at the start of a sentence or immediately following any punctuation
 * mark except those in {@link #noUppercasePunctuationRegex}.
 * 
 * @author Assaf Urieli
 *
 */
public class LowercaseKnownFirstWordFilter implements TextReplacer, NeedsTalismaneSession {
  private static final Logger LOG = LoggerFactory.getLogger(LowercaseKnownFirstWordFilter.class);
  private TalismaneSession session;

  public static final String noUppercasePunctuationRegex = "[,]";
  private static final Pattern noUppercasePunctuation = Pattern.compile(noUppercasePunctuationRegex);

  /**
   * Maximum number of spaces in a single word.
   */
  private static final int maxSpaces = 2;

  public LowercaseKnownFirstWordFilter() {
    super();
  }

  @Override
  public void replace(List<String> tokens) {
    boolean needsLowercasing = true;
    boolean inUppercaseWord = false;
    List<String> wordToLowercase = new ArrayList<>();
    int startPos = 0;
    int numSpaces = 0;

    for (int i = 0; i < tokens.size(); i++) {
      String token = tokens.get(i);

      if (needsLowercasing && !inUppercaseWord && (Tokeniser.getTokenSeparators(session).matcher(token).matches() || token.length() == 0)) {
        continue;
      }

      if (needsLowercasing) {
        // It is possible for a known word to include punctuation and be
        // spread across multiple tokens,
        // e.g. "aujourd'hui", "après-midi"

        if (!inUppercaseWord) {
          // start of uppercase word ?
          if (Character.isUpperCase(token.charAt(0))) {
            // next token is the start of an uppercase word
            inUppercaseWord = true;
            wordToLowercase = new ArrayList<>();
            wordToLowercase.add(token);
            startPos = i;
          } else {
            // not an uppercase word, no need to lowercase it
            needsLowercasing = false;
          }
        } else if (token.length() == 1 && Character.isWhitespace(token.charAt(0))) {
          // hit a space - word is maybe finished
          numSpaces++;
          if (numSpaces > maxSpaces)
            needsLowercasing = false;
          else
            wordToLowercase.add(token);
        } else {
          // inside an uppercase word - add the next token
          wordToLowercase.add(token);
        }

        // do we still need lowercasing?
        if (needsLowercasing) {
          StringBuilder wordBuilder = new StringBuilder();
          for (String part : wordToLowercase)
            wordBuilder.append(part);
          String word = wordBuilder.toString();

          // are there any dictionary words for this upper case word?
          Set<String> possibleWords = session.getDiacriticizer().diacriticize(word);
          if (possibleWords.size() > 0) {
            // found a dictionary word
            String possibleWord = possibleWords.iterator().next();
            List<String> possibleTokens = Tokeniser.bruteForceTokenise(possibleWord, session);
            if (possibleTokens.size() == wordToLowercase.size()) {
              // same number of tokens, we can update the existing
              // word
              for (int j = 0; j < possibleTokens.size(); j++) {
                tokens.set(startPos + j, possibleTokens.get(j));
              }
              // no longer need to lowercase
              needsLowercasing = false;
            } else {
              LOG.debug("Different number of tokens: |" + possibleTokens.toString() + "| and |" + wordToLowercase.toString() + "|");
            }
          }
        } // still needs lowercasing
      } else if (token.length() == 1 && !Character.isWhitespace(token.charAt(0)) && Tokeniser.getTokenSeparators(session).matcher(token).matches()
          && !noUppercasePunctuation.matcher(token).matches()) {
        needsLowercasing = true;
        inUppercaseWord = false;
        numSpaces = 0;
      } // needs lowercasing
    } // next token
  }

  @Override
  public TalismaneSession getTalismaneSession() {
    return session;
  }

  @Override
  public void setTalismaneSession(TalismaneSession talismaneSession) {
    this.session = talismaneSession;
  }

}
