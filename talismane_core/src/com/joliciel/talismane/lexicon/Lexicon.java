///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2016 Joliciel Informatique
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

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

/**
 * An interface for retrieving lexical information from a lexicon.
 * @author Assaf Urieli
 *
 */
public interface Lexicon extends Serializable {
	/**
	 * This lexicon's name, for use in features.
	 * @return
	 */
	public String getName();
	
	/**
	 * Return all lexical entries for a given word.
	 * @param name
	 * @return
	 */
	public List<LexicalEntry> getEntries(String word);
	

	/**
	 * Return all lexical entries for a given lemma.
	 * @param lemma
	 * @return
	 */
	public List<LexicalEntry> getEntriesForLemma(String lemma);
	
	/**
	 * Returns all lexical entries in this lexicon, without any guaranteed order.
	 * @return
	 */
	public Iterator<LexicalEntry> getAllEntries();
}
