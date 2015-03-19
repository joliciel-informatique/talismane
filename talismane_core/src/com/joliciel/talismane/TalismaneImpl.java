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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.filters.FilterService;
import com.joliciel.talismane.filters.RollingSentenceProcessor;
import com.joliciel.talismane.filters.Sentence;
import com.joliciel.talismane.filters.SentenceHolder;
import com.joliciel.talismane.filters.TextMarker;
import com.joliciel.talismane.filters.TextMarkerFilter;
import com.joliciel.talismane.languageDetector.LanguageDetector;
import com.joliciel.talismane.languageDetector.LanguageDetectorProcessor;
import com.joliciel.talismane.languageDetector.LanguageDetectorService;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.ClassificationModelTrainer;
import com.joliciel.talismane.machineLearning.ExternalResourceFinder;
import com.joliciel.talismane.machineLearning.MachineLearningAlgorithm;
import com.joliciel.talismane.machineLearning.MachineLearningModel;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.machineLearning.MachineLearningServiceLocator;
import com.joliciel.talismane.machineLearning.Ranker;
import com.joliciel.talismane.machineLearning.RankingEventStream;
import com.joliciel.talismane.machineLearning.RankingModelTrainer;
import com.joliciel.talismane.machineLearning.MachineLearningModel.MachineLearningModelType;
import com.joliciel.talismane.machineLearning.perceptron.PerceptronClassificationModelTrainer;
import com.joliciel.talismane.machineLearning.perceptron.PerceptronModelTrainerObserver;
import com.joliciel.talismane.machineLearning.perceptron.PerceptronService;
import com.joliciel.talismane.machineLearning.perceptron.PerceptronServiceLocator;
import com.joliciel.talismane.parser.NonDeterministicParser;
import com.joliciel.talismane.parser.ParseComparator;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.parser.ParseConfigurationProcessor;
import com.joliciel.talismane.parser.ParserEvaluator;
import com.joliciel.talismane.parser.ParserService;
import com.joliciel.talismane.parser.ParsingConstrainer;
import com.joliciel.talismane.posTagger.NonDeterministicPosTagger;
import com.joliciel.talismane.posTagger.PosTagComparator;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTagSequenceProcessor;
import com.joliciel.talismane.posTagger.PosTaggerEvaluator;
import com.joliciel.talismane.posTagger.PosTaggerService;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorEvaluator;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorService;
import com.joliciel.talismane.sentenceDetector.SentenceProcessor;
import com.joliciel.talismane.tokeniser.TokenComparator;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.TokenSequenceProcessor;
import com.joliciel.talismane.tokeniser.TokeniserEvaluator;
import com.joliciel.talismane.tokeniser.TokeniserService;
import com.joliciel.talismane.tokeniser.patterns.TokeniserPatternService.PatternTokeniserType;
import com.joliciel.talismane.utils.ArrayListNoNulls;
import com.joliciel.talismane.utils.CSVFormatter;
import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.PerformanceMonitor;
import com.joliciel.talismane.utils.WeightedOutcome;
import com.joliciel.talismane.utils.io.CurrentFileObserver;
import com.joliciel.talismane.utils.io.CurrentFileProvider;

/**
 * The default implementation for the Talismane interface.
 * @author Assaf Urieli
 */
class TalismaneImpl implements Talismane {
	private static final Log LOG = LogFactory.getLog(TalismaneImpl.class);
	private static final CSVFormatter CSV = new CSVFormatter();
	private static final PerformanceMonitor MONITOR = PerformanceMonitor.getMonitor(TalismaneImpl.class);

	private Reader reader;
	private Writer writer;
	private LanguageDetectorProcessor languageDetectorProcessor;
	private SentenceProcessor sentenceProcessor;
	private TokenSequenceProcessor tokenSequenceProcessor;
	private PosTagSequenceProcessor posTagSequenceProcessor;
	private ParseConfigurationProcessor parseConfigurationProcessor;
	
	private TalismaneService talismaneService;
	private SentenceDetectorService sentenceDetectorService;
	private TokeniserService tokeniserService;
	private PosTaggerService posTaggerService;
	private ParserService parserService;
	private FilterService filterService;
	private MachineLearningService machineLearningService;
	private LanguageDetectorService languageDetectorService;
	
	private boolean stopOnError = true;

	private TalismaneConfig config;
	
	protected TalismaneImpl(TalismaneConfig config) {
		this.config = config;
	}

	@Override
	public void process() {
		long startTime = new Date().getTime();

		PerformanceMonitor.start(config.getPerformanceConfigFile());
		try {
			if (this.getLanguageDetectorProcessor()==null)
				this.setLanguageDetectorProcessor(config.getLanguageDetectorProcessor());
			if (this.getSentenceProcessor()==null)
				this.setSentenceProcessor(config.getSentenceProcessor());
			if (this.getTokenSequenceProcessor()==null)
				this.setTokenSequenceProcessor(config.getTokenSequenceProcessor());
			if (this.getPosTagSequenceProcessor()==null)
				this.setPosTagSequenceProcessor(config.getPosTagSequenceProcessor());
			if (this.getParseConfigurationProcessor()==null)
				this.setParseConfigurationProcessor(config.getParseConfigurationProcessor());
			
			// kick-off writer creation if required
			this.getWriter();
			
			switch (config.getCommand()) {
			case analyse:
				MONITOR.startTask("analyse");
				try {
					switch (config.getModule()) {
					case LanguageDetector:
						LanguageDetector languageDetector = config.getLanguageDetector();
						if (this.getLanguageDetectorProcessor()==null)
							throw new TalismaneException("Cannot analyse language detector output without a language detector processor!");
						
						while (config.getSentenceCorpusReader().hasNextSentence()) {
							String sentence = config.getSentenceCorpusReader().nextSentence();
							MONITOR.startTask("processLanguages");
							try {
								List<WeightedOutcome<Locale>> results = languageDetector.detectLanguages(sentence);
								this.getLanguageDetectorProcessor().onNextText(sentence, results, this.getWriter());
							} finally {
								MONITOR.endTask();
							}
						}
						break;
					default:
						this.analyse(config);						
					}
				} finally {
					MONITOR.endTask();
				}
				break;
			case process:
				MONITOR.startTask("process");
				try {
					switch (config.getModule()) {
					case SentenceDetector:
						if (this.getSentenceProcessor()==null)
							throw new TalismaneException("Cannot process sentence detector output without a sentence processor!");
						
						while (config.getSentenceCorpusReader().hasNextSentence()) {
							String text = config.getSentenceCorpusReader().nextSentence();
							Sentence sentence = this.filterService.getSentence(text);
							MONITOR.startTask("processSentence");
							try {
								this.getSentenceProcessor().onNextSentence(sentence, this.getWriter());
							} finally {
								MONITOR.endTask();
							}
						}
						break;
					case Tokeniser:
						if (this.getTokenSequenceProcessor()==null)
							throw new TalismaneException("Cannot process tokeniser output without a token sequence processor!");
						
						while (config.getTokenCorpusReader().hasNextTokenSequence()) {
							TokenSequence tokenSequence = config.getTokenCorpusReader().nextTokenSequence();
							MONITOR.startTask("processTokenSequence");
							try {
								this.getTokenSequenceProcessor().onNextTokenSequence(tokenSequence, this.getWriter());
							} finally {
								MONITOR.endTask();
							}
						}
						break;
					case PosTagger:
						if (this.getPosTagSequenceProcessor()==null)
							throw new TalismaneException("Cannot process pos-tagger output without a pos-tag sequence processor!");
						
						try {
							while (config.getPosTagCorpusReader().hasNextPosTagSequence()) {
								PosTagSequence posTagSequence = config.getPosTagCorpusReader().nextPosTagSequence();
								MONITOR.startTask("processPosTagSequence");
								try {
									this.getPosTagSequenceProcessor().onNextPosTagSequence(posTagSequence, this.getWriter());
								} finally {
									MONITOR.endTask();
								}
							}
						} finally {
							this.getPosTagSequenceProcessor().onCompleteAnalysis();
						}
						break;
					case Parser:
						if (this.getParseConfigurationProcessor()==null)
							throw new TalismaneException("Cannot process parser output without a parse configuration processor!");
						try {
							if (config.getParserCorpusReader() instanceof CurrentFileObserver && config.getReader() instanceof CurrentFileProvider)
								((CurrentFileProvider) config.getReader()).addCurrentFileObserver((CurrentFileObserver) config.getParserCorpusReader());
							
							while (config.getParserCorpusReader().hasNextConfiguration()) {
								ParseConfiguration parseConfiguration = config.getParserCorpusReader().nextConfiguration();
								MONITOR.startTask("processParseConfiguration");
								try {
									this.getParseConfigurationProcessor().onNextParseConfiguration(parseConfiguration, this.getWriter());
								} finally {
									MONITOR.endTask();
								}
							}
						} finally {
							this.getParseConfigurationProcessor().onCompleteParse();
						}
						break;
					default:
						throw new TalismaneException("Command 'process' does not yet support module: " + config.getModule());
					}
				} finally {
					MONITOR.endTask();
				}
				break;
			case evaluate:
				MONITOR.startTask("evaluate");
				try {
					switch (config.getModule()) {
					case SentenceDetector:
						SentenceDetectorEvaluator sentenceDetectorEvaluator = config.getSentenceDetectorEvaluator();
						MONITOR.startTask("sentenceDetectorEvaluate");
						try {
							File sentenceErrorFile = new File(config.getOutDir(), config.getBaseName() + "_errors.txt");
							Writer sentenceErrorWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(sentenceErrorFile, false),"UTF8"));
							sentenceDetectorEvaluator.evaluate(config.getSentenceCorpusReader(), sentenceErrorWriter);
							sentenceErrorWriter.flush();
							sentenceErrorWriter.close();
						} finally {
							MONITOR.endTask();
						}
						break;
					case Tokeniser:
						TokeniserEvaluator tokeniserEvaluator = config.getTokeniserEvaluator();
						MONITOR.startTask("tokeniserEvaluate");
						try {
							tokeniserEvaluator.evaluate(config.getTokenCorpusReader());
						} finally {
							MONITOR.endTask();
						}
						break;
					case PosTagger:
						PosTaggerEvaluator posTaggerEvaluator = config.getPosTaggerEvaluator();
						MONITOR.startTask("posTaggerEvaluate");
						try {
							posTaggerEvaluator.evaluate(config.getPosTagCorpusReader());
						} finally {
							MONITOR.endTask();
						}
						break;
					case Parser:
						ParserEvaluator parserEvaluator = config.getParserEvaluator();
						MONITOR.startTask("parserEvaluate");
						try {
							parserEvaluator.evaluate(config.getParserCorpusReader());
						} finally {
							MONITOR.endTask();
						}
						break;
					default:
						throw new TalismaneException("Command 'evaluate' does not yet support module: " + config.getModule());
					}
				} finally {
					MONITOR.endTask();
				}
				break;
			case compare:
				MONITOR.startTask("compare");
				try {
					switch (config.getModule()) {
					case Tokeniser:
						TokenComparator tokenComparator = config.getTokenComparator();
						tokenComparator.compare();
						break;
					case PosTagger:
						PosTagComparator posTagComparator = config.getPosTagComparator();
						posTagComparator.evaluate(config.getPosTagCorpusReader(), config.getPosTagEvaluationCorpusReader());
						break;
					case Parser:
						ParseComparator parseComparator = config.getParseComparator();
						parseComparator.evaluate(config.getParserCorpusReader(), config.getParserEvaluationCorpusReader());
						break;
					default:
						throw new TalismaneException("Command 'compare' does not yet support module: " + config.getModule());
					}
				} finally {
					MONITOR.endTask();
				}
				break;
			case train:
				MONITOR.startTask("train");
				try {
					switch (config.getModule()) {
					case LanguageDetector: {
						File modelFile = new File(config.getLanguageModelFilePath());
						File modelDir = modelFile.getParentFile();
						modelDir.mkdirs();
						
						ClassificationModelTrainer trainer = machineLearningService.getClassificationModelTrainer(config.getAlgorithm(), config.getTrainParameters());
						
						ClassificationModel languageModel = trainer.trainModel(config.getClassificationEventStream(), config.getDescriptors());
						if (config.getExternalResourceFinder()!=null)
							languageModel.setExternalResources(config.getExternalResourceFinder().getExternalResources());
						languageModel.persist(modelFile);
						break;
					}
					case SentenceDetector: {
						File modelFile = new File(config.getSentenceModelFilePath());
						File modelDir = modelFile.getParentFile();
						modelDir.mkdirs();
						
						ClassificationModelTrainer trainer = machineLearningService.getClassificationModelTrainer(config.getAlgorithm(), config.getTrainParameters());
	
						ClassificationModel sentenceModel = trainer.trainModel(config.getClassificationEventStream(), config.getDescriptors());
						if (config.getExternalResourceFinder()!=null)
							sentenceModel.setExternalResources(config.getExternalResourceFinder().getExternalResources());
						sentenceModel.persist(modelFile);
						break;
					}
					case Tokeniser: {
						File modelFile = new File(config.getTokeniserModelFilePath());
						File modelDir = modelFile.getParentFile();
						modelDir.mkdirs();
						ClassificationModelTrainer trainer = this.getMachineLearningService().getClassificationModelTrainer(config.getAlgorithm(), config.getTrainParameters());
						
						ClassificationModel tokeniserModel = trainer.trainModel(config.getClassificationEventStream(), config.getDescriptors());
						if (config.getExternalResourceFinder()!=null)
							tokeniserModel.setExternalResources(config.getExternalResourceFinder().getExternalResources());
						tokeniserModel.getModelAttributes().put(PatternTokeniserType.class.getSimpleName(), config.getPatternTokeniserType().toString());
						tokeniserModel.persist(modelFile);
						break;
					}
					case PosTagger: {
						File modelFile = new File(config.getPosTaggerModelFilePath());
						File modelDir = modelFile.getParentFile();
						modelDir.mkdirs();
						if (config.getPerceptronObservationPoints()==null) {
							ClassificationModelTrainer trainer = machineLearningService.getClassificationModelTrainer(config.getAlgorithm(), config.getTrainParameters());
	
							ClassificationModel posTaggerModel = trainer.trainModel(config.getClassificationEventStream(), config.getDescriptors());			
							if (config.getExternalResourceFinder()!=null)
								posTaggerModel.setExternalResources(config.getExternalResourceFinder().getExternalResources());
							posTaggerModel.persist(modelFile);
						} else {
							if (config.getAlgorithm()!=MachineLearningAlgorithm.Perceptron)
								throw new RuntimeException("Incompatible argument perceptronTrainingInterval with algorithm " + config.getAlgorithm());
							MachineLearningServiceLocator machineLearningServiceLocator = MachineLearningServiceLocator.getInstance();
							PerceptronServiceLocator perceptronServiceLocator = PerceptronServiceLocator.getInstance(machineLearningServiceLocator);
							PerceptronService perceptronService = perceptronServiceLocator.getPerceptronService();
							PerceptronClassificationModelTrainer trainer = perceptronService.getPerceptronModelTrainer();
							trainer.setParameters(config.getTrainParameters());
							
							
							String modelName = modelFile.getName().substring(0, modelFile.getName().lastIndexOf('.'));
							PerceptronModelTrainerObserver observer = new PosTaggerPerceptronModelPersister(modelDir, modelName, config.getExternalResourceFinder());
							trainer.trainModelsWithObserver(config.getClassificationEventStream(), config.getDescriptors(), observer, config.getPerceptronObservationPoints());
						}
						break;
					}
					case Parser: {
						MachineLearningModel parserModel = null;
						File modelFile = new File(config.getParserModelFilePath());
						File modelDir = modelFile.getParentFile();
						modelDir.mkdirs();
	
						boolean needToPersist = true;
						if (config.getAlgorithm().getModelType()==MachineLearningModelType.Classification) {
							if (config.getPerceptronObservationPoints()==null) {
								ClassificationModelTrainer trainer = this.getMachineLearningService().getClassificationModelTrainer(config.getAlgorithm(), config.getTrainParameters());
			
								parserModel = trainer.trainModel(config.getClassificationEventStream(), config.getDescriptors());
							} else {
								if (config.getAlgorithm()!=MachineLearningAlgorithm.Perceptron)
									throw new RuntimeException("Incompatible argument perceptronTrainingInterval with algorithm " + config.getAlgorithm());
								MachineLearningServiceLocator machineLearningServiceLocator = MachineLearningServiceLocator.getInstance();
								PerceptronServiceLocator perceptronServiceLocator = PerceptronServiceLocator.getInstance(machineLearningServiceLocator);
								PerceptronService perceptronService = perceptronServiceLocator.getPerceptronService();
								PerceptronClassificationModelTrainer trainer = perceptronService.getPerceptronModelTrainer();
								trainer.setParameters(config.getTrainParameters());
								
								String modelName = modelFile.getName().substring(0, modelFile.getName().lastIndexOf('.'));
	
								PerceptronModelTrainerObserver observer = new ParserPerceptronModelPersister(modelDir, modelName, config.getExternalResourceFinder());
								trainer.trainModelsWithObserver(config.getClassificationEventStream(), config.getDescriptors(), observer, config.getPerceptronObservationPoints());
								needToPersist = false;
							}
						} else if (config.getAlgorithm().getModelType()==MachineLearningModelType.Ranking) {
							RankingEventStream<PosTagSequence> parseEventStream = parserService.getGlobalParseEventStream(config.getParserCorpusReader(), config.getParserFeatures());
							RankingModelTrainer<PosTagSequence> trainer = machineLearningService.getRankingModelTrainer(config.getAlgorithm(), config.getTrainParameters());
							Ranker<PosTagSequence> ranker = parserService.getRanker(config.getParsingConstrainer(), config.getParserFeatures(), config.getBeamWidth());
							parserModel = trainer.trainModel(parseEventStream, ranker, config.getDescriptors());
							parserModel.addDependency(ParsingConstrainer.class.getSimpleName(), config.getParsingConstrainer());
						} else {
							throw new TalismaneException("Unknown model type: " + config.getAlgorithm().getModelType());
						}
						
						if (needToPersist) {
							if (config.getExternalResourceFinder()!=null)
								parserModel.setExternalResources(config.getExternalResourceFinder().getExternalResources());
							parserModel.persist(modelFile);
						}
						break;
					}
					default:
						throw new TalismaneException("Command 'train' does not yet support module: " + config.getModule());
					}
				} finally {
					MONITOR.endTask();
				}
				break;
			default:
				throw new TalismaneException("Unsupported command: " + config.getCommand());
			} // which command?
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		} finally {
			PerformanceMonitor.end();
			
			if (PerformanceMonitor.isActivated()) {
				try {
					Writer csvFileWriter = null;
					File csvFile  =new File(config.getOutDir(), config.getBaseName() + ".performance.csv");
					
					csvFile.delete();
					csvFile.createNewFile();
					csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false),"UTF8"));
					PerformanceMonitor.writePerformanceCSV(csvFileWriter);
					csvFileWriter.flush();
					csvFileWriter.close();
				} catch (Exception e) {
					LogUtils.logError(LOG, e);
				}
			}
			
			long endTime = new Date().getTime();
			long totalTime = endTime - startTime;
			LOG.debug("Total time for Talismane.process(): " + totalTime);
			
			if (config.isLogStats()) {
				try {
					Writer csvFileWriter = null;
					File csvFile = new File(config.getOutDir(), config.getBaseName() + ".stats.csv");
					
					csvFile.delete();
					csvFile.createNewFile();
					csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false),"UTF8"));
					csvFileWriter.write(CSV.format("total time")
							+ CSV.format(totalTime) + "\n");
					csvFileWriter.flush();
					csvFileWriter.close();
				} catch (Exception e) {
					LogUtils.logError(LOG, e);
				}
			}
		}
	}

	public void analyse(TalismaneConfig config) {
		try {
			if (config.needsSentenceDetector()) {
				if (config.getSentenceDetector()==null) {
					throw new TalismaneException("Sentence detector not provided.");
				}
			}
			if (config.needsTokeniser()) {
				if (config.getTokeniser()==null) {
					throw new TalismaneException("Tokeniser not provided.");
				}
			}
			if (config.needsPosTagger()) {
				if (config.getPosTagger()==null) {
					throw new TalismaneException("Pos-tagger not provided.");
				}
			}
			if (config.needsParser()) {
				if (config.getParser()==null) {
					throw new TalismaneException("Parser not provided.");
				}
			}
			
			if (config.getEndModule().equals(Module.SentenceDetector)) {
				if (this.getSentenceProcessor()==null) {
					throw new TalismaneException("No sentence processor provided with sentence detector end module, cannot generate output.");
				}
			}
			if (config.getEndModule().equals(Module.Tokeniser)) {
				if (this.getTokenSequenceProcessor()==null) {
					throw new TalismaneException("No token sequence processor provided with tokeniser end module, cannot generate output.");
				}
			}
			if (config.getEndModule().equals(Module.PosTagger)) {
				if (this.getPosTagSequenceProcessor()==null) {
					throw new TalismaneException("No postag sequence processor provided with pos-tagger end module, cannot generate output.");
				}
			}
			if (config.getEndModule().equals(Module.Parser)) {
				if (this.getParseConfigurationProcessor()==null) {
					throw new TalismaneException("No parse configuration processor provided with parser end module, cannot generate output.");
				}
			}
			
			LinkedList<String> textSegments = new LinkedList<String>();
			LinkedList<Sentence> sentences = new LinkedList<Sentence>();
			TokenSequence tokenSequence = null;
			PosTagSequence posTagSequence = null;
			
			RollingSentenceProcessor rollingSentenceProcessor = this.getFilterService().getRollingSentenceProcessor(config.getFileName(), config.isProcessByDefault());
			if (this.getReader() instanceof CurrentFileProvider) {
				((CurrentFileProvider) this.getReader()).addCurrentFileObserver(rollingSentenceProcessor);
			}
			
			Sentence leftover = null;
			if (config.getStartModule().equals(Module.SentenceDetector)||config.getStartModule().equals(Module.Tokeniser)) {
				// prime the sentence detector with two text segments, to ensure everything gets processed
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
		    	if (config.getStartModule().equals(Module.SentenceDetector)||config.getStartModule().equals(Module.Tokeniser)) {
		    		// Note SentenceDetector and Tokeniser start modules treated identically,
		    		// except that for SentenceDetector we apply a probabilistic sentence detector
		    		// whereas for Tokeniser we assume all sentence breaks are marked by filters
		    		
				    // read characters from the reader, one at a time
			    	char c;
			    	int r = -1;
			    	try {
			    		r = this.getReader().read();
			    	} catch (IOException e) {
			    		LogUtils.logError(LOG, e);
			    	}
			    	
			    	if (r==-1) {
			    		finished = true;
			    		c = '\n';
			    	} else {
		    			c = (char) r;
			    	}

			    	// Jump out if we have 3 consecutive end-block characters.
			    	if (c==config.getEndBlockCharacter()) {
			    		endBlockCharacterCount++;
			    		if (endBlockCharacterCount==3) {
			    			LOG.info("Three consecutive end-block characters. Exiting.");
			    			finished = true;
			    		}
			    	} else {
			    		endBlockCharacterCount = 0;
			    	}
	    			
	    			// have sentence detector
		    		if (finished || (Character.isWhitespace(c) && c!='\r' && c!='\n' && stringBuilder.length()>config.getBlockSize()) || c==config.getEndBlockCharacter()) {
	    				if (c==config.getEndBlockCharacter())
	    					stringBuilder.append(c);
		    			if (stringBuilder.length()>0) {
			    			String textSegment = stringBuilder.toString();
			    			stringBuilder = new StringBuilder();
			    			
		    				textSegments.add(textSegment);
		    			} // is the current block > 0 characters?
	    				if (c==config.getEndBlockCharacter()) {
	    					textSegments.addLast("");
	    				}
		    		} // is there a next block available?
		    		
		    		if (finished) {
		    			if (stringBuilder.length()>0) {
		    				textSegments.addLast(stringBuilder.toString());
		    				stringBuilder = new StringBuilder();
		    			}
						textSegments.addLast("");
						textSegments.addLast("");
						textSegments.addLast("");
		    		}
		    		
		    		if (c!=config.getEndBlockCharacter())
		    			stringBuilder.append(c);
		    		
					while (textSegments.size()>=3) {
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
						
						// push the text segments back onto the beginning of Deque
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

					    boolean reallyFinished = finished && textSegments.size()==3;

						if (prevSentenceHolder!=null) {
							if (config.getStartModule().equals(Module.SentenceDetector)) {
								List<Integer> sentenceBreaks = config.getSentenceDetector().detectSentences(prevProcessedText, processedText, nextProcessedText);
								for (int sentenceBreak : sentenceBreaks) {
									prevSentenceHolder.addSentenceBoundary(sentenceBreak);
								}
							}
							
							List<Sentence> theSentences = prevSentenceHolder.getDetectedSentences(leftover);
							leftover = null;
							for (Sentence sentence : theSentences) {
								if (sentence.isComplete()||reallyFinished) {
									sentences.add(sentence);
									sentenceCount++;
								} else {
									LOG.debug("Setting leftover to: " + sentence.getText());
									leftover = sentence;
								}
							}
							if (config.getMaxSentenceCount()>0 && sentenceCount>=config.getMaxSentenceCount()) {
								finished = true;
							}
							
							// If we have any leftover original text segments, copy them over
							// they are necessarily at position 0 - since otherwise they would
							// have gotten added to the leftover sentence. The only case where
							// there isn't a leftover sentence is the case where the sentenceHolder
							// boundary happens to be a sentence boundary, hence position 0.
							if (prevSentenceHolder.getOriginalTextSegments().size()>0) {
								StringBuilder segmentsToInsert = new StringBuilder();
								for (String originalTextSegment : prevSentenceHolder.getOriginalTextSegments().values()) {
									segmentsToInsert.append(originalTextSegment);
								}
								String originalTextSegment0 = sentenceHolder.getOriginalTextSegments().get(0);
								if (originalTextSegment0==null)
									originalTextSegment0 = "";
								segmentsToInsert.append(originalTextSegment0);
								sentenceHolder.getOriginalTextSegments().put(0, segmentsToInsert.toString());
							}
						}
						prevSentenceHolder = sentenceHolder;
					} // we have at least 3 text segments (should always be the case once we get started)
		    	} else if (config.getStartModule().equals(Module.PosTagger)) {
	    			if (config.getTokenCorpusReader().hasNextTokenSequence()) {
	    				tokenSequence = config.getTokenCorpusReader().nextTokenSequence();
	    			} else {
	    				tokenSequence = null;
	    				finished = true;
	    			}
	    		} else if (config.getStartModule().equals(Module.Parser)) {
	    			if (config.getPosTagCorpusReader().hasNextPosTagSequence()) {
	    				posTagSequence = config.getPosTagCorpusReader().nextPosTagSequence();
	    			} else {
	    				posTagSequence = null;
	    				finished = true;
	    			}
	    		} // which start module?
	    		
	    		boolean needToProcess = false;
	    		if (config.getStartModule().equals(Module.SentenceDetector)||config.getStartModule().equals(Module.Tokeniser))
	    			needToProcess = !sentences.isEmpty();
	    		else if (config.getStartModule().equals(Module.PosTagger))
	    			needToProcess = tokenSequence!=null;
	    		else if (config.getStartModule().equals(Module.Parser))
	    			needToProcess = posTagSequence!=null;
	    		
	    		while (needToProcess) {
	    			Sentence sentence = null;
	    			if (config.getStartModule().compareTo(Module.Tokeniser)<=0 && config.getEndModule().compareTo(Module.SentenceDetector)>=0) {
		    			sentence = sentences.poll();
		    			LOG.debug("Sentence: " + sentence);
		    			if (this.getWriter() instanceof CurrentFileObserver && sentence.getFile()!=null && !sentence.getFile().equals(currentFile)) {
		    				currentFile = sentence.getFile();
		    				LOG.debug("Setting current file to " + currentFile.getPath());
		    				((CurrentFileObserver) this.getWriter()).onNextFile(currentFile);
		    			}
		    			
		    			if (this.getSentenceProcessor()!=null)
		    				this.getSentenceProcessor().onNextSentence(sentence, this.getWriter());
	    			} // need to read next sentence
	    			
	    			List<TokenSequence> tokenSequences = null;
	    			if (config.needsTokeniser()) {
	    				tokenSequences = config.getTokeniser().tokenise(sentence);
    					tokenSequence = tokenSequences.get(0);
	    				
    					if (this.getTokenSequenceProcessor()!=null) {
    						this.getTokenSequenceProcessor().onNextTokenSequence(tokenSequence, this.getWriter());
    					}
	    			} // need to tokenise ?
	    			
	    			List<PosTagSequence> posTagSequences = null;
 	    			if (config.needsPosTagger()) {
    					posTagSequence = null;
    					if (tokenSequences==null||!config.isPropagateTokeniserBeam()) {
    						tokenSequences = new ArrayListNoNulls<TokenSequence>();
    						tokenSequences.add(tokenSequence);
    					}

	    				if (config.getPosTagger() instanceof NonDeterministicPosTagger) {
	    					NonDeterministicPosTagger nonDeterministicPosTagger = (NonDeterministicPosTagger) config.getPosTagger();
	    					posTagSequences = nonDeterministicPosTagger.tagSentence(tokenSequences);
	    					posTagSequence = posTagSequences.get(0);
	    				} else {
	    					posTagSequence = config.getPosTagger().tagSentence(tokenSequence);
	    				}
	    				
	    				if (posTagSequenceProcessor!=null) {
	    					posTagSequenceProcessor.onNextPosTagSequence(posTagSequence, this.getWriter());
	    				}

	    				tokenSequence = null;
 	    			} // need to postag
 	    			
	    			if (config.needsParser()) {
    					if (posTagSequences==null||!config.isPropagatePosTaggerBeam()) {
    						posTagSequences = new ArrayListNoNulls<PosTagSequence>();
    						posTagSequences.add(posTagSequence);
    					}
    					
    					ParseConfiguration parseConfiguration = null;
    					List<ParseConfiguration> parseConfigurations = null;
    					try {
	    					if (config.getParser() instanceof NonDeterministicParser) {
	    						NonDeterministicParser nonDeterministicParser = (NonDeterministicParser) config.getParser();
		    					parseConfigurations = nonDeterministicParser.parseSentence(posTagSequences);
		    					parseConfiguration = parseConfigurations.get(0);
	    					} else {
	    						parseConfiguration = config.getParser().parseSentence(posTagSequence);
	    					}
	    					
	    					if (this.getParseConfigurationProcessor()!=null) {
	    						this.getParseConfigurationProcessor().onNextParseConfiguration(parseConfiguration, this.getWriter());
	    					}
    					} catch (Exception e) {
    						LOG.error(e);
    						if (stopOnError)
    							throw new RuntimeException(e);
    					}
    					posTagSequence = null;
    				} // need to parse
	    			
		    		if (config.getStartModule().equals(Module.SentenceDetector)||config.getStartModule().equals(Module.Tokeniser))
		    			needToProcess = !sentences.isEmpty();
		    		else if (config.getStartModule().equals(Module.PosTagger))
		    			needToProcess = tokenSequence!=null;
		    		else if (config.getStartModule().equals(Module.Parser))
		    			needToProcess = posTagSequence!=null;
	    		} // next sentence
			} // next character
		    
		    // Check if there's any leftover output to output!
		    if (prevSentenceHolder!=null && prevSentenceHolder.getOriginalTextSegments().size()>0) {
		    	for (String segment : prevSentenceHolder.getOriginalTextSegments().values()) {
		    		this.getWriter().append(segment);
		    	}
		    }
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		} finally {
			if (this.getParseConfigurationProcessor()!=null) {
				this.getParseConfigurationProcessor().onCompleteParse();
			}
			
			try {
				this.getReader().close();
				this.getWriter().flush();
				this.getWriter().close();
			} catch (IOException ioe2) {
				LOG.error(ioe2);
				throw new RuntimeException(ioe2);
			}

		}
	}

	
	public LanguageDetectorProcessor getLanguageDetectorProcessor() {
		return languageDetectorProcessor;
	}

	public void setLanguageDetectorProcessor(
			LanguageDetectorProcessor languageDetectorProcessor) {
		this.languageDetectorProcessor = languageDetectorProcessor;
	}

	@Override
	public SentenceProcessor getSentenceProcessor() {
		return sentenceProcessor;
	}

	@Override
	public void setSentenceProcessor(SentenceProcessor sentenceProcessor) {
		this.sentenceProcessor = sentenceProcessor;
	}

	@Override
	public TokenSequenceProcessor getTokenSequenceProcessor() {
		return tokenSequenceProcessor;
	}

	@Override
	public void setTokenSequenceProcessor(
			TokenSequenceProcessor tokenSequenceProcessor) {
		this.tokenSequenceProcessor = tokenSequenceProcessor;
	}

	@Override
	public PosTagSequenceProcessor getPosTagSequenceProcessor() {
		return posTagSequenceProcessor;
	}

	@Override
	public void setPosTagSequenceProcessor(
			PosTagSequenceProcessor posTagSequenceProcessor) {
		this.posTagSequenceProcessor = posTagSequenceProcessor;
	}

	@Override
	public ParseConfigurationProcessor getParseConfigurationProcessor() {
		return parseConfigurationProcessor;
	}

	@Override
	public void setParseConfigurationProcessor(
			ParseConfigurationProcessor parseConfigurationProcessor) {
		this.parseConfigurationProcessor = parseConfigurationProcessor;
	}

	
	public TalismaneService getTalismaneService() {
		return talismaneService;
	}

	public void setTalismaneService(TalismaneService talismaneService) {
		this.talismaneService = talismaneService;
	}

	public TokeniserService getTokeniserService() {
		return tokeniserService;
	}

	public void setTokeniserService(TokeniserService tokeniserService) {
		this.tokeniserService = tokeniserService;
	}

	public PosTaggerService getPosTaggerService() {
		return posTaggerService;
	}

	public void setPosTaggerService(PosTaggerService posTaggerService) {
		this.posTaggerService = posTaggerService;
	}

	public ParserService getParserService() {
		return parserService;
	}

	public void setParserService(ParserService parserService) {
		this.parserService = parserService;
	}

	public FilterService getFilterService() {
		return filterService;
	}

	public void setFilterService(FilterService filterService) {
		this.filterService = filterService;
	}


	public MachineLearningService getMachineLearningService() {
		return machineLearningService;
	}

	public void setMachineLearningService(
			MachineLearningService machineLearningService) {
		this.machineLearningService = machineLearningService;
	}

	public SentenceDetectorService getSentenceDetectorService() {
		return sentenceDetectorService;
	}

	public void setSentenceDetectorService(
			SentenceDetectorService sentenceDetectorService) {
		this.sentenceDetectorService = sentenceDetectorService;
	}

	public boolean isStopOnError() {
		return stopOnError;
	}

	public void setStopOnError(boolean stopOnError) {
		this.stopOnError = stopOnError;
	}

	public Reader getReader() {
		if (this.reader==null)
			this.reader = config.getReader();
		return reader;
	}

	public void setReader(Reader reader) {
		this.reader = reader;
	}

	public Writer getWriter() {
		if (this.writer==null)
			this.writer = config.getWriter();
		return writer;
	}

	public void setWriter(Writer writer) {
		this.writer = writer;
	}
	
	  private static final class PosTaggerPerceptronModelPersister implements PerceptronModelTrainerObserver {
	    	File outDir;
	    	ExternalResourceFinder externalResourceFinder;
	    	String baseName;
	    	
			public PosTaggerPerceptronModelPersister(File outDir, String baseName,
					ExternalResourceFinder externalResourceFinder) {
				super();
				this.outDir = outDir;
				this.baseName = baseName;
				this.externalResourceFinder = externalResourceFinder;
			}

			@Override
			public void onNextModel(ClassificationModel model,
					int iterations) {
				this.outDir.mkdirs();
				File posTaggerModelFile = new File(outDir, baseName + "_i" + iterations + ".zip");
				if (externalResourceFinder!=null)
					model.setExternalResources(externalResourceFinder.getExternalResources());
				LOG.info("Writing model " + posTaggerModelFile.getName());
				model.persist(posTaggerModelFile);
			}
	    }
	  
    private static final class ParserPerceptronModelPersister implements PerceptronModelTrainerObserver {
    	File outDir;
    	ExternalResourceFinder externalResourceFinder;
    	String baseName;
    	
		public ParserPerceptronModelPersister(File outDir, String baseName,
				ExternalResourceFinder externalResourceFinder) {
			super();
			this.outDir = outDir;
			this.baseName = baseName;
			this.externalResourceFinder = externalResourceFinder;
		}

		@Override
		public void onNextModel(ClassificationModel model,
				int iterations) {
			this.outDir.mkdirs();
			File parserModelFile = new File(outDir, baseName + "_i" + iterations + ".zip");
			if (externalResourceFinder!=null)
				model.setExternalResources(externalResourceFinder.getExternalResources());
			LOG.info("Writing model " + parserModelFile.getName());
			model.persist(parserModelFile);
		}
    }

	public LanguageDetectorService getLanguageDetectorService() {
		return languageDetectorService;
	}

	public void setLanguageDetectorService(
			LanguageDetectorService languageDetectorService) {
		this.languageDetectorService = languageDetectorService;
	}
    
    
}
