package com.joliciel.talismane.lexicon;

/**
 * A writable lexical entry. When adding a value, the resulting List is
 * guaranteed to be in ascending order.
 * 
 * @author Assaf Urieli
 *
 */
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

  /**
   * If the attribute is a {@link LexicalEntry}, it will set the corresponding
   * entry. If not, it will associate the next available OtherAttribute with the
   * attribute name provided, and store the value against this attribute. If all
   * eight OtherAttributes are already taken, an Exception should be thrown.
   * 
   * @param attribute
   * @param value
   */
  public void setAttribute(String attribute, String value);

}
