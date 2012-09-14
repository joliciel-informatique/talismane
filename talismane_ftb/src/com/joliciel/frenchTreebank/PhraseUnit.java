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
package com.joliciel.frenchTreebank;

import java.util.List;

/**
 * a single word within the French Treebank, along with it's various attributes and position inside a phrase
 * @author Assaf Urieli
 */
public interface PhraseUnit extends PhraseElement, Entity {
    /** id of word to which this phrase unit refers */
    public int getWordId();

    /** actual word referred by this phrase unit */
    public Word getWord();

    /** position of this phrase unit within the sentence containing it, starts at zero */
    public int getPositionInSentence();

    /** Id of phrase containing this unit */
    public int getPhraseId();

    /** Actual phrase containing this unit, or null if top-level in sentence */
    public Phrase getPhrase();

    /** Id of lemma to which this phrase unit refers */
    public int getLemmaId();

    /** actual lemma referred to by this phrase unit */
    public Word getLemma();

    /** Id of category of this phrase unit */
    public int getCategoryId();

    /** Category of this phrase unit */
    public Category getCategory();

    /** Id of subcategory for this phrase unit */
    public int getSubCategoryId();

    /** Sub-category for this phrase unit */
    public SubCategory getSubCategory();

    /** Id for morphology of this phrase unit */
    public int getMorphologyId();

    /** Morphology of this phrase unit */
    public Morphology getMorphology();

    /** Is this phrase unit compound or not?
     * If false, all subunits should be considered as separate tokens.
     * If true, all subunits should be considred as part of a single compound token. */
    public boolean isCompound();

    /** subunits for this phrase unit - only if it is compound */
    public List<PhraseSubunit> getSubunits();

    /** Adds a new subunit at the end of the current list 
     * @param morphologyCode 
     * @param subCategoryCode */
    public PhraseSubunit newSubunit(String categoryCode, String subCategoryCode, String morphologyCode);
    
    /** the text of this phrase unit (got from it's word) */
    public String getText();
    
    /** this may be set prior to the word, in which case the word will be found or created when saving */
    public void setText(String text);

    public void setLemma(Word lemma);
    
	public abstract int getNextCompoundPartId();

	/** For split compounds only, indicates the next part of the split compound, after any insertions.
	 * Note that each part of the split compound is given as a separate phrase unit, with the same lemma.
	 * @return
	 */
	public abstract PhraseUnit getNextCompoundPart();

	public int getPreviousCompoundPartId();


	/**
	 * The id of the postag that was guessed for this phrase unit, useful when evaluating
	 * a test set.
	 * @return
	 */
	public abstract int getGuessedPosTagId();
	public abstract void setGuessedPosTagId(int guessedPosTagId);


}
