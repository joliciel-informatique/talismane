package com.joliciel.lefff;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.joliciel.talismane.lexicon.LexicalEntry;
import com.joliciel.talismane.lexicon.LexicalEntryMorphologyReader;
import com.joliciel.talismane.lexicon.LexicalEntryStatus;
import com.joliciel.talismane.lexicon.PredicateArgument;

class LefffEntryMorphologyReader implements LexicalEntryMorphologyReader {

	@Override
	public LexicalEntry readEntry(String token, String lemma, String category,
			String morphology) {
		MorphologyReaderEntry lexicalEntry = new MorphologyReaderEntry();
		lexicalEntry.setCategory(category);
		lexicalEntry.setWord(token);
		lexicalEntry.setLemma(lemma);
		lexicalEntry.setMorphology(morphology);
		
		return lexicalEntry;
	}

	private static final class MorphologyReaderEntry implements LexicalEntry {
		private String word;
		private String lemma;
		private String lemmaComplement;
		private String category;
		private String subCategory;
		private String predicate;
		private List<PredicateArgument> predicateArguments = new ArrayList<PredicateArgument>();
		private Set<String> predicateMacros = new HashSet<String>();
		private List<String> gender = new ArrayList<String>();
		private List<String> number = new ArrayList<String>();
		private List<String> tense = new ArrayList<String>();
		private List<String> person = new ArrayList<String>();
		private List<String> possessorNumber = new ArrayList<String>();
		private String morphology;

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

		public String getCategory() {
			return category;
		}

		public void setCategory(String category) {
			this.category = category;
		}

		public String getSubCategory() {
			return subCategory;
		}

		public String getPredicate() {
			return predicate;
		}

		public List<PredicateArgument> getPredicateArguments() {
			return predicateArguments;
		}

		public Set<String> getPredicateMacros() {
			return predicateMacros;
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

		public List<String> getPerson() {
			return person;
		}

		public List<String> getPossessorNumber() {
			return possessorNumber;
		}

		public String getMorphology() {
			return morphology;
		}

		public void setMorphology(String morphology) {
			this.morphology = morphology;
			readMorphology(this, morphology);
		}

		@Override
		public PredicateArgument getPredicateArgument(String functionName) {
			return null;
		}

		@Override
		public LexicalEntryStatus getStatus() {
			return LexicalEntryStatus.NEUTRAL;
		}
	}
	
	static void readMorphology(LexicalEntry entry, String morphology) {
		if (morphology.contains("m"))
			entry.getGender().add("m");
		if (morphology.contains("f"))
			entry.getGender().add("f");
		if (entry.getGender().size()==0) {
			entry.getGender().add("m");
			entry.getGender().add("f");
		}
		
		if (morphology.contains("1"))
			entry.getPerson().add("1");
		if (morphology.contains("2"))
			entry.getPerson().add("2");
		if (morphology.contains("3"))
			entry.getPerson().add("3");
	
		String morphNoPossessor = morphology;
		if (morphology.endsWith("_P1p")||morphology.endsWith("_P2p")||morphology.endsWith("_P3p")) {
			entry.getPossessorNumber().add("p");
			morphNoPossessor = morphology.substring(0,morphology.length()-4);
		} else if (morphology.endsWith("_P1s")||morphology.endsWith("_P2s")||morphology.endsWith("_P3s")) {
			entry.getPossessorNumber().add("s");
			morphNoPossessor = morphology.substring(0,morphology.length()-4);
		}
		
		if (morphNoPossessor.contains("p"))
			entry.getNumber().add("p");
		if (morphNoPossessor.contains("s"))
			entry.getNumber().add("s");
		if (entry.getNumber().size()==0) {
			entry.getNumber().add("p");
			entry.getNumber().add("s");
		}
		
		if (morphNoPossessor.contains("P"))
			entry.getTense().add("P"); // present
		if (morphNoPossessor.contains("F"))
			entry.getTense().add("F"); // future
		if (morphNoPossessor.contains("I"))
			entry.getTense().add("I"); // imperfect
		if (morphNoPossessor.contains("J"))
			entry.getTense().add("J"); // simple past
		if (morphNoPossessor.contains("T"))
			entry.getTense().add("T"); // past subjunctive
		if (morphNoPossessor.contains("S"))
			entry.getTense().add("S"); // present subjunctive
		if (morphNoPossessor.contains("C"))
			entry.getTense().add("C"); // conditional
		if (morphNoPossessor.contains("K"))
			entry.getTense().add("K"); // past participle
		if (morphNoPossessor.contains("G"))
			entry.getTense().add("G"); // present participle
		if (morphNoPossessor.contains("W"))
			entry.getTense().add("W"); // infinitive
		if (morphNoPossessor.contains("Y"))
			entry.getTense().add("Y"); // imperative
	}
}
