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
package com.joliciel.talismane.tokeniser.patterns;

import java.util.ArrayList;
import java.util.List;

import com.joliciel.talismane.tokeniser.Token;

/**
 * A token sub-sequence matching a particular TokenPattern within a larger token
 * sequence (representing a sentence).
 * 
 * @author Assaf Urieli
 *
 */
public class TokenPatternMatchSequence implements Comparable<TokenPatternMatchSequence> {
	private final TokenPattern tokenPattern;
	private final List<Token> tokenSequence;
	private final List<Token> tokensToCheck;
	private final List<TokenPatternMatch> tokenPatternMatches = new ArrayList<>();

	TokenPatternMatchSequence(TokenPattern tokenPattern, List<Token> tokenSequence) {
		this.tokenPattern = tokenPattern;
		this.tokenSequence = tokenSequence;
		int i = 0;
		tokensToCheck = new ArrayList<Token>();
		for (Token token : this.tokenSequence) {
			if (this.tokenPattern.getIndexesToTest().contains(i))
				tokensToCheck.add(token);
			i++;
		}
	}

	/**
	 * The pattern that was matched
	 */
	public TokenPattern getTokenPattern() {
		return this.tokenPattern;
	}

	/**
	 * The full token sequence that matched this pattern.
	 */
	public List<Token> getTokenSequence() {
		return this.tokenSequence;
	}

	/**
	 * The list of tokens which need to be tested further.
	 */
	public List<Token> getTokensToCheck() {
		return tokensToCheck;
	}

	public TokenPatternMatch addMatch(Token token) {
		TokenPatternMatch match = null;
		if (token != null) {
			match = new TokenPatternMatch(this, token, this.getTokenPattern(), this.tokenPatternMatches.size());
			token.getMatches().add(match);

			this.tokenPatternMatches.add(match);
		}
		return match;
	}

	public List<TokenPatternMatch> getTokenPatternMatches() {
		return tokenPatternMatches;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((tokenPattern == null) ? 0 : tokenPattern.hashCode());
		result = prime * result + ((getTokensToCheck() == null) ? 0 : getTokensToCheck().get(0).getIndexWithWhiteSpace());
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
		TokenPatternMatchSequence other = (TokenPatternMatchSequence) obj;
		if (tokenPattern == null) {
			if (other.getTokenPattern() != null)
				return false;
		} else if (!tokenPattern.equals(other.getTokenPattern()))
			return false;
		if (getTokensToCheck() == null) {
			if (other.getTokensToCheck() != null)
				return false;
		} else if (getTokensToCheck().get(0).getIndexWithWhiteSpace() != other.getTokensToCheck().get(0).getIndexWithWhiteSpace())
			return false;
		return true;
	}

	@Override
	public String toString() {
		return tokenSequence.toString();
	}

	@Override
	public int compareTo(TokenPatternMatchSequence o) {
		if (this == o)
			return 0;

		int startIndex = tokensToCheck.get(0).getStartIndex();
		int oStartIndex = o.getTokensToCheck().get(0).getStartIndex();

		if (startIndex != oStartIndex) {
			return startIndex - oStartIndex;
		}
		int endIndex = tokensToCheck.get(tokensToCheck.size() - 1).getEndIndex();
		int oEndIndex = o.getTokensToCheck().get(o.getTokensToCheck().size() - 1).getEndIndex();

		if (endIndex != oEndIndex) {
			return endIndex - oEndIndex;
		}

		if (this.getTokenPattern().getRegExp().equals(o.getTokenPattern().getRegExp()))
			return this.hashCode() - o.hashCode();

		return this.getTokenPattern().getRegExp().compareTo(o.getTokenPattern().getRegExp());
	}

}
