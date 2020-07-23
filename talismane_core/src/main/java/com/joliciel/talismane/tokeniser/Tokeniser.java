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

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.Annotation;
import com.joliciel.talismane.Annotator;
import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.machineLearning.ClassificationObserver;
import com.joliciel.talismane.rawText.Sentence;
import com.joliciel.talismane.sentenceAnnotators.TokenPlaceholder;
import com.joliciel.talismane.tokeniser.filters.TokenFilter;
import com.typesafe.config.Config;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

/**
 * A Tokeniser splits a sentence up into tokens (parsing units).<br/>
 * <br/>
 * It adds annotations of type {@link TokenBoundary} to the sentence, which are
 * guaranteed not to overlap.<br/>
 * <br/>
 * The Tokeniser must recognise the following annotations in the sentence
 * provided:<br/>
 * <ul>
 * <li>{@link TokenPlaceholder}: will get replaced by a token.</li>
 * <li>{@link TokenAttribute}: will add attributes to all tokens contained
 * within its span.</li>
 * </ul>
 * 
 * @author Assaf Urieli
 *
 */
public abstract class Tokeniser implements Annotator<Sentence> {
  private static final Logger LOG = LoggerFactory.getLogger(Tokeniser.class);

  private static final Map<String, Tokeniser> tokeniserMap = new HashMap<>();
  private static final Map<String, Pattern> tokenSeparatorMap = new HashMap<>();

  private final String sessionId;
  private final List<TokenFilter> filters;

  public Tokeniser(String sessionId) throws IOException, TalismaneException, ReflectiveOperationException {
    this.sessionId = sessionId;

    Config config = ConfigFactory.load();

    this.filters = new ArrayList<>();

    String configPath = "talismane.core." + sessionId + ".tokeniser.filters";
    List<String> filterDescriptors = config.getStringList(configPath);
    for (String descriptor : filterDescriptors) {
      TokenFilter filter = TokenFilter.loadFilter(descriptor, sessionId);
      this.filters.add(filter);
    }
  }

  protected Tokeniser(Tokeniser tokeniser) {
    this.sessionId = tokeniser.sessionId;
    this.filters = tokeniser.filters;
  }

  /**
   * Similar to {@link #tokenise(String)}, but returns only the best token
   * sequence.
   * 
   * @throws IOException
   */

  public TokenSequence tokeniseText(String text) throws TalismaneException, IOException {
    List<TokenSequence> tokenSequences = this.tokenise(text);
    return tokenSequences.get(0);
  }

  /**
   * Similar to {@link #tokeniseWithDecisions(String)}, but returns the token
   * sequences inferred from the decisions, rather than the list of decisions
   * themselves.
   * 
   * @throws IOException
   */

  public List<TokenSequence> tokenise(String text) throws TalismaneException, IOException {
    Sentence sentence = new Sentence(text, sessionId);
    return this.tokenise(sentence);
  }

  /**
   * Similar to {@link #tokenise(Sentence, String...)}, but returns only the
   * best token sequence.
   * 
   * @throws IOException
   */

  public TokenSequence tokeniseSentence(Sentence sentence, String... labels) throws TalismaneException, IOException {
    List<TokenSequence> tokenSequences = this.tokenise(sentence, labels);
    return tokenSequences.get(0);
  }

  /**
   * Similar to {@link #tokeniseWithDecisions(Sentence, String...)}, but returns
   * the token sequences inferred from the decisions, rather than the list of
   * decisions themselves.
   * 
   * @throws IOException
   */

  public List<TokenSequence> tokenise(Sentence sentence, String... labels) throws TalismaneException, IOException {
    List<TokenisedAtomicTokenSequence> decisionSequences = this.tokeniseWithDecisions(sentence, labels);
    List<TokenSequence> tokenSequences = new ArrayList<TokenSequence>();
    for (TokenisedAtomicTokenSequence decisionSequence : decisionSequences) {
      tokenSequences.add(decisionSequence.inferTokenSequence());
    }
    return tokenSequences;
  }

  /**
   * Tokenise a given sentence. More specifically, return up to N most likely
   * tokeniser decision sequences, each of which breaks up the sentence into a
   * different a list of tokens. Note: we assume duplicate white-space has
   * already been removed from the sentence prior to calling the tokenise
   * method, e.g. multiple spaces have been replaced by a single space.
   * 
   * @param text
   *          the sentence to be tokenised
   * @return a List of up to <i>n</i> TokeniserDecisionTagSequence, ordered from
   *         most probable to least probable
   * @throws IOException
   */

  public List<TokenisedAtomicTokenSequence> tokeniseWithDecisions(String text) throws TalismaneException, IOException {
    Sentence sentence = new Sentence(text, sessionId);
    return this.tokeniseWithDecisions(sentence);
  }

  /**
   * Similar to {@link #tokeniseWithDecisions(String)}, but the text to be
   * tokenised is contained within a Sentence object.
   * 
   * @param sentence
   *          the sentence to tokenise
   * @param labels
   *          the labels to add to any annotations added.
   * @throws IOException
   */

  public List<TokenisedAtomicTokenSequence> tokeniseWithDecisions(Sentence sentence, String... labels) throws TalismaneException, IOException {
    // Initially, separate the sentence into tokens using the separators
    // provided
    TokenSequence tokenSequence = new TokenSequence(sentence, this.sessionId);
    tokenSequence.findDefaultTokens();

    List<TokenisedAtomicTokenSequence> sequences = this.tokeniseInternal(tokenSequence, sentence);

    LOG.debug("####Final token sequences:");
    int j = 1;
    for (TokenisedAtomicTokenSequence sequence : sequences) {
      TokenSequence newTokenSequence = sequence.inferTokenSequence();

      for (TokenFilter filter : filters)
        filter.apply(newTokenSequence);

      if (j == 1) {
        // add annotations for the very first token sequence
        List<Annotation<TokenBoundary>> tokenBoundaries = new ArrayList<>();
        for (Token token : newTokenSequence) {
          Annotation<TokenBoundary> tokenBoundary = new Annotation<>(token.getStartIndex(), token.getEndIndex(),
              new TokenBoundary(token.getText(), token.getAnalyisText(), token.getAttributes()), labels);
          tokenBoundaries.add(tokenBoundary);
        }
        sentence.addAnnotations(tokenBoundaries);
      }
      if (LOG.isDebugEnabled()) {
        LOG.debug("Token sequence " + j);
        LOG.debug("Atomic sequence: " + sequence);
        LOG.debug("Resulting sequence: " + newTokenSequence);
      }
      j++;
    }

    return sequences;
  }

  @Override
  public void annotate(Sentence sentence, String... labels) throws TalismaneException, IOException {
    this.tokeniseWithDecisions(sentence, labels);
  }

  protected abstract List<TokenisedAtomicTokenSequence> tokeniseInternal(TokenSequence initialSequence, Sentence sentence)
      throws TalismaneException, IOException;

  public void addObserver(ClassificationObserver observer) {
    // nothing to do here
  }

  public abstract Tokeniser cloneTokeniser();

  /**
   * Build a tokeniser using the configuration provided.
   * @return a tokeniser to be used - each call returns a separate tokeniser
   * @throws IOException
   *           if problems occurred reading the model
   * @throws ClassNotFoundException
   * @throws TalismaneException
   * @throws ReflectiveOperationException
   */
  public static Tokeniser getInstance(String sessionId) throws IOException, ClassNotFoundException, TalismaneException, ReflectiveOperationException {
    Tokeniser tokeniser = tokeniserMap.get(sessionId);
    if (tokeniser == null) {
      Config config = ConfigFactory.load();
      Config tokeniserConfig = config.getConfig("talismane.core." + sessionId + ".tokeniser");

      String className = tokeniserConfig.getString("tokeniser");

      @SuppressWarnings("rawtypes")
      Class untypedClass = Class.forName(className);
      if (!Tokeniser.class.isAssignableFrom(untypedClass))
        throw new TalismaneException("Class " + className + " does not implement interface " + Tokeniser.class.getSimpleName());

      @SuppressWarnings("unchecked")
      Class<? extends Tokeniser> clazz = untypedClass;

      Constructor<? extends Tokeniser> cons = null;

      if (cons == null) {
        try {
          cons = clazz.getConstructor(String.class);
        } catch (NoSuchMethodException e) {
          // do nothing
        }
        if (cons != null) {
          tokeniser = cons.newInstance(sessionId);
        } else {
          throw new TalismaneException("No constructor found with correct signature for: " + className);
        }
      }

      tokeniserMap.put(sessionId, tokeniser);
    }

    return tokeniser.cloneTokeniser();
  }

  /**
   * A pattern matching default separators for tokens.
   */
  public static Pattern getTokenSeparators(String sessionId) {
    Pattern tokenSeparators = tokenSeparatorMap.get(sessionId);
    if (tokenSeparators == null) {
      Config config = ConfigFactory.load();
      String separatorRegex = config.getString("talismane.core." + sessionId + ".tokeniser.separators");
      tokenSeparators = Pattern.compile(separatorRegex, Pattern.UNICODE_CHARACTER_CLASS);
      tokenSeparatorMap.put(sessionId, tokenSeparators);
    }
    return tokenSeparators;
  }

  /**
   * For a given text, returns a list of strings which are guaranteed not to
   * overlap any token boundaries, except on the unlikely occurrence when a
   * token placeholder cut a token in the middle of two non-separators.
   */
  public static List<String> bruteForceTokenise(CharSequence text, String sessionId) {
    List<String> tokens = new ArrayList<>();
    Pattern separatorPattern = Tokeniser.getTokenSeparators(sessionId);
    Matcher matcher = separatorPattern.matcher(text);
    TIntSet separatorMatches = new TIntHashSet();
    while (matcher.find())
      separatorMatches.add(matcher.start());

    int currentPos = 0;
    for (int i = 0; i < text.length(); i++) {
      if (separatorMatches.contains(i)) {
        if (i > currentPos)
          tokens.add(text.subSequence(currentPos, i).toString());
        tokens.add(text.subSequence(i, i + 1).toString());
        currentPos = i + 1;
      }
    }

    if (currentPos < text.length())
      tokens.add(text.subSequence(currentPos, text.length()).toString());

    return tokens;
  }

  public String getSessionId() {
    return sessionId;
  }
}
