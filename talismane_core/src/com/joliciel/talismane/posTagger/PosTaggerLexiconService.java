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
import java.util.Set;


/**
 * An interface for retrieving lexical information from a lexicon.
 * @author Assaf Urieli
 *
 */
public interface PosTaggerLexiconService extends LexiconService {
	/**
	 * For a given word, a set of all postags to be considered in tagging.
	 * @param word the word being considered
	 * @return List&lt;PosTag&gt;
	 */
	Set<PosTag> findPossiblePosTags(String word);
	
	/**
	 * Find the lexical entries corresponding to a given postag for this word.
	 * @param word
	 * @param posTag
	 * @return
	 */
	Set<LexicalEntry> findLexicalEntries(String word, PosTag posTag);
	

	/**
	 * Return all lexical entries for a given lemma and postag.
	 * @param lemma
	 * @param complement
	 * @return
	 */
	public List<? extends LexicalEntry> getEntriesForLemma(String lemma, String complement, PosTag posTag);

	/**
	 * Return an entry for the same lemma as the lexical entry provided, matching the criteria provided (posTag, gender, number).
	 * @param lexicalEntry
	 * @param posTag
	 * @param gender
	 * @param number
	 * @return
	 */
	public List<? extends LexicalEntry> getEntriesMatchingCriteria(LexicalEntry lexicalEntry, PosTag posTag, String gender, String number);

	
	/**
	 * The PosTagSet to use when retrieving postags for a given word.
	 * @return
	 */
	public abstract PosTagSet getPosTagSet();
	public abstract void setPosTagSet(PosTagSet posTagSet);
}
