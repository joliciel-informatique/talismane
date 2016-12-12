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

import java.util.List;
import java.util.regex.Pattern;

/**
 * Normalises all unicode characters representing some sort of double quote to
 * simple double quote, some sort of single quote to a simple apostrophe, and
 * some sort of dash or hyphen to a simple minus sign.
 * 
 * @author Assaf Urieli
 *
 */
public class QuoteNormaliser implements TextReplacer {
	Pattern doubleQuotes = Pattern.compile("[“”„‟″‴«»]");
	Pattern singleQuotes = Pattern.compile("[‘’]");
	Pattern dashes = Pattern.compile("[‒–—―]");

	@Override
	public void replace(List<String> tokens) {
		for (int i = 0; i < tokens.size(); i++) {
			String token = tokens.get(i);
			token = doubleQuotes.matcher(token).replaceAll("\"");
			token = singleQuotes.matcher(token).replaceAll("'");
			token = dashes.matcher(token).replaceAll("-");
			tokens.set(i, token);
		}
	}

}
