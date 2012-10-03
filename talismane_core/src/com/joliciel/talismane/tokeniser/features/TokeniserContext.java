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
package com.joliciel.talismane.tokeniser.features;

import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenisedAtomicTokenSequence;

public class TokeniserContext implements TokenWrapper {
	private Token token;
	private TokenisedAtomicTokenSequence history;
	public TokeniserContext(Token token,
			TokenisedAtomicTokenSequence history) {
		super();
		this.token = token;
		this.history = history;
	}
	public Token getToken() {
		return token;
	}
	public TokenisedAtomicTokenSequence getHistory() {
		return history;
	}
	

}
