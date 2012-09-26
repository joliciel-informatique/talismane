package com.joliciel.talismane.posTagger;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.HarmonicMeanScoringStrategy;
import com.joliciel.talismane.machineLearning.ScoringStrategy;
import com.joliciel.talismane.machineLearning.Solution;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;

public class PosTagSequenceImpl extends ArrayList<PosTaggedToken> implements PosTagSequence {
	private static final Log LOG = LogFactory.getLog(PosTagSequenceImpl.class);
	private static final long serialVersionUID = 5038343676751568000L;
	private TokenSequence tokenSequence;
	private double score = 0.0;
	private boolean scoreCalculated = false;
	private String string = null;
	private List<Decision<PosTag>> decisions = new ArrayList<Decision<PosTag>>();
	private List<Solution<?>> underlyingSolutions = new ArrayList<Solution<?>>();
	private ScoringStrategy scoringStrategy = new HarmonicMeanScoringStrategy();
	
	private PosTaggerServiceInternal posTaggerServiceInternal;
	
	PosTagSequenceImpl(TokenSequence tokenSequence) {
		super();
		this.setTokenSequence(tokenSequence);
	}

	PosTagSequenceImpl(TokenSequence tokenSequence, int initialCapacity) {
		super(initialCapacity);
		this.setTokenSequence(tokenSequence);
	}

	PosTagSequenceImpl(PosTagSequence history) {
		super(history.size()+1);
		for (PosTaggedToken posTaggedToken : history)
			this.addPosTaggedToken(posTaggedToken);
		this.decisions = new ArrayList<Decision<PosTag>>(history.getDecisions());
		this.setTokenSequence(history.getTokenSequence());
	}

	@Override
	public double getScore() {
		if (!scoreCalculated) {
			score = this.scoringStrategy.calculateScore(this);
			scoreCalculated = true;
		}
		return score;
	}

	public TokenSequence getTokenSequence() {
		return tokenSequence;
	}

	@Override
	public Token getNextToken() {
		return this.tokenSequence.get(this.size());
	}
	
	@Override
	public int compareTo(PosTagSequence o) {
		if (this.getScore()<o.getScore()) {
			return 1;
		} else if (this.getScore()>o.getScore()) {
			return -1;
		} else {
			return 0;
		}
	}
	

	@Override
	public synchronized String toString() {
		if (string==null) {
			StringBuilder builder = new StringBuilder();
			builder.append("Sequence: " );
			for (PosTaggedToken taggedToken : this) {
				builder.append(taggedToken.toString());
				builder.append(", ");
			}
			string = builder.toString();
		}
		return string;
	}

	@Override
	public PosTaggedToken prependRoot() {
		PosTaggedToken rootToken = null;
		if (this.size()>0) {
			rootToken = this.get(0);
			if (!rootToken.getTag().equals(PosTag.ROOT_POS_TAG))
				rootToken = null;
		}
		if (rootToken==null) {
			Token emptyToken = tokenSequence.addEmptyToken(0);
			emptyToken.setText("[ROOT]");
			tokenSequence.finalise();
			PosTagSet posTagSet = TalismaneSession.getPosTagSet();
			Decision<PosTag> rootDecision = posTagSet.createDefaultDecision(PosTag.ROOT_POS_TAG);
			rootToken = this.posTaggerServiceInternal.getPosTaggedToken(emptyToken, rootDecision);
			this.add(0, rootToken);
		}
		return rootToken;
	}

	public PosTaggerServiceInternal getPosTaggerServiceInternal() {
		return posTaggerServiceInternal;
	}

	public void setPosTaggerServiceInternal(
			PosTaggerServiceInternal posTaggerServiceInternal) {
		this.posTaggerServiceInternal = posTaggerServiceInternal;
	}

	@Override
	public List<Decision<PosTag>> getDecisions() {
		return this.decisions;
	}

	@Override
	public List<Solution<?>> getUnderlyingSolutions() {
		return this.underlyingSolutions;
	}

	@Override
	public void addDecision(Decision<PosTag> decision) {
		this.decisions.add(decision);
	}

	public ScoringStrategy getScoringStrategy() {
		return scoringStrategy;
	}

	public void setScoringStrategy(ScoringStrategy scoringStrategy) {
		this.scoringStrategy = scoringStrategy;
	}

	void setTokenSequence(TokenSequence tokenSequence) {
		this.tokenSequence = tokenSequence;
		if (tokenSequence.getUnderlyingAtomicTokenSequence()!=null) {
			this.underlyingSolutions.add(tokenSequence.getUnderlyingAtomicTokenSequence());
		}
	}

	@Override
	public void addPosTaggedToken(PosTaggedToken posTaggedToken) {
		this.add(posTaggedToken);
	}

	@Override
	public void removeEmptyPosTaggedTokens() {
		List<PosTaggedToken> nullTokensToRemove = new ArrayList<PosTaggedToken>();
		List<Token> emptyTokensToRemove = new ArrayList<Token>();
		for (PosTaggedToken posTaggedToken : this) {
			if (posTaggedToken.getToken().isEmpty() && posTaggedToken.getTag().isEmpty()) {
				nullTokensToRemove.add(posTaggedToken);
				emptyTokensToRemove.add(posTaggedToken.getToken());
				if (LOG.isDebugEnabled())
					LOG.debug("Removing null empty token at position " + posTaggedToken.getToken().getStartIndex() + ": " + posTaggedToken);
			}
		}
		this.removeAll(nullTokensToRemove);
		
		if (emptyTokensToRemove.size()>0) {
			// first clone the token sequence, so we don't mess with token sequences shared with other pos-tag sequences
			this.tokenSequence = this.tokenSequence.cloneTokenSequence();
			for (Token emptyToken : emptyTokensToRemove)
				this.tokenSequence.removeEmptyToken(emptyToken);
			
			this.tokenSequence.finalise();
		}
	}
	
	
}
