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

import java.util.ArrayList;
import java.util.List;

import com.joliciel.talismane.posTagger.LexicalEntry;
import com.joliciel.talismane.posTagger.LexicalEntryStatus;

class LefffEntryImpl extends EntityImpl implements LefffEntryInternal, Comparable<LexicalEntry> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5257360126650484517L;
	private transient int wordId;
	private transient int lemmaId;
	private transient int predicateId;
	private transient int morphologyId;
	private transient int categoryId;
	
	private int lexicalWeight = 100;
	
	private Word lefffWord;
	private Lemma lefffLemma;
	private Predicate lefffPredicate;
	private Attribute lefffMorphology;
	private Category lefffCategory;
	
	private List<Attribute> attributes;
	private LexicalEntryStatus status;

	transient private String word;
	transient private String lemma;
	transient private String lemmaComplement;
	transient private String category;
	transient private String predicate;
	transient private String morphology;
	transient private String subCategory = "";
	transient private List<String> gender = new ArrayList<String>();
	transient private List<String> number = new ArrayList<String>();
	transient private List<String> person = new ArrayList<String>();
	transient private List<String> tense = new ArrayList<String>();
	transient private List<String> possessorNumber = new ArrayList<String>();
	
	transient boolean lexicalEntryLoaded = false;

	private transient LefffServiceInternal lefffServiceInternal;

	@Override
	public void saveInternal() {
		this.lefffServiceInternal.saveEntry(this);
		if (this.attributes!=null)
			this.lefffServiceInternal.saveAttributes(this);
	}

	public int getWordId() {
		return wordId;
	}

	public void setWordId(int wordId) {
		this.wordId = wordId;
	}

	public int getLemmaId() {
		return lemmaId;
	}

	public void setLemmaId(int lemmaId) {
		this.lemmaId = lemmaId;
	}

	public int getPredicateId() {
		return predicateId;
	}

	public void setPredicateId(int predicateId) {
		this.predicateId = predicateId;
	}

	public int getMorphologyId() {
		return morphologyId;
	}

	public void setMorphologyId(int morphologyId) {
		this.morphologyId = morphologyId;
	}

	public Word getLefffWord() {
		if (lefffWord==null && wordId!=0)
			lefffWord = lefffServiceInternal.loadWord(wordId);
		return lefffWord;
	}

	public void setWord(Word word) {
		this.lefffWord = word;
		this.setWordId(word.getId());
	}

	public Lemma getLefffLemma() {
		if (lefffLemma==null && lemmaId!=0)
			lefffLemma = lefffServiceInternal.loadLemma(lemmaId);
		return lefffLemma;
	}

	@Override
	public void setLemma(Lemma lemma) {
		this.lefffLemma = lemma;
		this.setLemmaId(lemma.getId());
	}

	@Override
	public void setPredicate(Predicate predicate) {
		this.lefffPredicate = predicate;
		this.setPredicateId(predicate.getId());
	}

	@Override
	public void setMorphology(Attribute morphology) {
		this.lefffMorphology = morphology;
		this.setMorphologyId(morphology.getId());
	}

	@Override
	public void setCategory(Category category) {
		this.lefffCategory = category;
		this.setCategoryId(category.getId());
	}

	public Predicate getLefffPredicate() {
		if (lefffPredicate==null && predicateId!=0)
			lefffPredicate = lefffServiceInternal.loadPredicate(predicateId);
		return lefffPredicate;
	}

	public Attribute getLefffMorphology() {
		if (lefffMorphology==null && morphologyId!=0)
			lefffMorphology = lefffServiceInternal.loadAttribute(morphologyId);
		return lefffMorphology;
	}

	public List<Attribute> getAttributes() {
		if (attributes==null&&this.isNew())
			attributes = new ArrayList<Attribute>();
		else if (attributes==null)
			attributes = lefffServiceInternal.findAttributes(this);
		return attributes;
	}

	public LefffServiceInternal getLefffServiceInternal() {
		return lefffServiceInternal;
	}

	public void setLefffServiceInternal(LefffServiceInternal lefffServiceInternal) {
		this.lefffServiceInternal = lefffServiceInternal;
	}

	public int getLexicalWeight() {
		return lexicalWeight;
	}

	public void setLexicalWeight(int lexicalWeight) {
		this.lexicalWeight = lexicalWeight;
	}

	public int getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(int categoryId) {
		this.categoryId = categoryId;
	}

	public Category getLefffCategory() {
		if (this.lefffCategory==null && this.categoryId!=0)
			lefffCategory = this.lefffServiceInternal.loadCategory(categoryId);
		return lefffCategory;
	}

	void loadLexicalEntry() {
		if (!lexicalEntryLoaded) {
			this.word = this.getLefffWord().getText();
			this.lemma = this.getLefffLemma().getText();
			this.lemmaComplement = this.getLefffLemma().getComplement();
			this.category = this.getLefffCategory().getCode();
			this.predicate = this.getLefffPredicate().getText();
			this.subCategory = "";
			gender = new ArrayList<String>();
			number = new ArrayList<String>();
			person = new ArrayList<String>();
			tense = new ArrayList<String>();
			possessorNumber = new ArrayList<String>();
			
			String morph = this.getLefffMorphology().getValue();
			this.morphology = morph;
			
			if (morph.contains("m"))
				this.gender.add("m");
			if (morph.contains("f"))
				this.gender.add("f");
			if (this.gender.size()==0) {
				this.gender.add("m");
				this.gender.add("f");
			}
			
			if (morph.contains("1"))
				this.person.add("1");
			if (morph.contains("2"))
				this.person.add("2");
			if (morph.contains("3"))
				this.person.add("3");
		
			String morphNoPossessor = morph;
			if (morph.endsWith("_P1p")||morph.endsWith("_P2p")||morph.endsWith("_P3p")) {
				this.possessorNumber.add("p");
				morphNoPossessor = morph.substring(0,morph.length()-4);
			} else if (morph.endsWith("_P1s")||morph.endsWith("_P2s")||morph.endsWith("_P3s")) {
				this.possessorNumber.add("s");
				morphNoPossessor = morph.substring(0,morph.length()-4);
			}
			
			if (morphNoPossessor.contains("p"))
				this.number.add("p");
			if (morphNoPossessor.contains("s"))
				this.number.add("s");
			if (this.number.size()==0) {
				this.number.add("p");
				this.number.add("s");
			}
			
			if (morphNoPossessor.contains("P"))
				this.tense.add("P"); // present
			if (morphNoPossessor.contains("F"))
				this.tense.add("F"); // future
			if (morphNoPossessor.contains("I"))
				this.tense.add("I"); // imperfect
			if (morphNoPossessor.contains("J"))
				this.tense.add("J"); // simple past
			if (morphNoPossessor.contains("T"))
				this.tense.add("T"); // past subjunctive
			if (morphNoPossessor.contains("S"))
				this.tense.add("S"); // present subjunctive
			if (morphNoPossessor.contains("C"))
				this.tense.add("C"); // conditional
			if (morphNoPossessor.contains("K"))
				this.tense.add("K"); // past participle
			if (morphNoPossessor.contains("G"))
				this.tense.add("G"); // present participle
			if (morphNoPossessor.contains("W"))
				this.tense.add("W"); // infinitive
			if (morphNoPossessor.contains("Y"))
				this.tense.add("Y"); // imperative
			
			lexicalEntryLoaded = true;
		}
	}

	public String getWord() {
		this.loadLexicalEntry();
		return word;
	}

	public String getLemma() {
		this.loadLexicalEntry();
		return lemma;
	}

	public String getLemmaComplement() {
		this.loadLexicalEntry();
		return lemmaComplement;
	}

	public String getCategory() {
		this.loadLexicalEntry();
		return category;
	}

	public String getPredicate() {
		this.loadLexicalEntry();
		return predicate;
	}

	public String getMorphology() {
		this.loadLexicalEntry();
		return morphology;
	}

	public List<String> getGender() {
		this.loadLexicalEntry();
		return gender;
	}

	public List<String> getNumber() {
		this.loadLexicalEntry();
		return number;
	}

	public List<String> getPerson() {
		this.loadLexicalEntry();
		return person;
	}

	public List<String> getTense() {
		this.loadLexicalEntry();
		return tense;
	}

	public List<String> getPossessorNumber() {
		this.loadLexicalEntry();
		return possessorNumber;
	}

	public String getSubCategory() {
		return subCategory;
	}

	public LexicalEntryStatus getStatus() {
		return status;
	}

	public void setStatus(LexicalEntryStatus status) {
		this.status = status;
	}

	@Override
	public int compareTo(LexicalEntry o) {
		if (!this.word.equals(o.getWord()))
			return (this.word.compareTo(o.getWord()));
		if (!this.category.equals(o.getCategory()))
			return (this.category.compareTo(o.getCategory()));
		if (!this.status.equals(o.getStatus()))
			return (this.status.compareTo(o.getStatus()));
		if (!this.lemma.equals(o.getLemma()))
			return (this.lemma.compareTo(o.getLemma()));
		if (!this.subCategory.equals(o.getSubCategory()))
			return (this.subCategory.compareTo(o.getSubCategory()));
		if (!this.morphology.equals(o.getMorphology()))
			return (this.morphology.compareTo(o.getMorphology()));
		if (!this.equals(o))
			return -1;
		return 0;
	}
	
}
