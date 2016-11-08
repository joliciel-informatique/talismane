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

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.tokeniser.Tokeniser;

public abstract class PatternTokeniser extends Tokeniser {
	protected final TokeniserPatternManager tokeniserPatternManager;

	PatternTokeniser(PatternTokeniser that) {
		super(that.getTalismaneSession());
		this.tokeniserPatternManager = that.tokeniserPatternManager;
	}

	public PatternTokeniser(TalismaneSession talismaneSession, TokeniserPatternManager tokeniserPatternManager) {
		super(talismaneSession);
		this.tokeniserPatternManager = tokeniserPatternManager;
	}

	public TokeniserPatternManager getTokeniserPatternManager() {
		return this.tokeniserPatternManager;
	}
}
