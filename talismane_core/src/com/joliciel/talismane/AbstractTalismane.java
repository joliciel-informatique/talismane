///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
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
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.filters.FilterService;
import com.joliciel.talismane.filters.RollingSentenceProcessor;
import com.joliciel.talismane.filters.Sentence;
import com.joliciel.talismane.filters.SentenceHolder;
import com.joliciel.talismane.filters.TextMarker;
import com.joliciel.talismane.filters.TextMarkerFilter;
import com.joliciel.talismane.parser.NonDeterministicParser;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.parser.ParseConfigurationProcessor;
import com.joliciel.talismane.parser.ParserEvaluator;
import com.joliciel.talismane.parser.ParserService;
import com.joliciel.talismane.parser.TransitionSystem;
import com.joliciel.talismane.posTagger.NonDeterministicPosTagger;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTagSequenceProcessor;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.posTagger.PosTaggerEvaluator;
import com.joliciel.talismane.posTagger.PosTaggerLexicon;
import com.joliciel.talismane.posTagger.PosTaggerService;
import com.joliciel.talismane.sentenceDetector.SentenceProcessor;
import com.joliciel.talismane.stats.FScoreCalculator;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.TokenSequenceProcessor;
import com.joliciel.talismane.tokeniser.TokeniserService;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;
import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.PerformanceMonitor;

/**
 * A language-neutral abstract implementation of Talismane.
 * Sub-classes need to add language-specific method implementations.
 * @author Assaf Urieli
 */
public abstract class AbstractTalismane implements Talismane, LanguageSpecificImplementation {
	private static final Log LOG = LogFactory.getLog(AbstractTalismane.class);
	
	private static final int MIN_BLOCK_SIZE = 1000;

	private SentenceProcessor sentenceProcessor;
	private TokenSequenceProcessor tokenSequenceProcessor;
	private PosTagSequenceProcessor posTagSequenceProcessor;
	private ParseConfigurationProcessor parseConfigurationProcessor;
	
	private TokeniserService tokeniserService;
	private PosTaggerService posTaggerService;
	private ParserService parserService;
	private FilterService filterService;
	
	private boolean stopOnError = true;

	private TalismaneServiceLocator talismaneServiceLocator = null;

	protected AbstractTalismane() {
		talismaneServiceLocator = TalismaneServiceLocator.getInstance();
		TalismaneSession.setImplementation(this);
	}

	public void runCommand(TalismaneConfig config) throws Exception {
		long startTime = new Date().getTime();
		PerformanceMonitor.setActive(config.isLogPerformance());
		PerformanceMonitor.start();
		try {
			if (this.getSentenceProcessor()==null)
				this.setSentenceProcessor(config.getSentenceProcessor());
			if (this.getTokenSequenceProcessor()==null)
				this.setTokenSequenceProcessor(config.getTokenSequenceProcessor());
			if (this.getPosTagSequenceProcessor()==null)
				this.setPosTagSequenceProcessor(config.getPosTagSequenceProcessor());
			if (this.getParseConfigurationProcessor()==null)
				this.setParseConfigurationProcessor(config.getParseConfigurationProcessor());
			
			if (config.getCommand().equals(Command.analyse)) {
				this.analyse(config);
			} else if (config.getCommand().equals(Command.process)) {
				int sentenceCount = 0;
				if (config.getModule().equals(Talismane.Module.Tokeniser)) {
					if (this.getTokenSequenceProcessor()==null)
						throw new TalismaneException("Cannot process tokeniser output without a token sequence processor!");
					while (config.getTokenCorpusReader().hasNextTokenSequence()) {
						TokenSequence tokenSequence = config.getTokenCorpusReader().nextTokenSequence();
						this.getTokenSequenceProcessor().onNextTokenSequence(tokenSequence);
						sentenceCount++;
						if (config.getMaxSentenceCount()>0 && sentenceCount>=config.getMaxSentenceCount())
							break;
					}
				} else if (config.getModule().equals(Talismane.Module.PosTagger)) {
					if (this.getPosTagSequenceProcessor()==null)
						throw new TalismaneException("Cannot process pos-tagger output without a pos-tag sequence processor!");
					while (config.getPosTagCorpusReader().hasNextPosTagSequence()) {
						PosTagSequence posTagSequence = config.getPosTagCorpusReader().nextPosTagSequence();
						this.getPosTagSequenceProcessor().onNextPosTagSequence(posTagSequence);
						sentenceCount++;
						if (config.getMaxSentenceCount()>0 && sentenceCount>=config.getMaxSentenceCount())
							break;
					}
				} else if (config.getModule().equals(Talismane.Module.Parser)) {
					if (this.getParseConfigurationProcessor()==null)
						throw new TalismaneException("Cannot process parser output without a parse configuration processor!");
					try {
						while (config.getParserCorpusReader().hasNextConfiguration()) {
							ParseConfiguration parseConfiguration = config.getParserCorpusReader().nextConfiguration();
							this.getParseConfigurationProcessor().onNextParseConfiguration(parseConfiguration);
							sentenceCount++;
							if (config.getMaxSentenceCount()>0 && sentenceCount>=config.getMaxSentenceCount())
								break;
						}
					} finally {
						this.getParseConfigurationProcessor().onCompleteParse();
					}
				} else {
					throw new TalismaneException("Command 'process' does not yet support module: " + config.getModule());
				}
			} else if (config.getCommand().equals(Command.evaluate)) {
				FScoreCalculator<String> fScoreCalculator = null;
				if (config.getModule().equals(Talismane.Module.PosTagger)) {
					PosTaggerEvaluator posTaggerEvaluator = config.getPosTaggerEvaluator();
					try {
						fScoreCalculator = posTaggerEvaluator.evaluate(config.getPosTagCorpusReader());
						
						double unknownLexiconFScore = posTaggerEvaluator.getFscoreUnknownInLexicon().getTotalFScore();
						LOG.debug("F-score for words unknown in lexicon: " + unknownLexiconFScore);
						if (posTaggerEvaluator.getFscoreUnknownInCorpus()!=null) {
							double unknownCorpusFScore = posTaggerEvaluator.getFscoreUnknownInCorpus().getTotalFScore();
							LOG.debug("F-score for words unknown in corpus: " + unknownCorpusFScore);
						}
						
						double fscore = fScoreCalculator.getTotalFScore();
						LOG.debug("F-score: " + fscore);
					} finally {
						if (posTaggerEvaluator.getCsvFileWriter()!=null) {
							posTaggerEvaluator.getCsvFileWriter().flush();
							posTaggerEvaluator.getCsvFileWriter().close();
						}			
					}
				} else if (config.getModule().equals(Talismane.Module.Parser)) {
					ParserEvaluator parserEvaluator = config.getParserEvaluator();
					try {
						fScoreCalculator = parserEvaluator.evaluate(config.getParserCorpusReader());
						
						double fscore = fScoreCalculator.getTotalFScore();
						LOG.debug("F-score: " + fscore);
					} finally {
						if (parserEvaluator.getCsvFileWriter()!=null) {
							parserEvaluator.getCsvFileWriter().flush();
							parserEvaluator.getCsvFileWriter().close();
						}
					}
				} else {
					throw new TalismaneException("Command 'evaluate' does not yet support module: " + config.getModule());
				}
				
				File fscoreFile = new File(config.getOutDir(), config.getBaseName() + ".fscores.csv");
				fScoreCalculator.writeScoresToCSVFile(fscoreFile);

			} // which command?
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw e;
		} finally {
			PerformanceMonitor.end();
			
			if (config.isLogPerformance()) {
				try {
					Writer csvFileWriter = null;
					File csvFile = null;
					if (config.getOutDir()!=null) {
						csvFile = new File(config.getOutDir(), "performance.csv");
					} else {
						csvFile = new File("performance.csv");
					}
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
			LOG.info("Total time: " + totalTime);
		}
	}

	@Override
	public abstract TransitionSystem getDefaultTransitionSystem();

	@Override
	public abstract InputStream getDefaultTextMarkerFiltersFromStream();

	@Override
	public abstract InputStream getDefaultTokenFiltersFromStream();

	/**
	 * A list of filters to be applied to the atomic token sequences
	 * prior to tokenisation.
	 * @return
	 */
	@Override
	public abstract List<TokenSequenceFilter> getTokenSequenceFilters();
	
	/**
	 * A list of filters to be applied to token sequences generated by the tokeniser
	 * prior to pos-tagging.
	 * @return
	 */
	@Override
	public abstract List<TokenSequenceFilter> getPosTaggerPreprocessingFilters();

	@Override
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
			Sentence leftover = null;
			if (config.getStartModule().equals(Module.SentenceDetector)||config.getStartModule().equals(Module.Tokeniser)) {
				// prime the sentence detector with two text segments, to ensure everything gets processed
				textSegments.addLast("");
				textSegments.addLast("");
			}
			
		    StringBuilder stringBuilder = new StringBuilder();
		    boolean finished = false;
		    
			String prevProcessedText = "";
			String processedText = "";
			String nextProcessedText = "";
			SentenceHolder prevSentenceHolder = null;

		    while (!finished) {
		    	if (config.getStartModule().equals(Module.SentenceDetector)||config.getStartModule().equals(Module.Tokeniser)) {
				    // read characters from the reader, one at a time
			    	char c;
			    	int r = config.getReader().read();
			    	if (r==-1) {
			    		finished = true;
			    		c = '\n';
			    	} else {
		    			c = (char) r;
			    	}
	    			
	    			// have sentence detector
		    		if (finished || (Character.isWhitespace(c) && stringBuilder.length()>MIN_BLOCK_SIZE) || c==config.getEndBlockCharacter()) {
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
							LOG.trace("prevText: " + prevText);
							LOG.trace("text: " + text);
							LOG.trace("nextText: " + nextText);							
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
								} else {
									LOG.debug("Setting leftover to: " + sentence.getText());
									leftover = sentence;
								}
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
		    			if (this.getSentenceProcessor()!=null)
		    				this.getSentenceProcessor().process(sentence.getText());
	    			} // need to read next sentence
	    			
	    			List<TokenSequence> tokenSequences = null;
	    			if (config.needsTokeniser()) {
	    				tokenSequences = config.getTokeniser().tokenise(sentence);
    					tokenSequence = tokenSequences.get(0);
	    				
    					if (this.getTokenSequenceProcessor()!=null) {
    						this.getTokenSequenceProcessor().onNextTokenSequence(tokenSequence);
    					}
	    			} // need to tokenise ?
	    			
	    			List<PosTagSequence> posTagSequences = null;
 	    			if (config.needsPosTagger()) {
    					posTagSequence = null;
    					if (tokenSequences==null||!config.isPropagateBeam()) {
    						tokenSequences = new ArrayList<TokenSequence>();
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
	    					posTagSequenceProcessor.onNextPosTagSequence(posTagSequence);
	    				}

	    				tokenSequence = null;
 	    			} // need to postag
 	    			
	    			if (config.needsParser()) {
    					if (posTagSequences==null||!config.isPropagateBeam()) {
    						posTagSequences = new ArrayList<PosTagSequence>();
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
	    						this.getParseConfigurationProcessor().onNextParseConfiguration(parseConfiguration);
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
		} catch (IOException ioe) {
			LOG.error(ioe);
			throw new RuntimeException(ioe);
		} finally {
			if (this.getParseConfigurationProcessor()!=null) {
				this.getParseConfigurationProcessor().onCompleteParse();
			}
			
			try {
				config.getReader().close();
				config.getWriter().flush();
				config.getWriter().close();
			} catch (IOException ioe2) {
				LOG.error(ioe2);
				throw new RuntimeException(ioe2);
			}

		}
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

	public TokeniserService getTokeniserService() {
		if (tokeniserService==null)
			tokeniserService = talismaneServiceLocator.getTokeniserServiceLocator().getTokeniserService();
		return tokeniserService;
	}

	public void setTokeniserService(TokeniserService tokeniserService) {
		this.tokeniserService = tokeniserService;
	}

	public PosTaggerService getPosTaggerService() {
		if (posTaggerService==null)
			posTaggerService = talismaneServiceLocator.getPosTaggerServiceLocator().getPosTaggerService();
		return posTaggerService;
	}

	public void setPosTaggerService(PosTaggerService posTaggerService) {
		this.posTaggerService = posTaggerService;
	}

	public ParserService getParserService() {
		if (parserService==null)
			parserService = talismaneServiceLocator.getParserServiceLocator().getParserService();
		return parserService;
	}

	public void setParserService(ParserService parserService) {
		this.parserService = parserService;
	}

	public FilterService getFilterService() {
		if (filterService==null)
			filterService = talismaneServiceLocator.getFilterServiceLocator().getFilterService();
		return filterService;
	}

	public void setFilterService(FilterService filterService) {
		this.filterService = filterService;
	}

	@Override
	public abstract PosTaggerLexicon getDefaultLexiconService();
	@Override
	public abstract InputStream getDefaultPosTaggerRulesFromStream();
	@Override
	public abstract ZipInputStream getDefaultSentenceModelStream();
	@Override
	public abstract ZipInputStream getDefaultTokeniserModelStream();
	@Override
	public abstract ZipInputStream getDefaultPosTaggerModelStream();
	@Override
	public abstract ZipInputStream getDefaultParserModelStream();


	@Override
	public PosTagSet getDefaultPosTagSet() {
		Scanner posTagSetScanner = new Scanner(this.getDefaultPosTagSetFromStream());
		PosTagSet posTagSet = this.getPosTaggerService().getPosTagSet(posTagSetScanner);
		return posTagSet;
	}

	public boolean isStopOnError() {
		return stopOnError;
	}

	public void setStopOnError(boolean stopOnError) {
		this.stopOnError = stopOnError;
	}
	
	
}
