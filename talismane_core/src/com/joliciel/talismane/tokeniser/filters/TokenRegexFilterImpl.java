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

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.machineLearning.ExternalResourceFinder;
import com.joliciel.talismane.machineLearning.ExternalWordList;
import com.joliciel.talismane.utils.RegexUtils;

class TokenRegexFilterImpl implements TokenRegexFilter {
	private static Pattern wordListPattern = Pattern.compile("\\\\p\\{WordList\\((.*?)\\)\\}", Pattern.UNICODE_CHARACTER_CLASS);
	private String regex;
	private Pattern pattern;
	private String replacement;
	private int groupIndex;
	private boolean possibleSentenceBoundary = true;
	private Map<String,String> attributes = new HashMap<String,String>();
	
	private TokenFilterServiceInternal tokeniserFilterService;
	private ExternalResourceFinder externalResourceFinder;

	public TokenRegexFilterImpl(String regex) {
		this(regex, 0, null);
	}
	
	public TokenRegexFilterImpl(String regex, String replacement) {
		this(regex, 0, replacement);
	}
	
	public TokenRegexFilterImpl(String regex, int groupIndex, String replacement) {
		super();
		if (regex==null || regex.length()==0)
			throw new TalismaneException("Cannot use an empty regex for a filter");
		this.regex = regex;
		this.groupIndex = groupIndex;
		this.replacement = replacement;
	}

	@Override
	public Set<TokenPlaceholder> apply(String text) {
		Set<TokenPlaceholder> placeholders = new HashSet<TokenPlaceholder>();
		
		Matcher matcher = this.getPattern().matcher(text);
		int lastStart = -1;
		while (matcher.find()) {
			int start = matcher.start(groupIndex);
			if (start>lastStart) {
				int end = matcher.end(groupIndex);
				String newText = RegexUtils.getReplacement(replacement, text, matcher);
				TokenPlaceholder placeholder = this.tokeniserFilterService.getTokenPlaceholder(start, end, newText, regex);
				placeholder.setPossibleSentenceBoundary(this.possibleSentenceBoundary);
				for (String key : attributes.keySet())
					placeholder.addAttribute(key, attributes.get(key));
				placeholders.add(placeholder);
			}
			lastStart = start;
		}
		
		return placeholders;
	}

	@Override
	public String getRegex() {
		return regex;
	}

	@Override
	public String getReplacement() {
		return replacement;
	}

	public TokenFilterServiceInternal getTokeniserFilterService() {
		return tokeniserFilterService;
	}

	public void setTokeniserFilterService(
			TokenFilterServiceInternal tokeniserFilterService) {
		this.tokeniserFilterService = tokeniserFilterService;
	}

	@Override
	public String toString() {
		return "TokenRegexFilter [regex=" + regex + ", replacement="
				+ replacement + "]";
	}

	@Override
	public boolean isPossibleSentenceBoundary() {
		return possibleSentenceBoundary;
	}

	@Override
	public void setPossibleSentenceBoundary(boolean possibleSentenceBoundary) {
		this.possibleSentenceBoundary = possibleSentenceBoundary;
	}

	public int getGroupIndex() {
		return groupIndex;
	}

	public void setGroupIndex(int groupIndex) {
		this.groupIndex = groupIndex;
	}

	@Override
	public Map<String,String> getAttributes() {
		return attributes;
	}

	@Override
	public void addAttribute(String key, String value) {
		attributes.put(key, value);
	}

	Pattern getPattern() {
		if (pattern==null) {
			// we may need to replace WordLists by the list contents
			StringBuilder regexBuilder = new StringBuilder();
			Matcher matcher = wordListPattern.matcher(regex);
			int lastIndex = 0;
			while (matcher.find()) {
				String[] params = matcher.group(1).split(",");
				int start = matcher.start();
				int end = matcher.end();
				regexBuilder.append(regex.substring(lastIndex, start));
				
				String wordListName = params[0];
				boolean uppercaseOptional=false;
				boolean diacriticsOptional=false;
				if (params.length>1)
					diacriticsOptional = params[1].equalsIgnoreCase("true");
				if (params.length>2)
					uppercaseOptional = params[2].equalsIgnoreCase("true");
				
				ExternalWordList wordList = externalResourceFinder.getExternalWordList(wordListName);
				StringBuilder sb = new StringBuilder();

				boolean firstWord = true;
				for (String word : wordList.getWordList()) {
					if (!firstWord)
						sb.append("|");
					word = Normalizer.normalize(word, Form.NFC);
					if (uppercaseOptional || diacriticsOptional) {
						String wordNoDiacritics = Normalizer.normalize(word, Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
						String wordLowercase = word.toLowerCase(Locale.ENGLISH);
						String wordLowercaseNoDiacritics = Normalizer.normalize(wordLowercase, Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
						
						boolean needsGrouping = false;
						if (uppercaseOptional && !word.equals(wordLowercase))
							needsGrouping = true;
						if (diacriticsOptional && !word.equals(wordNoDiacritics))
							needsGrouping = true;
						if (needsGrouping) {
							for (int i=0; i<word.length(); i++) {
								char c = word.charAt(i);
								
								boolean grouped = false;
								if (uppercaseOptional && c!=wordLowercase.charAt(i))
									grouped = true;
								if (diacriticsOptional && c!=wordNoDiacritics.charAt(i))
									grouped = true;
								
								if (!grouped)
									sb.append(c);
								else {
									sb.append("[");
									sb.append(c);
									if (uppercaseOptional && c!=wordLowercase.charAt(i))
										sb.append(wordLowercase.charAt(i));
									if (diacriticsOptional && c!=wordNoDiacritics.charAt(i) && wordLowercase.charAt(i)!=wordNoDiacritics.charAt(i))
										sb.append(wordNoDiacritics.charAt(i));
									if (uppercaseOptional && diacriticsOptional && c!=wordLowercaseNoDiacritics.charAt(i)
											&& wordLowercase.charAt(i)!=wordLowercaseNoDiacritics.charAt(i)
											&& wordNoDiacritics.charAt(i)!=wordLowercaseNoDiacritics.charAt(i))
										sb.append(wordLowercaseNoDiacritics.charAt(i));
									
									sb.append("]");
								} // does this letter need grouping?
							} // next letter
						} else {
							sb.append(word);
						} // any options activated?
					} else {
						sb.append(word);
					}
					firstWord = false;
				} // next word in list
				
				regexBuilder.append(sb.toString());
				lastIndex = end;
			} // next match
			regexBuilder.append(regex.substring(lastIndex));
			String myRegex = regexBuilder.toString();
			this.pattern = Pattern.compile(myRegex, Pattern.UNICODE_CHARACTER_CLASS);
		}
		return pattern;
	}

	public ExternalResourceFinder getExternalResourceFinder() {
		return externalResourceFinder;
	}

	public void setExternalResourceFinder(
			ExternalResourceFinder externalResourceFinder) {
		this.externalResourceFinder = externalResourceFinder;
	}
	
}
