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
package com.joliciel.talismane.lang;

import com.joliciel.talismane.LinguisticRules;

class GenericRules implements LinguisticRules {

	@Override
	public boolean shouldAddSpace(String previousToken, String currentToken) {
		// Double quotes are tricky because they could be opening or closing quotations. Most of the time we can simply 
		// count quotes, but not if there's a sentence break inside a quotation.
		// Single quotes are tricky, as they can be apostrophes or actual quotes
		boolean result = false;
		if (previousToken.endsWith("'") || previousToken.endsWith("’") || previousToken.equals("“")
				|| previousToken.equals("(") || previousToken.equals("[")
				|| previousToken.equals("{")) {
			// do nothing
		} else if (currentToken.equals(".")||currentToken.equals(",")||currentToken.equals(")")||currentToken.equals("]")||currentToken.equals("}")||currentToken.equals("”")) {
			// do nothing
		} else if ((previousToken.endsWith(".") || previousToken.equals("?") || previousToken.equals("!"))
				&& (currentToken.equals("\""))) {
			// do nothing
		} else if (currentToken.equals("")) {
			// do nothing for empty tokens
		} else {
			// add a space
			result = true;
		}
		return result;
	}
}
