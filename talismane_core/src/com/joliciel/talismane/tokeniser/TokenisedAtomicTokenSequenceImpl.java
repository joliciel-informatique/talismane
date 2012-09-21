///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
//
//This file is part of Talismane.
//
//Talismane is free software: you can redistribute it and/or modify
//it under the terms of the GNU Affero General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//Talismane is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Affero General Public License for more details.
//
//You should have received a copy of the GNU Affero General Public License
//along with Talismane.  If not, see <http://www.gnu.org/licenses/>.
//////////////////////////////////////////////////////////////////////////////
package com.joliciel.talismane.tokeniser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.HarmonicMeanScoringStrategy;
import com.joliciel.talismane.machineLearning.ScoringStrategy;
import com.joliciel.talismane.machineLearning.Solution;

class TokenisedAtomicTokenSequenceImpl extends TaggedTokenSequenceImpl<TokeniserOutcome> implements TokenisedAtomicTokenSequence {
	private static final long serialVersionUID = -5837144642078063115L;
	private Pattern whitespace = Pattern.compile("\\s+");
	private String sentence;
	private TokeniserServiceInternal tokeniserServiceInternal;
	private TokenSequence tokenSequence = null;
	private List<Decision<TokeniserOutcome>> decisions = new ArrayList<Decision<TokeniserOutcome>>();
	private List<Solution<?>> underlyingSolutions = new ArrayList<Solution<?>>();
	private ScoringStrategy scoringStrategy = new HarmonicMeanScoringStrategy();
	double score = 1.0;
	boolean scoreCalculated = false;
	
	TokenisedAtomicTokenSequenceImpl(String sentence) {
		this.sentence = sentence;
	}
	
	TokenisedAtomicTokenSequenceImpl(String sentence, int initialCapacity) {
		super(initialCapacity);
		this.sentence = sentence;
	}

	TokenisedAtomicTokenSequenceImpl(
			TokenisedAtomicTokenSequence history) {
		super(history);
		this.decisions = new ArrayList<Decision<TokeniserOutcome>>(history.getDecisions());
		this.sentence = history.getSentence();
	}

	/* (non-Javadoc)
	 * @see com.joliciel.talismane.tokeniser.TokeniserDecisionTagSequence#getTokenSequence()
	 */
	@Override
	public TokenSequence inferTokenSequence() {
		if (tokenSequence==null) {
			tokenSequence = this.getTokeniserServiceInternal().getTokenSequence(sentence, this);
	
			int currentStart = 0;
			int currentEnd = 0;
			Token lastToken = null;
			List<TaggedToken<TokeniserOutcome>> currentAtomicParts = new ArrayList<TaggedToken<TokeniserOutcome>>();
			Token lastNewToken = null;
			
			for (TaggedToken<TokeniserOutcome> decisionTag : this) {
				currentAtomicParts.add(decisionTag);
				Token token = decisionTag.getToken();
				switch (decisionTag.getTag()) {
				case DOES_NOT_SEPARATE:
					// combine two tokens
					currentEnd = token.getEndIndex();
					break;
				case DOES_SEPARATE:
					// make separation (add token)
					String text = sentence.substring(currentStart, currentEnd);
					if (!whitespace.matcher(text).matches()) {
						lastNewToken = this.addToken(tokenSequence, lastToken, currentStart, currentEnd);
						if (lastNewToken!=null) {
							lastNewToken.setAtomicParts(currentAtomicParts);
							currentAtomicParts = new ArrayList<TaggedToken<TokeniserOutcome>>();
						}
					}
					
					currentStart = token.getStartIndex();
					currentEnd = token.getEndIndex();
					break;
				default:
					throw new RuntimeException("Unexpected tokeniser decision: " + decisionTag.getTag());
				}
				lastToken = token;
			}
			lastNewToken = this.addToken(tokenSequence, lastToken, currentStart, currentEnd);
			if (lastNewToken!=null) {
				lastNewToken.setAtomicParts(currentAtomicParts);
			}


			tokenSequence.finalise();
		}
		return tokenSequence;
	}
	
	private Token addToken(TokenSequence tokenSequence, Token lastToken, int start, int end) {
		Token token = null;
		if (start==end) {
			// do nothing
		} else if (lastToken!=null && lastToken.getStartIndex()==start && lastToken.getEndIndex()==end) {
//			tokenSequence.add(lastToken);
//			token = lastToken;
			// Not keeping tokens after all, because tokens refer to their token sequence
			// via token.getTokenSequence(), token.getIndex(), and token.getIndexWithWhiteSpace()
			//TODO: The downside is that tokens can no longer share features across sequences.
			token = tokenSequence.addToken(start, end);
		} else {
			token = tokenSequence.addToken(start, end);
		}
		return token;
	}

	public TokeniserServiceInternal getTokeniserServiceInternal() {
		return tokeniserServiceInternal;
	}

	public void setTokeniserServiceInternal(
			TokeniserServiceInternal tokeniserServiceInternal) {
		this.tokeniserServiceInternal = tokeniserServiceInternal;
	}

	/* (non-Javadoc)
	 * @see com.joliciel.talismane.tokeniser.TokeniserDecisionTagSequence#getSentence()
	 */
	@Override
	public String getSentence() {
		return sentence;
	}

	@Override
	public List<Decision<TokeniserOutcome>> getDecisions() {
		return decisions;
	}

	@Override
	public List<Solution<?>> getUnderlyingSolutions() {
		return underlyingSolutions;
	}

	@Override
	public void addDecision(Decision<TokeniserOutcome> decision) {
		this.decisions.add(decision);
	}

	public ScoringStrategy getScoringStrategy() {
		return scoringStrategy;
	}

	public void setScoringStrategy(ScoringStrategy scoringStrategy) {
		this.scoringStrategy = scoringStrategy;
	}

	@Override
	public double getScore() {
		if (!scoreCalculated) {
			score = this.scoringStrategy.calculateScore(this);
			scoreCalculated = true;
		}
		return score;
	}
	

	@Override
	public int compareTo(TokenisedAtomicTokenSequence o) {
		if (this.getScore()<o.getScore()) {
			return 1;
		} else if (this.getScore()>o.getScore()) {
			return -1;
		} else {
			return 0;
		}
	}
}
