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
package com.joliciel.talismane.lexicon;

/**
 * Reads lexical entries from a set of string representing various bits of data
 *  - useful to extract lexical entries from an annotated corpus without
 *  relying on an external lexicon.
 *  Notably, defines internally how to interpret a string containing morphological information
 *  (as per the CONLL data format).
 * @author Assaf Urieli
 *
 */
public interface LexicalEntryMorphologyReader {
	/**
	 * Construct a lexical entry given the component information.
	 * Notably, extract morphological information out of a morphology string.
	 * @param line
	 * @return
	 */
	public LexicalEntry readEntry(String token, String lemma, String category, String morphology);
}
