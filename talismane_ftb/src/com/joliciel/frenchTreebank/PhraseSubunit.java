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

/**
 * An individual member of a compound word
 * @author Assaf Urieli
 */
public interface PhraseSubunit extends Entity {
    /** id of parent phrase unit */
    public int getPhraseUnitId();

    /** Parent phrase unit */
    public PhraseUnit getPhraseUnit();

    /** id of word referred to by this subunit */
    public int getWordId();

    /** Word referred to by this subunit */
    public Word getWord();

    /** Id of category of this subunit */
    public int getCategoryId();

    /** Category of this subunit */
    public Category getCategory();
    
    /** Id of subcategory for this phrase unit */
    public int getSubCategoryId();

    /** Sub-category for this phrase unit */
    public SubCategory getSubCategory();
    public void setSubCategory(SubCategory subCategory);

    /** Id for morphology of this phrase unit */
    public int getMorphologyId();

    /** Morphology of this phrase unit */
    public Morphology getMorphology();
    public void setMorphology(Morphology morphology);

    /** the text of this phrase subunit (got from it's word) */
    public String getText();
    
    /** this may be set prior to the word, in which case the word will be found or created when saving */
    public void setText(String text);

    public int getPosition();

    public void setPosition(int position);
}
