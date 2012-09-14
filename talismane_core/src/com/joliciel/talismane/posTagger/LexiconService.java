package com.joliciel.talismane.posTagger;

import java.util.List;

public interface LexiconService {
	/**
	 * Return all lexical entries for a given word.
	 * @param name
	 * @return
	 */
	public List<? extends LexicalEntry> getEntries(String word);
	

	/**
	 * Return all lexical entries for a given lemma.
	 * @param lemma
	 * @param complement
	 * @return
	 */
	public List<? extends LexicalEntry> getEntriesForLemma(String lemma, String complement);
}
