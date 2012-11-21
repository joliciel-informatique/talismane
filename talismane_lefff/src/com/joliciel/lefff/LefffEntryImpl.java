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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.joliciel.talismane.lexicon.LexicalEntry;
import com.joliciel.talismane.lexicon.LexicalEntryStatus;
import com.joliciel.talismane.lexicon.PredicateArgument;

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
	transient private List<PredicateArgument> predicateArguments;
	transient private Map<String,PredicateArgument> predicateArgumentMap;
	transient private Set<String> predicateMacros;
	
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
			lexicalEntryLoaded = true;
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
			LefffEntryMorphologyReader.readMorphology(this, morph);
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
		if (this==o)
			return 0;
		if (!this.status.equals(o.getStatus()))
			return (this.status.compareTo(o.getStatus()));
		if (!this.word.equals(o.getWord()))
			return (this.word.compareTo(o.getWord()));
		if (!this.category.equals(o.getCategory()))
			return (this.category.compareTo(o.getCategory()));
		if (!this.lemma.equals(o.getLemma()))
			return (this.lemma.compareTo(o.getLemma()));
		if (!this.subCategory.equals(o.getSubCategory()))
			return (this.subCategory.compareTo(o.getSubCategory()));
		if (!this.morphology.equals(o.getMorphology()))
			return (this.morphology.compareTo(o.getMorphology()));
		return -1;
	}

	@Override
	public List<PredicateArgument> getPredicateArguments() {
		if (this.predicateArguments==null) {
			this.predicateArguments = new ArrayList<PredicateArgument>();
			this.predicateArgumentMap = new HashMap<String, PredicateArgument>();
			if (this.predicate.length()>0) {
				String[] arguments = this.predicate.split(",");
				int i = 0;
				for (String argument : arguments) {
					int colonIndex = argument.indexOf(':');
					LefffPredicateArgument predicateArgument = new LefffPredicateArgument();
					if (colonIndex>0) {
						String function = argument.substring(0, colonIndex);
						String realisationString = argument.substring(colonIndex+1);
						boolean optional = false;
						if (realisationString.charAt(0)=='(') {
							optional = true;
							realisationString = realisationString.substring(1,realisationString.length()-1);
						}
						String[] realisations = realisationString.split("[|]");
						predicateArgument.setFunction(function);
						predicateArgument.setOptional(optional);
						for (String realisation : realisations) {
							predicateArgument.getRealisations().add(realisation);
						}
					} else {
						predicateArgument.setFunction(argument);
					}
					predicateArgument.setIndex(i);
					this.predicateArguments.add(predicateArgument);
					this.predicateArgumentMap.put(predicateArgument.getFunction(), predicateArgument);
					i++;
				} // next argument
			} // has predicate?
		} // is null?
		return this.predicateArguments;
	}

	@Override
	public PredicateArgument getPredicateArgument(String functionName) {
		this.getPredicateArguments();
		return this.predicateArgumentMap.get(functionName);
	}

	private static final class LefffPredicateArgument implements PredicateArgument {
		private String function = "";
		private boolean optional = false;
		private Set<String> realisations = new HashSet<String>();
		private int index;
		
		public String getFunction() {
			return function;
		}
		public void setFunction(String function) {
			this.function = function;
		}
		public boolean isOptional() {
			return optional;
		}
		public void setOptional(boolean optional) {
			this.optional = optional;
		}
		public Set<String> getRealisations() {
			return realisations;
		}
		public int getIndex() {
			return index;
		}
		public void setIndex(int index) {
			this.index = index;
		}
		
		
	}
	
	@Override
	public Set<String> getPredicateMacros() {
		if (this.predicateMacros==null) {
			this.predicateMacros = new HashSet<String>();
			for (Attribute attribute : this.attributes) {
				String macro = null;
				if (attribute.getValue()==null || attribute.getValue().length()==0) {
					macro = attribute.getCode();
				} else {
					macro = attribute.getCode() + "=" + attribute.getValue();
				}
				this.predicateMacros.add(macro);
			}
		}
		return this.predicateMacros;
	}
	
}
