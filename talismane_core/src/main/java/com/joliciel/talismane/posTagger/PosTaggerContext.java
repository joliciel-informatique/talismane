package com.joliciel.talismane.posTagger;

import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.features.TokenWrapper;

/**
 * The PosTagger's current context, including the history of decisions taken by
 * the POS tagger so far.
 * 
 * @author Assaf Urieli
 *
 */
public interface PosTaggerContext extends TokenWrapper {

	/**
	 * The token being tested,.
	 */
	@Override
	Token getToken();

	/**
	 * The history of tags guessed up to now.
	 */
	PosTagSequence getHistory();

}