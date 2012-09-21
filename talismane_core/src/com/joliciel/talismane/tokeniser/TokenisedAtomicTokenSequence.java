package com.joliciel.talismane.tokeniser;

import com.joliciel.talismane.machineLearning.Solution;

/**
 * A sequence of atomic tokens tagged with tokeniser decisions,
 * from which a series of predicted tokens can be inferred.
 * @author Assaf
 *
 */
public interface TokenisedAtomicTokenSequence extends TaggedTokenSequence<TokeniserOutcome>, Solution<TokeniserOutcome>, Comparable<TokenisedAtomicTokenSequence> {
	/**
	 * Infer a token sequence based on the token decisions taken.
	 */
	public TokenSequence inferTokenSequence();

	/**
	 * The original sentence, as a string.
	 * @return
	 */
	public String getSentence();
}