///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2014 Assaf Urieli
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
package com.joliciel.talismane.en;

import com.joliciel.talismane.LinguisticRules;

class EnglishRules implements LinguisticRules {

	@Override
	public boolean shouldAddSpace(String previousToken, String currentToken) {
		// Double quotes are tricky because they could be opening or closing quotations. Most of the time we can simply 
		// count quotes, but not if there's a sentence break inside a quotation. The Penn Treebank makes it easy
		// by markning opening and closing quotations differently.
		// Single quotes are tricky, as they can be apostrophes or actual quotes
		boolean result = false;
		if (previousToken.equals("``") || previousToken.equals("“")
				|| previousToken.equals("(") || previousToken.equals("[")
				|| previousToken.equals("{") || previousToken.equals("/")) {
			// do nothing
		} else if (currentToken.equals(".")||currentToken.equals("?")||currentToken.equals("!")
				||currentToken.equals(";")||currentToken.equals("''")||currentToken.equals(":")
				||currentToken.equals(",")||currentToken.equals(")")||currentToken.equals("]")
				||currentToken.equals("}")||currentToken.equals("”") || currentToken.equals("/")) {
			// do nothing
		} else if (currentToken.equals("'s")||currentToken.equals("'re")||currentToken.equals("'ll")
				||currentToken.equals("'m")||currentToken.equals("'d")||currentToken.equals("'ve")||currentToken.equals("n't")) {
			// do nothing, as in dog's, we're, I'm, don't
		} else if (currentToken.equals("")) {
			// do nothing for empty tokens
		} else {
			// add a space
			result = true;
		}
		return result;
	}

}
