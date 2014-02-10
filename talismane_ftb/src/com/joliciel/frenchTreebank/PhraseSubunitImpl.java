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

class PhraseSubunitImpl extends EntityImpl implements PhraseSubunitInternal {
    /**
	 * 
	 */
	private static final long serialVersionUID = 7607123437155844820L;
	int phraseUnitId;
    PhraseUnit phraseUnit;
    int wordId;
    Word word;
    int categoryId;
    Category category;
    int subCategoryId;
    SubCategory subCategory;
    int morphologyId;
    Morphology morphology;
    String text = "";
    int position;
    boolean dirty = true;
    TreebankServiceInternal treebankServiceInternal;

    public int getPhraseUnitId() {
        return phraseUnitId;
    }
    public void setPhraseUnitId(int phraseUnitId) {
    	if (this.phraseUnitId!=phraseUnitId){
    		this.phraseUnitId = phraseUnitId;
    		this.dirty = true;
    	}
    }
    public PhraseUnit getPhraseUnit() {
    	if (this.phraseUnit==null && this.phraseUnitId!=0)
    		this.phraseUnit = this.treebankServiceInternal.loadPhraseUnit(this.phraseUnitId);
        return phraseUnit;
    }
    public void setPhraseUnit(PhraseUnit phraseUnit) {
        this.phraseUnit = phraseUnit;
    }
    public int getWordId() {
        return wordId;
    }
    public void setWordId(int wordId) {
    	if (this.wordId!=wordId) {
    		this.wordId = wordId;
    		this.dirty = true;
    	}
    }
    public Word getWord() {
    	if (this.word==null && this.wordId!=0)
    		this.word = this.treebankServiceInternal.loadWord(this.wordId);
        return word;
    }
    public void setWord(Word word) {
        if (this.word==null||!this.word.equals(word)) {
            this.word = word;
            this.setWordId(word==null ? 0 : word.getId());
        }
    }
    public int getCategoryId() {
        return categoryId;
    }
    public void setCategoryId(int categoryId) {
    	if (this.categoryId!=categoryId) {
    		this.categoryId = categoryId;
    		this.dirty = true;
    	}
    }
    public Category getCategory() {
    	if (this.category==null && this.categoryId!=0)
    		this.category = this.treebankServiceInternal.loadCategory(this.categoryId);
        return category;
    }
    public void setCategory(Category category) {
        if (this.category==null||!this.category.equals(category)) {
            this.category = category;
            this.setCategoryId(category==null ? 0 : category.getId());
        }
    }
    public int getSubCategoryId() {
        return subCategoryId;
    }
    public void setSubCategoryId(int subCategoryId) {
    	if (this.subCategoryId!=subCategoryId) {
    		this.subCategoryId = subCategoryId;
    		this.dirty = true;
    	}
    }
    public SubCategory getSubCategory() {
        if (this.subCategory==null && this.subCategoryId!=0) {
            this.subCategory = this.treebankServiceInternal.loadSubCategory(subCategoryId);
        }
        return subCategory;
    }
    public void setSubCategory(SubCategory subCategory) {
        if (this.subCategory==null||!this.subCategory.equals(subCategory)) {
            this.subCategory = subCategory;
            this.setSubCategoryId(subCategory==null ? 0 : subCategory.getId());
        }
    }
    public int getMorphologyId() {
        return morphologyId;
    }
    public void setMorphologyId(int morphologyId) {
    	if (this.morphologyId!=morphologyId) {
    		this.morphologyId = morphologyId;
    		this.dirty = true;
    	}
    }
    public Morphology getMorphology() {
        if (this.morphology==null && this.morphologyId!=0) {
            this.morphology = this.treebankServiceInternal.loadMorphology(morphologyId);
        }
        return morphology;
    }
    public void setMorphology(Morphology morphology) {
        if (this.morphology==null||!this.morphology.equals(morphology)) {
            this.morphology = morphology;
            this.setMorphologyId(morphology==null ? 0 : morphology.getId());
        }
    }
    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PhraseSubunit) {
            return ((PhraseSubunit) obj).getId()==this.getId();
        }
        return false;
    }
    @Override
    public int hashCode() {
        if (this.getId()!=0)
            return this.getId();
        else
            return super.hashCode();
    }
    public int getPosition() {
        return position;
    }
    public void setPosition(int position) {
    	if (this.position!=position) {
    		this.position = position;
    		this.dirty = true;
    	}
    }
    public TreebankServiceInternal getTreebankServiceInternal() {
        return treebankServiceInternal;
    }
    public void setTreebankServiceInternal(
            TreebankServiceInternal treebankServiceInternal) {
        this.treebankServiceInternal = treebankServiceInternal;
    }
    
    public void finalisePhraseSubunit() {
        if (this.wordId==0) {
        	String text = this.text;
        	if (text==null) text = "";
            this.setWord(this.treebankServiceInternal.loadOrCreateWord(text, text));
        }
    }
    
    @Override
    public void saveInternal() {
        if (this.dirty)
        	this.treebankServiceInternal.savePhraseSubunitInternal(this);
    }
	@Override
	public boolean isDirty() {
		return dirty;
	}
	@Override
	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}
    
    
}
