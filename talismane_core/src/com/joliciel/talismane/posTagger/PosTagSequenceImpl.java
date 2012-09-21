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
	@SuppressWarnings("unused")
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
		this.addAll(history);
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
	
	
}
