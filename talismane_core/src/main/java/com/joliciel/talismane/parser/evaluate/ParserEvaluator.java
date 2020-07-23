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
package com.joliciel.talismane.parser.evaluate;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.Talismane.Module;
import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.parser.NonDeterministicParser;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.parser.Parser;
import com.joliciel.talismane.parser.ParserAnnotatedCorpusReader;
import com.joliciel.talismane.parser.Parsers;
import com.joliciel.talismane.posTagger.NonDeterministicPosTagger;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTagger;
import com.joliciel.talismane.posTagger.PosTaggers;
import com.joliciel.talismane.rawText.Sentence;
import com.joliciel.talismane.sentenceAnnotators.SentenceAnnotator;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.Tokeniser;
import com.typesafe.config.Config;

/**
 * Evaluate a parser.
 * 
 * @author Assaf Urieli
 *
 */
public class ParserEvaluator {
  private static final Logger LOG = LoggerFactory.getLogger(ParserEvaluator.class);
  private final ParserAnnotatedCorpusReader corpusReader;
  private final Parser parser;
  private final PosTagger posTagger;
  private final Tokeniser tokeniser;

  private final List<ParseEvaluationObserver> observers;
  private final String sessionId;

  public ParserEvaluator(Reader evalReader, File outDir, String sessionId)
      throws ClassNotFoundException, IOException, ReflectiveOperationException, TalismaneException {
    this.sessionId = sessionId;
    Config config = ConfigFactory.load();
    Config parserConfig = config.getConfig("talismane.core." + sessionId + ".parser");
    Config evalConfig = parserConfig.getConfig("evaluate");

    this.observers = ParseEvaluationObserver.getObservers(outDir, sessionId);
    this.corpusReader = ParserAnnotatedCorpusReader.getCorpusReader(evalReader, parserConfig.getConfig("input"), sessionId);

    this.parser = Parsers.getParser(sessionId);

    Module startModule = Module.valueOf(evalConfig.getString("start-module"));
    if (startModule == Module.tokeniser) {
      tokeniser = Tokeniser.getInstance(sessionId);
    } else {
      tokeniser = null;
    }
    if (startModule == Module.tokeniser || startModule == Module.posTagger) {
      posTagger = PosTaggers.getPosTagger(sessionId);
    } else {
      posTagger = null;
    }
  }

  public ParserEvaluator(ParserAnnotatedCorpusReader corpusReader, Parser parser, PosTagger posTagger, Tokeniser tokeniser, boolean propagateTokeniserBeam,
      boolean propagateBeam, String sessionId) {
    this.sessionId = sessionId;
    this.corpusReader = corpusReader;
    this.parser = parser;
    this.posTagger = posTagger;
    this.tokeniser = tokeniser;
    this.observers = new ArrayList<>();
  }

  /**
   * 
   * @throws TalismaneException
   *           if an attempt is made to evaluate with a tokeniser but no
   *           pos-tagger
   * @throws IOException
   */
  public void evaluate() throws TalismaneException, IOException {
    while (corpusReader.hasNextSentence()) {
      ParseConfiguration realConfiguration = corpusReader.nextConfiguration();

      List<PosTagSequence> posTagSequences = null;
      List<TokenSequence> tokenSequences = null;
      if (tokeniser != null) {
        if (posTagger == null)
          throw new TalismaneException("Cannot evaluate with tokeniser but no pos-tagger");

        Sentence sentence = realConfiguration.getPosTagSequence().getTokenSequence().getSentence();

        // annotate the sentence for pre token filters
        for (SentenceAnnotator annotator : TalismaneSession.get(sessionId).getSentenceAnnotators()) {
          annotator.annotate(sentence);
          if (LOG.isTraceEnabled()) {
            LOG.trace("TokenFilter: " + annotator);
            LOG.trace("annotations: " + sentence.getAnnotations());
          }
        }

        tokenSequences = tokeniser.tokenise(sentence);
      } else {
        tokenSequences = new ArrayList<TokenSequence>();
        PosTagSequence posTagSequence = realConfiguration.getPosTagSequence().clonePosTagSequence();
        posTagSequence.removeRoot();
        tokenSequences.add(posTagSequence.getTokenSequence());
      }

      if (posTagger != null) {
        if (posTagger instanceof NonDeterministicPosTagger) {
          NonDeterministicPosTagger nonDeterministicPosTagger = (NonDeterministicPosTagger) posTagger;
          posTagSequences = nonDeterministicPosTagger.tagSentence(tokenSequences);
        } else {
          posTagSequences = new ArrayList<PosTagSequence>();
          PosTagSequence posTagSequence = null;
          posTagSequence = posTagger.tagSentence(tokenSequences.get(0));
          posTagSequences.add(posTagSequence);
        }
      } else {
        PosTagSequence posTagSequence = realConfiguration.getPosTagSequence();
        posTagSequences = new ArrayList<PosTagSequence>();
        posTagSequences.add(posTagSequence);
      }

      for (ParseEvaluationObserver observer : this.observers) {
        observer.onParseStart(realConfiguration, posTagSequences);
      }

      List<ParseConfiguration> guessedConfigurations = null;
      if (parser instanceof NonDeterministicParser) {
        NonDeterministicParser nonDeterministicParser = (NonDeterministicParser) parser;
        guessedConfigurations = nonDeterministicParser.parseSentence(posTagSequences);
      } else {
        ParseConfiguration bestGuess = parser.parseSentence(posTagSequences.get(0));
        guessedConfigurations = new ArrayList<ParseConfiguration>();
        guessedConfigurations.add(bestGuess);
      }

      for (ParseEvaluationObserver observer : this.observers) {
        observer.onParseEnd(realConfiguration, guessedConfigurations);
      }
    } // next sentence

    for (ParseEvaluationObserver observer : this.observers) {
      observer.onEvaluationComplete();
    }
  }

  public Parser getParser() {
    return parser;
  }

  /**
   * If provided, will apply pos-tagging as part of the evaluation.
   */
  public PosTagger getPosTagger() {
    return posTagger;
  }

  /**
   * If provided, will apply tokenisation as part of the evaluation. If
   * provided, a pos-tagger must be provided as well.
   */
  public Tokeniser getTokeniser() {
    return tokeniser;
  }

  public List<ParseEvaluationObserver> getObservers() {
    return observers;
  }

  public void addObserver(ParseEvaluationObserver observer) {
    this.observers.add(observer);
  }

}
