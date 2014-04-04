package com.joliciel.talismane.tokeniser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.LinguisticRules;
import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.filters.Sentence;
import com.joliciel.talismane.posTagger.PosTagSequence;

abstract class AbstractTokenSequence extends ArrayList<Token>  implements TokenSequence, PretokenisedSequence {
	private static final Log LOG = LogFactory.getLog(AbstractTokenSequence.class);
	private static final long serialVersionUID = 2675309892340757939L;
	private List<Integer> tokenSplits;
	private String text;
	private Sentence sentence;
	private List<Token> listWithWhiteSpace = new ArrayList<Token>();
	private List<Token> tokensAdded = null;
	private double score = 0;
	private boolean scoreCalculated = false;
	TokenisedAtomicTokenSequence underlyingAtomicTokenSequence;
	private Integer atomicTokenCount = null;
	private boolean finalised = false;
	private boolean withRoot = false;
	private PosTagSequence posTagSequence;
	protected boolean textProvided = false;
	
	private TokeniserServiceInternal tokeniserServiceInternal;

	public AbstractTokenSequence() {}
	public AbstractTokenSequence(Sentence sentence) {
		this.sentence = sentence;
		this.text = sentence.getText();
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
		if (this.text==null) {
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

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
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
		this.atomicTokenCount = null;
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
	public int getAtomicTokenCount() {
		if (atomicTokenCount==null) {
			atomicTokenCount = 0;
			for (Token token : this) {
				if (token.getAtomicParts().size()==0)
					atomicTokenCount += 1;
				else
					atomicTokenCount += token.getAtomicParts().size();
			}
		}
		return atomicTokenCount;
	}

	@Override
	public Token addToken(int start, int end) {
		return this.addToken(start, end, false);
	}

	Token addToken(int start, int end, boolean allowEmpty) {
		if (this.text==null) {
			throw new RuntimeException("Cannot add a token by index if no sentence has been set.");
		}
		if (!allowEmpty && start==end)
			return null;
		
		String string = this.text.substring(start, end);
		
		List<Token> tokensToRemove = new ArrayList<Token>();
		
		int prevTokenIndex = -1;
		int i = 0;
		for (Token aToken : this.listWithWhiteSpace) {
			if (aToken.getStartIndex()>end) {
				break;
			} else if (aToken.getStartIndex()==start&&aToken.getEndIndex()==end) {
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
				if (aToken.getStartIndex()>end) {
					break;
				} else if (aToken.getEndIndex()<=start) {
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
	public Token addToken(String tokenText) {
		Token token = null;
		
		if (this.size()==0) {
			// do nothing
		} else if (!textProvided) {
			// check if a space should be added before this token
			String lastTokenText = this.get(this.size()-1).getText();
			LinguisticRules rules = TalismaneSession.getLinguisticRules();
			if (rules==null)
				throw new TalismaneException("Linguistic rules have not been set.");
			
			if (rules.shouldAddSpace(lastTokenText, tokenText))
				this.addTokenInternal(" ");
			
		}
		token = this.addTokenInternal(tokenText);

		return token;
	}

	private Token addTokenInternal(String string) {
		// note, this is only called for PretokenisedSequence, where the sentence has to be reconstructed
		// from an annotated corpus
		if (this.text==null)
			this.text = "";
		
		TokenInternal token = this.getTokeniserServiceInternal().getTokenInternal(string, this, this.size());
		
		if (this.textProvided) {
			int currentIndex = 0;
			if (this.size()>0)
				currentIndex = this.get(this.size()-1).getEndIndex();
			int j=0;
			TokenInternal whiteSpace = null;
			for (int i=currentIndex; i<this.text.length(); i++) {
				char c = text.charAt(i);
				if (Character.isWhitespace(c)) {
					whiteSpace = this.getTokeniserServiceInternal().getTokenInternal(" ", this, this.size());
					whiteSpace.setStartIndex(currentIndex+j);
					whiteSpace.setEndIndex(whiteSpace.getStartIndex()+1);
					this.listWithWhiteSpace().add(whiteSpace);
				} else {
					break;
				}
				j++;
			}
			if (whiteSpace!=null) {
				token.setStartIndex(whiteSpace.getEndIndex());
				token.setEndIndex(whiteSpace.getEndIndex() + string.length());
			} else {
				token.setStartIndex(currentIndex);
				token.setEndIndex(currentIndex + string.length());
			}
			if (!text.substring(token.getStartIndex(), token.getEndIndex()).equals(string)) {
				throw new TalismaneException("Add token failed: Expected '" + string + "' but was '" + text.substring(token.getStartIndex(), token.getEndIndex()) + "' in sentence: " + text);
			}
			
		} else {
			token.setStartIndex(this.getText().length());
			token.setEndIndex(this.getText().length() + string.length());
			this.setText(this.getText() + string);
			this.getSentence().setText(this.getText());
		}

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
	public void removeEmptyToken(Token emptyToken) {
		if (!emptyToken.isEmpty()) {
			throw new TalismaneException("Can only remove empty tokens from token sequence.");
		}
		if (LOG.isTraceEnabled())
			LOG.trace("Removing empty token at position " + emptyToken.getStartIndex() + ": " + emptyToken);
		this.remove(emptyToken);
		this.listWithWhiteSpace.remove(emptyToken);
		this.markModified();
	}
	
	@Override
	public void cleanSlate() {
		this.tokensAdded = new ArrayList<Token>();
	}
	@Override
	public List<Token> getTokensAdded() {
		return this.tokensAdded;
	}
	private List<Token> getListWithWhiteSpace() {
		return listWithWhiteSpace;
	}
	private void setListWithWhiteSpace(List<Token> listWithWhiteSpace) {
		this.listWithWhiteSpace = listWithWhiteSpace;
	}
	private void setScore(double score) {
		this.score = score;
	}
	private void setScoreCalculated(boolean scoreCalculated) {
		this.scoreCalculated = scoreCalculated;
	}
	private void setUnderlyingAtomicTokenSequence(
			TokenisedAtomicTokenSequence underlyingAtomicTokenSequence) {
		this.underlyingAtomicTokenSequence = underlyingAtomicTokenSequence;
	}
	private void setAtomicTokenCount(Integer unitTokenCount) {
		this.atomicTokenCount = unitTokenCount;
	}
	
	public void cloneTokenSequence(AbstractTokenSequence tokenSequence) {
		tokenSequence.setText(this.getText());
		tokenSequence.setSentence(this.getSentence());
		tokenSequence.setTokeniserServiceInternal(this.getTokeniserServiceInternal());
		List<Token> listWithWhiteSpace = new ArrayList<Token>(this.getListWithWhiteSpace());
		tokenSequence.setListWithWhiteSpace(listWithWhiteSpace);
		tokenSequence.setScore(this.getScore());
		tokenSequence.setScoreCalculated(true);
		tokenSequence.setUnderlyingAtomicTokenSequence(this.getUnderlyingAtomicTokenSequence());
		tokenSequence.setAtomicTokenCount(this.getAtomicTokenCount());
		tokenSequence.setPosTagSequence(this.getPosTagSequence());
		
		for (Token token : this) {
			Token clone = token.cloneToken();
			tokenSequence.add(clone);
			clone.setTokenSequence(tokenSequence);
		}
	}
	
	public Sentence getSentence() {
		return sentence;
	}
	public void setSentence(Sentence sentence) {
		this.sentence = sentence;
	}
	public boolean isWithRoot() {
		return withRoot;
	}
	public void setWithRoot(boolean withRoot) {
		this.withRoot = withRoot;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Token token : this.listWithWhiteSpace) {
			sb.append('|');
			sb.append(token.getOriginalText());
		}
		sb.append('|');
		return sb.toString();
	}
	public PosTagSequence getPosTagSequence() {
		return posTagSequence;
	}
	public void setPosTagSequence(PosTagSequence posTagSequence) {
		this.posTagSequence = posTagSequence;
	}
	
	
}
