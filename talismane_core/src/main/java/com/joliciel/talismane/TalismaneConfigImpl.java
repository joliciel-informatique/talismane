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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.joliciel.talismane.lexicon.Diacriticizer;
import com.joliciel.talismane.lexicon.LexicalEntryReader;
import com.joliciel.talismane.lexicon.LexiconDeserializer;
import com.joliciel.talismane.lexicon.PosTaggerLexicon;
import com.joliciel.talismane.lexicon.RegexLexicalEntryReader;
import com.joliciel.talismane.machineLearning.ClassificationEventStream;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.ClassificationObserver;
import com.joliciel.talismane.machineLearning.ExternalResource;
import com.joliciel.talismane.machineLearning.ExternalResourceFinder;
import com.joliciel.talismane.machineLearning.ExternalWordList;
import com.joliciel.talismane.machineLearning.MachineLearningModel;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.output.FreemarkerTemplateWriter;
import com.joliciel.talismane.parser.ParseComparator;
import com.joliciel.talismane.parser.ParseComparisonStrategy;
import com.joliciel.talismane.parser.ParseConfigurationProcessor;
import com.joliciel.talismane.parser.ParseConfigurationProcessorChain;
import com.joliciel.talismane.parser.ParseEvaluationFScoreCalculator;
import com.joliciel.talismane.parser.ParseEvaluationGuessTemplateWriter;
import com.joliciel.talismane.parser.ParseEvaluationObserverImpl;
import com.joliciel.talismane.parser.ParseEvaluationSentenceWriter;
import com.joliciel.talismane.parser.ParseTimeByLengthObserver;
import com.joliciel.talismane.parser.Parser;
import com.joliciel.talismane.parser.Parser.ParseComparisonStrategyType;
import com.joliciel.talismane.parser.ParserAnnotatedCorpusReader;
import com.joliciel.talismane.parser.ParserEvaluator;
import com.joliciel.talismane.parser.ParserFScoreCalculatorByDistance;
import com.joliciel.talismane.parser.ParserRegexBasedCorpusReader;
import com.joliciel.talismane.parser.ParserService;
import com.joliciel.talismane.parser.TransitionBasedParser;
import com.joliciel.talismane.parser.TransitionSystem;
import com.joliciel.talismane.parser.features.ParseConfigurationFeature;
import com.joliciel.talismane.parser.features.ParserFeatureService;
import com.joliciel.talismane.parser.features.ParserRule;
import com.joliciel.talismane.posTagger.NonDeterministicPosTagger;
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
import com.joliciel.talismane.utils.ArrayListNoNulls;
import com.joliciel.talismane.utils.CSVFormatter;
import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.io.CurrentFileProvider;
import com.joliciel.talismane.utils.io.DirectoryReader;
import com.joliciel.talismane.utils.io.DirectoryWriter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import gnu.trove.list.TIntList;

class TalismaneConfigImpl implements TalismaneConfig {
	private static final Logger LOG = LoggerFactory.getLogger(TalismaneConfigImpl.class);

	private Config config;

	private Command command;
	private Option option;
	private Mode mode;

	private Module startModule;
	private Module endModule;
	private Module module;

	private ParserEvaluator parserEvaluator;
	private PosTaggerEvaluator posTaggerEvaluator;
	private TokeniserEvaluator tokeniserEvaluator;
	private SentenceDetectorEvaluator sentenceDetectorEvaluator;
	private ParseComparator parseComparator;
	private PosTagComparator posTagComparator;
	private TokenComparator tokenComparator;

	private TokeniserAnnotatedCorpusReader tokenCorpusReader;
	private PosTagAnnotatedCorpusReader posTagCorpusReader;
	private ParserAnnotatedCorpusReader parserCorpusReader;
	private ParserAnnotatedCorpusReader parserEvaluationCorpusReader;
	private PosTagAnnotatedCorpusReader posTagEvaluationCorpusReader;
	private TokeniserAnnotatedCorpusReader tokenEvaluationCorpusReader;
	private SentenceDetectorAnnotatedCorpusReader sentenceCorpusReader;
	private LanguageDetectorAnnotatedCorpusReader languageCorpusReader;

	private LanguageDetectorProcessor languageDetectorProcessor;
	private SentenceProcessor sentenceProcessor;
	private TokenSequenceProcessor tokenSequenceProcessor;
	private PosTagSequenceProcessor posTagSequenceProcessor;
	private ParseConfigurationProcessor parseConfigurationProcessor;

	private ClassificationModel languageModel;
	private ClassificationModel sentenceModel;
	private ClassificationModel tokeniserModel;
	private ClassificationModel posTaggerModel;
	private MachineLearningModel parserModel;

	private boolean processByDefault;
	private int maxSentenceCount;
	private int startSentence;
	private int beamWidth;
	private boolean propagateBeam;
	private boolean includeDetails;
	private Charset inputCharset;
	private Charset outputCharset;

	private int tokeniserBeamWidth;
	private int posTaggerBeamWidth;
	private int parserBeamWidth;
	private boolean propagateTokeniserBeam;

	private char endBlockCharacter;
	private String inputRegex;
	private String evaluationRegex;
	private int maxParseAnalysisTime;
	private int minFreeMemory;
	private boolean earlyStop;

	private Reader reader;
	private Writer writer;
	private Reader evaluationReader;

	private String sentenceTemplateName = "sentence_template.ftl";
	private String tokeniserTemplateName = "tokeniser_template.ftl";
	private String posTaggerTemplateName = "posTagger_template.ftl";
	private String parserTemplateName = "parser_conll_template.ftl";

	private String fileName;
	private boolean logStats;
	private String baseName;
	private String suffix;
	private boolean outputGuesses;
	private int outputGuessCount;
	private boolean labeledEvaluation;
	private boolean dynamiseFeatures;
	private String skipLabel;
	private Set<String> errorLabels;

	private List<PosTaggerRule> posTaggerRules;
	private List<ParserRule> parserRules;
	private List<TextMarkerFilter> textMarkerFilters;
	private List<TokenFilter> tokenFilters;
	private List<TokenFilter> additionalTokenFilters = new ArrayListNoNulls<TokenFilter>();
	private List<TokenFilter> prependedTokenFilters = new ArrayListNoNulls<TokenFilter>();
	private List<TokenSequenceFilter> tokenSequenceFilters;
	private List<PosTagSequenceFilter> posTaggerPostProcessingFilters;
	private boolean includeDistanceFScores;
	private boolean includeTransitionLog;
	private boolean predictTransitions;

	private MarkerFilterType newlineMarker;
	private int blockSize;

	private int crossValidationSize;
	private int includeIndex;
	private int excludeIndex;

	private Set<String> testWords;
	private Set<LanguageDetectorFeature<?>> languageFeatures;
	private Set<SentenceDetectorFeature<?>> sentenceFeatures;
	private Set<TokeniserContextFeature<?>> tokeniserContextFeatures;
	private Set<TokenPatternMatchFeature<?>> tokenPatternMatchFeatures;
	private Set<PosTaggerFeature<?>> posTaggerFeatures;
	private Set<ParseConfigurationFeature<?>> parserFeatures;
	private TokeniserPatternManager tokeniserPatternManager;
	private ClassificationEventStream classificationEventStream;
	private TokeniserType tokeniserType;
	private PatternTokeniserType patternTokeniserType;

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
	private boolean includeLexiconCoverage;
	private boolean includeUnknownWordResults;
	private boolean includeTimePerToken;

	// server parameters
	private int port;

	// training parameters
	private List<Integer> perceptronObservationPoints;

	private ExternalResourceFinder externalResourceFinder;
	private Map<String, List<String>> descriptors;
	private TalismaneSession talismaneSession;

	private boolean preloadLexicon;
	private boolean preloaded = false;

	private Locale locale;

	private String csvEncoding;
	private String outputDivider;

	private static final Map<String, Diacriticizer> diacriticizerMap = new HashMap<String, Diacriticizer>();

	public TalismaneConfigImpl() {
	}

	public synchronized void loadParameters(Map<String, String> args, Config config) {
		String logConfigPath = args.get("logConfigFile");
		if (logConfigPath != null && logConfigPath.length() > 0) {
			// don't do default configuration - only call this of not null
			args.remove("logConfigFile");
			LogUtils.configureLogging(logConfigPath);
		}

		String performanceConfigPath = args.get("performanceConfigFile");
		if (performanceConfigPath != null && performanceConfigPath.length() > 0) {
			args.remove("performanceConfigFile");
			performanceConfigFile = new File(performanceConfigPath);
		}

		Map<String, Object> values = new HashMap<>();
		Map<String, ImmutablePair<String, Class<?>>> mapping = new HashMap<>();

		String prefix = "talismane.core.";
		mapping.put("command", new ImmutablePair<String, Class<?>>(prefix + "command", String.class));
		mapping.put("mode", new ImmutablePair<String, Class<?>>(prefix + "mode", String.class));
		mapping.put("port", new ImmutablePair<String, Class<?>>(prefix + "port", Integer.class));
		mapping.put("module", new ImmutablePair<String, Class<?>>(prefix + "module", String.class));
		mapping.put("inFile", new ImmutablePair<String, Class<?>>(prefix + "inFile", String.class));
		mapping.put("inDir", new ImmutablePair<String, Class<?>>(prefix + "inDir", String.class));
		mapping.put("outFile", new ImmutablePair<String, Class<?>>(prefix + "outFile", String.class));
		mapping.put("outDir", new ImmutablePair<String, Class<?>>(prefix + "outDir", String.class));
		mapping.put("encoding", new ImmutablePair<String, Class<?>>(prefix + "encoding", String.class));
		mapping.put("inputEncoding", new ImmutablePair<String, Class<?>>(prefix + "inputEncoding", String.class));
		mapping.put("outputEncoding", new ImmutablePair<String, Class<?>>(prefix + "outputEncoding", String.class));
		mapping.put("locale", new ImmutablePair<String, Class<?>>(prefix + "locale", String.class));

		prefix = "talismane.core.analyse.";
		mapping.put("startModule", new ImmutablePair<String, Class<?>>(prefix + "startModule", String.class));
		mapping.put("endModule", new ImmutablePair<String, Class<?>>(prefix + "endModule", String.class));
		mapping.put("posTagSet", new ImmutablePair<String, Class<?>>(prefix + "posTagSet", String.class));
		mapping.put("transitionSystem", new ImmutablePair<String, Class<?>>(prefix + "transitionSystem", String.class));
		mapping.put("dependencyLabels", new ImmutablePair<String, Class<?>>(prefix + "dependencyLabels", String.class));
		mapping.put("languageModel", new ImmutablePair<String, Class<?>>(prefix + "languageModel", String.class));
		mapping.put("sentenceModel", new ImmutablePair<String, Class<?>>(prefix + "sentenceModel", String.class));
		mapping.put("tokeniserModel", new ImmutablePair<String, Class<?>>(prefix + "tokeniserModel", String.class));
		mapping.put("posTaggerModel", new ImmutablePair<String, Class<?>>(prefix + "posTaggerModel", String.class));
		mapping.put("parserModel", new ImmutablePair<String, Class<?>>(prefix + "parserModel", String.class));
		mapping.put("lexicon", new ImmutablePair<String, Class<?>>(prefix + "lexicons", List.class));
		mapping.put("preloadLexicons", new ImmutablePair<String, Class<?>>(prefix + "preloadLexicons", Boolean.class));
		mapping.put("diacriticizer", new ImmutablePair<String, Class<?>>(prefix + "diacriticizer", String.class));
		mapping.put("inputPattern", new ImmutablePair<String, Class<?>>(prefix + "inputPattern", String.class));
		mapping.put("inputPatternFile", new ImmutablePair<String, Class<?>>(prefix + "inputPatternFile", String.class));
		mapping.put("textFilters", new ImmutablePair<String, Class<?>>(prefix + "textFilters", List.class));
		mapping.put("tokenFilters", new ImmutablePair<String, Class<?>>(prefix + "tokenFilters", List.class));
		mapping.put("tokenSequenceFilters", new ImmutablePair<String, Class<?>>(prefix + "tokenSequenceFilters", List.class));
		mapping.put("posTagSequenceFilters", new ImmutablePair<String, Class<?>>(prefix + "posTagSequenceFilters", List.class));
		mapping.put("externalResources", new ImmutablePair<String, Class<?>>(prefix + "externalResources", List.class));
		mapping.put("newline", new ImmutablePair<String, Class<?>>(prefix + "newline", String.class));
		mapping.put("processByDefault", new ImmutablePair<String, Class<?>>(prefix + "processByDefault", Boolean.class));
		mapping.put("endBlockCharCode", new ImmutablePair<String, Class<?>>(prefix + "endBlockCharCode", String.class));
		mapping.put("blockSize", new ImmutablePair<String, Class<?>>(prefix + "blockSize", Integer.class));
		mapping.put("sentenceCount", new ImmutablePair<String, Class<?>>(prefix + "sentenceCount", Integer.class));
		mapping.put("startSentence", new ImmutablePair<String, Class<?>>(prefix + "startSentence", Integer.class));
		mapping.put("builtInTemplate", new ImmutablePair<String, Class<?>>(prefix + "builtInTemplate", String.class));
		mapping.put("template", new ImmutablePair<String, Class<?>>(prefix + "template", String.class));
		mapping.put("includeDetails", new ImmutablePair<String, Class<?>>(prefix + "includeDetails", Boolean.class));
		mapping.put("posTaggerRules", new ImmutablePair<String, Class<?>>(prefix + "posTaggerRules", List.class));
		mapping.put("parserRules", new ImmutablePair<String, Class<?>>(prefix + "parserRules", List.class));
		mapping.put("fileName", new ImmutablePair<String, Class<?>>(prefix + "fileName", String.class));
		mapping.put("suffix", new ImmutablePair<String, Class<?>>(prefix + "suffix", String.class));
		mapping.put("outputDivider", new ImmutablePair<String, Class<?>>(prefix + "outputDivider", String.class));
		mapping.put("beamWidth", new ImmutablePair<String, Class<?>>(prefix + "beamWidth", Integer.class));
		mapping.put("tokeniserBeamWidth", new ImmutablePair<String, Class<?>>(prefix + "tokeniserBeamWidth", Integer.class));
		mapping.put("posTaggerBeamWidth", new ImmutablePair<String, Class<?>>(prefix + "posTaggerBeamWidth", Integer.class));
		mapping.put("parserBeamWidth", new ImmutablePair<String, Class<?>>(prefix + "parserBeamWidth", Integer.class));
		mapping.put("propagateBeam", new ImmutablePair<String, Class<?>>(prefix + "propagateBeam", Boolean.class));
		mapping.put("propagateTokeniserBeam", new ImmutablePair<String, Class<?>>(prefix + "propagateTokeniserBeam", Boolean.class));
		mapping.put("dynamiseFeatures", new ImmutablePair<String, Class<?>>(prefix + "dynamiseFeatures", Boolean.class));

		prefix = "talismane.core.analyse.tokeniser";
		mapping.put("tokeniserType", new ImmutablePair<String, Class<?>>(prefix + "type", String.class));
		mapping.put("patternTokeniserType", new ImmutablePair<String, Class<?>>(prefix + "patternTokeniserType", String.class));

		prefix = "talismane.core.analyse.parser";
		mapping.put("maxParseAnalysisTime", new ImmutablePair<String, Class<?>>(prefix + "maxAnalysisTime", Integer.class));
		mapping.put("minFreeMemory", new ImmutablePair<String, Class<?>>(prefix + "minFreeMemory", Integer.class));
		mapping.put("parseComparisonStrategy", new ImmutablePair<String, Class<?>>(prefix + "comparisonStrategy", String.class));
		mapping.put("earlyStop", new ImmutablePair<String, Class<?>>(prefix + "earlyStop", Boolean.class));

		prefix = "talismane.core.evaluate.";
		mapping.put("evaluationFile", new ImmutablePair<String, Class<?>>(prefix + "evaluationFile", String.class));
		mapping.put("evaluationPattern", new ImmutablePair<String, Class<?>>(prefix + "evaluationPattern", String.class));
		mapping.put("evaluationPatternFile", new ImmutablePair<String, Class<?>>(prefix + "evaluationPatternFile", String.class));

		prefix = "talismane.core.evaluate.csv.";
		mapping.put("csvSeparator", new ImmutablePair<String, Class<?>>(prefix + "separator", String.class));
		mapping.put("csvEncoding", new ImmutablePair<String, Class<?>>(prefix + "encoding", String.class));
		mapping.put("outputLocale", new ImmutablePair<String, Class<?>>(prefix + "locale", String.class));

		prefix = "talismane.core.evaluate.posTagger.";
		mapping.put("includeUnknownWordResults", new ImmutablePair<String, Class<?>>(prefix + "includeUnknownWordResults", Boolean.class));
		mapping.put("includeLexiconCoverage", new ImmutablePair<String, Class<?>>(prefix + "includeLexiconCoverage", Boolean.class));

		prefix = "talismane.core.evaluate.parser.";
		mapping.put("labeledEvaluation", new ImmutablePair<String, Class<?>>(prefix + "labeledEvaluation", Boolean.class));
		mapping.put("includeTimePerToken", new ImmutablePair<String, Class<?>>(prefix + "includeTimePerToken", Boolean.class));
		mapping.put("includeDistanceFScores", new ImmutablePair<String, Class<?>>(prefix + "includeDistanceFScores", Boolean.class));
		mapping.put("skipLabel", new ImmutablePair<String, Class<?>>(prefix + "skipLabel", String.class));
		mapping.put("includeTransitionLog", new ImmutablePair<String, Class<?>>(prefix + "includeTransitionLog", Boolean.class));
		mapping.put("errorLabels", new ImmutablePair<String, Class<?>>(prefix + "errorLabels", List.class));

		prefix = "talismane.core.evaluate.crossValidation.";
		mapping.put("crossValidationSize", new ImmutablePair<String, Class<?>>(prefix + "foldCount", Integer.class));
		mapping.put("includeIndex", new ImmutablePair<String, Class<?>>(prefix + "includeIndex", Integer.class));
		mapping.put("excludeIndex", new ImmutablePair<String, Class<?>>(prefix + "excludeIndex", Integer.class));

		prefix = "talismane.core.evaluate.";
		mapping.put("outputGuesses", new ImmutablePair<String, Class<?>>(prefix + "outputGuesses", Boolean.class));
		mapping.put("outputGuessCount", new ImmutablePair<String, Class<?>>(prefix + "outputGuessCount", Integer.class));

		prefix = "talismane.core.process.";
		mapping.put("option", new ImmutablePair<String, Class<?>>(prefix + "option", String.class));
		mapping.put("predictTransitions", new ImmutablePair<String, Class<?>>(prefix + "predictTransitions", Boolean.class));
		mapping.put("corpusLexicalEntryRegex", new ImmutablePair<String, Class<?>>(prefix + "corpusLexicalEntryRegex", String.class));
		mapping.put("testWords", new ImmutablePair<String, Class<?>>(prefix + "posTagFeatureTester.testWords", List.class));

		prefix = "talismane.core.train.";
		mapping.put("languageFeatures", new ImmutablePair<String, Class<?>>(prefix + "languageDetector.features", String.class));
		mapping.put("languageCorpusMap", new ImmutablePair<String, Class<?>>(prefix + "languageDetector.languageCorpusMap", String.class));
		mapping.put("sentenceFeatures", new ImmutablePair<String, Class<?>>(prefix + "sentenceDetector.features", String.class));
		mapping.put("sentenceReader", new ImmutablePair<String, Class<?>>(prefix + "tokeniser.sentenceReader", String.class));
		mapping.put("tokeniserFeatures", new ImmutablePair<String, Class<?>>(prefix + "tokeniser.features", String.class));
		mapping.put("tokeniserPatterns", new ImmutablePair<String, Class<?>>(prefix + "tokeniser.patterns", String.class));
		mapping.put("posTaggerFeatures", new ImmutablePair<String, Class<?>>(prefix + "posTagger.features", String.class));
		mapping.put("parserFeatures", new ImmutablePair<String, Class<?>>(prefix + "parser.features", String.class));
		mapping.put("perceptronObservationPoints", new ImmutablePair<String, Class<?>>(prefix + "perceptronObservationPoints", TIntList.class));

		prefix = "talismane.core.";
		mapping.put("logStats", new ImmutablePair<String, Class<?>>(prefix + "logStats", Boolean.class));

		prefix = "talismane.machineLearning.";
		mapping.put("algorithm", new ImmutablePair<String, Class<?>>(prefix + "algorithm", String.class));
		mapping.put("cutoff", new ImmutablePair<String, Class<?>>(prefix + "cutoff", Integer.class));
		mapping.put("linearSVMEpsilon", new ImmutablePair<String, Class<?>>(prefix + "linearSVM.epsilon", Double.class));
		mapping.put("linearSVMCost", new ImmutablePair<String, Class<?>>(prefix + "linearSVM.cost", Double.class));
		mapping.put("oneVsRest", new ImmutablePair<String, Class<?>>(prefix + "linearSVM.oneVsRest", Boolean.class));
		mapping.put("iterations", new ImmutablePair<String, Class<?>>(prefix + "maxent.iterations", Integer.class));

		for (Entry<String, String> arg : args.entrySet()) {
			String key = arg.getKey();
			String argValue = arg.getValue();
			ImmutablePair<String, Class<?>> mappingType = mapping.get(key);
			if (mappingType == null) {
				System.err.println("Unknown argument: " + key);
				throw new RuntimeException("Unknown argument: " + key);
			} else if (String.class.equals(mappingType.right)) {
				values.put(mappingType.left, argValue);
			} else if (Integer.class.equals(mappingType.right)) {
				values.put(mappingType.left, Integer.parseInt(argValue));
			} else if (Double.class.equals(mappingType.right)) {
				values.put(mappingType.left, Double.parseDouble(argValue));
			} else if (Boolean.class.equals(mappingType.right)) {
				values.put(mappingType.left, argValue.equalsIgnoreCase("true"));
			} else if (List.class.equals(mappingType.right)) {
				if (argValue.startsWith("replace:")) {
					String paramName = mappingType.left;
					String paramNameReplace = paramName + "Replace";
					values.put(paramNameReplace, true);
					argValue = argValue.substring("replace:".length());
				}
				String[] parts = argValue.split(";");
				List<String> stringValues = new ArrayListNoNulls<>();
				for (String part : parts)
					stringValues.add(part);

				values.put(mappingType.left, stringValues);
			} else if (TIntList.class.equals(mappingType.right)) {
				String[] parts = argValue.split(",");
				List<Integer> intValues = new ArrayListNoNulls<Integer>();
				for (String part : parts)
					intValues.add(Integer.parseInt(part));

				values.put(mappingType.left, intValues);
			} else {
				throw new RuntimeException("Unable to parse class " + mappingType.right.getSimpleName() + " for " + key);
			}
		}

		Config commandLineConfig = ConfigFactory.parseMap(values, "command-line parameters").withFallback(config);
		this.loadParameters(commandLineConfig);
	}

	public synchronized void loadParameters(Config config) {
		try {
			config.checkValid(ConfigFactory.defaultReference(), "talismane.core");

			this.config = config;

			Config talismaneConfig = config.getConfig("talismane.core");
			Config analyseConfig = talismaneConfig.getConfig("analyse");
			Config evaluateConfig = talismaneConfig.getConfig("evaluate");
			Config trainConfig = talismaneConfig.getConfig("train");
			Config processConfig = talismaneConfig.getConfig("process");

			String commandString = talismaneConfig.getString("command");
			if (commandString.equals("analyze"))
				commandString = "analyse";

			command = Command.valueOf(commandString);

			mode = Mode.valueOf(talismaneConfig.getString("mode"));
			port = talismaneConfig.getInt("port");

			if (talismaneConfig.hasPath("module")) {
				String moduleString = talismaneConfig.getString("module");

				if (moduleString.length() == 0) {
					// do nothing
				} else if (moduleString.equalsIgnoreCase("sentence") || moduleString.equalsIgnoreCase("sentenceDetector"))
					module = Talismane.Module.SentenceDetector;
				else if (moduleString.equalsIgnoreCase("tokenise") || moduleString.equalsIgnoreCase("tokeniser"))
					module = Talismane.Module.Tokeniser;
				else if (moduleString.equalsIgnoreCase("postag") || moduleString.equalsIgnoreCase("posTagger"))
					module = Talismane.Module.PosTagger;
				else if (moduleString.equalsIgnoreCase("parse") || moduleString.equalsIgnoreCase("parser"))
					module = Talismane.Module.Parser;
				else if (moduleString.equalsIgnoreCase("language") || moduleString.equalsIgnoreCase("languageDetector"))
					module = Talismane.Module.LanguageDetector;
				else
					throw new TalismaneException("Unknown module: " + moduleString);
			}

			String encoding = null;
			if (talismaneConfig.hasPath("encoding"))
				encoding = talismaneConfig.getString("encoding");

			String inputEncoding = encoding;
			String outputEncoding = encoding;
			if (talismaneConfig.hasPath("inputEncoding"))
				inputEncoding = talismaneConfig.getString("inputEncoding");
			if (talismaneConfig.hasPath("outputEncoding"))
				outputEncoding = talismaneConfig.getString("outputEncoding");

			inputCharset = Charset.defaultCharset();
			outputCharset = Charset.defaultCharset();
			if (inputEncoding != null)
				inputCharset = Charset.forName(inputEncoding);
			if (outputEncoding != null)
				outputCharset = Charset.forName(outputEncoding);

			if (talismaneConfig.hasPath("locale"))
				locale = Locale.forLanguageTag(talismaneConfig.getString("locale"));

			String startModuleString = analyseConfig.getString("startModule");

			if (startModuleString.equalsIgnoreCase("sentence") || startModuleString.equalsIgnoreCase("sentenceDetector"))
				startModule = Talismane.Module.SentenceDetector;
			else if (startModuleString.equalsIgnoreCase("tokenise") || startModuleString.equalsIgnoreCase("tokeniser"))
				startModule = Talismane.Module.Tokeniser;
			else if (startModuleString.equalsIgnoreCase("postag") || startModuleString.equalsIgnoreCase("posTagger"))
				startModule = Talismane.Module.PosTagger;
			else if (startModuleString.equalsIgnoreCase("parse") || startModuleString.equalsIgnoreCase("parser"))
				startModule = Talismane.Module.Parser;
			else
				throw new TalismaneException("Unknown startModule: " + startModuleString);

			if (module != null)
				startModule = module;

			String endModuleString = analyseConfig.getString("endModule");

			if (endModuleString.equalsIgnoreCase("sentence") || endModuleString.equalsIgnoreCase("sentenceDetector"))
				endModule = Talismane.Module.SentenceDetector;
			else if (endModuleString.equalsIgnoreCase("tokenise") || endModuleString.equalsIgnoreCase("tokeniser"))
				endModule = Talismane.Module.Tokeniser;
			else if (endModuleString.equalsIgnoreCase("postag") || endModuleString.equalsIgnoreCase("posTagger"))
				endModule = Talismane.Module.PosTagger;
			else if (endModuleString.equalsIgnoreCase("parse") || endModuleString.equalsIgnoreCase("parser"))
				endModule = Talismane.Module.Parser;
			else
				throw new TalismaneException("Unknown endModule: " + endModuleString);

			if (module != null)
				endModule = module;

			preloadLexicon = analyseConfig.getBoolean("preloadLexicons");

			newlineMarker = MarkerFilterType.valueOf(analyseConfig.getString("newline"));

			processByDefault = analyseConfig.getBoolean("processByDefault");

			endBlockCharacter = analyseConfig.getString("endBlockCharCode").charAt(0);

			blockSize = analyseConfig.getInt("blockSize");

			maxSentenceCount = analyseConfig.getInt("sentenceCount");
			startSentence = analyseConfig.getInt("startSentence");

			String builtInTemplate = analyseConfig.getString("builtInTemplate");

			includeDetails = analyseConfig.getBoolean("includeDetails");

			if (analyseConfig.hasPath("fileName"))
				fileName = analyseConfig.getString("fileName");
			suffix = analyseConfig.getString("suffix");

			outputDivider = analyseConfig.getString("outputDivider");
			if (outputDivider.equals("NEWLINE"))
				outputDivider = "\n";

			beamWidth = analyseConfig.getInt("beamWidth");
			tokeniserBeamWidth = analyseConfig.getInt("tokeniserBeamWidth");
			posTaggerBeamWidth = analyseConfig.getInt("posTaggerBeamWidth");
			parserBeamWidth = analyseConfig.getInt("parserBeamWidth");
			propagateBeam = analyseConfig.getBoolean("propagateBeam");
			propagateTokeniserBeam = analyseConfig.getBoolean("propagateTokeniserBeam");

			dynamiseFeatures = analyseConfig.getBoolean("dynamiseFeatures");

			tokeniserType = TokeniserType.valueOf(analyseConfig.getString("tokeniser.type"));
			patternTokeniserType = PatternTokeniserType.valueOf(analyseConfig.getString("tokeniser.patternTokeniserType"));

			maxParseAnalysisTime = analyseConfig.getInt("parser.maxAnalysisTime");
			minFreeMemory = analyseConfig.getInt("parser.minFreeMemory");
			parseComparisonStrategyType = ParseComparisonStrategyType.valueOf(analyseConfig.getString("parser.comparisonStrategy"));
			earlyStop = analyseConfig.getBoolean("parser.earlyStop");

			String csvSeparator = evaluateConfig.getString("csv.separator");
			if (evaluateConfig.hasPath("csv.encoding"))
				csvEncoding = evaluateConfig.getString("csv.encoding");
			else
				csvEncoding = Charset.defaultCharset().name();

			Locale outputLocale = null;
			if (evaluateConfig.hasPath("csv.locale")) {
				String csvLocaleString = evaluateConfig.getString("csv.locale");
				outputLocale = Locale.forLanguageTag(csvLocaleString);
			}

			includeLexiconCoverage = evaluateConfig.getBoolean("posTagger.includeLexiconCoverage");
			includeUnknownWordResults = evaluateConfig.getBoolean("posTagger.includeUnknownWordResults");

			labeledEvaluation = evaluateConfig.getBoolean("parser.labeledEvaluation");
			includeTimePerToken = evaluateConfig.getBoolean("parser.includeTimePerToken");
			includeDistanceFScores = evaluateConfig.getBoolean("parser.includeDistanceFScores");

			if (evaluateConfig.hasPath("parser.skipLabel"))
				skipLabel = evaluateConfig.getString("parser.skipLabel");

			includeTransitionLog = evaluateConfig.getBoolean("parser.includeTransitionLog");
			List<String> errorLabelList = evaluateConfig.getStringList("parser.errorLabels");
			if (errorLabelList.size() > 0)
				errorLabels = new HashSet<String>(errorLabelList);

			crossValidationSize = evaluateConfig.getInt("crossValidation.foldCount");
			includeIndex = evaluateConfig.getInt("crossValidation.includeIndex");
			excludeIndex = evaluateConfig.getInt("crossValidation.excludeIndex");

			outputGuesses = evaluateConfig.getBoolean("outputGuesses");
			outputGuessCount = evaluateConfig.getInt("outputGuessCount");

			option = Option.valueOf(processConfig.getString("option"));
			predictTransitions = processConfig.getBoolean("predictTransitions");

			List<String> testWordList = processConfig.getStringList("posTagFeatureTester.testWords");
			if (testWordList.size() > 0) {
				testWords = new HashSet<String>(testWordList);
			}

			perceptronObservationPoints = trainConfig.getIntList("perceptronObservationPoints");

			logStats = talismaneConfig.getBoolean("logStats");

			if (command.equals(Command.evaluate)) {
				if (!config.hasPath("talismane.core.outDir"))
					throw new TalismaneException("talismane.core.outDir is required for command evaluate");
			}

			if (startModule == null)
				startModule = module;
			if (endModule == null)
				endModule = module;
			if (module == null)
				module = endModule;

			if (command == Command.train) {
				if (module == Module.LanguageDetector) {
					if (!config.hasPath("talismane.core.analyse.languageModel"))
						throw new TalismaneException("talismane.core.analyse.languageModel is required when training a language model");
					if (!config.hasPath("talismane.core.train.languageDetector.features"))
						throw new TalismaneException("talismane.core.train.languageDetector.features is required when training a language model");
					if (!config.hasPath("talismane.core.train.languageDetector.languageCorpusMap"))
						throw new TalismaneException("talismane.core.train.languageDetector.languageCorpusMap is required when training a language model");
				} else if (module == Module.SentenceDetector) {
					if (!config.hasPath("talismane.core.analyse.sentenceModel"))
						throw new TalismaneException("talismane.core.analyse.sentenceModel is required when training a sentence model");
					if (!config.hasPath("talismane.core.train.sentenceDetector.features"))
						throw new TalismaneException("talismane.core.train.sentenceDetector.features is required when training a sentence model");
				} else if (module == Module.Tokeniser) {
					if (!config.hasPath("talismane.core.analyse.tokeniserModel"))
						throw new TalismaneException("talismane.core.analyse.tokeniserModel is required when training a tokeniser model");
					if (!config.hasPath("talismane.core.train.tokeniser.features"))
						throw new TalismaneException("talismane.core.train.tokeniser.features is required when training a tokeniser model");
				} else if (module == Module.PosTagger) {
					if (!config.hasPath("talismane.core.analyse.posTaggerModel"))
						throw new TalismaneException("talismane.core.analyse.posTaggerModel is required when training a posTagger model");
					if (!config.hasPath("talismane.core.train.posTagger.features"))
						throw new TalismaneException("talismane.core.train.posTagger.features is required when training a posTagger model");
				} else if (module == Module.Parser) {
					this.predictTransitions = true;

					if (!config.hasPath("talismane.core.analyse.parserModel"))
						throw new TalismaneException("talismane.core.analyse.parserModel is required when training a parser model");
					if (!config.hasPath("talismane.core.train.parser.features"))
						throw new TalismaneException("talismane.core.train.parser.features is required when training a parser model");
				}
			}

			if (builtInTemplate.length() > 0) {
				if (builtInTemplate.equalsIgnoreCase("default")) {
					// don't change defaults
				} else if (builtInTemplate.equalsIgnoreCase("with_location")) {
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

			if (posTaggerBeamWidth < 0)
				posTaggerBeamWidth = beamWidth;
			if (parserBeamWidth < 0)
				parserBeamWidth = beamWidth;

			if (csvSeparator.length() > 0)
				CSVFormatter.setGlobalCsvSeparator(csvSeparator);

			if (outputLocale != null)
				CSVFormatter.setGlobalLocale(outputLocale);

			if (fileName == null && talismaneConfig.hasPath("inFile")) {
				fileName = talismaneConfig.getString("inFile");
			}

			String configPath = "talismane.core.analyse.posTagSet";
			if (config.hasPath(configPath)) {
				InputStream posTagSetFile = this.getFileFromConfig(configPath);
				try (Scanner posTagSetScanner = new Scanner(new BufferedReader(new InputStreamReader(posTagSetFile, this.getInputCharset().name())))) {

					PosTagSet posTagSet = this.getPosTaggerService().getPosTagSet(posTagSetScanner);
					talismaneSession.setPosTagSet(posTagSet);
				}
			}

			String transitionSystemStr = config.getString("talismane.core.analyse.transitionSystem");
			TransitionSystem transitionSystem = null;
			if (transitionSystemStr.equalsIgnoreCase("ShiftReduce")) {
				transitionSystem = this.getParserService().getShiftReduceTransitionSystem();
			} else if (transitionSystemStr.equalsIgnoreCase("ArcEager")) {
				transitionSystem = this.getParserService().getArcEagerTransitionSystem();
			} else {
				throw new TalismaneException("Unknown transition system: " + transitionSystemStr);
			}
			talismaneSession.setTransitionSystem(transitionSystem);

			configPath = "talismane.core.analyse.dependencyLabels";
			if (config.hasPath(configPath)) {
				InputStream dependencyLabelFile = this.getFileFromConfig(configPath);
				try (Scanner depLabelScanner = new Scanner(new BufferedReader(new InputStreamReader(dependencyLabelFile, "UTF-8")))) {
					Set<String> dependencyLabels = new HashSet<String>();
					while (depLabelScanner.hasNextLine()) {
						String dependencyLabel = depLabelScanner.nextLine();
						if (!dependencyLabel.startsWith("#"))
							dependencyLabels.add(dependencyLabel);
					}
					transitionSystem.setDependencyLabels(dependencyLabels);
				}
			}

			configPath = "talismane.core.analyse.lexicons";
			List<String> lexiconPaths = config.getStringList(configPath);
			for (String lexiconPath : lexiconPaths) {
				InputStream lexiconFile = this.getFile(configPath, lexiconPath);

				LexiconDeserializer lexiconDeserializer = new LexiconDeserializer(talismaneSession);
				List<PosTaggerLexicon> lexicons = lexiconDeserializer.deserializeLexicons(new ZipInputStream(lexiconFile));
				for (PosTaggerLexicon oneLexicon : lexicons) {
					talismaneSession.addLexicon(oneLexicon);
				}
			}

			configPath = "talismane.core.analyse.externalResources";
			List<String> externalResourcePaths = config.getStringList(configPath);
			if (externalResourcePaths.size() > 0) {
				externalResourceFinder = this.getMachineLearningService().getExternalResourceFinder();

				for (String path : externalResourcePaths) {
					LOG.info("Reading external resources from " + path);
					List<FileObject> fileObjects = this.getFileObjects(path);
					for (FileObject fileObject : fileObjects) {
						InputStream externalResourceFile = fileObject.getContent().getInputStream();
						try (Scanner scanner = new Scanner(externalResourceFile)) {
							externalResourceFinder.addExternalResource(fileObject.getName().getBaseName(), scanner);
						}
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

			configPath = "talismane.core.analyse.diacriticizer";
			if (config.hasPath(configPath)) {
				String diacriticizerPath = config.getString(configPath);
				Diacriticizer diacriticizer = diacriticizerMap.get(diacriticizerPath);

				if (diacriticizer == null) {
					LOG.info("Loading new diacriticizer from: " + diacriticizerPath);
					InputStream diacriticizerFile = this.getFileFromConfig(configPath);
					try (ZipInputStream zis = new ZipInputStream(diacriticizerFile)) {
						zis.getNextEntry();
						ObjectInputStream in = new ObjectInputStream(zis);
						diacriticizer = (Diacriticizer) in.readObject();
						diacriticizerMap.put(diacriticizerPath, diacriticizer);
					}
				} else {
					LOG.info("Fetching diacriticizer from cache for: " + diacriticizerPath);
				}
				talismaneSession.setDiacriticizer(diacriticizer);
			}

			configPath = "talismane.core.analyse.lowercasePreferences";

			if (config.hasPath(configPath)) {
				Map<String, String> lowercasePreferences = new HashMap<>();
				InputStream lowercasePreferencesFile = this.getFileFromConfig(configPath);
				try (Scanner scanner = new Scanner(lowercasePreferencesFile, "UTF-8")) {
					while (scanner.hasNextLine()) {
						String line = scanner.nextLine().trim();
						if (line.length() > 0 && !line.startsWith("#")) {
							String[] parts = line.split("\t");
							String uppercase = parts[0];
							String lowercase = parts[1];
							lowercasePreferences.put(uppercase, lowercase);
						}
					}
				}
				Diacriticizer diacriticizer = talismaneSession.getDiacriticizer();
				diacriticizer.setLowercasePreferences(lowercasePreferences);
			}

			this.getFilterService().setOutputDivider(outputDivider);
		} catch (

		IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		} catch (ClassNotFoundException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * The actual command to run by Talismane.
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
	 * If the command required a start module (e.g. analyse), the start module
	 * for this command. Default is
	 * {@link com.joliciel.talismane.Talismane.Module#SentenceDetector}.
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
	 * If the command requires an end module (e.g. analyse), the end module for
	 * this command. Default is
	 * {@link com.joliciel.talismane.Talismane.Module#Parser}.
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
	 * For commands which only affect a single module (e.g. evaluate), the
	 * module for this command.
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
	 * When analysing, should the raw text be processed by default, or should we
	 * wait until a text marker filter tells us to start processing. Default is
	 * true.
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
	 * For the "process" command, the maximum number of sentences to process. If
	 * <=0, all sentences will be processed. Default is 0 (all).
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
	 * A character (typically non-printing) which will mark a stop in the input
	 * stream and set-off analysis. The default value is the form-feed character
	 * (code=12).
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
	 * The beam width for beam-search analysis. Default is 1. Increasing this
	 * value will increase analysis time in a linear fashion, but will typically
	 * improve results.
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
	 * If true, the full beam of analyses produced as output by a given module
	 * will be used as input for the next module. If false, only the single best
	 * analysis will be used as input for the next module.
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
	 * The reader to be used to read the data for this analysis.
	 */
	@Override
	public Reader getReader() {
		try {
			if (this.reader == null) {
				String configPath = "talismane.core.inFile";
				if (config.hasPath(configPath)) {
					InputStream inFile = this.getFileFromConfig(configPath);
					this.reader = new BufferedReader(new InputStreamReader(inFile, this.getInputCharset()));
				} else {
					configPath = "talismane.core.inDir";
					if (config.hasPath(configPath)) {
						String inDirPath = config.getString(configPath);
						File inDir = new File(inDirPath);
						if (!inDir.exists())
							throw new FileNotFoundException("inDir does not exist: " + inDirPath);
						if (!inDir.isDirectory())
							throw new TalismaneException("inDir must be a directory, not a file - use inFile instead: " + inDirPath);

						DirectoryReader directoryReader = new DirectoryReader(inDir, this.getInputCharset());
						if (this.command == Command.analyse) {
							directoryReader.setEndOfFileString("\n" + this.getEndBlockCharacter());
						} else {
							directoryReader.setEndOfFileString("\n");
						}
						this.reader = directoryReader;
					} else {
						this.reader = new BufferedReader(new InputStreamReader(System.in, this.getInputCharset()));
					}
				}
			}
			return reader;
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public Reader getEvaluationReader() throws IOException {
		if (this.evaluationReader == null) {
			String configPath = "talismane.core.evaluate.evaluationFile";

			InputStream inFile = this.getFileFromConfig(configPath);
			this.evaluationReader = new BufferedReader(new InputStreamReader(inFile, this.getInputCharset()));
		}
		return evaluationReader;
	}

	@Override
	public Writer getWriter() throws IOException {
		if (writer == null) {
			String configPath = "talismane.core.outFile";
			if (config.hasPath(configPath)) {
				String outFilePath = config.getString(configPath);
				File outFile = new File(outFilePath);
				File outDir = outFile.getParentFile();
				if (outDir != null)
					outDir.mkdirs();
				outFile.delete();
				outFile.createNewFile();

				writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), this.getOutputCharset()));
			} else {
				String configPathOutDir = "talismane.core.outDir";
				String configPathInDir = "talismane.core.inDir";
				if (config.hasPath(configPathOutDir) && config.hasPath(configPathInDir) && (this.getReader() instanceof CurrentFileProvider)
						&& command != Command.evaluate) {
					String outDirPath = config.getString(configPathOutDir);
					String inDirPath = config.getString(configPathInDir);

					File outDir = new File(outDirPath);
					outDir.mkdirs();
					File inDir = new File(inDirPath);

					if (this.suffix == null)
						this.suffix = "";
					DirectoryWriter directoryWriter = new DirectoryWriter(inDir, outDir, suffix, this.getOutputCharset());
					writer = directoryWriter;
				} else {
					writer = new BufferedWriter(new OutputStreamWriter(System.out, this.getOutputCharset()));
				}
			}
		}
		return writer;
	}

	/**
	 * The filename to be applied to this analysis (if filename is included in
	 * the output).
	 */
	@Override
	public String getFileName() {
		return fileName;
	}

	/**
	 * The directory to which we write any output files.
	 */
	@Override
	public synchronized File getOutDir() {
		File outDir = null;
		String configPath = "talismane.core.outDir";
		if (config.hasPath(configPath)) {
			String outDirPath = config.getString(configPath);
			outDir = new File(outDirPath);
			outDir.mkdirs();
		} else {
			configPath = "talismane.core.outFile";
			if (config.hasPath(configPath)) {
				String outFilePath = config.getString(configPath);
				File outFile = new File(outFilePath);
				outDir = outFile.getParentFile();
				if (outDir != null) {
					outDir.mkdirs();
				}
			}
		}
		return outDir;
	}

	@Override
	public synchronized List<PosTaggerRule> getPosTaggerRules() throws IOException {
		if (posTaggerRules == null) {
			posTaggerRules = new ArrayListNoNulls<PosTaggerRule>();
			String configPath = "talismane.core.analyse.posTaggerRules";
			List<String> textFilterPaths = config.getStringList(configPath);
			for (String path : textFilterPaths) {
				LOG.debug("From: " + path);
				InputStream textFilterFile = this.getFile(configPath, path);
				try (Scanner scanner = new Scanner(textFilterFile, this.getInputCharset().name())) {
					List<String> ruleDescriptors = new ArrayListNoNulls<String>();
					while (scanner.hasNextLine()) {
						String ruleDescriptor = scanner.nextLine();
						if (ruleDescriptor.length() > 0) {
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
	}

	@Override
	public synchronized List<ParserRule> getParserRules() throws IOException {
		if (parserRules == null) {
			parserRules = new ArrayListNoNulls<ParserRule>();
			String configPath = "talismane.core.analyse.parserRules";
			List<String> textFilterPaths = config.getStringList(configPath);
			for (String path : textFilterPaths) {
				LOG.debug("From: " + path);
				InputStream textFilterFile = this.getFile(configPath, path);
				try (Scanner scanner = new Scanner(textFilterFile, this.getInputCharset().name())) {
					List<String> ruleDescriptors = new ArrayListNoNulls<String>();
					while (scanner.hasNextLine()) {
						String ruleDescriptor = scanner.nextLine();
						if (ruleDescriptor.length() > 0) {
							ruleDescriptors.add(ruleDescriptor);
							LOG.trace(ruleDescriptor);
						}
					}
					List<ParserRule> rules = this.getParserFeatureService().getRules(ruleDescriptors, dynamiseFeatures);
					parserRules.addAll(rules);
				}
			}
		}
		return parserRules;
	}

	/**
	 * A regex used to process the input, when pre-annotated.
	 * 
	 * @throws IOException
	 */
	@Override
	public synchronized String getInputRegex() throws IOException {
		if (inputRegex == null) {
			String configPath = "talismane.core.analyse.inputPattern";
			if (config.hasPath(configPath)) {
				inputRegex = config.getString(configPath);
			} else {
				configPath = "talismane.core.analyse.inputPatternFile";
				if (config.hasPath(configPath)) {
					InputStream inputPatternFile = this.getFileFromConfig(configPath);
					try (Scanner inputPatternScanner = new Scanner(
							new BufferedReader(new InputStreamReader(inputPatternFile, this.getInputCharset().name())))) {
						if (inputPatternScanner.hasNextLine()) {
							inputRegex = inputPatternScanner.nextLine();
						}
					}
					if (inputRegex == null)
						throw new TalismaneException("No input pattern found in " + config.getString("configPath"));
				}
			}
		}
		return inputRegex;
	}

	@Override
	public void setInputRegex(String inputRegex) {
		this.inputRegex = inputRegex;
	}

	@Override
	public synchronized String getEvaluationRegex() throws IOException {
		if (evaluationRegex == null) {
			String configPath = "talismane.core.evaluate.evaluationPattern";
			if (config.hasPath(configPath)) {
				evaluationRegex = config.getString(configPath);
			} else {
				configPath = "talismane.core.evaluate.evaluationPatternFile";
				if (config.hasPath(configPath)) {
					InputStream evaluationPatternFile = this.getFileFromConfig(configPath);
					try (Scanner evaluationPatternScanner = new Scanner(
							new BufferedReader(new InputStreamReader(evaluationPatternFile, this.getInputCharset().name())))) {
						if (evaluationPatternScanner.hasNextLine()) {
							evaluationRegex = evaluationPatternScanner.nextLine();
						}
					}
					if (evaluationRegex == null)
						throw new TalismaneException("No evaluation pattern found in " + config.getString(configPath));
				} else {
					evaluationRegex = this.getInputRegex();
				}
			}
		}
		return evaluationRegex;
	}

	@Override
	public synchronized List<TextMarkerFilter> getTextMarkerFilters() throws IOException {
		if (textMarkerFilters == null) {
			textMarkerFilters = new ArrayListNoNulls<TextMarkerFilter>();

			// insert sentence breaks at end of block
			this.addTextMarkerFilter(this.getFilterService().getRegexMarkerFilter(
					new MarkerFilterType[] { MarkerFilterType.SKIP, MarkerFilterType.SENTENCE_BREAK }, "" + endBlockCharacter, blockSize));

			// handle newline as requested
			if (newlineMarker.equals(MarkerFilterType.SENTENCE_BREAK))
				this.addTextMarkerFilter(this.getFilterService().getNewlineEndOfSentenceMarker());
			else if (newlineMarker.equals(MarkerFilterType.SPACE))
				this.addTextMarkerFilter(this.getFilterService().getNewlineSpaceMarker());

			// get rid of duplicate white-space always
			this.addTextMarkerFilter(this.getFilterService().getDuplicateWhiteSpaceFilter());

			// replace tabs with white space
			this.addTextMarkerFilter(this.getFilterService().getOtherWhiteSpaceFilter());

			String configPath = "talismane.core.analyse.textFilters";
			List<String> textFilterPaths = config.getStringList(configPath);
			for (String path : textFilterPaths) {
				LOG.debug("From: " + path);
				InputStream textFilterFile = this.getFile(configPath, path);
				try (Scanner scanner = new Scanner(textFilterFile, this.getInputCharset().name())) {
					while (scanner.hasNextLine()) {
						String descriptor = scanner.nextLine();
						LOG.debug(descriptor);
						if (descriptor.length() > 0 && !descriptor.startsWith("#")) {
							TextMarkerFilter textMarkerFilter = this.getFilterService().getTextMarkerFilter(descriptor, blockSize);
							this.addTextMarkerFilter(textMarkerFilter);
						}
					}
				}
			}
		}
		return textMarkerFilters;
	}

	@Override
	public void setTextMarkerFilters(Scanner scanner) {
		textMarkerFilters = new ArrayListNoNulls<TextMarkerFilter>();
		while (scanner.hasNextLine()) {
			String descriptor = scanner.nextLine();
			LOG.debug(descriptor);
			if (descriptor.length() > 0 && !descriptor.startsWith("#")) {
				TextMarkerFilter textMarkerFilter = this.getFilterService().getTextMarkerFilter(descriptor, blockSize);
				this.addTextMarkerFilter(textMarkerFilter);
			}
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
	 * 
	 * @throws IOException
	 */
	private synchronized List<TokenSequenceFilter> getTokenSequenceFilters(MachineLearningModel model) throws IOException {
		if (tokenSequenceFilters == null) {
			List<String> tokenSequenceFilterDescriptors = new ArrayListNoNulls<String>();
			tokenSequenceFilters = new ArrayListNoNulls<TokenSequenceFilter>();

			String configPath = "talismane.core.analyse.tokenSequenceFilters";
			List<String> tokenFilterPaths = config.getStringList(configPath);
			for (String path : tokenFilterPaths) {
				LOG.debug("From: " + path);
				InputStream tokenFilterFile = this.getFile(configPath, path);
				try (Scanner scanner = new Scanner(tokenFilterFile, this.getInputCharset().name())) {
					while (scanner.hasNextLine()) {
						String descriptor = scanner.nextLine();
						LOG.debug(descriptor);
						tokenSequenceFilterDescriptors.add(descriptor);
						if (descriptor.length() > 0 && !descriptor.startsWith("#")) {
							TokenSequenceFilter tokenSequenceFilter = this.getTokenFilterService().getTokenSequenceFilter(descriptor);
							if (tokenSequenceFilter instanceof NeedsTalismaneSession)
								((NeedsTalismaneSession) tokenSequenceFilter).setTalismaneSession(talismaneSession);
							tokenSequenceFilters.add(tokenSequenceFilter);
						}
					}
				}
			}

			boolean tokenSequenceFiltersReplace = config.getBoolean("talismane.core.analyse.tokenFiltersReplace");
			if (!tokenSequenceFiltersReplace && model != null) {
				LOG.debug("From model");
				List<String> modelDescriptors = model.getDescriptors().get(PosTagFilterService.POSTAG_PREPROCESSING_FILTER_DESCRIPTOR_KEY);

				if (modelDescriptors != null) {
					for (String descriptor : modelDescriptors) {
						LOG.debug(descriptor);
						tokenSequenceFilterDescriptors.add(descriptor);
						if (descriptor.length() > 0 && !descriptor.startsWith("#")) {
							TokenSequenceFilter tokenSequenceFilter = this.getTokenFilterService().getTokenSequenceFilter(descriptor);
							if (tokenSequenceFilter instanceof NeedsTalismaneSession)
								((NeedsTalismaneSession) tokenSequenceFilter).setTalismaneSession(talismaneSession);
							tokenSequenceFilters.add(tokenSequenceFilter);
						}
					}
				}
			}

			this.getDescriptors().put(PosTagFilterService.POSTAG_PREPROCESSING_FILTER_DESCRIPTOR_KEY, tokenSequenceFilterDescriptors);
		}
		return tokenSequenceFilters;
	}

	private synchronized List<PosTagSequenceFilter> getPosTagSequenceFilters(MachineLearningModel model) throws IOException {
		if (posTaggerPostProcessingFilters == null) {
			List<String> posTaggerPostProcessingFilterDescriptors = new ArrayListNoNulls<String>();
			posTaggerPostProcessingFilters = new ArrayListNoNulls<PosTagSequenceFilter>();

			String configPath = "talismane.core.analyse.posTagSequenceFilters";
			List<String> posTagSequenceFilterrPaths = config.getStringList(configPath);
			for (String path : posTagSequenceFilterrPaths) {
				LOG.debug("From: " + path);
				InputStream tokenFilterFile = this.getFile(configPath, path);
				try (Scanner scanner = new Scanner(tokenFilterFile, this.getInputCharset().name())) {
					while (scanner.hasNextLine()) {
						String descriptor = scanner.nextLine();
						LOG.debug(descriptor);
						posTaggerPostProcessingFilterDescriptors.add(descriptor);
						if (descriptor.length() > 0 && !descriptor.startsWith("#")) {
							PosTagSequenceFilter filter = this.getPosTagFilterService().getPosTagSequenceFilter(descriptor);
							posTaggerPostProcessingFilters.add(filter);
						}
					}
				}
			}

			boolean posTagSequenceFiltersReplace = config.getBoolean("talismane.core.analyse.posTagSequenceFiltersReplace");
			if (!posTagSequenceFiltersReplace && model != null) {
				List<String> modelDescriptors = model.getDescriptors().get(PosTagFilterService.POSTAG_POSTPROCESSING_FILTER_DESCRIPTOR_KEY);

				if (modelDescriptors != null) {
					for (String descriptor : modelDescriptors) {
						LOG.debug(descriptor);
						posTaggerPostProcessingFilterDescriptors.add(descriptor);
						if (descriptor.length() > 0 && !descriptor.startsWith("#")) {
							PosTagSequenceFilter filter = this.getPosTagFilterService().getPosTagSequenceFilter(descriptor);
							posTaggerPostProcessingFilters.add(filter);
						}
					}
				}
			}

			this.getDescriptors().put(PosTagFilterService.POSTAG_POSTPROCESSING_FILTER_DESCRIPTOR_KEY, posTaggerPostProcessingFilterDescriptors);
		}

		return posTaggerPostProcessingFilters;
	}

	/**
	 * TokenFilters to be applied during analysis.
	 * 
	 * @throws IOException
	 */
	private synchronized List<TokenFilter> getTokenFilters(MachineLearningModel model) throws IOException {
		if (tokenFilters == null) {
			List<String> tokenFilterDescriptors = new ArrayListNoNulls<String>();
			tokenFilters = new ArrayListNoNulls<TokenFilter>();

			LOG.debug("Token filters");

			for (TokenFilter tokenFilter : this.prependedTokenFilters)
				this.tokenFilters.add(tokenFilter);

			LOG.debug("tokenFilters");
			String configPath = "talismane.core.analyse.tokenFilters";
			List<String> tokenFilterPaths = config.getStringList(configPath);
			for (String path : tokenFilterPaths) {
				LOG.debug("From: " + path);
				InputStream tokenFilterFile = this.getFile(configPath, path);
				try (Scanner scanner = new Scanner(tokenFilterFile, this.getInputCharset().name())) {
					List<TokenFilter> myFilters = this.getTokenFilterService().readTokenFilters(scanner, path, tokenFilterDescriptors);
					for (TokenFilter tokenFilter : myFilters) {
						tokenFilters.add(tokenFilter);
					}
				}
			}

			boolean tokenFiltersReplace = config.getBoolean("talismane.core.analyse.tokenFiltersReplace");
			if (!tokenFiltersReplace) {
				if (model != null) {
					LOG.debug("From model");
					List<String> modelDescriptors = model.getDescriptors().get(TokenFilterService.TOKEN_FILTER_DESCRIPTOR_KEY);
					String modelDescriptorString = "";
					if (modelDescriptors != null) {
						for (String descriptor : modelDescriptors) {
							modelDescriptorString += descriptor + "\n";
						}
					}
					try (Scanner scanner = new Scanner(modelDescriptorString)) {
						List<TokenFilter> myFilters = this.getTokenFilterService().readTokenFilters(scanner, tokenFilterDescriptors);
						for (TokenFilter tokenFilter : myFilters) {
							tokenFilters.add(tokenFilter);
						}
					}
				}
			}

			this.getDescriptors().put(TokenFilterService.TOKEN_FILTER_DESCRIPTOR_KEY, tokenFilterDescriptors);

			for (TokenFilter tokenFilter : this.additionalTokenFilters)
				this.tokenFilters.add(tokenFilter);
		}
		return tokenFilters;
	}

	@Override
	public synchronized void setTokenFilters(Scanner scanner) {
		List<String> tokenFilterDescriptors = new ArrayListNoNulls<String>();
		tokenFilters = new ArrayListNoNulls<TokenFilter>();
		List<TokenFilter> myFilters = this.getTokenFilterService().readTokenFilters(scanner, tokenFilterDescriptors);
		for (TokenFilter tokenFilter : myFilters) {
			tokenFilters.add(tokenFilter);
		}
		this.getDescriptors().put(TokenFilterService.TOKEN_FILTER_DESCRIPTOR_KEY, tokenFilterDescriptors);
	}

	/**
	 * The language detector to use for analysis.
	 */
	@Override
	public LanguageDetector getLanguageDetector() {
		try {
			ClassificationModel languageModel = this.getLanguageModel();
			LanguageDetector languageDetector = this.getLanguageDetectorService().getLanguageDetector(languageModel);

			return languageDetector;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * The sentence detector to use for analysis.
	 */
	@Override
	public SentenceDetector getSentenceDetector() {
		try {
			ClassificationModel sentenceModel = this.getSentenceDetectorModel();
			SentenceDetector sentenceDetector = this.getSentenceDetectorService().getSentenceDetector(sentenceModel);

			return sentenceDetector;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * The tokeniser to use for analysis.
	 */
	@Override
	public Tokeniser getTokeniser() {
		try {
			Tokeniser tokeniser = null;
			ClassificationModel tokeniserModel = null;
			if (tokeniserType == TokeniserType.simple) {
				tokeniser = this.getTokeniserService().getSimpleTokeniser();
			} else if (tokeniserType == TokeniserType.pattern) {
				LOG.debug("Getting tokeniser model");
				tokeniserModel = this.getTokeniserModel();
				if (tokeniserModel == null)
					throw new TalismaneException("No tokeniserModel provided");

				tokeniser = this.getTokeniserPatternService().getPatternTokeniser(tokeniserModel, tokeniserBeamWidth);

				if (includeDetails) {
					String detailsFilePath = this.getBaseName() + "_tokeniser_details.txt";
					File detailsFile = new File(this.getOutDir(), detailsFilePath);
					detailsFile.delete();
					ClassificationObserver observer = tokeniserModel.getDetailedAnalysisObserver(detailsFile);
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

			return tokeniser;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	synchronized ClassificationModel getLanguageModel() throws IOException {
		if (languageModel == null) {
			LOG.debug("Getting languageDetector model");

			String configPath = "talismane.core.analyse.languageModel";
			InputStream languageModelFile = this.getFileFromConfig(configPath);
			languageModel = this.getMachineLearningService().getClassificationModel(new ZipInputStream(languageModelFile));
		}
		return languageModel;
	}

	synchronized ClassificationModel getSentenceDetectorModel() throws IOException {
		if (sentenceModel == null) {
			LOG.debug("Getting sentenceDetector model");

			String configPath = "talismane.core.analyse.sentenceModel";
			InputStream sentenceModelFile = this.getFileFromConfig(configPath);
			sentenceModel = this.getMachineLearningService().getClassificationModel(new ZipInputStream(sentenceModelFile));
		}
		return sentenceModel;
	}

	synchronized ClassificationModel getTokeniserModel() throws IOException {
		if (tokeniserModel == null) {
			LOG.debug("Getting tokeniser model");

			String configPath = "talismane.core.analyse.tokeniserModel";
			InputStream tokeniserModelFile = this.getFileFromConfig(configPath);
			tokeniserModel = this.getMachineLearningService().getClassificationModel(new ZipInputStream(tokeniserModelFile));
		}
		return tokeniserModel;
	}

	synchronized ClassificationModel getPosTaggerModel() throws IOException {
		if (posTaggerModel == null) {
			LOG.debug("Getting posTagger model");

			String configPath = "talismane.core.analyse.posTaggerModel";
			InputStream posTaggerModelFile = this.getFileFromConfig(configPath);
			posTaggerModel = this.getMachineLearningService().getClassificationModel(new ZipInputStream(posTaggerModelFile));
		}
		return posTaggerModel;
	}

	synchronized MachineLearningModel getParserModel() throws IOException {
		if (parserModel == null) {
			LOG.debug("Getting parser model");

			String configPath = "talismane.core.analyse.parserModel";
			InputStream parserModelFile = this.getFileFromConfig(configPath);
			parserModel = this.getMachineLearningService().getClassificationModel(new ZipInputStream(parserModelFile));

			talismaneSession.setTransitionSystem(parserService.getTransitionSystem(parserModel));
		}
		return parserModel;
	}

	@Override
	public synchronized TokeniserPatternManager getTokeniserPatternManager() throws IOException {
		if (tokeniserPatternManager == null) {
			String configPath = "talismane.core.train.tokeniser.patterns";
			InputStream tokeniserPatternFile = this.getFileFromConfig(configPath);
			try (Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(tokeniserPatternFile, this.getInputCharset())))) {
				List<String> patternDescriptors = new ArrayListNoNulls<String>();
				while (scanner.hasNextLine()) {
					String descriptor = scanner.nextLine();
					patternDescriptors.add(descriptor);
					LOG.debug(descriptor);
				}

				this.getDescriptors().put(TokeniserPatternService.PATTERN_DESCRIPTOR_KEY, patternDescriptors);

				tokeniserPatternManager = this.getTokeniserPatternService().getPatternManager(patternDescriptors);
			}
		}
		return tokeniserPatternManager;
	}

	@Override
	public synchronized Set<LanguageDetectorFeature<?>> getLanguageDetectorFeatures() throws IOException {
		if (languageFeatures == null) {
			String configPath = "talismane.core.train.languageDetector.features";
			InputStream languageFeatureFile = this.getFileFromConfig(configPath);
			try (Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(languageFeatureFile, this.getInputCharset())))) {
				List<String> featureDescriptors = new ArrayListNoNulls<String>();
				while (scanner.hasNextLine()) {
					String descriptor = scanner.nextLine();
					featureDescriptors.add(descriptor);
					LOG.debug(descriptor);
				}

				languageFeatures = this.getLanguageDetectorService().getFeatureSet(featureDescriptors);

				this.getDescriptors().put(MachineLearningModel.FEATURE_DESCRIPTOR_KEY, featureDescriptors);
			}
		}
		return languageFeatures;
	}

	@Override
	public synchronized Set<SentenceDetectorFeature<?>> getSentenceDetectorFeatures() throws IOException {
		if (sentenceFeatures == null) {
			String configPath = "talismane.core.train.sentenceDetector.features";

			InputStream sentenceFeatureFile = this.getFileFromConfig(configPath);
			try (Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(sentenceFeatureFile, this.getInputCharset())))) {
				List<String> featureDescriptors = new ArrayListNoNulls<String>();
				while (scanner.hasNextLine()) {
					String descriptor = scanner.nextLine();
					featureDescriptors.add(descriptor);
					LOG.debug(descriptor);
				}

				sentenceFeatures = this.getSentenceDetectorFeatureService().getFeatureSet(featureDescriptors);

				this.getDescriptors().put(MachineLearningModel.FEATURE_DESCRIPTOR_KEY, featureDescriptors);
			}
		}
		return sentenceFeatures;
	}

	@Override
	public synchronized Set<TokeniserContextFeature<?>> getTokeniserContextFeatures() throws IOException {
		if (tokeniserContextFeatures == null) {
			String configPath = "talismane.core.train.tokeniser.features";
			InputStream tokeniserFeatureFile = this.getFileFromConfig(configPath);
			try (Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(tokeniserFeatureFile, this.getInputCharset())))) {
				List<String> featureDescriptors = new ArrayListNoNulls<String>();
				while (scanner.hasNextLine()) {
					String descriptor = scanner.nextLine();
					featureDescriptors.add(descriptor);
					LOG.debug(descriptor);
				}

				TokeniserPatternManager tokeniserPatternManager = this.getTokeniserPatternManager();
				tokeniserContextFeatures = this.getTokenFeatureService().getTokeniserContextFeatureSet(featureDescriptors,
						tokeniserPatternManager.getParsedTestPatterns());

				this.getDescriptors().put(MachineLearningModel.FEATURE_DESCRIPTOR_KEY, featureDescriptors);
			}
		}
		return tokeniserContextFeatures;
	}

	@Override
	public synchronized Set<TokenPatternMatchFeature<?>> getTokenPatternMatchFeatures() throws IOException {
		if (tokenPatternMatchFeatures == null) {
			String configPath = "talismane.core.train.tokeniser.features";
			InputStream tokeniserFeatureFile = this.getFileFromConfig(configPath);
			try (Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(tokeniserFeatureFile, this.getInputCharset())))) {
				List<String> featureDescriptors = new ArrayListNoNulls<String>();
				while (scanner.hasNextLine()) {
					String descriptor = scanner.nextLine();
					featureDescriptors.add(descriptor);
					LOG.debug(descriptor);
				}

				tokenPatternMatchFeatures = this.getTokenFeatureService().getTokenPatternMatchFeatureSet(featureDescriptors);

				this.getDescriptors().put(MachineLearningModel.FEATURE_DESCRIPTOR_KEY, featureDescriptors);
			}
		}
		return tokenPatternMatchFeatures;
	}

	@Override
	public synchronized Set<PosTaggerFeature<?>> getPosTaggerFeatures() throws IOException {
		if (posTaggerFeatures == null) {
			String configPath = "talismane.core.train.posTagger.features";
			InputStream posTaggerFeatureFile = this.getFileFromConfig(configPath);
			try (Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(posTaggerFeatureFile, this.getInputCharset())))) {
				List<String> featureDescriptors = new ArrayListNoNulls<String>();
				while (scanner.hasNextLine()) {
					String descriptor = scanner.nextLine();
					featureDescriptors.add(descriptor);
					LOG.debug(descriptor);
				}

				posTaggerFeatures = this.getPosTaggerFeatureService().getFeatureSet(featureDescriptors);

				this.getDescriptors().put(MachineLearningModel.FEATURE_DESCRIPTOR_KEY, featureDescriptors);
			}
		}
		return posTaggerFeatures;
	}

	@Override
	public synchronized ClassificationEventStream getClassificationEventStream() throws IOException {
		if (this.classificationEventStream == null) {
			switch (this.getModule()) {
			case LanguageDetector:
				classificationEventStream = this.getLanguageDetectorService().getLanguageDetectorEventStream(this.getLanguageCorpusReader(),
						this.getLanguageDetectorFeatures());
				break;
			case SentenceDetector:
				classificationEventStream = this.getSentenceDetectorService().getSentenceDetectorEventStream(this.getSentenceCorpusReader(),
						this.getSentenceDetectorFeatures());
				break;
			case Tokeniser:
				if (patternTokeniserType == PatternTokeniserType.Interval) {
					Set<TokeniserContextFeature<?>> features = this.getTokeniserContextFeatures();
					classificationEventStream = this.getTokeniserPatternService().getIntervalPatternEventStream(this.getTokenCorpusReader(), features,
							this.getTokeniserPatternManager());
				} else {
					Set<TokenPatternMatchFeature<?>> features = this.getTokenPatternMatchFeatures();
					classificationEventStream = this.getTokeniserPatternService().getCompoundPatternEventStream(this.getTokenCorpusReader(), features,
							this.getTokeniserPatternManager());
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
	 */
	@Override
	public PosTagger getPosTagger() {
		try {
			PosTagger posTagger = null;

			ClassificationModel posTaggerModel = this.getPosTaggerModel();
			if (posTaggerModel == null)
				throw new TalismaneException("No posTaggerModel provided");

			posTagger = this.getPosTaggerService().getPosTagger(posTaggerModel, posTaggerBeamWidth);

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
				ClassificationObserver observer = posTaggerModel.getDetailedAnalysisObserver(detailsFile);
				posTagger.addObserver(observer);
			}
			return posTagger;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * The parser to use for analysis.
	 */
	@Override
	public Parser getParser() {
		try {
			Parser parser = null;
			MachineLearningModel parserModel = this.getParserModel();
			if (parserModel == null)
				throw new TalismaneException("No parserModel provided");

			parser = this.getParserService().getTransitionBasedParser(parserModel, parserBeamWidth, dynamiseFeatures);
			parser.setMaxAnalysisTimePerSentence(maxParseAnalysisTime);
			parser.setMinFreeMemory(minFreeMemory);

			parser.setParserRules(this.getParserRules());

			if (parser instanceof TransitionBasedParser) {
				TransitionBasedParser transitionBasedParser = (TransitionBasedParser) parser;
				transitionBasedParser.setEarlyStop(earlyStop);
			}

			if (parseComparisonStrategyType != null) {
				ParseComparisonStrategy parseComparisonStrategy = parserService.getParseComparisonStrategy(parseComparisonStrategyType);
				parser.setParseComparisonStrategy(parseComparisonStrategy);
			}

			if (includeDetails && parserModel instanceof ClassificationModel) {
				String detailsFilePath = this.getBaseName() + "_parser_details.txt";
				File detailsFile = new File(this.getOutDir(), detailsFilePath);
				detailsFile.delete();
				ClassificationModel classificationModel = (ClassificationModel) parserModel;
				ClassificationObserver observer = classificationModel.getDetailedAnalysisObserver(detailsFile);
				parser.addObserver(observer);
			}

			return parser;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public synchronized Set<ParseConfigurationFeature<?>> getParserFeatures() throws IOException {
		if (parserFeatures == null) {
			String configPath = "talismane.core.train.parser.features";
			InputStream parserFeatureFile = this.getFileFromConfig(configPath);
			try (Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(parserFeatureFile, this.getInputCharset())))) {
				List<String> featureDescriptors = new ArrayListNoNulls<String>();
				while (scanner.hasNextLine()) {
					String descriptor = scanner.nextLine();
					featureDescriptors.add(descriptor);
					LOG.debug(descriptor);
				}

				parserFeatures = this.getParserFeatureService().getFeatures(featureDescriptors);

				this.getDescriptors().put(MachineLearningModel.FEATURE_DESCRIPTOR_KEY, featureDescriptors);
			}
		}
		return parserFeatures;
	}

	/**
	 * The maximum amount of time the parser will spend analysing any single
	 * sentence, in seconds. If it exceeds this time, the parser will return a
	 * partial analysis, or a "dependency forest", where certain nodes are left
	 * unattached (no governor).<br/>
	 * A value of 0 indicates that there is no maximum time - the parser will
	 * always continue until sentence analysis is complete.<br/>
	 * The default value is 60.<br/>
	 */
	@Override
	public int getMaxParseAnalysisTime() {
		return maxParseAnalysisTime;
	}

	@Override
	public void setMaxParseAnalysisTime(int maxParseAnalysisTime) {
		this.maxParseAnalysisTime = maxParseAnalysisTime;
	}

	@Override
	public synchronized SentenceProcessor getSentenceProcessor() throws IOException {
		if (sentenceProcessor == null && endModule.equals(Module.SentenceDetector)) {
			Reader templateReader = null;
			String configPath = "talismane.core.analyse.template";
			if (config.hasPath(configPath)) {
				templateReader = new BufferedReader(new InputStreamReader(this.getFileFromConfig(configPath)));
			} else {
				templateReader = new BufferedReader(new InputStreamReader(getInputStreamFromResource(sentenceTemplateName)));
			}
			FreemarkerTemplateWriter templateWriter = new FreemarkerTemplateWriter(templateReader);
			sentenceProcessor = templateWriter;
		}
		return sentenceProcessor;
	}

	@Override
	public synchronized TokenSequenceProcessor getTokenSequenceProcessor() throws IOException {
		if (tokenSequenceProcessor == null && endModule.equals(Module.Tokeniser)) {
			Reader templateReader = null;
			String configPath = "talismane.core.analyse.template";
			if (config.hasPath(configPath)) {
				templateReader = new BufferedReader(new InputStreamReader(this.getFileFromConfig(configPath)));
			} else {
				templateReader = new BufferedReader(new InputStreamReader(getInputStreamFromResource(tokeniserTemplateName)));
			}

			FreemarkerTemplateWriter templateWriter = new FreemarkerTemplateWriter(templateReader);
			tokenSequenceProcessor = templateWriter;
		}
		return tokenSequenceProcessor;
	}

	@Override
	public synchronized PosTagSequenceProcessor getPosTagSequenceProcessor() throws IOException {
		if (posTagSequenceProcessor == null && endModule.equals(Module.PosTagger)) {
			if (this.option == Option.posTagFeatureTester) {
				File file = new File(this.getOutDir(), this.getBaseName() + "_featureTest.txt");
				posTagSequenceProcessor = this.getPosTaggerService().getPosTagFeatureTester(this.getPosTaggerFeatures(), this.testWords, file);
			} else {
				Reader templateReader = null;
				String configPath = "talismane.core.analyse.template";
				if (config.hasPath(configPath)) {
					templateReader = new BufferedReader(new InputStreamReader(this.getFileFromConfig(configPath)));
				} else {
					templateReader = new BufferedReader(new InputStreamReader(getInputStreamFromResource(posTaggerTemplateName)));
				}
				FreemarkerTemplateWriter templateWriter = new FreemarkerTemplateWriter(templateReader);
				posTagSequenceProcessor = templateWriter;
			}
		}
		return posTagSequenceProcessor;
	}

	@Override
	public synchronized ParseConfigurationProcessor getParseConfigurationProcessor() throws IOException {
		if (parseConfigurationProcessor == null && endModule.equals(Module.Parser)) {
			if (option.equals(Option.output)) {
				Reader templateReader = null;
				String configPath = "talismane.core.analyse.template";
				if (config.hasPath(configPath)) {
					templateReader = new BufferedReader(new InputStreamReader(this.getFileFromConfig(configPath)));
				} else {
					templateReader = new BufferedReader(new InputStreamReader(getInputStreamFromResource(parserTemplateName)));
				}
				FreemarkerTemplateWriter templateWriter = new FreemarkerTemplateWriter(templateReader);
				parseConfigurationProcessor = templateWriter;
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

				Writer csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false), csvEncoding));
				ParseConfigurationProcessor transitionLogWriter = this.getParserService().getTransitionLogWriter(csvFileWriter);

				chain.addProcessor(transitionLogWriter);

				parseConfigurationProcessor = chain;
			}
		}
		return parseConfigurationProcessor;
	}

	@Override
	public synchronized TokeniserAnnotatedCorpusReader getTokenCorpusReader() throws IOException {
		if (tokenCorpusReader == null) {
			TokenRegexBasedCorpusReader tokenRegexCorpusReader = this.getTokeniserService().getRegexBasedCorpusReader(this.getReader());
			if (this.getInputRegex() != null)
				tokenRegexCorpusReader.setRegex(this.getInputRegex());

			String configPath = "talismane.core.train.tokeniser.sentenceReader";
			if (config.hasPath(configPath)) {
				InputStream sentenceReaderFile = this.getFileFromConfig(configPath);
				Reader sentenceFileReader = new BufferedReader(new InputStreamReader(sentenceReaderFile, this.getInputCharset()));
				SentenceDetectorAnnotatedCorpusReader sentenceReader = this.getSentenceDetectorService().getDefaultReader(sentenceFileReader);
				tokenRegexCorpusReader.setSentenceReader(sentenceReader);
			}
			this.tokenCorpusReader = tokenRegexCorpusReader;
		}
		this.setCorpusReaderAttributes(tokenCorpusReader);

		this.addTokenCorpusReaderFilters(tokenCorpusReader);
		return tokenCorpusReader;
	}

	@Override
	public synchronized TokeniserAnnotatedCorpusReader getTokenEvaluationCorpusReader() throws IOException {
		if (tokenEvaluationCorpusReader == null) {
			TokenRegexBasedCorpusReader tokenRegexCorpusReader = this.getTokeniserService().getRegexBasedCorpusReader(this.getEvaluationReader());
			if (this.getEvaluationRegex() != null)
				tokenRegexCorpusReader.setRegex(this.getEvaluationRegex());

			this.tokenEvaluationCorpusReader = tokenRegexCorpusReader;
		}
		this.setCorpusReaderAttributes(tokenEvaluationCorpusReader);

		this.addTokenCorpusReaderFilters(tokenEvaluationCorpusReader);
		return tokenEvaluationCorpusReader;
	}

	synchronized void addTokenCorpusReaderFilters(TokeniserAnnotatedCorpusReader corpusReader) throws IOException {
		if (!tokenCorpusReaderFiltersAdded) {
			MachineLearningModel myTokeniserModel = null;

			if (command != Command.train) {
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
	public void setTokenCorpusReader(TokeniserAnnotatedCorpusReader tokenCorpusReader) {
		this.tokenCorpusReader = tokenCorpusReader;
	}

	@Override
	public synchronized PosTagAnnotatedCorpusReader getPosTagCorpusReader() throws IOException {
		if (posTagCorpusReader == null) {
			PosTagRegexBasedCorpusReader posTagRegexBasedCorpusReader = this.getPosTaggerService().getRegexBasedCorpusReader(this.getReader());
			if (this.getInputRegex() != null)
				posTagRegexBasedCorpusReader.setRegex(this.getInputRegex());
			posTagCorpusReader = posTagRegexBasedCorpusReader;
		}
		this.setCorpusReaderAttributes(posTagCorpusReader);

		this.addPosTagCorpusReaderFilters(posTagCorpusReader);
		return posTagCorpusReader;
	}

	@Override
	public synchronized PosTagAnnotatedCorpusReader getPosTagEvaluationCorpusReader() throws IOException {
		if (posTagEvaluationCorpusReader == null) {
			PosTagRegexBasedCorpusReader posTagRegexCorpusReader = this.getPosTaggerService().getRegexBasedCorpusReader(this.getEvaluationReader());
			if (this.getEvaluationRegex() != null)
				posTagRegexCorpusReader.setRegex(this.getEvaluationRegex());
			this.posTagEvaluationCorpusReader = posTagRegexCorpusReader;
		}
		this.addPosTagCorpusReaderFilters(posTagEvaluationCorpusReader);
		return posTagEvaluationCorpusReader;
	}

	synchronized void addPosTagCorpusReaderFilters(PosTagAnnotatedCorpusReader corpusReader) throws IOException {
		if (!posTagCorpusReaderFiltersAdded) {
			MachineLearningModel myPosTaggerModel = null;
			if (this.getCommand() != Command.train) {
				if (this.getStartModule().equals(Module.Tokeniser)) {
					myPosTaggerModel = this.getPosTaggerModel();
				} else if (this.getStartModule().equals(Module.PosTagger)) {
					myPosTaggerModel = this.getPosTaggerModel();
				} else {
					myPosTaggerModel = this.getParserModel();
				}
			} // do the models exist already?

			List<TokenFilter> tokenFilters = new ArrayListNoNulls<TokenFilter>();
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
	 * 
	 * @throws IOException
	 */
	@Override
	public synchronized ParserAnnotatedCorpusReader getParserCorpusReader() throws IOException {
		if (parserCorpusReader == null) {
			ParserRegexBasedCorpusReader parserRegexCorpusReader = this.getParserService().getRegexBasedCorpusReader(this.getReader());
			if (this.getInputRegex() != null)
				parserRegexCorpusReader.setRegex(this.getInputRegex());
			parserRegexCorpusReader.setPredictTransitions(predictTransitions);

			String configPath = "talismane.core.process.corpusLexicalEntryRegex";
			if (config.hasPath(configPath)) {
				InputStream corpusLexicalEntryRegexFile = this.getFileFromConfig(configPath);

				try (Scanner corpusLexicalEntryRegexScanner = new Scanner(
						new BufferedReader(new InputStreamReader(corpusLexicalEntryRegexFile, this.getInputCharset().name())))) {
					LexicalEntryReader lexicalEntryReader = new RegexLexicalEntryReader(corpusLexicalEntryRegexScanner);
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
	}

	synchronized void setCorpusReaderAttributes(AnnotatedCorpusReader corpusReader) {
		corpusReader.setMaxSentenceCount(maxSentenceCount);
		corpusReader.setStartSentence(startSentence);
		if (crossValidationSize > 0)
			corpusReader.setCrossValidationSize(crossValidationSize);
		if (includeIndex >= 0)
			corpusReader.setIncludeIndex(includeIndex);
		if (excludeIndex >= 0)
			corpusReader.setExcludeIndex(excludeIndex);
	}

	@Override
	public synchronized ParserAnnotatedCorpusReader getParserEvaluationCorpusReader() throws IOException {
		if (parserEvaluationCorpusReader == null) {
			ParserRegexBasedCorpusReader parserRegexCorpusReader = this.getParserService().getRegexBasedCorpusReader(this.getEvaluationReader());
			if (this.getEvaluationRegex() != null)
				parserRegexCorpusReader.setRegex(this.getEvaluationRegex());
			parserRegexCorpusReader.setPredictTransitions(predictTransitions);

			this.parserEvaluationCorpusReader = parserRegexCorpusReader;
		}
		this.addParserCorpusReaderFilters(parserEvaluationCorpusReader);
		return parserEvaluationCorpusReader;
	}

	@Override
	public void setParserEvaluationCorpusReader(ParserAnnotatedCorpusReader parserEvaluationCorpusReader) {
		this.parserEvaluationCorpusReader = parserEvaluationCorpusReader;
	}

	@Override
	public void setPosTagEvaluationCorpusReader(PosTagAnnotatedCorpusReader posTagEvaluationCorpusReader) {
		this.posTagEvaluationCorpusReader = posTagEvaluationCorpusReader;
	}

	synchronized void addParserCorpusReaderFilters(ParserAnnotatedCorpusReader corpusReader) throws IOException {
		if (!parserCorpusReaderFiltersAdded) {
			MachineLearningModel myPosTaggerModel = null;
			MachineLearningModel myParserModel = null;
			if (this.getCommand() != Command.train) {
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

			List<TokenFilter> tokenFilters = new ArrayListNoNulls<TokenFilter>();
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
	public void setPosTagCorpusReader(PosTagAnnotatedCorpusReader posTagCorpusReader) {
		this.posTagCorpusReader = posTagCorpusReader;
	}

	@Override
	public void setParserCorpusReader(ParserAnnotatedCorpusReader parserCorpusReader) {
		this.parserCorpusReader = parserCorpusReader;
	}

	/**
	 * Get a parser evaluator if command=evaluate and endModule=parser.
	 */
	@Override
	public synchronized ParserEvaluator getParserEvaluator() {
		try {
			if (parserEvaluator == null) {
				parserEvaluator = this.getParserService().getParserEvaluator();
				if (startModule.equals(Module.Tokeniser)) {
					parserEvaluator.setTokeniser(this.getTokeniser());
					parserEvaluator.setPosTagger(this.getPosTagger());
				} else if (startModule.equals(Module.PosTagger)) {
					parserEvaluator.setPosTagger(this.getPosTagger());
				}
				parserEvaluator.setParser(this.getParser());

				ParseTimeByLengthObserver parseTimeByLengthObserver = new ParseTimeByLengthObserver();
				if (includeTimePerToken) {
					File timePerTokenFile = new File(this.getOutDir(), this.getBaseName() + ".timePerToken.csv");
					Writer csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(timePerTokenFile, false), csvEncoding));
					parseTimeByLengthObserver.setWriter(csvFileWriter);
				}
				parserEvaluator.addObserver(parseTimeByLengthObserver);

				File fscoreFile = new File(this.getOutDir(), this.getBaseName() + ".fscores.csv");

				ParseEvaluationFScoreCalculator parseFScoreCalculator = new ParseEvaluationFScoreCalculator(fscoreFile);
				parseFScoreCalculator.setLabeledEvaluation(this.labeledEvaluation);

				if (parserEvaluator.getTokeniser() != null)
					parseFScoreCalculator.setHasTokeniser(true);
				if (parserEvaluator.getPosTagger() != null)
					parseFScoreCalculator.setHasPosTagger(true);
				parserEvaluator.addObserver(parseFScoreCalculator);

				if (outputGuesses) {
					File csvFile = new File(this.getOutDir(), this.getBaseName() + "_sentences.csv");
					csvFile.delete();
					csvFile.createNewFile();
					Writer csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false), csvEncoding));
					int guessCount = 1;
					if (outputGuessCount > 0)
						guessCount = outputGuessCount;
					else
						guessCount = parserEvaluator.getParser().getBeamWidth();

					ParseEvaluationSentenceWriter sentenceWriter = new ParseEvaluationSentenceWriter(csvFileWriter, guessCount);
					if (parserEvaluator.getTokeniser() != null)
						sentenceWriter.setHasTokeniser(true);
					if (parserEvaluator.getPosTagger() != null)
						sentenceWriter.setHasPosTagger(true);
					parserEvaluator.addObserver(sentenceWriter);
				}

				if (includeDistanceFScores) {
					File csvFile = new File(this.getOutDir(), this.getBaseName() + "_distances.csv");
					csvFile.delete();
					csvFile.createNewFile();
					Writer csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false), csvEncoding));
					ParserFScoreCalculatorByDistance calculator = new ParserFScoreCalculatorByDistance(csvFileWriter);
					calculator.setLabeledEvaluation(this.labeledEvaluation);
					if (skipLabel != null)
						calculator.setSkipLabel(skipLabel);
					parserEvaluator.addObserver(calculator);
				}

				if (includeTransitionLog) {
					File csvFile = new File(this.getOutDir(), this.getBaseName() + "_transitions.csv");
					csvFile.delete();
					csvFile.createNewFile();
					Writer csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false), csvEncoding));
					ParseConfigurationProcessor transitionLogWriter = this.getParserService().getTransitionLogWriter(csvFileWriter);
					ParseEvaluationObserverImpl observer = new ParseEvaluationObserverImpl(transitionLogWriter);
					observer.setWriter(csvFileWriter);
					if (this.errorLabels != null && this.errorLabels.size() > 0)
						observer.setErrorLabels(errorLabels);
					parserEvaluator.addObserver(observer);
				}

				Reader templateReader = null;
				String configPath = "talismane.core.analyse.template";
				if (config.hasPath(configPath)) {
					templateReader = new BufferedReader(new InputStreamReader(this.getFileFromConfig(configPath)));
				} else {
					templateReader = new BufferedReader(new InputStreamReader(getInputStreamFromResource(parserTemplateName)));
				}

				File freemarkerFile = new File(this.getOutDir(), this.getBaseName() + "_output.txt");
				freemarkerFile.delete();
				freemarkerFile.createNewFile();
				Writer freemakerFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(freemarkerFile, false), csvEncoding));
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
	 */
	@Override
	public synchronized ParseComparator getParseComparator() {
		try {
			if (parseComparator == null) {
				parseComparator = this.getParserService().getParseComparator();
				File fscoreFile = new File(this.getOutDir(), this.getBaseName() + ".fscores.csv");

				ParseEvaluationFScoreCalculator parseFScoreCalculator = new ParseEvaluationFScoreCalculator(fscoreFile);
				parseFScoreCalculator.setLabeledEvaluation(this.labeledEvaluation);

				parseComparator.addObserver(parseFScoreCalculator);

				if (includeDistanceFScores) {
					File csvFile = new File(this.getOutDir(), this.getBaseName() + ".distances.csv");
					csvFile.delete();
					csvFile.createNewFile();
					Writer csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false), csvEncoding));
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
	 */
	@Override
	public synchronized TokeniserEvaluator getTokeniserEvaluator() {
		if (tokeniserEvaluator == null) {
			tokeniserEvaluator = this.getTokeniserService().getTokeniserEvaluator(this.getTokeniser());

			List<TokenEvaluationObserver> observers = this.getTokenEvaluationObservers();
			for (TokenEvaluationObserver observer : observers)
				tokeniserEvaluator.addObserver(observer);

			tokeniserEvaluator.setSentenceCount(maxSentenceCount);
		}
		return tokeniserEvaluator;
	}

	/**
	 * Get a sentence detector evaluator if command=evaluate and
	 * endModule=sentenceDetector.
	 */
	@Override
	public synchronized SentenceDetectorEvaluator getSentenceDetectorEvaluator() {
		if (sentenceDetectorEvaluator == null) {
			sentenceDetectorEvaluator = this.getSentenceDetectorService().getEvaluator(this.getSentenceDetector());
		}
		return sentenceDetectorEvaluator;
	}

	private List<TokenEvaluationObserver> getTokenEvaluationObservers() {
		try {
			List<TokenEvaluationObserver> observers = new ArrayListNoNulls<TokenEvaluationObserver>();
			Writer errorFileWriter = null;
			File errorFile = new File(this.getOutDir(), this.getBaseName() + ".errorList.txt");
			errorFile.delete();
			errorFile.createNewFile();
			errorFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(errorFile, false), "UTF8"));

			Writer csvErrorFileWriter = null;
			File csvErrorFile = new File(this.getOutDir(), this.getBaseName() + ".errors.csv");
			csvErrorFile.delete();
			csvErrorFile.createNewFile();
			csvErrorFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvErrorFile, false), csvEncoding));

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
			corpusFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(corpusFile, false), "UTF8"));

			TokenEvaluationCorpusWriter corpusWriter = new TokenEvaluationCorpusWriter(corpusFileWriter);
			observers.add(corpusWriter);

			Reader templateReader = null;
			String configPath = "talismane.core.analyse.template";
			if (config.hasPath(configPath)) {
				templateReader = new BufferedReader(new InputStreamReader(this.getFileFromConfig(configPath)));
			} else {
				templateReader = new BufferedReader(new InputStreamReader(getInputStreamFromResource(tokeniserTemplateName)));
			}

			File freemarkerFile = new File(this.getOutDir(), this.getBaseName() + "_output.txt");
			freemarkerFile.delete();
			freemarkerFile.createNewFile();
			Writer freemakerFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(freemarkerFile, false), "UTF8"));
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
	 */
	@Override
	public synchronized TokenComparator getTokenComparator() {
		try {
			if (tokenComparator == null) {
				TokeniserPatternManager tokeniserPatternManager = null;

				Tokeniser tokeniser = this.getTokeniser();
				if (tokeniser instanceof PatternTokeniser) {
					PatternTokeniser patternTokeniser = (PatternTokeniser) tokeniser;
					tokeniserPatternManager = patternTokeniser.getTokeniserPatternManager();
				}
				tokenComparator = this.getTokeniserService().getTokenComparator(this.getTokenCorpusReader(), this.getTokenEvaluationCorpusReader(),
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
	 */
	@Override
	public synchronized PosTaggerEvaluator getPosTaggerEvaluator() {
		try {
			if (posTaggerEvaluator == null) {
				posTaggerEvaluator = this.getPosTaggerService().getPosTaggerEvaluator(this.getPosTagger());

				if (startModule.equals(Module.Tokeniser)) {
					posTaggerEvaluator.setTokeniser(this.getTokeniser());
				}

				if (outputGuesses) {
					File csvFile = new File(this.getOutDir(), this.getBaseName() + "_sentences.csv");
					csvFile.delete();
					csvFile.createNewFile();
					Writer csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false), csvEncoding));

					int guessCount = 1;
					if (outputGuessCount > 0)
						guessCount = outputGuessCount;
					else if (posTaggerEvaluator.getPosTagger() instanceof NonDeterministicPosTagger)
						guessCount = ((NonDeterministicPosTagger) posTaggerEvaluator.getPosTagger()).getBeamWidth();

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
				String configPath = "talismane.core.analyse.template";
				if (config.hasPath(configPath)) {
					templateReader = new BufferedReader(new InputStreamReader(this.getFileFromConfig(configPath)));
				} else {
					templateReader = new BufferedReader(new InputStreamReader(getInputStreamFromResource(posTaggerTemplateName)));
				}

				File freemarkerFile = new File(this.getOutDir(), this.getBaseName() + "_output.txt");
				freemarkerFile.delete();
				freemarkerFile.createNewFile();
				Writer freemakerFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(freemarkerFile, false), "UTF8"));
				PosTaggerGuessTemplateWriter templateWriter = new PosTaggerGuessTemplateWriter(freemakerFileWriter, templateReader);
				posTaggerEvaluator.addObserver(templateWriter);

				if (includeLexiconCoverage) {
					File lexiconCoverageFile = new File(this.getOutDir(), this.getBaseName() + ".lexiconCoverage.csv");
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
	 */
	@Override
	public synchronized PosTagComparator getPosTagComparator() {
		try {
			if (posTagComparator == null) {
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
	 */
	@Override
	public synchronized String getBaseName() {
		if (baseName == null) {
			baseName = "Talismane";
			String path = null;
			if (config.hasPath("talismane.core.outFile"))
				path = config.getString("talismane.core.outFile");
			else if (config.hasPath("talismane.core.inFile"))
				path = config.getString("talismane.core.inFile");
			else if (config.hasPath("talismane.core.analyse.languageModel") && module.equals(Talismane.Module.LanguageDetector)
					|| endModule.equals(Talismane.Module.LanguageDetector))
				path = config.getString("talismane.core.analyse.languageModel");
			else if (config.hasPath("talismane.core.analyse.sentenceModel") && module.equals(Talismane.Module.SentenceDetector)
					|| endModule.equals(Talismane.Module.SentenceDetector))
				path = config.getString("talismane.core.analyse.sentenceModel");
			else if (config.hasPath("talismane.core.analyse.tokeniserModel")
					&& (module.equals(Talismane.Module.Tokeniser) || endModule.equals(Talismane.Module.Tokeniser)))
				path = config.getString("talismane.core.analyse.tokeniserModel");
			else if (config.hasPath("talismane.core.analyse.posTaggerModel")
					&& (module.equals(Talismane.Module.PosTagger) || endModule.equals(Talismane.Module.PosTagger)))
				path = config.getString("talismane.core.analyse.posTaggerModel");
			else if (config.hasPath("talismane.core.analyse.parserModel")
					&& (module.equals(Talismane.Module.Parser) || endModule.equals(Talismane.Module.Parser)))
				path = config.getString("talismane.core.analyse.parserModel");

			if (path != null) {
				path = path.replace('\\', '/');

				if (path.indexOf('.') > 0)
					baseName = path.substring(path.lastIndexOf('/') + 1, path.lastIndexOf('.'));
				else
					baseName = path.substring(path.lastIndexOf('/') + 1);
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

	public void setPosTaggerFeatureService(PosTaggerFeatureService posTaggerFeatureService) {
		this.posTaggerFeatureService = posTaggerFeatureService;
	}

	public ParserFeatureService getParserFeatureService() {
		return parserFeatureService;
	}

	public void setParserFeatureService(ParserFeatureService parserFeatureService) {
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

	public void setSentenceDetectorService(SentenceDetectorService sentenceDetectorService) {
		this.sentenceDetectorService = sentenceDetectorService;
	}

	public LanguageDetectorService getLanguageDetectorService() {
		return languageDetectorService;
	}

	public void setLanguageDetectorService(LanguageDetectorService languageDetectorService) {
		this.languageDetectorService = languageDetectorService;
	}

	public SentenceDetectorFeatureService getSentenceDetectorFeatureService() {
		return sentenceDetectorFeatureService;
	}

	public void setSentenceDetectorFeatureService(SentenceDetectorFeatureService sentenceDetectorFeatureService) {
		this.sentenceDetectorFeatureService = sentenceDetectorFeatureService;
	}

	public MachineLearningService getMachineLearningService() {
		return machineLearningService;
	}

	public void setMachineLearningService(MachineLearningService machineLearningService) {
		this.machineLearningService = machineLearningService;
	}

	public TokeniserPatternService getTokeniserPatternService() {
		return tokeniserPatternService;
	}

	public void setTokeniserPatternService(TokeniserPatternService tokeniserPatternService) {
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
	}

	/**
	 * Does this instance of Talismane need a sentence detector to perform the
	 * requested processing.
	 */
	@Override
	public boolean needsSentenceDetector() {
		return startModule.compareTo(Module.SentenceDetector) <= 0 && endModule.compareTo(Module.SentenceDetector) >= 0;
	}

	/**
	 * Does this instance of Talismane need a tokeniser to perform the requested
	 * processing.
	 */
	@Override
	public boolean needsTokeniser() {
		return startModule.compareTo(Module.Tokeniser) <= 0 && endModule.compareTo(Module.Tokeniser) >= 0;
	}

	/**
	 * Does this instance of Talismane need a pos tagger to perform the
	 * requested processing.
	 */
	@Override
	public boolean needsPosTagger() {
		return startModule.compareTo(Module.PosTagger) <= 0 && endModule.compareTo(Module.PosTagger) >= 0;
	}

	/**
	 * Does this instance of Talismane need a parser to perform the requested
	 * processing.
	 */
	@Override
	public boolean needsParser() {
		return startModule.compareTo(Module.Parser) <= 0 && endModule.compareTo(Module.Parser) >= 0;
	}

	private static InputStream getInputStreamFromResource(String resource) {
		String path = "output/" + resource;
		InputStream inputStream = Talismane.class.getResourceAsStream(path);
		if (inputStream == null)
			throw new TalismaneException("Resource not found in classpath: " + path);
		return inputStream;
	}

	@Override
	public boolean isLogStats() {
		return logStats;
	}

	@Override
	public synchronized LanguageDetectorAnnotatedCorpusReader getLanguageCorpusReader() {
		try {
			if (languageCorpusReader == null) {
				String configPath = "talismane.core.train.languageDetector.languageCorpusMap";
				InputStream languageCorpusMapFile = this.getFileFromConfig(configPath);
				try (Scanner languageCorpusMapScanner = new Scanner(
						new BufferedReader(new InputStreamReader(languageCorpusMapFile, this.getInputCharset().name())))) {

					Map<Locale, Reader> languageMap = new HashMap<Locale, Reader>();
					while (languageCorpusMapScanner.hasNextLine()) {
						String line = languageCorpusMapScanner.nextLine();
						String[] parts = line.split("\t");
						Locale locale = Locale.forLanguageTag(parts[0]);
						String corpusPath = parts[1];
						InputStream corpusFile = new FileInputStream(new File(corpusPath));
						Reader corpusReader = new BufferedReader(new InputStreamReader(corpusFile, this.getInputCharset().name()));
						languageMap.put(locale, corpusReader);
					}
					languageCorpusReader = this.getLanguageDetectorService().getDefaultReader(languageMap);
				}
			}
			this.setCorpusReaderAttributes(languageCorpusReader);
			return languageCorpusReader;
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public synchronized SentenceDetectorAnnotatedCorpusReader getSentenceCorpusReader() {
		if (sentenceCorpusReader == null) {
			sentenceCorpusReader = this.getSentenceDetectorService().getDefaultReader(this.getReader());
		}
		this.setCorpusReaderAttributes(sentenceCorpusReader);
		return sentenceCorpusReader;
	}

	@Override
	public void setSentenceCorpusReader(SentenceDetectorAnnotatedCorpusReader sentenceCorpusReader) {
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
	 * the minimum block size, in characters, to process by the sentence
	 * detector. Filters are applied to a concatenation of the previous block,
	 * the current block, and the next block prior to sentence detection, in
	 * order to ensure that a filter which crosses block boundaries is correctly
	 * applied. It is not legal to have a filter which matches text greater than
	 * a block size, since this could result in a filter which stops analysis
	 * but doesn't start it again correctly, or vice versa. Block size can be
	 * increased if really big filters are really required. Default is 1000.
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
		if (this.getMode() == Mode.normal) {
			talismane = (this.getTalismaneServiceInternal()).getTalismane(this);
		} else if (this.getMode() == Mode.server) {
			talismane = this.getTalismaneServiceInternal().getTalismaneServer(this);
		} else {
			throw new TalismaneException("Unknown mode: " + this.getMode().name());
		}
		return talismane;
	}

	@Override
	public Map<String, List<String>> getDescriptors() {
		if (this.descriptors == null) {
			descriptors = new HashMap<String, List<String>>();
		}
		return descriptors;
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
	public PatternTokeniserType getPatternTokeniserType() {
		return patternTokeniserType;
	}

	/**
	 * The port where the Talismane Server should listen.
	 */
	@Override
	public int getPort() {
		return port;
	}

	/**
	 * The first sentence index to process.
	 */
	@Override
	public int getStartSentence() {
		return startSentence;
	}

	@Override
	public void preloadResources() {
		if (!preloaded) {
			LOG.info("Loading shared resources...");

			if (preloadLexicon) {
				LOG.info("Loading lexicon");
				// ping the lexicon to load it
				talismaneSession.getMergedLexicon();
				talismaneSession.getDiacriticizer();
			}

			// ping the models to load them
			if (this.needsSentenceDetector()) {
				LOG.info("Loading sentence detector");
				if (this.getSentenceDetector() == null) {
					throw new TalismaneException("Sentence detector not provided.");
				}
			}
			if (this.needsTokeniser()) {
				LOG.info("Loading tokeniser");
				if (this.getTokeniser() == null) {
					throw new TalismaneException("Tokeniser not provided.");
				}
			}
			if (this.needsPosTagger()) {
				LOG.info("Loading pos tagger");
				if (this.getPosTagger() == null) {
					throw new TalismaneException("Pos-tagger not provided.");
				}
			}
			if (this.needsParser()) {
				LOG.info("Loading parser");
				if (this.getParser() == null) {
					throw new TalismaneException("Parser not provided.");
				}
			}
			preloaded = true;
		}
	}

	private InputStream getFileFromConfig(String configPath) throws IOException {
		String path = config.getString(configPath);
		return this.getFile(configPath, path);
	}

	private InputStream getFile(String configPath, String path) throws IOException {
		FileObject fileObject = VFSWrapper.getInstance().getFileObject(path);

		if (!fileObject.exists()) {
			LOG.error(configPath + " file not found: " + path);
			throw new FileNotFoundException(configPath + " file not found: " + path);
		}

		return fileObject.getContent().getInputStream();
	}

	private List<FileObject> getFileObjects(String path) throws FileSystemException {
		List<FileObject> fileObjects = new ArrayList<>();
		FileObject fileObject = VFSWrapper.getInstance().getFileObject(path);
		this.getFileObjects(fileObject, fileObjects);
		return fileObjects;
	}

	private void getFileObjects(FileObject fileObject, List<FileObject> fileObjects) throws FileSystemException {
		if (fileObject.isFolder()) {
			for (FileObject child : fileObject.getChildren()) {
				this.getFileObjects(child, fileObjects);
			}
		} else {
			fileObjects.add(fileObject);
		}
	}

	private static final class VFSWrapper {
		private final String baseDir = System.getProperty("user.dir");
		private final Set<String> prefixes;
		private final Set<String> localPrefixes = new HashSet<>(Arrays.asList("file", "zip", "jar", "tar", "tgz", "tbz2", "gz", "bz2", "ear", "war"));
		private final FileSystemManager fsManager;

		private static VFSWrapper instance;

		public static VFSWrapper getInstance() throws FileSystemException {
			if (instance == null)
				instance = new VFSWrapper();
			return instance;
		}

		private VFSWrapper() throws FileSystemException {
			fsManager = VFS.getManager();
			prefixes = new HashSet<>(Arrays.asList(fsManager.getSchemes()));
		}

		public FileObject getFileObject(String path) throws FileSystemException {
			LOG.debug("Getting " + path);
			FileSystemManager fsManager = VFS.getManager();

			// make the path absolute if required, based on the working
			// directory
			String prefix = "";
			if (path.contains("://")) {
				prefix = path.substring(0, path.indexOf("://"));
				if (!prefixes.contains(prefix))
					prefix = "";
			}

			boolean makeAbsolute = prefix.length() == 0 || localPrefixes.contains(prefix);

			if (makeAbsolute) {
				if (prefix.length() > 0)
					prefix += "://";
				String fileSystemPath = path.substring(prefix.length());
				String pathNoSuffix = fileSystemPath;
				int exclamationPos = fileSystemPath.indexOf('!');

				if (exclamationPos >= 0)
					pathNoSuffix = pathNoSuffix.substring(0, exclamationPos);

				File file = new File(pathNoSuffix);
				if (!file.isAbsolute()) {
					path = prefix + baseDir + "/" + fileSystemPath;
					LOG.debug("Changed path to " + path);
				}
			}

			FileObject fileObject = fsManager.resolveFile(path);
			return fileObject;
		}
	}

	@Override
	public Locale getLocale() {
		return locale;
	}

	@Override
	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	@Override
	public synchronized LanguageDetectorProcessor getLanguageDetectorProcessor() throws IOException {
		if (this.languageDetectorProcessor == null) {
			this.languageDetectorProcessor = this.getLanguageDetectorService().getDefaultLanguageDetectorProcessor(this.getWriter());
		}
		return this.languageDetectorProcessor;
	}

	@Override
	public void setLanguageDetectorProcessor(LanguageDetectorProcessor languageDetectorProcessor) {
		this.languageDetectorProcessor = languageDetectorProcessor;
	}

	@Override
	public void addTokenFilter(TokenFilter tokenFilter) {
		this.additionalTokenFilters.add(tokenFilter);
		if (this.tokenFilters != null)
			this.tokenFilters.add(tokenFilter);
	}

	@Override
	public void prependTokenFilter(TokenFilter tokenFilter) {
		this.prependedTokenFilters.add(0, tokenFilter);
		if (this.tokenFilters != null)
			this.tokenFilters.add(0, tokenFilter);
	}

	@Override
	public Config getConfig() {
		return config;
	}

	@Override
	public String getPosTaggerModelFilePath() {
		return config.getString("talismane.core.analyse.posTaggerModel");
	}

	@Override
	public String getTokeniserModelFilePath() {
		return config.getString("talismane.core.analyse.tokeniserModel");
	}

	@Override
	public String getSentenceModelFilePath() {
		return config.getString("talismane.core.analyse.sentenceModel");
	}

	@Override
	public String getParserModelFilePath() {
		return config.getString("talismane.core.analyse.parserModel");
	}

	@Override
	public String getLanguageModelFilePath() {
		return config.getString("talismane.core.analyse.languageModel");
	}

}
