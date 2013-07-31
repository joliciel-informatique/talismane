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
package com.joliciel.talismane.tokeniser.patterns;

import java.util.List;
import java.util.ArrayList;

import com.joliciel.talismane.tokeniser.Token;

class TokenPatternMatchSequenceImpl implements TokenPatternMatchSequence {
	private TokenPattern tokenPattern;
	private List<Token> tokenSequence;
	private List<Token> tokensToCheck;
	private List<TokenPatternMatch> tokenPatternMatches = new ArrayList<TokenPatternMatch>();
	
	public TokenPatternMatchSequenceImpl(TokenPattern tokenPattern,
			List<Token> tokenSequence) {
		super();
		this.tokenPattern = tokenPattern;
		this.tokenSequence = tokenSequence;
	}

	/** The pattern that was matched */
	@Override
	public TokenPattern getTokenPattern() {
		return this.tokenPattern;
	}
	
	/**
	 * The full token sequence that matched this pattern.
	 * @return
	 */
	@Override
	public List<Token> getTokenSequence() {
		return this.tokenSequence;
	}
	
	/**
	 * The list of tokens which need to be tested further.
	 * @return
	 */
	@Override
	public List<Token> getTokensToCheck() {
		if (tokensToCheck==null) {
			int i = 0; 
			tokensToCheck = new ArrayList<Token>();
			for (Token token : this.tokenSequence) {
				if (this.tokenPattern.getIndexesToTest().contains(i))
					tokensToCheck.add(token);
				i++;
			}
		}
		return tokensToCheck;
	}

	@Override
	public TokenPatternMatch addMatch(Token token) {
		TokenPatternMatchImpl match = new TokenPatternMatchImpl(this, token, this.getTokenPattern(), this.tokenPatternMatches.size());
		token.getMatches().add(match);

		this.tokenPatternMatches.add(match);
		return match;
	}
	
	@Override
	public List<TokenPatternMatch> getTokenPatternMatches() {
		return tokenPatternMatches;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((tokenPattern == null) ? 0 : tokenPattern.hashCode());
		result = prime * result
				+ ((tokenSequence == null) ? 0 : tokenSequence.get(0).getIndexWithWhiteSpace());
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
		if (tokenSequence == null) {
			if (other.getTokenSequence() != null)
				return false;
		} else if (tokenSequence.get(0).getIndexWithWhiteSpace()!=other.getTokenSequence().get(0).getIndexWithWhiteSpace())
			return false;
		return true;
	}

	@Override
	public String toString() {
		return tokenSequence.toString();
	}
	
	
}
