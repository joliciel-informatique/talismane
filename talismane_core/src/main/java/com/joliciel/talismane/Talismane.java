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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.filters.RollingSentenceProcessor;
import com.joliciel.talismane.filters.Sentence;
import com.joliciel.talismane.filters.SentenceHolder;
import com.joliciel.talismane.filters.TextMarker;
import com.joliciel.talismane.filters.TextMarkerFilter;
import com.joliciel.talismane.languageDetector.LanguageDetector;
import com.joliciel.talismane.languageDetector.LanguageDetectorProcessor;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.ClassificationModelTrainer;
import com.joliciel.talismane.machineLearning.ExternalResourceFinder;
import com.joliciel.talismane.machineLearning.MachineLearningModel;
import com.joliciel.talismane.machineLearning.ModelTrainerFactory;
import com.joliciel.talismane.machineLearning.perceptron.PerceptronClassificationModelTrainer;
import com.joliciel.talismane.machineLearning.perceptron.PerceptronModelTrainerObserver;
import com.joliciel.talismane.parser.NonDeterministicParser;
import com.joliciel.talismane.parser.ParseComparator;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.parser.ParseConfigurationProcessor;
import com.joliciel.talismane.parser.Parser;
import com.joliciel.talismane.parser.ParserEvaluator;
import com.joliciel.talismane.posTagger.NonDeterministicPosTagger;
import com.joliciel.talismane.posTagger.PosTagComparator;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTagSequenceProcessor;
import com.joliciel.talismane.posTagger.PosTagger;
import com.joliciel.talismane.posTagger.PosTaggerEvaluator;
import com.joliciel.talismane.sentenceDetector.SentenceDetector;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorEvaluator;
import com.joliciel.talismane.sentenceDetector.SentenceProcessor;
import com.joliciel.talismane.tokeniser.TokenComparator;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.TokenSequenceProcessor;
import com.joliciel.talismane.tokeniser.Tokeniser;
import com.joliciel.talismane.tokeniser.TokeniserAnnotatedCorpusReader;
import com.joliciel.talismane.tokeniser.TokeniserEvaluator;
import com.joliciel.talismane.tokeniser.patterns.PatternTokeniserTrainer;
import com.joliciel.talismane.utils.ArrayListNoNulls;
import com.joliciel.talismane.utils.CSVFormatter;
import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.WeightedOutcome;
import com.joliciel.talismane.utils.io.CurrentFileObserver;
import com.joliciel.talismane.utils.io.CurrentFileProvider;

/**
 * An interface for processing a Reader from {@link TalismaneConfig#getReader()}
 * and writing the analysis result to a Writer from
 * {@link TalismaneConfig#getWriter()}.<br/>
 * Not thread-safe: a single Talismane cannot be used by multiple threads
 * simultaneously.<br/>
 * The output format is determined by the processor corresponding to
 * {@link TalismaneConfig#getEndModule()}.<br/>
 * This is accomplished by calling {@link #process()}.<br/>
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
	private static final CSVFormatter CSV = new CSVFormatter();

	private Reader reader;
	private Writer writer;
	private LanguageDetectorProcessor languageDetectorProcessor;
	private SentenceProcessor sentenceProcessor;
	private TokenSequenceProcessor tokenSequenceProcessor;
	private PosTagSequenceProcessor posTagSequenceProcessor;
	private ParseConfigurationProcessor parseConfigurationProcessor;

	private boolean stopOnError = true;

	private final TalismaneConfig config;

	private final TalismaneSession talismaneSession;

	protected Talismane(TalismaneConfig config, TalismaneSession talismaneSession) {
		this.config = config;
		this.talismaneSession = talismaneSession;
	}

	/**
	 * Run the {@link Command} specified by {@link TalismaneConfig#getCommand()}
	 * .
	 */
	public void process() {
		long startTime = new Date().getTime();

		try {
			if (this.getLanguageDetectorProcessor() == null)
				this.setLanguageDetectorProcessor(config.getLanguageDetectorProcessor());
			if (this.getSentenceProcessor() == null)
				this.setSentenceProcessor(config.getSentenceProcessor());
			if (this.getTokenSequenceProcessor() == null)
				this.setTokenSequenceProcessor(config.getTokenSequenceProcessor());
			if (this.getPosTagSequenceProcessor() == null)
				this.setPosTagSequenceProcessor(config.getPosTagSequenceProcessor());
			if (this.getParseConfigurationProcessor() == null)
				this.setParseConfigurationProcessor(config.getParseConfigurationProcessor());

			// kick-off writer creation if required
			this.getWriter();

			switch (config.getCommand()) {
			case analyse:
				switch (config.getModule()) {
				case languageDetector:
					LanguageDetector languageDetector = config.getLanguageDetector();
					if (this.getLanguageDetectorProcessor() == null)
						throw new TalismaneException("Cannot analyse language detector output without a language detector processor!");

					while (config.getSentenceCorpusReader().hasNextSentence()) {
						String sentence = config.getSentenceCorpusReader().nextSentence();

						List<WeightedOutcome<Locale>> results = languageDetector.detectLanguages(sentence);
						this.getLanguageDetectorProcessor().onNextText(sentence, results, this.getWriter());
					}
					break;
				default:
					this.analyse(config);
				}

				break;
			case process:
				switch (config.getModule()) {
				case sentenceDetector:
					if (this.getSentenceProcessor() == null)
						throw new TalismaneException("Cannot process sentence detector output without a sentence processor!");

					while (config.getSentenceCorpusReader().hasNextSentence()) {
						String text = config.getSentenceCorpusReader().nextSentence();
						Sentence sentence = new Sentence(text, talismaneSession);
						this.getSentenceProcessor().onNextSentence(sentence, this.getWriter());
					}
					break;
				case tokeniser: {
					if (this.getTokenSequenceProcessor() == null)
						throw new TalismaneException("Cannot process tokeniser output without a token sequence processor!");
					TokeniserAnnotatedCorpusReader tokenCorpusReader = TokeniserAnnotatedCorpusReader.getCorpusReader(talismaneSession.getReader(),
							config.getConfig().getConfig("talismane.core.tokeniser.evaluate"), talismaneSession);
					while (tokenCorpusReader.hasNextTokenSequence()) {
						TokenSequence tokenSequence = tokenCorpusReader.nextTokenSequence();
						this.getTokenSequenceProcessor().onNextTokenSequence(tokenSequence, this.getWriter());
					}
					break;
				}
				case posTagger:
					if (this.getPosTagSequenceProcessor() == null)
						throw new TalismaneException("Cannot process pos-tagger output without a pos-tag sequence processor!");

					try {
						while (config.getPosTagCorpusReader().hasNextPosTagSequence()) {
							PosTagSequence posTagSequence = config.getPosTagCorpusReader().nextPosTagSequence();
							this.getPosTagSequenceProcessor().onNextPosTagSequence(posTagSequence, this.getWriter());
						}
					} finally {
						this.getPosTagSequenceProcessor().onCompleteAnalysis();
					}
					break;
				case parser:
					if (this.getParseConfigurationProcessor() == null)
						throw new TalismaneException("Cannot process parser output without a parse configuration processor!");
					try {
						if (config.getParserCorpusReader() instanceof CurrentFileObserver && config.getReader() instanceof CurrentFileProvider)
							((CurrentFileProvider) config.getReader()).addCurrentFileObserver((CurrentFileObserver) config.getParserCorpusReader());

						while (config.getParserCorpusReader().hasNextConfiguration()) {
							ParseConfiguration parseConfiguration = config.getParserCorpusReader().nextConfiguration();
							this.getParseConfigurationProcessor().onNextParseConfiguration(parseConfiguration, this.getWriter());
						}
					} finally {
						this.getParseConfigurationProcessor().onCompleteParse();
					}
					break;
				default:
					throw new TalismaneException("Command 'process' does not yet support module: " + config.getModule());
				}
				break;
			case evaluate:
				switch (config.getModule()) {
				case sentenceDetector:
					SentenceDetectorEvaluator sentenceDetectorEvaluator = config.getSentenceDetectorEvaluator();
					File sentenceErrorFile = new File(talismaneSession.getOutDir(), talismaneSession.getBaseName() + "_errors.txt");
					Writer sentenceErrorWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(sentenceErrorFile, false), "UTF8"));
					sentenceDetectorEvaluator.evaluate(config.getSentenceCorpusReader(), sentenceErrorWriter);
					sentenceErrorWriter.flush();
					sentenceErrorWriter.close();
					break;
				case tokeniser:
					TokeniserEvaluator tokeniserEvaluator = new TokeniserEvaluator(talismaneSession);
					tokeniserEvaluator.evaluate();
					break;
				case posTagger:
					PosTaggerEvaluator posTaggerEvaluator = config.getPosTaggerEvaluator();
					posTaggerEvaluator.evaluate(config.getPosTagCorpusReader());
					break;
				case parser:
					ParserEvaluator parserEvaluator = config.getParserEvaluator();
					parserEvaluator.evaluate(config.getParserCorpusReader());
					break;
				default:
					throw new TalismaneException("Command 'evaluate' does not yet support module: " + config.getModule());
				}
				break;
			case compare:
				switch (config.getModule()) {
				case tokeniser:
					TokenComparator tokenComparator = new TokenComparator(talismaneSession);
					tokenComparator.compare();
					break;
				case posTagger:
					PosTagComparator posTagComparator = config.getPosTagComparator();
					posTagComparator.evaluate(config.getPosTagCorpusReader(), config.getPosTagEvaluationCorpusReader());
					break;
				case parser:
					ParseComparator parseComparator = config.getParseComparator();
					parseComparator.evaluate(config.getParserCorpusReader(), config.getParserEvaluationCorpusReader());
					break;
				default:
					throw new TalismaneException("Command 'compare' does not yet support module: " + config.getModule());
				}
				break;
			case train:
				switch (config.getModule()) {
				case languageDetector: {
					File modelFile = new File(config.getLanguageModelFilePath());
					File modelDir = modelFile.getParentFile();
					modelDir.mkdirs();

					ModelTrainerFactory factory = new ModelTrainerFactory();
					ClassificationModelTrainer trainer = factory.constructTrainer(config.getConfig());

					ClassificationModel languageModel = trainer.trainModel(config.getClassificationEventStream(), config.getDescriptors());
					languageModel.setExternalResources(talismaneSession.getExternalResourceFinder().getExternalResources());
					languageModel.persist(modelFile);
					break;
				}
				case sentenceDetector: {
					File modelFile = new File(config.getSentenceModelFilePath());
					File modelDir = modelFile.getParentFile();
					modelDir.mkdirs();

					ModelTrainerFactory factory = new ModelTrainerFactory();
					ClassificationModelTrainer trainer = factory.constructTrainer(config.getConfig());

					ClassificationModel sentenceModel = trainer.trainModel(config.getClassificationEventStream(), config.getDescriptors());
					sentenceModel.setExternalResources(talismaneSession.getExternalResourceFinder().getExternalResources());
					sentenceModel.persist(modelFile);
					break;
				}
				case tokeniser: {
					PatternTokeniserTrainer trainer = new PatternTokeniserTrainer(talismaneSession);
					trainer.train();
					break;
				}
				case posTagger: {
					File modelFile = new File(config.getPosTaggerModelFilePath());
					File modelDir = modelFile.getParentFile();
					modelDir.mkdirs();
					if (config.getPerceptronObservationPoints().size() == 0) {
						ModelTrainerFactory factory = new ModelTrainerFactory();
						ClassificationModelTrainer trainer = factory.constructTrainer(config.getConfig());

						ClassificationModel posTaggerModel = trainer.trainModel(config.getClassificationEventStream(), config.getDescriptors());
						posTaggerModel.setExternalResources(talismaneSession.getExternalResourceFinder().getExternalResources());
						posTaggerModel.persist(modelFile);
					} else {
						PerceptronClassificationModelTrainer trainer = new PerceptronClassificationModelTrainer();

						String modelName = modelFile.getName().substring(0, modelFile.getName().lastIndexOf('.'));
						PerceptronModelTrainerObserver observer = new PosTaggerPerceptronModelPersister(modelDir, modelName,
								talismaneSession.getExternalResourceFinder());
						trainer.trainModelsWithObserver(config.getClassificationEventStream(), config.getDescriptors(), observer,
								config.getPerceptronObservationPoints());
					}
					break;
				}
				case parser: {
					MachineLearningModel parserModel = null;
					File modelFile = new File(config.getParserModelFilePath());
					File modelDir = modelFile.getParentFile();
					modelDir.mkdirs();

					boolean needToPersist = true;
					if (config.getPerceptronObservationPoints().size() == 0) {
						ModelTrainerFactory factory = new ModelTrainerFactory();
						ClassificationModelTrainer trainer = factory.constructTrainer(config.getConfig());

						parserModel = trainer.trainModel(config.getClassificationEventStream(), config.getDescriptors());
					} else {
						PerceptronClassificationModelTrainer trainer = new PerceptronClassificationModelTrainer();

						String modelName = modelFile.getName().substring(0, modelFile.getName().lastIndexOf('.'));

						PerceptronModelTrainerObserver observer = new ParserPerceptronModelPersister(modelDir, modelName,
								talismaneSession.getExternalResourceFinder());
						trainer.trainModelsWithObserver(config.getClassificationEventStream(), config.getDescriptors(), observer,
								config.getPerceptronObservationPoints());
						needToPersist = false;
					}

					if (needToPersist) {
						parserModel.setExternalResources(talismaneSession.getExternalResourceFinder().getExternalResources());
						parserModel.persist(modelFile);
					}
					break;
				}
				default:
					throw new TalismaneException("Command 'train' does not yet support module: " + config.getModule());
				}
				break;
			default:
				throw new TalismaneException("Unsupported command: " + config.getCommand());
			} // which command?
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		} finally {
			long endTime = new Date().getTime();
			long totalTime = endTime - startTime;
			LOG.debug("Total time for Talismane.process(): " + totalTime);

			if (config.isLogStats()) {
				try {
					Writer csvFileWriter = null;
					File csvFile = new File(talismaneSession.getOutDir(), talismaneSession.getBaseName() + ".stats.csv");

					csvFile.delete();
					csvFile.createNewFile();
					csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false), "UTF8"));
					csvFileWriter.write(CSV.format("total time") + CSV.format(totalTime) + "\n");
					csvFileWriter.flush();
					csvFileWriter.close();
				} catch (Exception e) {
					LogUtils.logError(LOG, e);
				}
			}
		}
	}

	public void analyse(TalismaneConfig config) throws ClassNotFoundException, ReflectiveOperationException {
		try {
			SentenceDetector sentenceDetector = null;
			Tokeniser tokeniser = null;
			PosTagger posTagger = null;
			Parser parser = null;
			if (config.needsSentenceDetector())
				sentenceDetector = config.getSentenceDetector();
			if (config.needsTokeniser())
				tokeniser = Tokeniser.getInstance(talismaneSession);
			if (config.needsPosTagger())
				posTagger = config.getPosTagger();
			if (config.needsParser())
				parser = config.getParser();

			TokeniserAnnotatedCorpusReader tokenCorpusReader = null;
			if (config.getStartModule().equals(Module.posTagger)) {
				// TODO: should not use the evaluate reader - need a standard
				// reader vs a compare reader
				tokenCorpusReader = TokeniserAnnotatedCorpusReader.getCorpusReader(talismaneSession.getReader(),
						config.getConfig().getConfig("talismane.core.tokeniser.evaluate"), talismaneSession);
			}

			if (config.getEndModule().equals(Module.sentenceDetector)) {
				if (this.getSentenceProcessor() == null) {
					throw new TalismaneException("No sentence processor provided with sentence detector end module, cannot generate output.");
				}
			}
			if (config.getEndModule().equals(Module.tokeniser)) {
				if (this.getTokenSequenceProcessor() == null) {
					throw new TalismaneException("No token sequence processor provided with tokeniser end module, cannot generate output.");
				}
			}
			if (config.getEndModule().equals(Module.posTagger)) {
				if (this.getPosTagSequenceProcessor() == null) {
					throw new TalismaneException("No postag sequence processor provided with pos-tagger end module, cannot generate output.");
				}
			}
			if (config.getEndModule().equals(Module.parser)) {
				if (this.getParseConfigurationProcessor() == null) {
					throw new TalismaneException("No parse configuration processor provided with parser end module, cannot generate output.");
				}
			}

			LinkedList<String> textSegments = new LinkedList<String>();
			LinkedList<Sentence> sentences = new LinkedList<Sentence>();
			TokenSequence tokenSequence = null;
			PosTagSequence posTagSequence = null;

			RollingSentenceProcessor rollingSentenceProcessor = new RollingSentenceProcessor(talismaneSession.getFileName(), config.isProcessByDefault(),
					talismaneSession);
			if (this.getReader() instanceof CurrentFileProvider) {
				((CurrentFileProvider) this.getReader()).addCurrentFileObserver(rollingSentenceProcessor);
			}

			Sentence leftover = null;
			if (config.getStartModule().equals(Module.sentenceDetector) || config.getStartModule().equals(Module.tokeniser)) {
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
				if (config.getStartModule().equals(Module.sentenceDetector) || config.getStartModule().equals(Module.tokeniser)) {
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
					if (c == config.getEndBlockCharacter()) {
						endBlockCharacterCount++;
						if (endBlockCharacterCount == 3) {
							LOG.info("Three consecutive end-block characters. Exiting.");
							finished = true;
						}
					} else {
						endBlockCharacterCount = 0;
					}

					// have sentence detector
					if (finished || (Character.isWhitespace(c) && c != '\r' && c != '\n' && stringBuilder.length() > config.getBlockSize())
							|| c == config.getEndBlockCharacter()) {
						if (c == config.getEndBlockCharacter())
							stringBuilder.append(c);
						if (stringBuilder.length() > 0) {
							String textSegment = stringBuilder.toString();
							stringBuilder = new StringBuilder();

							textSegments.add(textSegment);
						} // is the current block > 0 characters?
						if (c == config.getEndBlockCharacter()) {
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

					if (c != config.getEndBlockCharacter())
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
						for (TextMarkerFilter textMarkerFilter : config.getTextMarkerFilters()) {
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
						nextProcessedText = sentenceHolder.getText();

						if (LOG.isTraceEnabled()) {
							LOG.trace("prevProcessedText: " + prevProcessedText);
							LOG.trace("processedText: " + processedText);
							LOG.trace("nextProcessedText: " + nextProcessedText);
						}

						boolean reallyFinished = finished && textSegments.size() == 3;

						if (prevSentenceHolder != null) {
							if (config.getStartModule().equals(Module.sentenceDetector)) {
								List<Integer> sentenceBreaks = sentenceDetector.detectSentences(prevProcessedText, processedText, nextProcessedText);
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
							if (config.getMaxSentenceCount() > 0 && sentenceCount >= config.getMaxSentenceCount()) {
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
								Sentence sentence = new Sentence(talismaneSession);
								if (sentences.size() > 0)
									sentence.setFile(sentences.peek().getFile());
								StringBuilder segmentsToInsert = new StringBuilder();
								if (prevSentenceHolder.getLeftoverOriginalText() != null)
									segmentsToInsert.append(prevSentenceHolder.getLeftoverOriginalText());
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
				} else if (config.getStartModule().equals(Module.posTagger)) {
					if (tokenCorpusReader.hasNextTokenSequence()) {
						tokenSequence = tokenCorpusReader.nextTokenSequence();
					} else {
						tokenSequence = null;
						finished = true;
					}
				} else if (config.getStartModule().equals(Module.parser)) {
					if (config.getPosTagCorpusReader().hasNextPosTagSequence()) {
						posTagSequence = config.getPosTagCorpusReader().nextPosTagSequence();
					} else {
						posTagSequence = null;
						finished = true;
					}
				} // which start module?

				boolean needToProcess = false;
				if (config.getStartModule().equals(Module.sentenceDetector) || config.getStartModule().equals(Module.tokeniser))
					needToProcess = !sentences.isEmpty();
				else if (config.getStartModule().equals(Module.posTagger))
					needToProcess = tokenSequence != null;
				else if (config.getStartModule().equals(Module.parser))
					needToProcess = posTagSequence != null;

				while (needToProcess) {
					Sentence sentence = null;
					if (config.getStartModule().compareTo(Module.tokeniser) <= 0 && config.getEndModule().compareTo(Module.sentenceDetector) >= 0) {
						sentence = sentences.poll();
						LOG.debug("Sentence: " + sentence);
						if (sentence.getLeftoverOriginalText() != null) {
							this.getWriter().append(sentence.getLeftoverOriginalText() + "\n");
						}
						if (this.getWriter() instanceof CurrentFileObserver && sentence.getFile() != null && !sentence.getFile().equals(currentFile)) {
							currentFile = sentence.getFile();
							LOG.debug("Setting current file to " + currentFile.getPath());
							((CurrentFileObserver) this.getWriter()).onNextFile(currentFile);
						}

						if (this.getSentenceProcessor() != null)
							this.getSentenceProcessor().onNextSentence(sentence, this.getWriter());
					} // need to read next sentence

					List<TokenSequence> tokenSequences = null;
					if (config.needsTokeniser()) {
						tokenSequences = tokeniser.tokenise(sentence);
						tokenSequence = tokenSequences.get(0);

						if (this.getTokenSequenceProcessor() != null) {
							this.getTokenSequenceProcessor().onNextTokenSequence(tokenSequence, this.getWriter());
						}
					} // need to tokenise ?

					List<PosTagSequence> posTagSequences = null;
					if (config.needsPosTagger()) {
						posTagSequence = null;
						if (tokenSequences == null || !config.isPropagateTokeniserBeam()) {
							tokenSequences = new ArrayListNoNulls<TokenSequence>();
							tokenSequences.add(tokenSequence);
						}

						if (posTagger instanceof NonDeterministicPosTagger) {
							NonDeterministicPosTagger nonDeterministicPosTagger = (NonDeterministicPosTagger) posTagger;
							posTagSequences = nonDeterministicPosTagger.tagSentence(tokenSequences);
							posTagSequence = posTagSequences.get(0);
						} else {
							posTagSequence = posTagger.tagSentence(tokenSequence);
						}

						if (posTagSequenceProcessor != null) {
							posTagSequenceProcessor.onNextPosTagSequence(posTagSequence, this.getWriter());
						}

						tokenSequence = null;
					} // need to postag

					if (config.needsParser()) {
						if (posTagSequences == null || !config.isPropagatePosTaggerBeam()) {
							posTagSequences = new ArrayListNoNulls<PosTagSequence>();
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

							if (this.getParseConfigurationProcessor() != null) {
								this.getParseConfigurationProcessor().onNextParseConfiguration(parseConfiguration, this.getWriter());
							}
						} catch (Exception e) {
							LogUtils.logError(LOG, e);
							if (stopOnError)
								throw new RuntimeException(e);
						}
						posTagSequence = null;
					} // need to parse

					if (config.getStartModule().equals(Module.sentenceDetector) || config.getStartModule().equals(Module.tokeniser))
						needToProcess = !sentences.isEmpty();
					else if (config.getStartModule().equals(Module.posTagger))
						needToProcess = tokenSequence != null;
					else if (config.getStartModule().equals(Module.parser))
						needToProcess = posTagSequence != null;
				} // next sentence
			} // next character

			// Check if there's any leftover output to output!
			if (prevSentenceHolder != null && prevSentenceHolder.getOriginalTextSegments().size() > 0) {
				for (String segment : prevSentenceHolder.getOriginalTextSegments().values()) {
					this.getWriter().append(segment);
				}
			}
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		} finally {
			if (this.getParseConfigurationProcessor() != null) {
				this.getParseConfigurationProcessor().onCompleteParse();
			}

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
	 * The language detector processor to be used if the end-module is the
	 * language detector.
	 */
	public LanguageDetectorProcessor getLanguageDetectorProcessor() {
		return languageDetectorProcessor;
	}

	public void setLanguageDetectorProcessor(LanguageDetectorProcessor languageDetectorProcessor) {
		this.languageDetectorProcessor = languageDetectorProcessor;
	}

	/**
	 * The sentence processor to be used if the end-module is the sentence
	 * detector.
	 */
	public SentenceProcessor getSentenceProcessor() {
		return sentenceProcessor;
	}

	public void setSentenceProcessor(SentenceProcessor sentenceProcessor) {
		this.sentenceProcessor = sentenceProcessor;
	}

	/**
	 * The token sequence processor to be used if the end-module is the
	 * tokeniser.
	 */
	public TokenSequenceProcessor getTokenSequenceProcessor() {
		return tokenSequenceProcessor;
	}

	public void setTokenSequenceProcessor(TokenSequenceProcessor tokenSequenceProcessor) {
		this.tokenSequenceProcessor = tokenSequenceProcessor;
	}

	/**
	 * The pos-tag sequence processor to be used if the end-module is the
	 * pos-tagger.
	 */
	public PosTagSequenceProcessor getPosTagSequenceProcessor() {
		return posTagSequenceProcessor;
	}

	public void setPosTagSequenceProcessor(PosTagSequenceProcessor posTagSequenceProcessor) {
		this.posTagSequenceProcessor = posTagSequenceProcessor;
	}

	/**
	 * The parse configuration processor to be used if the end-module is the
	 * parser.
	 */
	public ParseConfigurationProcessor getParseConfigurationProcessor() {
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

	public void setStopOnError(boolean stopOnError) {
		this.stopOnError = stopOnError;
	}

	/**
	 * The reader to be used for input by this instance of Talismane.
	 */
	public Reader getReader() {
		if (this.reader == null)
			this.reader = config.getReader();
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
			this.writer = talismaneSession.getWriter();
		return writer;
	}

	public void setWriter(Writer writer) {
		this.writer = writer;
	}

	private static final class PosTaggerPerceptronModelPersister implements PerceptronModelTrainerObserver {
		File outDir;
		ExternalResourceFinder externalResourceFinder;
		String baseName;

		public PosTaggerPerceptronModelPersister(File outDir, String baseName, ExternalResourceFinder externalResourceFinder) {
			super();
			this.outDir = outDir;
			this.baseName = baseName;
			this.externalResourceFinder = externalResourceFinder;
		}

		@Override
		public void onNextModel(ClassificationModel model, int iterations) {
			this.outDir.mkdirs();
			File posTaggerModelFile = new File(outDir, baseName + "_i" + iterations + ".zip");
			if (externalResourceFinder != null)
				model.setExternalResources(externalResourceFinder.getExternalResources());
			LOG.info("Writing model " + posTaggerModelFile.getName());
			model.persist(posTaggerModelFile);
		}
	}

	private static final class ParserPerceptronModelPersister implements PerceptronModelTrainerObserver {
		File outDir;
		ExternalResourceFinder externalResourceFinder;
		String baseName;

		public ParserPerceptronModelPersister(File outDir, String baseName, ExternalResourceFinder externalResourceFinder) {
			super();
			this.outDir = outDir;
			this.baseName = baseName;
			this.externalResourceFinder = externalResourceFinder;
		}

		@Override
		public void onNextModel(ClassificationModel model, int iterations) {
			this.outDir.mkdirs();
			File parserModelFile = new File(outDir, baseName + "_i" + iterations + ".zip");
			if (externalResourceFinder != null)
				model.setExternalResources(externalResourceFinder.getExternalResources());
			LOG.info("Writing model " + parserModelFile.getName());
			model.persist(parserModelFile);
		}
	}
}
