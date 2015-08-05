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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class SentenceImpl extends PhraseImpl implements SentenceInternal {

	static final Log LOG = LogFactory.getLog(SentenceImpl.class);
    private static final String SPLIT_COMPOUND_DUMMY_TEXT = "[[split]]";
    String sentenceNumber;
    int fileId;
    TreebankFile file;
    String text = "";
    int textItemId;
    TextItem textItem;
    boolean isNew = true;
    boolean sentenceDirty = true;
    
    private PhraseInternal currentPhrase;
    
    SentenceImpl() {
        this.setDepth(0);
        this.setPositionInPhrase(0);
        currentPhrase = this;
    }
    
    public String getSentenceNumber() {
        return sentenceNumber;
    }
    public void setSentenceNumber(String sentenceNumber) {
        this.sentenceNumber = sentenceNumber;
    }
    public int getFileId() {
        return fileId;
    }
    public void setFileId(int fileId) {
    	if (this.fileId!=fileId) {
    		this.fileId = fileId;
    		this.sentenceDirty=true;
    	}
    }
    public TreebankFile getFile() {
        if (this.file==null && this.fileId!=0) {
            this.file = this.treebankServiceInternal.loadTreebankFile(this.fileId);
        }
        return file;
    }
    public void setFile(TreebankFile file) {
        if (this.file==null||!this.file.equals(file)) {
            this.file = file;
            this.setFileId(file.getId());
        }
    }
    public int getTextItemId() {
        return textItemId;
    }
    public void setTextItemId(int textItemId) {
    	if (this.textItemId!=textItemId) {
	        this.textItemId = textItemId;
			this.sentenceDirty=true;
    	}
    }
    public TextItem getTextItem() {
    	if (textItem==null&&textItemId!=0)
    		textItem = this.treebankServiceInternal.loadTextItem(textItemId);
        return textItem;
    }
    public void setTextItem(TextItem textItem) {
        if (this.textItem==null||!this.textItem.equals(textItem)) {
            this.textItem = textItem;
            this.setTextItemId(textItem.getId());
        }
    }

	public void saveInternal() {
        super.saveInternal();
        if (this.sentenceDirty)
        	this.treebankServiceInternal.saveSentenceInternal(this);
        if (this.children!=null)
            for (PhraseInternal phrase : this.children)
                phrase.save();
        
        //TODO: this should really update the mapping only when one of the descendents has changed
        // but for now we're assuming they're assigned once and don't change
        if (this.isNew())
        	this.savePhraseDescendentMapping();

        // assign verb sub-categories before saving phrase units
        this.assignVerbSubcategories();
        
        if (this.phraseUnits!=null) {
            for (PhraseUnitInternal phraseUnit : this.phraseUnits)
                phraseUnit.save();
            
            // indicate split compounds
            // this has to be done after phrase units are first saved, so that we already have ids
            // to tie the parts of the split compound together
            for (PhraseUnitInternal phraseUnit : this.phraseUnits) {
            	if (phraseUnit.getNextCompoundPart()!=null) {
            		// fix the words so that we have the full compound word once
            		// against the correct part of speech
            		Word firstPartWord = phraseUnit.getWord();
            		Word secondPartWord = phraseUnit.getNextCompoundPart().getWord();
            		String fullWordText = firstPartWord.getText();
            		if (fullWordText.endsWith("'"))
            			fullWordText += secondPartWord.getText();
            		else
            			fullWordText += " " + secondPartWord.getText();
            		Word newFirstWord = this.treebankServiceInternal.loadOrCreateWord(fullWordText, firstPartWord.getOriginalText());
            		phraseUnit.setWord(newFirstWord);
            		Word newSecondWord = this.treebankServiceInternal.loadOrCreateWord(SPLIT_COMPOUND_DUMMY_TEXT, secondPartWord.getOriginalText());
            		((PhraseUnitInternal)phraseUnit.getNextCompoundPart()).setWord(newSecondWord);
            		
            		// set the newly saved id for the next compound part
            		phraseUnit.setNextCompoundPartId(phraseUnit.getNextCompoundPart().getId());
            		phraseUnit.save();
            		phraseUnit.getNextCompoundPart().save();
            	}
            }
        }
    }

    public Phrase openPhrase(String phraseTypeCode, String functionCode) {
        PhraseInternal phrase = currentPhrase.newChild();
        PhraseType phraseType = this.treebankServiceInternal.loadOrCreatePhraseType(phraseTypeCode);
        Function function = this.treebankServiceInternal.loadOrCreateFunction(functionCode);
        phrase.setPhraseType(phraseType);
        phrase.setFunction(function);
        currentPhrase = phrase;
        
        return phrase;
    }
    
    public void closePhrase() {
        if (currentPhrase==null)
            throw new RuntimeException("Cannot close phrase unless it's been opened first.");
        else
            currentPhrase = (PhraseInternal) currentPhrase.getParent();
    }
    
    public void close() {
    	for (PhraseUnitInternal phraseUnit : this.getPhraseUnitsInternal()) {
    		phraseUnit.finalisePhraseUnit();
    		
    		if (phraseUnit.getSplitCompoundNextId().length()>0) {
    			for (PhraseUnitInternal nextPhraseUnit : this.getPhraseUnitsInternal()) {
    				if (!nextPhraseUnit.equals(phraseUnit) && nextPhraseUnit.getSplitCompoundId().equals(phraseUnit.getSplitCompoundNextId())) {
    					phraseUnit.setNextCompoundPart(nextPhraseUnit);
    					break;
    				}
    			}
    		}
    	}
    	for (PhraseUnitInternal phraseUnit : this.getPhraseUnitsInternal()) {
    		if (phraseUnit.getSplitCompoundPrevId().length()>0) {
    			for (PhraseUnitInternal prevPhraseUnit : this.getPhraseUnitsInternal()) {
    				if (!prevPhraseUnit.equals(phraseUnit) && prevPhraseUnit.getSplitCompoundId().equals(phraseUnit.getSplitCompoundPrevId())) {
    					prevPhraseUnit.setNextCompoundPart(phraseUnit);
    					break;
    				}
    			}
    		}
    	}
    }
    
    public PhraseUnit newPhraseUnit(String categoryCode,
            String subcategoryCode, String morphologyCode, String lemma) {
    	return this.newPhraseUnit(categoryCode, subcategoryCode, morphologyCode, lemma, false, "", "", "");
    }
    
    public PhraseUnit newPhraseUnit(String categoryCode,
            String subcategoryCode, String morphologyCode, String lemma, boolean isCompound, 
            String splitCompoundId, String splitCompoundNextId, String splitCompoundPrevId) {
        PhraseUnitInternal phraseUnit = this.treebankServiceInternal.newPhraseUnit();
        phraseUnit.setPositionInSentence(this.getPhraseUnits().size());
        
        if (categoryCode==null || categoryCode.trim().length()==0)
        	categoryCode = "null";
        if (subcategoryCode==null || subcategoryCode.trim().length()==0)
        	subcategoryCode = "null";
        if (morphologyCode==null || morphologyCode.trim().length()==0)
        	morphologyCode = "null";
        if (lemma == null) lemma = "";
        if (splitCompoundId == null) splitCompoundId = "";
        if (splitCompoundNextId == null) splitCompoundNextId = "";
        if (splitCompoundPrevId == null) splitCompoundPrevId = "";
        
        Category category = this.treebankServiceInternal.loadOrCreateCategory(categoryCode.trim());
        SubCategory subCategory = this.treebankServiceInternal.loadOrCreateSubCategory(category, subcategoryCode.trim());
        Morphology morphology = this.treebankServiceInternal.loadOrCreateMorphology(morphologyCode.trim());
        Word lemmaWord = this.treebankServiceInternal.loadOrCreateWord(lemma.trim(), lemma.trim());
        
        phraseUnit.setCategory(category);
        phraseUnit.setSubCategory(subCategory);
        phraseUnit.setMorphology(morphology);
        phraseUnit.setLemma(lemmaWord);
        phraseUnit.setCompound(isCompound);
        phraseUnit.setSplitCompoundId(splitCompoundId.trim());
        phraseUnit.setSplitCompoundNextId(splitCompoundNextId.trim());
        phraseUnit.setSplitCompoundPrevId(splitCompoundPrevId.trim());
        
        this.getPhraseUnitsInternal().add(phraseUnit);
        currentPhrase.addPhraseUnit(phraseUnit);
        
        this.getAllPhraseUnitsDB().add(phraseUnit);
        return phraseUnit;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Sentence) {
            return ((Sentence) obj).getId()==this.getId();
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
    
    public boolean isNew() {
        return isNew;
    }
    
    public void setIsNew(boolean isNew) { this.isNew = isNew; }

	@Override
	public String getText() {
		return text;
	}

	@Override
	public void setText(String text) {
		if (text==null) text = "";
		if (!this.text.equals(text)) {
			this.text = text;
			this.sentenceDirty=true;
		}
	}

	@Override
	public boolean isSentenceDirty() {
		return sentenceDirty;
	}

	@Override
	public void setSentenceDirty(boolean sentenceDirty) {
		this.sentenceDirty = sentenceDirty;
	}

}
