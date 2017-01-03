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

import java.util.Map;
import java.util.regex.Pattern;

import com.joliciel.talismane.tokeniser.TokenAttribute;

/**
 * A sentence annotator which identifies areas to annotate using a regular
 * expression. The regular expression can also give context, with the actual
 * sequence to annotate identified by a group in the regex.
 * 
 * @author Assaf Urieli
 *
 */
public interface RegexAnnotator extends SentenceAnnotator {
	/**
	 * The regex to recognise.
	 */
	public String getRegex();

	/**
	 * The pattern used by this filter.
	 * 
	 * @return
	 */
	public Pattern getPattern();

	/**
	 * If provided, indicates the regex capture group index. Useful for
	 * identifying sections by their context. Default is 0, meaning the entire
	 * regex matched.
	 */
	public int getGroupIndex();

	/**
	 * Set of attributes to be assigned to tokens entirely contained in the
	 * sequence identified by this regex.
	 */
	public Map<String, TokenAttribute<?>> getAttributes();

	public void addAttribute(String key, TokenAttribute<?> value);

	/**
	 * If true, will automatically add a word boundary at the beginning and end
	 * of the regex, as long as the regex begins/ends with a letter (inside
	 * round or square brackets or not), or one of the character classes \d, \w,
	 * or \p{WordList|Lower|Upper|Alpha|Digit|ASCII}. Note that a + at the end
	 * of the regex is ignored, but a * or ? is not (as it doesn't guarantee
	 * that the class will be matched). Default is false.
	 */
	public boolean isAutoWordBoundaries();

	/**
	 * If false, will replace any letter by a class containing the uppercase and
	 * lowercase versions of the letter. If the letter has a diacritic, both the
	 * unadorned and adorned uppercase versions will be included. Default is
	 * true.
	 */
	public boolean isCaseSensitive();

	/**
	 * If false, will replace any adorned letter with a class containing both
	 * unadorned and adorned versions. Default is true.
	 */
	public boolean isDiacriticSensitive();
}
