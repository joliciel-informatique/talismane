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

import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.tokeniser.features.TokenWrapper;

/**
 * A token together with a piece of information that is tagged onto it.
 * This can either be a tokeniser decision (whether or not this token is attached to the previous/next token)
 * or a PosTag, or any other piece of information added by the learners.
 * @author Assaf Urieli
 *
 * @param <T>
 */
public interface TaggedToken<T extends TokenTag> extends Comparable<TaggedToken<T>>, TokenWrapper {
	/**
	 * The token being tagged.
	 * @return
	 */
	public Token getToken();
	public void setToken(Token token);
	
	/**
	 * The Tag for this token.
	 * @return
	 */
	public T getTag();
	public void setTag(T tag);
	
	/**
	 * The decision which was used to tag this token.
	 * @return
	 */
	public Decision<T> getDecision();
	
	public double getProbability();
}
