///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2015 Joliciel Informatique
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

import java.util.regex.Pattern;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;

/**
 * Normalises all unicode characters representing some sort of double quote to
 * simple double quote, some sort of single quote to a simple apostrophe, and
 * some sort of dash or hyphen to a simple minus sign.
 * 
 * @author Assaf Urieli
 *
 */
public class QuoteNormaliser implements TokenFilter {
  Pattern doubleQuotes = Pattern.compile("[“”„‟″‴«»]");
  Pattern singleQuotes = Pattern.compile("[‘’]");
  Pattern dashes = Pattern.compile("[‒–—―]");

  public QuoteNormaliser(String sessionId) {
  }

  @Override
  public void apply(TokenSequence tokenSequence) {
    for (Token token : tokenSequence) {
      token.setText(doubleQuotes.matcher(token.getText()).replaceAll("\""));
      token.setText(singleQuotes.matcher(token.getText()).replaceAll("'"));
      token.setText(dashes.matcher(token.getText()).replaceAll("-"));
    }
  }

}
