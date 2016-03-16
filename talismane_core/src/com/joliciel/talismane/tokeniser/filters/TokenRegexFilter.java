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

import java.util.Map;

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
	 */
	public String getRegex();

	/**
	 * If provided, will replace the token's text with this replacement.
	 */
	public String getReplacement();
	public void setReplacement(String replacement);

	/**
	 * If provided, indicates the group index to tokenise (and possibly replace). Useful for 
	 * identifying tokens by their context. Default is 0, meaning the entire regex is tokenised and replaced.
	 */
	public int getGroupIndex();
	public void setGroupIndex(int groupIndex);
	
	/**
	 * Can this regex represent a sentence boundary (at its last character that is)?
	 */
	public boolean isPossibleSentenceBoundary();
	public void setPossibleSentenceBoundary(boolean possibleSentenceBoundary);
	
	/**
	 * Set of attributes to be assigned to tokens recognised by this regex filter.
	 */
	Map<String,String> getAttributes();
	public void addAttribute(String key, String value);

	/**
	 * If true, will automatically add a word boundary at the beginning and end of the regex,
	 * as long as the regex begins/ends with a letter (inside round or square brackets or not),
	 * or one of the character classes \d, \w, or \p{WordList|Lower|Upper|Alpha|Digit|ASCII}. Note that a + at the end of the regex
	 * is ignored, but a * or ? is not (as it doesn't guarantee that the class will be matched).
	 * Default is false.
	 */
	public boolean isAutoWordBoundaries();
	public void setAutoWordBoundaries(boolean autoWordBoundaries);

	/**
	 * If false, will replace any letter by a class containing the uppercase and lowercase
	 * versions of the letter. If the letter has a diacritic, both the unadorned and adorned
	 * uppercase versions will be included.
	 * Default is true.
	 */
	public boolean isCaseSensitive();
	public void setCaseSensitive(boolean caseSensitive);

	/**
	 * If false, will replace any adorned letter with a class containing both unadorned and adorned
	 * versions.
	 * Default is true.
	 */
	public boolean isDiacriticSensitive();
	public void setDiacriticSensitive(boolean diacriticSensitive);

	/**
	 * Verify that this filter can be used with the parameters provided, otherwise throws a TalismaneException.
	 */
	public void verify();
}