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

import java.util.List;
import java.util.Set;

/**
 * A single lexical entry for a given string.
 * @author Assaf Urieli
 *
 */
public interface LexicalEntry {
	/**
	 * The original text of this entry.
	 * @return
	 */
	public String getWord();
	
	/**
	 * The lemma for this lexical entry.
	 * @return
	 */
	public String getLemma();
	
	/**
	 * In some cases, a lemma may be accompanied by a complement, to differentiate
	 * it from other lemmas with the same text and category.
	 * @return
	 */
	public String getLemmaComplement();
	
	/**
	 * The original grammatical category of this entry, using the categorisation of the lexicon.
	 * @return
	 */
	public String getCategory();
	
	/**
	 * Many lexicons provide a two-level part-of-speech, with a category and a sub-category.
	 * @return
	 */
	public String getSubCategory();
	
	/**
	 * This entry's predicate structure, when available.
	 * @return
	 */
	public String getPredicate();
	
	/**
	 * A list of predicate arguments (where the order is typically significant)
	 * forming this lexical entry's predicate-argument structure.
	 * @return
	 */
	public List<PredicateArgument> getPredicateArguments();
	
	/**
	 * Returns the predicate argument corresponding to a particular function name,
	 * or null if not found.
	 * @param name
	 * @return
	 */
	public PredicateArgument getPredicateArgument(String functionName);
	
	/**
	 * A set of so-called "macros" describing more abstract aspects of the predicate, 
	 * e.g. impersonal construction, passive construction, etc.
	 * @return
	 */
	public Set<String> getPredicateMacros();
	
	/**
	 * A list of possible (language-specific) genders for this entry.
	 * In French, this will include entries such as "masculine", "feminine".
	 * @return
	 */
	public List<String> getGender();
	
	/**
	 * A list of possible (language-specific) numbers for this entry.
	 * In French, this will include entries such as "singular", "plural".
	 * @return
	 */
	public List<String> getNumber();
	
	/**
	 * A list of possible (language-specific) tenses/moods for this entry, when the entry is a verb.
	 * @return
	 */
	public List<String> getTense();
	
	/**
	 * A list of possible persons for this entry.
	 * In French, this will inlude entries such as "1st person", "2nd person", "3rd person".
	 * @return
	 */
	public List<String> getPerson();
	
	/**
	 * A list of possible (language-specific) numbers for the possessor in this entry,
	 * when the entry is a possessive determinant or pronoun.
	 * @return
	 */
	public List<String> getPossessorNumber();
	
	/**
	 * A string representation of all of the morpho-syntaxic information combined
	 * (gender, number, tense, person, possessor number).
	 * @return
	 */
	public String getMorphology();
	
	/**
	 * Status of this lexical entry, in the case of homographs with different lemmas
	 * within the same grammatical category.
	 * @return
	 */
	public LexicalEntryStatus getStatus();
	
	/**
	 * A string representation of all of the morpho-syntaxic information combined
	 * in conll format.
	 * @return
	 */
	public String getMorphologyForCoNLL();
}
