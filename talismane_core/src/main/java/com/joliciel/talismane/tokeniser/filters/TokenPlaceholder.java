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

/**
 * A place-holder that will be replaced by a proper token when tokenising.
 * 
 * @author Assaf Urieli
 *
 */
public class TokenPlaceholder {
	private String replacement;
	private String regex;
	private boolean possibleSentenceBoundary = true;

	public TokenPlaceholder(String replacement, String regex) {
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

	@Override
	public String toString() {
		return "TokenPlaceholder [replacement=" + replacement + ", regex=" + regex + ", possibleSentenceBoundary=" + possibleSentenceBoundary + "]";
	}
}
