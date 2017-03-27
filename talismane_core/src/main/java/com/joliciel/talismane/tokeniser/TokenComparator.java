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

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.rawText.Sentence;
import com.joliciel.talismane.tokeniser.Tokeniser.TokeniserType;
import com.joliciel.talismane.tokeniser.patterns.PatternTokeniser;
import com.joliciel.talismane.tokeniser.patterns.TokenPattern;
import com.joliciel.talismane.tokeniser.patterns.TokenPatternMatchSequence;
import com.joliciel.talismane.tokeniser.patterns.TokeniserPatternManager;
import com.typesafe.config.Config;

/**
 * An interface for comparing two tokenised corpora, one of which is considered
 * a reference.
 * 
 * @author Assaf Urieli
 *
 */
public class TokenComparator {
  private static final Logger LOG = LoggerFactory.getLogger(TokenComparator.class);
  private final List<TokenEvaluationObserver> observers = new ArrayList<>();

  private final TokeniserAnnotatedCorpusReader referenceCorpusReader;
  private final TokeniserAnnotatedCorpusReader evaluationCorpusReader;
  private final TokeniserPatternManager tokeniserPatternManager;
  private final TalismaneSession session;

  public TokenComparator(Reader referenceReader, Reader evalReader, File outDir, TalismaneSession session)
      throws IOException, ClassNotFoundException, ReflectiveOperationException, TalismaneException {
    this.session = session;
    Config config = session.getConfig();
    Config tokeniserConfig = config.getConfig("talismane.core.tokeniser");
    TokeniserType tokeniserType = TokeniserType.valueOf(tokeniserConfig.getString("type"));

    Tokeniser tokeniser = Tokeniser.getInstance(session);

    if (tokeniserType == TokeniserType.pattern) {
      PatternTokeniser patternTokeniser = (PatternTokeniser) tokeniser;
      this.tokeniserPatternManager = patternTokeniser.getTokeniserPatternManager();
    } else {
      this.tokeniserPatternManager = null;
    }
    this.referenceCorpusReader = TokeniserAnnotatedCorpusReader.getCorpusReader(referenceReader, tokeniserConfig.getConfig("input"), session);

    this.evaluationCorpusReader = TokeniserAnnotatedCorpusReader.getCorpusReader(evalReader, tokeniserConfig.getConfig("evaluate"), session);

    List<TokenEvaluationObserver> observers = TokenEvaluationObserver.getTokenEvaluationObservers(outDir, session);
    for (TokenEvaluationObserver observer : observers)
      this.addObserver(observer);

  }

  public TokenComparator(TokeniserAnnotatedCorpusReader referenceCorpusReader, TokeniserAnnotatedCorpusReader evaluationCorpusReader,
      TokeniserPatternManager tokeniserPatternManager, TalismaneSession talismaneSession) {
    this.session = talismaneSession;
    this.referenceCorpusReader = referenceCorpusReader;
    this.evaluationCorpusReader = evaluationCorpusReader;
    this.tokeniserPatternManager = tokeniserPatternManager;
  }

  /**
   * Evaluate the evaluation corpus against the reference corpus.
   * 
   * @throws TalismaneException
   * @throws IOException
   */
  public void compare() throws TalismaneException, IOException {
    while (referenceCorpusReader.hasNextSentence()) {
      TokenSequence realSequence = referenceCorpusReader.nextTokenSequence();

      TokenSequence guessedSequence = null;
      if (evaluationCorpusReader.hasNextSentence())
        guessedSequence = evaluationCorpusReader.nextTokenSequence();
      else {
        throw new TalismaneException("Wrong number of sentences in eval corpus: " + realSequence.getSentence().getText());
      }

      Sentence sentence = realSequence.getSentence();

      // Initially, separate the sentence into tokens using the separators
      // provided
      TokenSequence realAtomicSequence = new TokenSequence(sentence, session);
      realAtomicSequence.findDefaultTokens();
      TokenSequence guessedAtomicSequence = new TokenSequence(guessedSequence.getSentence(), session);
      guessedAtomicSequence.findDefaultTokens();

      List<TokenPatternMatchSequence> matchingSequences = new ArrayList<TokenPatternMatchSequence>();
      Map<Token, Set<TokenPatternMatchSequence>> tokenMatchSequenceMap = new HashMap<Token, Set<TokenPatternMatchSequence>>();
      Set<Token> matchedTokens = new HashSet<Token>();

      for (TokenPattern parsedPattern : tokeniserPatternManager.getParsedTestPatterns()) {
        List<TokenPatternMatchSequence> matchesForThisPattern = parsedPattern.match(realAtomicSequence);
        for (TokenPatternMatchSequence matchSequence : matchesForThisPattern) {
          matchingSequences.add(matchSequence);
          matchedTokens.addAll(matchSequence.getTokensToCheck());

          Token token = null;
          for (Token aToken : matchSequence.getTokensToCheck()) {
            token = aToken;
            if (!aToken.isWhiteSpace()) {
              break;
            }
          }

          Set<TokenPatternMatchSequence> matchSequences = tokenMatchSequenceMap.get(token);
          if (matchSequences == null) {
            matchSequences = new TreeSet<TokenPatternMatchSequence>();
            tokenMatchSequenceMap.put(token, matchSequences);
          }
          matchSequences.add(matchSequence);
        }
      }

      TokenisedAtomicTokenSequence guess = new TokenisedAtomicTokenSequence(realSequence.getSentence(), 0, session);

      int i = 0;
      int mismatches = 0;
      for (Token token : realAtomicSequence) {
        if (!token.getText().equals(guessedAtomicSequence.get(i).getToken().getText())) {
          // skipped stuff at start of sentence on guess, if it's been
          // through the parser
          TokeniserOutcome outcome = TokeniserOutcome.SEPARATE;
          Decision decision = new Decision(outcome.name());
          decision.addAuthority("_" + this.getClass().getSimpleName());
          Set<TokenPatternMatchSequence> matchSequences = tokenMatchSequenceMap.get(token);
          if (matchSequences != null) {
            decision.addAuthority("_Patterns");
            for (TokenPatternMatchSequence matchSequence : matchSequences) {
              decision.addAuthority(matchSequence.getTokenPattern().getName());
            }
          }
          guess.addTaggedToken(token, decision, outcome);
          mismatches++;
          LOG.debug("Mismatch: '" + token.getText() + "', '" + guessedAtomicSequence.get(i).getToken().getText() + "'");
          if (mismatches > 6) {
            LOG.info("Real sequence: " + realSequence.getSentence().getText());
            LOG.info("Guessed sequence: " + guessedSequence.getSentence().getText());
            throw new TalismaneException("Too many mismatches for sentence: " + realSequence.getSentence().getText());
          }
          continue;
        }
        TokeniserOutcome outcome = TokeniserOutcome.JOIN;

        if (guessedSequence.getTokenSplits().contains(guessedAtomicSequence.get(i).getToken().getStartIndex())) {
          outcome = TokeniserOutcome.SEPARATE;
        }
        Decision decision = new Decision(outcome.name());
        decision.addAuthority("_" + this.getClass().getSimpleName());

        Set<TokenPatternMatchSequence> matchSequences = tokenMatchSequenceMap.get(token);
        if (matchSequences != null) {
          decision.addAuthority("_Patterns");
          for (TokenPatternMatchSequence matchSequence : matchSequences) {
            decision.addAuthority(matchSequence.getTokenPattern().getName());
          }
        }
        guess.addTaggedToken(token, decision, outcome);
        i++;
      }

      List<TokenisedAtomicTokenSequence> guessedAtomicSequences = new ArrayList<TokenisedAtomicTokenSequence>();
      guessedAtomicSequences.add(guess);

      for (TokenEvaluationObserver observer : observers) {
        observer.onNextTokenSequence(realSequence, guessedAtomicSequences);
      }
    } // next sentence

    for (TokenEvaluationObserver observer : observers) {
      observer.onEvaluationComplete();
    }
  }

  public void addObserver(TokenEvaluationObserver observer) {
    this.observers.add(observer);
  }
}
