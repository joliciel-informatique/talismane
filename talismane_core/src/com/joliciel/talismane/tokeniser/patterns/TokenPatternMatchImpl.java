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

import com.joliciel.talismane.tokeniser.Token;

class TokenPatternMatchImpl implements TokenPatternMatch {
	private TokenPattern pattern;
	private int index;
	private Token token;
	private TokenPatternMatchSequence sequence;
	
	public TokenPatternMatchImpl(TokenPatternMatchSequence sequence,
			Token token, TokenPattern pattern, int index) {
		this.sequence = sequence;
		this.token = token;
		this.pattern = pattern;
		this.index = index;
	}
	@Override
	public TokenPattern getPattern() {
		return pattern;
	}
	@Override
	public int getIndex() {
		return index;
	}
	@Override
	public Token getToken() {
		return token;
	}
	public TokenPatternMatchSequence getSequence() {
		return sequence;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + index;
		result = prime * result + ((pattern == null) ? 0 : pattern.hashCode());
		result = prime * result + ((token == null) ? 0 : token.hashCode());
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
		TokenPatternMatchImpl other = (TokenPatternMatchImpl) obj;
		if (index != other.index)
			return false;
		if (pattern == null) {
			if (other.pattern != null)
				return false;
		} else if (!pattern.equals(other.pattern))
			return false;
		if (token == null) {
			if (other.token != null)
				return false;
		} else if (!token.equals(other.token))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "TokenPatternMatch [pattern=" + pattern + ", index=" + index
				+ ", token=" + token + ", sequence=" + sequence + "]";
	}
	
	
	
}
