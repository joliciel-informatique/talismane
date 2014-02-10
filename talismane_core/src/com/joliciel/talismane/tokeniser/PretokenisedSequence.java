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
package com.joliciel.talismane.tokeniser;

/**
 * A token sequence that has been pre-tokenised by another source (manual annotation, external module, etc.).
 * @author Assaf Urieli
 *
 */
public interface PretokenisedSequence extends TokenSequence {
	/**
	 * Adds a token to the current sequence, where the sequence is constructed
	 * from unit tokens, rather than from an existing sentence.
	 * Will automatically attempt to add the correct whitespace prior to this token.
	 * @param text
	 * @return
	 */
	public Token addToken(String string);
}
