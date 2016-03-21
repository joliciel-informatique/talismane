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
package com.joliciel.talismane.lexicon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class SimpleLexicalEntry extends AbstractLexicalEntry implements WritableLexicalEntry {
	private static final long serialVersionUID = 1L;
	
	private String word;
	private String lemma;
	private String lemmaComplement = "";
	private String category = "";
	private String subCategory = "";
	private List<String> gender = new ArrayList<String>();
	private List<String> number = new ArrayList<String>();
	private List<String> tense = new ArrayList<String>();
	private List<String> mood = new ArrayList<String>();
	private List<String> aspect = new ArrayList<String>();
	private List<String> person = new ArrayList<String>();
	private List<String> possessorNumber = new ArrayList<String>();
	private List<String> grammaticalCase = new ArrayList<String>();
	private String morphology = "";
	private String lexiconName = "";
	private Map<String,String> attributes;
	
	@Override
	public String getAttribute(String attribute) {
		return this.attributes.get(attribute);
	}

	@Override
	public void setAttribute(String attribute, String value) {
		this.attributes.put(attribute, value);
	}
	
	public String getWord() {
		return word;
	}

	public void setWord(String word) {
		this.word = word;
	}

	public String getLemma() {
		return lemma;
	}

	public void setLemma(String lemma) {
		this.lemma = lemma;
	}

	public String getLemmaComplement() {
		return lemmaComplement;
	}

	public void setLemmaComplement(String lemmaComplement) {
		this.lemmaComplement = lemmaComplement;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getSubCategory() {
		return subCategory;
	}

	public void setSubCategory(String subCategory) {
		this.subCategory = subCategory;
	}

	public String getMorphology() {
		return morphology;
	}

	public void setMorphology(String morphology) {
		this.morphology = morphology;
	}

	public String getLexiconName() {
		return lexiconName;
	}

	public void setLexiconName(String lexiconName) {
		this.lexiconName = lexiconName;
	}
	
	public List<String> getGender() {
		return gender;
	}

	public List<String> getNumber() {
		return number;
	}

	public List<String> getTense() {
		return tense;
	}

	public List<String> getMood() {
		return mood;
	}

	public List<String> getAspect() {
		return aspect;
	}

	public List<String> getPerson() {
		return person;
	}

	public List<String> getPossessorNumber() {
		return possessorNumber;
	}

	public List<String> getCase() {
		return grammaticalCase;
	}

	@Override
	public void addGender(String gender) {
		if (this.gender==null) this.gender = new ArrayList<String>();
		this.gender.add(gender);
	}

	@Override
	public void addNumber(String number) {
		if (this.number==null) this.number = new ArrayList<String>();
		this.number.add(number);
	}

	@Override
	public void addPerson(String person) {
		if (this.person==null) this.person = new ArrayList<String>();
		this.person.add(person);
	}

	@Override
	public void addTense(String tense) {
		if (this.tense==null) this.tense = new ArrayList<String>();
		this.tense.add(tense);
	}

	@Override
	public void addMood(String mood) {
		if (this.mood==null) this.mood = new ArrayList<String>();
		this.mood.add(mood);
	}

	@Override
	public void addAspect(String aspect) {
		if (this.aspect==null) this.aspect = new ArrayList<String>();
		this.aspect.add(aspect);
	}

	@Override
	public void addCase(String grammaticalCase) {
		if (this.grammaticalCase==null) this.grammaticalCase = new ArrayList<String>();
		this.grammaticalCase.add(grammaticalCase);
	}

	@Override
	public void addPossessorNumber(String possessorNumber) {
		if (this.possessorNumber==null) this.possessorNumber = new ArrayList<String>();
		this.possessorNumber.add(possessorNumber);
	}

	@Override
	public boolean hasAttribute(LexicalAttribute attribute) {
		switch (attribute) {
		case Aspect:
			return this.aspect.size()>0;
		case Case:
			return this.grammaticalCase.size()>0;
		case Category:
			return this.category.length()>0;
		case Gender:
			return this.gender.size()>0;
		case Lemma:
			return this.lemma != null;
		case LemmaComplement:
			return this.lemmaComplement.length()>0;
		case Mood:
			return this.mood.size()>0;
		case Morphology:
			return this.morphology.length()>0;
		case Number:
			return this.number.size()>0;
		case Person:
			return this.person.size()>0;
		case PossessorNumber:
			return this.possessorNumber.size()>0;
		case SubCategory:
			return this.subCategory.length()>0;
		case Tense:
			return this.tense.size()>0;
		case Word:
			return this.word != null;
		default:
			return false;
		}
	}


}
