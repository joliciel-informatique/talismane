//Copyright (C) 2014 Joliciel Informatique
package com.joliciel.talismane;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import com.joliciel.talismane.Talismane.Command;
import com.joliciel.talismane.Talismane.Mode;
import com.joliciel.talismane.Talismane.Module;
import com.joliciel.talismane.filters.TextMarkerFilter;
import com.joliciel.talismane.languageDetector.LanguageDetector;
import com.joliciel.talismane.languageDetector.LanguageDetectorAnnotatedCorpusReader;
import com.joliciel.talismane.languageDetector.LanguageDetectorFeature;
import com.joliciel.talismane.languageDetector.LanguageDetectorProcessor;
import com.joliciel.talismane.machineLearning.ClassificationEventStream;
import com.joliciel.talismane.parser.ParseComparator;
import com.joliciel.talismane.parser.ParseConfigurationProcessor;
import com.joliciel.talismane.parser.Parser;
import com.joliciel.talismane.parser.ParserAnnotatedCorpusReader;
import com.joliciel.talismane.parser.ParserEvaluator;
import com.joliciel.talismane.parser.ParserService;
import com.joliciel.talismane.parser.features.ParseConfigurationFeature;
import com.joliciel.talismane.parser.features.ParserRule;
import com.joliciel.talismane.posTagger.PosTagAnnotatedCorpusReader;
import com.joliciel.talismane.posTagger.PosTagComparator;
import com.joliciel.talismane.posTagger.PosTagSequenceProcessor;
import com.joliciel.talismane.posTagger.PosTagger;
import com.joliciel.talismane.posTagger.PosTaggerEvaluator;
import com.joliciel.talismane.posTagger.PosTaggerService;
import com.joliciel.talismane.posTagger.features.PosTaggerFeature;
import com.joliciel.talismane.posTagger.features.PosTaggerRule;
import com.joliciel.talismane.sentenceDetector.SentenceDetector;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorAnnotatedCorpusReader;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorEvaluator;
import com.joliciel.talismane.sentenceDetector.SentenceProcessor;
import com.joliciel.talismane.sentenceDetector.features.SentenceDetectorFeature;
import com.joliciel.talismane.tokeniser.TokenComparator;
import com.joliciel.talismane.tokeniser.TokenSequenceProcessor;
import com.joliciel.talismane.tokeniser.Tokeniser;
import com.joliciel.talismane.tokeniser.TokeniserAnnotatedCorpusReader;
import com.joliciel.talismane.tokeniser.TokeniserEvaluator;
import com.joliciel.talismane.tokeniser.features.TokenPatternMatchFeature;
import com.joliciel.talismane.tokeniser.features.TokeniserContextFeature;
import com.joliciel.talismane.tokeniser.filters.TokenFilter;
import com.joliciel.talismane.tokeniser.patterns.PatternTokeniserFactory.PatternTokeniserType;
import com.joliciel.talismane.tokeniser.patterns.TokeniserPatternManager;
import com.typesafe.config.Config;

/**
 * An abstract base class for loading, storing and translating configuration
 * information to be passed to Talismane when processing.<br/>
 * Implementing classes must include language-specific implementation resources.
 * <br/>
 * The processing must go from a given start module to a given end module in
 * sequence, where the modules available are: Sentence detector, Tokeniser, Pos
 * tagger, Parser.<br/>
 * There is a default input format for each start module, which can be
 * over-ridden by providing a regex for processing lines of input in the
 * command-line or configuration file.
 * 
 * @author Assaf Urieli
 *
 */
public interface TalismaneConfig {
	/**
	 * Get the configuration object used to build this TalismaneConfig.
	 */
	public Config getConfig();

	/**
	 * The actual command to run by Talismane.
	 */
	public Command getCommand();

	public void setCommand(Command command);

	/**
	 * If the command required a start module (e.g. analyse), the start module
	 * for this command. Default is
	 * {@link com.joliciel.talismane.Talismane.Module#SentenceDetector}.
	 */
	public Module getStartModule();

	public void setStartModule(Module startModule);

	/**
	 * If the command requires an end module (e.g. analyse), the end module for
	 * this command. Default is
	 * {@link com.joliciel.talismane.Talismane.Module#Parser}.
	 */
	public Module getEndModule();

	public void setEndModule(Module endModule);

	/**
	 * For commands which only affect a single module (e.g. evaluate), the
	 * module for this command.
	 */
	public Module getModule();

	public void setModule(Module module);

	/**
	 * When analysing, should the raw text be processed by default, or should we
	 * wait until a text marker filter tells us to start processing. Default is
	 * true.
	 */
	public boolean isProcessByDefault();

	public void setProcessByDefault(boolean processByDefault);

	/**
	 * For the "process" command, the maximum number of sentences to process. If
	 * &lt;=0, all sentences will be processed. Default is 0 (all).
	 */
	public int getMaxSentenceCount();

	public void setMaxSentenceCount(int maxSentenceCount);

	/**
	 * The charset that is used to interpret the input stream.
	 */
	public Charset getInputCharset();

	public void setInputCharset(Charset inputCharset);

	/**
	 * The charset that is used to write to the output stream.
	 */
	public Charset getOutputCharset();

	public void setOutputCharset(Charset outputCharset);

	/**
	 * A character (typically non-printing) which will mark a stop in the input
	 * stream and set-off analysis. The default value is the form-feed character
	 * (code=12).
	 */
	public char getEndBlockCharacter();

	public void setEndBlockCharacter(char endBlockCharacter);

	/**
	 * The beam width for beam-search analysis. Default is 1. Increasing this
	 * value will increase analysis time in a linear fashion, but will typically
	 * improve results.
	 */
	public int getBeamWidth();

	public void setBeamWidth(int beamWidth);

	/**
	 * If true, the full beam of analyses produced as output by a given module
	 * will be used as input for the next module. If false, only the single best
	 * analysis will be used as input for the next module.
	 */
	public boolean isPropagateBeam();

	public void setPropagateBeam(boolean propagateBeam);

	/**
	 * The reader to be used to read the data for this analysis.
	 */
	public Reader getReader();

	/**
	 * The reader to be used to read the data for evaluation, when
	 * command=compare.
	 * 
	 * @throws IOException
	 */
	public Reader getEvaluationReader() throws IOException;

	/**
	 * A writer to which Talismane should write its output when analysing.
	 * 
	 * @throws IOException
	 */
	public Writer getWriter() throws IOException;

	/**
	 * The filename to be applied to this analysis (if filename is included in
	 * the output).
	 */
	public String getFileName();

	/**
	 * The directory to which we write any output files.
	 */
	public File getOutDir();

	/**
	 * The rules to apply when running the pos-tagger.
	 * 
	 * @throws IOException
	 */
	public List<PosTaggerRule> getPosTaggerRules() throws IOException;

	/**
	 * The rules to apply when running the parser.
	 * 
	 * @throws IOException
	 */
	public List<ParserRule> getParserRules() throws IOException;

	/**
	 * Text marker filters are applied to raw text segments extracted from the
	 * stream, 3 segments at a time. This means that if a particular marker
	 * crosses segment borders, it is handled correctly.
	 * 
	 * @throws IOException
	 */
	public List<TextMarkerFilter> getTextMarkerFilters() throws IOException;

	public void setTextMarkerFilters(Scanner scanner);

	public void setTextMarkerFilters(List<TextMarkerFilter> textMarkerFilters);

	public void addTextMarkerFilter(TextMarkerFilter textMarkerFilter);

	/**
	 * Create and return a new language detector based on this configuration.
	 */
	public LanguageDetector getLanguageDetector();

	/**
	 * Create and return a new sentence detector based on this configuration.
	 */
	public SentenceDetector getSentenceDetector();

	/**
	 * Create and return a new tokeniser based on this configuration.
	 */
	public Tokeniser getTokeniser();

	public TokeniserPatternManager getTokeniserPatternManager() throws IOException;

	public Set<LanguageDetectorFeature<?>> getLanguageDetectorFeatures() throws IOException;

	public Set<SentenceDetectorFeature<?>> getSentenceDetectorFeatures() throws IOException;

	public Set<TokeniserContextFeature<?>> getTokeniserContextFeatures() throws IOException;

	public Set<TokenPatternMatchFeature<?>> getTokenPatternMatchFeatures() throws IOException;

	public Set<PosTaggerFeature<?>> getPosTaggerFeatures() throws IOException;

	public ClassificationEventStream getClassificationEventStream() throws IOException;

	/**
	 * Create and return a new pos-tagger based on this configuration.
	 */
	public PosTagger getPosTagger();

	/**
	 * Create and return a new parser based on this configuration.
	 */
	public Parser getParser();

	public Set<ParseConfigurationFeature<?>> getParserFeatures() throws IOException;

	/**
	 * The maximum amount of time the parser will spend analysing any single
	 * sentence, in seconds. If it exceeds this time, the parser will return a
	 * partial analysis, or a "dependency forest", where certain nodes are left
	 * unattached (no governor).<br/>
	 * A value of 0 indicates that there is no maximum time - the parser will
	 * always continue until sentence analysis is complete.<br/>
	 * The default value is 60.<br/>
	 */
	public int getMaxParseAnalysisTime();

	public void setMaxParseAnalysisTime(int maxParseAnalysisTime);

	/**
	 * A language detector processor to process language detector output.
	 * 
	 * @throws IOException
	 */
	public LanguageDetectorProcessor getLanguageDetectorProcessor() throws IOException;

	public void setLanguageDetectorProcessor(LanguageDetectorProcessor languageDetectorProcessor);

	/**
	 * A sentence processor to process sentences that have been read.
	 * 
	 * @throws IOException
	 */
	public SentenceProcessor getSentenceProcessor() throws IOException;

	/**
	 * A token sequence processor to process token sequences that have been
	 * read.
	 * 
	 * @throws IOException
	 */
	public TokenSequenceProcessor getTokenSequenceProcessor() throws IOException;

	/**
	 * A pos-tag sequence processor to process pos-tag sequences that have been
	 * read.
	 * 
	 * @throws IOException
	 */
	public PosTagSequenceProcessor getPosTagSequenceProcessor() throws IOException;

	/**
	 * A parse configuration processor to process parse configurations that have
	 * been read.
	 * 
	 * @throws IOException
	 */
	public ParseConfigurationProcessor getParseConfigurationProcessor() throws IOException;

	/**
	 * A token corpus reader to read a corpus pre-annotated in tokens. Note that
	 * in general, any filters up to and including the tokeniser should be
	 * applied to the corpus reader.
	 * 
	 * @throws IOException
	 */
	public TokeniserAnnotatedCorpusReader getTokenCorpusReader() throws IOException;

	public TokeniserAnnotatedCorpusReader getTokenEvaluationCorpusReader() throws IOException;

	public void setTokenCorpusReader(TokeniserAnnotatedCorpusReader tokenCorpusReader);

	/**
	 * A pos tag corpus reader to read a corpus pre-annotated in postags. Note
	 * that, in general, any filters up to and including the pos-tagger should
	 * be applied to the reader.
	 * 
	 * @throws IOException
	 */
	public PosTagAnnotatedCorpusReader getPosTagCorpusReader() throws IOException;

	public PosTagAnnotatedCorpusReader getPosTagEvaluationCorpusReader() throws IOException;

	/**
	 * A parser corpus reader to read a corpus pre-annotated in dependencies.
	 * 
	 * @throws IOException
	 */
	public ParserAnnotatedCorpusReader getParserCorpusReader() throws IOException;

	public ParserAnnotatedCorpusReader getParserEvaluationCorpusReader() throws IOException;

	public void setParserEvaluationCorpusReader(ParserAnnotatedCorpusReader parserEvaluationCorpusReader);

	public void setPosTagEvaluationCorpusReader(PosTagAnnotatedCorpusReader posTagEvaluationCorpusReader);

	public void setPosTagCorpusReader(PosTagAnnotatedCorpusReader posTagCorpusReader);

	public void setParserCorpusReader(ParserAnnotatedCorpusReader parserCorpusReader);

	/**
	 * Get a parser evaluator if command=evaluate and endModule=parser.
	 */
	public ParserEvaluator getParserEvaluator();

	/**
	 * Get a parser comparator if command=compare and endModule=parser.
	 */
	public ParseComparator getParseComparator();

	/**
	 * Get a tokeniser evaluator if command=evaluate and endModule=tokeniser.
	 */
	public TokeniserEvaluator getTokeniserEvaluator();

	/**
	 * Get a sentence detector evaluator if command=evaluate and
	 * endModule=sentenceDetector.
	 */
	public SentenceDetectorEvaluator getSentenceDetectorEvaluator();

	/**
	 * Get a token comparator if command=compare and endModule=parser.
	 */
	public TokenComparator getTokenComparator();

	/**
	 * Get a pos-tagger evaluator if command=evaluate and endModule=posTagger.
	 */
	public PosTaggerEvaluator getPosTaggerEvaluator();

	/**
	 * Get a pos-tag comparator if command=compare and endModule=parser.
	 */
	public PosTagComparator getPosTagComparator();

	/**
	 * The base name, out of which to construct output file names.
	 */
	public String getBaseName();

	public PosTaggerService getPosTaggerService();

	public ParserService getParserService();

	public TalismaneService getTalismaneService();

	/**
	 * Does this instance of Talismane need a sentence detector to perform the
	 * requested processing.
	 */
	public boolean needsSentenceDetector();

	/**
	 * Does this instance of Talismane need a tokeniser to perform the requested
	 * processing.
	 */
	public boolean needsTokeniser();

	/**
	 * Does this instance of Talismane need a pos tagger to perform the
	 * requested processing.
	 */
	public boolean needsPosTagger();

	/**
	 * Does this instance of Talismane need a parser to perform the requested
	 * processing.
	 */
	public boolean needsParser();

	public boolean isLogStats();

	public SentenceDetectorAnnotatedCorpusReader getSentenceCorpusReader();

	public void setSentenceCorpusReader(SentenceDetectorAnnotatedCorpusReader sentenceCorpusReader);

	public int getTokeniserBeamWidth();

	public int getPosTaggerBeamWidth();

	public int getParserBeamWidth();

	public boolean isPropagateTokeniserBeam();

	public boolean isPropagatePosTaggerBeam();

	/**
	 * the minimum block size, in characters, to process by the sentence
	 * detector. Filters are applied to a concatenation of the previous block,
	 * the current block, and the next block prior to sentence detection, in
	 * order to ensure that a filter which crosses block boundaries is correctly
	 * applied. It is not legal to have a filter which matches text greater than
	 * a block size, since this could result in a filter which stops analysis
	 * but doesn't start it again correctly, or vice versa. Block size can be
	 * increased if really big filters are really required. Default is 1000.
	 */
	public int getBlockSize();

	public void setBlockSize(int blockSize);

	public File getPerformanceConfigFile();

	public void setPerformanceConfigFile(File performanceConfigFile);

	/**
	 * Should the parser corpus reader predict the transitions or not?
	 */
	public boolean isPredictTransitions();

	public void setPredictTransitions(boolean predictTransitions);

	public Mode getMode();

	public void setMode(Mode mode);

	public Talismane getTalismane();

	public Map<String, List<String>> getDescriptors();

	public List<Integer> getPerceptronObservationPoints();

	public String getPosTaggerModelFilePath();

	public String getTokeniserModelFilePath();

	public String getSentenceModelFilePath();

	public String getParserModelFilePath();

	public String getLanguageModelFilePath();

	public LanguageDetectorAnnotatedCorpusReader getLanguageCorpusReader();

	public PatternTokeniserType getPatternTokeniserType();

	/**
	 * The port where the Talismane Server should listen.
	 */
	public int getPort();

	/**
	 * The first sentence index to process.
	 */
	public int getStartSentence();

	/**
	 * Preload any lexicons or models required for this processing.
	 */
	public void preloadResources();

	/**
	 * The locale indicated for this configuration.
	 */
	public Locale getLocale();

	public void setLocale(Locale locale);

	/**
	 * Set all token filters replacing those loaded from the model or config.
	 */
	public void setTokenFilters(Scanner scanner);

	/**
	 * Add a token filter in addition to those loaded from the model or config.
	 * Token filters added here will always be run after the ones already
	 * loaded.
	 */
	public void addTokenFilter(TokenFilter tokenFilter);

	/**
	 * Prepend a token filter to the list of those loaded from the model or
	 * config. This will always be run before the others.
	 */
	public void prependTokenFilter(TokenFilter tokenFilter);

	String getParserEvluationReaderRegex();

	String getParserReaderRegex();

}