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
import java.util.Set;

interface TreebankDao {
    /**
     * Load existing category from datastore.
     */
    public Category loadCategory(String categoryCode);

    /**
     * Save a category to the database.
     */
    public void saveCategory(CategoryInternal category);
    
    public Function loadFunction(String functionCode);
    public void saveFunction(FunctionInternal function);
    
    public Morphology loadMorphology(String morphologyCode);
    public void saveMorphology(MorphologyInternal morphology);
    
    public PhraseType loadPhraseType(String phraseTypeCode);
    public void savePhraseType(PhraseTypeInternal phraseType);
    
    public SubCategory loadSubCategory(Category category, String subCategoryCode);
    public void saveSubCategory(SubCategoryInternal subCategory);
    
    public Word loadWord(String text, String originalText);
    public Word loadWord(int wordId);
    public void saveWord(WordInternal word);
    public List<Word> findWords(Phrase phrase);
    
    /**
     * Save a sentence to the database.
     */
    public void saveSentence(SentenceInternal sentence);
    
    /**
     * Save a phrase to the database
     */
    public void savePhrase(PhraseInternal phrase);
    
    /**
     * Save a phrase unit to the database.
     * @param phraseUnit
     */
    public void savePhraseUnit(PhraseUnitInternal phraseUnit);
    
    public void savePhraseSubunit(PhraseSubunitInternal phraseSubunit);

    public TreebankServiceInternal getTreebankServiceInternal();

    public void setTreebankServiceInternal(TreebankServiceInternal treebankServiceInternal);

    public void saveTextItem(TextItemInternal textItem);

    public void saveTreebankFile(TreebankFileInternal treebankFile);

    public TreebankFile loadTreebankFile(String fileName);
    
    public TreebankFile loadTreebankFile(int fileId);

    public void savePhraseDescendantMapping(Phrase parent,
            Phrase descendant);
    
    public List<List<Entity>> findStuff(List<String> tablesToReturn, List<String>tables, List<String> conditions, List<String> orderBy);
    
    public List<Integer> findSentenceIds(int minId, int maxId);
    public List<Integer> findSentenceIds(TreebankSubSet treebankSubSet, int numIds, int startId);
    public List<Integer> findSentenceIds(TreebankSubSet treebankSubSet, int numIds);
    public List<Integer> findWordIds(int minId, int maxId);

    public List<PhraseUnit> findAllPhraseUnits(Phrase phrase);
    public List<PhraseSubunit> findPhraseSubunits(PhraseUnit phraseUnit);
    
    public Set<String> findUnknownWords(TreebankSubSet knownSet, TreebankSubSet unknownSet);
    
    public Sentence loadSentence(int sentenceId);

    public Category loadCategory(int categoryId);

    public Morphology loadMorphology(int morphologyId);

    public SubCategory loadSubCategory(int subCategoryId);
	
	public List<Integer> findCompoundWordIds(int minId, int maxId);
	
    public List<Integer> findCompoundPhraseUnitIds(int minId, int maxId);
    
    public PhraseUnit loadPhraseUnit(int phraseUnitId);
    
    /**
     * Find all words corresponding to a given text.
     */
    public List<Word> findWords(String text);
    
	public void beginTransaction();
	
	public void rollbackTransaction();
	
	public void commitTransaction();

	public abstract void findAllWordsAndLemmas(Phrase phrase, List<? extends PhraseUnit> phraseUnits);

	public abstract List<TreebankFile> findTreebankFiles();
	
	public List<Sentence> findSentences(TreebankFile treebankFile);
	public List<Integer> findSentenceIds(TreebankFile treebankFile);

	public abstract void deleteTextItem(int textItemId);

	public abstract TextItem loadTextItem(String externalId);

	public abstract TextItem loadTextItem(int textItemId);

	public List<PhraseUnitInternal> findPhraseUnits(Phrase phrase);

	public List<PhraseInternal> findChildren(Phrase phrase);

	public PhraseType loadPhraseType(int phraseTypeId);

	public Phrase loadPhrase(int phraseId);
	
	public Sentence loadFullSentence(int sentenceId);

}
