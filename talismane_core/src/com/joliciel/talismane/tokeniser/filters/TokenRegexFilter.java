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

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.joliciel.talismane.TalismaneException;

class TokenRegexFilter implements TokenFilter {
	private String regex;
	private Pattern pattern;
	private String replacement;
	private int groupIndex;
	
	private TokenFilterServiceInternal tokeniserFilterService;
	
	public TokenRegexFilter(String regex, String replacement) {
		this(regex, 0, replacement);
	}
	
	public TokenRegexFilter(String regex, int groupIndex, String replacement) {
		super();
		if (regex==null || regex.length()==0)
			throw new TalismaneException("Cannot use an empty regex for a filter");
		this.regex = regex;
		this.groupIndex = groupIndex;
		this.pattern = Pattern.compile(regex);
		this.replacement = replacement;
	}

	@Override
	public Set<TokenPlaceholder> apply(String text) {
		Set<TokenPlaceholder> placeholders = new HashSet<TokenPlaceholder>();
		
		Matcher matcher = pattern.matcher(text);
		int lastStart = -1;
		while (matcher.find()) {
			int start = matcher.start(groupIndex);
			if (start>lastStart) {
				int end = matcher.end(groupIndex);

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
				TokenPlaceholder placeholder = this.tokeniserFilterService.getTokenPlaceholder(start, end, newText, regex);
				placeholders.add(placeholder);
			}
			lastStart = start;
		}
		
		return placeholders;
	}

	public String getRegex() {
		return regex;
	}

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
}
