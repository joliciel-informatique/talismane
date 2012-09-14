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
package com.joliciel.talismane.tokeniser.filters;

import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;

public class PrettyQuotesFilter implements TokenFilter {

	@Override
	public void apply(TokenSequence tokenSequence) {
		for (Token token : tokenSequence) {
			String text = token.getText();
			if (text.equals("’"))
				token.setText("'");
			else if (text.equals("“")||text.equals("”")||text.equals("„")||text.equals("‟")||text.equals("″")||text.equals("‴"))
				token.setText("\"");
			else if (text.equals("«")||text.equals("»")) {
				token.setText("\"");
				//TODO: remove space after/before this guillemet
			} else if (text.equals("•")) {
				token.setText("*");
			}
		}
	}

}
