package com.joliciel.talismane.posTagger;

import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;

public class PosTagSequenceImpl extends ArrayList<PosTaggedToken> implements PosTagSequence {
	private static final Log LOG = LogFactory.getLog(PosTagSequenceImpl.class);
	private static final long serialVersionUID = 5038343676751568000L;
	private TokenSequence tokenSequence;
	private double score = 0.0;
	private boolean scoreCalculated = false;
	private String string = null;
	
	private PosTaggerServiceInternal posTaggerServiceInternal;
	
	PosTagSequenceImpl(TokenSequence tokenSequence) {
		super();
		this.tokenSequence = tokenSequence;
	}

	PosTagSequenceImpl(TokenSequence tokenSequence, int initialCapacity) {
		super(initialCapacity);
		this.tokenSequence = tokenSequence;
	}

	PosTagSequenceImpl(PosTagSequence history) {
		super(history.size()+1);
		this.addAll(history);
		this.tokenSequence = history.getTokenSequence();
	}

	@Override
	public double getScore() {
		if (!scoreCalculated) {
			score = 0;
			if (this.size()>0) {
				for (PosTaggedToken token : this) {
					score += token.getProbLog();
				}
	
				score = score / this.size();
			}
			
			score = Math.exp(score);
			
			score *= this.getTokenSequence().getScore();
			
			scoreCalculated = true;
			
			if (LOG.isTraceEnabled()) {
				LOG.trace(this);
				StringBuilder sb = new StringBuilder();
				sb.append(" * ");
				for (PosTaggedToken posTaggedToken : this) {
					sb.append(" * ");
					sb.append(posTaggedToken.getProbability());
				}
				sb.append(" root ");
				sb.append(this.size());
				sb.append(" * tokenSeqScore ");
				sb.append(this.getTokenSequence().getScore());
				sb.append(" = ");
				sb.append(score);
				LOG.trace(sb.toString());
			}
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
			rootToken = this.posTaggerServiceInternal.getPosTaggedToken(emptyToken, PosTag.ROOT_POS_TAG, 1.0);
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
	
	
}
