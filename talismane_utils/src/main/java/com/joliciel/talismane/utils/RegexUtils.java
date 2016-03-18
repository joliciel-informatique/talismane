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
package com.joliciel.talismane.utils;

import java.util.regex.Matcher;

/**
 * Various generic utilities for use with patterns and regexes.
 * @author Assaf Urieli
 *
 */
public class RegexUtils {
	/**
	 * For a given replacement string which can include $1, $2, etc. placeholders,
	 * and a given original text with matcher, returns a modified replacement
	 * string which fills in the placeholders from the original text.
	 * @param replacement the replacment string
	 * @param text the original text
	 * @param matcher the matcher
	 */
	public static String getReplacement(String replacement, String text, Matcher matcher) {
		String newText = replacement;
		if (replacement!=null) {
			if (replacement.indexOf('$')>=0) {
				StringBuilder sb = new StringBuilder();
				boolean backslash = false;
				boolean dollar = false;
				String group = "";
				for (int i=0; i<replacement.length(); i++) {
					char c = replacement.charAt(i);
					if (c=='\\') {
						backslash = true;
					} else if (c=='$' && !backslash) {
						dollar = true;
					} else if (Character.isDigit(c) && dollar) {
						group += c;
					} else {
						if (dollar) {
							int groupNumber = Integer.parseInt(group);
							sb.append(text.substring(matcher.start(groupNumber), matcher.end(groupNumber)));
							group = "";
						}
						sb.append(c);
						backslash = false;
						dollar = false;
					} // character type
				} // next character
				if (dollar) {
					int groupNumber = Integer.parseInt(group);
					sb.append(text.substring(matcher.start(groupNumber), matcher.end(groupNumber)));
				}

				newText = sb.toString();
			} // has dollars
		} // has replacement
		return newText;
	}
}
