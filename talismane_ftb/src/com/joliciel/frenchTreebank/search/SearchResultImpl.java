/*
 * Created on 21 Jan 2010
 */
package com.joliciel.frenchTreebank.search;

import java.util.ArrayList;
import java.util.List;

import com.joliciel.frenchTreebank.Phrase;
import com.joliciel.frenchTreebank.PhraseUnit;
import com.joliciel.frenchTreebank.Sentence;

public class SearchResultImpl implements SearchResult {
    private Sentence sentence;
    private Phrase phrase;
    private List<PhraseUnit> phraseUnits = new ArrayList<PhraseUnit>();
    public Phrase getPhrase() {
        return phrase;
    }
    public void setPhrase(Phrase phrase) {
        this.phrase = phrase;
    }
    public List<PhraseUnit> getPhraseUnits() {
        return phraseUnits;
    }
    public Sentence getSentence() {
        return sentence;
    }
    public void setSentence(Sentence sentence) {
        this.sentence = sentence;
    }


}
