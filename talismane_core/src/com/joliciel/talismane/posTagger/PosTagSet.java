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
import java.util.Locale;
import java.util.Set;

/**
 * <p>A tag set to be used for pos tagging.
 * The default format for reading a PosTagSet from a file is as follows:</p>
 * <p>All lines starting with # are ignored.
 * The first line read is the PosTagSet name.
 * The second line read is the PosTagSet locale.</p>
 * All further lines are postags, in a tab delimited format shown below:</p>
 * <pre>PosTag	description	PosTagOpenClassIndicator</pre>
 * <p>For example:</p>
 * <pre>
 * # Example of a PosTagSet file
 * Talismane 2013
 * fr
 * ADJ	adjectif	OPEN
 * ADV	adverbe	OPEN
 * ADVWH	adverbe intérrogatif	CLOSED
 * CC	conjonction de coordination	CLOSED
 * PONCT	ponctuation	PUNCTUATION
 * </pre>
 * 
 * @see PosTag
 * @see PosTagOpenClassIndicator
 * @author Assaf Urieli
 *
 */
public interface PosTagSet extends Serializable {
	/**
	 * Name of this posTagSet.
	 * @return
	 */
	public String getName();
	
	/**
	 * The locale to which this PosTagSet applies.
	 * @return
	 */
	public Locale getLocale();
	
	/**
	 * Returns the full tagset.
	 */
	Set<PosTag> getTags();
	
	/**
	 * Return the PosTag corresponding to a given code.
	 * @param code
	 */
	PosTag getPosTag(String code);
}
