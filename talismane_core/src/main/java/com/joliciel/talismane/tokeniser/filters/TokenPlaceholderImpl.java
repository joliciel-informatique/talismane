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

import java.util.HashMap;
import java.util.Map;

import com.joliciel.talismane.tokeniser.TokenAttribute;

class TokenPlaceholderImpl implements TokenPlaceholder {
	private int startIndex;
	private int endIndex;
	private String replacement;
	private String regex;
	private boolean possibleSentenceBoundary = true;
	private boolean singleToken = true;
	private Map<String, TokenAttribute<?>> attributes = new HashMap<String, TokenAttribute<?>>();

	public TokenPlaceholderImpl() {
	}

	@Override
	public int getStartIndex() {
		return startIndex;
	}

	public void setStartIndex(int startIndex) {
		this.startIndex = startIndex;
	}

	@Override
	public int getEndIndex() {
		return endIndex;
	}

	public void setEndIndex(int endIndex) {
		this.endIndex = endIndex;
	}

	@Override
	public String getReplacement() {
		return replacement;
	}

	public void setReplacement(String replacement) {
		this.replacement = replacement;
	}

	@Override
	public String toString() {
		return "TokenPlaceholderImpl [startIndex=" + startIndex + ", endIndex=" + endIndex + ", replacement=" + replacement + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + endIndex;
		result = prime * result + ((replacement == null) ? 0 : replacement.hashCode());
		result = prime * result + startIndex;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TokenPlaceholderImpl other = (TokenPlaceholderImpl) obj;
		if (endIndex != other.endIndex)
			return false;
		if (replacement == null) {
			if (other.replacement != null)
				return false;
		} else if (!replacement.equals(other.replacement))
			return false;
		if (startIndex != other.startIndex)
			return false;
		return true;
	}

	@Override
	public String getRegex() {
		return regex;
	}

	public void setRegex(String regex) {
		this.regex = regex;
	}

	@Override
	public boolean isPossibleSentenceBoundary() {
		return possibleSentenceBoundary;
	}

	@Override
	public void setPossibleSentenceBoundary(boolean possibleSentenceBoundary) {
		this.possibleSentenceBoundary = possibleSentenceBoundary;
	}

	@Override
	public Map<String, TokenAttribute<?>> getAttributes() {
		return attributes;
	}

	@Override
	public void addAttribute(String key, TokenAttribute<?> value) {
		attributes.put(key, value);
	}

	@Override
	public boolean isSingleToken() {
		return singleToken;
	}

	@Override
	public void setSingleToken(boolean singleToken) {
		this.singleToken = singleToken;
	}
}