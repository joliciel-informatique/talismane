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

class TokeniserDecisionTagSequenceImpl extends TaggedTokenSequenceImpl<TokeniserDecision> implements TokeniserDecisionTagSequence {

	private static final long serialVersionUID = -5837144642078063115L;
	private Pattern whitespace = Pattern.compile("\\s+");
	private String sentence;
	private TokeniserServiceInternal tokeniserServiceInternal;
	private int currentPatternEnd = -1;
	private TokenSequence tokenSequence = null;
	private List<Double> decisionProbabilities = new ArrayList<Double>();
	private List<Double> decisionProbabilityLogs = new ArrayList<Double>();
	
	TokeniserDecisionTagSequenceImpl(String sentence) {
		this.sentence = sentence;
	}
	
	TokeniserDecisionTagSequenceImpl(String sentence, int initialCapacity) {
		super(initialCapacity);
		this.sentence = sentence;
	}

	TokeniserDecisionTagSequenceImpl(
			TokeniserDecisionTagSequence sequence1,
			TokeniserDecisionTagSequence sequence2) {
		super(sequence1, sequence2);
		this.sentence = sequence1.getSentence();

	}

	TokeniserDecisionTagSequenceImpl(
			TokeniserDecisionTagSequence history) {
		super(history);
		this.sentence = history.getSentence();
	}

	/* (non-Javadoc)
	 * @see com.joliciel.talismane.tokeniser.TokeniserDecisionTagSequence#getTokenSequence()
	 */
	@Override
	public TokenSequence getTokenSequence() {
		if (tokenSequence==null) {
			tokenSequence = this.getTokeniserServiceInternal().getTokenSequence(sentence, this);
	
			int currentStart = 0;
			int currentEnd = 0;
			Token lastToken = null;
			List<TaggedToken<TokeniserDecision>> currentDecisions = new ArrayList<TaggedToken<TokeniserDecision>>();
			Token lastNewToken = null;
			
			for (TaggedToken<TokeniserDecision> decisionTag : this) {
				currentDecisions.add(decisionTag);
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
							lastNewToken.setAtomicParts(currentDecisions);
							currentDecisions = new ArrayList<TaggedToken<TokeniserDecision>>();
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
				lastNewToken.setAtomicParts(currentDecisions);
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

	/* (non-Javadoc)
	 * @see com.joliciel.talismane.tokeniser.TokeniserDecisionTagSequence#getCurrentPatternEnd()
	 */
	@Override
	public int getCurrentPatternEnd() {
		return currentPatternEnd;
	}

	/* (non-Javadoc)
	 * @see com.joliciel.talismane.tokeniser.TokeniserDecisionTagSequence#setCurrentPatternEnd(int)
	 */
	@Override
	public void setCurrentPatternEnd(int currentPatternEnd) {
		this.currentPatternEnd = currentPatternEnd;
	}

	@Override
	public void addDecision(double probability) {
		this.decisionProbabilities.add(probability);
		this.decisionProbabilityLogs.add(Math.log(probability));
	}

	@Override
	public List<Double> getDecisionProbabilities() {
		return this.decisionProbabilities;
	}

	@Override
	public List<Double> getDecisionProbabilityLogs() {
		return this.decisionProbabilityLogs;
	}
	
	
	
}
