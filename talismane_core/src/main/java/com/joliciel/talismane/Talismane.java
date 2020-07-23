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
package com.joliciel.talismane;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.parser.NonDeterministicParser;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.parser.Parser;
import com.joliciel.talismane.parser.Parsers;
import com.joliciel.talismane.parser.output.ParseConfigurationProcessor;
import com.joliciel.talismane.posTagger.NonDeterministicPosTagger;
import com.joliciel.talismane.posTagger.PosTagAnnotatedCorpusReader;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTagger;
import com.joliciel.talismane.posTagger.PosTaggers;
import com.joliciel.talismane.posTagger.output.PosTagSequenceProcessor;
import com.joliciel.talismane.rawText.RawTextAnnotator;
import com.joliciel.talismane.rawText.RollingTextBlock;
import com.joliciel.talismane.rawText.Sentence;
import com.joliciel.talismane.sentenceAnnotators.SentenceAnnotator;
import com.joliciel.talismane.sentenceDetector.SentenceDetector;
import com.joliciel.talismane.sentenceDetector.SentenceProcessor;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.Tokeniser;
import com.joliciel.talismane.tokeniser.TokeniserAnnotatedCorpusReader;
import com.joliciel.talismane.tokeniser.output.TokenSequenceProcessor;
import com.joliciel.talismane.utils.ArrayListNoNulls;
import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.io.CurrentFileObserver;
import com.joliciel.talismane.utils.io.CurrentFileProvider;
import com.typesafe.config.Config;

/**
 * A class for processing a stream of data using a reader via
 * {@link #analyse(Reader)}, and creating various outputs, with formats as
 * defined by the configuration, with their location specified in the
 * constructor.<br/>
 * <br/>
 * The processing will go from {@link #getStartModule()} to
 * {@link #getEndModule()}. Not thread-safe.<br/>
 * <br/>
 * Processing will stop when the reader finishes reading, or when three
 * consecutive {@link TalismaneSession#getEndBlockCharacter()} are encountered.
 * <br/>
 * <br/>
 * 
 * @author Assaf Urieli
 *
 */
public class Talismane {
  /**
   * A module within the Talismane Suite.
   * 
   * @author Assaf Urieli
   *
   */
  public enum Module {
    languageDetector,
    sentenceDetector,
    tokeniser,
    posTagger,
    parser
  }

  /**
   * The command which Talismane is asked to perform.
   * 
   * @author Assaf Urieli
   *
   */
  public enum Command {
    /**
     * Train a model using a corpus, a feature set, a classifier + parameters,
     * etc.
     */
    train,
    /**
     * Analyse a corpus and add annotations.
     */
    analyse,
    /**
     * Evaluate an annotated corpus, by re-analysing the corpus and comparing
     * the new annotations to the existing ones.
     */
    evaluate,
    /**
     * Process an annotated corpus - Talismane simply reads the corpus using the
     * appropriate corpus reader and passes the results to the appropriate
     * processors.
     */
    process,
    /**
     * Compare two annotated corpora.
     */
    compare
  }

  public enum ProcessingOption {
    /**
     * Simply output what you read, usually changing the format.
     */
    output,
    /**
     * Test pos-tag features on a subset of words in the training set.
     */
    posTagFeatureTester,
    /**
     * Test parse features on the training set.
     */
    parseFeatureTester
  }

  public enum Mode {
    /**
     * Command line mode, reading from standard in or file, and writing to
     * standard out or file.
     */
    normal,
    /**
     * Server listening on port, and processing input as it comes.
     */
    server
  }

  /**
   * How to output the Talismane analysis
   */
  public enum BuiltInTemplate {
    /**
     * Standard CoNLL-X output.
     */
    standard,
    /**
     * Include extra columns for the original file location of each token.
     */
    with_location,
    /**
     * Include extra columns for the probability of each decision.
     */
    with_prob,
    /**
     * Include extra columns for user-supplied comments in the training corpus.
     */
    with_comments,
    /**
     * Include the original lemma, morphology and category on each line.
     */
    original
  }

  private static final Logger LOG = LoggerFactory.getLogger(Talismane.class);

  private final List<SentenceProcessor> sentenceProcessors;
  private final List<TokenSequenceProcessor> tokenSequenceProcessors;
  private final List<PosTagSequenceProcessor> posTagSequenceProcessors;
  private final List<ParseConfigurationProcessor> parseConfigurationProcessors;

  private final boolean stopOnError;

  private final Config config;
  private final String sessionId;
  private final Module startModule;
  private final Module endModule;

  private final boolean processByDefault;

  private final int sentenceCount;
  private final Writer writer;

  private final SentenceDetector sentenceDetector;
  private final Tokeniser tokeniser;
  private final PosTagger posTagger;
  private final Parser parser;

  /**
   * 
   * @param writer
   *          a writer for writing the main output - if null, all output is
   *          written to the outDir with predefined filenames
   * @param outDir
   *          a directory for writing any additional output files specified in
   *          the configuration
   * @param sessionId
   * @throws IOException
   * @throws ReflectiveOperationException
   * @throws TalismaneException
   *           if start module comes after end module in the configuration.
   */
  public Talismane(Writer writer, File outDir, String sessionId) throws IOException, ReflectiveOperationException, TalismaneException {
    this.sessionId = sessionId;
    this.config = ConfigFactory.load();
    Config analyseConfig = config.getConfig("talismane.core." + sessionId + ".analysis");

    this.startModule = Module.valueOf(analyseConfig.getString("start-module"));
    this.endModule = Module.valueOf(analyseConfig.getString("end-module"));

    if (startModule.compareTo(endModule) > 0) {
      throw new TalismaneException("Start-module (" + startModule.name() + ") cannot come after end-module (" + endModule.name() + ")");
    }

    this.processByDefault = analyseConfig.getBoolean("process-by-default");
    this.stopOnError = analyseConfig.getBoolean("stop-on-error");
    this.sentenceCount = config.getInt("talismane.core." + sessionId + ".input.sentence-count");
    boolean outputIntermediateModules = analyseConfig.getBoolean("output-intermediate-modules");

    if (this.endModule == Module.sentenceDetector) {
      this.sentenceProcessors = SentenceProcessor.getProcessors(writer, outDir, sessionId);
    } else
      if (outputIntermediateModules && this.startModule.compareTo(Module.sentenceDetector) <= 0 && this.endModule.compareTo(Module.sentenceDetector) >= 0) {
      this.sentenceProcessors = SentenceProcessor.getProcessors(null, outDir, sessionId);
    } else {
      this.sentenceProcessors = new ArrayList<>();
    }
    if (this.endModule == Module.tokeniser) {
      this.tokenSequenceProcessors = TokenSequenceProcessor.getProcessors(writer, outDir, sessionId);
    } else if (outputIntermediateModules && this.startModule.compareTo(Module.tokeniser) <= 0 && this.endModule.compareTo(Module.tokeniser) >= 0) {
      this.tokenSequenceProcessors = TokenSequenceProcessor.getProcessors(null, outDir, sessionId);
    } else {
      this.tokenSequenceProcessors = new ArrayList<>();
    }
    if (this.endModule == Module.posTagger) {
      this.posTagSequenceProcessors = PosTagSequenceProcessor.getProcessors(writer, outDir, sessionId);
    } else if (outputIntermediateModules && this.startModule.compareTo(Module.posTagger) <= 0 && this.endModule.compareTo(Module.posTagger) >= 0) {
      this.posTagSequenceProcessors = PosTagSequenceProcessor.getProcessors(null, outDir, sessionId);
    } else {
      this.posTagSequenceProcessors = new ArrayList<>();
    }
    if (this.endModule == Module.parser
        || (outputIntermediateModules && this.startModule.compareTo(Module.parser) <= 0 && this.endModule.compareTo(Module.parser) >= 0)) {
      this.parseConfigurationProcessors = ParseConfigurationProcessor.getProcessors(writer, outDir, sessionId);
    } else if (outputIntermediateModules && this.startModule.compareTo(Module.parser) <= 0 && this.endModule.compareTo(Module.parser) >= 0) {
      this.parseConfigurationProcessors = ParseConfigurationProcessor.getProcessors(null, outDir, sessionId);
    } else {
      this.parseConfigurationProcessors = new ArrayList<>();
    }

    if (this.needsSentenceDetector())
      sentenceDetector = SentenceDetector.getInstance(sessionId);
    else
      sentenceDetector = null;
    if (this.needsTokeniser())
      tokeniser = Tokeniser.getInstance(sessionId);
    else
      tokeniser = null;
    if (this.needsPosTagger())
      posTagger = PosTaggers.getPosTagger(sessionId);
    else
      posTagger = null;
    if (this.needsParser())
      parser = Parsers.getParser(sessionId);
    else
      parser = null;

    this.writer = writer;
  }

  /**
   * Analyse the data provided by this reader, as specified by the
   * configuration.
   * 
   * @param reader
   * @throws IOException
   * @throws ReflectiveOperationException
   * @throws TalismaneException
   *           if it's impossible to read a sentence from an annotated corpus
   */
  public void analyse(Reader reader) throws IOException, ReflectiveOperationException, TalismaneException {
    long startTime = System.currentTimeMillis();
    try {
      TokeniserAnnotatedCorpusReader tokenCorpusReader = null;
      PosTagAnnotatedCorpusReader posTagCorpusReader = null;

      if (this.startModule.equals(Module.posTagger)) {
        tokenCorpusReader = TokeniserAnnotatedCorpusReader.getCorpusReader(reader, config.getConfig("talismane.core." + sessionId + ".tokeniser.input"),
            sessionId);
      }

      if (this.startModule.equals(Module.parser)) {
        posTagCorpusReader = PosTagAnnotatedCorpusReader.getCorpusReader(reader, config.getConfig("talismane.core." + sessionId + ".pos-tagger.input"),
          sessionId);
      }

      LinkedList<String> textSegments = new LinkedList<String>();
      LinkedList<Sentence> sentences = new LinkedList<Sentence>();
      TokenSequence tokenSequence = null;
      PosTagSequence posTagSequence = null;

      StringBuilder stringBuilder = new StringBuilder();
      boolean finished = false;
      int sentenceCount = 0;

      CurrentFileProvider currentFileProvider = reader instanceof CurrentFileProvider ? (CurrentFileProvider) reader : null;
      RollingTextBlock rollingTextBlock = new RollingTextBlock(this.processByDefault, currentFileProvider, sessionId);

      int endBlockCharacterCount = 0;

      URI currentURI = null;
      File currentFile = null;

      while (!finished) {
        if (this.startModule.equals(Module.sentenceDetector) || this.startModule.equals(Module.tokeniser)) {
          // Note SentenceDetector and Tokeniser start modules treated
          // identically,
          // except that for SentenceDetector we apply a probabilistic
          // sentence detector
          // whereas for Tokeniser we assume all sentence breaks are
          // marked by filters

          // read characters from the reader, one at a time
          char c;
          int r = -1;
          try {
            r = reader.read();
          } catch (IOException e) {
            LogUtils.logError(LOG, e);
          }

          if (r == -1) {
            finished = true;
            c = '\n';
          } else {
            c = (char) r;
          }

          // Jump out if we have 3 consecutive end-block characters.
          if (c == TalismaneSession.get(sessionId).getEndBlockCharacter()) {
            endBlockCharacterCount++;
            if (endBlockCharacterCount == 3) {
              LOG.info("Three consecutive end-block characters. Exiting.");
              finished = true;
            }
          } else {
            endBlockCharacterCount = 0;
          }

          // have sentence detector
          if (finished || (Character.isWhitespace(c) && c != '\r' && c != '\n' && stringBuilder.length() > TalismaneSession.get(sessionId).getBlockSize())
              || c == TalismaneSession.get(sessionId).getEndBlockCharacter()) {
            if (c == TalismaneSession.get(sessionId).getEndBlockCharacter())
              stringBuilder.append(c);
            if (stringBuilder.length() > 0) {
              String textSegment = stringBuilder.toString();
              stringBuilder = new StringBuilder();

              textSegments.add(textSegment);
            } // is the current block > 0 characters?
            if (c == TalismaneSession.get(sessionId).getEndBlockCharacter()) {
              textSegments.addLast("");
            }
          } // is there a next block available?

          if (finished) {
            if (stringBuilder.length() > 0) {
              textSegments.addLast(stringBuilder.toString());
              stringBuilder = new StringBuilder();
            }
            // add three final text segments to roll everything
            // through processing
            textSegments.addLast("");
            textSegments.addLast("");
            textSegments.addLast("");
          }

          if (c != TalismaneSession.get(sessionId).getEndBlockCharacter())
            stringBuilder.append(c);

          while (textSegments.size() > 0) {
            // roll in a new block 4, and roll the other blocks
            // leftwards
            String nextText = textSegments.removeFirst();
            rollingTextBlock = rollingTextBlock.roll(nextText);

            // annotate block 3 with raw text filters
            AnnotatedText rawTextBlock = rollingTextBlock.getRawTextBlock();

            for (RawTextAnnotator textAnnotator : TalismaneSession.get(sessionId).getTextAnnotators()) {
              textAnnotator.annotate(rawTextBlock);
            }

            // detect sentences in block 2 using the sentence
            // detector
            AnnotatedText processedText = rollingTextBlock.getProcessedText();
            if (LOG.isTraceEnabled()) {
              LOG.trace("processedText: " + processedText.getText().toString().replace('\n', '¶').replace('\r', '¶'));
            }

            if (this.startModule.equals(Module.sentenceDetector)) {
              sentenceDetector.detectSentences(processedText);
            }

            // get the sentences detected in block 2
            List<Sentence> theSentences = rollingTextBlock.getDetectedSentences();
            for (Sentence sentence : theSentences) {
              sentences.add(sentence);
              sentenceCount++;
            }
            if (this.sentenceCount > 0 && sentenceCount >= this.sentenceCount) {
              finished = true;
            }
          } // we have at least one text segment to process
        } else if (this.startModule.equals(Module.posTagger)) {
          if (tokenCorpusReader.hasNextSentence()) {
            tokenSequence = tokenCorpusReader.nextTokenSequence();
          } else {
            tokenSequence = null;
            finished = true;
          }
        } else if (this.startModule.equals(Module.parser)) {
          if (posTagCorpusReader.hasNextSentence()) {
            posTagSequence = posTagCorpusReader.nextPosTagSequence();
          } else {
            posTagSequence = null;
            finished = true;
          }
        } // which start module?

        boolean needToProcess = false;
        if (this.startModule.equals(Module.sentenceDetector) || this.startModule.equals(Module.tokeniser))
          needToProcess = !sentences.isEmpty();
        else if (this.startModule.equals(Module.posTagger))
          needToProcess = tokenSequence != null;
        else if (this.startModule.equals(Module.parser))
          needToProcess = posTagSequence != null;

        while (needToProcess) {
          Sentence sentence = null;
          if (this.startModule.compareTo(Module.tokeniser) <= 0 && this.endModule.compareTo(Module.sentenceDetector) >= 0) {
            sentence = sentences.poll();
            LOG.debug("Sentence: " + sentence);

            for (SentenceAnnotator annotator : TalismaneSession.get(sessionId).getSentenceAnnotators())
              annotator.annotate(sentence);

            if (sentence.getFileURI() != null && !sentence.getFileURI().equals(currentURI)) {
              currentURI = sentence.getFileURI();
              currentFile = sentence.getFile();
              LOG.debug("Setting current file to " + currentFile.getPath());
              if (writer instanceof CurrentFileObserver)
                ((CurrentFileObserver) writer).onNextFile(currentFile);
              for (SentenceProcessor processor : sentenceProcessors)
                if (processor instanceof CurrentFileObserver)
                  ((CurrentFileObserver) processor).onNextFile(currentFile);
              for (TokenSequenceProcessor processor : tokenSequenceProcessors)
                if (processor instanceof CurrentFileObserver)
                  ((CurrentFileObserver) processor).onNextFile(currentFile);
              for (PosTagSequenceProcessor processor : posTagSequenceProcessors)
                if (processor instanceof CurrentFileObserver)
                  ((CurrentFileObserver) processor).onNextFile(currentFile);
              for (ParseConfigurationProcessor processor : parseConfigurationProcessors)
                if (processor instanceof CurrentFileObserver)
                  ((CurrentFileObserver) processor).onNextFile(currentFile);
            }

            if (sentence.getLeftoverOriginalText().length() > 0) {
              writer.append(sentence.getLeftoverOriginalText() + "\n");
            }

            for (SentenceProcessor sentenceProcessor : sentenceProcessors) {
              sentenceProcessor.onNextSentence(sentence);
            }
          } // need to read next sentence

          List<TokenSequence> tokenSequences = null;
          if (this.needsTokeniser()) {
            tokenSequences = tokeniser.tokenise(sentence);
            tokenSequence = tokenSequences.get(0);

            for (TokenSequenceProcessor tokenSequenceProcessor : tokenSequenceProcessors) {
              tokenSequenceProcessor.onNextTokenSequence(tokenSequence);
            }
          } // need to tokenise ?

          List<PosTagSequence> posTagSequences = null;
          if (this.needsPosTagger()) {
            posTagSequence = null;
            if (tokenSequences == null) {
              tokenSequences = new ArrayListNoNulls<>();
              tokenSequences.add(tokenSequence);
            }

            if (posTagger instanceof NonDeterministicPosTagger) {
              NonDeterministicPosTagger nonDeterministicPosTagger = (NonDeterministicPosTagger) posTagger;
              posTagSequences = nonDeterministicPosTagger.tagSentence(tokenSequences);
              posTagSequence = posTagSequences.get(0);
            } else {
              posTagSequence = posTagger.tagSentence(tokenSequence);
            }

            for (PosTagSequenceProcessor posTagSequenceProcessor : this.posTagSequenceProcessors) {
              posTagSequenceProcessor.onNextPosTagSequence(posTagSequence);
            }

            tokenSequence = null;
          } // need to postag

          if (this.needsParser()) {
            if (posTagSequences == null) {
              posTagSequences = new ArrayListNoNulls<>();
              posTagSequences.add(posTagSequence);
            }

            ParseConfiguration parseConfiguration = null;
            List<ParseConfiguration> parseConfigurations = null;
            try {
              if (parser instanceof NonDeterministicParser) {
                NonDeterministicParser nonDeterministicParser = (NonDeterministicParser) parser;
                parseConfigurations = nonDeterministicParser.parseSentence(posTagSequences);
                parseConfiguration = parseConfigurations.get(0);
              } else {
                parseConfiguration = parser.parseSentence(posTagSequence);
              }

              for (ParseConfigurationProcessor parseConfigurationProcessor : this.parseConfigurationProcessors) {
                parseConfigurationProcessor.onNextParseConfiguration(parseConfiguration);
              }
            } catch (Exception e) {
              LogUtils.logError(LOG, e);
              if (stopOnError)
                throw new RuntimeException(e);
            }
            posTagSequence = null;
          } // need to parse

          if (this.startModule.equals(Module.sentenceDetector) || this.startModule.equals(Module.tokeniser))
            needToProcess = !sentences.isEmpty();
          else if (this.startModule.equals(Module.posTagger))
            needToProcess = tokenSequence != null;
          else if (this.startModule.equals(Module.parser))
            needToProcess = posTagSequence != null;
        } // next sentence
      } // next character

      // Check if there's any leftover output to output!
      if (rollingTextBlock.getLeftoverOriginalText().length() > 0)
        writer.append(rollingTextBlock.getLeftoverOriginalText());
    } finally {
      IOException exception = null;
      try {
        reader.close();
        writer.flush();
      } catch (IOException e) {
        LogUtils.logError(LOG, e);
        exception = e;
      }
      for (SentenceProcessor processor : this.sentenceProcessors)
        try {
          processor.close();
        } catch (IOException e) {
          LogUtils.logError(LOG, e);
          exception = e;
        }
      for (TokenSequenceProcessor processor : this.tokenSequenceProcessors)
        try {
          processor.close();
        } catch (IOException e) {
          LogUtils.logError(LOG, e);
          exception = e;
        }
      for (PosTagSequenceProcessor processor : this.posTagSequenceProcessors) {
        try {
          processor.onCompleteAnalysis();
          processor.close();
        } catch (IOException e) {
          LogUtils.logError(LOG, e);
          exception = e;
        }
      }
      for (ParseConfigurationProcessor processor : this.parseConfigurationProcessors) {
        try {
          processor.onCompleteParse();
          processor.close();
        } catch (IOException e) {
          LogUtils.logError(LOG, e);
          exception = e;
        }
      }
      long endTime = System.currentTimeMillis();
      long totalTime = endTime - startTime;
      LOG.debug("Total time for Talismane.process(): " + totalTime);

      try {
        writer.close();
      } catch (IOException e) {
        LogUtils.logError(LOG, e);
        exception = e;
      }

      if (exception != null)
        throw exception;
    }
  }

  /**
   * Does this instance of Talismane need a sentence detector to perform the
   * requested processing.
   */
  private boolean needsSentenceDetector() {
    return startModule.compareTo(Module.sentenceDetector) <= 0 && endModule.compareTo(Module.sentenceDetector) >= 0;
  }

  /**
   * Does this instance of Talismane need a tokeniser to perform the requested
   * processing.
   */
  private boolean needsTokeniser() {
    return startModule.compareTo(Module.tokeniser) <= 0 && endModule.compareTo(Module.tokeniser) >= 0;
  }

  /**
   * Does this instance of Talismane need a pos tagger to perform the requested
   * processing.
   */
  private boolean needsPosTagger() {
    return startModule.compareTo(Module.posTagger) <= 0 && endModule.compareTo(Module.posTagger) >= 0;
  }

  /**
   * Does this instance of Talismane need a parser to perform the requested
   * processing.
   */
  private boolean needsParser() {
    return startModule.compareTo(Module.parser) <= 0 && endModule.compareTo(Module.parser) >= 0;
  }

  /**
   * If an error occurs during analysis, should Talismane stop immediately, or
   * try to keep going with the next sentence? Default is true (stop
   * immediately).
   */
  public boolean isStopOnError() {
    return stopOnError;
  }

  public Module getStartModule() {
    return startModule;
  }

  public Module getEndModule() {
    return endModule;
  }

  public boolean isProcessByDefault() {
    return processByDefault;
  }

  public int getSentenceCount() {
    return sentenceCount;
  }

  public List<SentenceProcessor> getSentenceProcessors() {
    return sentenceProcessors;
  }

  public List<TokenSequenceProcessor> getTokenSequenceProcessors() {
    return tokenSequenceProcessors;
  }

  public List<PosTagSequenceProcessor> getPosTagSequenceProcessors() {
    return posTagSequenceProcessors;
  }

  public List<ParseConfigurationProcessor> getParseConfigurationProcessors() {
    return parseConfigurationProcessors;
  }

}
