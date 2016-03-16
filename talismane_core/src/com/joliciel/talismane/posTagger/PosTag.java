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
package com.joliciel.talismane.posTagger;

import java.io.Serializable;

import com.joliciel.talismane.tokeniser.TokenTag;

/**
 * A part of speech tag, representing a certain morpho-syntaxic category for a word.
 * @author Assaf Urieli
 * 
 */
public interface PosTag extends TokenTag, Serializable, Comparable<PosTag> {
	/**
	 * An empty PosTag is used for the "empty" sentence start element in n-gram models.
	 * @return true if this is the empty PosTag, false otherwise
	 */
	public boolean isEmpty();

	/**
	 * The PosTag's unique code.
	 */
	public String getCode();
	
	/**
	 * Description of this PosTag.
	 */
	public String getDescription();
	
	/**
	 * Is this PosTag an open or a closed class, and which type of open/closed class?
	 */
	public PosTagOpenClassIndicator getOpenClassIndicator();
	
	/**
	 * A null pos tag, to be used for empty tokens that should be discarded.
	 */
	public static PosTag NULL_POS_TAG = new NullPosTag();
	
	/**
	 * An artificial pos-tag used to indicate the artificial root added to all sentences
	 * for parsing.
	 */
	public static PosTag ROOT_POS_TAG = new RootPosTag();
	
	/**
	 * The code to be used by the null pos tag.
	 */
	public static String NULL_POS_TAG_CODE = "null";
	
	/**
	 * The code to be used by the root pos tag. 
	 */
	public static String ROOT_POS_TAG_CODE = "root";

}
