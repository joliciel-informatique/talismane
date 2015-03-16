package com.joliciel.talismane.tokeniser;

import com.joliciel.talismane.filters.Sentence;
import com.joliciel.talismane.machineLearning.ClassificationSolution;

/**
 * A sequence of atomic tokens tagged with tokeniser decisions,
 * from which a series of predicted tokens can be inferred.
 * @author Assaf
 *
 */
public interface TokenisedAtomicTokenSequence extends TaggedTokenSequence<TokeniserOutcome>, ClassificationSolution, Comparable<TokenisedAtomicTokenSequence> {
	/**
	 * Infer a token sequence based on the token decisions taken.
	 */
	public TokenSequence inferTokenSequence();

	/**
	 * The original sentence.
	 * @return
	 */
	public Sentence getSentence();
}