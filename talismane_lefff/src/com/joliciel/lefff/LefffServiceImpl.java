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
package com.joliciel.lefff;

import java.io.File;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.lexicon.LexicalEntry;
import com.joliciel.talismane.lexicon.LexicalEntryMorphologyReader;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.utils.ObjectCache;

class LefffServiceImpl implements LefffServiceInternal {
    @SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(LefffServiceImpl.class);
    private LefffDao lefffDao;
    private ObjectCache objectCache;
    private LefffLoaderImpl lefffLoader;

    public Attribute loadAttribute(int attributeId) {
        Attribute attribute = (Attribute) this.objectCache.getEntity(Attribute.class, attributeId);
        if (attribute==null) {
            attribute = this.getLefffDao().loadAttribute(attributeId);
            if (attribute==null) {
                throw new EntityNotFoundException("No Attribute found for id " + attributeId);
            }
            this.objectCache.putEntity(Attribute.class, attributeId, attribute);
        }
        return attribute;        
    }
    
    public Attribute loadAttribute(String attributeCode, String attributeValue) {
    	String key = attributeCode + "|" + attributeValue;
        Attribute attribute = (Attribute) this.objectCache.getEntity(Attribute.class, key);
        if (attribute==null) {
            attribute = this.getLefffDao().loadAttribute(attributeCode, attributeValue);
            if (attribute==null) {
                throw new EntityNotFoundException("No Attribute found for code " + attributeCode + ", value " + attributeValue);
            }
            this.objectCache.putEntity(Attribute.class, key, attribute);
        }
        return attribute;
    }
    
    public Attribute loadOrCreateAttribute(String attributeCode, String attributeValue) {
        if (attributeCode==null || attributeCode.length()==0)
            return null;
        Attribute attribute = null;
        String key = attributeCode + "|" + attributeValue;
        try {
            attribute = this.loadAttribute(attributeCode, attributeValue);
        } catch (EntityNotFoundException enfe) {
            AttributeInternal newAttribute = this.newAttribute();
            newAttribute.setCode(attributeCode);
            newAttribute.setValue(attributeValue);
            newAttribute.save();
            attribute = newAttribute;
            this.objectCache.putEntity(Attribute.class, key, attribute);
        }
        return attribute;
    }
    
    public Category loadCategory(int categoryId) {
        Category category = (Category) this.objectCache.getEntity(Category.class, categoryId);
        if (category==null) {
            category = this.getLefffDao().loadCategory(categoryId);
            if (category==null) {
                throw new EntityNotFoundException("No Category found for id " + categoryId);
            }
            this.objectCache.putEntity(Category.class, categoryId, category);
        }
        return category;        
    }
    
    public Category loadCategory(String categoryCode) {
        Category category = (Category) this.objectCache.getEntity(Category.class, categoryCode);
        if (category==null) {
            category = this.getLefffDao().loadCategory(categoryCode);
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
    
    public Predicate loadPredicate(int predicateId) {
        Predicate predicate = (Predicate) this.objectCache.getEntity(Predicate.class, predicateId);
        if (predicate==null) {
            predicate = this.getLefffDao().loadPredicate(predicateId);
            if (predicate==null) {
                throw new EntityNotFoundException("No Predicate found for id " + predicateId);
            }
            this.objectCache.putEntity(Predicate.class, predicateId, predicate);
        }
        return predicate;        
    }
    
    public Predicate loadPredicate(String text) {
        Predicate predicate = (Predicate) this.objectCache.getEntity(Predicate.class, text);
        if (predicate==null) {
            predicate = this.getLefffDao().loadPredicate(text);
            if (predicate==null) {
                throw new EntityNotFoundException("No Predicate found for code " + text);
            }
            this.objectCache.putEntity(Predicate.class, text, predicate);
        }
        return predicate;
    }
    
    public Predicate loadOrCreatePredicate(String text) {
        if (text==null || text.length()==0)
            return null;
        Predicate predicate = null;
        try {
            predicate = this.loadPredicate(text);
        } catch (EntityNotFoundException enfe) {
            PredicateInternal newPredicate = this.newPredicate();
            newPredicate.setText(text);
            newPredicate.save();
            predicate = newPredicate;
            this.objectCache.putEntity(Predicate.class, text, predicate);
        }
        return predicate;
    }

    public Lemma loadLemma(int lemmaId) {
         Lemma lemma = (Lemma) this.objectCache.getEntity(Lemma.class, lemmaId);
        if (lemma==null) {
            lemma = this.getLefffDao().loadLemma(lemmaId);
            if (lemma==null) {
                throw new EntityNotFoundException("No Lemma found for id " + lemmaId);
            }
            this.objectCache.putEntity(Lemma.class, lemmaId, lemma);
        }
        return lemma;        
    }


    public Lemma loadLemma(String text, int index,
            String complement) {
        String key = text + "|" + index + "|" + complement;
        Lemma lemma = (Lemma) this.objectCache.getEntity(Lemma.class, key);
        if (lemma==null) {
            lemma = this.getLefffDao().loadLemma(text, index, complement);
            if (lemma==null) {
                throw new EntityNotFoundException("No Lemma found for text " + text + ", index " + index + ", complement " + complement);
            }
            this.objectCache.putEntity(Lemma.class, key, lemma);
        }
        return lemma;
    }
    
    public Lemma loadOrCreateLemma(String text, int index,
            String complement) {
        if (text==null)
            return null;
        Lemma lemma = null;
        try {
            lemma = this.loadLemma(text, index, complement);
        } catch (EntityNotFoundException enfe) {
            String key = text + "|" + index + "|" + complement;
            LemmaInternal newLemma = this.newLemma();
            newLemma.setText(text);
            newLemma.setIndex(index);
            newLemma.setComplement(complement);
            newLemma.save();
            lemma = newLemma;
            this.objectCache.putEntity(Lemma.class, key, lemma);
        }
        return lemma;
    }

    public Word loadWord(int wordId) {
        Word word = this.getLefffDao().loadWord(wordId);
        if (word==null) {
            throw new EntityNotFoundException("No Word found for id " + wordId);
        }
        return word;   
    }
    
    public Word loadWord(String text) {
        // not caching words as that would take up too much memory!
        Word word = this.getLefffDao().loadWord(text);
        if (word==null) {
            throw new EntityNotFoundException("No Word found for text " + text);
        }
        return word;
    }
    
    public Word loadOrCreateWord(String text) {
        // not caching words as that would take up too much memory!
        // also, allowing the null word
        if (text == null)
            text = "";
        Word word = null;
        try {
            word = this.loadWord(text);
        } catch (EntityNotFoundException enfe) {
            WordInternal newWord = this.newWord();
            newWord.setText(text);
            newWord.save();
            word = newWord;
        }
        return word;
    }

    public LefffDao getLefffDao() {
        return lefffDao;
    }

    public void setLefffDao(LefffDao treebankDao) {
        this.lefffDao = treebankDao;
        this.lefffDao.setLefffServiceInternal(this);
    }

    public ObjectCache getObjectCache() {
        return objectCache;
    }

    public void setObjectCache(ObjectCache objectCache) {
        this.objectCache = objectCache;
    }
    
    public WordInternal newWord() {
        WordImpl word = new WordImpl();
        word.setLefffServiceInternal(this);
        return word;

    }


    public void saveWord(WordInternal word) {
        this.getLefffDao().saveWord(word);
    }

	@Override
	public List<Attribute> findAttributes(LefffEntryInternal entry) {
		return this.getLefffDao().findAttributes(entry);
	}

	@Override
	public AttributeInternal newAttribute() {
		AttributeImpl attribute = new AttributeImpl();
		attribute.setLefffServiceInternal(this);
		return attribute;
	}
	
	public CategoryInternal newCategory() {
		CategoryImpl category = new CategoryImpl();
		category.setLefffServiceInternal(this);
		return category;
	}

	@Override
	public LemmaInternal newLemma() {
		LemmaImpl lemma = new LemmaImpl();
		lemma.setLefffServiceInternal(this);
		return lemma;
	}

	@Override
	public PredicateInternal newPredicate() {
		PredicateImpl predicate = new PredicateImpl();
		predicate.setLefffServiceInternal(this);
		return predicate;
	}

	@Override
	public void saveAttribute(AttributeInternal attribute) {
		this.getLefffDao().saveAttribute(attribute);
	}

	@Override
	public void saveAttributes(LefffEntryInternal entry) {
		this.getLefffDao().saveAttributes(entry);
	}

	@Override
	public void saveEntry(LefffEntryInternal entry) {
		this.getLefffDao().saveEntry(entry);
	}

	@Override
	public void saveLemma(LemmaInternal lemma) {
		this.getLefffDao().saveLemma(lemma);
	}

	@Override
	public void savePredicate(PredicateInternal predicate) {
		this.getLefffDao().savePredicate(predicate);
	}

	@Override
	public LefffEntry loadEntry(int entryId) {
		LefffEntry entry = (LefffEntry) this.objectCache.getEntity(LefffEntry.class, entryId);
        if (entry==null) {
            entry = this.getLefffDao().loadEntry(entryId);
            if (entry==null) {
                throw new EntityNotFoundException("No Entry found for id " + entryId);
            }
            this.objectCache.putEntity(LefffEntry.class, entryId, entry);
        }
        return entry;        
	}

	public LefffEntry newEntry() {
		return this.newEntryInternal();
	}

	@Override
	public LefffEntryInternal newEntryInternal() {
		LefffEntryInternal entry = new LefffEntryImpl();
		entry.setLefffServiceInternal(this);
		return entry;
	}

	public LefffLoader getLefffLoader() {
		if (lefffLoader==null) {
			lefffLoader = new LefffLoaderImpl();
			lefffLoader.setLefffServiceInternal(this);
		}
		return lefffLoader;
	}

	@Override
	public void saveCategory(CategoryInternal category) {
		this.getLefffDao().saveCategory(category);
	}


	@Override
	public Map<String, List<LexicalEntry>> findEntryMap() {
		return this.lefffDao.findEntryMap(null);
	}
	

	@Override
	public Map<String, List<LexicalEntry>> findEntryMap(List<String> categories) {
		return this.lefffDao.findEntryMap(categories);
	}



	@Override
	public LefffPosTagMapper getPosTagMapper(File file, PosTagSet posTagSet) {
		LefffPosTagMapperImpl posTagMapper = new LefffPosTagMapperImpl(file, posTagSet);
		return posTagMapper;
	}

	@Override
	public LefffPosTagMapper getPosTagMapper(List<String> descriptors,
			PosTagSet posTagSet) {
		LefffPosTagMapperImpl posTagMapper = new LefffPosTagMapperImpl(descriptors, posTagSet);
		return posTagMapper;
	}

	@Override
	public LexicalEntryMorphologyReader getLexicalEntryMorphologyReader() {
		LefffEntryMorphologyReader reader = new LefffEntryMorphologyReader();
		return reader;
	}
    
    
}
