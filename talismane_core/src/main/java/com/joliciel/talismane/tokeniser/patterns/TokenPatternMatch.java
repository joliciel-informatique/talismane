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
import com.joliciel.talismane.tokeniser.features.TokenWrapper;

/**
 * A single match for a token on a given TokeniserPattern, giving the index of the match.
 * @author Assaf
 *
 */
public interface TokenPatternMatch extends TokenWrapper {
	public Token getToken();
	public TokenPattern getPattern();
	
	/**
	 * The index of this pattern match in the sequence containing it.
	 */
	public int getIndex();
	
	/**
	 * The sequence containing this pattern match.
	 */
	public TokenPatternMatchSequence getSequence();
}