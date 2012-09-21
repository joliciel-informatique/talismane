package com.joliciel.talismane.tokeniser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.TalismaneException;

abstract class AbstractTokenSequence extends ArrayList<Token>  implements TokenSequence, PretokenisedSequence {
	private static final Log LOG = LogFactory.getLog(AbstractTokenSequence.class);
	private static final long serialVersionUID = 2675309892340757939L;
	private List<Integer> tokenSplits;
	private String sentence;
	private List<Token> listWithWhiteSpace = new ArrayList<Token>();
	private List<Token> tokensAdded = null;
	private double score = 0;
	private boolean scoreCalculated = false;
	TokenisedAtomicTokenSequence underlyingAtomicTokenSequence;
	private Integer unitTokenCount = null;
	private boolean finalised = false;
	
	private TokeniserServiceInternal tokeniserServiceInternal;

	public AbstractTokenSequence() {}
	public AbstractTokenSequence(String sentence) {
		this.sentence = sentence;
	}
	
	@Override
	public Token get(int index) {
		this.finalise();
		return super.get(index);
	}

	@Override
	public Iterator<Token> iterator() {
		this.finalise();
		return super.iterator();
	}

	@Override
	public ListIterator<Token> listIterator() {
		this.finalise();
		return super.listIterator();
	}

	@Override
	public ListIterator<Token> listIterator(int index) {
		this.finalise();
		return super.listIterator(index);
	}

	@Override
	public List<Token> subList(int fromIndex, int toIndex) {
		this.finalise();
		return super.subList(fromIndex, toIndex);
	}

	@Override
	public List<Integer> getTokenSplits() {
		if (this.sentence==null) {
			throw new TalismaneException("Cannot get token splits if no sentence has been set.");
		}
		if (this.tokenSplits==null) {
			this.tokenSplits = new ArrayList<Integer>();
			this.tokenSplits.add(0);
			for (Token token : this) {
				if (!this.tokenSplits.contains(token.getStartIndex()))
					this.tokenSplits.add(token.getStartIndex());
				this.tokenSplits.add(token.getEndIndex());
			}
		}
		return tokenSplits;
	}

	public String getSentence() {
		return sentence;
	}

	public void setSentence(String sentence) {
		this.sentence = sentence;
	}
	@Override
	public List<Token> listWithWhiteSpace() {
		return listWithWhiteSpace;
	}

	@Override
	public void finalise() {
		if (!finalised) {
			finalised = true;
			int i = 0;
			for (Token token : this.listWithWhiteSpace) {
				token.setIndexWithWhiteSpace(i++);
			}
			i = 0;
			for (Token token : this) {
				token.setIndex(i++);
			}
		}
	}
	
	void markModified() {
		this.finalised = false;
		this.tokenSplits = null;
	}
	
	public double getScore() {
		if (!this.scoreCalculated) {
			this.finalise();
			if (LOG.isTraceEnabled())
				LOG.trace(this);
			score = 1.0;
			if (this.underlyingAtomicTokenSequence!=null) {
				score = this.underlyingAtomicTokenSequence.getScore();
			}
			
			this.scoreCalculated = true;
		}
		return score;
	}

	public TokenisedAtomicTokenSequence getUnderlyingAtomicTokenSequence() {
		return underlyingAtomicTokenSequence;
	}

	@Override
	public int getUnitTokenCount() {
		if (unitTokenCount==null) {
			unitTokenCount = 0;
			for (Token token : this) {
				if (token.getAtomicParts().size()==0)
					unitTokenCount += 1;
				else
					unitTokenCount += token.getAtomicParts().size();
			}
		}
		return unitTokenCount;
	}

	@Override
	public Token addToken(int start, int end) {
		return this.addToken(start, end, false);
	}

	Token addToken(int start, int end, boolean allowEmpty) {
		if (this.sentence==null) {
			throw new RuntimeException("Cannot add a token by index if no sentence has been set.");
		}
		if (!allowEmpty && start==end)
			return null;
		
		String string = this.sentence.substring(start, end);
		
		List<Token> tokensToRemove = new ArrayList<Token>();
		
		int prevTokenIndex = -1;
		int i = 0;
		for (Token aToken : this.listWithWhiteSpace) {
			if (aToken.getStartIndex()==start&&aToken.getEndIndex()==end) {
				return aToken;
			} else if (aToken.getStartIndex()<end && aToken.getEndIndex()>start) {
				tokensToRemove.add(aToken);
			} else if (aToken.getEndIndex()<=start) {
				prevTokenIndex = i;
			}
			i++;
		}
		
		for (Token tokenToRemove : tokensToRemove) {
			this.listWithWhiteSpace.remove(tokenToRemove);
			this.remove(tokenToRemove);
		}
		
		TokenInternal token = this.getTokeniserServiceInternal().getTokenInternal(string, this, this.size());
		token.setStartIndex(start);
		token.setEndIndex(end);
		token.setIndexWithWhiteSpace(prevTokenIndex+1);

		this.listWithWhiteSpace.add(prevTokenIndex+1, token);
		
		if (!token.isWhiteSpace()) {
			prevTokenIndex = -1;
			i = 0;
			for (Token aToken : this) {
				if (aToken.getEndIndex()<=start) {
					prevTokenIndex = i;
				}
				i++;
			}

			super.add(prevTokenIndex+1, token);
			if (tokensAdded!=null)
				tokensAdded.add(token);
			token.setIndex(prevTokenIndex+1);
		}
		
		this.markModified();

		return token;
	}

	@Override
	public Token addToken(String string) {
		TokenInternal token = this.getTokeniserServiceInternal().getTokenInternal(string, this, this.size());
		token.setStartIndex(this.getSentence().length());
		token.setEndIndex(this.getSentence().length() + string.length());
		this.setSentence(this.getSentence() + string);

		this.listWithWhiteSpace().add(token);
		
		if (!token.isWhiteSpace()) {
			super.add(token);
			if (tokensAdded!=null)
				tokensAdded.add(token);
		}
		
		this.markModified();
		return token;
	}
	
	public TokeniserServiceInternal getTokeniserServiceInternal() {
		return tokeniserServiceInternal;
	}
	public void setTokeniserServiceInternal(
			TokeniserServiceInternal tokeniserServiceInternal) {
		this.tokeniserServiceInternal = tokeniserServiceInternal;
	}

	@Override
	public Token addEmptyToken(int position) {
		return this.addToken(position, position, true);
	}
	@Override
	public void cleanSlate() {
		this.tokensAdded = new ArrayList<Token>();
	}
	@Override
	public List<Token> getTokensAdded() {
		return this.tokensAdded;
	}
	
	
}
