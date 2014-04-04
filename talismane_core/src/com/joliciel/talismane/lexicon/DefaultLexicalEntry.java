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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A default lexical entry, which can be overridden to provide specific morphology rules (especially getMorphology()).
 * @author Assaf Urieli
 *
 */
public class DefaultLexicalEntry implements LexicalEntry, Serializable {
	private static final long serialVersionUID = 1L;
	
	private String word;
	private String lemma;
	private String lemmaComplement;
	private String category;
	private String predicate;
	protected String morphology;
	private String subCategory = "";
	private List<String> gender = new ArrayList<String>();
	private List<String> number = new ArrayList<String>();
	private List<String> person = new ArrayList<String>();
	private List<String> tense = new ArrayList<String>();
	private List<String> possessorNumber = new ArrayList<String>();
	private List<PredicateArgument> predicateArguments;
	private Map<String,PredicateArgument> predicateArgumentMap;
	private Set<String> predicateMacros;
	
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
	public String getPredicate() {
		return predicate;
	}
	public void setPredicate(String predicate) {
		this.predicate = predicate;
	}
	public String getMorphology() {
		if (this.morphology==null) {
			morphology = "";
			if (number.size()>0) {
				morphology+= "number=";
				boolean first = true;
				for (String aNumber : number) {
					if (!first)
						morphology+= ",";
					morphology += aNumber;
					morphology += "|";
				}
			}
			if (gender.size()>0) {
				morphology+= "gender=";
				boolean first = true;
				for (String aGender : gender) {
					if (!first)
						morphology+= ",";
					morphology += aGender;
					morphology += "|";
				}
			}
			if (tense.size()>0) {
				morphology+= "tense=";
				boolean first = true;
				for (String aTense : tense) {
					if (!first)
						morphology+= ",";
					morphology += aTense;
					morphology += "|";
				}
			}
			if (person.size()>0) {
				morphology+= "person=";
				boolean first = true;
				for (String aPerson : person) {
					if (!first)
						morphology+= ",";
					morphology += aPerson;
					morphology += "|";
				}
			}
			if (possessorNumber.size()>0) {
				morphology+= "possessorNumber=";
				boolean first = true;
				for (String aPossessorNumber : possessorNumber) {
					if (!first)
						morphology+= ",";
					morphology += aPossessorNumber;
					morphology += "|";
				}
			}
		}
		return morphology;
	}
	public void setMorphology(String morphology) {
		this.morphology = morphology;
	}
	public String getSubCategory() {
		return subCategory;
	}
	public void setSubCategory(String subCategory) {
		this.subCategory = subCategory;
	}
	public List<String> getGender() {
		return gender;
	}
	public void setGender(List<String> gender) {
		this.gender = gender;
	}
	public List<String> getNumber() {
		return number;
	}
	public void setNumber(List<String> number) {
		this.number = number;
	}
	public List<String> getPerson() {
		return person;
	}
	public void setPerson(List<String> person) {
		this.person = person;
	}
	public List<String> getTense() {
		return tense;
	}
	public void setTense(List<String> tense) {
		this.tense = tense;
	}
	public List<String> getPossessorNumber() {
		return possessorNumber;
	}
	public void setPossessorNumber(List<String> possessorNumber) {
		this.possessorNumber = possessorNumber;
	}
	public List<PredicateArgument> getPredicateArguments() {
		return predicateArguments;
	}
	public void setPredicateArguments(List<PredicateArgument> predicateArguments) {
		this.predicateArguments = predicateArguments;
	}
	public Map<String, PredicateArgument> getPredicateArgumentMap() {
		return predicateArgumentMap;
	}
	public void setPredicateArgumentMap(
			Map<String, PredicateArgument> predicateArgumentMap) {
		this.predicateArgumentMap = predicateArgumentMap;
	}
	public Set<String> getPredicateMacros() {
		return predicateMacros;
	}
	public void setPredicateMacros(Set<String> predicateMacros) {
		this.predicateMacros = predicateMacros;
	}
	@Override
	public PredicateArgument getPredicateArgument(String functionName) {
		return null;
	}
	@Override
	public LexicalEntryStatus getStatus() {
		return LexicalEntryStatus.NEUTRAL;
	}
	@Override
	public String getMorphologyForCoNLL() {
		return this.getMorphology();
	}

}
