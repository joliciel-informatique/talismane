//Copyright (C) 2014 Joliciel Informatique
package com.joliciel.talismane;

import com.joliciel.talismane.tokeniser.TokenSequence;

/**
 * Various generic rules for a given language,
 * indicating via heuristics how to handle different situations.
 * @author Assaf Urieli
 *
 */
public interface LinguisticRules {
	/**
	 * For corpora which provide tokens but not the original text (with white space), should a white space be added
	 * before adding the current token?
	 * Language-specific - e.g. French takes space before :, ? and !, English doesn't
	 * @param tokenSequence the token sequence up to now
	 * @param currentToken the token about to get added
	 * @return
	 */
	boolean shouldAddSpace(TokenSequence tokenSequence, String currentToken);
	
	/**
	 * Attempts to make an adjective in plural form singular.
	 * @param adjective
	 * @return
	 */
	String makeAdjectiveSingular(String adjective);
}
