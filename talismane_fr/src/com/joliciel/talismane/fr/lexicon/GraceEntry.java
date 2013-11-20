///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2013 Assaf Urieli
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
package com.joliciel.talismane.fr.lexicon;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.joliciel.talismane.lexicon.LexicalEntry;
import com.joliciel.talismane.lexicon.LexicalEntryStatus;
import com.joliciel.talismane.lexicon.PredicateArgument;

/**
 * A lexical entry in Grace format, see http://www.limsi.fr/TLP/grace/www/gracdoc.html
 * @author Assaf Urieli
 */
public class GraceEntry implements LexicalEntry, Serializable {
	private static final long serialVersionUID = 1L;
	String word;
	String lemma;
	String morphology;
	
	transient private String lemmaComplement;
	transient private String category;
	transient private String predicate;
	transient private String subCategory = "";
	transient private List<String> gender = new ArrayList<String>();
	transient private List<String> number = new ArrayList<String>();
	transient private List<String> person = new ArrayList<String>();
	transient private List<String> tense = new ArrayList<String>();
	transient private List<String> possessorNumber = new ArrayList<String>();
	transient private List<PredicateArgument> predicateArguments;
	transient private Map<String,PredicateArgument> predicateArgumentMap;
	transient private Set<String> predicateMacros;
	transient boolean lexicalEntryLoaded = false;
	transient private String morphologyForConll = null;
	
	public GraceEntry(String word, String gracePos, String lemma) {
		this.word = word;
		this.lemma = lemma;
		this.morphology = gracePos;
	}

	public String getWord() {
		return word;
	}

	public String getLemma() {
		return lemma;
	}

	public String getMorphology() {
		return morphology;
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

	public String getSubCategory() {
		this.loadLexicalEntry();
		return subCategory;
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

	public List<PredicateArgument> getPredicateArguments() {
		this.loadLexicalEntry();
		return predicateArguments;
	}

	public Map<String, PredicateArgument> getPredicateArgumentMap() {
		this.loadLexicalEntry();
		return predicateArgumentMap;
	}

	public Set<String> getPredicateMacros() {
		this.loadLexicalEntry();
		return predicateMacros;
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
		if (morphologyForConll==null) {
			StringBuilder sb = new StringBuilder();
			if (this.getGender().size()==1)
				sb.append("g=" + this.getGender().get(0) + "|");
			if (this.getNumber().size()==1)
				sb.append("n=" + this.getNumber().get(0) + "|");
			if (this.getPerson().size()>0) {
				sb.append("p=");
				for (String person : this.getPerson()) {
					sb.append(person);
				}
				sb.append("|");
			}
			if (this.getPossessorNumber().size()>0) {
				sb.append("poss=");
				for (String possessorNumber : this.getPossessorNumber()) {
					sb.append(possessorNumber);
				}
				sb.append("|");
				
			}
			if (this.getTense().size()>0) {
				if (this.getTense().contains("P"))
					sb.append("t=pst|");
				else if (this.getTense().contains("I"))
					sb.append("t=imp|");
				else if (this.getTense().contains("F"))
					sb.append("t=fut|");
				else if (this.getTense().contains("C"))
					sb.append("t=cond|");
				else if (this.getTense().contains("K"))
					sb.append("t=past|");
				else if (this.getTense().contains("G"))
					sb.append("t=pst|");
				else if (this.getTense().contains("S"))
					sb.append("t=pst|");
				else if (this.getTense().contains("J"))
					sb.append("t=past|");
				else if (this.getTense().contains("T"))
					sb.append("t=imp|");
				
//				sb.append("m=");
//				for (String tense : lexicalEntry.getTense()) {
//					sb.append(tense);
//				}
//				sb.append("|");
			}
			
			if (sb.length()>0)
				morphologyForConll = sb.substring(0, sb.length()-1);
			else
				morphologyForConll = "_";

		}
		return morphologyForConll;
	}
	
	void loadLexicalEntry() {
		if (!lexicalEntryLoaded) {
			lexicalEntryLoaded = true;

			this.lemmaComplement = "";
			this.category = this.morphology.substring(0,1);
			this.predicate = "";
			this.subCategory = this.morphology.substring(1,2);
			gender = new ArrayList<String>();
			number = new ArrayList<String>();
			person = new ArrayList<String>();
			tense = new ArrayList<String>();
			possessorNumber = new ArrayList<String>();
			
			if (this.category.equals("N")) {
				gender.add(this.morphology.substring(2,3));
				number.add(this.morphology.substring(3,4));
			} else if (this.category.equals("A")) {
				gender.add(this.morphology.substring(3,4));
				number.add(this.morphology.substring(4,5));
			} else if (this.category.equals("V")) {
				String moodString = this.morphology.substring(2,3);
				String tenseString = this.morphology.substring(3,4);
				if (moodString.equals("i")) {
					if (tenseString.equals("f")) {
						tense.add("F"); // future
					} else if (tenseString.equals("i")) {
						tense.add("I"); // imperfect
					} else if (tenseString.equals("p")) {
						tense.add("P"); // present
					} else if (tenseString.equals("s")) {
						tense.add("J"); // simple past
					}
				} else if (moodString.equals("s")) {
					if (tenseString.equals("i")) {
						tense.add("T"); // past subjunctive
					} else if (tenseString.equals("p")) {
						tense.add("S"); // present subjunctive
					}
				} else if (moodString.equals("c")) {
					tense.add("C"); // conditional
				} else if (moodString.equals("m")) {
					tense.add("Y"); // imperative
				} else if (moodString.equals("n")) {
					tense.add("W"); // imperative
				} else if (moodString.equals("p")) {
					if (tenseString.equals("p")) {
						tense.add("G"); // present participle
					} else if (tenseString.equals("s")) {
						tense.add("K"); // past participle
					}
				}

				String personString = this.morphology.substring(4,5);
				if (!personString.equals("-"))
					person.add(personString);
				String genderString = this.morphology.substring(5,6);
				if (genderString.equals("-")) {
					gender.add("m");
					gender.add("f");
				} else {
					gender.add(genderString);
				}
				String numberString = this.morphology.substring(6,7);
				if (numberString.equals("-")) {
					number.add("p");
					number.add("s");
				} else {
					number.add(numberString);
				}	
			} else if (this.category.equals("R")) {
				// adverb
				// nothing to add
			} else if (this.category.equals("P")) {
				String personString = this.morphology.substring(2,3);
				if (!personString.equals("-"))
					person.add(personString);
				String genderString = this.morphology.substring(3,4);
				if (genderString.equals("-")) {
					gender.add("m");
					gender.add("f");
				} else {
					gender.add(genderString);
				}
				String numberString = this.morphology.substring(4,5);
				if (numberString.equals("-")) {
					number.add("p");
					number.add("s");
				} else {
					number.add(numberString);
				}				
			} else if (this.category.equals("D")) {
				String personString = this.morphology.substring(2,3);
				if (!personString.equals("-"))
					person.add(personString);
				String genderString = this.morphology.substring(3,4);
				if (genderString.equals("-")) {
					gender.add("m");
					gender.add("f");
				} else {
					gender.add(genderString);
				}
				String numberString = this.morphology.substring(4,5);
				if (numberString.equals("-")) {
					number.add("p");
					number.add("s");
				} else {
					number.add(numberString);
				}				
				String possessorString = this.morphology.substring(4,5);
				if (!possessorString.equals("-")) {
					possessorNumber.add(possessorString);
				}
			} else if (this.category.equals("S")) {
				// prepositions
				// nothing to add
			} else if (this.category.equals("C")) {
				// conjunctions
				// nothing to add
			} else if (this.category.equals("I")) {
				// interjections
				// nothing to add
			}
		}
	}
}
