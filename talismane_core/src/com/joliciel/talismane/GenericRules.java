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

import com.joliciel.talismane.LinguisticRules;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;

public class GenericRules implements LinguisticRules {
	TalismaneSession talismaneSession;
	
	public GenericRules(TalismaneSession talismaneSession) {
		super();
		this.talismaneSession = talismaneSession;
	}

	@Override
	public boolean shouldAddSpace(TokenSequence tokenSequence, String currentToken) {
		// Double quotes are tricky because they could be opening or closing quotations. Most of the time we can simply 
		// count quotes, but not if there's a sentence break inside a quotation.
		// We'll assume single quote are always actual quotes, since apostrophes would not be tokenised separately.
		String previousToken = tokenSequence.get(tokenSequence.size()-1).getOriginalText();

		if (currentToken.equals(".")||currentToken.equals(",")
				|| currentToken.equals(")")||currentToken.equals("]")
				|| currentToken.equals("}")||currentToken.equals("”")
				|| previousToken.equals("“")
				|| previousToken.equals("(") || previousToken.equals("[")
				|| previousToken.equals("{")
				|| currentToken.length()==0)
			return false;
		
		if (currentToken.equals("'") || currentToken.equals("\"")) {
			int prevCount = 0;
			for (Token token : tokenSequence) {
				if (token.getOriginalText().equals(currentToken)) {
					prevCount++;
				}
			}
			
			if (prevCount % 2 == 0) {
				// even number of quotes, add space before this one
				return true;
			} else {
				// odd number of quotes
				return false;
			}
		}
		
		if (previousToken.equals("'") || previousToken.equals("\"")) {
			int prevCount = 0;
			for (Token token : tokenSequence) {
				if (token.getOriginalText().equals(currentToken)) {
					prevCount++;
				}
			}
			
			if (prevCount % 2 == 0) {
				// even number of quotes, add space after the quote
				return true;
			} else {
				// odd number of quotes
				return false;
			}
		}
		
		Locale locale = this.talismaneSession.getLocale();
		if (locale.getLanguage().equals("fr")) {
			if (currentToken.equals(":") || currentToken.equals("?")
					|| currentToken.equals("!"))
				return true;			
		}
		
		if (currentToken.equals(":") || currentToken.equals("?")
				|| currentToken.equals("!"))
			return false;
		
		if (previousToken.endsWith("'")||previousToken.endsWith("’")
				||currentToken.startsWith("'")||currentToken.startsWith("’"))
			return false;
		
		return true;
	}
}
