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
package com.joliciel.talismane.fr.ftb;

import java.util.List;

import com.joliciel.talismane.utils.ObjectCache;

interface TreebankServiceInternal extends TreebankService {
    public SentenceInternal newSentenceInternal(PhraseInternal phrase);
    
    /**
     * Get an empty Phrase for saving.
     */
    public PhraseInternal newPhrase();
    
    /**
     * Get an empty Phrase as child of a given phrase for saving.
     */
    public PhraseInternal newPhrase(Phrase parent);
    
    /**
     * Get an empty phrase unit for saving.
     */
    public PhraseUnitInternal newPhraseUnit();
    
    public PhraseSubunitInternal newPhraseSubunit(PhraseUnit phraseUnit);
    public PhraseSubunitInternal newPhraseSubunit();
    
    public TreebankFileInternal newTreebankFile();
    
    public CategoryInternal newCategory();
    public SubCategoryInternal newSubCategory();
    public FunctionInternal newFunction();
    public MorphologyInternal newMorphology();
    public PhraseTypeInternal newPhraseType();
    public WordInternal newWord();
    
    /**
     * Load existing category or create a new one if non-existent.
     */
    public Category loadOrCreateCategory(String categoryCode);
    
    /**
     * Load existing sub-category or create a new one if non-existent for this category.
     */
    public SubCategory loadOrCreateSubCategory(Category category, String subCategoryCode);
    
    /**
     * Load existing morphology or create a new one if non-existent.
     */
    public Morphology loadOrCreateMorphology(String morphologyCode);
    
    /**
     * Load existing function or create a new one if non-existent.
     */
    public Function loadOrCreateFunction(String functionCode);
    
    /**
     * Load existing phrase type or create a new one if non-existent.
     */
    public PhraseType loadOrCreatePhraseType(String phraseTypeCode);
    
    /**
     * Load existing word or create a new one if non-existent.
     * @param originalText 
     */
    public Word loadOrCreateWord(String text, String originalText);
    
    /**
     * Save a sentence to the database.
     */
    public void saveSentenceInternal(SentenceInternal sentence);
    
    /**
     * Save a phrase to the database
     */
    public void savePhraseInternal(PhraseInternal phrase);
    
    /**
     * Save a phrase unit to the database.
     */
    public void savePhraseUnitInternal(PhraseUnitInternal phraseUnit);
    
    /**
     * Save a phrase subunit to the database.
     */
    public void savePhraseSubunitInternal(PhraseSubunitInternal phraseSubunit);

    public void saveCategoryInternal(CategoryInternal category);

    public void saveFunctionInternal(FunctionInternal function);

    public void saveMorphologyInternal(MorphologyInternal morphology);

    public void savePhraseTypeInternal(PhraseTypeInternal phraseType);

    public void saveSubCategoryInternal(SubCategoryInternal subCategory);

    public void saveTextItemInternal(TextItemInternal textItem);

    public void saveTreebankFileInternal(TreebankFileInternal treebankFile);

    public void saveWordInternal(WordInternal word);

    public void savePhraseDescendantMapping(Phrase parent,
            Phrase descendant);

    public Word loadWord(int wordId);

    public List<Word> findWords(Phrase phrase);
    
    public TreebankFile loadTreebankFile(int fileId);

    public List<PhraseUnit> findAllPhraseUnits(Phrase phrase);

	public List<PhraseSubunit> findPhraseSubunits(
			PhraseUnit phraseUnit);
	
	public void beginTransaction();
	
	public void rollbackTransaction();
	
	public void commitTransaction();

	public TextItem loadTextItem(int textItemId);

	public List<PhraseUnitInternal> findPhraseUnits(Phrase phrase);

	public List<PhraseInternal> findChildren(Phrase phrase);

	public abstract PhraseType loadPhraseType(int phraseTypeId);
	public Phrase loadPhrase(int phraseId);
	public List<Integer> findSentenceIds(TreebankFile treebankFile);
	public ObjectCache getObjectCache();
}
