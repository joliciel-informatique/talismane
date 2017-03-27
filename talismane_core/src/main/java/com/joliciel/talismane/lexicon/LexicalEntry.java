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

import java.io.Serializable;
import java.util.List;

/**
 * A single lexical entry for a given string.
 * 
 * @author Assaf Urieli
 *
 */
public interface LexicalEntry extends Serializable {
  /**
   * The original text of this entry.
   */
  public String getWord();

  /**
   * The lemma for this lexical entry.
   */
  public String getLemma();

  /**
   * In some cases, a lemma may be accompanied by a complement, to
   * differentiate it from other lemmas with the same text and category.
   */
  public String getLemmaComplement();

  /**
   * The original grammatical category of this entry, using the categorisation
   * of the lexicon.
   */
  public String getCategory();

  /**
   * Many lexicons provide a two-level part-of-speech, with a category and a
   * sub-category.
   */
  public String getSubCategory();

  /**
   * A list of possible (language-specific) genders for this entry. In French,
   * this will include entries such as "masculine", "feminine".
   */
  public List<String> getGender();

  /**
   * A list of possible (language-specific) numbers for this entry. In French,
   * this will include entries such as "singular", "plural".
   */
  public List<String> getNumber();

  /**
   * A list of possible (language-specific) tenses for this entry, when the
   * entry is a verb.
   */
  public List<String> getTense();

  /**
   * A list of possible (language-specific) moods for this entry, when the
   * entry is a verb.
   */
  public List<String> getMood();

  /**
   * A list of possible (language-specific) aspects for this entry, when the
   * entry is a verb.
   */
  public List<String> getAspect();

  /**
   * A list of possible persons for this entry. In French, this will inlude
   * entries such as "1st person", "2nd person", "3rd person".
   */
  public List<String> getPerson();

  /**
   * A list of possible (language-specific) numbers for the possessor in this
   * entry, when the entry is a possessive determinant or pronoun.
   */
  public List<String> getPossessorNumber();

  /**
   * A list of possible (language-specific) grammatical cases for this entry,
   * for languages with case markers, e.g. Nominative, Accusative, Genitive,
   * Dative, etc.
   */
  public List<String> getCase();

  /**
   * The original string representation of morpho-syntaxic information (e.g.
   * gender, number, tense, person, possessor number), often existing in some
   * condensed form in lexicons.
   */
  public String getMorphology();

  /**
   * Get any attribute for this lexical entry, as a String.<br/>
   * If the attribute is a {@link LexicalAttribute}, it will return the
   * corresponding attribute.<br/>
   * If it is a String, will try to match it to an existing OtherAttribute,
   * and return the corresponding value.<br/>
   * If the attribute is a List, the items will be joined together into a
   * single pipe-delimited String.
   */
  public String getAttribute(String attribute);

  /**
   * Like {@link #getAttribute(String)}, but if the original attribute is a
   * String, will convert it to a List of length 1.
   */
  public List<String> getAttributeAsList(String attribute);

  /**
   * The name of the lexicon which contained this entry.
   */
  public String getLexiconName();

  /**
   * Returns true if the lexical attribute provided exists for this lexical
   * entry.
   */
  public boolean hasAttribute(LexicalAttribute attribute);
}
