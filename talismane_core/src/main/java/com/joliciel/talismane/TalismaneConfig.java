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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.Talismane.BuiltInTemplate;
import com.joliciel.talismane.Talismane.Command;
import com.joliciel.talismane.Talismane.Mode;
import com.joliciel.talismane.Talismane.Module;
import com.joliciel.talismane.filters.DuplicateWhiteSpaceFilter;
import com.joliciel.talismane.filters.MarkerFilterType;
import com.joliciel.talismane.filters.NewlineEndOfSentenceMarker;
import com.joliciel.talismane.filters.NewlineSpaceMarker;
import com.joliciel.talismane.filters.OtherWhiteSpaceFilter;
import com.joliciel.talismane.filters.RegexMarkerFilter;
import com.joliciel.talismane.filters.TextMarkerFilter;
import com.joliciel.talismane.filters.TextMarkerFilterFactory;
import com.joliciel.talismane.languageDetector.DefaultLanguageDetectorProcessor;
import com.joliciel.talismane.languageDetector.LanguageDetector;
import com.joliciel.talismane.languageDetector.LanguageDetectorAnnotatedCorpusReader;
import com.joliciel.talismane.languageDetector.LanguageDetectorEventStream;
import com.joliciel.talismane.languageDetector.LanguageDetectorFeature;
import com.joliciel.talismane.languageDetector.LanguageDetectorFeatureFactory;
import com.joliciel.talismane.languageDetector.LanguageDetectorProcessor;
import com.joliciel.talismane.languageDetector.TextPerLineCorpusReader;
import com.joliciel.talismane.lexicon.LexicalEntryReader;
import com.joliciel.talismane.lexicon.RegexLexicalEntryReader;
import com.joliciel.talismane.machineLearning.ClassificationEventStream;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.MachineLearningModel;
import com.joliciel.talismane.machineLearning.MachineLearningModelFactory;
import com.joliciel.talismane.parser.ParseEventStream;
import com.joliciel.talismane.parser.ParserAnnotatedCorpusReader;
import com.joliciel.talismane.parser.ParserRegexBasedCorpusReader;
import com.joliciel.talismane.parser.Parsers;
import com.joliciel.talismane.parser.features.ParseConfigurationFeature;
import com.joliciel.talismane.parser.features.ParserFeatureParser;
import com.joliciel.talismane.parser.features.ParserRule;
import com.joliciel.talismane.posTagger.PosTaggers;
import com.joliciel.talismane.posTagger.features.PosTaggerFeature;
import com.joliciel.talismane.posTagger.features.PosTaggerFeatureParser;
import com.joliciel.talismane.posTagger.features.PosTaggerRule;
import com.joliciel.talismane.sentenceDetector.SentenceDetector;
import com.joliciel.talismane.tokeniser.Tokeniser;
import com.joliciel.talismane.tokeniser.filters.TokenFilter;
import com.joliciel.talismane.utils.ArrayListNoNulls;
import com.joliciel.talismane.utils.ConfigUtils;
import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.io.DirectoryReader;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * A class for loading, storing and translating configuration information to be
 * passed to Talismane when processing.<br/>
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
public class TalismaneConfig {
	private static final Logger LOG = LoggerFactory.getLogger(TalismaneConfig.class);

	private Config config;

	private Command command;
	private Mode mode;

	private Module startModule;
	private Module endModule;
	private Module module;

	private ParserAnnotatedCorpusReader parserCorpusReader;
	private ParserAnnotatedCorpusReader parserEvaluationCorpusReader;
	private LanguageDetectorAnnotatedCorpusReader languageCorpusReader;

	private LanguageDetectorProcessor languageDetectorProcessor;
	private ClassificationModel languageModel;
	private ClassificationModel tokeniserModel;
	private ClassificationModel posTaggerModel;
	private ClassificationModel parserModel;

	private boolean processByDefault;
	private int maxSentenceCount;
	private int startSentence;
	private int beamWidth;
	private boolean propagateBeam;
	private Charset inputCharset;
	private Charset outputCharset;

	private int posTaggerBeamWidth;
	private int parserBeamWidth;
	private boolean propagateTokeniserBeam;

	private char endBlockCharacter;
	private int maxParseAnalysisTime;

	private Reader reader;
	private Reader evaluationReader;

	private boolean logStats;
	private boolean dynamiseFeatures;

	private List<PosTaggerRule> posTaggerRules;
	private List<ParserRule> parserRules;
	private List<TextMarkerFilter> textMarkerFilters;
	private List<TokenFilter> tokenFilters;
	private List<TokenFilter> additionalTokenFilters = new ArrayListNoNulls<TokenFilter>();
	private List<TokenFilter> prependedTokenFilters = new ArrayListNoNulls<TokenFilter>();
	private boolean predictTransitions;

	private MarkerFilterType newlineMarker;
	private int blockSize;

	private int crossValidationSize;
	private int includeIndex;
	private int excludeIndex;

	private Set<LanguageDetectorFeature<?>> languageFeatures;
	private Set<PosTaggerFeature<?>> posTaggerFeatures;
	private Set<ParseConfigurationFeature<?>> parserFeatures;
	private ClassificationEventStream classificationEventStream;

	private boolean parserCorpusReaderFiltersAdded = false;
	private boolean parserCorpusReaderDecorated = false;

	// server parameters
	private int port;

	// training parameters
	private List<Integer> perceptronObservationPoints;

	private Map<String, List<String>> descriptors;

	private boolean preloadLexicon;
	private boolean preloaded = false;

	private Locale locale;

	// various static maps for ensuring we don't load the same large resource
	// multiple times if multiple talismane configurations share the same
	// resource
	private static final Map<String, ClassificationModel> modelMap = new HashMap<>();

	private final TalismaneSession session;

	public TalismaneConfig(Config config, TalismaneSession talismaneSession) throws IOException, ClassNotFoundException {
		this.session = talismaneSession;
		this.loadParameters(config);
	}

	private synchronized void loadParameters(Config config) throws IOException, ClassNotFoundException {
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
				module = Talismane.Module.sentenceDetector;
			else if (moduleString.equalsIgnoreCase("tokenise") || moduleString.equalsIgnoreCase("tokeniser"))
				module = Talismane.Module.tokeniser;
			else if (moduleString.equalsIgnoreCase("postag") || moduleString.equalsIgnoreCase("posTagger"))
				module = Talismane.Module.posTagger;
			else if (moduleString.equalsIgnoreCase("parse") || moduleString.equalsIgnoreCase("parser"))
				module = Talismane.Module.parser;
			else if (moduleString.equalsIgnoreCase("language") || moduleString.equalsIgnoreCase("languageDetector"))
				module = Talismane.Module.languageDetector;
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
			startModule = Talismane.Module.sentenceDetector;
		else if (startModuleString.equalsIgnoreCase("tokenise") || startModuleString.equalsIgnoreCase("tokeniser"))
			startModule = Talismane.Module.tokeniser;
		else if (startModuleString.equalsIgnoreCase("postag") || startModuleString.equalsIgnoreCase("posTagger"))
			startModule = Talismane.Module.posTagger;
		else if (startModuleString.equalsIgnoreCase("parse") || startModuleString.equalsIgnoreCase("parser"))
			startModule = Talismane.Module.parser;
		else
			throw new TalismaneException("Unknown startModule: " + startModuleString);

		if (module != null)
			startModule = module;

		String endModuleString = analyseConfig.getString("endModule");

		if (endModuleString.equalsIgnoreCase("sentence") || endModuleString.equalsIgnoreCase("sentenceDetector"))
			endModule = Talismane.Module.sentenceDetector;
		else if (endModuleString.equalsIgnoreCase("tokenise") || endModuleString.equalsIgnoreCase("tokeniser"))
			endModule = Talismane.Module.tokeniser;
		else if (endModuleString.equalsIgnoreCase("postag") || endModuleString.equalsIgnoreCase("posTagger"))
			endModule = Talismane.Module.posTagger;
		else if (endModuleString.equalsIgnoreCase("parse") || endModuleString.equalsIgnoreCase("parser"))
			endModule = Talismane.Module.parser;
		else
			throw new TalismaneException("Unknown endModule: " + endModuleString);

		if (module != null)
			endModule = module;

		newlineMarker = MarkerFilterType.valueOf(analyseConfig.getString("newline"));

		processByDefault = analyseConfig.getBoolean("processByDefault");

		endBlockCharacter = analyseConfig.getString("endBlockCharCode").charAt(0);

		blockSize = analyseConfig.getInt("blockSize");

		maxSentenceCount = analyseConfig.getInt("sentenceCount");
		startSentence = analyseConfig.getInt("startSentence");

		BuiltInTemplate builtInTemplate = BuiltInTemplate.valueOf(analyseConfig.getString("builtInTemplate"));
		switch (builtInTemplate) {
		case standard:
			// don't change defaults
			break;
		case with_location:
			break;
		case with_prob:
			break;
		case with_comments:
			break;
		default:
			throw new TalismaneException("Unknown builtInTemplate for tokeniser: " + builtInTemplate.name());
		}

		String outputDivider = analyseConfig.getString("outputDivider");
		if (outputDivider.equals("NEWLINE"))
			outputDivider = "\n";
		session.setOutputDivider(outputDivider);

		beamWidth = analyseConfig.getInt("beamWidth");
		posTaggerBeamWidth = analyseConfig.getInt("posTaggerBeamWidth");
		parserBeamWidth = analyseConfig.getInt("parserBeamWidth");
		propagateBeam = analyseConfig.getBoolean("propagateBeam");
		propagateTokeniserBeam = analyseConfig.getBoolean("propagateTokeniserBeam");

		dynamiseFeatures = analyseConfig.getBoolean("dynamiseFeatures");

		maxParseAnalysisTime = analyseConfig.getInt("parser.maxAnalysisTime");

		crossValidationSize = evaluateConfig.getInt("crossValidation.foldCount");
		includeIndex = evaluateConfig.getInt("crossValidation.includeIndex");
		excludeIndex = evaluateConfig.getInt("crossValidation.excludeIndex");

		predictTransitions = processConfig.getBoolean("predictTransitions");

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
			if (module == Module.languageDetector) {
				if (!config.hasPath("talismane.core.analyse.languageModel"))
					throw new TalismaneException("talismane.core.analyse.languageModel is required when training a language model");
				if (!config.hasPath("talismane.core.train.languageDetector.features"))
					throw new TalismaneException("talismane.core.train.languageDetector.features is required when training a language model");
				if (!config.hasPath("talismane.core.train.languageDetector.languageCorpusMap"))
					throw new TalismaneException("talismane.core.train.languageDetector.languageCorpusMap is required when training a language model");
			} else if (module == Module.sentenceDetector) {
				if (!config.hasPath("talismane.core.analyse.sentenceModel"))
					throw new TalismaneException("talismane.core.analyse.sentenceModel is required when training a sentence model");
				if (!config.hasPath("talismane.core.train.sentenceDetector.features"))
					throw new TalismaneException("talismane.core.train.sentenceDetector.features is required when training a sentence model");
			} else if (module == Module.tokeniser) {
				if (!config.hasPath("talismane.core.analyse.tokeniserModel"))
					throw new TalismaneException("talismane.core.analyse.tokeniserModel is required when training a tokeniser model");
				if (!config.hasPath("talismane.core.train.tokeniser.features"))
					throw new TalismaneException("talismane.core.train.tokeniser.features is required when training a tokeniser model");
			} else if (module == Module.posTagger) {
				if (!config.hasPath("talismane.core.analyse.posTaggerModel"))
					throw new TalismaneException("talismane.core.analyse.posTaggerModel is required when training a posTagger model");
				if (!config.hasPath("talismane.core.train.posTagger.features"))
					throw new TalismaneException("talismane.core.train.posTagger.features is required when training a posTagger model");
			} else if (module == Module.parser) {
				this.predictTransitions = true;

				if (!config.hasPath("talismane.core.analyse.parserModel"))
					throw new TalismaneException("talismane.core.analyse.parserModel is required when training a parser model");
				if (!config.hasPath("talismane.core.train.parser.features"))
					throw new TalismaneException("talismane.core.train.parser.features is required when training a parser model");
			}
		}

		if (posTaggerBeamWidth < 0)
			posTaggerBeamWidth = beamWidth;
		if (parserBeamWidth < 0)
			parserBeamWidth = beamWidth;

	}

	/**
	 * The actual command to run by Talismane.
	 */

	public Command getCommand() {
		return command;
	}

	public void setCommand(Command command) {
		this.command = command;
	}

	/**
	 * If the command required a start module (e.g. analyse), the start module
	 * for this command.
	 */

	public Module getStartModule() {
		return startModule;
	}

	public void setStartModule(Module startModule) {
		this.startModule = startModule;
	}

	/**
	 * If the command requires an end module (e.g. analyse), the end module for
	 * this command.
	 */

	public Module getEndModule() {
		return endModule;
	}

	public void setEndModule(Module endModule) {
		this.endModule = endModule;
	}

	/**
	 * For commands which only affect a single module (e.g. evaluate), the
	 * module for this command.
	 */

	public Module getModule() {
		return module;
	}

	public void setModule(Module module) {
		this.module = module;
	}

	/**
	 * When analysing, should the raw text be processed by default, or should we
	 * wait until a text marker filter tells us to start processing. Default is
	 * true.
	 */

	public boolean isProcessByDefault() {
		return processByDefault;
	}

	public void setProcessByDefault(boolean processByDefault) {
		this.processByDefault = processByDefault;
	}

	/**
	 * For the "process" command, the maximum number of sentences to process. If
	 * &lt;=0, all sentences will be processed. Default is 0 (all).
	 */

	public int getMaxSentenceCount() {
		return maxSentenceCount;
	}

	public void setMaxSentenceCount(int maxSentenceCount) {
		this.maxSentenceCount = maxSentenceCount;
	}

	/**
	 * The charset that is used to interpret the input stream.
	 */

	public Charset getInputCharset() {
		return inputCharset;
	}

	public void setInputCharset(Charset inputCharset) {
		this.inputCharset = inputCharset;
	}

	/**
	 * The charset that is used to write to the output stream.
	 */

	public Charset getOutputCharset() {
		return outputCharset;
	}

	public void setOutputCharset(Charset outputCharset) {
		this.outputCharset = outputCharset;
	}

	/**
	 * A character (typically non-printing) which will mark a stop in the input
	 * stream and set-off analysis. The default value is the form-feed character
	 * (code=12).
	 */

	public char getEndBlockCharacter() {
		return endBlockCharacter;
	}

	public void setEndBlockCharacter(char endBlockCharacter) {
		this.endBlockCharacter = endBlockCharacter;
	}

	/**
	 * The beam width for beam-search analysis. Default is 1. Increasing this
	 * value will increase analysis time in a linear fashion, but will typically
	 * improve results.
	 */

	public int getBeamWidth() {
		return beamWidth;
	}

	public void setBeamWidth(int beamWidth) {
		this.beamWidth = beamWidth;
	}

	/**
	 * If true, the full beam of analyses produced as output by a given module
	 * will be used as input for the next module. If false, only the single best
	 * analysis will be used as input for the next module.
	 */

	public boolean isPropagateBeam() {
		return propagateBeam;
	}

	public void setPropagateBeam(boolean propagateBeam) {
		this.propagateBeam = propagateBeam;
	}

	/**
	 * The reader to be used to read the data for this analysis.
	 */

	public Reader getReader() {
		try {
			if (this.reader == null) {
				String configPath = "talismane.core.inFile";
				if (config.hasPath(configPath)) {
					InputStream inFile = ConfigUtils.getFileFromConfig(config, configPath);
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

	public Reader getEvaluationReader() throws IOException {
		if (this.evaluationReader == null) {
			String configPath = "talismane.core.evaluate.evaluationFile";

			InputStream inFile = ConfigUtils.getFileFromConfig(config, configPath);
			this.evaluationReader = new BufferedReader(new InputStreamReader(inFile, this.getInputCharset()));
		}
		return evaluationReader;
	}

	public synchronized List<PosTaggerRule> getPosTaggerRules() throws IOException {
		if (posTaggerRules == null) {
			posTaggerRules = new ArrayListNoNulls<PosTaggerRule>();
			PosTaggerFeatureParser featureParser = new PosTaggerFeatureParser(this.session);

			String configPath = "talismane.core.analyse.posTaggerRules";
			List<String> textFilterPaths = config.getStringList(configPath);
			for (String path : textFilterPaths) {
				LOG.debug("From: " + path);
				InputStream textFilterFile = ConfigUtils.getFile(config, configPath, path);
				try (Scanner scanner = new Scanner(textFilterFile, this.getInputCharset().name())) {
					List<String> ruleDescriptors = new ArrayListNoNulls<String>();
					while (scanner.hasNextLine()) {
						String ruleDescriptor = scanner.nextLine();
						if (ruleDescriptor.length() > 0) {
							ruleDescriptors.add(ruleDescriptor);
							LOG.trace(ruleDescriptor);
						}
					}
					List<PosTaggerRule> rules = featureParser.getRules(ruleDescriptors);
					posTaggerRules.addAll(rules);
				}
			}
		}
		return posTaggerRules;
	}

	public synchronized List<ParserRule> getParserRules() throws IOException {
		if (parserRules == null) {
			parserRules = new ArrayListNoNulls<ParserRule>();

			ParserFeatureParser parserFeatureParser = new ParserFeatureParser(this.session, this.dynamiseFeatures);

			String configPath = "talismane.core.analyse.parserRules";
			List<String> textFilterPaths = config.getStringList(configPath);
			for (String path : textFilterPaths) {
				LOG.debug("From: " + path);
				InputStream textFilterFile = ConfigUtils.getFile(config, configPath, path);
				try (Scanner scanner = new Scanner(textFilterFile, this.getInputCharset().name())) {
					List<String> ruleDescriptors = new ArrayListNoNulls<String>();
					while (scanner.hasNextLine()) {
						String ruleDescriptor = scanner.nextLine();
						if (ruleDescriptor.length() > 0) {
							ruleDescriptors.add(ruleDescriptor);
							LOG.trace(ruleDescriptor);
						}
					}
					List<ParserRule> rules = parserFeatureParser.getRules(ruleDescriptors);
					parserRules.addAll(rules);
				}
			}
		}
		return parserRules;
	}

	public synchronized List<TextMarkerFilter> getTextMarkerFilters() throws IOException {
		if (textMarkerFilters == null) {
			textMarkerFilters = new ArrayListNoNulls<TextMarkerFilter>();

			// insert sentence breaks at end of block
			this.addTextMarkerFilter(new RegexMarkerFilter(Arrays.asList(new MarkerFilterType[] { MarkerFilterType.SKIP, MarkerFilterType.SENTENCE_BREAK }),
					"" + endBlockCharacter, 0, blockSize));

			// handle newline as requested
			if (newlineMarker.equals(MarkerFilterType.SENTENCE_BREAK))
				this.addTextMarkerFilter(new NewlineEndOfSentenceMarker(blockSize));
			else if (newlineMarker.equals(MarkerFilterType.SPACE))
				this.addTextMarkerFilter(new NewlineSpaceMarker(blockSize));

			// get rid of duplicate white-space always
			this.addTextMarkerFilter(new DuplicateWhiteSpaceFilter(blockSize));

			// replace tabs with white space
			this.addTextMarkerFilter(new OtherWhiteSpaceFilter(blockSize));

			TextMarkerFilterFactory factory = new TextMarkerFilterFactory();

			String configPath = "talismane.core.analyse.textFilters";
			List<String> textFilterPaths = config.getStringList(configPath);
			for (String path : textFilterPaths) {
				LOG.debug("From: " + path);
				InputStream textFilterFile = ConfigUtils.getFile(config, configPath, path);
				try (Scanner scanner = new Scanner(textFilterFile, this.getInputCharset().name())) {
					while (scanner.hasNextLine()) {
						String descriptor = scanner.nextLine();
						LOG.debug(descriptor);
						if (descriptor.length() > 0 && !descriptor.startsWith("#")) {
							TextMarkerFilter textMarkerFilter = factory.getTextMarkerFilter(descriptor, blockSize);
							this.addTextMarkerFilter(textMarkerFilter);
						}
					}
				}
			}
		}
		return textMarkerFilters;
	}

	public void setTextMarkerFilters(Scanner scanner) {
		textMarkerFilters = new ArrayListNoNulls<TextMarkerFilter>();
		TextMarkerFilterFactory factory = new TextMarkerFilterFactory();
		while (scanner.hasNextLine()) {
			String descriptor = scanner.nextLine();
			LOG.debug(descriptor);
			if (descriptor.length() > 0 && !descriptor.startsWith("#")) {
				TextMarkerFilter textMarkerFilter = factory.getTextMarkerFilter(descriptor, blockSize);
				this.addTextMarkerFilter(textMarkerFilter);
			}
		}
	}

	public void setTextMarkerFilters(List<TextMarkerFilter> textMarkerFilters) {
		this.textMarkerFilters = textMarkerFilters;
	}

	public void addTextMarkerFilter(TextMarkerFilter textMarkerFilter) {
		this.textMarkerFilters.add(textMarkerFilter);
	}

	/**
	 * The language detector to use for analysis.
	 */

	public LanguageDetector getLanguageDetector() {
		try {
			ClassificationModel languageModel = this.getLanguageModel();
			LanguageDetector languageDetector = new LanguageDetector(languageModel);

			return languageDetector;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	synchronized ClassificationModel getLanguageModel() throws IOException {
		if (languageModel == null) {
			LOG.debug("Getting languageDetector model");

			String configPath = "talismane.core.analyse.languageModel";
			String modelFilePath = config.getString(configPath);
			languageModel = modelMap.get(modelFilePath);
			if (languageModel == null) {
				InputStream languageModelFile = ConfigUtils.getFileFromConfig(config, configPath);
				MachineLearningModelFactory factory = new MachineLearningModelFactory();
				languageModel = factory.getClassificationModel(new ZipInputStream(languageModelFile));
				modelMap.put(modelFilePath, languageModel);
			}
		}
		return languageModel;
	}

	synchronized ClassificationModel getTokeniserModel() throws IOException {
		if (tokeniserModel == null) {
			LOG.debug("Getting tokeniser model");

			String configPath = "talismane.core.analyse.tokeniserModel";
			String modelFilePath = config.getString(configPath);
			tokeniserModel = modelMap.get(modelFilePath);
			if (tokeniserModel == null) {
				InputStream tokeniserModelFile = ConfigUtils.getFileFromConfig(config, configPath);
				MachineLearningModelFactory factory = new MachineLearningModelFactory();
				tokeniserModel = factory.getClassificationModel(new ZipInputStream(tokeniserModelFile));
				modelMap.put(modelFilePath, tokeniserModel);
			}
		}
		return tokeniserModel;
	}

	synchronized ClassificationModel getPosTaggerModel() throws IOException {
		if (posTaggerModel == null) {
			LOG.debug("Getting posTagger model");

			String configPath = "talismane.core.analyse.posTaggerModel";
			String modelFilePath = config.getString(configPath);
			posTaggerModel = modelMap.get(modelFilePath);
			if (posTaggerModel == null) {
				InputStream posTaggerModelFile = ConfigUtils.getFileFromConfig(config, configPath);
				MachineLearningModelFactory factory = new MachineLearningModelFactory();
				posTaggerModel = factory.getClassificationModel(new ZipInputStream(posTaggerModelFile));
				modelMap.put(modelFilePath, posTaggerModel);
			}
		}
		return posTaggerModel;
	}

	synchronized ClassificationModel getParserModel() throws IOException {
		if (parserModel == null) {
			LOG.debug("Getting parser model");

			String configPath = "talismane.core.analyse.parserModel";
			String modelFilePath = config.getString(configPath);
			parserModel = modelMap.get(modelFilePath);
			if (parserModel == null) {
				InputStream posTaggerModelFile = ConfigUtils.getFileFromConfig(config, configPath);
				MachineLearningModelFactory factory = new MachineLearningModelFactory();
				parserModel = factory.getClassificationModel(new ZipInputStream(posTaggerModelFile));
				modelMap.put(modelFilePath, parserModel);
			}
		}
		return parserModel;
	}

	public synchronized Set<LanguageDetectorFeature<?>> getLanguageDetectorFeatures() throws IOException {
		if (languageFeatures == null) {
			String configPath = "talismane.core.train.languageDetector.features";
			InputStream languageFeatureFile = ConfigUtils.getFileFromConfig(config, configPath);
			try (Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(languageFeatureFile, this.getInputCharset())))) {
				List<String> featureDescriptors = new ArrayListNoNulls<String>();
				while (scanner.hasNextLine()) {
					String descriptor = scanner.nextLine();
					featureDescriptors.add(descriptor);
					LOG.debug(descriptor);
				}

				LanguageDetectorFeatureFactory factory = new LanguageDetectorFeatureFactory();
				languageFeatures = factory.getFeatureSet(featureDescriptors);

				this.getDescriptors().put(MachineLearningModel.FEATURE_DESCRIPTOR_KEY, featureDescriptors);
			}
		}
		return languageFeatures;
	}

	public synchronized Set<PosTaggerFeature<?>> getPosTaggerFeatures() throws IOException {
		if (posTaggerFeatures == null) {
			PosTaggerFeatureParser featureParser = new PosTaggerFeatureParser(session);
			String configPath = "talismane.core.train.posTagger.features";
			InputStream posTaggerFeatureFile = ConfigUtils.getFileFromConfig(config, configPath);
			try (Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(posTaggerFeatureFile, this.getInputCharset())))) {
				List<String> featureDescriptors = new ArrayListNoNulls<String>();
				while (scanner.hasNextLine()) {
					String descriptor = scanner.nextLine();
					featureDescriptors.add(descriptor);
					LOG.debug(descriptor);
				}

				posTaggerFeatures = featureParser.getFeatureSet(featureDescriptors);

				this.getDescriptors().put(MachineLearningModel.FEATURE_DESCRIPTOR_KEY, featureDescriptors);
			}
		}
		return posTaggerFeatures;
	}

	public synchronized ClassificationEventStream getClassificationEventStream() throws IOException {
		if (this.classificationEventStream == null) {
			switch (this.getModule()) {
			case languageDetector:
				classificationEventStream = new LanguageDetectorEventStream(this.getLanguageCorpusReader(), this.getLanguageDetectorFeatures());
				break;
			case parser:
				classificationEventStream = new ParseEventStream(this.getParserCorpusReader(), this.getParserFeatures());
				break;
			default:
				throw new TalismaneException("Unsupported module: " + this.getModule());
			}
		}
		return classificationEventStream;
	}

	public synchronized Set<ParseConfigurationFeature<?>> getParserFeatures() throws IOException {
		if (parserFeatures == null) {
			ParserFeatureParser parserFeatureParser = new ParserFeatureParser(this.session, this.dynamiseFeatures);

			String configPath = "talismane.core.train.parser.features";
			InputStream parserFeatureFile = ConfigUtils.getFileFromConfig(config, configPath);
			try (Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(parserFeatureFile, this.getInputCharset())))) {
				List<String> featureDescriptors = new ArrayListNoNulls<String>();
				while (scanner.hasNextLine()) {
					String descriptor = scanner.nextLine();
					featureDescriptors.add(descriptor);
					LOG.debug(descriptor);
				}

				parserFeatures = parserFeatureParser.getFeatures(featureDescriptors);

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

	public int getMaxParseAnalysisTime() {
		return maxParseAnalysisTime;
	}

	public void setMaxParseAnalysisTime(int maxParseAnalysisTime) {
		this.maxParseAnalysisTime = maxParseAnalysisTime;
	}

	/**
	 * A parser corpus reader to read a corpus pre-annotated in dependencies.
	 * 
	 * @throws IOException
	 */

	public synchronized ParserAnnotatedCorpusReader getParserCorpusReader() throws IOException {
		if (parserCorpusReader == null) {
			String configPath = "talismane.core.train.parser.readerRegex";
			config.getString(configPath);

			ParserRegexBasedCorpusReader parserRegexCorpusReader = new ParserRegexBasedCorpusReader(this.getReader(), this.config, this.session);

			parserRegexCorpusReader.setPredictTransitions(predictTransitions);

			configPath = "talismane.core.process.corpusLexicalEntryRegex";
			if (config.hasPath(configPath)) {
				InputStream corpusLexicalEntryRegexFile = ConfigUtils.getFileFromConfig(config, configPath);

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

	public synchronized ParserAnnotatedCorpusReader getParserEvaluationCorpusReader() throws IOException {
		if (parserEvaluationCorpusReader == null) {
			String configPath = "talismane.core.evaluate.parser.readerRegex";
			config.getString(configPath);

			ParserRegexBasedCorpusReader parserRegexCorpusReader = new ParserRegexBasedCorpusReader(this.getEvaluationReader(), this.config, this.session);
			parserRegexCorpusReader.setPredictTransitions(predictTransitions);

			this.parserEvaluationCorpusReader = parserRegexCorpusReader;
		}
		this.addParserCorpusReaderFilters(parserEvaluationCorpusReader);
		return parserEvaluationCorpusReader;
	}

	public void setParserEvaluationCorpusReader(ParserAnnotatedCorpusReader parserEvaluationCorpusReader) {
		this.parserEvaluationCorpusReader = parserEvaluationCorpusReader;
	}

	synchronized void addParserCorpusReaderFilters(ParserAnnotatedCorpusReader corpusReader) throws IOException {
		if (!parserCorpusReaderFiltersAdded) {
			if (this.getCommand() != Command.train) {
				if (this.getStartModule().equals(Module.tokeniser)) {
					this.getPosTaggerModel();
					this.getParserModel();
				} else if (this.getStartModule().equals(Module.posTagger)) {
					this.getPosTaggerModel();
					this.getParserModel();
				} else {
					this.getParserModel();
					this.getParserModel();
				}
			} // models exist already?

			parserCorpusReaderFiltersAdded = true;
		}
	}

	public void setParserCorpusReader(ParserAnnotatedCorpusReader parserCorpusReader) {
		this.parserCorpusReader = parserCorpusReader;
	}

	/**
	 * Does this instance of Talismane need a sentence detector to perform the
	 * requested processing.
	 */

	public boolean needsSentenceDetector() {
		return startModule.compareTo(Module.sentenceDetector) <= 0 && endModule.compareTo(Module.sentenceDetector) >= 0;
	}

	/**
	 * Does this instance of Talismane need a tokeniser to perform the requested
	 * processing.
	 */

	public boolean needsTokeniser() {
		return startModule.compareTo(Module.tokeniser) <= 0 && endModule.compareTo(Module.tokeniser) >= 0;
	}

	/**
	 * Does this instance of Talismane need a pos tagger to perform the
	 * requested processing.
	 */

	public boolean needsPosTagger() {
		return startModule.compareTo(Module.posTagger) <= 0 && endModule.compareTo(Module.posTagger) >= 0;
	}

	/**
	 * Does this instance of Talismane need a parser to perform the requested
	 * processing.
	 */

	public boolean needsParser() {
		return startModule.compareTo(Module.parser) <= 0 && endModule.compareTo(Module.parser) >= 0;
	}

	public boolean isLogStats() {
		return logStats;
	}

	public synchronized LanguageDetectorAnnotatedCorpusReader getLanguageCorpusReader() {
		try {
			if (languageCorpusReader == null) {
				String configPath = "talismane.core.train.languageDetector.languageCorpusMap";
				InputStream languageCorpusMapFile = ConfigUtils.getFileFromConfig(config, configPath);
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
					languageCorpusReader = new TextPerLineCorpusReader(languageMap);
				}
			}
			this.setCorpusReaderAttributes(languageCorpusReader);
			return languageCorpusReader;
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	public int getPosTaggerBeamWidth() {
		return posTaggerBeamWidth;
	}

	public int getParserBeamWidth() {
		return parserBeamWidth;
	}

	public boolean isPropagateTokeniserBeam() {
		return propagateTokeniserBeam;
	}

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

	public int getBlockSize() {
		return blockSize;
	}

	public void setBlockSize(int blockSize) {
		this.blockSize = blockSize;
	}

	/**
	 * Should the parser corpus reader predict the transitions or not?
	 */

	public boolean isPredictTransitions() {
		return predictTransitions;
	}

	public void setPredictTransitions(boolean predictTransitions) {
		this.predictTransitions = predictTransitions;
	}

	public Mode getMode() {
		return mode;
	}

	public void setMode(Mode mode) {
		this.mode = mode;
	}

	public Talismane getTalismane() {
		Talismane talismane = null;
		if (this.getMode() == Mode.normal) {
			talismane = new Talismane(this, session);
		} else if (this.getMode() == Mode.server) {
			talismane = new TalismaneServer(this, session);
		} else {
			throw new TalismaneException("Unknown mode: " + this.getMode().name());
		}
		return talismane;
	}

	public Map<String, List<String>> getDescriptors() {
		if (this.descriptors == null) {
			descriptors = new HashMap<String, List<String>>();
		}
		return descriptors;
	}

	public List<Integer> getPerceptronObservationPoints() {
		return perceptronObservationPoints;
	}

	/**
	 * The port where the Talismane Server should listen.
	 */

	public int getPort() {
		return port;
	}

	/**
	 * The first sentence index to process.
	 */

	public int getStartSentence() {
		return startSentence;
	}

	/**
	 * Preload any lexicons or models required for this processing.
	 * 
	 * @throws IOException
	 */
	public void preloadResources() throws IOException {
		if (!preloaded) {
			LOG.info("Loading shared resources...");

			if (preloadLexicon) {
				LOG.info("Loading lexicon");
				// ping the lexicon to load it
				session.getMergedLexicon();
				session.getDiacriticizer();
			}

			// ping the models to load them
			if (this.needsSentenceDetector()) {
				LOG.info("Loading sentence detector");
				SentenceDetector.getInstance(session);
			}
			if (this.needsTokeniser()) {
				LOG.info("Loading tokeniser");
				Tokeniser.getInstance(session);
			}
			if (this.needsPosTagger()) {
				LOG.info("Loading pos tagger");
				PosTaggers.getPosTagger(session);
			}
			if (this.needsParser()) {
				LOG.info("Loading parser");
				Parsers.getParser(session);
			}
			preloaded = true;
		}
	}

	/**
	 * The locale indicated for this configuration.
	 */
	public Locale getLocale() {
		return locale;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	public synchronized LanguageDetectorProcessor getLanguageDetectorProcessor() throws IOException {
		if (this.languageDetectorProcessor == null) {
			this.languageDetectorProcessor = new DefaultLanguageDetectorProcessor(session.getWriter());
		}
		return this.languageDetectorProcessor;
	}

	public void setLanguageDetectorProcessor(LanguageDetectorProcessor languageDetectorProcessor) {
		this.languageDetectorProcessor = languageDetectorProcessor;
	}

	/**
	 * Add a token filter in addition to those loaded from the model or config.
	 * Token filters added here will always be run after the ones already
	 * loaded.
	 */
	public void addTokenFilter(TokenFilter tokenFilter) {
		this.additionalTokenFilters.add(tokenFilter);
		if (this.tokenFilters != null)
			this.tokenFilters.add(tokenFilter);
	}

	/**
	 * Prepend a token filter to the list of those loaded from the model or
	 * config. This will always be run before the others.
	 */
	public void prependTokenFilter(TokenFilter tokenFilter) {
		this.prependedTokenFilters.add(0, tokenFilter);
		if (this.tokenFilters != null)
			this.tokenFilters.add(0, tokenFilter);
	}

	/**
	 * Get the configuration object used to build this TalismaneConfig.
	 */

	public Config getConfig() {
		return config;
	}

	public String getPosTaggerModelFilePath() {
		return config.getString("talismane.core.analyse.posTaggerModel");
	}

	public String getTokeniserModelFilePath() {
		return config.getString("talismane.core.analyse.tokeniserModel");
	}

	public String getParserModelFilePath() {
		return config.getString("talismane.core.analyse.parserModel");
	}

	public String getLanguageModelFilePath() {
		return config.getString("talismane.core.analyse.languageModel");
	}

	public String getParserReaderRegex() {
		return config.getString("talismane.core.train.parser.readerRegex");
	}

	public String getParserEvluationReaderRegex() {
		return config.getString("talismane.core.evaluate.parser.readerRegex");
	}

	public TalismaneSession getTalismaneSession() {
		return session;
	}
}
