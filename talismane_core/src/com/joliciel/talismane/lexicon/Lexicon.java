package com.joliciel.talismane.lexicon;

import java.util.List;

/**
 * An interface for retrieving lexical information from a lexicon.
 * @author Assaf Urieli
 *
 */
public interface Lexicon {
	/**
	 * Return all lexical entries for a given word.
	 * @param name
	 * @return
	 */
	public List<LexicalEntry> getEntries(String word);
	

	/**
	 * Return all lexical entries for a given lemma.
	 * @param lemma
	 * @param complement
	 * @return
	 */
	public List<LexicalEntry> getEntriesForLemma(String lemma, String complement);
}
