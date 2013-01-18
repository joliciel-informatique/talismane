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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.Talismane.Command;
import com.joliciel.talismane.Talismane.Module;
import com.joliciel.talismane.filters.FilterService;
import com.joliciel.talismane.filters.MarkerFilterType;
import com.joliciel.talismane.filters.TextMarkerFilter;
import com.joliciel.talismane.machineLearning.AnalysisObserver;
import com.joliciel.talismane.machineLearning.MachineLearningModel;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.output.FreemarkerTemplateWriter;
import com.joliciel.talismane.parser.ParseComparator;
import com.joliciel.talismane.parser.ParseConfigurationProcessor;
import com.joliciel.talismane.parser.ParseEvaluationFScoreCalculator;
import com.joliciel.talismane.parser.ParseEvaluationGuessTemplateWriter;
import com.joliciel.talismane.parser.ParseEvaluationSentenceWriter;
import com.joliciel.talismane.parser.Parser;
import com.joliciel.talismane.parser.ParserAnnotatedCorpusReader;
import com.joliciel.talismane.parser.ParserEvaluator;
import com.joliciel.talismane.parser.ParserFScoreCalculatorByDistance;
import com.joliciel.talismane.parser.ParserRegexBasedCorpusReader;
import com.joliciel.talismane.parser.ParserService;
import com.joliciel.talismane.parser.Transition;
import com.joliciel.talismane.parser.TransitionSystem;
import com.joliciel.talismane.parser.features.ParserFeatureService;
import com.joliciel.talismane.parser.features.ParserRule;
import com.joliciel.talismane.posTagger.NonDeterministicPosTagger;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagAnnotatedCorpusReader;
import com.joliciel.talismane.posTagger.PosTagEvaluationFScoreCalculator;
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
import com.joliciel.talismane.sentenceDetector.SentenceDetector;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorOutcome;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorService;
import com.joliciel.talismane.sentenceDetector.SentenceProcessor;
import com.joliciel.talismane.sentenceDetector.features.SentenceDetectorFeature;
import com.joliciel.talismane.sentenceDetector.features.SentenceDetectorFeatureService;
import com.joliciel.talismane.tokeniser.TokenRegexBasedCorpusReader;
import com.joliciel.talismane.tokeniser.TokenSequenceProcessor;
import com.joliciel.talismane.tokeniser.Tokeniser;
import com.joliciel.talismane.tokeniser.TokeniserOutcome;
import com.joliciel.talismane.tokeniser.TokeniserService;
import com.joliciel.talismane.tokeniser.features.TokenFeatureService;
import com.joliciel.talismane.tokeniser.features.TokeniserContextFeature;
import com.joliciel.talismane.tokeniser.filters.TokenFilter;
import com.joliciel.talismane.tokeniser.filters.TokenFilterService;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;
import com.joliciel.talismane.tokeniser.patterns.TokeniserPatternManager;
import com.joliciel.talismane.tokeniser.patterns.TokeniserPatternService;
import com.joliciel.talismane.utils.LogUtils;

/**
 * An abstract base class for loading, storing and translating configuration information to be passed to Talismane when processing.<br/>
 * Implementing classes must include language-specific implementation resources.<br/>
 * The processing must go from a given start module to a given end module in sequence, where the modules available are:
 * Sentence detector, Tokeniser, Pos tagger, Parser.<br/>
 * There is a default input format for each start module,
 * which can be over-ridden by providing a regex for processing lines of input. The default format is:<br/>
 * <li>Sentence detector: newlines indicate sentence breaks.</li>
 * <li>Tokeniser: expect exactly one sentence per newline.</li>
 * <li>Pos tagger: {@link  com.joliciel.talismane.tokeniser.TokenRegexBasedCorpusReader#DEFAULT_REGEX default regex} </li>
 * <li>Parser: {@link  com.joliciel.talismane.posTagger.PosTagRegexBasedCorpusReader#DEFAULT_REGEX default regex} </li>
 * @author Assaf Urieli
 *
 */
public abstract class TalismaneConfig implements LanguageSpecificImplementation {
	private static final Log LOG = LogFactory.getLog(TalismaneConfig.class);
	private Command command = null;
	
	private Module startModule = Module.SentenceDetector;
	private Module endModule = Module.Parser;
	private Module module = null;
	
	private SentenceDetector sentenceDetector;
	private Tokeniser tokeniser;
	private PosTagger posTagger;
	private Parser parser;
	
	private ParserEvaluator parserEvaluator;
	private PosTaggerEvaluator posTaggerEvaluator;
	private ParseComparator parseComparator;

	private TokenRegexBasedCorpusReader tokenCorpusReader = null;
	private PosTagAnnotatedCorpusReader posTagCorpusReader = null;
	private ParserRegexBasedCorpusReader parserCorpusReader = null;
	private ParserRegexBasedCorpusReader parserEvaluationCorpusReader = null;

	private SentenceProcessor sentenceProcessor;
	private TokenSequenceProcessor tokenSequenceProcessor;
	private PosTagSequenceProcessor posTagSequenceProcessor;
	private ParseConfigurationProcessor parseConfigurationProcessor;
	
	MachineLearningModel<TokeniserOutcome> tokeniserModel = null;
	private MachineLearningModel<PosTag> posTaggerModel = null;
	private MachineLearningModel<Transition> parserModel = null;

	private boolean processByDefault = true;
	private int maxSentenceCount = 0;
	private int beamWidth = 1;
	private boolean propagateBeam = true;
	private boolean includeDetails = false;	
	private Charset inputCharset = null;
	private Charset outputCharset = null;
	
	private char endBlockCharacter = '\f';
	private String inputRegex;
	private String inputPatternFilePath = null;
	private int maxParseAnalysisTime = 60;
	
	private Reader reader = null;
	private Writer writer = null;
	private Reader evaluationReader = null;
	
	private String inFilePath = null;
	private String outFilePath = null;
	private String outDirPath = null;
	private String parserModelFilePath = null;
	private String posTaggerModelFilePath = null;
	private String tokeniserModelFilePath = null;
	private String sentenceModelFilePath = null;
	private String textFiltersPath = null;
	private String tokenFiltersPath = null;
	private String templatePath = null;
	private String evaluationFilePath = null;
	
	private String posTaggerFeaturePath = null;

	private String sentenceTemplateName = "sentence_template.ftl";
	private String tokeniserTemplateName = "tokeniser_template.ftl";
	private String posTaggerTemplateName = "posTagger_template.ftl";
	private String parserTemplateName = "parser_conll_template.ftl";
	
	private String fileName = null;
	private boolean logPerformance = false;
	private boolean logStats = false;
	private File outDir = null;
	private String baseName = null;
	private String suffix = "";
	private boolean outputGuesses = false;
	private int outputGuessCount = 0;
	
	private List<PosTaggerRule> posTaggerRules = null;
	private String posTaggerRuleFilePath = null;
	private List<ParserRule> parserRules = null;
	private String parserRuleFilePath = null;
	private List<TextMarkerFilter> textMarkerFilters = null;
	private List<TokenFilter> tokenFilters = null;
	private boolean includeDistanceFScores = false;
	
	private MarkerFilterType newlineMarker = MarkerFilterType.SENTENCE_BREAK;
	
	private boolean parserCorpusReaderFiltersAdded = false;
	private boolean posTagCorpusReaderFiltersAdded = false;
	
	private TalismaneServiceLocator talismaneServiceLocator = null;
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
	
	public TalismaneConfig(String[] args) throws Exception {
		TalismaneSession.setImplementation(this);
		talismaneServiceLocator = TalismaneServiceLocator.getInstance();
		Map<String,String> argMap = convertArgs(args);
		this.loadParameters(argMap);
	}
	
	public TalismaneConfig(Map<String,String> args) throws Exception {
		TalismaneSession.setImplementation(this);

		talismaneServiceLocator = TalismaneServiceLocator.getInstance();
		this.loadParameters(args);
	}
	
	public static Map<String, String> convertArgs(String[] args) {
		Map<String,String> argMap = new HashMap<String, String>();
		for (String arg : args) {
			int equalsPos = arg.indexOf('=');
			String argName = arg.substring(0, equalsPos);
			String argValue = arg.substring(equalsPos+1);
			argMap.put(argName, argValue);
		}
		return argMap;
	}
	
	void loadParameters(Map<String,String> args) throws Exception {
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
		
		String encoding = null;
		String inputEncoding = null;
		String outputEncoding = null;
		String builtInTemplate = null;
		
		String posTagSetPath = null;

		String transitionSystemStr = null;
		
		for (Entry<String,String> arg : args.entrySet()) {
			String argName = arg.getKey();
			String argValue = arg.getValue();
			if (argName.equals("command")) {
				String commandString = argValue;
				if (commandString.equals("analyze"))
					commandString = "analyse";
				
				command = Command.valueOf(commandString);
			} else if (argName.equals("module")) {
				if (argValue.equalsIgnoreCase("sentence")||argValue.equalsIgnoreCase("sentenceDetector"))
					module = Talismane.Module.SentenceDetector;
				else if (argValue.equalsIgnoreCase("tokenise")||argValue.equalsIgnoreCase("tokeniser"))
					module = Talismane.Module.Tokeniser;
				else if (argValue.equalsIgnoreCase("postag")||argValue.equalsIgnoreCase("posTagger"))
					module = Talismane.Module.PosTagger;
				else if (argValue.equalsIgnoreCase("parse")||argValue.equalsIgnoreCase("parser"))
					module = Talismane.Module.Parser;
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
			else if (argName.equals("outFile")) 
				outFilePath = argValue;
			else if (argName.equals("outDir")) 
				outDirPath = argValue;
			else if (argName.equals("template")) 
				templatePath = argValue;
			else if (argName.equals("builtInTemplate")) {
				builtInTemplate = argValue;
			}
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
			else if (argName.equals("posTaggerRules"))
				posTaggerRuleFilePath = argValue;
			else if (argName.equals("parserRules"))
				parserRuleFilePath = argValue;
			else if (argName.equals("posTagSet"))
				posTagSetPath = argValue;
			else if (argName.equals("textFilters"))
				textFiltersPath = argValue;
			else if (argName.equals("tokenFilters"))
				tokenFiltersPath = argValue;
			else if (argName.equals("logPerformance"))
				logPerformance = argValue.equalsIgnoreCase("true");
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
			else if (argName.equals("transitionSystem"))
				transitionSystemStr = argValue;
			else if (argName.equals("sentenceCount"))
				maxSentenceCount = Integer.parseInt(argValue);
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
			else if (argName.equals("evaluationFile"))
				evaluationFilePath = argValue;
			else if (argName.equals("posTaggerFeatures"))
				posTaggerFeaturePath = argValue;
			else {
				System.out.println("Unknown argument: " + argName);
				throw new RuntimeException("Unknown argument: " + argName);
			}
		}
		
		if (command==null)
			throw new TalismaneException("No command provided.");
		
		if (command.equals(Command.evaluate)) {
			if (outDirPath.length()==0)
				throw new RuntimeException("Missing argument: outdir");
		}
		
		if (command.equals(Command.evaluate)||command.equals(Command.process)) {
			if (module!=null) {
				startModule = module;
				endModule = module;
			} else {
				module = endModule;
			}
		}

		if (builtInTemplate!=null && builtInTemplate.equalsIgnoreCase("with_location")) {
			tokeniserTemplateName = "tokeniser_template_with_location.ftl";
			posTaggerTemplateName = "posTagger_template_with_location.ftl";
			parserTemplateName = "parser_conll_template_with_location.ftl";
		}
		
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
			File posTagSetFile = new File(posTagSetPath);
			Scanner posTagSetScanner = new Scanner(posTagSetFile,"UTF-8");
			PosTagSet posTagSet = this.getPosTaggerService().getPosTagSet(posTagSetScanner);
			TalismaneSession.setPosTagSet(posTagSet);
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
			TalismaneSession.setTransitionSystem(transitionSystem);
		}
	}

	/**
	 * The actual command to run by Talismane.
	 * @return
	 */
	public Command getCommand() {
		return command;
	}
	public void setCommand(Command command) {
		this.command = command;
	}

	/**
	 * If the command required a start module (e.g. analyse), the start module for this command.
	 * Default is {@link com.joliciel.talismane.Talismane.Module#SentenceDetector}.
	 * @return
	 */
	public Module getStartModule() {
		return startModule;
	}
	public void setStartModule(Module startModule) {
		this.startModule = startModule;
	}

	/**
	 * If the command requires an end module (e.g. analyse), the end module for this command.
	 * Default is {@link com.joliciel.talismane.Talismane.Module#Parser}.
	 * @return
	 */
	public Module getEndModule() {
		return endModule;
	}
	public void setEndModule(Module endModule) {
		this.endModule = endModule;
	}

	/**
	 * For commands which only affect a single module (e.g. evaluate), the module for this command.
	 * @return
	 */
	public Module getModule() {
		return module;
	}
	public void setModule(Module module) {
		this.module = module;
	}

	/**
	 * When analysing, should the raw text be processed by default, or should we wait until a text
	 * marker filter tells us to start processing. Default is true.
	 * @return
	 */
	public boolean isProcessByDefault() {
		return processByDefault;
	}
	public void setProcessByDefault(boolean processByDefault) {
		this.processByDefault = processByDefault;
	}

	/**
	 * For the "process" command, the maximum number of sentences to process. If <=0, all sentences
	 * will be processed. Default is 0 (all).
	 * @return
	 */
	public int getMaxSentenceCount() {
		return maxSentenceCount;
	}
	public void setMaxSentenceCount(int maxSentenceCount) {
		this.maxSentenceCount = maxSentenceCount;
	}

	/**
	 * The charset that is used to interpret the input stream.
	 * @return
	 */
	public Charset getInputCharset() {
		return inputCharset;
	}
	public void setInputCharset(Charset inputCharset) {
		this.inputCharset = inputCharset;
	}

	/**
	 * The charset that is used to write to the output stream.
	 * @return
	 */
	public Charset getOutputCharset() {
		return outputCharset;
	}
	public void setOutputCharset(Charset outputCharset) {
		this.outputCharset = outputCharset;
	}

	/**
	 * A character (typically non-printing) which will mark a stop in the input stream and set-off analysis.
	 * The default value is the form-feed character (code=12).
	 * @return
	 */
	public char getEndBlockCharacter() {
		return endBlockCharacter;
	}
	public void setEndBlockCharacter(char endBlockCharacter) {
		this.endBlockCharacter = endBlockCharacter;
	}

	/**
	 * The beam width for beam-search analysis. Default is 1.
	 * Increasing this value will increase analysis time in a linear fashion, but will typically improve results.
	 * @return
	 */
	public int getBeamWidth() {
		return beamWidth;
	}
	public void setBeamWidth(int beamWidth) {
		this.beamWidth = beamWidth;
	}

	/**
	 * If true, the full beam of analyses produced as output by a given module will be used as input for the next module.
	 * If false, only the single best analysis will be used as input for the next module.
	 * @return
	 */
	public boolean isPropagateBeam() {
		return propagateBeam;
	}
	public void setPropagateBeam(boolean propagateBeam) {
		this.propagateBeam = propagateBeam;
	}

	/**
	 * If true, a generates a very detailed analysis on how Talismane obtained the results it displays.
	 * @return
	 */
	public boolean isIncludeDetails() {
		return includeDetails;
	}
	public void setIncludeDetails(boolean includeDetails) {
		this.includeDetails = includeDetails;
	}

	/**
	 * The reader to be used to read the data for this analysis.
	 * @return
	 */
	public Reader getReader() {
		if (this.reader==null) {
			if (inFilePath!=null) {
				try {
					File inFile = new File(inFilePath);
					this.reader = new BufferedReader(new InputStreamReader(new FileInputStream(inFile), this.getInputCharset()));
				} catch (FileNotFoundException fnfe) {
					LogUtils.logError(LOG, fnfe);
					throw new RuntimeException(fnfe);
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
	public Reader getEvaluationReader() {
		if (this.evaluationReader==null) {
			try {
				File inFile = new File(evaluationFilePath);
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
	public Writer getWriter() {
		try {
			if (writer==null) {
				if (outFilePath!=null) {
					if (outFilePath.lastIndexOf("/")>=0) {
						String outFileDirPath = outFilePath.substring(0, outFilePath.lastIndexOf("/"));
						File outFileDir = new File(outFileDirPath);
						outFileDir.mkdirs();
					}
					File outFile = new File(outFilePath);
					outFile.delete();
					outFile.createNewFile();
				
					writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), this.getOutputCharset()));
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
	public String getFileName() {
		return fileName;
	}

	/**
	 * Whether or not we should log performance for this run.
	 * @return
	 */
	public boolean isLogPerformance() {
		return logPerformance;
	}
	public void setLogPerformance(boolean logPerformance) {
		this.logPerformance = logPerformance;
	}

	/**
	 * The directory to which we write any output files.
	 * @return
	 */
	public File getOutDir() {
		if (outDirPath!=null) {
			outDir = new File(outDirPath);
			outDir.mkdirs();
		} else if (outFilePath!=null) {
			if (outFilePath.lastIndexOf("/")>=0) {
				String outFileDirPath = outFilePath.substring(0, outFilePath.lastIndexOf("/"));
				outDir = new File(outFileDirPath);
				outDir.mkdirs();
			}
		}
		return outDir;
	}

	/**
	 * The rules to apply when running the pos-tagger.
	 * @return
	 */
	public List<PosTaggerRule> getPosTaggerRules() {
		try {
			if (posTaggerRules == null) {
				posTaggerRules = new ArrayList<PosTaggerRule>();
				for (int i=0; i<=1; i++) {
					Scanner rulesScanner = null;
					if (i==0) {
						InputStream defaultRulesStream = this.getDefaultPosTaggerRulesFromStream();
						if (defaultRulesStream!=null)
							rulesScanner = new Scanner(defaultRulesStream);
					} else {
						if (posTaggerRuleFilePath!=null && posTaggerRuleFilePath.length()>0) {
							File posTaggerRuleFile = new File(posTaggerRuleFilePath);
							rulesScanner = new Scanner(posTaggerRuleFile);
						}
					}
					
					if (rulesScanner!=null) {
						List<String> ruleDescriptors = new ArrayList<String>();
						while (rulesScanner.hasNextLine()) {
							String ruleDescriptor = rulesScanner.nextLine();
							if (ruleDescriptor.length()>0) {
								ruleDescriptors.add(ruleDescriptor);
								LOG.debug(ruleDescriptor);
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
							InputStream defaultRulesStream = this.getDefaultParserRulesFromStream();
							if (defaultRulesStream!=null)
								rulesScanner = new Scanner(defaultRulesStream);
						} else {
							if (parserRuleFilePath!=null && parserRuleFilePath.length()>0) {
								File parserRuleFile = new File(parserRuleFilePath);
								rulesScanner = new Scanner(parserRuleFile);
							}
						}
						
						if (rulesScanner!=null) {
							List<String> ruleDescriptors = new ArrayList<String>();
							while (rulesScanner.hasNextLine()) {
								String ruleDescriptor = rulesScanner.nextLine();
								if (ruleDescriptor.length()>0) {
									ruleDescriptors.add(ruleDescriptor);
									LOG.debug(ruleDescriptor);
								}
							}
							List<ParserRule> rules = this.getParserFeatureService().getRules(ruleDescriptors);
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
	public String getInputRegex() {
		try {
			if (inputRegex==null && inputPatternFilePath!=null && inputPatternFilePath.length()>0) {
				Scanner inputPatternScanner = null;
				File inputPatternFile = new File(inputPatternFilePath);
				inputPatternScanner = new Scanner(inputPatternFile);
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

	/**
	 * Text marker filters are applied to raw text segments extracted from the stream, 3 segments at a time.
	 * This means that if a particular marker crosses segment borders, it is handled correctly.
	 * @return
	 */
	public List<TextMarkerFilter> getTextMarkerFilters() {
		try {
			if (textMarkerFilters==null) {
				textMarkerFilters = new ArrayList<TextMarkerFilter>();
				
				// insert sentence breaks at end of block
				this.addTextMarkerFilter(this.getFilterService().getRegexMarkerFilter(new MarkerFilterType[] { MarkerFilterType.SENTENCE_BREAK }, "" + endBlockCharacter));
				
				// handle newline as requested
				if (newlineMarker.equals(MarkerFilterType.SENTENCE_BREAK))
					this.addTextMarkerFilter(this.getFilterService().getNewlineEndOfSentenceMarker());
				else if (newlineMarker.equals(MarkerFilterType.SPACE))
					this.addTextMarkerFilter(this.getFilterService().getNewlineSpaceMarker());
				
				// get rid of duplicate white-space always
				this.addTextMarkerFilter(this.getFilterService().getDuplicateWhiteSpaceFilter());
	
				for (int i=0; i<=1; i++) {
					LOG.debug("Text marker filters");
					Scanner textFilterScanner = null;
					if (i==0) {
						if (textFiltersPath!=null && textFiltersPath.length()>0) {
							LOG.debug("From: " + textFiltersPath);
							File textFilterFile = new File(textFiltersPath);
							textFilterScanner = new Scanner(textFilterFile);
						}
					} else {
						InputStream stream = this.getDefaultTextMarkerFiltersFromStream();
						if (stream!=null) {
							LOG.debug("From default");
							textFilterScanner = new Scanner(stream);
						}
					}
					if (textFilterScanner!=null) {
						while (textFilterScanner.hasNextLine()) {
							String descriptor = textFilterScanner.nextLine();
							LOG.debug(descriptor);
							if (descriptor.length()>0 && !descriptor.startsWith("#")) {
								TextMarkerFilter textMarkerFilter = this.getFilterService().getTextMarkerFilter(descriptor);
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

	public void setTextMarkerFilters(List<TextMarkerFilter> textMarkerFilters) {
		this.textMarkerFilters = textMarkerFilters;
	}

	public void addTextMarkerFilter(TextMarkerFilter textMarkerFilter) {
		this.textMarkerFilters.add(textMarkerFilter);
	}
	
	/**
	 * TokenFilters to be applied during analysis.
	 * @return
	 */
	public List<TokenFilter> getTokenFilters() {
		try {
			if (tokenFilters==null) {
				tokenFilters = new ArrayList<TokenFilter>();
				for (int i=0; i<=1; i++) {
					LOG.debug("Token filters");
					Scanner tokenFilterScanner = null;
					if (i==0) {
						if (tokenFiltersPath!=null && tokenFiltersPath.length()>0) {
							LOG.debug("From: " + tokenFiltersPath);
							File tokenFilterFile = new File(tokenFiltersPath);
							tokenFilterScanner = new Scanner(tokenFilterFile);
						}
					} else {
						InputStream stream = this.getDefaultTokenFiltersFromStream();
						if (stream!=null) {
							LOG.debug("From default");
							tokenFilterScanner = new Scanner(stream);
						}
					}
					if (tokenFilterScanner!=null) {
						while (tokenFilterScanner.hasNextLine()) {
							String descriptor = tokenFilterScanner.nextLine();
							LOG.debug(descriptor);
							if (descriptor.length()>0 && !descriptor.startsWith("#")) {
								TokenFilter tokenFilter = this.getTokenFilterService().getTokenFilter(descriptor);
								tokenFilters.add(tokenFilter);
							}
						}
					}
				}
			}
			return tokenFilters;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	public void setTokenFilters(List<TokenFilter> tokenFilters) {
		this.tokenFilters = tokenFilters;
	}

	/**
	 * The sentence detector to use for analysis.
	 * @return
	 */
	public SentenceDetector getSentenceDetector() {
		try {
			if (sentenceDetector==null) {
				LOG.debug("Getting sentence detector model");
				MachineLearningModel<SentenceDetectorOutcome> sentenceModel = null;
				if (sentenceModelFilePath!=null) {
					sentenceModel = this.getMachineLearningService().getModel(new ZipInputStream(new FileInputStream(sentenceModelFilePath)));
				} else {
					sentenceModel = this.getMachineLearningService().getModel(this.getDefaultSentenceModelStream());
				}
				Set<SentenceDetectorFeature<?>> sentenceDetectorFeatures =
					this.getSentenceDetectorFeatureService().getFeatureSet(sentenceModel.getFeatureDescriptors());
				sentenceDetector = this.getSentenceDetectorService().getSentenceDetector(sentenceModel.getDecisionMaker(), sentenceDetectorFeatures);
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
	public Tokeniser getTokeniser() {
		try {
			if (tokeniser==null) {
				LOG.debug("Getting tokeniser model");
				MachineLearningModel<TokeniserOutcome> tokeniserModel = this.getTokeniserModel();
				
				TokeniserPatternManager tokeniserPatternManager = this.getTokeniserPatternService().getPatternManager(tokeniserModel.getDescriptors().get(TokeniserPatternService.PATTERN_DESCRIPTOR_KEY));
				Set<TokeniserContextFeature<?>> tokeniserContextFeatures = this.getTokenFeatureService().getTokeniserContextFeatureSet(tokeniserModel.getFeatureDescriptors(), tokeniserPatternManager.getParsedTestPatterns());
				tokeniser = this.getTokeniserPatternService().getPatternTokeniser(tokeniserPatternManager, tokeniserContextFeatures, tokeniserModel.getDecisionMaker(), beamWidth);
	
				if (includeDetails) {
					String detailsFilePath = this.getBaseName() + "_tokeniser_details.txt";
					File detailsFile = new File(this.getOutDir(), detailsFilePath);
					detailsFile.delete();
					AnalysisObserver observer = tokeniserModel.getDetailedAnalysisObserver(detailsFile);
					tokeniser.addObserver(observer);
				}
				
				for (TokenFilter tokenFilter : this.getTokenFilters()) {
					tokeniser.addTokenFilter(tokenFilter);
					if (this.needsSentenceDetector()) {
						this.getSentenceDetector().addTokenFilter(tokenFilter);
					}
				}
	
				List<String> tokenFilterDescriptors = tokeniserModel.getDescriptors().get(TokenFilterService.TOKEN_FILTER_DESCRIPTOR_KEY);
				if (tokenFilterDescriptors!=null) {
					for (String descriptor : tokenFilterDescriptors) {
						if (descriptor.length()>0 && !descriptor.startsWith("#")) {
							TokenFilter tokenFilter = this.getTokenFilterService().getTokenFilter(descriptor);
							tokeniser.addTokenFilter(tokenFilter);
							if (this.needsSentenceDetector()) {
								this.getSentenceDetector().addTokenFilter(tokenFilter);
							}
						}
					}
				}
				
				for (TokenSequenceFilter tokenFilter : this.getTokenSequenceFilters()) {
					tokeniser.addTokenSequenceFilter(tokenFilter);
				}
	
				List<String> tokenSequenceFilterDescriptors = tokeniserModel.getDescriptors().get(TokenFilterService.TOKEN_SEQUENCE_FILTER_DESCRIPTOR_KEY);
				if (tokenSequenceFilterDescriptors!=null) {
					for (String descriptor : tokenSequenceFilterDescriptors) {
						if (descriptor.length()>0 && !descriptor.startsWith("#")) {
							TokenSequenceFilter tokenSequenceFilter = this.getTokenFilterService().getTokenSequenceFilter(descriptor);
							tokeniser.addTokenSequenceFilter(tokenSequenceFilter);
						}
					}
				}
			}
			return tokeniser;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	MachineLearningModel<TokeniserOutcome> getTokeniserModel() {
		try {
			if (tokeniserModel==null) {
				if (tokeniserModelFilePath!=null) {
					tokeniserModel = this.getMachineLearningService().getModel(new ZipInputStream(new FileInputStream(tokeniserModelFilePath)));
				} else {
					tokeniserModel = this.getMachineLearningService().getModel(this.getDefaultTokeniserModelStream());
				}
			}
			return tokeniserModel;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
	
	MachineLearningModel<PosTag> getPosTaggerModel() {
		try {
			if (posTaggerModel==null) {
				if (posTaggerModelFilePath!=null) {
					posTaggerModel = this.getMachineLearningService().getModel(new ZipInputStream(new FileInputStream(posTaggerModelFilePath)));
				} else {
					posTaggerModel = this.getMachineLearningService().getModel(this.getDefaultPosTaggerModelStream());
				}
			}
			return posTaggerModel;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
	
	MachineLearningModel<Transition> getParserModel() {
		try {
			if (parserModel==null) {
				if (parserModelFilePath!=null) {
					parserModel = this.getMachineLearningService().getModel(new ZipInputStream(new FileInputStream(parserModelFilePath)));
				} else {
					parserModel = this.getMachineLearningService().getModel(this.getDefaultParserModelStream());
				}
			}
			return parserModel;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * The pos-tagger to use for analysis.
	 * @return
	 */
	public PosTagger getPosTagger() {
		try {
			if (posTagger==null) {
				LOG.debug("Getting pos-tagger model");
				
				MachineLearningModel<PosTag> posTaggerModel = this.getPosTaggerModel();
				Set<PosTaggerFeature<?>> posTaggerFeatures = null;
				if (this.posTaggerFeaturePath!=null) {
					File posTaggerFeatureFile = new File(posTaggerFeaturePath);
					Scanner scanner = new Scanner(posTaggerFeatureFile);
					List<String> featureDescriptors = new ArrayList<String>();
					while (scanner.hasNextLine()) {
						String descriptor = scanner.nextLine();
						featureDescriptors.add(descriptor);
						LOG.debug(descriptor);
					}
					posTaggerFeatures = this.getPosTaggerFeatureService().getFeatureSet(featureDescriptors);
				} else {
					posTaggerFeatures = this.getPosTaggerFeatureService().getFeatureSet(posTaggerModel.getFeatureDescriptors());
				}
				posTagger = this.getPosTaggerService().getPosTagger(posTaggerFeatures, posTaggerModel.getDecisionMaker(), beamWidth);
				
				List<String> posTaggerPreprocessingFilters = posTaggerModel.getDescriptors().get(PosTagger.POSTAG_PREPROCESSING_FILTER_DESCRIPTOR_KEY);
				if (posTaggerPreprocessingFilters!=null) {
					for (String descriptor : posTaggerPreprocessingFilters) {
						if (descriptor.length()>0 && !descriptor.startsWith("#")) {
							TokenSequenceFilter tokenSequenceFilter = this.getTokenFilterService().getTokenSequenceFilter(descriptor);
							posTagger.addPreprocessingFilter(tokenSequenceFilter);
						}
					}
				}
				
				for (TokenSequenceFilter tokenFilter : this.getPosTaggerPreprocessingFilters()) {
					posTagger.addPreprocessingFilter(tokenFilter);
				}
				posTagger.setPosTaggerRules(this.getPosTaggerRules());
		
				if (includeDetails) {
					String detailsFilePath = this.getBaseName() + "_posTagger_details.txt";
					File detailsFile = new File(this.getOutDir(), detailsFilePath);
					detailsFile.delete();
					AnalysisObserver observer = posTaggerModel.getDetailedAnalysisObserver(detailsFile);
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
	public Parser getParser() {
		try {
			if (parser==null) {
				LOG.debug("Getting parser model");
				MachineLearningModel<Transition> parserModel = this.getParserModel();
				
				parser = this.getParserService().getTransitionBasedParser(parserModel, beamWidth);
				parser.setMaxAnalysisTimePerSentence(maxParseAnalysisTime);
				parser.setParserRules(this.getParserRules());
				
				if (includeDetails) {
					String detailsFilePath = this.getBaseName() + "_parser_details.txt";
					File detailsFile = new File(this.getOutDir(), detailsFilePath);
					detailsFile.delete();
					AnalysisObserver observer = parserModel.getDetailedAnalysisObserver(detailsFile);
					parser.addObserver(observer);
				}
				TalismaneSession.setTransitionSystem(parser.getTransitionSystem());

			}
			return parser;
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
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
	public int getMaxParseAnalysisTime() {
		return maxParseAnalysisTime;
	}

	public void setMaxParseAnalysisTime(int maxParseAnalysisTime) {
		this.maxParseAnalysisTime = maxParseAnalysisTime;
	}
	
	/**
	 * A sentence processor to process sentences that have been read.
	 * @return
	 */
	public SentenceProcessor getSentenceProcessor() {
		try {
			if (sentenceProcessor==null && endModule.equals(Module.SentenceDetector)) {
				Reader templateReader = null;
				if (templatePath==null) {
					templateReader = new BufferedReader(new InputStreamReader(getInputStreamFromResource(sentenceTemplateName)));
				} else {
					templateReader = new BufferedReader(new FileReader(new File(templatePath)));
				}
				FreemarkerTemplateWriter templateWriter = new FreemarkerTemplateWriter(this.getWriter(), templateReader);
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
	public TokenSequenceProcessor getTokenSequenceProcessor() {
		try {
			if (tokenSequenceProcessor==null && endModule.equals(Module.Tokeniser)) {
				Reader templateReader = null;
				if (templatePath==null) {
					templateReader = new BufferedReader(new InputStreamReader(getInputStreamFromResource(tokeniserTemplateName)));
				} else {
					templateReader = new BufferedReader(new FileReader(new File(templatePath)));
				}
				FreemarkerTemplateWriter templateWriter = new FreemarkerTemplateWriter(this.getWriter(), templateReader);
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
	public PosTagSequenceProcessor getPosTagSequenceProcessor() {
		try {
			if (posTagSequenceProcessor==null && endModule.equals(Module.PosTagger)) {
				Reader templateReader = null;
				if (templatePath==null) {
					templateReader = new BufferedReader(new InputStreamReader(getInputStreamFromResource(posTaggerTemplateName)));
				} else {
					templateReader = new BufferedReader(new FileReader(new File(templatePath)));
				}
				FreemarkerTemplateWriter templateWriter = new FreemarkerTemplateWriter(this.getWriter(), templateReader);
				posTagSequenceProcessor = templateWriter;
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
	public ParseConfigurationProcessor getParseConfigurationProcessor() {
		try {
			if (parseConfigurationProcessor==null && endModule.equals(Module.Parser)) {
				Reader templateReader = null;
				if (templatePath==null) {
					templateReader = new BufferedReader(new InputStreamReader(getInputStreamFromResource(parserTemplateName)));
				} else {
					templateReader = new BufferedReader(new FileReader(new File(templatePath)));
				}
				FreemarkerTemplateWriter templateWriter = new FreemarkerTemplateWriter(this.getWriter(), templateReader);
				parseConfigurationProcessor = templateWriter;
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
	public TokenRegexBasedCorpusReader getTokenCorpusReader() {
		if (tokenCorpusReader==null) {
			tokenCorpusReader = this.getTokeniserService().getRegexBasedCorpusReader(this.getReader());
			if (this.getInputRegex()!=null)
				tokenCorpusReader.setRegex(this.getInputRegex());
			
			for (TokenFilter tokenFilter : this.getTokenFilters()) {
				tokenCorpusReader.addTokenFilter(tokenFilter);
			}
			
			if (startModule.equals(Module.PosTagger)) {
				MachineLearningModel<PosTag> posTaggerModel = this.getPosTaggerModel();
				
				List<String> tokenFilterDescriptors = posTaggerModel.getDescriptors().get(TokenFilterService.TOKEN_FILTER_DESCRIPTOR_KEY);
				if (tokenFilterDescriptors!=null) {
					for (String descriptor : tokenFilterDescriptors) {
						if (descriptor.length()>0 && !descriptor.startsWith("#")) {
							TokenFilter tokenFilter = this.getTokenFilterService().getTokenFilter(descriptor);
							tokenCorpusReader.addTokenFilter(tokenFilter);
						}
					}
				}
				
				List<String> tokenSequenceFilterDescriptors = posTaggerModel.getDescriptors().get(TokenFilterService.TOKEN_SEQUENCE_FILTER_DESCRIPTOR_KEY);
				if (tokenSequenceFilterDescriptors!=null) {
					for (String descriptor : tokenSequenceFilterDescriptors) {
						if (descriptor.length()>0 && !descriptor.startsWith("#")) {
							TokenSequenceFilter tokenSequenceFilter = this.getTokenFilterService().getTokenSequenceFilter(descriptor);
							tokenCorpusReader.addTokenSequenceFilter(tokenSequenceFilter);
						}
					}
				}
			}
			
			for (TokenSequenceFilter tokenSequenceFilter : this.getTokenSequenceFilters()) {
				tokenCorpusReader.addTokenSequenceFilter(tokenSequenceFilter);
			}
		}
		return tokenCorpusReader;
	}

	/**
	 * A pos tag corpus reader to read a corpus pre-annotated in postags.
	 * Note that, in general, any filters up to and including the pos-tagger should be applied to the reader.
	 * @return
	 */
	public PosTagAnnotatedCorpusReader getPosTagCorpusReader() {
		if (posTagCorpusReader==null) {
			PosTagRegexBasedCorpusReader posTagRegexBasedCorpusReader = this.getPosTaggerService().getRegexBasedCorpusReader(this.getReader());
			if (this.getInputRegex()!=null)
				posTagRegexBasedCorpusReader.setRegex(this.getInputRegex());
			posTagCorpusReader = posTagRegexBasedCorpusReader;
		}
		this.addPosTagCorpusReaderFilters(posTagCorpusReader);
		return posTagCorpusReader;
	}
	
	void addPosTagCorpusReaderFilters(PosTagAnnotatedCorpusReader corpusReader) {
		if (!posTagCorpusReaderFiltersAdded) {
			MachineLearningModel<?> myTokeniserModel = null;
			MachineLearningModel<?> myPosTaggerModel = null;
			if (this.getStartModule().equals(Module.Tokeniser)) {
				myTokeniserModel = this.getTokeniserModel();
				myPosTaggerModel = this.getPosTaggerModel();
			} else if (this.getStartModule().equals(Module.PosTagger)) {
				myTokeniserModel = this.getPosTaggerModel();
				myPosTaggerModel = this.getPosTaggerModel();
			} else {
				myTokeniserModel = this.getParserModel();
				myPosTaggerModel = this.getParserModel();
			}
				
			List<String> tokenFilterDescriptors = myTokeniserModel.getDescriptors().get(TokenFilterService.TOKEN_FILTER_DESCRIPTOR_KEY);
			if (tokenFilterDescriptors!=null) {
				List<TokenFilter> parserTokenFilters = new ArrayList<TokenFilter>();
				for (String descriptor : tokenFilterDescriptors) {
					if (descriptor.length()>0 && !descriptor.startsWith("#")) {
						TokenFilter tokenFilter = this.getTokenFilterService().getTokenFilter(descriptor);
						parserTokenFilters.add(tokenFilter);
					}
				}
				TokenSequenceFilter tokenFilterWrapper = this.getTokenFilterService().getTokenSequenceFilter(parserTokenFilters);
				corpusReader.addTokenSequenceFilter(tokenFilterWrapper);
			}
			
			List<String> tokenSequenceFilterDescriptors = myTokeniserModel.getDescriptors().get(TokenFilterService.TOKEN_SEQUENCE_FILTER_DESCRIPTOR_KEY);
			if (tokenSequenceFilterDescriptors!=null) {
				for (String descriptor : tokenSequenceFilterDescriptors) {
					if (descriptor.length()>0 && !descriptor.startsWith("#")) {
						TokenSequenceFilter tokenSequenceFilter = this.getTokenFilterService().getTokenSequenceFilter(descriptor);
						corpusReader.addTokenSequenceFilter(tokenSequenceFilter);
					}
				}
			}
			
			List<String> posTaggerPreprocessingFilters = myPosTaggerModel.getDescriptors().get(PosTagger.POSTAG_PREPROCESSING_FILTER_DESCRIPTOR_KEY);
			if (posTaggerPreprocessingFilters!=null) {
				for (String descriptor : posTaggerPreprocessingFilters) {
					if (descriptor.length()>0 && !descriptor.startsWith("#")) {
						TokenSequenceFilter tokenSequenceFilter = this.getTokenFilterService().getTokenSequenceFilter(descriptor);
						corpusReader.addTokenSequenceFilter(tokenSequenceFilter);
					}
				}
			}
			
			for (TokenSequenceFilter tokenFilter : this.getTokenSequenceFilters()) {
				corpusReader.addTokenSequenceFilter(tokenFilter);
			}
			for (TokenSequenceFilter tokenFilter : this.getPosTaggerPreprocessingFilters()) {
				corpusReader.addTokenSequenceFilter(tokenFilter);
			}
			posTagCorpusReaderFiltersAdded = true;
		}
	}

	/**
	 * A parser corpus reader to read a corpus pre-annotated in dependencies.
	 * @return
	 */
	public ParserRegexBasedCorpusReader getParserCorpusReader() {
		if (parserCorpusReader==null) {
			parserCorpusReader = this.getParserService().getRegexBasedCorpusReader(this.getReader());
			if (this.getInputRegex()!=null)
				parserCorpusReader.setRegex(this.getInputRegex());
		}
		this.addParserCorpusReaderFilters(parserCorpusReader);
		return parserCorpusReader;
	}
	

	public ParserAnnotatedCorpusReader getParserEvaluationCorpusReader() {
		if (parserEvaluationCorpusReader==null) {
			parserEvaluationCorpusReader = this.getParserService().getRegexBasedCorpusReader(this.getEvaluationReader());
			if (this.getInputRegex()!=null)
				parserEvaluationCorpusReader.setRegex(this.getInputRegex());
		}
		this.addParserCorpusReaderFilters(parserEvaluationCorpusReader);
		return parserEvaluationCorpusReader;
	}
	
	void addParserCorpusReaderFilters(ParserRegexBasedCorpusReader corpusReader) {
		if (!parserCorpusReaderFiltersAdded) {
			MachineLearningModel<?> myTokeniserModel = null;
			MachineLearningModel<?> myPosTaggerModel = null;
			if (this.getStartModule().equals(Module.Tokeniser)) {
				myTokeniserModel = this.getTokeniserModel();
				myPosTaggerModel = this.getPosTaggerModel();
			} else if (this.getStartModule().equals(Module.PosTagger)) {
				myTokeniserModel = this.getPosTaggerModel();
				myPosTaggerModel = this.getPosTaggerModel();
			} else {
				myTokeniserModel = this.getParserModel();
				myPosTaggerModel = this.getParserModel();
			}
			
				
			List<String> tokenFilterDescriptors = myTokeniserModel.getDescriptors().get(TokenFilterService.TOKEN_FILTER_DESCRIPTOR_KEY);
			if (tokenFilterDescriptors!=null) {
				List<TokenFilter> parserTokenFilters = new ArrayList<TokenFilter>();
				for (String descriptor : tokenFilterDescriptors) {
					if (descriptor.length()>0 && !descriptor.startsWith("#")) {
						TokenFilter tokenFilter = this.getTokenFilterService().getTokenFilter(descriptor);
						parserTokenFilters.add(tokenFilter);
					}
				}
				TokenSequenceFilter tokenFilterWrapper = this.getTokenFilterService().getTokenSequenceFilter(parserTokenFilters);
				corpusReader.addTokenSequenceFilter(tokenFilterWrapper);
			}
			
			List<String> tokenSequenceFilterDescriptors = myTokeniserModel.getDescriptors().get(TokenFilterService.TOKEN_SEQUENCE_FILTER_DESCRIPTOR_KEY);
			if (tokenSequenceFilterDescriptors!=null) {
				for (String descriptor : tokenSequenceFilterDescriptors) {
					if (descriptor.length()>0 && !descriptor.startsWith("#")) {
						TokenSequenceFilter tokenSequenceFilter = this.getTokenFilterService().getTokenSequenceFilter(descriptor);
						corpusReader.addTokenSequenceFilter(tokenSequenceFilter);
					}
				}
			}
			
			List<String> posTaggerPreprocessingFilters = myPosTaggerModel.getDescriptors().get(PosTagger.POSTAG_PREPROCESSING_FILTER_DESCRIPTOR_KEY);
			if (posTaggerPreprocessingFilters!=null) {
				for (String descriptor : posTaggerPreprocessingFilters) {
					if (descriptor.length()>0 && !descriptor.startsWith("#")) {
						TokenSequenceFilter tokenSequenceFilter = this.getTokenFilterService().getTokenSequenceFilter(descriptor);
						corpusReader.addTokenSequenceFilter(tokenSequenceFilter);
					}
				}
			}
			
			for (TokenSequenceFilter tokenFilter : this.getTokenSequenceFilters()) {
				corpusReader.addTokenSequenceFilter(tokenFilter);
			}
			for (TokenSequenceFilter tokenFilter : this.getPosTaggerPreprocessingFilters()) {
				corpusReader.addTokenSequenceFilter(tokenFilter);
			}
			parserCorpusReaderFiltersAdded = true;
		}
	}

	public void setPosTagCorpusReader(
			PosTagAnnotatedCorpusReader posTagCorpusReader) {
		this.posTagCorpusReader = posTagCorpusReader;
	}

	public void setParserCorpusReader(
			ParserRegexBasedCorpusReader parserCorpusReader) {
		this.parserCorpusReader = parserCorpusReader;
	}

	/**
	 * Get a parser evaluator if command=evaluate and endModule=parser.
	 * @return
	 */
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
				parseFScoreCalculator.setLabeledEvaluation(true);
				
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
					File csvFile = new File(this.getOutDir(), this.getBaseName() + ".distances.csv");
					csvFile.delete();
					csvFile.createNewFile();
					Writer csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false),"UTF8"));
					ParserFScoreCalculatorByDistance calculator = new ParserFScoreCalculatorByDistance(csvFileWriter);
					parserEvaluator.addObserver(calculator);
				}
				
				Reader templateReader = null;
				if (templatePath==null) {
					templateReader = new BufferedReader(new InputStreamReader(getInputStreamFromResource(parserTemplateName)));
				} else {
					templateReader = new BufferedReader(new FileReader(new File(templatePath)));
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
	public ParseComparator getParseComparator() {
		try {
			if (parseComparator==null) {
				parseComparator = this.getParserService().getParseComparator();
				File fscoreFile = new File(this.getOutDir(), this.getBaseName() + ".fscores.csv");

				ParseEvaluationFScoreCalculator parseFScoreCalculator = new ParseEvaluationFScoreCalculator(fscoreFile);
				parseFScoreCalculator.setLabeledEvaluation(true);

				parseComparator.addObserver(parseFScoreCalculator);
				
				if (includeDistanceFScores) {
					File csvFile = new File(this.getOutDir(), this.getBaseName() + ".distances.csv");
					csvFile.delete();
					csvFile.createNewFile();
					Writer csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false),"UTF8"));
					ParserFScoreCalculatorByDistance calculator = new ParserFScoreCalculatorByDistance(csvFileWriter);
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
	 * Get a pos-tagger evaluator if command=evaluate and endModule=pos-tagger.
	 * @return
	 */
	public PosTaggerEvaluator getPosTaggerEvaluator() {
		try {
			if (posTaggerEvaluator==null) {				
				posTaggerEvaluator = posTaggerService.getPosTaggerEvaluator(this.getPosTagger());
				
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
				
				File fscoreFile = new File(this.getOutDir(), this.getBaseName() + ".fscores.csv");

				PosTagEvaluationFScoreCalculator posTagFScoreCalculator = new PosTagEvaluationFScoreCalculator(fscoreFile);
				posTaggerEvaluator.addObserver(posTagFScoreCalculator);
				
				Reader templateReader = null;
				if (templatePath==null) {
					templateReader = new BufferedReader(new InputStreamReader(getInputStreamFromResource(posTaggerTemplateName)));
				} else {
					templateReader = new BufferedReader(new FileReader(new File(templatePath)));
				}
				
				File freemarkerFile = new File(this.getOutDir(), this.getBaseName() + "_output.txt");
				freemarkerFile.delete();
				freemarkerFile.createNewFile();
				Writer freemakerFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(freemarkerFile, false),"UTF8"));
				PosTaggerGuessTemplateWriter templateWriter = new PosTaggerGuessTemplateWriter(freemakerFileWriter, templateReader);
				posTaggerEvaluator.addObserver(templateWriter);
				
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
	 * The base name, out of which to construct output file names.
	 * @return
	 */
	public String getBaseName() {
		if (baseName==null) {
			baseName = "Talismane";
			if (outFilePath!=null) {
				if (outFilePath.indexOf('.')>0)
					baseName = outFilePath.substring(outFilePath.lastIndexOf('/')+1, outFilePath.indexOf('.'));
				else
					baseName = outFilePath.substring(outFilePath.lastIndexOf('/')+1);
			} else if (inFilePath!=null) {
				if (inFilePath.indexOf('.')>0)
					baseName = inFilePath.substring(inFilePath.lastIndexOf('/')+1, inFilePath.indexOf('.'));
				else
					baseName = inFilePath.substring(inFilePath.lastIndexOf('/')+1);
			} else if (sentenceModelFilePath!=null && module.equals(Talismane.Module.SentenceDetector)||endModule.equals(Talismane.Module.SentenceDetector)) {
				if (sentenceModelFilePath.indexOf('.')>0)
					baseName = sentenceModelFilePath.substring(sentenceModelFilePath.lastIndexOf('/')+1, sentenceModelFilePath.indexOf('.'));
				else
					baseName = sentenceModelFilePath.substring(sentenceModelFilePath.lastIndexOf('/')+1);
			} else if (tokeniserModelFilePath!=null && (module.equals(Talismane.Module.Tokeniser)||endModule.equals(Talismane.Module.Tokeniser))) {
				if (tokeniserModelFilePath.indexOf('.')>0)
					baseName = tokeniserModelFilePath.substring(tokeniserModelFilePath.lastIndexOf('/')+1, tokeniserModelFilePath.indexOf('.'));
				else
					baseName = tokeniserModelFilePath.substring(tokeniserModelFilePath.lastIndexOf('/')+1);
			} else if (posTaggerModelFilePath!=null && (module.equals(Talismane.Module.PosTagger)||endModule.equals(Talismane.Module.PosTagger))) {
				if (posTaggerModelFilePath.indexOf('.')>0)
					baseName = posTaggerModelFilePath.substring(posTaggerModelFilePath.lastIndexOf('/')+1, posTaggerModelFilePath.indexOf('.'));
				else
					baseName = posTaggerModelFilePath.substring(posTaggerModelFilePath.lastIndexOf('/')+1);
			} else if (parserModelFilePath!=null && (module.equals(Talismane.Module.Parser)||endModule.equals(Talismane.Module.Parser))) {
				if (parserModelFilePath.indexOf('.')>0)
					baseName = parserModelFilePath.substring(parserModelFilePath.lastIndexOf('/')+1, parserModelFilePath.indexOf('.'));
				else
					baseName = parserModelFilePath.substring(parserModelFilePath.lastIndexOf('/')+1);
			}
			baseName = baseName + suffix;
		}
		return baseName;
	}

	public PosTaggerService getPosTaggerService() {
		if (posTaggerService==null) {
			posTaggerService = talismaneServiceLocator.getPosTaggerServiceLocator().getPosTaggerService();
		}
		return posTaggerService;
	}

	public void setPosTaggerService(PosTaggerService posTaggerService) {
		this.posTaggerService = posTaggerService;
	}

	public ParserService getParserService() {
		if (parserService==null) {
			parserService = talismaneServiceLocator.getParserServiceLocator().getParserService();
		}
		return parserService;
	}

	public void setParserService(ParserService parserService) {
		this.parserService = parserService;
	}

	public PosTaggerFeatureService getPosTaggerFeatureService() {
		if (posTaggerFeatureService==null) {
			posTaggerFeatureService = talismaneServiceLocator.getPosTaggerFeatureServiceLocator().getPosTaggerFeatureService();
		}
		return posTaggerFeatureService;
	}

	public void setPosTaggerFeatureService(
			PosTaggerFeatureService posTaggerFeatureService) {
		this.posTaggerFeatureService = posTaggerFeatureService;
	}
	
	public ParserFeatureService getParserFeatureService() {
		if (parserFeatureService==null) {
			parserFeatureService = talismaneServiceLocator.getParserFeatureServiceLocator().getParserFeatureService();
		}
		return parserFeatureService;
	}

	public void setParserFeatureService(
			ParserFeatureService parserFeatureService) {
		this.parserFeatureService = parserFeatureService;
	}

	public FilterService getFilterService() {
		if (filterService==null) {
			filterService = talismaneServiceLocator.getFilterServiceLocator().getFilterService();
		}
		return filterService;
	}

	public void setFilterService(FilterService filterService) {
		this.filterService = filterService;
	}

	public TokenFilterService getTokenFilterService() {
		if (tokenFilterService==null) {
			tokenFilterService = talismaneServiceLocator.getTokenFilterServiceLocator().getTokenFilterService();
		}
		return tokenFilterService;
	}

	public void setTokenFilterService(TokenFilterService tokenFilterService) {
		this.tokenFilterService = tokenFilterService;
	}

	public SentenceDetectorService getSentenceDetectorService() {
		if (sentenceDetectorService==null) {
			sentenceDetectorService=talismaneServiceLocator.getSentenceDetectorServiceLocator().getSentenceDetectorService();
		}
		return sentenceDetectorService;
	}

	public void setSentenceDetectorService(
			SentenceDetectorService sentenceDetectorService) {
		this.sentenceDetectorService = sentenceDetectorService;
	}

	public SentenceDetectorFeatureService getSentenceDetectorFeatureService() {
		if (sentenceDetectorFeatureService==null) {
			sentenceDetectorFeatureService = talismaneServiceLocator.getSentenceDetectorFeatureServiceLocator().getSentenceDetectorFeatureService();
		}
		return sentenceDetectorFeatureService;
	}

	public void setSentenceDetectorFeatureService(
			SentenceDetectorFeatureService sentenceDetectorFeatureService) {
		this.sentenceDetectorFeatureService = sentenceDetectorFeatureService;
	}

	public MachineLearningService getMachineLearningService() {
		if (machineLearningService==null) {
			machineLearningService = talismaneServiceLocator.getMachineLearningServiceLocator().getMachineLearningService();
		}
		return machineLearningService;
	}

	public void setMachineLearningService(
			MachineLearningService machineLearningService) {
		this.machineLearningService = machineLearningService;
	}

	public TokeniserPatternService getTokeniserPatternService() {
		if (tokeniserPatternService==null) {
			tokeniserPatternService = talismaneServiceLocator.getTokenPatternServiceLocator().getTokeniserPatternService();
		}
		return tokeniserPatternService;
	}

	public void setTokeniserPatternService(
			TokeniserPatternService tokeniserPatternService) {
		this.tokeniserPatternService = tokeniserPatternService;
	}

	public TokenFeatureService getTokenFeatureService() {
		if (tokenFeatureService==null) {
			tokenFeatureService = talismaneServiceLocator.getTokenFeatureServiceLocator().getTokenFeatureService();
		}
		return tokenFeatureService;
	}

	public void setTokenFeatureService(TokenFeatureService tokenFeatureService) {
		this.tokenFeatureService = tokenFeatureService;
	}
	
	
	public TokeniserService getTokeniserService() {
		if (this.tokeniserService==null)
			this.tokeniserService = talismaneServiceLocator.getTokeniserServiceLocator().getTokeniserService();
		return tokeniserService;
	}

	public void setTokeniserService(TokeniserService tokeniserService) {
		this.tokeniserService = tokeniserService;
	}

	/**
	 * Does this instance of Talismane need a sentence detector to perform the requested processing.
	 */
	public boolean needsSentenceDetector() {
		return startModule.compareTo(Module.SentenceDetector)<=0 && endModule.compareTo(Module.SentenceDetector)>=0;
	}
	
	/**
	 * Does this instance of Talismane need a tokeniser to perform the requested processing.
	 */
	public boolean needsTokeniser() {
		return startModule.compareTo(Module.Tokeniser)<=0 && endModule.compareTo(Module.Tokeniser)>=0;
	}

	/**
	 * Does this instance of Talismane need a pos tagger to perform the requested processing.
	 */
	public boolean needsPosTagger() {
		return startModule.compareTo(Module.PosTagger)<=0 && endModule.compareTo(Module.PosTagger)>=0;
	}
	
	/**
	 * Does this instance of Talismane need a parser to perform the requested processing.
	 */
	public boolean needsParser() {
		return startModule.compareTo(Module.Parser)<=0 && endModule.compareTo(Module.Parser)>=0;
	}
	
	private static InputStream getInputStreamFromResource(String resource) {
		String path = "/com/joliciel/talismane/output/" + resource;
		InputStream inputStream = Talismane.class.getResourceAsStream(path); 
		
		return inputStream;
	}

	public String getInFilePath() {
		return inFilePath;
	}

	@Override
	public PosTagSet getDefaultPosTagSet() {
		Scanner posTagSetScanner = new Scanner(this.getDefaultPosTagSetFromStream(),"UTF-8");
		PosTagSet posTagSet = this.getPosTaggerService().getPosTagSet(posTagSetScanner);
		return posTagSet;
	}

	public boolean isLogStats() {
		return logStats;
	}
}
