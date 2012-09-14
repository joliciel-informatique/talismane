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
package com.joliciel.talismane.posTagger;

import java.util.List;

import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;

/**
 * A sequence of postags applied to a given token sequence.
 * @author Assaf Urieli
 *
 */
public interface PosTagSequence extends List<PosTaggedToken>, Comparable<PosTagSequence> {
	/**
	 * Get the Token Sequence on which this PosTagSequence is based.
	 * @return
	 */
	public TokenSequence getTokenSequence();
	
	/**
	 * The score is calculated as the geometric mean of postag decisions,
	 * multiplied by the score of the token sequence.
	 */
	public double getScore();
	
	/**
	 * Get the next token for which no pos tag has yet been assigned.
	 * @return
	 */
	public Token getNextToken();
	
	/**
	 * Prepend a root to this PosTagSequence, unless there's a root already,
	 * and return the prepended root.
	 */
	public PosTaggedToken prependRoot();
}
