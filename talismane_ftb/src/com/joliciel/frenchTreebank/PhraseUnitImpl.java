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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class PhraseUnitImpl extends EntityImpl implements PhraseUnitInternal {

	int wordId;
    Word word;
    int positionInSentence;
    int phraseId;
    Phrase phrase;
    int lemmaId;
    Word lemma;
    int categoryId;
    Category category;
    int subCategoryId;
    SubCategory subCategory;
    int morphologyId;
    Morphology morphology;
    boolean compound;
    String text = "";
    int positionInPhrase;
    PhraseUnit nextCompoundPart = null;
    int nextCompoundPartId;
    int previousCompoundPartId;
    PhraseUnit previousCompoundPart = null;
    
    boolean dirty = true;
    
    String splitCompoundId;
    String splitCompoundNextId;
    String splitCompoundPrevId;
    
    int guessedPosTagId = 0;
    
    TreebankServiceInternal treebankServiceInternal;
    
    public List<PhraseSubunit> subunits;

    public int getWordId() {
        return wordId;
    }
    public void setWordId(int wordId) {
    	if (wordId!=this.wordId) {
    		this.dirty = true;
    		this.wordId = wordId;
    	}
    }
    public Word getWord() {
    	if (this.word==null&&this.wordId!=0)
    		this.word = this.treebankServiceInternal.loadWord(this.wordId);
        return word;
    }
    public void setWord(Word word) {
        if (this.word==null||!this.word.equals(word)) {
            this.word = word;
            this.setWordId(word==null ? 0 : word.getId());
        }
    }
    public int getPositionInSentence() {
        return positionInSentence;
    }
    public void setPositionInSentence(int position) {
    	if (this.positionInSentence!=position) {
    		this.positionInSentence = position;
    		dirty = true;
    	}
    }
    public int getPhraseId() {
        return phraseId;
    }
    public void setPhraseId(int phraseId) {
    	if (this.phraseId!=phraseId) {
    		this.phraseId = phraseId;
    		this.dirty = true;
    	}
    }
    public Phrase getPhrase() {
    	if (this.phrase==null && this.phraseId!=0) {
    		this.phrase = this.treebankServiceInternal.loadPhrase(this.phraseId);
    	}
        return phrase;
    }
    
    public void setPhrase(Phrase phrase) {
        if (this.phrase==null||!this.phrase.equals(phrase)) {
            this.phrase = phrase;
            this.setPhraseId(phrase==null ? 0 : phrase.getId());
        }
    }
    public int getLemmaId() {
        return lemmaId;
    }
    public void setLemmaId(int lemmaId) {
    	if (this.lemmaId!=lemmaId) {
    		this.lemmaId = lemmaId;
    		this.dirty = true;
    	}
    }
    public Word getLemma() {
        if (this.lemma==null && this.lemmaId!=0) {
            this.lemma = this.treebankServiceInternal.loadWord(this.lemmaId);
        }
        return lemma;
    }
    public void setLemma(Word lemma) {
        if (this.lemma==null||!this.lemma.equals(lemma)) {
            this.lemma = lemma;
            this.setLemmaId(lemma==null ? 0 : lemma.getId());
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
        if (this.category==null && this.categoryId!=0) {
            this.category = this.treebankServiceInternal.loadCategory(this.categoryId);
        }
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
    public boolean isCompound() {
        return compound;
    }
    public void setCompound(boolean compound) {
    	if (this.compound!=compound) {
    		this.compound = compound;
    		this.dirty = true;
    	}
    }

    public List<PhraseSubunit> getSubunits() {
        if (this.isNew()&&this.subunits==null)
            this.subunits = new ArrayList<PhraseSubunit>();
        else if (this.subunits==null)
        	this.subunits = this.treebankServiceInternal.findPhraseSubunits(this);
        return subunits;     
    }
    
    public List<PhraseSubunit> getSubunitsInternal() {
    	return this.subunits;
    }
    public void setSubunitsInternal(List<PhraseSubunit> subunits) {
		this.subunits = subunits;
	}
	public String getText() {
        return text;
    }
    public void setText(String text) {
    	if (!this.text.equals(text)) {
    		this.text = text;
    		dirty = true;
    	}
    }
    public PhraseSubunit newSubunit(String categoryCode, String subcategoryCode, String morphologyCode) {
        PhraseSubunitInternal phraseSubunit = this.treebankServiceInternal.newPhraseSubunit(this);
        phraseSubunit.setPosition(this.getSubunits().size());
        if (categoryCode==null || categoryCode.trim().length()==0)
        	categoryCode = "null";
        if (subcategoryCode==null || subcategoryCode.trim().length()==0)
        	subcategoryCode = "null";
        if (morphologyCode==null || morphologyCode.trim().length()==0)
        	morphologyCode = "null";
		Category category = this.treebankServiceInternal.loadOrCreateCategory(categoryCode.trim());
		SubCategory subCategory = this.treebankServiceInternal.loadOrCreateSubCategory(category, subcategoryCode.trim());
		Morphology morphology = this.treebankServiceInternal.loadOrCreateMorphology(morphologyCode.trim());
       
        phraseSubunit.setCategory(category);
        phraseSubunit.setSubCategory(subCategory);
        phraseSubunit.setMorphology(morphology);
        
        this.getSubunits().add(phraseSubunit);
        return phraseSubunit;
    }
    public int getPositionInPhrase() {
        return positionInPhrase;
    }
    public void setPositionInPhrase(int positionInPhrase) {
    	if (this.positionInPhrase!=positionInPhrase) {
    		this.positionInPhrase = positionInPhrase;
    		this.dirty = true;
    	}
    }
    
    public void finalisePhraseUnit() {
        if (this.wordId==0) {
        	String originalText = this.text;
            if (originalText.length()==0 && this.isCompound()) {
                String compoundText = "";
                PhraseSubunit previousSubunit = null;
                for (PhraseSubunit subunit : this.getSubunits()) {
                    if (previousSubunit == null)
                        compoundText += subunit.getText();
                    else if (previousSubunit.getText().endsWith("'")
                    		|| (previousSubunit.getText().length()==0)
                    		|| (previousSubunit.getCategory()!=null && previousSubunit.getCategory().getCode().equals("PONCT"))
                    		|| (subunit.getCategory()!=null && subunit.getCategory().getCode().equals("PONCT"))
                    		|| (previousSubunit.getText().equals("-"))
                    		|| (subunit.getText().equals("-"))
                    		|| (previousSubunit.getText().equals(",")) // inside numbers
                    		|| (subunit.getText().equals(","))
                    		)
                        compoundText += subunit.getText();
                    else
                        compoundText += " " + subunit.getText();

                    previousSubunit = subunit;
                }
                originalText = compoundText;
            }
            String text = originalText;
            if (originalText.length()>0) {
                String firstWordLetter = originalText.substring(0, 1);
	            if (!firstWordLetter.toLowerCase(Locale.FRENCH).equals(firstWordLetter)) {
	            	if (this.getLemma().getText().length()>0) {
		            	String firstLemmaLetter = this.getLemma().getText().substring(0,1);
		            	if (!firstLemmaLetter.toUpperCase(Locale.FRENCH).equals(firstLemmaLetter)) {
		            		// if word upper-case, but lemma lower-case, lowercase the word's text
		            		if (originalText.startsWith("Au ")&&this.getLemma().getText().startsWith("à ")) {
		            			text = "a" + originalText.substring(1);
		            		} else if (originalText.equals("Au")) {
		            			text = "au";
		            		} else if (firstWordLetter.equals("E")&&this.getLemma().getText().equals("être")) {
		            			if (originalText.equals("Etre"))
		            				text = "être";
		            			else
		            				text = "é" + originalText.substring(1);
		            		} else if ((firstWordLetter.equals("A")&&firstLemmaLetter.equals("à"))
		            			||(firstWordLetter.equals("E")&&firstLemmaLetter.equals("é"))
		            			||(firstWordLetter.equals("E")&&firstLemmaLetter.equals("è"))
		            			||(firstWordLetter.equals("E")&&firstLemmaLetter.equals("ê"))
		            			||(firstWordLetter.equals("E")&&firstLemmaLetter.equals("ë"))
		            			||(firstWordLetter.equals("U")&&firstLemmaLetter.equals("ù"))
		            			||(firstWordLetter.equals("C")&&firstLemmaLetter.equals("ç"))
		            			||(firstWordLetter.equals("O")&&firstLemmaLetter.equals("ô"))
		            			) {
		            			text = firstLemmaLetter + originalText.substring(1);
		            		} else
		            			text = firstWordLetter.toLowerCase(Locale.FRENCH) + originalText.substring(1);
		            	}
	            	}
	            }
            }
            this.setWord(this.treebankServiceInternal.loadOrCreateWord(text, originalText));
        }
        if (this.subunits!=null) {
        	for (PhraseSubunit subunit : this.subunits) {
        		PhraseSubunitInternal subunitInternal = (PhraseSubunitInternal) subunit;
        		subunitInternal.finalisePhraseSubunit();
        	}
        }
    }
    
    public void saveInternal() {
    	if (this.dirty) {
    		if (this.phraseId==0 && this.phrase!=null) {
    			this.phraseId = this.phrase.getId();
    		}
	        
	        this.treebankServiceInternal.savePhraseUnitInternal(this);
    	} // is this phrase unit dirty?
        if (this.subunits!=null)
            for (PhraseSubunit subunit : subunits)
                subunit.save();
    }
    
    public TreebankServiceInternal getTreebankServiceInternal() {
        return treebankServiceInternal;
    }
    public void setTreebankServiceInternal(
            TreebankServiceInternal treebankServiceInternal) {
        this.treebankServiceInternal = treebankServiceInternal;
    }

    @Override
	public String getSplitCompoundId() {
		return splitCompoundId;
	}
	@Override
	public void setSplitCompoundId(String compoundId) {
		this.splitCompoundId = compoundId;
	}
	@Override
	public String getSplitCompoundNextId() {
		return splitCompoundNextId;
	}
	@Override
	public void setSplitCompoundNextId(String compoundNextId) {
		this.splitCompoundNextId = compoundNextId;
	}
	@Override
	public String getSplitCompoundPrevId() {
		return splitCompoundPrevId;
	}
	@Override
	public void setSplitCompoundPrevId(String compoundPrevId) {
		this.splitCompoundPrevId = compoundPrevId;
	}

	@Override
	public PhraseUnit getNextCompoundPart() {
		if (nextCompoundPart==null && nextCompoundPartId!=0) {
			nextCompoundPart = this.treebankServiceInternal.loadPhraseUnit(nextCompoundPartId);
		}
		return nextCompoundPart;
	}
	@Override
	public void setNextCompoundPart(PhraseUnit nextCompoundPart) {
		this.nextCompoundPart = nextCompoundPart;
		if (nextCompoundPart!=null)
			this.setNextCompoundPartId(nextCompoundPart.getId());
	}
	
	@Override
	public int getNextCompoundPartId() {
		return nextCompoundPartId;
	}
	@Override
	public void setNextCompoundPartId(int nextCompoundPartId) {
		if (this.nextCompoundPartId!=nextCompoundPartId) {
			this.nextCompoundPartId = nextCompoundPartId;
			this.dirty = true;
		}
	}
	
	public PhraseUnit getPreviousCompoundPart() {
		if (previousCompoundPart==null && previousCompoundPartId!=0) {
			previousCompoundPart = this.treebankServiceInternal.loadPhraseUnit(previousCompoundPartId);
		}
		return previousCompoundPart;
	}
	
	
	public int getPreviousCompoundPartId() {
		((PhraseInternal)this.getPhrase()).finalisePhrase();
		return previousCompoundPartId;
	}
	public void setPreviousCompoundPartId(int previousCompoundPartId) {
		this.previousCompoundPartId = previousCompoundPartId;
	}

	@Override
    public boolean equals(Object obj) {
        if (obj instanceof PhraseUnit) {
        	if (this.getId()==0)
        		return super.equals(obj);
            return ((PhraseUnit) obj).getId()==this.getId();
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
	@Override
	public int getGuessedPosTagId() {
		return guessedPosTagId;
	}
	@Override
	public void setGuessedPosTagId(int guessedPosTagId) {
		if (this.guessedPosTagId!=guessedPosTagId) {
			this.guessedPosTagId = guessedPosTagId;
			this.dirty = true;
		}
	}
	@Override
	public boolean isDirty() {
		return dirty;
	}
	@Override
	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}
    
	@Override
	public boolean isPhrase() {
		return false;
	}
}
