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

/**
 * a single sentence, containing any number of phrases
 * @author Assaf Urieli
 */
public interface Sentence extends Phrase {
    /** sentence number from treebank (can include text, e.g. "80bis") */
    public String getSentenceNumber();
    public void setSentenceNumber(String sentenceNumber);

    /** id of file containing this sentence */
    public int getFileId();

    /** file containing this sentence */
    public TreebankFile getFile();
    public void setFile(TreebankFile file);

    /** id of text item containing this sentence */
    public int getTextItemId();

    /** Text item containing this sentence */
    public TextItem getTextItem();
    public void setTextItem(TextItem textItem);

    /**
     *  Add a new phrase unit at the end of the current sentence.
     * If a phrase is currently open, the phrase unit will get added to this phrase as well.
     * @param isCompound 
     */
    public PhraseUnit newPhraseUnit(String categoryCode, String subcategoryCode, String morphologyCode, String lemma,
    		boolean isCompound, String splitCompoundId, String splitCompoundNextId, String splitCompoundPrevId);

    /**
     *  Add a new phrase unit at the end of the current sentence.
     * If a phrase is currently open, the phrase unit will get added to this phrase as well.
     * Used when phrase unit is not part of a split compound.
     */
    public PhraseUnit newPhraseUnit(String categoryCode, String subcategoryCode, String morphologyCode, String lemma);

    /** Open a new phrase in the current position within the sentence */
    public Phrase openPhrase(String phraseTypeCode, String functionCode);
    
    /** Close the last opened phrase */
    public void closePhrase();
    
    /** Close the sentence as a whole */
    public void close();
    
    /** Save this sentence */
    public void save();
	public abstract void setText(String text);
	public abstract String getText();
}
