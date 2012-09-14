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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Takes a regex find/replace pair and applies it.
 * In the case of a text stream, if the pattern find crosses the boundary of prevText + text, the replacement
 * is placed in the text, and if it crosses the boundary of text+nextText, the replacement is placed in nextText.
 * @author Assaf Urieli
 *
 */
public class RegexFindReplaceFilter extends AbstractTextFilter implements TextFilter, TextStreamFilter {
	private Pattern findPattern;
	private String find;
	private String replace;
	
	RegexFindReplaceFilter() {
	}
	
	public RegexFindReplaceFilter(String find, String replace) {
		this.find = find;
		this.replace = replace;
		this.findPattern = Pattern.compile(find);
	}

	@Override
	public List<String> apply(String prevText, String text, String nextText) {
		return this.apply(prevText, text, nextText, this.findPattern, this.replace);
	}
	
	final List<String> apply(String prevText, String text, String nextText, Pattern findPattern, String replace) {
		String context = prevText + text + nextText;
		
		int textStartPos = prevText.length();
		int textEndPos = prevText.length() + text.length();
		StringBuilder prevTextBuilder = new StringBuilder();
		StringBuilder textBuilder = new StringBuilder();
		StringBuilder nextTextBuilder = new StringBuilder();
		Matcher matcher = findPattern.matcher(context);
		StringBuffer sb = new StringBuffer();
		int currentPos = 0;
		while (matcher.find()) {
			int lengthBefore = sb.length();
			matcher.appendReplacement(sb, replace);
			int lengthAfter = sb.length();
			if (matcher.end()<=textStartPos) {
				prevTextBuilder.append(sb.subSequence(lengthBefore, lengthAfter));
			} else if (matcher.start()<textStartPos) {
				prevTextBuilder.append(sb.subSequence(lengthBefore, lengthBefore + (matcher.start() - currentPos)));
				textBuilder.append(sb.subSequence(lengthBefore + (matcher.start() - currentPos), lengthAfter));
			} else if (matcher.end()<=textEndPos) {
				if (currentPos<textStartPos) {
					prevTextBuilder.append(sb.subSequence(lengthBefore, lengthBefore + (textStartPos - currentPos)));
					textBuilder.append(sb.subSequence(lengthBefore + (textStartPos - currentPos), lengthAfter));
				} else {
					textBuilder.append(sb.subSequence(lengthBefore, lengthAfter));
				}
			} else if (matcher.start()<textEndPos) {
				if (currentPos<textStartPos) {
					prevTextBuilder.append(sb.subSequence(lengthBefore, lengthBefore + (textStartPos - currentPos)));
					textBuilder.append(sb.subSequence(lengthBefore + (textStartPos - currentPos), lengthBefore + (matcher.start() - currentPos)));
				} else {
					textBuilder.append(sb.subSequence(lengthBefore, lengthBefore + (matcher.start() - currentPos)));
				}
				nextTextBuilder.append(sb.subSequence(lengthBefore + (matcher.start() - currentPos), lengthAfter));
			} else {
				if (currentPos<textStartPos) {
					prevTextBuilder.append(sb.subSequence(lengthBefore, lengthBefore + (textStartPos - currentPos)));
					textBuilder.append(sb.subSequence(lengthBefore + (textStartPos - currentPos), lengthBefore + (textEndPos - currentPos)));
					nextTextBuilder.append(sb.subSequence(lengthBefore + (textEndPos - currentPos), lengthAfter));
				} else if (currentPos < textEndPos) {
					textBuilder.append(sb.subSequence(lengthBefore, lengthBefore + (textEndPos - currentPos)));
					nextTextBuilder.append(sb.subSequence(lengthBefore + (textEndPos - currentPos), lengthAfter));
				} else {
					nextTextBuilder.append(sb.subSequence(lengthBefore, lengthAfter));
				}
			}
			currentPos = matcher.end();
		}
		int lengthBefore = sb.length();
		matcher.appendTail(sb);
		int lengthAfter = sb.length();
		if (currentPos<textStartPos) {
			prevTextBuilder.append(sb.subSequence(lengthBefore, lengthBefore + (textStartPos - currentPos)));
			textBuilder.append(sb.subSequence(lengthBefore + (textStartPos - currentPos), lengthBefore + (textEndPos - currentPos)));
			nextTextBuilder.append(sb.subSequence(lengthBefore + (textEndPos - currentPos), lengthAfter));
		} else if (currentPos < textEndPos) {
			textBuilder.append(sb.subSequence(lengthBefore, lengthBefore + (textEndPos - currentPos)));
			nextTextBuilder.append(sb.subSequence(lengthBefore + (textEndPos - currentPos), lengthAfter));
		} else {
			nextTextBuilder.append(sb.subSequence(lengthBefore, lengthAfter));
		}

		List<String> result = new ArrayList<String>();
		result.add(prevTextBuilder.toString());
		result.add(textBuilder.toString());
		result.add(nextTextBuilder.toString());
		return result;
	}
	
	public String getFind() {
		return find;
	}

	public String getReplace() {
		return replace;
	}


}
