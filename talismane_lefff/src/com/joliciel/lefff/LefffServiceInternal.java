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
package com.joliciel.lefff;

import java.util.List;

interface LefffServiceInternal extends LefffService {
    
    public AttributeInternal newAttribute();
    public CategoryInternal newCategory();
    public LemmaInternal newLemma();
    public PredicateInternal newPredicate();
    public WordInternal newWord();
    
    /**
     * Load existing attribute or create a new one if non-existent.
     */
    public Attribute loadOrCreateAttribute(String attributeCode, String attributeValue);
	public Attribute loadOrCreateAttribute(String attributeCode, String attributeValue,
			boolean morphological);
    /**
     * Load existing category or create a new one if non-existent.
     */
    public Category loadOrCreateCategory(String categoryCode);

    /**
     * Load existing predicate or create a new one if non-existent.
     */
    public Predicate loadOrCreatePredicate(String predicateText);
    
    /**
     * Load existing word or create a new one if non-existent.
     */
    public Word loadOrCreateWord(String text);
    
    /**
     * Load existing lemma or create a new one if non-existent.
     */
    public Lemma loadOrCreateLemma(String text, int index, String complement);
    
    /**
     * Save an entry to the database.
     */
    public void saveEntry(LefffEntryInternal entry);

    public void saveAttribute(AttributeInternal attribute);

    public void saveLemma(LemmaInternal lemma);

    public void savePredicate(PredicateInternal predicate);
    
    public void saveAttributes(LefffEntryInternal entry);

    public void saveWord(WordInternal word);
    
	public void saveCategory(CategoryInternal category);

    public List<Attribute> findAttributes(LefffEntryInternal entry);
	public LefffEntryInternal newEntryInternal();

	
	public LefffEntryInternal loadOrCreateEntry(Word word, Lemma lemma, Category category);

}
