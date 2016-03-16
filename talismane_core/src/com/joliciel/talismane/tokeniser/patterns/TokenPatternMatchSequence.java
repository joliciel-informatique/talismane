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

import java.util.List;

import com.joliciel.talismane.tokeniser.Token;

/**
 * A token sub-sequence matching a particular TokenPattern
 * within a larger token sequence (representing a sentence).
 * @author Assaf Urieli
 *
 */
public interface TokenPatternMatchSequence {

	/** The pattern that was matched */
	public TokenPattern getTokenPattern();

	/**
	 * The full token sequence that matched this pattern.
	 */
	public List<Token> getTokenSequence();

	/**
	 * The list of tokens which need to be tested further.
	 */
	public List<Token> getTokensToCheck();

	public TokenPatternMatch addMatch(Token token);

	public List<TokenPatternMatch> getTokenPatternMatches();

}