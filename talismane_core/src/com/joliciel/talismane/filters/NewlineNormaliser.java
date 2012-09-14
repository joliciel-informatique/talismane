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
package com.joliciel.talismane.filters;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Normalised all line separators to a single standard (the newline character).
 * @author Assaf Urieli
 *
 */
public class NewlineNormaliser extends RegexFindReplaceFilter implements TextFilter, TextStreamFilter {
	private String[][] patternStrings = new String[][] {
			{"[\f\r]\n?", "\n"},
	};
	
	private List<Pattern> findPatterns = new ArrayList<Pattern>();
	private List<String> replaceStrings = new ArrayList<String>();
	
	public NewlineNormaliser() {
		super();
		for (String[] findReplace : patternStrings) {
			Pattern findPattern = Pattern.compile(findReplace[0]);
			String replaceString = findReplace[1];
			findPatterns.add(findPattern);
			replaceStrings.add(replaceString);
		}
	}

	@Override
	public List<String> apply(String prevText, String text, String nextText) {
		List<String> result = new ArrayList<String>();
		for (int i=0;i<findPatterns.size();i++) {
			Pattern findPattern = findPatterns.get(i);
			String replaceString = replaceStrings.get(i);
			result = this.apply(prevText, text, nextText, findPattern, replaceString);
			prevText = result.get(0);
			text = result.get(1);
			nextText = result.get(2);
		}
		return result;
	}
	
	

}
