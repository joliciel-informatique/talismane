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
package com.joliciel.frenchTreebank;

import java.util.List;
import java.util.Set;

import com.joliciel.talismane.utils.ObjectCache;

class TreebankServiceImpl implements TreebankServiceInternal {
    private TreebankDao treebankDao;
    private ObjectCache objectCache;
      
    public SentenceInternal newSentenceInternal(PhraseInternal phrase) {
        SentenceImpl sentence = new SentenceImpl();
        sentence.setTreebankServiceInternal(this);
        sentence.setId(phrase.getId());
        sentence.setDepth(phrase.getDepth());
        sentence.setFunctionId(phrase.getFunctionId());
        sentence.setParentId(phrase.getParentId());
        sentence.setPhraseTypeId(phrase.getPhraseTypeId());
        sentence.setPositionInPhrase(phrase.getPositionInPhrase());
        return sentence;
    }

    public PhraseInternal newPhrase() {
        return this.newPhrase(null);
    }
    
    public PhraseInternal newPhrase(Phrase parent) {
        PhraseImpl phrase = new PhraseImpl();
        phrase.setTreebankServiceInternal(this);
        phrase.setParent(parent);
        return phrase;
    }


    public Sentence newSentence() {
        SentenceImpl sentence = new SentenceImpl();
        sentence.setTreebankServiceInternal(this);
        PhraseType sentenceType = this.loadOrCreatePhraseType("SENT");
        sentence.setPhraseType(sentenceType);
        return sentence;
    }

    public PhraseUnitInternal newPhraseUnit() {
        PhraseUnitImpl phraseUnit = new PhraseUnitImpl();
        phraseUnit.setTreebankServiceInternal(this);
        return phraseUnit;
    }
    
    public PhraseSubunitInternal newPhraseSubunit(PhraseUnit phraseUnit) {
        PhraseSubunitImpl phraseSubunit = new PhraseSubunitImpl();
        phraseSubunit.setTreebankServiceInternal(this);
        phraseSubunit.setPhraseUnit(phraseUnit);
        return phraseSubunit;
    }
    public PhraseSubunitInternal newPhraseSubunit() {
        PhraseSubunitImpl phraseSubunit = new PhraseSubunitImpl();
        phraseSubunit.setTreebankServiceInternal(this);
        return phraseSubunit;
    }

    public TreebankFileInternal newTreebankFile() {
        TreebankFileImpl treebankFile = new TreebankFileImpl();
        treebankFile.setTreebankServiceInternal(this);
        return treebankFile;
    }

    public Category loadCategory(int categoryId) {
        Category category = (Category) this.objectCache.getEntity(Category.class, categoryId);
        if (category==null) {
        	if (this.getTreebankDao()!=null)
        		category = this.getTreebankDao().loadCategory(categoryId);
            if (category==null) {
                throw new EntityNotFoundException("No Category found for id " + categoryId);
            }
            this.objectCache.putEntity(Category.class, categoryId, category);
        }
        return category;        
    }
    
	public Phrase loadPhrase(int phraseId) {
        Phrase phrase = (Phrase) this.objectCache.getEntity(Phrase.class, phraseId);
        if (phrase==null) {
            phrase = this.getTreebankDao().loadPhrase(phraseId);
            if (phrase==null) {
                throw new EntityNotFoundException("No Phrase found for id " + phraseId);
            }
            this.objectCache.putEntity(Phrase.class, phraseId, phrase);
        }
        return phrase;        
	}
	
	@Override
	public PhraseUnit loadPhraseUnit(int phraseUnitId) {
        PhraseUnit phraseUnit = (PhraseUnit) this.objectCache.getEntity(PhraseUnit.class, phraseUnitId);
        if (phraseUnit==null) {
            phraseUnit = this.getTreebankDao().loadPhraseUnit(phraseUnitId);
            if (phraseUnit==null) {
                throw new EntityNotFoundException("No PhraseUnit found for id " + phraseUnitId);
            }
            this.objectCache.putEntity(PhraseUnit.class, phraseUnitId, phraseUnit);
        }
        return phraseUnit;        
	}
    
    public Category loadCategory(String categoryCode) {
        Category category = (Category) this.objectCache.getEntity(Category.class, categoryCode);
        if (category==null) {
        	if (this.getTreebankDao()!=null)
        		category = this.getTreebankDao().loadCategory(categoryCode);
            if (category==null) {
                throw new EntityNotFoundException("No Category found for code " + categoryCode);
            }
            this.objectCache.putEntity(Category.class, categoryCode, category);
        }
        return category;
    }
    
    public Category loadOrCreateCategory(String categoryCode) {
        if (categoryCode==null || categoryCode.length()==0)
            return null;
        Category category = null;
        try {
            category = this.loadCategory(categoryCode);
        } catch (EntityNotFoundException enfe) {
            CategoryInternal newCategory = this.newCategory();
            newCategory.setCode(categoryCode);
            newCategory.save();
            category = newCategory;
            this.objectCache.putEntity(Category.class, categoryCode, category);
        }
        return category;
    }

    public Function loadFunction(String functionCode) {
       Function function = (Function) this.objectCache.getEntity(Function.class, functionCode);
        if (function==null) {
        	if (this.getTreebankDao()!=null)
        		function = this.getTreebankDao().loadFunction(functionCode);
            if (function==null) {
                throw new EntityNotFoundException("No Function found for code " + functionCode);
            }
            this.objectCache.putEntity(Function.class, functionCode, function);
        }
        return function;
    }
    
    public Function loadOrCreateFunction(String functionCode) {
        if (functionCode==null || functionCode.length()==0)
            return null;
        Function function = null;
        try {
            function = this.loadFunction(functionCode);
        } catch (EntityNotFoundException enfe) {
            FunctionInternal newFunction = this.newFunction();
            newFunction.setCode(functionCode);
            newFunction.save();
            function = newFunction;
            this.objectCache.putEntity(Function.class, functionCode, function);
        }
        return function;
    }

    public Morphology loadMorphology(int morphologyId) {
        Morphology morphology = (Morphology) this.objectCache.getEntity(Morphology.class, morphologyId);
        if (morphology==null) {
        	if (this.getTreebankDao()!=null)
        		morphology = this.getTreebankDao().loadMorphology(morphologyId);
            if (morphology==null) {
                throw new EntityNotFoundException("No Morphology found for id " + morphologyId);
            }
            this.objectCache.putEntity(Morphology.class, morphologyId, morphology);
        }
        return morphology;        
    }

    public Morphology loadMorphology(String morphologyCode) {
        Morphology morphology = (Morphology) this.objectCache.getEntity(Morphology.class, morphologyCode);
        if (morphology==null) {
        	if (this.getTreebankDao()!=null)
        		morphology = this.getTreebankDao().loadMorphology(morphologyCode);
            if (morphology==null) {
                throw new EntityNotFoundException("No Morphology found for code " + morphologyCode);
            }
            this.objectCache.putEntity(Morphology.class, morphologyCode, morphology);
        }
        return morphology;
    }
    
    public Morphology loadOrCreateMorphology(String morphologyCode) {
        if (morphologyCode==null || morphologyCode.length()==0)
            return null;
        Morphology morphology = null;
        try {
            morphology = this.loadMorphology(morphologyCode);
        } catch (EntityNotFoundException enfe) {
            MorphologyInternal newMorphology = this.newMorphology();
            newMorphology.setCode(morphologyCode);
            newMorphology.save();
            morphology = newMorphology;
            this.objectCache.putEntity(Morphology.class, morphologyCode, morphology);
        }
         return morphology;
    }

    @Override
	public PhraseType loadPhraseType(int phraseTypeId) {
        PhraseType phraseType = (PhraseType) this.objectCache.getEntity(PhraseType.class, phraseTypeId);
        if (phraseType==null) {
            phraseType = this.getTreebankDao().loadPhraseType(phraseTypeId);
            if (phraseType==null) {
                throw new EntityNotFoundException("No PhraseType found for id " + phraseTypeId);
            }
            this.objectCache.putEntity(PhraseType.class, phraseTypeId, phraseType);
        }
        return phraseType;        
    }
    

    public PhraseType loadPhraseType(String phraseTypeCode) {
        PhraseType phraseType = (PhraseType) this.objectCache.getEntity(PhraseType.class, phraseTypeCode);
        if (phraseType==null) {

        	if (this.getTreebankDao()!=null)
        		phraseType = this.getTreebankDao().loadPhraseType(phraseTypeCode);

            if (phraseType==null) {
                throw new EntityNotFoundException("No PhraseType found for code " + phraseTypeCode);
            }
            this.objectCache.putEntity(PhraseType.class, phraseTypeCode, phraseType);
        }
        return phraseType;
    }
    
    public PhraseType loadOrCreatePhraseType(String phraseTypeCode) {
        if (phraseTypeCode==null || phraseTypeCode.length()==0)
            return null;
        PhraseType phraseType = null;
        try {
            phraseType = this.loadPhraseType(phraseTypeCode);
        } catch (EntityNotFoundException enfe) {
            PhraseTypeInternal newPhraseType = this.newPhraseType();
            newPhraseType.setCode(phraseTypeCode);
            newPhraseType.save();
            phraseType = newPhraseType;
            this.objectCache.putEntity(PhraseType.class, phraseTypeCode, phraseType);
        }
        return phraseType;
    }

    public SubCategory loadSubCategory(int subCategoryId) {
         SubCategory subCategory = (SubCategory) this.objectCache.getEntity(SubCategory.class, subCategoryId);
        if (subCategory==null) {
        	if (this.getTreebankDao()!=null)
        		subCategory = this.getTreebankDao().loadSubCategory(subCategoryId);
            if (subCategory==null) {
                throw new EntityNotFoundException("No SubCategory found for id " + subCategoryId);
            }
            this.objectCache.putEntity(SubCategory.class, subCategoryId, subCategory);
        }
        return subCategory;        
    }


    public SubCategory loadSubCategory(Category category,
            String subCategoryCode) {
        String key = category.getCode() + "|" + subCategoryCode;
        SubCategory subCategory = (SubCategory) this.objectCache.getEntity(SubCategory.class, key);
        if (subCategory==null) {
        	if (this.getTreebankDao()!=null)
        		subCategory = this.getTreebankDao().loadSubCategory(category, subCategoryCode);
            if (subCategory==null) {
                throw new EntityNotFoundException("No SubCategory found for category " + category.getCode() + ", code " + subCategoryCode);
            }
            this.objectCache.putEntity(SubCategory.class, key, subCategory);
        }
        return subCategory;
    }
    
    public SubCategory loadOrCreateSubCategory(Category category,
            String subCategoryCode) {
        if (subCategoryCode==null || subCategoryCode.length()==0)
            return null;
        SubCategory subCategory = null;
        try {
            subCategory = this.loadSubCategory(category, subCategoryCode);
        } catch (EntityNotFoundException enfe) {
            String key = category.getCode() + "|" + subCategoryCode;
            SubCategoryInternal newSubCategory = this.newSubCategory();
            newSubCategory.setCode(subCategoryCode);
            newSubCategory.setCategoryId(category.getId());
            newSubCategory.setCategory(category);
            newSubCategory.save();
            subCategory = newSubCategory;
            this.objectCache.putEntity(SubCategory.class, key, subCategory);
        }
        return subCategory;
    }

    public Word loadWord(int wordId) {
        Word word = this.getTreebankDao().loadWord(wordId);
        if (word==null) {
            throw new EntityNotFoundException("No Word found for id " + wordId);
        }
        return word;   
    }
    
    public Word loadWord(String text, String originalText) {
        // not caching words as that would take up too much memory!
    	Word word = null;
    	if (this.getTreebankDao()!=null)
    		word = this.getTreebankDao().loadWord(text, originalText);
        if (word==null) {
            throw new EntityNotFoundException("No Word found for text " + text + "," + originalText);
        }
        return word;
    }
    
    public Word loadOrCreateWord(String text, String originalText) {
        // not caching words as that would take up too much memory!
        // also, allowing the null word
        if (text == null)
            text = "";
        if (originalText == null)
        	originalText = "";
        Word word = null;
        try {
            word = this.loadWord(text, originalText);
        } catch (EntityNotFoundException enfe) {
            WordInternal newWord = this.newWord();
            newWord.setText(text);
            newWord.setOriginalText(originalText);
            newWord.save();
            word = newWord;
        }
        return word;
    }

    public TreebankFile loadTreebankFile(int fileId) {
        TreebankFile file = (TreebankFile) this.objectCache.getEntity(TreebankFile.class, fileId);
        if (file==null) {
            file = this.getTreebankDao().loadTreebankFile(fileId);
            if (file==null) {
                throw new EntityNotFoundException("No TreebankFile found for fileId " + fileId);
            }
            this.objectCache.putEntity(TreebankFile.class, fileId, file);
        }
        return file;
    }
    
    public TreebankFile loadTreebankFile(String fileName) {
        String key = fileName;
        TreebankFile file = (TreebankFile) this.objectCache.getEntity(TreebankFile.class, key);
        if (file==null) {
            file = this.getTreebankDao().loadTreebankFile(fileName);
            if (file==null) {
                throw new EntityNotFoundException("No TreebankFile found for fileName " + fileName);
            }
            this.objectCache.putEntity(TreebankFile.class, key, file);
        }
        return file;
    }
    
    public TreebankFile loadOrCreateTreebankFile(String fileName) {
        if (fileName==null || fileName.length()==0)
            return null;
        TreebankFile file = null;
        try {
            file = this.loadTreebankFile(fileName);
        } catch (EntityNotFoundException enfe) {
            TreebankFileInternal newTreebankFile = this.newTreebankFile();
            newTreebankFile.setFileName(fileName);
            newTreebankFile.save();
            file = newTreebankFile;
            this.objectCache.putEntity(TreebankFile.class, fileName, file);
        }
        return file;
    }

	@Override
	public TreebankFile newTreebankFile(String fileName) {
		TreebankFileInternal treebankFile = this.newTreebankFile();
		treebankFile.setFileName(fileName);
        return treebankFile;
	}
    
    public TextItem loadTextItem(String externalId) {
        TextItem textItem = (TextItem) this.objectCache.getEntity(TextItem.class, externalId);
        if (textItem==null) {
            textItem = this.getTreebankDao().loadTextItem(externalId);
            if (textItem==null) {
                throw new EntityNotFoundException("No TextItem found for external id " + externalId);
            }
            this.objectCache.putEntity(TextItem.class, externalId, textItem);
        }
        return textItem;
    }
    
    public TextItem loadTextItem(int textItemId) {
         TextItem textItem = (TextItem) this.objectCache.getEntity(TextItem.class, textItemId);
        if (textItem==null) {
            textItem = this.getTreebankDao().loadTextItem(textItemId);
            if (textItem==null) {
                throw new EntityNotFoundException("No TextItem found for id " + textItemId);
            }
            this.objectCache.putEntity(TextItem.class, textItemId, textItem);
        }
        return textItem;        
    }
    
    public TextItemInternal newTextItem() {
        TextItemImpl textItem = new TextItemImpl();
        textItem.setTreebankServiceInternal(this);
        return textItem;
    }

    public void savePhraseInternal(PhraseInternal phrase) {
        this.getTreebankDao().savePhrase(phrase);
    }

    public void saveSentenceInternal(SentenceInternal sentence) {
        this.getTreebankDao().saveSentence(sentence);
    }

    public void savePhraseUnitInternal(PhraseUnitInternal phraseUnit) {
        this.getTreebankDao().savePhraseUnit(phraseUnit);
    }

    public void savePhraseSubunitInternal(PhraseSubunitInternal phraseSubunit) {
        this.getTreebankDao().savePhraseSubunit(phraseSubunit);
    }

    public void savePhraseDescendantMapping(Phrase parent,
            Phrase descendant) {
        this.getTreebankDao().savePhraseDescendantMapping(parent,
                descendant);
    }

    public TreebankDao getTreebankDao() {
        return treebankDao;
    }

    public void setTreebankDao(TreebankDao treebankDao) {
        this.treebankDao = treebankDao;
        if (treebankDao!=null)
        	treebankDao.setTreebankServiceInternal(this);
    }

    public ObjectCache getObjectCache() {
        return objectCache;
    }

    public void setObjectCache(ObjectCache objectCache) {
        this.objectCache = objectCache;
    }

    public CategoryInternal newCategory() {
        CategoryImpl category = new CategoryImpl();
        category.setTreebankServiceInternal(this);
        return category;
    }

    public FunctionInternal newFunction() {
        FunctionImpl function = new FunctionImpl();
        function.setTreebankServiceInternal(this);
        return function;
    }

    public MorphologyInternal newMorphology() {
        MorphologyImpl morphology = new MorphologyImpl();
        morphology.setTreebankServiceInternal(this);
        return morphology;
    }

    public PhraseTypeInternal newPhraseType() {
        PhraseTypeImpl phraseType = new PhraseTypeImpl();
        phraseType.setTreebankServiceInternal(this);
        return phraseType;
    }

    public SubCategoryInternal newSubCategory() {
        SubCategoryImpl subCategory = new SubCategoryImpl();
        subCategory.setTreebankServiceInternal(this);
        return subCategory;

    }

    public WordInternal newWord() {
        WordImpl word = new WordImpl();
        word.setTreebankServiceInternal(this);
        return word;

    }

    public void saveCategoryInternal(CategoryInternal category) {
    	if (this.getTreebankDao()!=null)
    		this.getTreebankDao().saveCategory(category);
    }

    public void saveFunctionInternal(FunctionInternal function) {
    	if (this.getTreebankDao()!=null)
    		this.getTreebankDao().saveFunction(function);
    }

    public void saveMorphologyInternal(MorphologyInternal morphology) {
    	if (this.getTreebankDao()!=null)
    		this.getTreebankDao().saveMorphology(morphology);
    }

    public void savePhraseTypeInternal(PhraseTypeInternal phraseType) {
    	if (this.getTreebankDao()!=null)
        	this.getTreebankDao().savePhraseType(phraseType);
    }

    public void saveSubCategoryInternal(SubCategoryInternal subCategory) {
    	if (this.getTreebankDao()!=null)
    		this.getTreebankDao().saveSubCategory(subCategory);
    }

    public void saveTextItemInternal(TextItemInternal textItem) {
        this.getTreebankDao().saveTextItem(textItem);
    }

    public void saveTreebankFileInternal(TreebankFileInternal treebankFile) {
        this.getTreebankDao().saveTreebankFile(treebankFile);
    }

    public void saveWordInternal(WordInternal word) {
    	if (this.getTreebankDao()!=null)
    		this.getTreebankDao().saveWord(word);
    }

    public List<List<Entity>> findStuff(List<String> tablesToReturn, List<String>tables, List<String> conditions, List<String> orderBy) {
        return this.getTreebankDao().findStuff(tablesToReturn, tables, conditions, orderBy);
    }

    public List<Word> findWords(Phrase phrase) {
        return this.getTreebankDao().findWords(phrase);
    }

    public Sentence loadSentence(int sentenceId) {
        return this.getTreebankDao().loadSentence(sentenceId);
    }
    
    public List<Integer> findSentenceIds(int minId, int maxId) {
        return this.getTreebankDao().findSentenceIds(minId, maxId);
    }
    public List<Integer> findWordIds(int minId, int maxId) {
        return this.getTreebankDao().findWordIds(minId, maxId);
    }    
    public List<PhraseUnit> findAllPhraseUnits(Phrase phrase) {
    	List<PhraseUnit> phraseUnits = this.getTreebankDao().findAllPhraseUnits(phrase);
    	this.treebankDao.findAllWordsAndLemmas(phrase, phraseUnits);
    	return phraseUnits;
    }
    
    public List<PhraseUnitInternal> findPhraseUnits(Phrase phrase) {
    	List<PhraseUnitInternal> phraseUnits = this.getTreebankDao().findPhraseUnits(phrase);
    	this.treebankDao.findAllWordsAndLemmas(phrase, phraseUnits);
    	return phraseUnits;
    }
    
	@Override
	public List<PhraseInternal> findChildren(Phrase phrase) {
		List<PhraseInternal> children = this.getTreebankDao().findChildren(phrase);
		return children;
	}

	@Override
	public List<Integer> findSentenceIds(TreebankSubSet treebankSubSet, int numIds) {
		return this.getTreebankDao().findSentenceIds(treebankSubSet, numIds);
	}
	@Override
	public List<Integer> findSentenceIds(TreebankSubSet treebankSubSet, int numIds, int startId) {
		return this.getTreebankDao().findSentenceIds(treebankSubSet, numIds, startId);
	}
	
	public List<Integer> findCompoundWordIds(int minId, int maxId) {
		return this.getTreebankDao().findCompoundWordIds(minId, maxId);
	}

	@Override
	public List<PhraseSubunit> findPhraseSubunits(PhraseUnit phraseUnit) {
		return this.treebankDao.findPhraseSubunits(phraseUnit);
	}

	@Override
	public List<Integer> findCompoundPhraseUnitIds(int minId, int maxId) {
		return this.treebankDao.findCompoundPhraseUnitIds(minId, maxId);
	}

	@Override
	public List<Word> findWords(String text) {
		return this.treebankDao.findWords(text);
	}

	public void beginTransaction() {
		this.treebankDao.beginTransaction();
	}
	
	public void rollbackTransaction() {
		this.treebankDao.rollbackTransaction();
	}
	
	public void commitTransaction() {
		this.treebankDao.commitTransaction();
	}

	@Override
	public PartOfSpeech getPartOfSpeech(int categoryId, int subcategoryId,
			int morphologyId) {
		Category category = this.loadCategory(categoryId);
		SubCategory subCategory = this.loadSubCategory(subcategoryId);
		Morphology morphology = this.loadMorphology(morphologyId);
		PartOfSpeechImpl partOfSpeech = new PartOfSpeechImpl();
		partOfSpeech.setCategory(category);
		partOfSpeech.setSubCategory(subCategory);
		partOfSpeech.setMorphology(morphology);
		
		return partOfSpeech;
	}

	@Override
	public Set<String> findUnknownWords(TreebankSubSet knownSet, TreebankSubSet unknownSet) {
		return this.treebankDao.findUnknownWords(knownSet, unknownSet);
	}

	@Override
	public List<TreebankFile> findTreebankFiles() {
		return this.treebankDao.findTreebankFiles();
	}
	
	public List<Sentence> findSentences(TreebankFile treebankFile) {
		return this.treebankDao.findSentences(treebankFile);
	}
	
	public Sentence loadFullSentence(int sentenceId) {
		Sentence sentence = this.treebankDao.loadFullSentence(sentenceId);
		
		return sentence;
	}
	
	public List<Integer> findSentenceIds(TreebankFile treebankFile) {
		return this.treebankDao.findSentenceIds(treebankFile);
	}

	@Override
	public TreebankReader getDatabaseReader(TreebankSubSet treebankSubSet, int startSentence) {
		TreebankDatabaseReader treebankDatabaseReader = new TreebankDatabaseReader(treebankSubSet);
		treebankDatabaseReader.setTreebankService(this);
		treebankDatabaseReader.setStartSentence(startSentence);
		return treebankDatabaseReader;
	}

}
