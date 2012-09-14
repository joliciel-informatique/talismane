package com.joliciel.talismane.tokeniser;

import java.util.List;

public interface TokeniserDecisionTagSequence extends TaggedTokenSequence<TokeniserDecision> {

	/**
	 * Rebuild a token sequence based on the token decisions taken.
	 */
	public TokenSequence getTokenSequence();

	public String getSentence();

	public int getCurrentPatternEnd();

	public void setCurrentPatternEnd(int currentPatternEnd);
	
	/**
	 * Indicate that this decision tag sequence was based on the decision added.
	 * @param probability
	 */
	public void addDecision(double probability);
	
	/**
	 * The decision probabilities.
	 * @return
	 */
	public List<Double> getDecisionProbabilities();
	
	/**
	 * The log of decision probabilities.
	 * @return
	 */
	public List<Double> getDecisionProbabilityLogs();

}