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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;

import com.joliciel.talismane.Talismane.Command;
import com.joliciel.talismane.Talismane.Mode;
import com.joliciel.talismane.Talismane.Module;
import com.joliciel.talismane.Talismane.Option;
import com.joliciel.talismane.filters.FilterService;
import com.joliciel.talismane.filters.MarkerFilterType;
import com.joliciel.talismane.filters.TextMarkerFilter;
import com.joliciel.talismane.languageDetector.LanguageDetector;
import com.joliciel.talismane.languageDetector.LanguageDetectorAnnotatedCorpusReader;
import com.joliciel.talismane.languageDetector.LanguageDetectorFeature;
import com.joliciel.talismane.languageDetector.LanguageDetectorProcessor;
import com.joliciel.talismane.languageDetector.LanguageDetectorService;
import com.joliciel.talismane.languageDetector.LanguageOutcome;
import com.joliciel.talismane.lexicon.LexicalEntryReader;
import com.joliciel.talismane.lexicon.LexiconDeserializer;
import com.joliciel.talismane.lexicon.PosTaggerLexicon;
import com.joliciel.talismane.lexicon.RegexLexicalEntryReader;
import com.joliciel.talismane.machineLearning.ClassificationEventStream;
import com.joliciel.talismane.machineLearning.ClassificationObserver;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.ExternalResource;
import com.joliciel.talismane.machineLearning.ExternalResourceFinder;
import com.joliciel.talismane.machineLearning.ExternalWordList;
import com.joliciel.talismane.machineLearning.MachineLearningAlgorithm;
import com.joliciel.talismane.machineLearning.MachineLearningModel;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.machineLearning.MachineLearningSession;
import com.joliciel.talismane.machineLearning.linearsvm.LinearSVMModelTrainer;
import com.joliciel.talismane.machineLearning.linearsvm.LinearSVMModelTrainer.LinearSVMSolverType;
import com.joliciel.talismane.machineLearning.maxent.MaxentModelTrainer;
import com.joliciel.talismane.machineLearning.perceptron.PerceptronClassificationModelTrainer;
import com.joliciel.talismane.machineLearning.perceptron.PerceptronService.PerceptronScoring;
import com.joliciel.talismane.output.FreemarkerTemplateWriter;
import com.joliciel.talismane.parser.ParseComparator;
import com.joliciel.talismane.parser.ParseComparisonStrategy;
import com.joliciel.talismane.parser.ParseConfigurationProcessor;
import com.joliciel.talismane.parser.ParseEvaluationFScoreCalculator;
import com.joliciel.talismane.parser.ParseEvaluationGuessTemplateWriter;
import com.joliciel.talismane.parser.ParseEvaluationObserverImpl;
import com.joliciel.talismane.parser.ParseEvaluationSentenceWriter;
import com.joliciel.talismane.parser.Parser;
import com.joliciel.talismane.parser.Parser.ParseComparisonStrategyType;
import com.joliciel.talismane.parser.ParseConfigurationProcessorChain;
import com.joliciel.talismane.parser.ParserAnnotatedCorpusReader;
import com.joliciel.talismane.parser.ParserEvaluator;
import com.joliciel.talismane.parser.ParserFScoreCalculatorByDistance;
import com.joliciel.talismane.parser.ParserRegexBasedCorpusReader;
import com.joliciel.talismane.parser.ParserService;
import com.joliciel.talismane.parser.ParsingConstrainer;
import com.joliciel.talismane.parser.Transition;
import com.joliciel.talismane.parser.TransitionBasedParser;
import com.joliciel.talismane.parser.TransitionSystem;
import com.joliciel.talismane.parser.features.ParseConfigurationFeature;
import com.joliciel.talismane.parser.features.ParserFeatureService;
import com.joliciel.talismane.parser.features.ParserRule;
import com.joliciel.talismane.posTagger.NonDeterministicPosTagger;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagAnnotatedCorpusReader;
import com.joliciel.talismane.posTagger.PosTagComparator;
import com.joliciel.talismane.posTagger.PosTagEvaluationFScoreCalculator;
import com.joliciel.talismane.posTagger.PosTagEvaluationLexicalCoverageTester;
import com.joliciel.talismane.posTagger.PosTagEvaluationSentenceWriter;
import com.joliciel.talismane.posTagger.PosTagRegexBasedCorpusReader;
import com.joliciel.talismane.posTagger.PosTagSequenceProcessor;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.posTagger.PosTagger;
import com.joliciel.talismane.posTagger.PosTaggerEvaluator;
import com.joliciel.talismane.posTagger.PosTaggerGuessTemplateWriter;
import com.joliciel.talismane.posTagger.PosTaggerService;
import com.joliciel.talismane.posTagger.features.PosTaggerFeature;
import com.joliciel.talismane.posTagger.features.PosTaggerFeatureService;
import com.joliciel.talismane.posTagger.features.PosTaggerRule;
import com.joliciel.talismane.posTagger.filters.PosTagFilterService;
import com.joliciel.talismane.posTagger.filters.PosTagSequenceFilter;
import com.joliciel.talismane.sentenceDetector.SentenceDetector;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorAnnotatedCorpusReader;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorEvaluator;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorOutcome;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorService;
import com.joliciel.talismane.sentenceDetector.SentenceProcessor;
import com.joliciel.talismane.sentenceDetector.features.SentenceDetectorFeature;
import com.joliciel.talismane.sentenceDetector.features.SentenceDetectorFeatureService;
import com.joliciel.talismane.tokeniser.TokenComparator;
import com.joliciel.talismane.tokeniser.TokenEvaluationCorpusWriter;
import com.joliciel.talismane.tokeniser.TokenEvaluationFScoreCalculator;
import com.joliciel.talismane.tokeniser.TokenEvaluationObserver;
import com.joliciel.talismane.tokeniser.TokenRegexBasedCorpusReader;
import com.joliciel.talismane.tokeniser.TokenSequenceProcessor;
import com.joliciel.talismane.tokeniser.Tokeniser;
import com.joliciel.talismane.tokeniser.Tokeniser.TokeniserType;
import com.joliciel.talismane.tokeniser.TokeniserAnnotatedCorpusReader;
import com.joliciel.talismane.tokeniser.TokeniserEvaluator;
import com.joliciel.talismane.tokeniser.TokeniserGuessTemplateWriter;
import com.joliciel.talismane.tokeniser.TokeniserOutcome;
import com.joliciel.talismane.tokeniser.TokeniserService;
import com.joliciel.talismane.tokeniser.features.TokenFeatureService;
import com.joliciel.talismane.tokeniser.features.TokenPatternMatchFeature;
import com.joliciel.talismane.tokeniser.features.TokeniserContextFeature;
import com.joliciel.talismane.tokeniser.filters.TokenFilter;
import com.joliciel.talismane.tokeniser.filters.TokenFilterService;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;
import com.joliciel.talismane.tokeniser.patterns.PatternTokeniser;
import com.joliciel.talismane.tokeniser.patterns.TokeniserPatternManager;
import com.joliciel.talismane.tokeniser.patterns.TokeniserPatternService;
import com.joliciel.talismane.tokeniser.patterns.TokeniserPatternService.PatternTokeniserType;
import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.io.CurrentFileProvider;
import com.joliciel.talismane.utils.io.DirectoryReader;
import com.joliciel.talismane.utils.io.DirectoryWriter;

class TalismaneConfigImpl implements TalismaneConfig {
	private static final Log LOG = LogFactory.getLog(TalismaneConfigImpl.class);
	private Command command = null;
	private Option option = null;
	private Mode mode = Mode.normal;
	
	private Module startModule = null;
	private Module endModule = null;
	private Module module = null;
	
	private LanguageDetector languageDetector;
	private SentenceDetector sentenceDetector;
	private Tokeniser tokeniser;
	private PosTagger posTagger;
	private Parser parser;
	
	private ParserEvaluator parserEvaluator;
	private PosTaggerEvaluator posTaggerEvaluator;
	private TokeniserEvaluator tokeniserEvaluator;
	private SentenceDetectorEvaluator sentenceDetectorEvaluator;
	private ParseComparator parseComparator;
	private PosTagComparator posTagComparator;
	private TokenComparator tokenComparator;

	private TokeniserAnnotatedCorpusReader tokenCorpusReader = null;
	private PosTagAnnotatedCorpusReader posTagCorpusReader = null;
	private ParserAnnotatedCorpusReader parserCorpusReader = null;
	private ParserAnnotatedCorpusReader parserEvaluationCorpusReader = null;
	private PosTagAnnotatedCorpusReader posTagEvaluationCorpusReader = null;
	private TokeniserAnnotatedCorpusReader tokenEvaluationCorpusReader = null;
	private SentenceDetectorAnnotatedCorpusReader sentenceCorpusReader = null;
	private LanguageDetectorAnnotatedCorpusReader languageCorpusReader = null;

	private LanguageDetectorProcessor languageDetectorProcessor;
	private SentenceProcessor sentenceProcessor;
	private TokenSequenceProcessor tokenSequenceProcessor;
	private PosTagSequenceProcessor posTagSequenceProcessor;
	private ParseConfigurationProcessor parseConfigurationProcessor;
	
	private ClassificationModel<TokeniserOutcome> tokeniserModel = null;
	private ClassificationModel<PosTag> posTaggerModel = null;
	private MachineLearningModel parserModel = null;

	private boolean processByDefault = true;
	private int maxSentenceCount = 0;
	private int startSentence = 0;
	private int beamWidth = 1;
	private boolean propagateBeam = true;
	private boolean includeDetails = false;	
	private Charset inputCharset = null;
	private Charset outputCharset = null;
	
	private int tokeniserBeamWidth = 1;
	private int posTaggerBeamWidth = -1;
	private int parserBeamWidth = -1;
	private boolean propagateTokeniserBeam = false;
	
	private char endBlockCharacter = '\f';
	private String inputRegex;
	private String inputPatternFilePath = null;
	private String evaluationRegex;
	private String evaluationPatternFilePath = null;
	private int maxParseAnalysisTime = 60;
	private int minFreeMemory = 64;
	private boolean earlyStop = false;
	
	private Reader reader = null;
	private Writer writer = null;
	private Reader evaluationReader = null;
	
	private String inFilePath = null;
	private String inDirPath = null;
	private String outFilePath = null;
	private String outDirPath = null;
	private String parserModelFilePath = null;
	private String posTaggerModelFilePath = null;
	private String tokeniserModelFilePath = null;
	private String sentenceModelFilePath = null;
	private String languageModelFilePath = null;
	private String textFiltersPath = null;
	private String tokenFiltersPath = null;
	private String tokenSequenceFilterPath = null;
	private String posTagSequenceFilterPath = null;
	private String templatePath = null;
	private String evaluationFilePath = null;
	private String sentenceReaderPath = null;
	private String posTaggerRuleFilePath = null;
	private String posTaggerFeaturePath = null;
	private String tokeniserFeaturePath = null;
	private String tokeniserPatternFilePath = null;
	private String sentenceFeaturePath = null;
	private String languageFeaturePath = null;
	private String languageCorpusMapPath = null;
	
	private String lexiconPath = null;
	private boolean replaceLexicon = false;

	private String sentenceTemplateName = "sentence_template.ftl";
	private String tokeniserTemplateName = "tokeniser_template.ftl";
	private String posTaggerTemplateName = "posTagger_template.ftl";
	private String parserTemplateName = "parser_conll_template.ftl";
	
	private String fileName = null;
	private boolean logStats = false;
	private File outDir = null;
	private String baseName = null;
	private String suffix = "";
	private boolean outputGuesses = false;
	private int outputGuessCount = 0;
	private boolean labeledEvaluation = true;
	private boolean dynamiseFeatures = false;
	private String skipLabel = null;
	private Set<String> errorLabels = null;
	
	private List<PosTaggerRule> posTaggerRules = null;
	private List<ParserRule> parserRules = null;
	private String parserRuleFilePath = null;
	private String parserFeaturePath = null;
	private List<TextMarkerFilter> textMarkerFilters = null;
	private List<TokenFilter> tokenFilters = null;
	private List<TokenFilter> additionalTokenFilters = new ArrayList<TokenFilter>();
	private List<TokenSequenceFilter> tokenSequenceFilters = null;
	private List<PosTagSequenceFilter> posTaggerPostProcessingFilters = null;
	private boolean includeDistanceFScores = false;
	private boolean includeTransitionLog = false;
	private boolean predictTransitions = false;
	private boolean posTaggerRulesReplace = false;
	private boolean parserRulesReplace = false;
	private boolean tokenFiltersReplace = false;
	private boolean textFiltersReplace = false;
	private boolean tokenSequenceFiltersReplace = false;
	
	private MarkerFilterType newlineMarker = MarkerFilterType.SENTENCE_BREAK;
	private int blockSize = 1000;
	
	private int crossValidationSize = -1;
	private int includeIndex = -1;
	private int excludeIndex = -1;
	
	private Set<String> testWords = null;
	private Set<LanguageDetectorFeature<?>> languageFeatures;
	private Set<SentenceDetectorFeature<?>> sentenceFeatures;
	private Set<TokeniserContextFeature<?>> tokeniserContextFeatures;
	private Set<TokenPatternMatchFeature<?>> tokenPatternMatchFeatures;
	private Set<PosTaggerFeature<?>> posTaggerFeatures;
	private Set<ParseConfigurationFeature<?>> parserFeatures;
	private TokeniserPatternManager tokeniserPatternManager;
	private ClassificationEventStream classificationEventStream;
	private TokeniserType tokeniserType = TokeniserType.pattern;
	private PatternTokeniserType patternTokeniserType = PatternTokeniserType.Compound;

	private boolean parserCorpusReaderFiltersAdded = false;
	private boolean posTagCorpusReaderFiltersAdded = false;
	private boolean tokenCorpusReaderFiltersAdded = false;
	private boolean parserCorpusReaderDecorated = false;
	
	private TalismaneServiceInternal talismaneService;
	private PosTaggerService posTaggerService;
	private ParserService parserService;
	private PosTaggerFeatureService posTaggerFeatureService;
	private ParserFeatureService parserFeatureService;
	private FilterService filterService;
	private TokenFilterService tokenFilterService;
    private SentenceDetectorService sentenceDetectorService;
    private SentenceDetectorFeatureService sentenceDetectorFeatureService;
	private MachineLearningService machineLearningService;
	private TokeniserPatternService tokeniserPatternService;
	private TokenFeatureService tokenFeatureService;
	private TokeniserService tokeniserService;
	private PosTagFilterService posTagFilterService;
	private LanguageDetectorService languageDetectorService;
	
	private File performanceConfigFile;
	private ParseComparisonStrategyType parseComparisonStrategyType;
	private boolean includeLexiconCoverage = false;
	private boolean includeUnknownWordResults = false;
	
	// server parameters
	private int port = 7272;
	
	// training parameters
	int iterations = 0;
	int cutoff = 0;
	MachineLearningAlgorithm algorithm = MachineLearningAlgorithm.MaxEnt;
	double constraintViolationCost = -1;
	double epsilon = -1;
	LinearSVMSolverType solverType = null;
	double perceptronTolerance = -1;
	boolean averageAtIntervals = false;
	List<Integer> perceptronObservationPoints = null;
	String dependencyLabelPath = null;
	String excludeFileName = null;
	
	ExternalResourceFinder externalResourceFinder = null;
	Map<String,List<String>> descriptors = null;
	String parsingConstrainerPath = null;
	ParsingConstrainer parsingConstrainer = null;
	LanguageImplementation implementation;
	TalismaneSession talismaneSession = null;
	
	File baseDir = null;
	
	boolean preloadLexicon = true;
	
	Locale locale = null;
	
	String corpusLexicalEntryRegexPath = null;
	
	public TalismaneConfigImpl(LanguageImplementation implementation) {
		this.implementation = implementation;
	}
	
	/**
	 * Constructor without language implementation - requires languagePack parameter
	 * or else all resources specified individually.
	 */
	public TalismaneConfigImpl(String sessionId) {
		this.implementation = new GenericLanguageImplementation(sessionId);
	}
	
	public void loadParameters(Map<String,String> args) {
		try {
			if (args.size()==0) {
				System.out.println("Talismane usage instructions: ");
				System.out.println("* indicates optional, + indicates default value");
				System.out.println("");
				System.out.println("Usage: command=analyse *startModule=[sentence+|tokenise|postag|parse] *endModule=[sentence|tokenise|postag|parse+] *inFile=[inFilePath, stdin if missing] *outFile=[outFilePath, stdout if missing] *template=[outputTemplatePath]");
				System.out.println("");
				System.out.println("Additional optional parameters:");
				System.out.println(" *encoding=[UTF-8, ...] *includeDetails=[true|false+] posTaggerRules*=[posTaggerRuleFilePath] textFilters*=[regexFilterFilePath] *sentenceModel=[path] *tokeniserModel=[path] *posTaggerModel=[path] *parserModel=[path] *inputPatternFile=[inputPatternFilePath] *posTagSet=[posTagSetPath]");
				return;
			}
			
			String logConfigPath = args.get("logConfigFile");
			if (logConfigPath!=null) {
				args.remove("logConfigFile");
				Properties props = new Properties();
				props.load(new FileInputStream(logConfigPath));
				PropertyConfigurator.configure(props);
			}
			
			String performanceConifPath = args.get("performanceConfigFile");
			if (performanceConifPath!=null) {
				args.remove("performanceConfigFile");
				performanceConfigFile = this.getFile(performanceConifPath);
			}
	
			String encoding = null;
			String inputEncoding = null;
			String outputEncoding = null;
			String builtInTemplate = null;
			
			String posTagSetPath = null;
			String externalResourcePath = null;
			String transitionSystemStr = null;
			
			String languagePackPath = null;
			
			for (Entry<String,String> arg : args.entrySet()) {
				String argName = arg.getKey();
				String argValue = arg.getValue();
				if (argName.equals("command")) {
					String commandString = argValue;
					if (commandString.equals("analyze"))
						commandString = "analyse";
					
					command = Command.valueOf(commandString);
				} else if (argName.equals("option")) {
					option = Option.valueOf(argValue);
				} else if (argName.equals("mode")) {
					mode = Mode.valueOf(argValue);
				} else if (argName.equals("module")) {
					if (argValue.equalsIgnoreCase("sentence")||argValue.equalsIgnoreCase("sentenceDetector"))
						module = Talismane.Module.SentenceDetector;
					else if (argValue.equalsIgnoreCase("tokenise")||argValue.equalsIgnoreCase("tokeniser"))
						module = Talismane.Module.Tokeniser;
					else if (argValue.equalsIgnoreCase("postag")||argValue.equalsIgnoreCase("posTagger"))
						module = Talismane.Module.PosTagger;
					else if (argValue.equalsIgnoreCase("parse")||argValue.equalsIgnoreCase("parser"))
						module = Talismane.Module.Parser;
					else if (argValue.equalsIgnoreCase("language")||argValue.equalsIgnoreCase("languageDetector"))
						module = Talismane.Module.LanguageDetector;
					else
						throw new TalismaneException("Unknown module: " + argValue);
				} else if (argName.equals("startModule")) {
					if (argValue.equalsIgnoreCase("sentence")||argValue.equalsIgnoreCase("sentenceDetector"))
						startModule = Talismane.Module.SentenceDetector;
					else if (argValue.equalsIgnoreCase("tokenise")||argValue.equalsIgnoreCase("tokeniser"))
						startModule = Talismane.Module.Tokeniser;
					else if (argValue.equalsIgnoreCase("postag")||argValue.equalsIgnoreCase("posTagger"))
						startModule = Talismane.Module.PosTagger;
					else if (argValue.equalsIgnoreCase("parse")||argValue.equalsIgnoreCase("parser"))
						startModule = Talismane.Module.Parser;
					else
						throw new TalismaneException("Unknown startModule: " + argValue);
				} else if (argName.equals("endModule")) {
					if (argValue.equalsIgnoreCase("sentence")||argValue.equalsIgnoreCase("sentenceDetector"))
						endModule = Talismane.Module.SentenceDetector;
					else if (argValue.equalsIgnoreCase("tokenise")||argValue.equalsIgnoreCase("tokeniser"))
						endModule = Talismane.Module.Tokeniser;
					else if (argValue.equalsIgnoreCase("postag")||argValue.equalsIgnoreCase("posTagger"))
						endModule = Talismane.Module.PosTagger;
					else if (argValue.equalsIgnoreCase("parse")||argValue.equalsIgnoreCase("parser"))
						endModule = Talismane.Module.Parser;
					else
						throw new TalismaneException("Unknown endModule: " + argValue);
				} else if (argName.equals("inFile"))
					inFilePath = argValue;
				else if (argName.equals("inDir"))
					inDirPath = argValue;
				else if (argName.equals("outFile")) 
					outFilePath = argValue;
				else if (argName.equals("outDir")) 
					outDirPath = argValue;
				else if (argName.equals("template")) 
					templatePath = argValue;
				else if (argName.equals("builtInTemplate"))
					builtInTemplate = argValue;
				else if (argName.equals("encoding")) {
					if (inputEncoding!=null || outputEncoding !=null)
						throw new TalismaneException("The parameter 'encoding' cannot be used with 'inputEncoding' or 'outputEncoding'");
					encoding = argValue;
				} else if (argName.equals("inputEncoding")) {
					if (encoding !=null)
						throw new TalismaneException("The parameter 'encoding' cannot be used with 'inputEncoding' or 'outputEncoding'");
					inputEncoding = argValue;
				} else if (argName.equals("outputEncoding")) {
					if (encoding !=null)
						throw new TalismaneException("The parameter 'encoding' cannot be used with 'inputEncoding' or 'outputEncoding'");
					outputEncoding = argValue;
				} else if (argName.equals("includeDetails"))
					includeDetails = argValue.equalsIgnoreCase("true");
				else if (argName.equals("propagateBeam"))
					propagateBeam = argValue.equalsIgnoreCase("true");
				else if (argName.equals("beamWidth"))
					beamWidth = Integer.parseInt(argValue);
				else if (argName.equals("languageModel"))
					languageModelFilePath = argValue;
				else if (argName.equals("sentenceModel"))
					sentenceModelFilePath = argValue;
				else if (argName.equals("tokeniserModel"))
					tokeniserModelFilePath = argValue;
				else if (argName.equals("posTaggerModel"))
					posTaggerModelFilePath = argValue;
				else if (argName.equals("parserModel"))
					parserModelFilePath = argValue;
				else if (argName.equals("inputPatternFile"))
					inputPatternFilePath = argValue;
				else if (argName.equals("inputPattern"))
					inputRegex = argValue;
				else if (argName.equals("evaluationPatternFile"))
					evaluationPatternFilePath = argValue;
				else if (argName.equals("evaluationPattern"))
					evaluationRegex = argValue;
				else if (argName.equals("posTaggerRules")) {
					if (argValue.startsWith("replace:")) {
						posTaggerRulesReplace = true;
						posTaggerRuleFilePath = argValue.substring("replace:".length());
					} else {
						posTaggerRuleFilePath = argValue;
					}
				}else if (argName.equals("parserRules")) {
					if (argValue.startsWith("replace:")) {
						parserRulesReplace = true;
						parserRuleFilePath = argValue.substring("replace:".length());
					} else {
						parserRuleFilePath = argValue;
					}
				} else if (argName.equals("posTagSet"))
					posTagSetPath = argValue;
				else if (argName.equals("textFilters")) {
					if (argValue.startsWith("replace:")) {
						textFiltersReplace = true;
						textFiltersPath = argValue.substring("replace:".length());
					} else {
						textFiltersPath = argValue;
					}
				} else if (argName.equals("tokenFilters")) {
					if (argValue.startsWith("replace:")) {
						tokenFiltersReplace = true;
						tokenFiltersPath = argValue.substring("replace:".length());
					} else {
						tokenFiltersPath = argValue;
					}
				}
				else if (argName.equals("tokenSequenceFilters")) {
					if (argValue.startsWith("replace:")) {
						tokenSequenceFiltersReplace = true;
						tokenSequenceFilterPath = argValue.substring("replace:".length());
					} else {
						tokenSequenceFilterPath = argValue;
					}
				} else if (argName.equals("posTagSequenceFilters"))
					posTagSequenceFilterPath = argValue;
				else if (argName.equals("logStats"))
					logStats = argValue.equalsIgnoreCase("true");
				else if (argName.equals("newline"))
					newlineMarker = MarkerFilterType.valueOf(argValue);
				else if (argName.equals("fileName"))
					fileName = argValue;
				else if (argName.equals("processByDefault"))
					processByDefault = argValue.equalsIgnoreCase("true");
				else if (argName.equals("maxParseAnalysisTime"))
					maxParseAnalysisTime = Integer.parseInt(argValue);
				else if (argName.equals("minFreeMemory"))
					minFreeMemory = Integer.parseInt(argValue);
				else if (argName.equals("transitionSystem"))
					transitionSystemStr = argValue;
				else if (argName.equals("sentenceCount"))
					maxSentenceCount = Integer.parseInt(argValue);
				else if (argName.equals("startSentence"))
					startSentence = Integer.parseInt(argValue);
				else if (argName.equals("endBlockCharCode"))
					endBlockCharacter = (char) Integer.parseInt(argValue);
				else if (argName.equals("outputGuesses"))
					outputGuesses = argValue.equalsIgnoreCase("true");
				else if (argName.equals("outputGuessCount"))
					outputGuessCount = Integer.parseInt(argValue);
				else if (argName.equals("suffix"))
					suffix = argValue;
				else if (argName.equals("includeDistanceFScores"))
					includeDistanceFScores = argValue.equalsIgnoreCase("true");
				else if (argName.equals("includeTransitionLog"))
					includeTransitionLog = argValue.equalsIgnoreCase("true");
				else if (argName.equals("evaluationFile"))
					evaluationFilePath = argValue;
				else if (argName.equals("labeledEvaluation"))
					labeledEvaluation = argValue.equalsIgnoreCase("true");
				else if (argName.equals("tokeniserBeamWidth"))
					tokeniserBeamWidth = Integer.parseInt(argValue);
				else if (argName.equals("posTaggerBeamWidth"))
					posTaggerBeamWidth = Integer.parseInt(argValue);
				else if (argName.equals("parserBeamWidth"))
					parserBeamWidth = Integer.parseInt(argValue);
				else if (argName.equals("propagateTokeniserBeam"))
					propagateTokeniserBeam = argValue.equalsIgnoreCase("true");
				else if (argName.equals("blockSize"))
					blockSize = Integer.parseInt(argValue);
				else if (argName.equals("crossValidationSize"))
					crossValidationSize = Integer.parseInt(argValue);
				else if (argName.equals("includeIndex"))
					includeIndex = Integer.parseInt(argValue);
				else if (argName.equals("excludeIndex"))
					excludeIndex = Integer.parseInt(argValue);
				else if (argName.equals("dynamiseFeatures"))
					dynamiseFeatures = argValue.equalsIgnoreCase("true");
				else if (argName.equals("predictTransitions"))
					predictTransitions = argValue.equalsIgnoreCase("true");
				else if (argName.equals("lexicon")) {
					if (argValue.startsWith("replace:")) {
						replaceLexicon = true;
						lexiconPath = argValue.substring("replace:".length());
					} else {
						lexiconPath = argValue;
					}
				} else if (argName.equals("perceptronScoring")) {
					PerceptronScoring perceptronScoring = PerceptronScoring.valueOf(argValue);
					MachineLearningSession.setPerceptronScoring(perceptronScoring);
				} else if (argName.equals("parseComparisonStrategy")) {
					parseComparisonStrategyType = ParseComparisonStrategyType.valueOf(argValue);
				} else if (argName.equals("sentenceReader")) {
					sentenceReaderPath = argValue;
				} else if (argName.equals("skipLabel")) {
					skipLabel = argValue;
				} else if (argName.equals("errorLabels")) {
					errorLabels = new HashSet<String>();
					String[] labels = argValue.split(",");
					for (String label : labels) {
						errorLabels.add(label);
					}
				} else if (argName.equals("earlyStop")) {
					earlyStop = argValue.equalsIgnoreCase("true");
				} else if (argName.equals("languageFeatures")) {
					languageFeaturePath = argValue;
				} else if (argName.equals("sentenceFeatures")) {
					sentenceFeaturePath = argValue;
				} else if (argName.equals("tokeniserFeatures")) {
					tokeniserFeaturePath = argValue;
				} else if (argName.equals("tokeniserPatterns")) {
					tokeniserPatternFilePath = argValue;
				} else if (argName.equals("posTaggerFeatures")) {
					posTaggerFeaturePath = argValue;
				} else if (argName.equals("parserFeatures")) {
					parserFeaturePath = argValue;
				} else if (argName.equals("externalResources")) {
					externalResourcePath = argValue;
				} else if (argName.equals("testWords")) {
					String[] parts = argValue.split(";");
					testWords = new HashSet<String>();
					for (String part : parts)
						testWords.add(part);
				} else if (argName.equals("includeLexiconCoverage")) {
					includeLexiconCoverage = argValue.equalsIgnoreCase("true");
				} else if (argName.equals("includeUnknownWordResults")) {
					includeUnknownWordResults = argValue.equalsIgnoreCase("true");
				}
				else if (argName.equals("iterations"))
					iterations = Integer.parseInt(argValue);
				else if (argName.equals("cutoff"))
					cutoff = Integer.parseInt(argValue);
				else if (argName.equals("dependencyLabels"))
					dependencyLabelPath = argValue;
				else if (argName.equals("parsingConstrainer"))
					parsingConstrainerPath = argValue;
				else if (argName.equals("algorithm"))
					algorithm = MachineLearningAlgorithm.valueOf(argValue);
				else if (argName.equals("linearSVMSolver"))
					solverType = LinearSVMSolverType.valueOf(argValue);
				else if (argName.equals("linearSVMCost"))
					constraintViolationCost = Double.parseDouble(argValue);
				else if (argName.equals("linearSVMEpsilon"))
					epsilon = Double.parseDouble(argValue);
				else if (argName.equals("perceptronTolerance"))
					perceptronTolerance = Double.parseDouble(argValue);
				else if (argName.equals("averageAtIntervals"))
					averageAtIntervals = argValue.equalsIgnoreCase("true");
				else if (argName.equals("perceptronObservationPoints")) {
					String[] points = argValue.split(",");
					perceptronObservationPoints = new ArrayList<Integer>();
					for (String point : points)
						perceptronObservationPoints.add(Integer.parseInt(point));
				}
				else if (argName.equals("tokeniserType")) {
					tokeniserType = TokeniserType.valueOf(argValue);
				}
				else if (argName.equals("patternTokeniser"))
					patternTokeniserType = PatternTokeniserType.valueOf(argValue);
				else if (argName.equals("excludeFile")) {
					excludeFileName = argValue;
				} else if (argName.equals("port")) {
					port = Integer.parseInt(argValue);
				} else if (argName.equals("preloadLexicon")) {
					preloadLexicon = argValue.equalsIgnoreCase("true");
				} else if (argName.equals("locale")) {
					locale = Locale.forLanguageTag(argValue);
				} else if (argName.equals("languageCorpusMap")) {
					languageCorpusMapPath = argValue;
				} else if (argName.equals("corpusLexicalEntryRegex")) {
					corpusLexicalEntryRegexPath = argValue;
				} else if (argName.equals("languagePack")) {
					languagePackPath = argValue;
				} else {
					System.out.println("Unknown argument: " + argName);
					throw new RuntimeException("Unknown argument: " + argName);
				}
			}
			
			if (command==null)
				throw new TalismaneException("No command provided.");
			
			if (!(implementation instanceof LanguagePackImplementation) && languagePackPath!=null)
				throw new TalismaneException("The implementation " + implementation.getClass().getSimpleName() + " does not accept language packs");
			
			if (implementation instanceof LanguagePackImplementation) {
				if (languagePackPath!=null) {
			   		File languagePackFile = this.getFile(languagePackPath);
		    		if (!languagePackFile.exists())
		    			throw new TalismaneException("languagePack: could not find file: " + languagePackFile.getPath());
		    		
		    		LOG.debug("Setting language pack to " + languagePackFile.getPath());
		    		((LanguagePackImplementation)implementation).setLanguagePack(languagePackFile);
				}
			}
			
			if (command.equals(Command.evaluate)) {
				if (outDirPath.length()==0)
					throw new TalismaneException("Missing argument: outdir");
			}
			
			if (startModule==null)
				startModule = module;
			if (startModule==null)
				startModule = Module.SentenceDetector;
			if (endModule==null)
				endModule = module;
			if (endModule==null)
				endModule = Module.Parser;
			if (module==null)
				module = endModule;
			
			if (command==Command.train) {
				if (module==Module.LanguageDetector) {
					if (languageModelFilePath==null)
						throw new TalismaneException("languageModel is required when training a language detector model");
					if (languageCorpusMapPath==null)
						throw new TalismaneException("languageCorpusMap is required when training a language detector model");
					if (languageFeaturePath==null)
						throw new TalismaneException("languageFeatures is required when training a language detector model");
				} else if (module==Module.SentenceDetector) {
					if (sentenceModelFilePath==null)
						throw new TalismaneException("sentenceModel is required when training a sentence detector model");
					if (sentenceFeaturePath==null)
						throw new TalismaneException("sentenceFeatures is required when training a sentence detector model");
				} else if (module==Module.Tokeniser) {
					if (tokeniserModelFilePath==null)
						throw new TalismaneException("tokeniserModel is required when training a tokeniser model");
					if (tokeniserFeaturePath==null)
						throw new TalismaneException("tokeniserFeatures is required when training a tokeniser model");
				} else if (module==Module.PosTagger) {
					if (posTaggerModelFilePath==null)
						throw new TalismaneException("posTaggerModel is required when training a posTagger model");
					if (posTaggerFeaturePath==null)
						throw new TalismaneException("posTaggerFeatures is required when training a posTagger model");
				} else if (module==Module.Parser) {
					this.predictTransitions = true;
					
					if (parserModelFilePath==null)
						throw new TalismaneException("parserModel is required when training a parser model");
					if (parserFeaturePath==null)
						throw new TalismaneException("parserFeatures is required when training a parser model");
				}
			}
			
			if (builtInTemplate!=null) {
				if (builtInTemplate.equalsIgnoreCase("with_location")) {
					tokeniserTemplateName = "tokeniser_template_with_location.ftl";
					posTaggerTemplateName = "posTagger_template_with_location.ftl";
					parserTemplateName = "parser_conll_template_with_location.ftl";
				} else if (builtInTemplate.equalsIgnoreCase("with_prob")) {
					tokeniserTemplateName = "tokeniser_template_with_prob.ftl";
					posTaggerTemplateName = "posTagger_template_with_prob.ftl";
					parserTemplateName = "parser_conll_template_with_prob.ftl";
				} else if (builtInTemplate.equalsIgnoreCase("with_comments")) {
					posTaggerTemplateName = "posTagger_template_with_comments.ftl";
					parserTemplateName = "parser_conll_template_with_comments.ftl";
				} else {
					throw new TalismaneException("Unknown builtInTemplate: " + builtInTemplate);
				}
			}
			
			if (posTaggerBeamWidth<0)
				posTaggerBeamWidth = beamWidth;
			if (parserBeamWidth<0)
				parserBeamWidth = beamWidth;
			
			inputCharset = Charset.defaultCharset();
			outputCharset = Charset.defaultCharset();
			if (encoding!=null) {
				inputCharset = Charset.forName(encoding);
				outputCharset = Charset.forName(encoding);
			} else {
				if (inputEncoding!=null)
					inputCharset = Charset.forName(inputEncoding);
				if (outputEncoding!=null)
					outputCharset = Charset.forName(outputEncoding);
			}
	
			if (fileName==null && inFilePath!=null) {
				fileName = inFilePath;
			}
			
			if (posTagSetPath!=null) {
				File posTagSetFile = this.getFile(posTagSetPath);
				Scanner posTagSetScanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(posTagSetFile), this.getInputCharset().name())));
				
				PosTagSet posTagSet = this.getPosTaggerService().getPosTagSet(posTagSetScanner);
				talismaneSession.setPosTagSet(posTagSet);
			}
			
			if (transitionSystemStr!=null) {
				TransitionSystem transitionSystem = null;
				if (transitionSystemStr.equalsIgnoreCase("ShiftReduce")) {
					transitionSystem = this.getParserService().getShiftReduceTransitionSystem();
				} else if (transitionSystemStr.equalsIgnoreCase("ArcEager")) {
					transitionSystem = this.getParserService().getArcEagerTransitionSystem();
				} else {
					throw new TalismaneException("Unknown transition system: " + transitionSystemStr);
				}
				
				if (dependencyLabelPath!=null) {
					File dependencyLabelFile = this.getFile(dependencyLabelPath);
					Scanner depLabelScanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(dependencyLabelFile), "UTF-8")));
					List<String> dependencyLabels = new ArrayList<String>();
					while (depLabelScanner.hasNextLine()) {
						String dependencyLabel = depLabelScanner.nextLine();
						if (!dependencyLabel.startsWith("#"))
							dependencyLabels.add(dependencyLabel);
					}
					transitionSystem.setDependencyLabels(dependencyLabels);
				}
				
				talismaneSession.setTransitionSystem(transitionSystem);
			}
			
			if (this.lexiconPath!=null) {				
				File lexiconFile = this.getFile(lexiconPath);
				if (!lexiconFile.exists())
					throw new TalismaneException("lexicon: File " + lexiconPath + " does not exist");
				
				LexiconDeserializer lexiconDeserializer = new LexiconDeserializer(talismaneSession);
				List<PosTaggerLexicon> lexicons = lexiconDeserializer.deserializeLexicons(lexiconFile);
				for (PosTaggerLexicon oneLexicon : lexicons) {
					talismaneSession.addLexicon(oneLexicon);
				}
				
				if (!replaceLexicon) {
					List<PosTaggerLexicon> defaultLexicons = this.implementation.getDefaultLexicons();
					if (defaultLexicons!=null) {
						for (PosTaggerLexicon oneLexicon : defaultLexicons) {
							talismaneSession.addLexicon(oneLexicon);
						}
					}
				}
			}
	
			if (externalResourcePath!=null) {
				externalResourceFinder = this.getMachineLearningService().getExternalResourceFinder();
				
				List<String> paths = new ArrayList<String>();
				if (externalResourcePath!=null && externalResourcePath.length()>0) {
					LOG.info("externalResourcePath: " + externalResourcePath);
					String[] parts = externalResourcePath.split(";");
					for (String part : parts)
						paths.add(part);
				}
				
				for (String path : paths) {
					LOG.info("Reading external resources from " + path);
					if (path.length()>0) {
						File externalResourceFile = this.getFile(path);
						externalResourceFinder.addExternalResources(externalResourceFile);
					}
				}
				
				ExternalResourceFinder parserResourceFinder = this.getParserFeatureService().getExternalResourceFinder();
				ExternalResourceFinder posTaggerResourceFinder = this.getPosTaggerFeatureService().getExternalResourceFinder();
				ExternalResourceFinder tokeniserResourceFinder = this.getTokenFeatureService().getExternalResourceFinder();
				ExternalResourceFinder sentenceResourceFinder = this.getSentenceDetectorFeatureService().getExternalResourceFinder();
				for (ExternalResource<?> externalResource : externalResourceFinder.getExternalResources()) {
					parserResourceFinder.addExternalResource(externalResource);
					posTaggerResourceFinder.addExternalResource(externalResource);
					tokeniserResourceFinder.addExternalResource(externalResource);
					sentenceResourceFinder.addExternalResource(externalResource);
				}
				
				ExternalResourceFinder tokenFilterResourceFinder = this.getTokenFilterService().getExternalResourceFinder();
				for (ExternalWordList externalWordList : externalResourceFinder.getExternalWordLists()) {
					tokenFilterResourceFinder.addExternalWordList(externalWordList);
				}
			}
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * The actual command to run by Talismane.
	 * @return
	 */
	@Override
	public Command getCommand() {
		return command;
	}
	@Override
	public void setCommand(Command command) {
		this.command = command;
	}

	/**
	 * If the command required a start module (e.g. analyse), the start module for this command.
	 * Default is {@link com.joliciel.talismane.Talismane.Module#SentenceDetector}.
	 * @return
	 */
	@Override
	public Module getStartModule() {
		return startModule;
	}
	@Override
	public void setStartModule(Module startModule) {
		this.startModule = startModule;
	}

	/**
	 * If the command requires an end module (e.g. analyse), the end module for this command.
	 * Default is {@link com.joliciel.talismane.Talismane.Module#Parser}.
	 * @return
	 */
	@Override
	public Module getEndModule() {
		return endModule;
	}
	@Override
	public void setEndModule(Module endModule) {
		this.endModule = endModule;
	}

	/**
	 * For commands which only affect a single module (e.g. evaluate), the module for this command.
	 * @return
	 */
	@Override
	public Module getModule() {
		return module;
	}
	@Override
	public void setModule(Module module) {
		this.module = module;
	}

	/**
	 * When analysing, should the raw text be processed by default, or should we wait until a text
	 * marker filter tells us to start processing. Default is true.
	 * @return
	 */
	@Override
	public boolean isProcessByDefault() {
		return processByDefault;
	}
	@Override
	public void setProcessByDefault(boolean processByDefault) {
		this.processByDefault = processByDefault;
	}

	/**
	 * For the "process" command, the maximum number of sentences to process. If <=0, all sentences
	 * will be processed. Default is 0 (all).
	 * @return
	 */
	@Override
	public int getMaxSentenceCount() {
		return maxSentenceCount;
	}
	@Override
	public void setMaxSentenceCount(int maxSentenceCount) {
		this.maxSentenceCount = maxSentenceCount;
	}

	/**
	 * The charset that is used to interpret the input stream.
	 * @return
	 */
	@Override
	public Charset getInputCharset() {
		return inputCharset;
	}
	@Override
	public void setInputCharset(Charset inputCharset) {
		this.inputCharset = inputCharset;
	}

	/**
	 * The charset that is used to write to the output stream.
	 * @return
	 */
	@Override
	public Charset getOutputCharset() {
		return outputCharset;
	}
	@Override
	public void setOutputCharset(Charset outputCharset) {
		this.outputCharset = outputCharset;
	}

	/**
	 * A character (typically non-printing) which will mark a stop in the input stream and set-off analysis.
	 * The default value is the form-feed character (code=12).
	 * @return
	 */
	@Override
	public char getEndBlockCharacter() {
		return endBlockCharacter;
	}
	@Override
	public void setEndBlockCharacter(char endBlockCharacter) {
		this.endBlockCharacter = endBlockCharacter;
	}

	/**
	 * The beam width for beam-search analysis. Default is 1.
	 * Increasing this value will increase analysis time in a linear fashion, but will typically improve results.
	 * @return
	 */
	@Override
	public int getBeamWidth() {
		return beamWidth;
	}
	@Override
	public void setBeamWidth(int beamWidth) {
		this.beamWidth = beamWidth;
	}

	/**
	 * If true, the full beam of analyses produced as output by a given module will be used as input for the next module.
	 * If false, only the single best analysis will be used as input for the next module.
	 * @return
	 */
	@Override
	public boolean isPropagateBeam() {
		return propagateBeam;
	}
	@Override
	public void setPropagateBeam(boolean propagateBeam) {
		this.propagateBeam = propagateBeam;
	}

	/**
	 * If true, a generates a very detailed analysis on how Talismane obtained the results it displays.
	 * @return
	 */
	@Override
	public boolean isIncludeDetails() {
		return includeDetails;
	}
	@Override
	public void setIncludeDetails(boolean includeDetails) {
		this.includeDetails = includeDetails;
	}

	/**
	 * The reader to be used to read the data for this analysis.
	 * @return
	 */
	@Override
	public Reader getReader() {
		if (this.reader==null) {
			if (inFilePath!=null) {
				try {
					File inFile = this.getFile(inFilePath);
					if (!inFile.exists())
						throw new TalismaneException("inFile does not exist: " + inFilePath);
					if (inFile.isDirectory())
						throw new TalismaneException("inFile must be a file, not a directory - use inDir instead: " + inFilePath);
					
					this.reader = new BufferedReader(new InputStreamReader(new FileInputStream(inFile), this.getInputCharset()));
					
				} catch (FileNotFoundException fnfe) {
					LogUtils.logError(LOG, fnfe);
					throw new RuntimeException(fnfe);
				}
			} else if (inDirPath!=null) {
				File inDir = this.getFile(inDirPath);
				if (!inDir.exists())
					throw new TalismaneException("inDir does not exist: " + inDirPath);
				if (inDir.isDirectory()) {
					DirectoryReader directoryReader = new DirectoryReader(inDir, this.getInputCharset());
					if (this.command == Command.analyse) {
						directoryReader.setEndOfFileString("\n" + this.getEndBlockCharacter());
					} else {
						directoryReader.setEndOfFileString("\n");
					}
					this.reader = directoryReader;
				} else {
					throw new TalismaneException("inDir must be a directory, not a file - use inFile instead: " + inDirPath);
				}
			} else {
				this.reader = new BufferedReader(new InputStreamReader(System.in, this.getInputCharset()));
			}
		}
		return reader;
	}
	
	/**
	 * The reader to be used to read the data for evaluation, when command=compare.
	 * @return
	 */
	@Override
	public Reader getEvaluationReader() {
		if (this.evaluationReader==null) {
			try {
				File inFile = this.getFile(evaluationFilePath);
				this.evaluationReader = new BufferedReader(new InputStreamReader(new FileInputStream(inFile), this.getInputCharset()));
			} catch (FileNotFoundException fnfe) {
				LogUtils.logError(LOG, fnfe);
				throw new RuntimeException(fnfe);
			}
		}
		return evaluationReader;
	}

	/**
	 * A writer to which Talismane should write its output when analysing.
	 * @return
	 */
	@Override
	public Writer getWriter() {
		try {
			if (writer==null) {
				if (outFilePath!=null) {
					File outFile = this.getFile(outFilePath);
					File outDir = outFile.getParentFile();
					if (outDir!=null)
						outDir.mkdirs();
					outFile.delete();
					outFile.createNewFile();
				
					writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), this.getOutputCharset()));
				} else if (outDirPath!=null && inDirPath !=null && (this.getReader() instanceof CurrentFileProvider) && command!=Command.evaluate) {
					File outDir = this.getFile(outDirPath);
					outDir.mkdirs();
					File inDir = this.getFile(inDirPath);
					
					if (this.suffix == null)
						this.suffix = "";
					DirectoryWriter directoryWriter = new DirectoryWriter(inDir, outDir, suffix, this.getOutputCharset());
					this.writer = directoryWriter;
					((CurrentFileProvider) this.getReader()).addCurrentFileObserver(directoryWriter);
				} else {
					writer = new BufferedWriter(new OutputStreamWriter(System.out, this.getOutputCharset()));
				}
			}
			return writer;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * The filename to be applied to this analysis (if filename is included in the output).
	 * @return
	 */
	@Override
	public String getFileName() {
		return fileName;
	}

	/**
	 * The directory to which we write any output files.
	 * @return
	 */
	@Override
	public File getOutDir() {
		if (outDirPath!=null) {
			outDir = this.getFile(outDirPath);
			outDir.mkdirs();
		} else if (outFilePath!=null) {
			File outFile = this.getFile(outFilePath);
			outDir = outFile.getParentFile();
			if (outDir!=null) {
				outDir.mkdirs();
			}
		}
		return outDir;
	}

	/**
	 * The rules to apply when running the pos-tagger.
	 * @return
	 */
	@Override
	public List<PosTaggerRule> getPosTaggerRules() {
		try {
			if (posTaggerRules == null) {
				posTaggerRules = new ArrayList<PosTaggerRule>();
				for (int i=0; i<=1; i++) {
					Scanner rulesScanner = null;
					if (i==0) {
						if (posTaggerRulesReplace)
							continue;
						rulesScanner = this.implementation.getDefaultPosTaggerRulesScanner();
					} else {
						if (posTaggerRuleFilePath!=null && posTaggerRuleFilePath.length()>0) {
							File posTaggerRuleFile = this.getFile(posTaggerRuleFilePath);
							if (!posTaggerRuleFile.exists()) {
								throw new TalismaneException("posTaggerRules: File " + posTaggerRuleFilePath + " does not exist");
							}
							rulesScanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(posTaggerRuleFile), this.getInputCharset().name())));
						}
					}
					
					if (rulesScanner!=null) {
						List<String> ruleDescriptors = new ArrayList<String>();
						while (rulesScanner.hasNextLine()) {
							String ruleDescriptor = rulesScanner.nextLine();
							if (ruleDescriptor.length()>0) {
								ruleDescriptors.add(ruleDescriptor);
								LOG.trace(ruleDescriptor);
							}
						}
						List<PosTaggerRule> rules = this.getPosTaggerFeatureService().getRules(ruleDescriptors);
						posTaggerRules.addAll(rules);
						
					}
				}
			}
			return posTaggerRules;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * The rules to apply when running the parser.
	 * @return
	 */
	@Override
	public List<ParserRule> getParserRules() {
		try {
			if (parserRules == null) {
				parserRules = new ArrayList<ParserRule>();
				if (parserRuleFilePath!=null && parserRuleFilePath.equalsIgnoreCase("null")) {
					// add no rules! (not even built-in ones)
				} else {
					for (int i=0; i<=1; i++) {
						Scanner rulesScanner = null;
						if (i==0) {
							if (parserRulesReplace)
								continue;
							rulesScanner = this.implementation.getDefaultParserRulesScanner();
						} else {
							if (parserRuleFilePath!=null && parserRuleFilePath.length()>0) {
								File parserRuleFile = this.getFile(parserRuleFilePath);
								if (!parserRuleFile.exists()) {
									throw new TalismaneException("parserRules: File " + parserRuleFilePath + " does not exist");
								}
								rulesScanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(parserRuleFile), this.getInputCharset().name())));
							}
						}
						
						if (rulesScanner!=null) {
							List<String> ruleDescriptors = new ArrayList<String>();
							while (rulesScanner.hasNextLine()) {
								String ruleDescriptor = rulesScanner.nextLine();
								if (ruleDescriptor.length()>0) {
									ruleDescriptors.add(ruleDescriptor);
									LOG.trace(ruleDescriptor);
								}
							}
							List<ParserRule> rules = this.getParserFeatureService().getRules(ruleDescriptors, dynamiseFeatures);
							parserRules.addAll(rules);
							
						}
					}
				}
			}
			return parserRules;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * A regex used to process the input, when pre-annotated.
	 * @return
	 */
	@Override
	public String getInputRegex() {
		try {
			if (inputRegex==null && inputPatternFilePath!=null && inputPatternFilePath.length()>0) {
				Scanner inputPatternScanner = null;
				File inputPatternFile = this.getFile(inputPatternFilePath);
				inputPatternScanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(inputPatternFile), this.getInputCharset().name())));
				if (inputPatternScanner.hasNextLine()) {
					inputRegex = inputPatternScanner.nextLine();
				}
				inputPatternScanner.close();
				if (inputRegex==null)
					throw new TalismaneException("No input pattern found in " + inputPatternFilePath);
			}
			return inputRegex;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
	
	public void setInputRegex(String inputRegex) {
		this.inputRegex = inputRegex;
	}

	/**
	 * A regex used to process the evaluation corpus.
	 * @return
	 */
	@Override
	public String getEvaluationRegex() {
		try {
			if (evaluationRegex==null) {
				if (evaluationPatternFilePath!=null && evaluationPatternFilePath.length()>0) {
					Scanner evaluationPatternScanner = null;
					File evaluationPatternFile = this.getFile(evaluationPatternFilePath);
					evaluationPatternScanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(evaluationPatternFile), this.getInputCharset().name())));
					if (evaluationPatternScanner.hasNextLine()) {
						evaluationRegex = evaluationPatternScanner.nextLine();
					}
					evaluationPatternScanner.close();
					if (evaluationRegex==null)
						throw new TalismaneException("No evaluation pattern found in " + evaluationPatternFilePath);
				} else {
					evaluationRegex = this.getInputRegex();
				}
			}
			return evaluationRegex;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Text marker filters are applied to raw text segments extracted from the stream, 3 segments at a time.
	 * This means that if a particular marker crosses segment borders, it is handled correctly.
	 * @return
	 */
	@Override
	public List<TextMarkerFilter> getTextMarkerFilters() {
		try {
			if (textMarkerFilters==null) {
				textMarkerFilters = new ArrayList<TextMarkerFilter>();
				
				// insert sentence breaks at end of block
				this.addTextMarkerFilter(this.getFilterService().getRegexMarkerFilter(new MarkerFilterType[] { MarkerFilterType.SENTENCE_BREAK }, "" + endBlockCharacter, blockSize));
				
				// handle newline as requested
				if (newlineMarker.equals(MarkerFilterType.SENTENCE_BREAK))
					this.addTextMarkerFilter(this.getFilterService().getNewlineEndOfSentenceMarker());
				else if (newlineMarker.equals(MarkerFilterType.SPACE))
					this.addTextMarkerFilter(this.getFilterService().getNewlineSpaceMarker());
				
				// get rid of duplicate white-space always
				this.addTextMarkerFilter(this.getFilterService().getDuplicateWhiteSpaceFilter());
	
				List<String> paths = new ArrayList<String>();
				if (textFiltersPath!=null && textFiltersPath.length()>0) {
					LOG.debug("textFiltersPath: " + textFiltersPath);
					String[] parts = textFiltersPath.split(";");
					for (String part : parts)
						paths.add(part);
				}
				if (!textFiltersReplace) {
					// default text filter path
					paths.add("");
				}
				
				for (String path : paths) {
					LOG.debug("Text marker filters");
					Scanner textFilterScanner = null;
					if (path.length()>0) {
						LOG.debug("From: " + path);
						File textFilterFile = this.getFile(path);
						if (!textFilterFile.exists()) {
							throw new TalismaneException("textFilters: File " + path + " does not exist");
						}
						textFilterScanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(textFilterFile), this.getInputCharset().name())));
					} else {
						LOG.debug("From default");
						textFilterScanner = this.implementation.getDefaultTextMarkerFiltersScanner();
					}
					if (textFilterScanner!=null) {
						while (textFilterScanner.hasNextLine()) {
							String descriptor = textFilterScanner.nextLine();
							LOG.debug(descriptor);
							if (descriptor.length()>0 && !descriptor.startsWith("#")) {
								TextMarkerFilter textMarkerFilter = this.getFilterService().getTextMarkerFilter(descriptor, blockSize);
								this.addTextMarkerFilter(textMarkerFilter);
							}
						}
					}
				}
				
			}
			return textMarkerFilters;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setTextMarkerFilters(List<TextMarkerFilter> textMarkerFilters) {
		this.textMarkerFilters = textMarkerFilters;
	}

	@Override
	public void addTextMarkerFilter(TextMarkerFilter textMarkerFilter) {
		this.textMarkerFilters.add(textMarkerFilter);
	}
	
	/**
	 * TokenFilters to be applied during analysis.
	 * @return
	 */
	private List<TokenSequenceFilter> getTokenSequenceFilters(MachineLearningModel model) {
		try {
			if (tokenSequenceFilters==null) {
				List<String> tokenSequenceFilterDescriptors = new ArrayList<String>();
				tokenSequenceFilters = new ArrayList<TokenSequenceFilter>();
				
				LOG.debug("Token sequence filters");
				
				List<Scanner> scanners = new ArrayList<Scanner>();
				if (tokenSequenceFilterPath!=null && tokenSequenceFilterPath.length()>0) {
					LOG.debug("tokenSequenceFilterPath: " + tokenSequenceFilterPath);
					String[] parts = tokenSequenceFilterPath.split(";");
					for (String part : parts) {
						if (part.length()>0) {
							LOG.debug("From: " + part);
							File tokenSequenceFilterFile = this.getFile(part);
							if (!tokenSequenceFilterFile.exists()) {
								throw new TalismaneException("tokenSequenceFilters: File " + part + " does not exist");
							}
							Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(tokenSequenceFilterFile), this.getInputCharset())));
							scanners.add(scanner);
						}
					}
				}
				if (!tokenSequenceFiltersReplace) {
					if (model!=null) {
						LOG.debug("From model");
						List<String> modelDescriptors = model.getDescriptors().get(PosTagFilterService.POSTAG_PREPROCESSING_FILTER_DESCRIPTOR_KEY);
						String modelDescriptorString = "";
						if (modelDescriptors!=null) {
							for (String descriptor : modelDescriptors) {
								modelDescriptorString += descriptor + "\n";
							}
						}
						Scanner scanner = new Scanner(modelDescriptorString);
						scanners.add(scanner);
					} else {
						// default token filters
						LOG.debug("From default");
						Scanner scanner = this.implementation.getDefaultTokenSequenceFiltersScanner();
						scanners.add(scanner);
					}
				}
				
				for (Scanner scanner : scanners) {
					while (scanner.hasNextLine()) {
						String descriptor = scanner.nextLine();
						LOG.debug(descriptor);
						tokenSequenceFilterDescriptors.add(descriptor);
						if (descriptor.length()>0 && !descriptor.startsWith("#")) {
							TokenSequenceFilter tokenSequenceFilter = this.getTokenFilterService().getTokenSequenceFilter(descriptor);
							if (tokenSequenceFilter instanceof NeedsTalismaneSession)
								((NeedsTalismaneSession) tokenSequenceFilter).setTalismaneSession(talismaneSession);
							tokenSequenceFilters.add(tokenSequenceFilter);
						}
					}
				}
				
				this.getDescriptors().put(PosTagFilterService.POSTAG_PREPROCESSING_FILTER_DESCRIPTOR_KEY, tokenSequenceFilterDescriptors);
			}
			return tokenSequenceFilters;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
	
	private List<PosTagSequenceFilter> getPosTagSequenceFilters(MachineLearningModel model) {
		try {
			if (posTaggerPostProcessingFilters==null) {
				List<String> posTaggerPostProcessingFilterDescriptors = new ArrayList<String>();
				posTaggerPostProcessingFilters = new ArrayList<PosTagSequenceFilter>();
				
				List<Scanner> scanners = new ArrayList<Scanner>();
				
				if (posTagSequenceFilterPath!=null) {
					File filterFile = this.getFile(posTagSequenceFilterPath);
					Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(filterFile), this.getInputCharset())));
					scanners.add(scanner);
				} else if (model!=null) {
					List<String> modelDescriptors = model.getDescriptors().get(PosTagFilterService.POSTAG_POSTPROCESSING_FILTER_DESCRIPTOR_KEY);
					if (modelDescriptors!=null) {
						String modelDescriptorString = "";
						if (modelDescriptors!=null) {
							for (String descriptor : modelDescriptors) {
								modelDescriptorString += descriptor + "\n";
							}
						}
						Scanner scanner = new Scanner(modelDescriptorString);
						scanners.add(scanner);
					}
				}
				
				for (Scanner scanner : scanners) {
					while (scanner.hasNextLine()) {
						String descriptor = scanner.nextLine();
						LOG.debug(descriptor);
						posTaggerPostProcessingFilterDescriptors.add(descriptor);
						if (descriptor.length()>0 && !descriptor.startsWith("#")) {
							PosTagSequenceFilter filter = this.getPosTagFilterService().getPosTagSequenceFilter(descriptor);
							posTaggerPostProcessingFilters.add(filter);
						}
					}
				}

				
				this.getDescriptors().put(PosTagFilterService.POSTAG_POSTPROCESSING_FILTER_DESCRIPTOR_KEY, posTaggerPostProcessingFilterDescriptors);

			}
			return posTaggerPostProcessingFilters;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
	
	
	/**
	 * TokenFilters to be applied during analysis.
	 * @return
	 */
	private List<TokenFilter> getTokenFilters(MachineLearningModel model) {
		try {
			if (tokenFilters==null) {
				List<String> tokenFilterDescriptors = new ArrayList<String>();
				tokenFilters = new ArrayList<TokenFilter>();
				
				LOG.debug("Token filters");
				
				List<Scanner> scanners = new ArrayList<Scanner>();
				if (tokenFiltersPath!=null && tokenFiltersPath.length()>0) {
					LOG.debug("tokenFiltersPath: " + tokenFiltersPath);
					String[] parts = tokenFiltersPath.split(";");
					for (String part : parts) {
						if (part.length()>0) {
							LOG.debug("From: " + part);
							File tokenFilterFile = this.getFile(part);
							if (!tokenFilterFile.exists()) {
								throw new TalismaneException("tokenFilters: File " + part + " does not exist");
							}
							Scanner tokenFilterScanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(tokenFilterFile), this.getInputCharset())));
							scanners.add(tokenFilterScanner);
						}
					}
				}
				if (!tokenFiltersReplace) {
					if (model!=null) {
						LOG.debug("From model");
						List<String> modelDescriptors = model.getDescriptors().get(TokenFilterService.TOKEN_FILTER_DESCRIPTOR_KEY);
						String modelDescriptorString = "";
						if (modelDescriptors!=null) {
							for (String descriptor : modelDescriptors) {
								modelDescriptorString += descriptor + "\n";
							}
						}
						Scanner scanner = new Scanner(modelDescriptorString);
						scanners.add(scanner);
					} else {
						// default token filters
						LOG.debug("From default");
						Scanner tokenFilterScanner = this.implementation.getDefaultTokenFiltersScanner();
						scanners.add(tokenFilterScanner);
					}
				}
				
				for (Scanner scanner : scanners) {
					List<TokenFilter> myFilters = this.getTokenFilterService().readTokenFilters(scanner, tokenFilterDescriptors);
					for (TokenFilter tokenFilter : myFilters) {
						tokenFilters.add(tokenFilter);
					}
				}
				this.getDescriptors().put(TokenFilterService.TOKEN_FILTER_DESCRIPTOR_KEY, tokenFilterDescriptors);
				
				for (TokenFilter tokenFilter : this.additionalTokenFilters)
					this.tokenFilters.add(tokenFilter);
			}
			return tokenFilters;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * The language detector to use for analysis.
	 * @return
	 */
	@Override
	public LanguageDetector getLanguageDetector() {
		try {
			if (languageDetector==null) {
				LOG.debug("Getting language detector model");
				ClassificationModel<LanguageOutcome> languageModel = null;
				if (languageModelFilePath!=null) {
					File languageModelFile = this.getFile(languageModelFilePath);
					if (!languageModelFile.exists())
						throw new TalismaneException("Could not find languageModel at: " + languageModelFilePath);
					languageModel = this.getMachineLearningService().getClassificationModel(new ZipInputStream(new FileInputStream(languageModelFile)));
				} else {
					throw new TalismaneException("Cannot detect languages with languageModel");
				}
				languageDetector = this.getLanguageDetectorService().getLanguageDetector(languageModel);
			}
			return languageDetector;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * The sentence detector to use for analysis.
	 * @return
	 */
	@Override
	public SentenceDetector getSentenceDetector() {
		try {
			if (sentenceDetector==null) {
				LOG.debug("Getting sentence detector model");
				ClassificationModel<SentenceDetectorOutcome> sentenceModel = null;
				if (sentenceModelFilePath!=null) {
					File sentenceModelFile = this.getFile(sentenceModelFilePath);
					if (!sentenceModelFile.exists())
						throw new TalismaneException("Could not find sentenceModel at: " + sentenceModelFilePath);
					
					sentenceModel = this.getMachineLearningService().getClassificationModel(new ZipInputStream(new FileInputStream(sentenceModelFile)));
				} else {
					sentenceModel = this.implementation.getDefaultSentenceModel();
					if (sentenceModel==null)
						throw new TalismaneException("No sentenceModel provided");
				}
				sentenceDetector = this.getSentenceDetectorService().getSentenceDetector(sentenceModel);
			}
			return sentenceDetector;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * The tokeniser to use for analysis.
	 * @return
	 */
	@Override
	public Tokeniser getTokeniser() {
		try {
			if (tokeniser==null) {
				ClassificationModel<TokeniserOutcome> tokeniserModel = null;
				if (tokeniserType==TokeniserType.simple) {
					tokeniser = this.getTokeniserService().getSimpleTokeniser();
				} else if (tokeniserType==TokeniserType.pattern) {
					LOG.debug("Getting tokeniser model");
					tokeniserModel = this.getTokeniserModel();
					if (tokeniserModel==null)
						throw new TalismaneException("No tokeniserModel provided");

					tokeniser = this.getTokeniserPatternService().getPatternTokeniser(tokeniserModel, tokeniserBeamWidth);
		
					if (includeDetails) {
						String detailsFilePath = this.getBaseName() + "_tokeniser_details.txt";
						File detailsFile = new File(this.getOutDir(), detailsFilePath);
						detailsFile.delete();
						ClassificationObserver<TokeniserOutcome> observer = tokeniserModel.getDetailedAnalysisObserver(detailsFile);
						tokeniser.addObserver(observer);
					}
				} else {
					throw new TalismaneException("Unknown tokeniserType: " + tokeniserType);
				}
				
				for (TokenFilter tokenFilter : this.getTokenFilters(tokeniserModel)) {
					tokeniser.addTokenFilter(tokenFilter);
					if (this.needsSentenceDetector()) {
						this.getSentenceDetector().addTokenFilter(tokenFilter);
					}
				}
				
				for (TokenSequenceFilter tokenFilter : this.getTokenSequenceFilters(tokeniserModel)) {
					tokeniser.addTokenSequenceFilter(tokenFilter);
				}
			}
			return tokeniser;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	ClassificationModel<TokeniserOutcome> getTokeniserModel() {
		try {
			if (tokeniserModel==null) {
				if (tokeniserModelFilePath!=null) {
					File tokeniserModelFile = this.getFile(tokeniserModelFilePath);
					if (!tokeniserModelFile.exists())
						throw new TalismaneException("Could not find tokeniserModel at: " + tokeniserModelFilePath);
					
					tokeniserModel = this.getMachineLearningService().getClassificationModel(new ZipInputStream(new FileInputStream(tokeniserModelFile)));
				} else {
					tokeniserModel = this.implementation.getDefaultTokeniserModel();
				}
			}
			return tokeniserModel;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
	
	ClassificationModel<PosTag> getPosTaggerModel() {
		try {
			if (posTaggerModel==null) {
				if (posTaggerModelFilePath!=null) {
					File posTaggerModelFile = this.getFile(posTaggerModelFilePath);
					if (!posTaggerModelFile.exists())
						throw new TalismaneException("Could not find posTaggerModel at: " + posTaggerModelFilePath);
					
					posTaggerModel = this.getMachineLearningService().getClassificationModel(new ZipInputStream(new FileInputStream(posTaggerModelFile)));
				} else {
					posTaggerModel = this.implementation.getDefaultPosTaggerModel();
				}
			}
			return posTaggerModel;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
	
	MachineLearningModel getParserModel() {
		try {
			if (parserModel==null) {
				if (parserModelFilePath!=null) {
					File parserModelFile = this.getFile(parserModelFilePath);
					if (!parserModelFile.exists())
						throw new TalismaneException("Could not find parserModel at: " + parserModelFilePath);
					
					parserModel = this.getMachineLearningService().getClassificationModel(new ZipInputStream(new FileInputStream(parserModelFile)));
				} else {
					parserModel = this.implementation.getDefaultParserModel();
				}
			}
			return parserModel;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}	

	@Override
	public TokeniserPatternManager getTokeniserPatternManager() {
		if (tokeniserPatternManager==null) {
			if (tokeniserPatternFilePath.length()==0)
				throw new RuntimeException("Missing argument: tokeniserPatterns");
			try {
				File tokeniserPatternFile = this.getFile(tokeniserPatternFilePath);
				Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(tokeniserPatternFile), this.getInputCharset())));
				List<String> patternDescriptors = new ArrayList<String>();
				while (scanner.hasNextLine()) {
					String descriptor = scanner.nextLine();
					patternDescriptors.add(descriptor);
					LOG.debug(descriptor);
				}
				scanner.close();
				
				this.getDescriptors().put(TokeniserPatternService.PATTERN_DESCRIPTOR_KEY, patternDescriptors);

				tokeniserPatternManager =
					this.getTokeniserPatternService().getPatternManager(patternDescriptors);
			} catch (Exception e) {
				LogUtils.logError(LOG, e);
				throw new RuntimeException(e);
			}
		}
		return tokeniserPatternManager;
	}
	
	@Override
	public Set<LanguageDetectorFeature<?>> getLanguageDetectorFeatures() {
		if (languageFeatures==null) {
			try {
				if (languageFeaturePath!=null) {
					LOG.debug("Found setting to change language detector features");
					File languageFeatureFile = this.getFile(languageFeaturePath);
					Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(languageFeatureFile), this.getInputCharset())));
					List<String> featureDescriptors = new ArrayList<String>();
					while (scanner.hasNextLine()) {
						String descriptor = scanner.nextLine();
						featureDescriptors.add(descriptor);
						LOG.debug(descriptor);
					}
					languageFeatures = this.getLanguageDetectorService().getFeatureSet(featureDescriptors);
					this.getDescriptors().put(MachineLearningModel.FEATURE_DESCRIPTOR_KEY, featureDescriptors);
				}
			} catch (Exception e) {
				LogUtils.logError(LOG, e);
				throw new RuntimeException(e);
			}
		}
		return languageFeatures;
	}
	
	@Override
	public Set<SentenceDetectorFeature<?>> getSentenceDetectorFeatures() {
		if (sentenceFeatures==null) {
			try {
				if (sentenceFeaturePath!=null) {
					LOG.debug("Found setting to change sentence detector features");
					File sentenceFeatureFile = this.getFile(sentenceFeaturePath);
					Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(sentenceFeatureFile), this.getInputCharset())));
					List<String> featureDescriptors = new ArrayList<String>();
					while (scanner.hasNextLine()) {
						String descriptor = scanner.nextLine();
						featureDescriptors.add(descriptor);
						LOG.debug(descriptor);
					}
					sentenceFeatures = this.getSentenceDetectorFeatureService().getFeatureSet(featureDescriptors);
					this.getDescriptors().put(MachineLearningModel.FEATURE_DESCRIPTOR_KEY, featureDescriptors);
				}
			} catch (Exception e) {
				LogUtils.logError(LOG, e);
				throw new RuntimeException(e);
			}
		}
		return sentenceFeatures;
	}
	
	@Override
	public Set<TokeniserContextFeature<?>> getTokeniserContextFeatures() {
		if (tokeniserContextFeatures==null) {
			try {
				if (tokeniserFeaturePath!=null) {
					TokeniserPatternManager tokeniserPatternManager = this.getTokeniserPatternManager();
					LOG.debug("Found setting to change tokeniser context features");
					File tokeniserFeatureFile = this.getFile(tokeniserFeaturePath);
					Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(tokeniserFeatureFile), this.getInputCharset())));
					List<String> featureDescriptors = new ArrayList<String>();
					while (scanner.hasNextLine()) {
						String descriptor = scanner.nextLine();
						featureDescriptors.add(descriptor);
						LOG.debug(descriptor);
					}
					tokeniserContextFeatures = this.getTokenFeatureService().getTokeniserContextFeatureSet(featureDescriptors, tokeniserPatternManager.getParsedTestPatterns());
					this.getDescriptors().put(MachineLearningModel.FEATURE_DESCRIPTOR_KEY, featureDescriptors);
				}
			} catch (Exception e) {
				LogUtils.logError(LOG, e);
				throw new RuntimeException(e);
			}
		}
		return tokeniserContextFeatures;
	}
	
	@Override
	public Set<TokenPatternMatchFeature<?>> getTokenPatternMatchFeatures() {
		if (tokenPatternMatchFeatures==null) {
			try {
				if (tokeniserFeaturePath!=null) {
					LOG.debug("Found setting to change token pattern match features");
					File tokeniserFeatureFile = this.getFile(tokeniserFeaturePath);
					Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(tokeniserFeatureFile), this.getInputCharset())));
					List<String> featureDescriptors = new ArrayList<String>();
					while (scanner.hasNextLine()) {
						String descriptor = scanner.nextLine();
						featureDescriptors.add(descriptor);
						LOG.debug(descriptor);
					}
					tokenPatternMatchFeatures = this.getTokenFeatureService().getTokenPatternMatchFeatureSet(featureDescriptors);
					this.getDescriptors().put(MachineLearningModel.FEATURE_DESCRIPTOR_KEY, featureDescriptors);
				}
			} catch (Exception e) {
				LogUtils.logError(LOG, e);
				throw new RuntimeException(e);
			}
		}
		return tokenPatternMatchFeatures;
	}
	
	@Override
	public Set<PosTaggerFeature<?>> getPosTaggerFeatures() {
		if (posTaggerFeatures==null) {
			try {
				if (posTaggerFeaturePath!=null) {
					LOG.debug("Found setting to change pos-tagger features");
					File posTaggerFeatureFile = this.getFile(posTaggerFeaturePath);
					Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(posTaggerFeatureFile), this.getInputCharset())));
					List<String> featureDescriptors = new ArrayList<String>();
					while (scanner.hasNextLine()) {
						String descriptor = scanner.nextLine();
						featureDescriptors.add(descriptor);
						LOG.debug(descriptor);
					}
					posTaggerFeatures = this.getPosTaggerFeatureService().getFeatureSet(featureDescriptors);
					this.getDescriptors().put(MachineLearningModel.FEATURE_DESCRIPTOR_KEY, featureDescriptors);
				}
			} catch (Exception e) {
				LogUtils.logError(LOG, e);
				throw new RuntimeException(e);
			}
		}
		return posTaggerFeatures;
	}
	

	@Override
	public ClassificationEventStream getClassificationEventStream() {
		if (this.classificationEventStream==null) {
			switch (this.getModule()) {
			case LanguageDetector:
				classificationEventStream = this.getLanguageDetectorService().getLanguageDetectorEventStream(this.getLanguageCorpusReader(), this.getLanguageDetectorFeatures());
				break;
			case SentenceDetector:
				classificationEventStream = this.getSentenceDetectorService().getSentenceDetectorEventStream(this.getSentenceCorpusReader(), this.getSentenceDetectorFeatures());
				break;
			case Tokeniser:
				if (patternTokeniserType==PatternTokeniserType.Interval) {
					Set<TokeniserContextFeature<?>> features = this.getTokeniserContextFeatures();
					classificationEventStream = this.getTokeniserPatternService().getIntervalPatternEventStream(this.getTokenCorpusReader(), features, this.getTokeniserPatternManager());
				} else {
					Set<TokenPatternMatchFeature<?>> features = this.getTokenPatternMatchFeatures();	
					classificationEventStream = this.getTokeniserPatternService().getCompoundPatternEventStream(this.getTokenCorpusReader(), features, this.getTokeniserPatternManager());
				}
				break;
			case PosTagger:
				classificationEventStream = this.getPosTaggerService().getPosTagEventStream(this.getPosTagCorpusReader(), this.getPosTaggerFeatures());
				break;
			case Parser:
				classificationEventStream = this.getParserService().getParseEventStream(this.getParserCorpusReader(), this.getParserFeatures());
				break;
			default:
				throw new TalismaneException("Unsupported module: " + this.getModule());
			}
		}
		return classificationEventStream;
	}
	
	/**
	 * The pos-tagger to use for analysis.
	 * @return
	 */
	@Override
	public PosTagger getPosTagger() {
		try {
			if (posTagger==null) {
				LOG.debug("Getting pos-tagger model");
				
				ClassificationModel<PosTag> posTaggerModel = this.getPosTaggerModel();
				if (posTaggerModel==null)
					throw new TalismaneException("No posTaggerModel provided");

				posTagger = this.getPosTaggerService().getPosTagger(posTaggerModel, posTaggerBeamWidth);
				
				if (posTaggerFeaturePath!=null) {
					Set<PosTaggerFeature<?>> posTaggerFeatures = this.getPosTaggerFeatures();
					posTagger.setPosTaggerFeatures(posTaggerFeatures);
				}
				
				for (TokenSequenceFilter tokenFilter : this.getTokenSequenceFilters(posTaggerModel)) {
					posTagger.addPreProcessingFilter(tokenFilter);
				}
								
				for (PosTagSequenceFilter posTagFilter : this.getPosTagSequenceFilters(posTaggerModel)) {
					posTagger.addPostProcessingFilter(posTagFilter);
				}
				
				posTagger.setPosTaggerRules(this.getPosTaggerRules());
		
				if (includeDetails) {
					String detailsFilePath = this.getBaseName() + "_posTagger_details.txt";
					File detailsFile = new File(this.getOutDir(), detailsFilePath);
					detailsFile.delete();
					ClassificationObserver<PosTag> observer = posTaggerModel.getDetailedAnalysisObserver(detailsFile);
					posTagger.addObserver(observer);
				}
			}
			return posTagger;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * The parser to use for analysis.
	 * @return
	 */
	@Override
	public Parser getParser() {
		try {
			if (parser==null) {
				LOG.debug("Getting parser model");
				MachineLearningModel parserModel = this.getParserModel();
				if (parserModel==null)
					throw new TalismaneException("No parserModel provided");
				
				parser = this.getParserService().getTransitionBasedParser(parserModel, parserBeamWidth, dynamiseFeatures);
				parser.setMaxAnalysisTimePerSentence(maxParseAnalysisTime);
				parser.setMinFreeMemory(minFreeMemory);
				
				if (this.parserFeaturePath!=null) {
					Set<ParseConfigurationFeature<?>> parserFeatures = this.getParserFeatures();
					parser.setParseFeatures(parserFeatures);
				}
				
				parser.setParserRules(this.getParserRules());
				
				if (parser instanceof TransitionBasedParser) {
					TransitionBasedParser transitionBasedParser = (TransitionBasedParser) parser;
					transitionBasedParser.setEarlyStop(earlyStop);
				}
				
				if (parseComparisonStrategyType!=null) {
					ParseComparisonStrategy parseComparisonStrategy = parserService.getParseComparisonStrategy(parseComparisonStrategyType);
					parser.setParseComparisonStrategy(parseComparisonStrategy);
				}
				
				if (includeDetails && parserModel instanceof ClassificationModel) {
					String detailsFilePath = this.getBaseName() + "_parser_details.txt";
					File detailsFile = new File(this.getOutDir(), detailsFilePath);
					detailsFile.delete();
					@SuppressWarnings("unchecked")
					ClassificationModel<Transition> classificationModel = (ClassificationModel<Transition>) parserModel;
					ClassificationObserver<Transition> observer = classificationModel.getDetailedAnalysisObserver(detailsFile);
					parser.addObserver(observer);
				}
				talismaneSession.setTransitionSystem(parser.getTransitionSystem());

			}
			return parser;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
	

	@Override
	public Set<ParseConfigurationFeature<?>> getParserFeatures() {
		if (parserFeatures==null) {
			try {
				if (parserFeaturePath!=null) {
					LOG.debug("Found setting to change parser features");
					File parserFeatureFile = this.getFile(parserFeaturePath);
					Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(parserFeatureFile), this.getInputCharset())));
					List<String> featureDescriptors = new ArrayList<String>();
					while (scanner.hasNextLine()) {
						String descriptor = scanner.nextLine();
						featureDescriptors.add(descriptor);
						LOG.debug(descriptor);
					}
					parserFeatures = this.getParserFeatureService().getFeatures(featureDescriptors);
					
					this.getDescriptors().put(MachineLearningModel.FEATURE_DESCRIPTOR_KEY, featureDescriptors);
				}
			} catch (Exception e) {
				LogUtils.logError(LOG, e);
				throw new RuntimeException(e);
			}
		}
		return parserFeatures;
	}

	/**
	 * The maximum amount of time the parser will spend analysing any single sentence, in seconds.
	 * If it exceeds this time, the parser will return a partial analysis, or a "dependency forest",
	 * where certain nodes are left unattached (no governor).<br/>
	 * A value of 0 indicates that there is no maximum time -
	 * the parser will always continue until sentence analysis is complete.<br/>
	 * The default value is 60.<br/>
	 * @return
	 */
	@Override
	public int getMaxParseAnalysisTime() {
		return maxParseAnalysisTime;
	}

	@Override
	public void setMaxParseAnalysisTime(int maxParseAnalysisTime) {
		this.maxParseAnalysisTime = maxParseAnalysisTime;
	}
	
	/**
	 * A sentence processor to process sentences that have been read.
	 * @return
	 */
	@Override
	public SentenceProcessor getSentenceProcessor() {
		try {
			if (sentenceProcessor==null && endModule.equals(Module.SentenceDetector)) {
				Reader templateReader = null;
				if (templatePath==null) {
					templateReader = new BufferedReader(new InputStreamReader(getInputStreamFromResource(sentenceTemplateName)));
				} else {
					templateReader = new BufferedReader(new FileReader(this.getFile(templatePath)));
				}
				FreemarkerTemplateWriter templateWriter = new FreemarkerTemplateWriter(templateReader);
				sentenceProcessor=templateWriter;
			}
			return sentenceProcessor;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * A token sequence processor to process token sequences that have been read.
	 * @return
	 */
	@Override
	public TokenSequenceProcessor getTokenSequenceProcessor() {
		try {
			if (tokenSequenceProcessor==null && endModule.equals(Module.Tokeniser)) {
				Reader templateReader = null;
				if (templatePath==null) {
					templateReader = new BufferedReader(new InputStreamReader(getInputStreamFromResource(tokeniserTemplateName)));
				} else {
					templateReader = new BufferedReader(new FileReader(this.getFile(templatePath)));
				}
				FreemarkerTemplateWriter templateWriter = new FreemarkerTemplateWriter(templateReader);
				tokenSequenceProcessor = templateWriter;
			}
			return tokenSequenceProcessor;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * A pos-tag sequence processor to process pos-tag sequences that have been read.
	 * @return
	 */
	@Override
	public PosTagSequenceProcessor getPosTagSequenceProcessor() {
		try {
			if (posTagSequenceProcessor==null && endModule.equals(Module.PosTagger)) {
				if (this.option==Option.posTagFeatureTester) {
					File file = new File(this.getOutDir(), this.getBaseName() + "_featureTest.txt");
					posTagSequenceProcessor = this.getPosTaggerService().getPosTagFeatureTester(this.getPosTaggerFeatures(), this.testWords, file);
				} else {
					Reader templateReader = null;
					if (templatePath==null) {
						templateReader = new BufferedReader(new InputStreamReader(getInputStreamFromResource(posTaggerTemplateName)));
					} else {
						templateReader = new BufferedReader(new FileReader(this.getFile(templatePath)));
					}
					FreemarkerTemplateWriter templateWriter = new FreemarkerTemplateWriter(templateReader);
					posTagSequenceProcessor = templateWriter;
				}
			}
			return posTagSequenceProcessor;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * A parse configuration processor to process parse configurations that have been read.
	 * @return
	 */
	@Override
	public ParseConfigurationProcessor getParseConfigurationProcessor() {
		try {
			if (parseConfigurationProcessor==null && endModule.equals(Module.Parser)) {
				if (option==null) {
					Reader templateReader = null;
					if (templatePath==null) {
						templateReader = new BufferedReader(new InputStreamReader(getInputStreamFromResource(parserTemplateName)));
					} else {
						templateReader = new BufferedReader(new FileReader(this.getFile(templatePath)));
					}
					FreemarkerTemplateWriter templateWriter = new FreemarkerTemplateWriter(templateReader);
					parseConfigurationProcessor = templateWriter;
				} else if (option.equals(Option.loadParsingConstraints)) {
					ParsingConstrainer constrainer = this.getParserService().getParsingConstrainer();
					this.getOutDir();
					File outFile = this.getFile(outFilePath);
					constrainer.setFile(outFile);
					parseConfigurationProcessor = constrainer;
				} else if (option.equals(Option.parseFeatureTester)) {
					File file = new File(this.getOutDir(), this.getBaseName() + "_featureTest.txt");
					parseConfigurationProcessor = this.getParserService().getParseFeatureTester(this.getParserFeatures(), file);
				} else {
					throw new TalismaneException("Unknown option: " + option.toString());
				}
				
				if (includeTransitionLog) {
					ParseConfigurationProcessorChain chain = new ParseConfigurationProcessorChain();
					chain.addProcessor(parseConfigurationProcessor);
					
					File csvFile = new File(this.getOutDir(), this.getBaseName() + "_transitions.csv");
					csvFile.delete();
					csvFile.createNewFile();
					Writer csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false),"UTF8"));
					ParseConfigurationProcessor transitionLogWriter = this.getParserService().getTransitionLogWriter(csvFileWriter);
					
					chain.addProcessor(transitionLogWriter);
					
					parseConfigurationProcessor = chain;
				}
			}
			return parseConfigurationProcessor;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * A token corpus reader to read a corpus pre-annotated in tokens.
	 * Note that in general, any filters up to and including the tokeniser should be applied to the corpus reader.
	 * @return
	 */
	@Override
	public TokeniserAnnotatedCorpusReader getTokenCorpusReader() {
		if (tokenCorpusReader==null) {
			TokenRegexBasedCorpusReader tokenRegexCorpusReader = this.getTokeniserService().getRegexBasedCorpusReader(this.getReader());
			if (this.getInputRegex()!=null)
				tokenRegexCorpusReader.setRegex(this.getInputRegex());
		
			if (this.sentenceReaderPath!=null) {
				
				try {
					File sentenceReaderFile = this.getFile(sentenceReaderPath);
					Reader sentenceFileReader = new BufferedReader(new InputStreamReader(new FileInputStream(sentenceReaderFile), this.getInputCharset()));
					SentenceDetectorAnnotatedCorpusReader sentenceReader = this.getSentenceDetectorService().getDefaultReader(sentenceFileReader);
					tokenRegexCorpusReader.setSentenceReader(sentenceReader);
				} catch (FileNotFoundException fnfe) {
					LogUtils.logError(LOG, fnfe);
					throw new RuntimeException(fnfe);
				}
			}
			this.tokenCorpusReader = tokenRegexCorpusReader;
		}
		this.setCorpusReaderAttributes(tokenCorpusReader);

		this.addTokenCorpusReaderFilters(tokenCorpusReader);
		return tokenCorpusReader;
	}
	

	@Override
	public TokeniserAnnotatedCorpusReader getTokenEvaluationCorpusReader() {
		if (tokenEvaluationCorpusReader==null) {
			TokenRegexBasedCorpusReader tokenRegexCorpusReader = this.getTokeniserService().getRegexBasedCorpusReader(this.getEvaluationReader());
			if (this.getEvaluationRegex()!=null)
				tokenRegexCorpusReader.setRegex(this.getEvaluationRegex());
			
			this.tokenEvaluationCorpusReader = tokenRegexCorpusReader;
		}
		this.setCorpusReaderAttributes(tokenEvaluationCorpusReader);

		this.addTokenCorpusReaderFilters(tokenEvaluationCorpusReader);
		return tokenEvaluationCorpusReader;
	}
	
	void addTokenCorpusReaderFilters(TokeniserAnnotatedCorpusReader corpusReader) {
		if (!tokenCorpusReaderFiltersAdded) {
			MachineLearningModel myTokeniserModel = null;
			
			if (command!=Command.train) {
				myTokeniserModel = this.getTokeniserModel();
			}

			for (TokenFilter tokenFilter : this.getTokenFilters(myTokeniserModel)) {
				corpusReader.addTokenFilter(tokenFilter);
			}

			for (TokenSequenceFilter tokenSequenceFilter : this.getTokenSequenceFilters(myTokeniserModel)) {
				corpusReader.addTokenSequenceFilter(tokenSequenceFilter);
			}
			this.tokenCorpusReaderFiltersAdded = true;		
		}
	}

	@Override
	public void setTokenCorpusReader(
			TokeniserAnnotatedCorpusReader tokenCorpusReader) {
		this.tokenCorpusReader = tokenCorpusReader;
	}

	/**
	 * A pos tag corpus reader to read a corpus pre-annotated in postags.
	 * Note that, in general, any filters up to and including the pos-tagger should be applied to the reader.
	 * @return
	 */
	@Override
	public PosTagAnnotatedCorpusReader getPosTagCorpusReader() {
		if (posTagCorpusReader==null) {
			PosTagRegexBasedCorpusReader posTagRegexBasedCorpusReader = this.getPosTaggerService().getRegexBasedCorpusReader(this.getReader());
			if (this.getInputRegex()!=null)
				posTagRegexBasedCorpusReader.setRegex(this.getInputRegex());
			posTagCorpusReader = posTagRegexBasedCorpusReader;
		}
		this.setCorpusReaderAttributes(posTagCorpusReader);

		this.addPosTagCorpusReaderFilters(posTagCorpusReader);
		return posTagCorpusReader;
	}
	

	@Override
	public PosTagAnnotatedCorpusReader getPosTagEvaluationCorpusReader() {
		if (posTagEvaluationCorpusReader==null) {
			PosTagRegexBasedCorpusReader posTagRegexCorpusReader = this.getPosTaggerService().getRegexBasedCorpusReader(this.getEvaluationReader());
			if (this.getEvaluationRegex()!=null)
				posTagRegexCorpusReader.setRegex(this.getEvaluationRegex());
			this.posTagEvaluationCorpusReader = posTagRegexCorpusReader;
		}
		this.addPosTagCorpusReaderFilters(posTagEvaluationCorpusReader);
		return posTagEvaluationCorpusReader;
	}

	
	void addPosTagCorpusReaderFilters(PosTagAnnotatedCorpusReader corpusReader) {
		if (!posTagCorpusReaderFiltersAdded) {
			MachineLearningModel myPosTaggerModel = null;
			if (this.getCommand()!=Command.train) {				
				if (this.getStartModule().equals(Module.Tokeniser)) {
					myPosTaggerModel = this.getPosTaggerModel();
				} else if (this.getStartModule().equals(Module.PosTagger)) {
					myPosTaggerModel = this.getPosTaggerModel();
				} else {
					myPosTaggerModel = this.getParserModel();
				}
			} // do the models exist already?
			
			List<TokenFilter> tokenFilters = new ArrayList<TokenFilter>();
			for (TokenFilter tokenFilter : this.getTokenFilters(myPosTaggerModel)) {
				tokenFilters.add(tokenFilter);
			}

			TokenSequenceFilter tokenFilterWrapper = this.getTokenFilterService().getTokenSequenceFilter(tokenFilters);
			corpusReader.addTokenSequenceFilter(tokenFilterWrapper);
			
			for (TokenSequenceFilter tokenSequenceFilter : this.getTokenSequenceFilters(myPosTaggerModel)) {
				corpusReader.addTokenSequenceFilter(tokenSequenceFilter);
			}

			for (PosTagSequenceFilter posTagSequenceFilter : this.getPosTagSequenceFilters(myPosTaggerModel)) {
				corpusReader.addPosTagSequenceFilter(posTagSequenceFilter);
			}

			posTagCorpusReaderFiltersAdded = true;
		}
	}

	/**
	 * A parser corpus reader to read a corpus pre-annotated in dependencies.
	 * @return
	 */
	@Override
	public ParserAnnotatedCorpusReader getParserCorpusReader() {
		try {
			if (parserCorpusReader==null) {
				ParserRegexBasedCorpusReader parserRegexCorpusReader = this.getParserService().getRegexBasedCorpusReader(this.getReader());
				if (this.getInputRegex()!=null)
					parserRegexCorpusReader.setRegex(this.getInputRegex());
				parserRegexCorpusReader.setPredictTransitions(predictTransitions);
				if (this.excludeFileName!=null)
					parserRegexCorpusReader.setExcludeFileName(this.excludeFileName);
				
				if (corpusLexicalEntryRegexPath!=null) {
					File corpusLexicalEntryRegexFile = this.getFile(corpusLexicalEntryRegexPath);
					if (!corpusLexicalEntryRegexFile.exists())
						throw new TalismaneException("corpusLexicalEntryRegex file not found: " + corpusLexicalEntryRegexPath);
					
					Scanner corpusLexicalEntryRegexScanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(corpusLexicalEntryRegexFile), this.getInputCharset().name())));
					LexicalEntryReader lexicalEntryReader = new RegexLexicalEntryReader(corpusLexicalEntryRegexScanner);
					corpusLexicalEntryRegexScanner.close();
					parserRegexCorpusReader.setLexicalEntryReader(lexicalEntryReader);
				} else {
					LexicalEntryReader lexicalEntryReader = implementation.getDefaultCorpusLexicalEntryReader();
					if (lexicalEntryReader!=null) {
						parserRegexCorpusReader.setLexicalEntryReader(lexicalEntryReader);
					}
				}
				
				this.parserCorpusReader = parserRegexCorpusReader;
			}
			
			if (!parserCorpusReaderDecorated) {
				this.setCorpusReaderAttributes(parserCorpusReader);
				this.addParserCorpusReaderFilters(parserCorpusReader);
				parserCorpusReaderDecorated = true;
			}
			
			return parserCorpusReader;
		} catch (UnsupportedEncodingException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		} catch (FileNotFoundException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
	
	void setCorpusReaderAttributes(AnnotatedCorpusReader corpusReader) {
		corpusReader.setMaxSentenceCount(maxSentenceCount);
		corpusReader.setStartSentence(startSentence);
		if (crossValidationSize>0)
			corpusReader.setCrossValidationSize(crossValidationSize);
		if (includeIndex>=0)
			corpusReader.setIncludeIndex(includeIndex);
		if (excludeIndex>=0)
			corpusReader.setExcludeIndex(excludeIndex);
	}
	

	@Override
	public ParserAnnotatedCorpusReader getParserEvaluationCorpusReader() {
		if (parserEvaluationCorpusReader==null) {
			ParserRegexBasedCorpusReader parserRegexCorpusReader = this.getParserService().getRegexBasedCorpusReader(this.getEvaluationReader());
			if (this.getEvaluationRegex()!=null)
				parserRegexCorpusReader.setRegex(this.getEvaluationRegex());
			parserRegexCorpusReader.setPredictTransitions(predictTransitions);

			this.parserEvaluationCorpusReader = parserRegexCorpusReader;
		}
		this.addParserCorpusReaderFilters(parserEvaluationCorpusReader);
		return parserEvaluationCorpusReader;
	}
	
	
	
	@Override
	public String getEvaluationFilePath() {
		return evaluationFilePath;
	}

	@Override
	public void setParserEvaluationCorpusReader(
			ParserAnnotatedCorpusReader parserEvaluationCorpusReader) {
		this.parserEvaluationCorpusReader = parserEvaluationCorpusReader;
	}

	@Override
	public void setPosTagEvaluationCorpusReader(
			PosTagAnnotatedCorpusReader posTagEvaluationCorpusReader) {
		this.posTagEvaluationCorpusReader = posTagEvaluationCorpusReader;
	}

	void addParserCorpusReaderFilters(ParserAnnotatedCorpusReader corpusReader) {
		if (!parserCorpusReaderFiltersAdded) {
			MachineLearningModel myPosTaggerModel = null;
			MachineLearningModel myParserModel = null;
			if (this.getCommand()!=Command.train) {
				if (this.getStartModule().equals(Module.Tokeniser)) {
					myPosTaggerModel = this.getPosTaggerModel();
					myParserModel = this.getParserModel();
				} else if (this.getStartModule().equals(Module.PosTagger)) {
					myPosTaggerModel = this.getPosTaggerModel();
					myParserModel = this.getParserModel();
				} else {
					myPosTaggerModel = this.getParserModel();
					myParserModel = this.getParserModel();
				}
			} // models exist already?
			
			List<TokenFilter> tokenFilters = new ArrayList<TokenFilter>();
			for (TokenFilter tokenFilter : this.getTokenFilters(myPosTaggerModel)) {
				tokenFilters.add(tokenFilter);
			}
			
			TokenSequenceFilter tokenFilterWrapper = this.getTokenFilterService().getTokenSequenceFilter(tokenFilters);
			corpusReader.addTokenSequenceFilter(tokenFilterWrapper);

			for (TokenSequenceFilter tokenFilter : this.getTokenSequenceFilters(myPosTaggerModel)) {
				corpusReader.addTokenSequenceFilter(tokenFilter);
			}
			for (PosTagSequenceFilter posTagSequenceFilter : this.getPosTagSequenceFilters(myParserModel)) {
				corpusReader.addPosTagSequenceFilter(posTagSequenceFilter);
			}
			parserCorpusReaderFiltersAdded = true;
		}
	}

	@Override
	public void setPosTagCorpusReader(
			PosTagAnnotatedCorpusReader posTagCorpusReader) {
		this.posTagCorpusReader = posTagCorpusReader;
	}

	@Override
	public void setParserCorpusReader(
			ParserAnnotatedCorpusReader parserCorpusReader) {
		this.parserCorpusReader = parserCorpusReader;
	}

	/**
	 * Get a parser evaluator if command=evaluate and endModule=parser.
	 * @return
	 */
	@Override
	public ParserEvaluator getParserEvaluator() {
		try {
			if (parserEvaluator==null) {
				parserEvaluator = this.getParserService().getParserEvaluator();
				if (startModule.equals(Module.Tokeniser)) {
					parserEvaluator.setTokeniser(this.getTokeniser());
					parserEvaluator.setPosTagger(this.getPosTagger());
				} else if (startModule.equals(Module.PosTagger)) {
					parserEvaluator.setPosTagger(this.getPosTagger());
				}
				parserEvaluator.setParser(this.getParser());
				
				File fscoreFile = new File(this.getOutDir(), this.getBaseName() + ".fscores.csv");

				ParseEvaluationFScoreCalculator parseFScoreCalculator = new ParseEvaluationFScoreCalculator(fscoreFile);
				parseFScoreCalculator.setLabeledEvaluation(this.labeledEvaluation);
				parseFScoreCalculator.setSkipLabel(skipLabel);
				
				if (parserEvaluator.getTokeniser()!=null)
					parseFScoreCalculator.setHasTokeniser(true);
				if (parserEvaluator.getPosTagger()!=null)
					parseFScoreCalculator.setHasPosTagger(true);
				parserEvaluator.addObserver(parseFScoreCalculator);
				
				if (outputGuesses) {
					File csvFile = new File(this.getOutDir(), this.getBaseName() + "_sentences.csv");
					csvFile.delete();
					csvFile.createNewFile();
					Writer csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false),"UTF8"));
					int guessCount = 1;
					if (outputGuessCount>0)
						guessCount = outputGuessCount;
					else
						guessCount = this.getParser().getBeamWidth();

					ParseEvaluationSentenceWriter sentenceWriter = new ParseEvaluationSentenceWriter(csvFileWriter, guessCount);
					if (parserEvaluator.getTokeniser()!=null)
						sentenceWriter.setHasTokeniser(true);
					if (parserEvaluator.getPosTagger()!=null)
						sentenceWriter.setHasPosTagger(true);
					parserEvaluator.addObserver(sentenceWriter);
				}
				
				if (includeDistanceFScores) {
					File csvFile = new File(this.getOutDir(), this.getBaseName() + "_distances.csv");
					csvFile.delete();
					csvFile.createNewFile();
					Writer csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false),"UTF8"));
					ParserFScoreCalculatorByDistance calculator = new ParserFScoreCalculatorByDistance(csvFileWriter);
					calculator.setLabeledEvaluation(this.labeledEvaluation);
					calculator.setSkipLabel(skipLabel);
					parserEvaluator.addObserver(calculator);
				}
				
				if (includeTransitionLog) {
					File csvFile = new File(this.getOutDir(), this.getBaseName() + "_transitions.csv");
					csvFile.delete();
					csvFile.createNewFile();
					Writer csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false),"UTF8"));
					ParseConfigurationProcessor transitionLogWriter = this.getParserService().getTransitionLogWriter(csvFileWriter);
					ParseEvaluationObserverImpl observer = new ParseEvaluationObserverImpl(transitionLogWriter);
					observer.setWriter(csvFileWriter);
					if (this.errorLabels!=null)
						observer.setErrorLabels(errorLabels);
					parserEvaluator.addObserver(observer);
				}
				
				Reader templateReader = null;
				if (templatePath==null) {
					templateReader = new BufferedReader(new InputStreamReader(getInputStreamFromResource(parserTemplateName)));
				} else {
					templateReader = new BufferedReader(new FileReader(this.getFile(templatePath)));
				}
				
				File freemarkerFile = new File(this.getOutDir(), this.getBaseName() + "_output.txt");
				freemarkerFile.delete();
				freemarkerFile.createNewFile();
				Writer freemakerFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(freemarkerFile, false),"UTF8"));
				ParseEvaluationGuessTemplateWriter templateWriter = new ParseEvaluationGuessTemplateWriter(freemakerFileWriter, templateReader);
				parserEvaluator.addObserver(templateWriter);
				parserEvaluator.setSentenceCount(maxSentenceCount);
				parserEvaluator.setPropagateBeam(propagateBeam);
			}
			
			return parserEvaluator;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Get a parser comparator if command=compare and endModule=parser.
	 * @return
	 */
	@Override
	public ParseComparator getParseComparator() {
		try {
			if (parseComparator==null) {
				parseComparator = this.getParserService().getParseComparator();
				File fscoreFile = new File(this.getOutDir(), this.getBaseName() + ".fscores.csv");

				ParseEvaluationFScoreCalculator parseFScoreCalculator = new ParseEvaluationFScoreCalculator(fscoreFile);
				parseFScoreCalculator.setLabeledEvaluation(this.labeledEvaluation);
				parseFScoreCalculator.setSkipLabel(skipLabel);
				
				parseComparator.addObserver(parseFScoreCalculator);
				
				if (includeDistanceFScores) {
					File csvFile = new File(this.getOutDir(), this.getBaseName() + ".distances.csv");
					csvFile.delete();
					csvFile.createNewFile();
					Writer csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false),"UTF8"));
					ParserFScoreCalculatorByDistance calculator = new ParserFScoreCalculatorByDistance(csvFileWriter);
					calculator.setLabeledEvaluation(this.labeledEvaluation);
					calculator.setSkipLabel(skipLabel);
					parseComparator.addObserver(calculator);
				}
				
				parseComparator.setSentenceCount(maxSentenceCount);
			}
			
			return parseComparator;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get a tokeniser evaluator if command=evaluate and endModule=tokeniser.
	 * @return
	 */
	@Override
	public TokeniserEvaluator getTokeniserEvaluator() {
		if (tokeniserEvaluator==null) {				
			tokeniserEvaluator = this.getTokeniserService().getTokeniserEvaluator(this.getTokeniser());
			
			List<TokenEvaluationObserver> observers = this.getTokenEvaluationObservers();
			for (TokenEvaluationObserver observer : observers)
				tokeniserEvaluator.addObserver(observer);
			
			tokeniserEvaluator.setSentenceCount(maxSentenceCount);
		}
		return tokeniserEvaluator;
	}
	
	/**
	 * Get a sentence detector evaluator if command=evaluate and endModule=sentenceDetector.
	 * @return
	 */
	@Override
	public SentenceDetectorEvaluator getSentenceDetectorEvaluator() {
		if (sentenceDetectorEvaluator==null) {				
			sentenceDetectorEvaluator = this.getSentenceDetectorService().getEvaluator(this.getSentenceDetector());
		}
		return sentenceDetectorEvaluator;
	}
	
	private List<TokenEvaluationObserver> getTokenEvaluationObservers() {
		try {
			List<TokenEvaluationObserver> observers = new ArrayList<TokenEvaluationObserver>();
			Writer errorFileWriter = null;
			File errorFile = new File(this.getOutDir(), this.getBaseName() + ".errorList.txt");
			errorFile.delete();
			errorFile.createNewFile();
			errorFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(errorFile, false),"UTF8"));
	
			Writer csvErrorFileWriter = null;
			File csvErrorFile = new File(this.getOutDir(), this.getBaseName() + ".errors.csv");
			csvErrorFile.delete();
			csvErrorFile.createNewFile();
			csvErrorFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvErrorFile, false),"UTF8"));
	
			File fScoreFile = new File(this.getOutDir(), this.getBaseName() + ".fscores.csv");
	
			TokenEvaluationFScoreCalculator tokenFScoreCalculator = new TokenEvaluationFScoreCalculator();
			tokenFScoreCalculator.setErrorWriter(errorFileWriter);
			tokenFScoreCalculator.setCsvErrorWriter(csvErrorFileWriter);
			tokenFScoreCalculator.setFScoreFile(fScoreFile);
			observers.add(tokenFScoreCalculator);
			
			Writer corpusFileWriter = null;
			File corpusFile = new File(this.getOutDir(), this.getBaseName() + ".corpus.txt");
			corpusFile.delete();
			corpusFile.createNewFile();
			corpusFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(corpusFile, false),"UTF8"));
	
			TokenEvaluationCorpusWriter corpusWriter = new TokenEvaluationCorpusWriter(corpusFileWriter);
			observers.add(corpusWriter);
			
			Reader templateReader = null;
			if (templatePath==null) {
				templateReader = new BufferedReader(new InputStreamReader(getInputStreamFromResource(tokeniserTemplateName)));
			} else {
				templateReader = new BufferedReader(new FileReader(this.getFile(templatePath)));
			}
			
			File freemarkerFile = new File(this.getOutDir(), this.getBaseName() + "_output.txt");
			freemarkerFile.delete();
			freemarkerFile.createNewFile();
			Writer freemakerFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(freemarkerFile, false),"UTF8"));
			TokeniserGuessTemplateWriter templateWriter = new TokeniserGuessTemplateWriter(freemakerFileWriter, templateReader);
			observers.add(templateWriter);
			
			return observers;
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Get a token comparator if command=compare and endModule=parser.
	 * @return
	 */
	@Override
	public TokenComparator getTokenComparator() {
		try {
			if (tokenComparator==null) {
				TokeniserPatternManager tokeniserPatternManager = null;
				
				Tokeniser tokeniser = this.getTokeniser();
				if (tokeniser instanceof PatternTokeniser) {
					PatternTokeniser patternTokeniser = (PatternTokeniser) tokeniser;
					tokeniserPatternManager = patternTokeniser.getTokeniserPatternManager();
				}
				tokenComparator = this.getTokeniserService().getTokenComparator(this.getTokenCorpusReader(),
						this.getTokenEvaluationCorpusReader(),
						tokeniserPatternManager);

				List<TokenEvaluationObserver> observers = this.getTokenEvaluationObservers();
				for (TokenEvaluationObserver observer : observers)
					tokenComparator.addObserver(observer);
				
				tokenComparator.setSentenceCount(maxSentenceCount);
			}
			
			return tokenComparator;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Get a pos-tagger evaluator if command=evaluate and endModule=posTagger.
	 * @return
	 */
	@Override
	public PosTaggerEvaluator getPosTaggerEvaluator() {
		try {
			if (posTaggerEvaluator==null) {				
				posTaggerEvaluator = this.getPosTaggerService().getPosTaggerEvaluator(this.getPosTagger());
				
				if (startModule.equals(Module.Tokeniser)) {
					posTaggerEvaluator.setTokeniser(this.getTokeniser());
				}
				
				if (outputGuesses) {
					File csvFile = new File(this.getOutDir(), this.getBaseName() + "_sentences.csv");
					csvFile.delete();
					csvFile.createNewFile();
					Writer csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false),"UTF8"));
					
					int guessCount = 1;
					if (outputGuessCount>0)
						guessCount = outputGuessCount;
					else if (this.getPosTagger() instanceof NonDeterministicPosTagger)
						guessCount = ((NonDeterministicPosTagger) this.getPosTagger()).getBeamWidth();
					
					PosTagEvaluationSentenceWriter sentenceWriter = new PosTagEvaluationSentenceWriter(csvFileWriter, guessCount);
					posTaggerEvaluator.addObserver(sentenceWriter);
				}
				
				File fscoreFile = new File(this.getOutDir(), this.getBaseName() + "_fscores.csv");

				PosTagEvaluationFScoreCalculator posTagFScoreCalculator = new PosTagEvaluationFScoreCalculator(fscoreFile);
				if (includeUnknownWordResults) {
					File fscoreUnknownWordFile = new File(this.getOutDir(), this.getBaseName() + "_unknown.csv");
					posTagFScoreCalculator.setFScoreUnknownInLexiconFile(fscoreUnknownWordFile);
					File fscoreKnownWordFile = new File(this.getOutDir(), this.getBaseName() + "_known.csv");
					posTagFScoreCalculator.setFScoreKnownInLexiconFile(fscoreKnownWordFile);
				}
				
				posTaggerEvaluator.addObserver(posTagFScoreCalculator);
				
				Reader templateReader = null;
				if (templatePath==null) {
					templateReader = new BufferedReader(new InputStreamReader(getInputStreamFromResource(posTaggerTemplateName)));
				} else {
					templateReader = new BufferedReader(new FileReader(this.getFile(templatePath)));
				}
				
				File freemarkerFile = new File(this.getOutDir(), this.getBaseName() + "_output.txt");
				freemarkerFile.delete();
				freemarkerFile.createNewFile();
				Writer freemakerFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(freemarkerFile, false),"UTF8"));
				PosTaggerGuessTemplateWriter templateWriter = new PosTaggerGuessTemplateWriter(freemakerFileWriter, templateReader);
				posTaggerEvaluator.addObserver(templateWriter);
				
				if (includeLexiconCoverage) {
					File lexiconCoverageFile = new File(this.getOutDir(), this.getBaseName() + ".unknown.csv");
					PosTagEvaluationLexicalCoverageTester lexiconCoverageTester = new PosTagEvaluationLexicalCoverageTester(lexiconCoverageFile);
					posTaggerEvaluator.addObserver(lexiconCoverageTester);
				}
				
				posTaggerEvaluator.setPropagateBeam(propagateBeam);
				posTaggerEvaluator.setSentenceCount(maxSentenceCount);
			}
			return posTaggerEvaluator;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Get a pos-tag comparator if command=compare and endModule=parser.
	 * @return
	 */
	@Override
	public PosTagComparator getPosTagComparator() {
		try {
			if (posTagComparator==null) {
				posTagComparator = this.getPosTaggerService().getPosTagComparator();
				File fscoreFile = new File(this.getOutDir(), this.getBaseName() + ".fscores.csv");

				PosTagEvaluationFScoreCalculator fScoreCalculator = new PosTagEvaluationFScoreCalculator(fscoreFile);

				posTagComparator.addObserver(fScoreCalculator);
				
				posTagComparator.setSentenceCount(maxSentenceCount);
			}
			
			return posTagComparator;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * The base name, out of which to construct output file names.
	 * @return
	 */
	@Override
	public String getBaseName() {
		if (baseName==null) {
			baseName = "Talismane";
			if (outFilePath!=null) {
				if (outFilePath.indexOf('.')>0)
					baseName = outFilePath.substring(outFilePath.lastIndexOf('/')+1, outFilePath.lastIndexOf('.'));
				else
					baseName = outFilePath.substring(outFilePath.lastIndexOf('/')+1);
			} else if (inFilePath!=null) {
				if (inFilePath.indexOf('.')>0)
					baseName = inFilePath.substring(inFilePath.lastIndexOf('/')+1, inFilePath.lastIndexOf('.'));
				else
					baseName = inFilePath.substring(inFilePath.lastIndexOf('/')+1);
			} else if (languageModelFilePath!=null && module.equals(Talismane.Module.LanguageDetector)||endModule.equals(Talismane.Module.LanguageDetector)) {
				if (languageModelFilePath.indexOf('.')>0)
					baseName = languageModelFilePath.substring(languageModelFilePath.lastIndexOf('/')+1, languageModelFilePath.lastIndexOf('.'));
				else
					baseName = languageModelFilePath.substring(languageModelFilePath.lastIndexOf('/')+1);
			} else if (sentenceModelFilePath!=null && module.equals(Talismane.Module.SentenceDetector)||endModule.equals(Talismane.Module.SentenceDetector)) {
				if (sentenceModelFilePath.indexOf('.')>0)
					baseName = sentenceModelFilePath.substring(sentenceModelFilePath.lastIndexOf('/')+1, sentenceModelFilePath.lastIndexOf('.'));
				else
					baseName = sentenceModelFilePath.substring(sentenceModelFilePath.lastIndexOf('/')+1);
			} else if (tokeniserModelFilePath!=null && (module.equals(Talismane.Module.Tokeniser)||endModule.equals(Talismane.Module.Tokeniser))) {
				if (tokeniserModelFilePath.indexOf('.')>0)
					baseName = tokeniserModelFilePath.substring(tokeniserModelFilePath.lastIndexOf('/')+1, tokeniserModelFilePath.lastIndexOf('.'));
				else
					baseName = tokeniserModelFilePath.substring(tokeniserModelFilePath.lastIndexOf('/')+1);
			} else if (posTaggerModelFilePath!=null && (module.equals(Talismane.Module.PosTagger)||endModule.equals(Talismane.Module.PosTagger))) {
				if (posTaggerModelFilePath.indexOf('.')>0)
					baseName = posTaggerModelFilePath.substring(posTaggerModelFilePath.lastIndexOf('/')+1, posTaggerModelFilePath.lastIndexOf('.'));
				else
					baseName = posTaggerModelFilePath.substring(posTaggerModelFilePath.lastIndexOf('/')+1);
			} else if (parserModelFilePath!=null && (module.equals(Talismane.Module.Parser)||endModule.equals(Talismane.Module.Parser))) {
				if (parserModelFilePath.indexOf('.')>0)
					baseName = parserModelFilePath.substring(parserModelFilePath.lastIndexOf('/')+1, parserModelFilePath.lastIndexOf('.'));
				else
					baseName = parserModelFilePath.substring(parserModelFilePath.lastIndexOf('/')+1);
			}
			baseName = baseName + suffix;
		}
		return baseName;
	}

	@Override
	public PosTaggerService getPosTaggerService() {
		return posTaggerService;
	}

	public void setPosTaggerService(PosTaggerService posTaggerService) {
		this.posTaggerService = posTaggerService;
	}

	@Override
	public ParserService getParserService() {
		return parserService;
	}

	public void setParserService(ParserService parserService) {
		this.parserService = parserService;
	}

	public PosTaggerFeatureService getPosTaggerFeatureService() {
		return posTaggerFeatureService;
	}

	public void setPosTaggerFeatureService(
			PosTaggerFeatureService posTaggerFeatureService) {
		this.posTaggerFeatureService = posTaggerFeatureService;
	}
	
	public ParserFeatureService getParserFeatureService() {
		return parserFeatureService;
	}

	public void setParserFeatureService(
			ParserFeatureService parserFeatureService) {
		this.parserFeatureService = parserFeatureService;
	}

	@Override
	public FilterService getFilterService() {
		return filterService;
	}

	public void setFilterService(FilterService filterService) {
		this.filterService = filterService;
	}

	@Override
	public TokenFilterService getTokenFilterService() {
		return tokenFilterService;
	}
	

	private PosTagFilterService getPosTagFilterService() {
		return posTagFilterService;
	}

	public void setTokenFilterService(TokenFilterService tokenFilterService) {
		this.tokenFilterService = tokenFilterService;
	}

	public SentenceDetectorService getSentenceDetectorService() {
		return sentenceDetectorService;
	}

	public void setSentenceDetectorService(
			SentenceDetectorService sentenceDetectorService) {
		this.sentenceDetectorService = sentenceDetectorService;
	}

	
	
	public LanguageDetectorService getLanguageDetectorService() {
		return languageDetectorService;
	}

	public void setLanguageDetectorService(
			LanguageDetectorService languageDetectorService) {
		this.languageDetectorService = languageDetectorService;
	}

	public SentenceDetectorFeatureService getSentenceDetectorFeatureService() {
		return sentenceDetectorFeatureService;
	}

	public void setSentenceDetectorFeatureService(
			SentenceDetectorFeatureService sentenceDetectorFeatureService) {
		this.sentenceDetectorFeatureService = sentenceDetectorFeatureService;
	}

	public MachineLearningService getMachineLearningService() {
		return machineLearningService;
	}

	public void setMachineLearningService(
			MachineLearningService machineLearningService) {
		this.machineLearningService = machineLearningService;
	}

	public TokeniserPatternService getTokeniserPatternService() {
		return tokeniserPatternService;
	}

	public void setTokeniserPatternService(
			TokeniserPatternService tokeniserPatternService) {
		this.tokeniserPatternService = tokeniserPatternService;
	}

	public TokenFeatureService getTokenFeatureService() {
		return tokenFeatureService;
	}

	public void setTokenFeatureService(TokenFeatureService tokenFeatureService) {
		this.tokenFeatureService = tokenFeatureService;
	}
	
	
	@Override
	public TokeniserService getTokeniserService() {
		return tokeniserService;
	}

	public void setTokeniserService(TokeniserService tokeniserService) {
		this.tokeniserService = tokeniserService;
	}

	TalismaneServiceInternal getTalismaneServiceInternal() {
		this.getTalismaneService();
		return talismaneService;
	}
	
	@Override
	public TalismaneService getTalismaneService() {
		return talismaneService;
	}

	public void setTalismaneService(TalismaneService talismaneService) {
		this.talismaneService = (TalismaneServiceInternal) talismaneService;
		this.talismaneSession = talismaneService.getTalismaneSession();
		this.talismaneSession.setImplementation(this.implementation);
	}

	/**
	 * Does this instance of Talismane need a sentence detector to perform the requested processing.
	 */
	@Override
	public boolean needsSentenceDetector() {
		return startModule.compareTo(Module.SentenceDetector)<=0 && endModule.compareTo(Module.SentenceDetector)>=0;
	}
	
	/**
	 * Does this instance of Talismane need a tokeniser to perform the requested processing.
	 */
	@Override
	public boolean needsTokeniser() {
		return startModule.compareTo(Module.Tokeniser)<=0 && endModule.compareTo(Module.Tokeniser)>=0;
	}

	/**
	 * Does this instance of Talismane need a pos tagger to perform the requested processing.
	 */
	@Override
	public boolean needsPosTagger() {
		return startModule.compareTo(Module.PosTagger)<=0 && endModule.compareTo(Module.PosTagger)>=0;
	}
	
	/**
	 * Does this instance of Talismane need a parser to perform the requested processing.
	 */
	@Override
	public boolean needsParser() {
		return startModule.compareTo(Module.Parser)<=0 && endModule.compareTo(Module.Parser)>=0;
	}
	
	private static InputStream getInputStreamFromResource(String resource) {
		String path = "/com/joliciel/talismane/output/" + resource;
		InputStream inputStream = Talismane.class.getResourceAsStream(path); 
		
		return inputStream;
	}

	@Override
	public String getInFilePath() {
		return inFilePath;
	}


	@Override
	public boolean isLogStats() {
		return logStats;
	}

	public LanguageDetectorAnnotatedCorpusReader getLanguageCorpusReader() {
		try {
			if (languageCorpusReader==null) {
				File languageCorpusMapFile = this.getFile(languageCorpusMapPath);
				Scanner languageCorpusMapScanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(languageCorpusMapFile), this.getInputCharset().name())));
				
				Map<Locale, Reader> languageMap = new HashMap<Locale, Reader>();
				while (languageCorpusMapScanner.hasNextLine()) {
					String line = languageCorpusMapScanner.nextLine();
					String[] parts = line.split("\t");
					Locale locale = Locale.forLanguageTag(parts[0]);
					String corpusPath = parts[1];
					File corpusFile = this.getFile(corpusPath);
					Reader corpusReader = new BufferedReader(new InputStreamReader(new FileInputStream(corpusFile), this.getInputCharset().name()));
					languageMap.put(locale, corpusReader);
				}
				languageCorpusMapScanner.close();
				languageCorpusReader = this.getLanguageDetectorService().getDefaultReader(languageMap);
			}
			this.setCorpusReaderAttributes(languageCorpusReader);
			return languageCorpusReader;
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public SentenceDetectorAnnotatedCorpusReader getSentenceCorpusReader() {
		if (sentenceCorpusReader==null) {
			sentenceCorpusReader = this.getSentenceDetectorService().getDefaultReader(this.getReader());
		}
		this.setCorpusReaderAttributes(sentenceCorpusReader);
		return sentenceCorpusReader;
	}

	@Override
	public void setSentenceCorpusReader(
			SentenceDetectorAnnotatedCorpusReader sentenceCorpusReader) {
		this.sentenceCorpusReader = sentenceCorpusReader;
	}

	@Override
	public int getTokeniserBeamWidth() {
		return tokeniserBeamWidth;
	}

	@Override
	public int getPosTaggerBeamWidth() {
		return posTaggerBeamWidth;
	}

	@Override
	public int getParserBeamWidth() {
		return parserBeamWidth;
	}

	@Override
	public boolean isPropagateTokeniserBeam() {
		return propagateTokeniserBeam;
	}

	@Override
	public boolean isPropagatePosTaggerBeam() {
		return propagateBeam;
	}

	/**
	 * the minimum block size, in characters, to process by the sentence detector. Filters are applied to a concatenation of the previous block, the current block,
	 * and the next block prior to sentence detection, in order to ensure that a filter which crosses block boundaries is correctly applied.
	 * It is not legal to have a filter which matches text greater than a block size, since this could result in a filter which stops analysis but doesn't start it again correctly,
	 * or vice versa. Block size can be increased if really big filters are really required. Default is 1000.
	 * @return
	 */
	@Override
	public int getBlockSize() {
		return blockSize;
	}

	@Override
	public void setBlockSize(int blockSize) {
		this.blockSize = blockSize;
	}

	@Override
	public File getPerformanceConfigFile() {
		return performanceConfigFile;
	}

	@Override
	public void setPerformanceConfigFile(File performanceConfigFile) {
		this.performanceConfigFile = performanceConfigFile;
	}

	/**
	 * Should the parser corpus reader predict the transitions or not?
	 * @return
	 */
	@Override
	public boolean isPredictTransitions() {
		return predictTransitions;
	}

	@Override
	public void setPredictTransitions(boolean predictTransitions) {
		this.predictTransitions = predictTransitions;
	}

	@Override
	public Mode getMode() {
		return mode;
	}

	@Override
	public void setMode(Mode mode) {
		this.mode = mode;
	}
	
	@Override
	public Talismane getTalismane() {
		Talismane talismane = null;
		if (this.getMode()==Mode.normal) {
			talismane = (this.getTalismaneServiceInternal()).getTalismane(this);
		} else if (this.getMode()==Mode.server) {
			talismane = this.getTalismaneServiceInternal().getTalismaneServer(this);
		} else {
			throw new TalismaneException("Unknown mode: " + this.getMode().name());
		}
		return talismane;
	}
	
	@Override
	public Map<String,Object> getTrainParameters() {
		Map<String,Object> trainParameters = new HashMap<String, Object>();
		if (algorithm==MachineLearningAlgorithm.MaxEnt) {
			trainParameters.put(MaxentModelTrainer.MaxentModelParameter.Iterations.name(), iterations);
			trainParameters.put(MaxentModelTrainer.MaxentModelParameter.Cutoff.name(), cutoff);
		} else if (algorithm==MachineLearningAlgorithm.Perceptron || algorithm==MachineLearningAlgorithm.PerceptronRanking) {
			trainParameters.put(PerceptronClassificationModelTrainer.PerceptronModelParameter.Iterations.name(), iterations);
			trainParameters.put(PerceptronClassificationModelTrainer.PerceptronModelParameter.Cutoff.name(), cutoff);
			trainParameters.put(PerceptronClassificationModelTrainer.PerceptronModelParameter.AverageAtIntervals.name(), averageAtIntervals);
			
			if (perceptronTolerance>=0)
				trainParameters.put(PerceptronClassificationModelTrainer.PerceptronModelParameter.Tolerance.name(), perceptronTolerance);					
		} else if (algorithm==MachineLearningAlgorithm.LinearSVM) {
			trainParameters.put(LinearSVMModelTrainer.LinearSVMModelParameter.Cutoff.name(), cutoff);
			if (solverType!=null)
				trainParameters.put(LinearSVMModelTrainer.LinearSVMModelParameter.SolverType.name(), solverType);
			if (constraintViolationCost>=0)
				trainParameters.put(LinearSVMModelTrainer.LinearSVMModelParameter.ConstraintViolationCost.name(), constraintViolationCost);
			if (epsilon>=0)
				trainParameters.put(LinearSVMModelTrainer.LinearSVMModelParameter.Epsilon.name(), epsilon);
		}
		return trainParameters;
	}
	
	@Override
	public Map<String,List<String>> getDescriptors() {
		if (this.descriptors==null) {
			descriptors = new HashMap<String, List<String>>();
		}
		return descriptors;
	}

	@Override
	public MachineLearningAlgorithm getAlgorithm() {
		return algorithm;
	}

	@Override
	public ExternalResourceFinder getExternalResourceFinder() {
		return externalResourceFinder;
	}

	@Override
	public List<Integer> getPerceptronObservationPoints() {
		return perceptronObservationPoints;
	}

	@Override
	public ParsingConstrainer getParsingConstrainer() {
		if (parsingConstrainer==null) {
			if (parsingConstrainerPath==null) {
				throw new RuntimeException("Missing argument: parsingConstrainer");
			}
			parsingConstrainer = parserService.getParsingConstrainer(this.getFile(parsingConstrainerPath));
		}
		return parsingConstrainer;
	}

	@Override
	public String getPosTaggerModelFilePath() {
		return posTaggerModelFilePath;
	}

	@Override
	public String getTokeniserModelFilePath() {
		return tokeniserModelFilePath;
	}

	@Override
	public String getSentenceModelFilePath() {
		return sentenceModelFilePath;
	}

	public String getLanguageModelFilePath() {
		return languageModelFilePath;
	}

	@Override
	public String getParserModelFilePath() {
		return parserModelFilePath;
	}

	@Override
	public PatternTokeniserType getPatternTokeniserType() {
		return patternTokeniserType;
	}

	/**
	 * The port where the Talismane Server should listen.
	 * @return
	 */
	@Override
	public int getPort() {
		return port;
	}

	/**
	 * The first sentence index to process.
	 * @return
	 */
	@Override
	public int getStartSentence() {
		return startSentence;
	}


	@Override
	public void preloadResources() {
    	LOG.info("Loading shared resources...");
    	
    	if (preloadLexicon) {
	    	LOG.info("Loading lexicon");
	    	// ping the lexicon to load it
	    	talismaneSession.getMergedLexicon();
    	}
    	
    	// ping the models to load them
		if (this.needsSentenceDetector()) {
	    	LOG.info("Loading sentence detector");
			if (this.getSentenceDetector()==null) {
				throw new TalismaneException("Sentence detector not provided.");
			}
		}
		if (this.needsTokeniser()) {
	    	LOG.info("Loading tokeniser");
			if (this.getTokeniser()==null) {
				throw new TalismaneException("Tokeniser not provided.");
			}
		}
		if (this.needsPosTagger()) {
	    	LOG.info("Loading pos tagger");
			if (this.getPosTagger()==null) {
				throw new TalismaneException("Pos-tagger not provided.");
			}
		}
		if (this.needsParser()) {
	    	LOG.info("Loading parser");
			if (this.getParser()==null) {
				throw new TalismaneException("Parser not provided.");
			}
		}
	}

	public File getBaseDir() {
		return baseDir;
	}

	public void setBaseDir(File baseDir) {
		this.baseDir = baseDir;
	}

	private File getFile(String path) {
		File file = new File(path);
		if (!file.isAbsolute() && baseDir!=null) {
			file = new File(baseDir, path);
		}
		return file;
	}

	public Locale getLocale() {
		if (locale==null) {
			return this.implementation.getLocale();
		}
		return locale;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	public String getLanguageCorpusMapPath() {
		return languageCorpusMapPath;
	}

	public void setLanguageCorpusMapPath(String languageCorpusMapPath) {
		this.languageCorpusMapPath = languageCorpusMapPath;
	}

	@Override
	public LanguageDetectorProcessor getLanguageDetectorProcessor() {
		if (this.languageDetectorProcessor == null) {
			this.languageDetectorProcessor = this.getLanguageDetectorService().getDefaultLanguageDetectorProcessor(this.getWriter());
		}
		return this.languageDetectorProcessor;
	}

	public void setLanguageDetectorProcessor(
			LanguageDetectorProcessor languageDetectorProcessor) {
		this.languageDetectorProcessor = languageDetectorProcessor;
	}

	@Override
	public LanguageImplementation getLanguageImplementation() {
		return implementation;
	}

	@Override
	public void addTokenFilter(TokenFilter tokenFilter) {
		this.additionalTokenFilters.add(tokenFilter);
		if (this.tokenFilters!=null)
			this.tokenFilters.add(tokenFilter);
	}
	
	
}
