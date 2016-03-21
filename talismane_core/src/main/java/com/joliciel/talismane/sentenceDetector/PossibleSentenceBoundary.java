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
package com.joliciel.talismane.sentenceDetector;

import com.joliciel.talismane.tokeniser.TokenSequence;

public interface PossibleSentenceBoundary {
	/**
	 * The text englobing this possible boundary, where the end of the text is either the real end of the input stream,
	 * or at least n characters beyond the possible boundary.
	 */
	public String getText();
	
	/**
	 * The index of the possible boundary being tested.
	 */
	public int getIndex();
	
	/**
	 * The actual string being tested.
	 */
	public String getBoundaryString();
	
	/**
	 * A token sequence representing the text.
	 */
	public TokenSequence getTokenSequence();
	
	/**
	 * Index of this boundary's token, including whitespace.
	 */
	public int getTokenIndexWithWhitespace();
}
