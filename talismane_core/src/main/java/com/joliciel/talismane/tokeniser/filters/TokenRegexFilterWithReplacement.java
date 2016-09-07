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

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.utils.RegexUtils;

class TokenRegexFilterWithReplacement extends AbstractRegexFilter {
	String replacement;

	public TokenRegexFilterWithReplacement(String regex, TalismaneSession talismaneSession) {
		super();
		this.setRegex(regex);
		this.setTalismaneSession(talismaneSession);
	}

	TokenRegexFilterWithReplacement() {
		super();
	}

	public String getReplacement() {
		return replacement;
	}

	public void setReplacement(String replacement) {
		this.replacement = replacement;
	}

	@Override
	protected String findReplacement(String text, Matcher matcher) {
		String newText = RegexUtils.getReplacement(replacement, text, matcher);
		return newText;
	}

	@Override
	public void loadInternal(Map<String, String> parameters, List<String> tabs) throws TokenFilterLoadException {
		if (tabs.size() < 1 || tabs.size() > 2)
			throw new TokenFilterLoadException(
					"Wrong number of additional tabs for " + TokenRegexFilter.class.getSimpleName() + ". Expected 1 or 2, but was " + tabs.size());

		if (tabs.size() == 2) {
			this.setReplacement(tabs.get(1));
		}
	}

}
