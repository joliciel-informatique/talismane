package com.joliciel.talismane.tokeniser;

import java.io.Serializable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.Annotation;
import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.lexicon.PosTaggerLexicon;
import com.joliciel.talismane.rawText.Sentence;
import com.joliciel.talismane.sentenceAnnotators.TokenPlaceholder;

/**
 * A sequence of tokens. Note: by default, List iteration and associated methods
 * will only return non-whitespace tokens. For a list that includes whitespace
 * tokens, use the listWithWhiteSpace() method.<br/>
 * <br/>
 * Only a single attribute of a given key can be added per token. If two
 * attribute placeholders overlap and assign the same key, the one with the
 * lower start index is privileged for all tokens, and the other one is skipped
 * for all tokens. If they have the same start index, the one with the higher
 * end index is priveleged for all tokens, and the other one is skipped for all
 * tokens.
 * 
 * @author Assaf Urieli
 *
 */
public class TokenSequence extends ArrayList<Token>implements Serializable {
  private static final Logger LOG = LoggerFactory.getLogger(TokenSequence.class);
  private static final long serialVersionUID = 1L;

  private List<Integer> tokenSplits;
  private final Sentence sentence;
  private final List<Token> listWithWhiteSpace;
  private List<Token> tokensAdded = null;
  private double score = 0;
  private boolean scoreCalculated = false;
  private TokenisedAtomicTokenSequence underlyingAtomicTokenSequence;
  private Integer atomicTokenCount = null;
  private boolean withRoot = false;
  private final Map<Integer, Annotation<TokenPlaceholder>> placeholderMap;
  private boolean defaultTokensFound = false;

  @SuppressWarnings("rawtypes")
  private final Map<String, NavigableSet<Annotation<TokenAttribute>>> attributeOrderingMap;

  private final String sessionId;

  public TokenSequence(Sentence sentence, String sessionId) {
    this.sentence = sentence;
    this.sessionId = sessionId;
    this.placeholderMap = new HashMap<>();
    this.listWithWhiteSpace = new ArrayList<>();

    List<Annotation<TokenPlaceholder>> placeholders = sentence.getAnnotations(TokenPlaceholder.class);
    for (Annotation<TokenPlaceholder> placeholder : placeholders) {
      // take the first placeholder at this start index only
      // thus declaration order is the order at which they're
      // applied
      if (!placeholderMap.containsKey(placeholder.getStart()))
        placeholderMap.put(placeholder.getStart(), placeholder);
    }

    @SuppressWarnings("rawtypes")
    List<Annotation<TokenAttribute>> tokenAttributes = sentence.getAnnotations(TokenAttribute.class);
    this.attributeOrderingMap = new HashMap<>();
    // select order in which to add attributes, when attributes exist
    // for identical keys
    for (@SuppressWarnings("rawtypes")
    Annotation<TokenAttribute> attribute : tokenAttributes) {
      String key = attribute.getData().getKey();
      @SuppressWarnings("rawtypes")
      NavigableSet<Annotation<TokenAttribute>> attributeSet = attributeOrderingMap.get(key);
      if (attributeSet == null) {
        attributeSet = new TreeSet<>();
        attributeOrderingMap.put(key, attributeSet);
        attributeSet.add(attribute);
      } else {
        // do not add overlapping attributes for the same key
        @SuppressWarnings("rawtypes")
        Annotation<TokenAttribute> floor = attributeSet.floor(attribute);
        if (floor == null || floor.getEnd() <= attribute.getStart()) {
          attributeSet.add(attribute);
        }
      }
    }

  }

  TokenSequence(TokenSequence sequenceToClone) {
    this.sessionId = sequenceToClone.sessionId;
    this.sentence = sequenceToClone.sentence;
    this.listWithWhiteSpace = new ArrayList<>(sequenceToClone.listWithWhiteSpace);
    this.score = sequenceToClone.score;
    this.scoreCalculated = true;
    this.underlyingAtomicTokenSequence = sequenceToClone.underlyingAtomicTokenSequence;
    this.atomicTokenCount = sequenceToClone.atomicTokenCount;
    this.placeholderMap = sequenceToClone.placeholderMap;
    this.attributeOrderingMap = sequenceToClone.attributeOrderingMap;

    for (Token token : sequenceToClone) {
      Token clone = token.cloneToken();
      this.add(clone);
      clone.setTokenSequence(this);
    }
  }

  public TokenSequence(Sentence sentence, TokenisedAtomicTokenSequence tokenisedAtomicTokenSequence, String sessionId) {
    this(sentence, sessionId);
    this.underlyingAtomicTokenSequence = tokenisedAtomicTokenSequence;
  }

  /**
   * Add tokens from the underlying sentence, pre-separated into tokens matching
   * {@link Tokeniser#getTokenSeparators(String)}, except wherever
   * {@link TokenPlaceholder} annotations have been added.
   */
  public void findDefaultTokens() {
    if (!defaultTokensFound) {
      CharSequence text = sentence.getText();
      Pattern separatorPattern = Tokeniser.getTokenSeparators(sessionId);
      Matcher matcher = separatorPattern.matcher(text);
      Set<Integer> separatorMatches = new HashSet<Integer>();
      while (matcher.find())
        separatorMatches.add(matcher.start());

      int currentPos = 0;
      for (int i = 0; i < text.length(); i++) {
        if (placeholderMap.containsKey(i)) {
          if (i > currentPos)
            this.addToken(currentPos, i);
          Annotation<TokenPlaceholder> placeholder = placeholderMap.get(i);
          Token token = this.addToken(placeholder.getStart(), placeholder.getEnd());
          if (placeholder.getData().getReplacement() != null)
            token.setText(placeholder.getData().getReplacement());

          if (separatorPattern.matcher(token.getText()).matches())
            token.setSeparator(true);

          // skip until after the placeholder
          i = placeholder.getEnd() - 1;
          currentPos = placeholder.getEnd();
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

      this.defaultTokensFound = true;
    }
  }

  /**
   * Get the nth token in this sequence.
   */
  @Override
  public Token get(int index) {
    return super.get(index);
  }

  @Override
  public Iterator<Token> iterator() {
    return super.iterator();
  }

  @Override
  public ListIterator<Token> listIterator() {
    return super.listIterator();
  }

  @Override
  public ListIterator<Token> listIterator(int index) {
    return super.listIterator(index);
  }

  @Override
  public List<Token> subList(int fromIndex, int toIndex) {
    return super.subList(fromIndex, toIndex);
  }

  /**
   * Returns the token splits represented by this token sequence, where each
   * integer represents the symbol immediately following a token split.
   */

  public List<Integer> getTokenSplits() {
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
   * A list of tokens that includes white space tokens.
   */

  public List<Token> listWithWhiteSpace() {
    return listWithWhiteSpace;
  }

  /**
   * Finalise a reconstructed token sequence so that all of the indexes are
   * correct on the component tokens, and so that all attributes are correctly
   * assigned to component tokens from the containing sentence.
   */
  private void reindex() {
    int i = 0;
    for (Token token : this.listWithWhiteSpace) {
      token.setIndexWithWhiteSpace(i++);
    }
    i = 0;
    for (Token token : this) {
      token.setIndex(i++);
    }
  }

  void markModified() {
    this.tokenSplits = null;
    this.atomicTokenCount = null;
  }

  /**
   * The geometric mean of the tokeniser decisions. Note that only actual
   * decisions made by a decision maker will be taken into account - any default
   * decisions will not be included.
   */

  public double getScore() {
    if (!this.scoreCalculated) {
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
   * Returns the tokenised atomic token sequence, generated by a tokeniser, from
   * which this token sequence was initially inferred.
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
   * Adds a token to the current sequence, using substring coordinates from the
   * associated sentence. If a token already exists with this exact start and
   * end, will not perform any action. Any other existing token whose start &lt;
   * end and end &gt; start will be removed.
   */

  public Token addToken(int start, int end) {
    return this.addTokenInternal(start, end, false);
  }

  @SuppressWarnings("unchecked")
  Token addTokenInternal(int start, int end, boolean allowEmpty) {
    if (!allowEmpty && start == end)
      return null;

    String string = this.sentence.getText().subSequence(start, end).toString();

    List<Token> tokensToRemove = new ArrayList<Token>();

    int prevTokenIndex = -1;
    int i = 0;
    for (i = this.listWithWhiteSpace.size() - 1; i >= 0; i--) {
      Token aToken = this.listWithWhiteSpace.get(i);
      if (aToken.getEndIndex() <= start) {
        prevTokenIndex = i;
        break;
      } else if (aToken.getStartIndex() == start && aToken.getEndIndex() == end) {
        return aToken;
      } else if (aToken.getStartIndex() < end && aToken.getEndIndex() > start) {
        tokensToRemove.add(aToken);
      }
    }
    boolean lastToken = prevTokenIndex == this.listWithWhiteSpace.size() - 1;

    for (Token tokenToRemove : tokensToRemove) {
      this.listWithWhiteSpace.remove(tokenToRemove);
      this.remove(tokenToRemove);
    }

    Token token = new Token(string, this, this.size(), start, end, this.sessionId);
    token.setIndexWithWhiteSpace(prevTokenIndex + 1);

    this.listWithWhiteSpace.add(prevTokenIndex + 1, token);

    if (!token.isWhiteSpace()) {
      prevTokenIndex = -1;
      for (i = this.size() - 1; i >= 0; i--) {
        Token aToken = this.get(i);
        if (aToken.getEndIndex() <= start) {
          prevTokenIndex = i;
          break;
        }
      }

      super.add(prevTokenIndex + 1, token);
      if (tokensAdded != null)
        tokensAdded.add(token);
      token.setIndex(prevTokenIndex + 1);
    }

    // set token text to replacement, if required
    if (this.placeholderMap.containsKey(token.getStartIndex())) {
      Annotation<TokenPlaceholder> placeholder = this.placeholderMap.get(token.getStartIndex());
      if (placeholder.getEnd() == token.getEndIndex() && placeholder.getData().getReplacement() != null) {
        token.setText(placeholder.getData().getReplacement());
        if (LOG.isTraceEnabled()) {
          LOG.trace("Set token text to: " + token.getText() + " using " + placeholder.toString());
        }
      }
    }

    // add any attributes provided by attribute placeholders
    for (String key : attributeOrderingMap.keySet()) {
      @SuppressWarnings("rawtypes")
      Set<Annotation<TokenAttribute>> myAttributes = attributeOrderingMap.get(key);
      for (@SuppressWarnings("rawtypes")
      Annotation<TokenAttribute> attribute : myAttributes) {
        if (attribute.getEnd() < token.getEndIndex())
          continue;

        if (attribute.getStart() <= token.getStartIndex())
          token.addAttribute(key, attribute.getData());

        break;
      }
    }

    this.markModified();
    if (!lastToken) {
      this.reindex();
    }

    return token;
  }

  /**
   * Add an empty token at a certain position in the sentence.
   */

  public Token addEmptyToken(int position) {
    return this.addTokenInternal(position, position, true);
  }

  /**
   * Remove an empty token from this token sequence.
   * 
   * @throws TalismaneException
   *           if an attempt is made to remove a non-empty token
   */
  public void removeEmptyToken(Token emptyToken) throws TalismaneException {
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

  /**
   * The sentence object on which this token sequence was built, allowing us to
   * identify its location in the source text.
   */

  public Sentence getSentence() {
    return sentence;
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

  public String getSessionId() {
    return sessionId;
  }

  /**
   * Returns an exact copy of the current token sequence.
   */
  public TokenSequence cloneTokenSequence() {
    TokenSequence tokenSequence = new TokenSequence(this);
    return tokenSequence;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    TokenSequence tokens = (TokenSequence) o;
    return sentence.equals(tokens.sentence) &&
      listWithWhiteSpace.equals(tokens.listWithWhiteSpace) &&
      sessionId.equals(tokens.sessionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), sentence, listWithWhiteSpace, sessionId);
  }
}
