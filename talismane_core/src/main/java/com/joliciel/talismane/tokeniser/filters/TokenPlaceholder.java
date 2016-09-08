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

/**
 * A place-holder that will be replaced by a proper token when tokenising.<br/>
 * A placeholder where {@link #isSingleToken()} returns false is an attribute
 * placeholder, which only adds attributes. In this case, only a single
 * attribute of a given key can be added per token. If two attribute
 * placeholders overlap and assign the same key, the one with the lower start
 * index is privileged for all tokens, and the other one is skipped for all
 * tokens. If they have the same start index, the one with the higher end index
 * is priveleged for all tokens, and the other one is skipped for all tokens.
 * 
 * @author Assaf Urieli
 *
 */
public class TokenPlaceholder implements Comparable<TokenPlaceholder> {
	private int startIndex;
	private int endIndex;
	private String replacement;
	private String regex;
	private boolean possibleSentenceBoundary = true;
	private boolean singleToken = true;
	private Map<String, TokenAttribute<?>> attributes = new HashMap<String, TokenAttribute<?>>();

	public TokenPlaceholder(int startIndex, int endIndex, String replacement, String regex) {
		super();
		this.startIndex = startIndex;
		this.endIndex = endIndex;
		this.replacement = replacement;
		this.regex = regex;
	}

	/**
	 * The replacement text for this placeholder.
	 */
	public String getReplacement() {
		return this.replacement;
	}

	/**
	 * Start index for this placeholder.
	 */
	public int getStartIndex() {
		return this.startIndex;
	}

	/**
	 * The index just after this placeholder ends.
	 */
	public int getEndIndex() {
		return this.endIndex;
	}

	/**
	 * The regex which matched this placeholder.
	 */
	public String getRegex() {
		return this.regex;
	}

	/**
	 * Can this placeholder represent a sentence boundary (at its last character
	 * that is)?
	 */
	public boolean isPossibleSentenceBoundary() {
		return this.possibleSentenceBoundary;
	}

	public void setPossibleSentenceBoundary(boolean possibleSentenceBoundary) {
		this.possibleSentenceBoundary = possibleSentenceBoundary;
	}

	/**
	 * Set of attributes to be assigned to tokens recognised by this regex
	 * filter.
	 */
	public Map<String, TokenAttribute<?>> getAttributes() {
		return this.attributes;
	}

	public void addAttribute(String key, TokenAttribute<?> value) {
		this.attributes.put(key, value);
	}

	/**
	 * Should this placeholder be interpreted as a single token, or should it
	 * simply be used to add attributes to all tokens matched by it.
	 */
	public boolean isSingleToken() {
		return this.singleToken;
	}

	public void setSingleToken(boolean singleToken) {
		this.singleToken = singleToken;
	}

	@Override
	public String toString() {
		return "TokenPlaceholder [startIndex=" + startIndex + ", endIndex=" + endIndex + ", replacement=" + replacement + ", regex=" + regex
				+ ", possibleSentenceBoundary=" + possibleSentenceBoundary + ", singleToken=" + singleToken + ", attributes=" + attributes + "]";
	}

	/**
	 * Comparison order is: order by start index. If start indexes are
	 * identical, order by reverse end index (to give precedence to longer
	 * attributes).
	 */
	@Override
	public int compareTo(TokenPlaceholder o) {
		if (this.startIndex != o.startIndex)
			return this.startIndex - o.startIndex;
		if (this.endIndex != o.endIndex)
			return o.endIndex - this.endIndex;
		return this.hashCode() - o.hashCode();
	}
}
