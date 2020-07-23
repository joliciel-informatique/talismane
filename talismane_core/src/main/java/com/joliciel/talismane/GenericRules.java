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
package com.joliciel.talismane;

import java.util.Locale;

public class GenericRules implements LinguisticRules {
  String sessionId;

  public GenericRules(String sessionId) {
    super();
    this.sessionId = sessionId;
  }

  @Override
  public boolean shouldAddSpace(String text, String word) {
    // Double quotes are tricky because they could be opening or closing
    // quotations. Most of the time we can simply
    // count quotes, but not if there's a sentence break inside a quotation.
    // We'll assume single quote are always actual quotes, since apostrophes
    // would not be tokenised separately.
    if (word.equals(".") || word.equals(",") || word.equals(")") || word.equals("]") || word.equals("}") || word.equals("”") || text.endsWith("“")
        || text.endsWith("(") || text.endsWith("[") || text.endsWith("{") || word.length() == 0)
      return false;

    if (word.equals("'") || word.equals("\"")) {
      int prevCount = 0;
      for (int i = 0; i < text.length(); i++) {
        char c = text.charAt(i);
        if (c == word.charAt(0))
          prevCount++;
      }

      if (prevCount % 2 == 0) {
        // even number of quotes, add space before this one
        return true;
      } else {
        // odd number of quotes
        return false;
      }
    }

    if (text.endsWith("'") || text.endsWith("\"")) {
      char lastTextChar = text.charAt(text.length() - 1);
      int prevCount = 0;
      for (int i = 0; i < text.length(); i++) {
        char c = text.charAt(i);
        if (c == lastTextChar)
          prevCount++;
      }

      if (prevCount % 2 == 0) {
        // even number of quotes, add space after the quote
        return true;
      } else {
        // odd number of quotes
        return false;
      }
    }

    Locale locale = TalismaneSession.get(sessionId).getLocale();
    if (locale.getLanguage().equals("fr")) {
      if (word.equals(":") || word.equals("?") || word.equals("!"))
        return true;
    }

    if (word.equals(":") || word.equals("?") || word.equals("!"))
      return false;

    if (text.endsWith("'") || text.endsWith("’") || word.startsWith("'") || word.startsWith("’"))
      return false;

    return true;
  }

  @Override
  public String makeAdjectiveSingular(String adjective) throws TalismaneException {
    Locale locale = TalismaneSession.get(sessionId).getLocale();
    if (locale.getLanguage().equals("fr")) {
      String result = adjective;
      if (adjective.endsWith("aux")) {
        result = adjective.substring(0, adjective.length() - 3) + "al";
      } else if (adjective.endsWith("s")) {
        result = adjective.substring(0, adjective.length() - 1);
      }
      return result;
    } else {
      throw new TalismaneException("Language not yet supported for GenericRules.makeAdjectiveSingular: " + locale.getLanguage());
    }
  }

  @Override
  public char[] getLowercaseOptionsWithDiacritics(char c) {
    Locale locale = TalismaneSession.get(sessionId).getLocale();
    char[] lowerCaseChars = null;
    if (locale.getLanguage().equals("fr")) {
      switch (c) {
      case 'E':
        lowerCaseChars = new char[] { 'e', 'é', 'ê', 'è', 'ë' };
        break;
      case 'A':
        lowerCaseChars = new char[] { 'à', 'a', 'â', 'á' };
        break;
      case 'O':
        lowerCaseChars = new char[] { 'o', 'ô', 'ò', 'ó' };
        break;
      case 'I':
        lowerCaseChars = new char[] { 'i', 'î', 'ï', 'í' };
        break;
      case 'U':
        lowerCaseChars = new char[] { 'u', 'ú', 'ü' };
        break;
      case 'C':
        lowerCaseChars = new char[] { 'c', 'ç' };
        break;
      default:
        lowerCaseChars = new char[] { Character.toLowerCase(c) };
        break;
      }
    } else {
      lowerCaseChars = new char[] { Character.toLowerCase(c) };
    }
    return lowerCaseChars;
  }
}
