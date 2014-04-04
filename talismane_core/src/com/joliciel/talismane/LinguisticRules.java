//Copyright (C) 2014 Joliciel Informatique
package com.joliciel.talismane;

/**
 * Various generic rules for a given language,
 * indicating via heuristics how to handle different situations.
 * @author Assaf Urieli
 *
 */
public interface LinguisticRules {
	/**
	 * For corpora which provide tokens but not the original text (with white space), should a white space be added
	 * between the previous token and the current token?
	 * Language-specific - e.g. French takes space before :, ? and !, English doesn't
	 * @param previousToken
	 * @param currentToken
	 * @return
	 */
	boolean shouldAddSpace(String previousToken, String currentToken);
}
