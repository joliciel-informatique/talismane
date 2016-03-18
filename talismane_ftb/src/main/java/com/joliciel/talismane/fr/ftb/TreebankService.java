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
import java.util.Set;

public interface TreebankService {
	public TreebankReader getDatabaseReader(TreebankSubSet treebankSubSet, int startSentence);
	
    /**
     * Get an empty sentence for saving.
     */
    public Sentence newSentence();
    
    /**
     * Get an empty sentence for saving.
     */
    public TreebankFile newTreebankFile(String fileName);

    
    /**
     * Load an existing treebank file or create a new one
     */
    public TreebankFile loadOrCreateTreebankFile(String fileName);

    /**
     * Load an existing treebank file.
     */
    public TreebankFile loadTreebankFile(String fileName);
    
    /**
     * Load an existing text item.
     */
    public TextItem loadTextItem(String externalId);
    public TextItem newTextItem();

    /**
     * Load existing category or throw EntityNotFoundException if non-existent.
     */
    public Category loadCategory(String categoryCode);
    
    /**
     * Load existing sub-category or throw EntityNotFoundException if non-existent.
     */
    public SubCategory loadSubCategory(Category category, String subCategoryCode);
    
    /**
     * Load existing morphology  or throw EntityNotFoundException if non-existent.
     */
    public Morphology loadMorphology(String morphologyCode);
    
    /**
     * Load existing function  or throw EntityNotFoundException if non-existent.
     */
    public Function loadFunction(String functionCode);
    
    /**
     * Load existing phrase type or throw EntityNotFoundException if non-existent.
     */
    public PhraseType loadPhraseType(String phraseTypeCode);
    
    /**
     * Load existing word or throw EntityNotFoundException if non-existent.
     */
    public Word loadWord(String text, String originalText);
    
    /**
     * Find all words corresponding to a given text.
     */
    public List<Word> findWords(String text);
    
    
    
    public List<List<Entity>> findStuff(List<String> tablesToReturn, List<String>tables, List<String> conditions, List<String> orderBy);
    
    public Sentence loadSentence(int sentenceId);
    
    /**
     * Loads a sentence, including all of its phrases and phrase units.
     */
    public Sentence loadFullSentence(int sentenceId);
    
    public List<Integer> findSentenceIds(int minId, int maxId);
    
    /**
     * @param treebankSubSet the subset to take from
     * @param numIds if not zero, limit to this number of ids.
     */
    public List<Integer> findSentenceIds(TreebankSubSet treebankSubSet, int numIds);
    public List<Integer> findSentenceIds(TreebankSubSet treebankSubSet, int numIds, int startId);
    
    public List<Integer> findWordIds(int minId, int maxId);
    public List<Integer> findCompoundWordIds(int minId, int maxId);
    public List<Integer> findCompoundPhraseUnitIds(int minId, int maxId);
    
    /**
     * Find unknown words in a given portion of the corpus, compared to the training part of the corpus.
     * @param knownSet where to find known words
     * @param unknownSet where to find unknown words
     */
    public Set<String> findUnknownWords(TreebankSubSet knownSet, TreebankSubSet unknownSet);
    
    public PhraseUnit loadPhraseUnit(int phraseUnitId);

    public Category loadCategory(int categoryId);

    public SubCategory loadSubCategory(int subCategoryId);

    public Morphology loadMorphology(int morphologyId);
 	
	public PartOfSpeech getPartOfSpeech(int categoryId, int subcategoryId, int morphologyId);
	
	/**
	 * Find all treebank files in the database.
	 */
	public abstract List<TreebankFile> findTreebankFiles();
	
	public List<Sentence> findSentences(TreebankFile treebankFile);
}
