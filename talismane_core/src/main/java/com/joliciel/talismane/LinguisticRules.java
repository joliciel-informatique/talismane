//Copyright (C) 2014 Joliciel Informatique
package com.joliciel.talismane;

/**
 * Various generic rules for a given language, indicating via heuristics how to
 * handle different situations.
 * 
 * @author Assaf Urieli
 *
 */
public interface LinguisticRules {
	/**
	 * For corpora which provide tokens but not the original text (with white
	 * space), should a white space be added before adding the current token?
	 * Language-specific - e.g. French takes space before :, ? and !, English
	 * doesn't
	 * 
	 * @param text
	 *            the sentence text up to now
	 * @param word
	 *            the word about to get added
	 */
	boolean shouldAddSpace(String text, String word);

	/**
	 * Attempts to make an adjective in plural form singular.
	 */
	String makeAdjectiveSingular(String adjective);

	/**
	 * If c is an upper-case character, return the various possible lowercase
	 * characters with diacritics. For example, in French, E will return e, é,
	 * è, ë, ê. Order can be important, since, when looking for the lowercase
	 * equivalent of an uppercase word, the letters will be replaced in the
	 * order given, and the first match will be returned.
	 */
	char[] getLowercaseOptionsWithDiacritics(char c);
}
