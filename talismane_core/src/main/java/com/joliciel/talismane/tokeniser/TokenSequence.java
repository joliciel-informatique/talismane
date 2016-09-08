package com.joliciel.talismane.tokeniser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.filters.Sentence;
import com.joliciel.talismane.filters.SentenceTag;
import com.joliciel.talismane.lexicon.PosTaggerLexicon;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.tokeniser.filters.TokenPlaceholder;

/**
 * A sequence of tokens. Note: by default, List iteration and associated methods
 * will only return non-whitespace tokens. For a list that includes whitespace
 * tokens, use the listWithWhiteSpace() method.
 * 
 * @author Assaf Urieli
 *
 */
public class TokenSequence extends ArrayList<Token> implements Serializable {
	private static final Logger LOG = LoggerFactory.getLogger(TokenSequence.class);
	private static final long serialVersionUID = 1L;

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
	private boolean sentenceTagsAdded = false;
	private boolean withRoot = false;
	private PosTagSequence posTagSequence;
	protected boolean textProvided = false;

	private PosTaggerLexicon lexicon;

	private final TalismaneSession talismaneSession;

	/**
	 * Create a token sequence for a given sentence.
	 */
	public TokenSequence(Sentence sentence, TalismaneSession talismaneSession) {
		this.sentence = sentence;
		this.talismaneSession = talismaneSession;
		this.text = sentence.getText();
	}

	public TokenSequence(TokenSequence sequenceToClone) {
		this.talismaneSession = sequenceToClone.talismaneSession;
		this.setText(sequenceToClone.getText());
		this.setSentence(sequenceToClone.getSentence());
		List<Token> listWithWhiteSpace = new ArrayList<Token>(sequenceToClone.getListWithWhiteSpace());
		this.setListWithWhiteSpace(listWithWhiteSpace);
		this.setScore(sequenceToClone.getScore());
		this.setScoreCalculated(true);
		this.setUnderlyingAtomicTokenSequence(sequenceToClone.getUnderlyingAtomicTokenSequence());
		this.setAtomicTokenCount(sequenceToClone.getAtomicTokenCount());
		this.setPosTagSequence(sequenceToClone.getPosTagSequence());

		for (Token token : sequenceToClone) {
			Token clone = token.cloneToken();
			this.add(clone);
			clone.setTokenSequence(this);
		}
	}

	/**
	 * Create a token sequence from a given sentence, pre-separated into tokens
	 * matching the separatorPattern.
	 */
	public TokenSequence(Sentence sentence, Pattern separatorPattern, TalismaneSession talismaneSession) {
		this(sentence, separatorPattern, null, talismaneSession);
	}

	/**
	 * Create a token sequence from a given sentence, pre-separated into tokens
	 * matching the separatorPattern, except for the placeholders provided.
	 */
	public TokenSequence(Sentence sentence, Pattern separatorPattern, List<TokenPlaceholder> placeholders, TalismaneSession talismaneSession) {
		this(sentence, talismaneSession);
		this.initialise(sentence.getText(), separatorPattern, placeholders);
	}

	public TokenSequence(Sentence sentence, TokenisedAtomicTokenSequence tokenisedAtomicTokenSequence, TalismaneSession talismaneSession) {
		this(sentence, talismaneSession);
		this.underlyingAtomicTokenSequence = tokenisedAtomicTokenSequence;
	}

	private void initialise(String text, Pattern separatorPattern, List<TokenPlaceholder> placeholders) {
		Matcher matcher = separatorPattern.matcher(text);
		Set<Integer> separatorMatches = new HashSet<Integer>();
		while (matcher.find())
			separatorMatches.add(matcher.start());

		Map<Integer, TokenPlaceholder> placeholderMap = new HashMap<Integer, TokenPlaceholder>();
		List<TokenPlaceholder> attributePlaceholders = new ArrayList<TokenPlaceholder>();
		if (placeholders != null) {
			for (TokenPlaceholder placeholder : placeholders) {
				if (placeholder.isSingleToken()) {
					// take the first placeholder at this start index only
					// thus declaration order is the order at which they're
					// applied
					if (!placeholderMap.containsKey(placeholder.getStartIndex()))
						placeholderMap.put(placeholder.getStartIndex(), placeholder);
				} else {
					attributePlaceholders.add(placeholder);
				}
			}
		}

		int currentPos = 0;
		for (int i = 0; i < text.length(); i++) {
			if (placeholderMap.containsKey(i)) {
				if (i > currentPos)
					this.addToken(currentPos, i);
				TokenPlaceholder placeholder = placeholderMap.get(i);
				Token token = this.addToken(placeholder.getStartIndex(), placeholder.getEndIndex());
				if (placeholder.getReplacement() != null)
					token.setText(placeholder.getReplacement());

				for (String key : placeholder.getAttributes().keySet())
					token.addAttribute(key, placeholder.getAttributes().get(key));

				if (separatorPattern.matcher(token.getText()).matches())
					token.setSeparator(true);

				// skip until after the placeholder
				i = placeholder.getEndIndex() - 1;
				currentPos = placeholder.getEndIndex();
			} else if (separatorMatches.contains(i)) {
				if (i > currentPos)
					this.addToken(currentPos, i);
				Token separator = this.addToken(i, i + 1);
				separator.setSeparator(true);
				currentPos = i + 1;
			}
		}

		if (currentPos < text.length())
			this.addToken(currentPos, text.length());

		// select order in which to add attributes, when attributes exist for
		// identical keys
		Map<String, NavigableSet<TokenPlaceholder>> attributeOrderingMap = new HashMap<>();
		for (TokenPlaceholder placeholder : attributePlaceholders) {
			for (String key : placeholder.getAttributes().keySet()) {
				NavigableSet<TokenPlaceholder> placeholderSet = attributeOrderingMap.get(key);
				if (placeholderSet == null) {
					placeholderSet = new TreeSet<>();
					attributeOrderingMap.put(key, placeholderSet);
					placeholderSet.add(placeholder);
				} else {
					// only add one placeholder per location for each attribute
					// key
					TokenPlaceholder floor = placeholderSet.floor(placeholder);
					if (floor == null || floor.compareTo(placeholder) < 0) {
						placeholderSet.add(placeholder);
					}
				}
			}
		}

		// add any attributes provided by attribute placeholders
		for (String key : attributeOrderingMap.keySet()) {
			for (TokenPlaceholder placeholder : attributeOrderingMap.get(key)) {
				for (Token token : this) {
					if (token.getStartIndex() < placeholder.getStartIndex()) {
						continue;
					} else if (token.getEndIndex() <= placeholder.getEndIndex()) {
						if (token.getAttributes().containsKey(key)) {
							// this token already contains the key in question,
							// only possible when two placeholders overlap
							// in this case, the second placeholder is
							// completely skipped
							// so we break out of the token loop to avoid
							// assigning the remaining tokens
							break;
						}
						token.addAttribute(key, placeholder.getAttributes().get(key));
					} else {
						// token.getEndIndex() > placeholder.getEndIndex()
						break;
					}
				}
			}
		}

		this.finalise();
	}

	/**
	 * Get the nth token in this sequence.
	 */
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

	/**
	 * Returns the token splits represented by this token sequence, where each
	 * integer represents the symbol immediately following a token split. Only
	 * available if this TokenSequence is associated with a sentence, e.g.
	 * TokenSequence.getSentence()!=null.
	 */

	public List<Integer> getTokenSplits() {
		if (this.text == null) {
			throw new TalismaneException("Cannot get token splits if no sentence has been set.");
		}
		if (this.tokenSplits == null) {
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

	/**
	 * The sentence text on which this token sequence was built.
	 */

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	/**
	 * A list of tokens that includes white space tokens.
	 */

	public List<Token> listWithWhiteSpace() {
		return listWithWhiteSpace;
	}

	/**
	 * Finalise a reconstructed token sequence so that all of the indexes are
	 * correct on the component tokens.
	 */

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

	void addSentenceTags() {
		if (!sentenceTagsAdded) {
			sentenceTagsAdded = true;
			for (SentenceTag<?> sentenceTag : this.sentence.getSentenceTags()) {
				for (Token token : this) {
					if (token.getStartIndex() >= sentenceTag.getStartIndex() && token.getEndIndex() <= sentenceTag.getEndIndex()) {
						token.addAttribute(sentenceTag.getAttribute(), sentenceTag.getValue());
					}
					if (token.getStartIndex() > sentenceTag.getEndIndex())
						break;
				}
			}

		}
	}

	void markModified() {
		this.finalised = false;
		this.tokenSplits = null;
		this.atomicTokenCount = null;
	}

	/**
	 * The geometric mean of the tokeniser decisions. Note that only actual
	 * decisions made by a decision maker will be taken into account - any
	 * default decisions will not be included.
	 */

	public double getScore() {
		if (!this.scoreCalculated) {
			this.finalise();
			if (LOG.isTraceEnabled())
				LOG.trace(this.toString());
			score = 1.0;
			if (this.underlyingAtomicTokenSequence != null) {
				score = this.underlyingAtomicTokenSequence.getScore();
			}

			this.scoreCalculated = true;
		}
		return score;
	}

	/**
	 * Returns the tokenised atomic token sequence, generated by a tokeniser,
	 * from which this token sequence was initially inferred.
	 */

	public TokenisedAtomicTokenSequence getUnderlyingAtomicTokenSequence() {
		return underlyingAtomicTokenSequence;
	}

	/**
	 * The number of atomic tokens making up this token sequence (+1 for each
	 * empty token).
	 */

	public int getAtomicTokenCount() {
		if (atomicTokenCount == null) {
			atomicTokenCount = 0;
			for (Token token : this) {
				if (token.getAtomicParts().size() == 0)
					atomicTokenCount += 1;
				else
					atomicTokenCount += token.getAtomicParts().size();
			}
		}
		return atomicTokenCount;
	}

	/**
	 * Adds a token to the current sequence, using substring coordinates from
	 * the associated sentence. If a token already exists with this exact start
	 * and end, will not perform any action. Any other existing token whose
	 * start &lt; end and end &gt; start will be removed.
	 */

	public Token addToken(int start, int end) {
		return this.addToken(start, end, false);
	}

	Token addToken(int start, int end, boolean allowEmpty) {
		if (this.text == null) {
			throw new RuntimeException("Cannot add a token by index if no sentence has been set.");
		}
		if (!allowEmpty && start == end)
			return null;

		String string = this.text.substring(start, end);

		List<Token> tokensToRemove = new ArrayList<Token>();

		int prevTokenIndex = -1;
		int i = 0;
		for (Token aToken : this.listWithWhiteSpace) {
			if (aToken.getStartIndex() > end) {
				break;
			} else if (aToken.getStartIndex() == start && aToken.getEndIndex() == end) {
				return aToken;
			} else if (aToken.getStartIndex() < end && aToken.getEndIndex() > start) {
				tokensToRemove.add(aToken);
			} else if (aToken.getEndIndex() <= start) {
				prevTokenIndex = i;
			}
			i++;
		}

		for (Token tokenToRemove : tokensToRemove) {
			this.listWithWhiteSpace.remove(tokenToRemove);
			this.remove(tokenToRemove);
		}

		Token token = new Token(string, this, this.size(), this.getLexicon());
		token.setStartIndex(start);
		token.setEndIndex(end);
		token.setIndexWithWhiteSpace(prevTokenIndex + 1);

		this.listWithWhiteSpace.add(prevTokenIndex + 1, token);

		if (!token.isWhiteSpace()) {
			prevTokenIndex = -1;
			i = 0;
			for (Token aToken : this) {
				if (aToken.getStartIndex() > end) {
					break;
				} else if (aToken.getEndIndex() <= start) {
					prevTokenIndex = i;
				}
				i++;
			}

			super.add(prevTokenIndex + 1, token);
			if (tokensAdded != null)
				tokensAdded.add(token);
			token.setIndex(prevTokenIndex + 1);
		}

		this.markModified();

		return token;
	}

	protected Token addTokenInternal(String string) {
		// note, this is only called for PretokenisedSequence, where the
		// sentence has to be reconstructed
		// from an annotated corpus
		if (this.text == null)
			this.text = "";

		Token token = new Token(string, this, this.size(), this.getLexicon());

		if (this.textProvided) {
			int currentIndex = 0;
			if (this.size() > 0)
				currentIndex = this.get(this.size() - 1).getEndIndex();
			int j = 0;
			Token whiteSpace = null;
			for (int i = currentIndex; i < this.text.length(); i++) {
				char c = text.charAt(i);
				if (Character.isWhitespace(c)) {
					whiteSpace = new Token(" ", this, this.size(), this.getLexicon());
					whiteSpace.setStartIndex(currentIndex + j);
					whiteSpace.setEndIndex(whiteSpace.getStartIndex() + 1);
					this.listWithWhiteSpace().add(whiteSpace);
				} else {
					break;
				}
				j++;
			}
			if (whiteSpace != null) {
				token.setStartIndex(whiteSpace.getEndIndex());
				token.setEndIndex(whiteSpace.getEndIndex() + string.length());
			} else {
				token.setStartIndex(currentIndex);
				token.setEndIndex(currentIndex + string.length());
			}
			if (!text.substring(token.getStartIndex(), token.getEndIndex()).equals(string)) {
				throw new TalismaneException("Add token failed: Expected '" + string + "' but was '"
						+ text.substring(token.getStartIndex(), token.getEndIndex()) + "' in sentence: " + text);
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
			if (tokensAdded != null)
				tokensAdded.add(token);
		}

		this.markModified();
		return token;
	}

	/**
	 * Add an empty token at a certain position in the sentence.
	 */

	public Token addEmptyToken(int position) {
		return this.addToken(position, position, true);
	}

	/**
	 * Remove an empty token from this token sequence.
	 */

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

	/**
	 * Cleans out any collections of modifications, so that any modifications
	 * after this clean slate can be viewed.<br/>
	 * If run before applying filters, will enable the client code to detect any
	 * tokens added by the filters.
	 * 
	 * @see #getTokensAdded()
	 */

	public void cleanSlate() {
		this.tokensAdded = new ArrayList<Token>();
	}

	/**
	 * Returns the tokens added since the last clean slate.
	 * 
	 * @see #cleanSlate()
	 */

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

	private void setUnderlyingAtomicTokenSequence(TokenisedAtomicTokenSequence underlyingAtomicTokenSequence) {
		this.underlyingAtomicTokenSequence = underlyingAtomicTokenSequence;
	}

	private void setAtomicTokenCount(Integer unitTokenCount) {
		this.atomicTokenCount = unitTokenCount;
	}

	/**
	 * The sentence object on which this token sequence was built, allowing us
	 * to identify its location in the source text.
	 */

	public Sentence getSentence() {
		return sentence;
	}

	public void setSentence(Sentence sentence) {
		this.sentence = sentence;
	}

	/**
	 * Does this token sequence have an "artificial" root or not.
	 */

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

	/**
	 * A PosTagSequence enclosing this token sequence - only useful in tokeniser
	 * evaluation, when we want to know the pos-tags in the original annotated
	 * corpus.
	 */

	public PosTagSequence getPosTagSequence() {
		return posTagSequence;
	}

	public void setPosTagSequence(PosTagSequence posTagSequence) {
		this.posTagSequence = posTagSequence;
	}

	/**
	 * Returns the token sequence text after any token filters have replaced
	 * original text with something else.
	 */

	public String getCorrectedText() {
		StringBuilder sb = new StringBuilder();
		int lastPos = 0;
		for (Token token : this) {
			if (token.getOriginalIndex() > lastPos) {
				sb.append(" ");
			}
			sb.append(token.getText());
			lastPos = token.getOriginalIndexEnd();
		}
		return sb.toString();
	}

	public PosTaggerLexicon getLexicon() {
		if (this.lexicon == null) {
			this.lexicon = this.getTalismaneSession().getMergedLexicon();
		}
		return lexicon;
	}

	public TalismaneSession getTalismaneSession() {
		return talismaneSession;
	}

	/**
	 * Returns an exact copy of the current token sequence.
	 */
	public TokenSequence cloneTokenSequence() {
		TokenSequence tokenSequence = new TokenSequence(this);
		return tokenSequence;
	}
}
