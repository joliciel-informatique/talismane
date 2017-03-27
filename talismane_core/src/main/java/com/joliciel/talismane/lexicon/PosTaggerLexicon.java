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
package com.joliciel.talismane.lexicon;

import java.util.List;
import java.util.Set;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagSet;

/**
 * An interface for retrieving lexical information from a lexicon, including
 * pos-tag specific information.
 * 
 * @author Assaf Urieli
 *
 */
public interface PosTaggerLexicon extends Lexicon {
  /**
   * For a given word, an ordered set of all postags to be considered in
   * tagging (using the natural ordering for postags).
   * 
   * @param word
   *            the word being considered
   * @return List&lt;PosTag&gt;
   * @throws TalismaneException
   */
  public Set<PosTag> findPossiblePosTags(String word) throws TalismaneException;

  /**
   * Find the lexical entries corresponding to a given postag for this word.
   * 
   * @return an ordered Set containing the lexical entries, or an empty set if
   *         none found
   */
  public List<LexicalEntry> findLexicalEntries(String word, PosTag posTag);

  /**
   * Return all lexical entries for a given lemma and postag.
   */
  public List<LexicalEntry> getEntriesForLemma(String lemma, PosTag posTag);

  /**
   * Return an entry for the same lemma as the lexical entry provided,
   * matching the criteria provided (posTag, gender, number).
   */
  public List<LexicalEntry> getEntriesMatchingCriteria(LexicalEntry lexicalEntry, PosTag posTag, String gender, String number);

  /**
   * The PosTagSet to use when retrieving postags for a given word.
   */
  public abstract PosTagSet getPosTagSet();

  public abstract void setPosTagSet(PosTagSet posTagSet);

  /**
   * The PosTagMapper to use when selecting the possible pos-tags for a given
   * lexical entry.
   */
  public PosTagMapper getPosTagMapper();

  public void setPosTagMapper(PosTagMapper posTagMapper);
}
