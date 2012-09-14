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
package com.joliciel.talismane.tokeniser;

import java.util.List;

/**
 * A sequence of tagged tokens with a score.
 * @author Assaf Urieli
 *
 */
public interface TaggedTokenSequence<T extends TokenTag> extends List<TaggedToken<T>> {

	/**
	 * Get the sum of logs of the weights.
	 * @return
	 */
	public double getScore();
	
	/**
	 * Add a tagged token to the end of the current tagged token list.
	 * @param string
	 * @param posTag
	 * @return
	 */
	public TaggedToken<T> addTaggedToken(Token token, T tag, double probability);
}
