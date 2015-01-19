package com.joliciel.talismane.lexicon;

import java.io.Serializable;
import java.util.List;

/**
 * An interface for retrieving lexical information from a lexicon.
 * @author Assaf Urieli
 *
 */
public interface Lexicon extends Serializable {
	/**
	 * This lexicon's name, for use in features.
	 * @return
	 */
	public String getName();
	
	/**
	 * Return all lexical entries for a given word.
	 * @param name
	 * @return
	 */
	public List<LexicalEntry> getEntries(String word);
	

	/**
	 * Return all lexical entries for a given lemma.
	 * @param lemma
	 * @return
	 */
	public List<LexicalEntry> getEntriesForLemma(String lemma);
}
