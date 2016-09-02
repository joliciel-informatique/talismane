///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2014 Joliciel Informatique
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.filters.Sentence;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.GeometricMeanScoringStrategy;
import com.joliciel.talismane.machineLearning.ScoringStrategy;
import com.joliciel.talismane.machineLearning.Solution;

class TokenisedAtomicTokenSequenceImpl extends TaggedTokenSequenceImpl<TokeniserOutcome> implements TokenisedAtomicTokenSequence {
	private static final long serialVersionUID = -5837144642078063115L;
	private final Sentence sentence;
	private TokeniserServiceInternal tokeniserServiceInternal;
	private TokenSequence tokenSequence = null;
	private List<Decision> decisions = new ArrayList<Decision>();
	private List<Solution> underlyingSolutions = new ArrayList<Solution>();
	@SuppressWarnings("rawtypes")
	private ScoringStrategy scoringStrategy = new GeometricMeanScoringStrategy();
	double score = 1.0;
	boolean scoreCalculated = false;

	private final TalismaneSession talismaneSession;

	TokenisedAtomicTokenSequenceImpl(Sentence sentence, TalismaneSession talismaneSession) {
		this.talismaneSession = talismaneSession;
		this.sentence = sentence;
	}

	TokenisedAtomicTokenSequenceImpl(Sentence sentence, int initialCapacity, TalismaneSession talismaneSession) {
		super(initialCapacity);
		this.sentence = sentence;
		this.talismaneSession = talismaneSession;
	}

	TokenisedAtomicTokenSequenceImpl(TokenisedAtomicTokenSequenceImpl history) {
		super(history);
		this.decisions = new ArrayList<Decision>(history.getDecisions());
		this.sentence = history.getSentence();
		this.talismaneSession = history.talismaneSession;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.joliciel.talismane.tokeniser.TokeniserDecisionTagSequence#
	 * getTokenSequence()
	 */
	@Override
	public TokenSequence inferTokenSequence() {
		if (tokenSequence == null) {
			Map<Integer, TaggedToken<TokeniserOutcome>> indexTokenMap = new HashMap<Integer, TaggedToken<TokeniserOutcome>>();

			tokenSequence = new TokenSequence(sentence, this, talismaneSession);

			int currentStart = 0;
			int currentEnd = 0;
			StringBuilder currentText = new StringBuilder();
			List<TaggedToken<TokeniserOutcome>> currentAtomicParts = new ArrayList<TaggedToken<TokeniserOutcome>>();
			boolean isWhiteSpace = true;
			for (TaggedToken<TokeniserOutcome> decisionTag : this) {
				currentAtomicParts.add(decisionTag);
				indexTokenMap.put(decisionTag.getToken().getStartIndex(), decisionTag);
				Token token = decisionTag.getToken();

				if (decisionTag.getTag().equals(TokeniserOutcome.SEPARATE)) {
					// make separation (add token)
					if (!isWhiteSpace) {
						this.addToken(tokenSequence, currentStart, currentEnd, currentText, currentAtomicParts);
						currentAtomicParts = new ArrayList<TaggedToken<TokeniserOutcome>>();
					} else {
						this.addToken(tokenSequence, currentStart, currentEnd, currentText, null);
					}
					currentText = new StringBuilder();
					currentText.append(token.getText());
					currentStart = token.getStartIndex();
					isWhiteSpace = true;
				} else {
					currentText.append(token.getText());
				}
				isWhiteSpace = isWhiteSpace && token.isWhiteSpace();
				currentEnd = token.getEndIndex();
			}
			if (!isWhiteSpace) {
				// TODO: S.MICHEL: current atomic parts contains "S.MICHEL" for
				// "S.", " " for "MICHEL" -
				// therefore we lose all attributes on MICHEL!
				this.addToken(tokenSequence, currentStart, currentEnd, currentText, currentAtomicParts);
			} else {
				this.addToken(tokenSequence, currentStart, currentEnd, currentText, null);
			}

			tokenSequence.finalise();

			// Assign any token attributes that existed on the corresponding
			// atomic tokens.
			for (Token token : tokenSequence) {
				TaggedToken<TokeniserOutcome> decisionTag = indexTokenMap.get(token.getStartIndex());
				if (decisionTag != null) {
					Token atomicToken = decisionTag.getToken();
					if (atomicToken.getStartIndex() == token.getStartIndex() && atomicToken.getEndIndex() == token.getEndIndex()) {
						for (String key : atomicToken.getAttributes().keySet()) {
							token.addAttribute(key, atomicToken.getAttributes().get(key));
						}
					}
				}
			}
		}
		return tokenSequence;
	}

	private Token addToken(TokenSequence tokenSequence, int start, int end, StringBuilder currentText, List<TaggedToken<TokeniserOutcome>> currentAtomicParts) {
		Token token = null;
		if (start == end) {
			// do nothing
		} else {
			token = tokenSequence.addToken(start, end);
			token.setText(currentText.toString());
			token.setAtomicParts(currentAtomicParts);
		}

		return token;
	}

	public TokeniserServiceInternal getTokeniserServiceInternal() {
		return tokeniserServiceInternal;
	}

	public void setTokeniserServiceInternal(TokeniserServiceInternal tokeniserServiceInternal) {
		this.tokeniserServiceInternal = tokeniserServiceInternal;
	}

	@Override
	public List<Decision> getDecisions() {
		return decisions;
	}

	@Override
	public List<Solution> getUnderlyingSolutions() {
		return underlyingSolutions;
	}

	@Override
	public void addDecision(Decision decision) {
		this.decisions.add(decision);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public ScoringStrategy getScoringStrategy() {
		return scoringStrategy;
	}

	@Override
	public void setScoringStrategy(@SuppressWarnings("rawtypes") ScoringStrategy scoringStrategy) {
		this.scoringStrategy = scoringStrategy;
	}

	@SuppressWarnings("unchecked")
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
		if (this.getScore() < o.getScore()) {
			return 1;
		} else if (this.getScore() > o.getScore()) {
			return -1;
		} else {
			return 0;
		}
	}

	@Override
	public Sentence getSentence() {
		return sentence;
	}

	public TokenisedAtomicTokenSequence cloneSequence() {
		TokenisedAtomicTokenSequenceImpl sequence = new TokenisedAtomicTokenSequenceImpl(this);
		return sequence;
	}

}
