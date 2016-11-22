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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.filters.RollingSentenceProcessor;
import com.joliciel.talismane.filters.Sentence;
import com.joliciel.talismane.filters.SentenceHolder;
import com.joliciel.talismane.filters.TextMarker;
import com.joliciel.talismane.filters.TextMarkerFilter;
import com.joliciel.talismane.parser.NonDeterministicParser;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.parser.ParseConfigurationProcessor;
import com.joliciel.talismane.parser.Parser;
import com.joliciel.talismane.parser.Parsers;
import com.joliciel.talismane.posTagger.NonDeterministicPosTagger;
import com.joliciel.talismane.posTagger.PosTagAnnotatedCorpusReader;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTagSequenceProcessor;
import com.joliciel.talismane.posTagger.PosTagger;
import com.joliciel.talismane.posTagger.PosTaggers;
import com.joliciel.talismane.sentenceDetector.RollingTextBlock;
import com.joliciel.talismane.sentenceDetector.SentenceDetector;
import com.joliciel.talismane.sentenceDetector.SentenceProcessor;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.TokenSequenceProcessor;
import com.joliciel.talismane.tokeniser.Tokeniser;
import com.joliciel.talismane.tokeniser.TokeniserAnnotatedCorpusReader;
import com.joliciel.talismane.utils.ArrayListNoNulls;
import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.io.CurrentFileObserver;
import com.joliciel.talismane.utils.io.CurrentFileProvider;
import com.typesafe.config.Config;

/**
 * An interface for processing a Reader from
 * {@link TalismaneSession#getReader()} and writing the analysis result to a
 * Writer from {@link TalismaneSession#getWriter()}.<br/>
 * Not thread-safe: a single Talismane cannot be used by multiple threads
 * simultaneously.<br/>
 * The output format is determined by the processor corresponding to
 * {@link #getEndModule()}.<br/>
 * This is accomplished by calling {@link #analyse()}.<br/>
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
		 * Train a model using a corpus, a feature set, a classifier +
		 * parameters, etc.
		 */
		train,
		/**
		 * Analyse a corpus and add annotations.
		 */
		analyse,
		/**
		 * Evaluate an annotated corpus, by re-analysing the corpus and
		 * comparing the new annotations to the existing ones.
		 */
		evaluate,
		/**
		 * Process an annotated corpus - Talismane simply reads the corpus using
		 * the appropriate corpus reader and passes the results to the
		 * appropriate processors.
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
		 * Include extra columns for user-supplied comments in the training
		 * corpus.
		 */
		with_comments
	}

	private static final Logger LOG = LoggerFactory.getLogger(Talismane.class);

	private Reader reader;
	private Writer writer;
	private SentenceProcessor sentenceProcessor;
	private TokenSequenceProcessor tokenSequenceProcessor;
	private PosTagSequenceProcessor posTagSequenceProcessor;
	private ParseConfigurationProcessor parseConfigurationProcessor;

	private final boolean stopOnError;

	private final Config config;
	private final TalismaneSession session;
	private final Module startModule;
	private final Module endModule;

	private final boolean processByDefault;

	private final int sentenceCount;
	private final TokeniserAnnotatedCorpusReader tokenCorpusReader;
	private final PosTagAnnotatedCorpusReader posTagCorpusReader;

	public Talismane(TalismaneSession session) throws IOException, ReflectiveOperationException {
		this.session = session;
		this.config = session.getConfig();
		Config analyseConfig = config.getConfig("talismane.core.analysis");

		this.startModule = Module.valueOf(analyseConfig.getString("start-module"));
		this.endModule = Module.valueOf(analyseConfig.getString("end-module"));

		if (startModule.compareTo(endModule) > 0) {
			throw new TalismaneException("Start-module (" + startModule.name() + ") cannot come after end-module (" + endModule.name() + ")");
		}

		this.processByDefault = analyseConfig.getBoolean("process-by-default");
		this.stopOnError = analyseConfig.getBoolean("stop-on-error");
		this.sentenceCount = config.getInt("talismane.core.input.sentence-count");

		if (this.startModule.equals(Module.posTagger)) {
			tokenCorpusReader = TokeniserAnnotatedCorpusReader.getCorpusReader(session.getReader(), config.getConfig("talismane.core.tokeniser.input"),
					session);
		} else {
			tokenCorpusReader = null;
		}

		if (this.startModule.equals(Module.parser)) {
			posTagCorpusReader = PosTagAnnotatedCorpusReader.getCorpusReader(session.getReader(), config.getConfig("talismane.core.pos-tagger.input"), session);
		} else {
			posTagCorpusReader = null;
		}
	}

	public void analyse() throws IOException {
		long startTime = System.currentTimeMillis();
		ParseConfigurationProcessor parseConfigurationProcessor = this.getParseConfigurationProcessor();
		try {
			SentenceDetector sentenceDetector = null;
			Tokeniser tokeniser = null;
			PosTagger posTagger = null;
			Parser parser = null;
			if (this.needsSentenceDetector())
				sentenceDetector = SentenceDetector.getInstance(session);
			if (this.needsTokeniser())
				tokeniser = Tokeniser.getInstance(session);
			if (this.needsPosTagger())
				posTagger = PosTaggers.getPosTagger(session);
			if (this.needsParser())
				parser = Parsers.getParser(session);

			LinkedList<String> textSegments = new LinkedList<String>();
			LinkedList<Sentence> sentences = new LinkedList<Sentence>();
			TokenSequence tokenSequence = null;
			PosTagSequence posTagSequence = null;

			RollingSentenceProcessor rollingSentenceProcessor = new RollingSentenceProcessor(session.getFileName(), this.processByDefault, session);
			if (this.getReader() instanceof CurrentFileProvider) {
				((CurrentFileProvider) this.getReader()).addCurrentFileObserver(rollingSentenceProcessor);
			}

			Sentence leftover = null;
			if (this.startModule.equals(Module.sentenceDetector) || this.startModule.equals(Module.tokeniser)) {
				// prime the sentence detector with two text segments, to ensure
				// everything gets processed
				textSegments.addLast("");
				textSegments.addLast("");
			}

			StringBuilder stringBuilder = new StringBuilder();
			boolean finished = false;
			int sentenceCount = 0;

			String prevProcessedText = "";
			String processedText = "";
			String nextProcessedText = "";
			SentenceHolder prevSentenceHolder = null;

			int endBlockCharacterCount = 0;

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
						r = this.getReader().read();
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
					if (c == session.getEndBlockCharacter()) {
						endBlockCharacterCount++;
						if (endBlockCharacterCount == 3) {
							LOG.info("Three consecutive end-block characters. Exiting.");
							finished = true;
						}
					} else {
						endBlockCharacterCount = 0;
					}

					// have sentence detector
					if (finished || (Character.isWhitespace(c) && c != '\r' && c != '\n' && stringBuilder.length() > session.getBlockSize())
							|| c == session.getEndBlockCharacter()) {
						if (c == session.getEndBlockCharacter())
							stringBuilder.append(c);
						if (stringBuilder.length() > 0) {
							String textSegment = stringBuilder.toString();
							stringBuilder = new StringBuilder();

							textSegments.add(textSegment);
						} // is the current block > 0 characters?
						if (c == session.getEndBlockCharacter()) {
							textSegments.addLast("");
						}
					} // is there a next block available?

					if (finished) {
						if (stringBuilder.length() > 0) {
							textSegments.addLast(stringBuilder.toString());
							stringBuilder = new StringBuilder();
						}
						textSegments.addLast("");
						textSegments.addLast("");
						textSegments.addLast("");
					}

					if (c != session.getEndBlockCharacter())
						stringBuilder.append(c);

					while (textSegments.size() >= 3) {
						String prevText = textSegments.removeFirst();
						String text = textSegments.removeFirst();
						String nextText = textSegments.removeFirst();
						if (LOG.isTraceEnabled()) {
							LOG.trace("prevText: " + prevText.replace('\n', '¶').replace('\r', '¶'));
							LOG.trace("text: " + text.replace('\n', '¶').replace('\r', '¶'));
							LOG.trace("nextText: " + nextText.replace('\n', '¶').replace('\r', '¶'));
						}

						Set<TextMarker> textMarkers = new TreeSet<TextMarker>();
						for (TextMarkerFilter textMarkerFilter : session.getTextFilters()) {
							Set<TextMarker> result = textMarkerFilter.apply(prevText, text, nextText);
							textMarkers.addAll(result);
						}

						// push the text segments back onto the beginning of
						// Deque
						textSegments.addFirst(nextText);
						textSegments.addFirst(text);

						SentenceHolder sentenceHolder = rollingSentenceProcessor.addNextSegment(text, textMarkers);
						prevProcessedText = processedText;
						processedText = nextProcessedText;
						nextProcessedText = sentenceHolder.getProcessedText().toString();

						if (LOG.isTraceEnabled()) {
							LOG.trace("prevProcessedText: " + prevProcessedText);
							LOG.trace("processedText: " + processedText);
							LOG.trace("nextProcessedText: " + nextProcessedText);
						}

						boolean reallyFinished = finished && textSegments.size() == 3;

						if (prevSentenceHolder != null) {
							if (this.startModule.equals(Module.sentenceDetector)) {
								RollingTextBlock textBlock = new RollingTextBlock(prevProcessedText, processedText, nextProcessedText);
								for (Annotator annotator : session.getTextAnnotators())
									annotator.annotate(textBlock);

								List<Integer> sentenceBreaks = sentenceDetector.detectSentences(textBlock);
								for (int sentenceBreak : sentenceBreaks) {
									prevSentenceHolder.addSentenceBoundary(sentenceBreak);
								}
							}

							List<Sentence> theSentences = prevSentenceHolder.getDetectedSentences(leftover);
							leftover = null;
							for (Sentence sentence : theSentences) {
								if (sentence.isComplete() || reallyFinished) {
									sentences.add(sentence);
									sentenceCount++;
								} else {
									LOG.debug("Setting leftover to: " + sentence.getText());
									leftover = sentence;
								}
							}
							if (this.sentenceCount > 0 && sentenceCount >= this.sentenceCount) {
								finished = true;
							}

							// If we have any leftover original text segments,
							// copy them over
							// they are necessarily at position 0 - since
							// otherwise they would
							// have gotten added to the leftover sentence. The
							// only case where
							// there isn't a leftover sentence is the case where
							// the sentenceHolder
							// boundary happens to be a sentence boundary, hence
							// position 0.
							if (prevSentenceHolder.getOriginalTextSegments().size() > 0) {
								String fileName = "";
								File file = null;
								if (sentences.size() > 0) {
									Sentence lastSentence = sentences.peek();
									fileName = lastSentence.getFileName();
									file = lastSentence.getFile();
								}

								Sentence sentence = new Sentence("", fileName, file, session);
								StringBuilder segmentsToInsert = new StringBuilder();

								for (String originalTextSegment : prevSentenceHolder.getOriginalTextSegments().values()) {
									segmentsToInsert.append(originalTextSegment);
								}
								sentence.setLeftoverOriginalText(segmentsToInsert.toString());
								sentences.add(sentence);
							}
						}
						prevSentenceHolder = sentenceHolder;
					} // we have at least 3 text segments (should always be the
						// case once we get started)
				} else if (this.startModule.equals(Module.posTagger)) {
					if (tokenCorpusReader.hasNextTokenSequence()) {
						tokenSequence = tokenCorpusReader.nextTokenSequence();
					} else {
						tokenSequence = null;
						finished = true;
					}
				} else if (this.startModule.equals(Module.parser)) {
					if (posTagCorpusReader.hasNextPosTagSequence()) {
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

						for (Annotator annotator : session.getTextAnnotators())
							annotator.annotate(sentence);

						if (sentence.getLeftoverOriginalText() != null) {
							this.getWriter().append(sentence.getLeftoverOriginalText() + "\n");
						}
						if (this.getWriter() instanceof CurrentFileObserver && sentence.getFile() != null && !sentence.getFile().equals(currentFile)) {
							currentFile = sentence.getFile();
							LOG.debug("Setting current file to " + currentFile.getPath());
							((CurrentFileObserver) this.getWriter()).onNextFile(currentFile);
						}

						if (this.endModule == Module.sentenceDetector)
							this.getSentenceProcessor().onNextSentence(sentence, this.getWriter());
					} // need to read next sentence

					List<TokenSequence> tokenSequences = null;
					if (this.needsTokeniser()) {
						tokenSequences = tokeniser.tokenise(sentence);
						tokenSequence = tokenSequences.get(0);

						if (this.endModule == Module.tokeniser) {
							this.getTokenSequenceProcessor().onNextTokenSequence(tokenSequence, this.getWriter());
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

						if (this.endModule == Module.posTagger) {
							posTagSequenceProcessor.onNextPosTagSequence(posTagSequence, this.getWriter());
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

							if (this.endModule == Module.parser) {
								this.getParseConfigurationProcessor().onNextParseConfiguration(parseConfiguration, this.getWriter());
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
			if (prevSentenceHolder != null && prevSentenceHolder.getOriginalTextSegments().size() > 0) {
				for (String segment : prevSentenceHolder.getOriginalTextSegments().values()) {
					this.getWriter().append(segment);
				}
			}
		} finally {
			if (parseConfigurationProcessor != null)
				parseConfigurationProcessor.onCompleteParse();

			long endTime = System.currentTimeMillis();
			long totalTime = endTime - startTime;
			LOG.debug("Total time for Talismane.process(): " + totalTime);

			try {
				this.getReader().close();
				this.getWriter().flush();
				this.getWriter().close();
			} catch (IOException ioe2) {
				LogUtils.logError(LOG, ioe2);
				throw new RuntimeException(ioe2);
			}
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
	 * Does this instance of Talismane need a pos tagger to perform the
	 * requested processing.
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
	 * The sentence processor to be used if the end-module is the sentence
	 * detector.
	 * 
	 * @throws IOException
	 */
	public SentenceProcessor getSentenceProcessor() throws IOException {
		if (this.sentenceProcessor == null)
			this.sentenceProcessor = SentenceProcessor.getProcessor(session);
		return sentenceProcessor;
	}

	public void setSentenceProcessor(SentenceProcessor sentenceProcessor) {
		this.sentenceProcessor = sentenceProcessor;
	}

	/**
	 * The token sequence processor to be used if the end-module is the
	 * tokeniser.
	 * 
	 * @throws IOException
	 */
	public TokenSequenceProcessor getTokenSequenceProcessor() throws IOException {
		if (this.tokenSequenceProcessor == null)
			this.tokenSequenceProcessor = TokenSequenceProcessor.getProcessor(session);
		return tokenSequenceProcessor;
	}

	public void setTokenSequenceProcessor(TokenSequenceProcessor tokenSequenceProcessor) {
		this.tokenSequenceProcessor = tokenSequenceProcessor;
	}

	/**
	 * The pos-tag sequence processor to be used if the end-module is the
	 * pos-tagger.
	 * 
	 * @throws IOException
	 */
	public PosTagSequenceProcessor getPosTagSequenceProcessor() throws IOException {
		if (this.posTagSequenceProcessor == null)
			this.posTagSequenceProcessor = PosTagSequenceProcessor.getProcessor(session);
		return posTagSequenceProcessor;
	}

	public void setPosTagSequenceProcessor(PosTagSequenceProcessor posTagSequenceProcessor) {
		this.posTagSequenceProcessor = posTagSequenceProcessor;
	}

	/**
	 * The parse configuration processor to be used if the end-module is the
	 * parser.
	 * 
	 * @throws IOException
	 */
	public ParseConfigurationProcessor getParseConfigurationProcessor() throws IOException {
		if (this.parseConfigurationProcessor == null)
			this.parseConfigurationProcessor = ParseConfigurationProcessor.getProcessor(session);
		return parseConfigurationProcessor;
	}

	public void setParseConfigurationProcessor(ParseConfigurationProcessor parseConfigurationProcessor) {
		this.parseConfigurationProcessor = parseConfigurationProcessor;
	}

	/**
	 * If an error occurs during analysis, should Talismane stop immediately, or
	 * try to keep going with the next sentence? Default is true (stop
	 * immediately).
	 */
	public boolean isStopOnError() {
		return stopOnError;
	}

	/**
	 * The reader to be used for input by this instance of Talismane.
	 * 
	 * @throws IOException
	 */
	public Reader getReader() throws IOException {
		if (this.reader == null)
			this.reader = session.getReader();
		return reader;
	}

	public void setReader(Reader reader) {
		this.reader = reader;
	}

	/**
	 * The writer to be used for output by this instance of Talismane.
	 * 
	 * @throws IOException
	 */
	public Writer getWriter() throws IOException {
		if (this.writer == null)
			this.writer = session.getWriter();
		return writer;
	}

	public void setWriter(Writer writer) {
		this.writer = writer;
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

}
