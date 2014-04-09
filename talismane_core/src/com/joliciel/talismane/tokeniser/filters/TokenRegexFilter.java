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

/**
 * A token filter which recognises a token based on a regular expression.
 * The regular expression can also give context, with the actual token identified by a group in the regex.
 * Also, the token filter can optionally replace the token contents by a replacement, which can use the $1, $2, etc. notation
 * to represent the content of different groups in the original match.
 * @author Assaf Urieli
 *
 */
public interface TokenRegexFilter extends TokenFilter {
	/**
	 * The regex to recognise.
	 * @return
	 */
	public String getRegex();

	/**
	 * The replacement to replace it with.
	 * @return
	 */
	public String getReplacement();

	/**
	 * The group index to tokenise and replace within the regex.
	 * @return
	 */
	public int getGroupIndex();
	
	/**
	 * Can this regex represent a sentence boundary (at its last character that is)?
	 * @return
	 */
	public boolean isPossibleSentenceBoundary();

	public void setPossibleSentenceBoundary(boolean possibleSentenceBoundary);

}