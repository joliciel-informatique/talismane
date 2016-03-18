package com.joliciel.talismane.lexicon;

public interface WritableLexicalEntry extends LexicalEntry {

	public void setWord(String word);

	public void setLemma(String lemma);

	public void setLemmaComplement(String lemmaComplement);

	public void setCategory(String category);

	public void setMorphology(String morphology);

	public void setSubCategory(String subCategory);

	public void addGender(String gender);

	public void addNumber(String number);

	public void addPerson(String person);

	public void addTense(String tense);

	public void addMood(String mood);

	public void addAspect(String aspect);

	public void addCase(String grammaticalCase);

	public void addPossessorNumber(String possessorNumber);

	public void setAttribute(String attribute, String value);

}