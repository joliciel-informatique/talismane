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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Set;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


import com.joliciel.talismane.filters.DuplicateWhiteSpaceFilter;
import com.joliciel.talismane.filters.NewlineNormaliser;
import com.joliciel.talismane.filters.RegexFindReplaceFilter;
import com.joliciel.talismane.filters.TextStreamFilter;
import com.joliciel.talismane.machineLearning.AnalysisObserver;
import com.joliciel.talismane.machineLearning.MachineLearningModel;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.output.FreemarkerTemplateWriter;
import com.joliciel.talismane.parser.NonDeterministicParser;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.parser.ParseConfigurationProcessor;
import com.joliciel.talismane.parser.Parser;
import com.joliciel.talismane.parser.ParserEvaluator;
import com.joliciel.talismane.parser.ParserRegexBasedCorpusReader;
import com.joliciel.talismane.parser.ParserService;
import com.joliciel.talismane.parser.Transition;
import com.joliciel.talismane.parser.TransitionSystem;
import com.joliciel.talismane.parser.features.ParseConfigurationFeature;
import com.joliciel.talismane.parser.features.ParserFeatureService;
import com.joliciel.talismane.posTagger.NonDeterministicPosTagger;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagRegexBasedCorpusReader;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTagSequenceProcessor;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.posTagger.PosTagger;
import com.joliciel.talismane.posTagger.PosTaggerEvaluator;
import com.joliciel.talismane.posTagger.PosTaggerLexiconService;
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
import com.joliciel.talismane.stats.FScoreCalculator;
import com.joliciel.talismane.tokeniser.PretokenisedSequence;
import com.joliciel.talismane.tokeniser.TokenRegexBasedCorpusReader;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.TokenSequenceProcessor;
import com.joliciel.talismane.tokeniser.Tokeniser;
import com.joliciel.talismane.tokeniser.TokeniserOutcome;
import com.joliciel.talismane.tokeniser.TokeniserService;
import com.joliciel.talismane.tokeniser.features.TokenFeatureService;
import com.joliciel.talismane.tokeniser.features.TokeniserContextFeature;
import com.joliciel.talismane.tokeniser.filters.NumberFilter;
import com.joliciel.talismane.tokeniser.filters.PrettyQuotesFilter;
import com.joliciel.talismane.tokeniser.filters.french.EmptyTokenAfterDuFilter;
import com.joliciel.talismane.tokeniser.filters.french.EmptyTokenBeforeDuquelFilter;
import com.joliciel.talismane.tokeniser.filters.french.LowercaseFirstWordFrenchFilter;
import com.joliciel.talismane.tokeniser.filters.french.UpperCaseSeriesFilter;
import com.joliciel.talismane.tokeniser.patterns.TokeniserPatternManager;
import com.joliciel.talismane.tokeniser.patterns.TokeniserPatternService;
import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.PerformanceMonitor;

public abstract class AbstractTalismane implements Talismane {
	private static final Log LOG = LogFactory.getLog(AbstractTalismane.class);
	
	private SentenceDetector sentenceDetector;
	private Tokeniser tokeniser;
	private PosTagger posTagger;
	private Parser parser;
	private boolean propagateBeam = true;
	private static final int MIN_BLOCK_SIZE = 300;
	private List<TextStreamFilter> textStreamFilters = new ArrayList<TextStreamFilter>();
	private SentenceProcessor sentenceProcessor;
	private TokenSequenceProcessor tokenSequenceProcessor;
	private PosTagSequenceProcessor posTagSequenceProcessor;
	private ParseConfigurationProcessor parseConfigurationProcessor;
	private String inputRegex;
	private List<PosTaggerRule> posTaggerRules = null;
	
	private Module startModule = Module.SentenceDetector;
	private Module endModule = Module.Parser;
	
	private TokeniserService tokeniserService;
	private PosTaggerService posTaggerService;
	private PosTaggerLexiconService lexiconService;
	
	private boolean stopOnError = false;
	
	protected AbstractTalismane() {
		
	}
	
	public void runCommand(String[] args) throws Exception {
		if (args.length==0) {
			System.out.println("Talismane usage instructions: ");
			System.out.println("* indicates optional, + indicates default value");
			System.out.println("");
			System.out.println("Usage: command=analyse *startModule=[sentence+|tokenise|postag|parse] *endModule=[sentence|tokenise|postag|parse+] *inFile=[inFilePath, stdin if missing] *outFile=[outFilePath, stdout if missing] *template=[outputTemplatePath]");
			System.out.println("");
			System.out.println("Usage: command=evaluate module=[sentence|tokenise|postag|parse] *inFile=[inFilePath] *outDir=[outDirPath]");
			System.out.println("");
			System.out.println("Additional optional parameters shared by both command types:");
			System.out.println(" *encoding=[UTF-8+ or other] *includeDetails=[true|false+] posTaggerRules*=[posTaggerRuleFilePath] regexFilters*=[regexFilterFilePath] *sentenceModel=[path] *tokeniserModel=[path] *posTaggerModel=[path] *parserModel=[path] *inputPatternFile=[inputPatternFilePath] *posTagSet=[posTagSetPath]");
			return;
		}
		
		Command command = Command.analyse;
		String inFilePath = null;
		String outFilePath = null;
		String outDirPath = null;
		String encoding = "UTF-8";
		String templatePath = null;
		int beamWidth = 1;
		boolean propagateBeam = true;
		boolean includeDetails = false;
		String parserModelFilePath = null;
		String posTaggerModelFilePath = null;
		String tokeniserModelFilePath = null;
		String sentenceModelFilePath = null;
		Talismane.Module module = Talismane.Module.PosTagger;
		Talismane.Module startModule = Talismane.Module.SentenceDetector;
		Talismane.Module endModule = Talismane.Module.Parser;
		
		String inputPatternFilePath = null;
		String inputRegex = null;
		String posTaggerRuleFilePath = null;
		String posTagSetPath = null;
		String regexFiltersPath = null;
		
		boolean logPerformance = false;
		
		for (String arg : args) {
			int equalsPos = arg.indexOf('=');
			String argName = arg.substring(0, equalsPos);
			String argValue = arg.substring(equalsPos+1);
			if (argName.equals("command")) {
				if (argValue.equalsIgnoreCase("analyse")||argValue.equalsIgnoreCase("analyze"))
					command = Command.analyse;
				else if (argValue.equalsIgnoreCase("evaluate"))
					command = Command.evaluate;
				else
					throw new TalismaneException("Unknown command: " + argValue);
			} else if (argName.equals("module")) {
				if (argValue.equalsIgnoreCase("sentence"))
					module = Talismane.Module.SentenceDetector;
				else if (argValue.equalsIgnoreCase("tokenise"))
					module = Talismane.Module.Tokeniser;
				else if (argValue.equalsIgnoreCase("postag"))
					module = Talismane.Module.PosTagger;
				else if (argValue.equalsIgnoreCase("parse"))
					module = Talismane.Module.Parser;
				else
					throw new TalismaneException("Unknown module: " + argValue);
			} else if (argName.equals("startModule")) {
				if (argValue.equalsIgnoreCase("sentence"))
					startModule = Talismane.Module.SentenceDetector;
				else if (argValue.equalsIgnoreCase("tokenise"))
					startModule = Talismane.Module.Tokeniser;
				else if (argValue.equalsIgnoreCase("postag"))
					startModule = Talismane.Module.PosTagger;
				else if (argValue.equalsIgnoreCase("parse"))
					startModule = Talismane.Module.Parser;
				else
					throw new TalismaneException("Unknown startModule: " + argValue);
			} else if (argName.equals("endModule")) {
				if (argValue.equalsIgnoreCase("sentence"))
					endModule = Talismane.Module.SentenceDetector;
				else if (argValue.equalsIgnoreCase("tokenise"))
					endModule = Talismane.Module.Tokeniser;
				else if (argValue.equalsIgnoreCase("postag"))
					endModule = Talismane.Module.PosTagger;
				else if (argValue.equalsIgnoreCase("parse"))
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
			else if (argName.equals("encoding")) 
				encoding = argValue;
			else if (argName.equals("includeDetails"))
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
			else if (argName.equals("posTagSet"))
				posTagSetPath = argValue;
			else if (argName.equals("regexFilters"))
				regexFiltersPath = argValue;
			else if (argName.equals("logPerformance"))
				logPerformance = argValue.equalsIgnoreCase("true");
			else {
				System.out.println("Unknown argument: " + argName);
				throw new RuntimeException("Unknown argument: " + argName);
			}
		}
		
		if (command==null)
			throw new TalismaneException("No command provided.");

		long startTime = new Date().getTime();
		PerformanceMonitor.setActive(logPerformance);
		PerformanceMonitor.start();
		try {
			String sentenceTemplateName = "sentence_template.ftl";
			String tokeniserTemplateName = "tokeniser_template.ftl";
			String posTaggerTemplateName = "posTagger_template.ftl";
			String parserTemplateName = "parser_conll_template.ftl";

	    	TalismaneServiceLocator talismaneServiceLocator = TalismaneServiceLocator.getInstance();
			this.setTokeniserService(talismaneServiceLocator.getTokeniserServiceLocator().getTokeniserService());
			this.setPosTaggerService(talismaneServiceLocator.getPosTaggerServiceLocator().getPosTaggerService());

			TalismaneSession.setLexiconService(this.getLexiconService());
	
	        SentenceDetectorService sentenceDetectorService = talismaneServiceLocator.getSentenceDetectorServiceLocator().getSentenceDetectorService();
	        SentenceDetectorFeatureService sentenceDetectorFeatureService = talismaneServiceLocator.getSentenceDetectorFeatureServiceLocator().getSentenceDetectorFeatureService();
	 
	        TokenFeatureService tokenFeatureService = talismaneServiceLocator.getTokeniserFeatureServiceLocator().getTokenFeatureService();
	        TokeniserPatternService tokeniserPatternService = talismaneServiceLocator.getTokenPatternServiceLocator().getTokeniserPatternService();
	        
	        PosTaggerService posTaggerService = talismaneServiceLocator.getPosTaggerServiceLocator().getPosTaggerService();
			PosTaggerFeatureService posTaggerFeatureService = talismaneServiceLocator.getPosTaggerFeatureServiceLocator().getPosTaggerFeatureService();
			
			ParserService parserService = talismaneServiceLocator.getParserServiceLocator().getParserService();
			ParserFeatureService parserFeatureService = talismaneServiceLocator.getParserFeatureServiceLocator().getParserFeatureService();

			MachineLearningService machineLearningService = talismaneServiceLocator.getMachineLearningServiceLocator().getMachineLearningService();
			
			Scanner posTagSetScanner = null;
			if (posTagSetPath==null||posTagSetPath.length()==0) {
				posTagSetScanner = new Scanner(this.getDefaultPosTagSetFromStream());
			} else {
				File posTagSetFile = new File(posTagSetPath);
				posTagSetScanner = new Scanner(posTagSetFile);
			}
			
			PosTagSet posTagSet = posTaggerService.getPosTagSet(posTagSetScanner);
			TalismaneSession.setPosTagSet(posTagSet);
			
			Reader reader = null;
			if (inFilePath!=null) {
				try {
					File inFile = new File(inFilePath);
					reader = new BufferedReader(new InputStreamReader(new FileInputStream(inFile), encoding));
				} catch (FileNotFoundException fnfe) {
					LogUtils.logError(LOG, fnfe);
					throw new RuntimeException(fnfe);
				} catch (UnsupportedEncodingException uee) {
					LogUtils.logError(LOG, uee);
					throw new RuntimeException(uee);
				}
			} else {
				reader = new BufferedReader(new InputStreamReader(System.in));
			}
			
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
			this.setInputRegex(inputRegex);
			
			Scanner rulesScanner = null;
			if (posTaggerRuleFilePath!=null && posTaggerRuleFilePath.length()>0) {
				File posTaggerRuleFile = new File(posTaggerRuleFilePath);
				rulesScanner = new Scanner(posTaggerRuleFile);
			} else {
				InputStream defaultRulesStream = this.getDefaultPosTaggerRulesFromStream();
				rulesScanner = new Scanner(defaultRulesStream);
			}
			if (rulesScanner!=null) {
				List<String> ruleDescriptors = new ArrayList<String>();
				while (rulesScanner.hasNextLine()) {
					String ruleDescriptor = rulesScanner.nextLine();
					ruleDescriptors.add(ruleDescriptor);
					LOG.debug(ruleDescriptor);
				}
				List<PosTaggerRule> rules = posTaggerFeatureService.getRules(ruleDescriptors);
				this.setPosTaggerRules(rules);
			}
			
			if (command.equals(Command.analyse)) {
				this.setStartModule(startModule);
				this.setEndModule(endModule);
				
				Writer writer = null;
				if (outFilePath!=null) {
					if (outFilePath.lastIndexOf("/")>=0) {
						String outFileDirPath = outFilePath.substring(0, outFilePath.lastIndexOf("/"));
						File outFileDir = new File(outFileDirPath);
						outFileDir.mkdirs();
					}
					File outFile = new File(outFilePath);
					outFile.delete();
					outFile.createNewFile();
				
					writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), encoding));
				} else {
					writer = new BufferedWriter(new OutputStreamWriter(System.out));
				}
				
				this.addTextStreamFilter(new NewlineNormaliser());

				if (this.needsSentenceDetector()) {
					LOG.debug("Getting sentence detector model");
					MachineLearningModel<SentenceDetectorOutcome> sentenceModel = null;
					if (sentenceModelFilePath!=null) {
						sentenceModel = machineLearningService.getModel(new ZipInputStream(new FileInputStream(sentenceModelFilePath)));
					} else {
						sentenceModel = machineLearningService.getModel(this.getDefaultSentenceModelStream());
					}
					Set<SentenceDetectorFeature<?>> sentenceDetectorFeatures =
						sentenceDetectorFeatureService.getFeatureSet(sentenceModel.getFeatureDescriptors());
					SentenceDetector sentenceDetector = sentenceDetectorService.getSentenceDetector(sentenceModel.getDecisionMaker(), sentenceDetectorFeatures);
			
					this.setSentenceDetector(sentenceDetector);
					
					if (regexFiltersPath!=null) {
						File regexFiltersFile = new File(regexFiltersPath);
						Scanner regexFiltersScanner = new Scanner(regexFiltersFile);
						List<String> findList = new ArrayList<String>();
						List<String> replaceList = new ArrayList<String>();
						while (regexFiltersScanner.hasNextLine()) {
							String line = regexFiltersScanner.nextLine();
							findList.add(line);
							line = regexFiltersScanner.nextLine();
							line = line.replace("¶", "\n");
							replaceList.add(line);
						}
						for (int i=0; i<findList.size(); i++) {
							RegexFindReplaceFilter regexFilter = new RegexFindReplaceFilter(findList.get(i), replaceList.get(i));
							this.addTextStreamFilter(regexFilter);
						}
					}
					
					this.addTextStreamFilter(new DuplicateWhiteSpaceFilter());
				}
				
				if (this.needsTokeniser()) {
					LOG.debug("Getting tokeniser model");
					MachineLearningModel<TokeniserOutcome> tokeniserModel = null;
					if (tokeniserModelFilePath!=null) {
						tokeniserModel = machineLearningService.getModel(new ZipInputStream(new FileInputStream(tokeniserModelFilePath)));
					} else {
						tokeniserModel = machineLearningService.getModel(this.getDefaultTokeniserModelStream());
					}
	
					TokeniserPatternManager tokeniserPatternManager = tokeniserPatternService.getPatternManager(tokeniserModel.getDescriptors().get(TokeniserPatternService.PATTERN_DESCRIPTOR_KEY));
					Set<TokeniserContextFeature<?>> tokeniserContextFeatures = tokenFeatureService.getTokeniserContextFeatureSet(tokeniserModel.getFeatureDescriptors(), tokeniserPatternManager.getParsedTestPatterns());
					Tokeniser tokeniser = tokeniserPatternService.getPatternTokeniser(tokeniserPatternManager, tokeniserContextFeatures, tokeniserModel.getDecisionMaker(), beamWidth);
	
					if (includeDetails) {
						String detailsFilePath = outFilePath.substring(0, outFilePath.lastIndexOf(".")) + "_tokeniser_details.txt";
						File detailsFile = new File(detailsFilePath);
						detailsFile.delete();
						AnalysisObserver observer = tokeniserModel.getDetailedAnalysisObserver(detailsFile);
						tokeniser.addObserver(observer);
					}
					
					tokeniser.addTokenFilter(new NumberFilter());
					tokeniser.addTokenFilter(new PrettyQuotesFilter());
					tokeniser.addTokenFilter(new UpperCaseSeriesFilter());
					tokeniser.addTokenFilter(new LowercaseFirstWordFrenchFilter());
	
					this.setTokeniser(tokeniser);
				}
				
				if (this.needsPosTagger()) {				
					LOG.debug("Getting pos-tagger model");
					MachineLearningModel<PosTag> posTaggerModel = null;
					if (posTaggerModelFilePath!=null) {
						posTaggerModel = machineLearningService.getModel(new ZipInputStream(new FileInputStream(posTaggerModelFilePath)));
					} else {
						posTaggerModel = machineLearningService.getModel(this.getDefaultPosTaggerModelStream());
					}
					Set<PosTaggerFeature<?>> posTaggerFeatures = posTaggerFeatureService.getFeatureSet(posTaggerModel.getFeatureDescriptors());
					PosTagger posTagger = posTaggerService.getPosTagger(posTaggerFeatures, posTagSet, posTaggerModel.getDecisionMaker(), beamWidth);
					
					if (!this.needsTokeniser()) {
						posTagger.addPreprocessingFilter(new NumberFilter());
						posTagger.addPreprocessingFilter(new PrettyQuotesFilter());
						posTagger.addPreprocessingFilter(new UpperCaseSeriesFilter());
						posTagger.addPreprocessingFilter(new LowercaseFirstWordFrenchFilter());
					}
					posTagger.addPreprocessingFilter(new EmptyTokenAfterDuFilter());
					posTagger.addPreprocessingFilter(new EmptyTokenBeforeDuquelFilter());
					posTagger.setPosTaggerRules(this.getPosTaggerRules());
			
					if (includeDetails) {
						String detailsFilePath = outFilePath.substring(0, outFilePath.lastIndexOf(".")) + "_posTagger_details.txt";
						File detailsFile = new File(detailsFilePath);
						detailsFile.delete();
						AnalysisObserver observer = posTaggerModel.getDetailedAnalysisObserver(detailsFile);
						posTagger.addObserver(observer);
					}
					
					this.setPosTagger(posTagger);
				}
				
				if (this.needsParser()) {
					LOG.debug("Getting parser model");
					MachineLearningModel<Transition> parserModel = null;
					if (parserModelFilePath!=null) {
						parserModel = machineLearningService.getModel(new ZipInputStream(new FileInputStream(parserModelFilePath)));
					} else {
						parserModel = machineLearningService.getModel(this.getDefaultParserModelStream());
					}
					NonDeterministicParser parser = parserService.getTransitionBasedParser(parserModel, beamWidth);
	
					if (includeDetails) {
						String detailsFilePath = outFilePath.substring(0, outFilePath.lastIndexOf(".")) + "_parser_details.txt";
						File detailsFile = new File(detailsFilePath);
						detailsFile.delete();
						AnalysisObserver observer = parserModel.getDetailedAnalysisObserver(detailsFile);
						parser.addObserver(observer);
					}
					TalismaneSession.setTransitionSystem(parser.getTransitionSystem());

					this.setParser(parser);				
				}

				this.setPropagateBeam(propagateBeam);
	
				ParseConfigurationProcessor parseConfigurationProcessor = null;
				if (endModule.equals(Talismane.Module.Parser)) {
					if (this.getParseConfigurationProcessor()==null) {
						Reader templateReader = null;
						if (templatePath==null) {
							templateReader = new BufferedReader(new InputStreamReader(getInputStreamFromResource(parserTemplateName)));
						} else {
							templateReader = new BufferedReader(new FileReader(new File(templatePath)));
						}
						FreemarkerTemplateWriter templateWriter = new FreemarkerTemplateWriter(writer, templateReader);
						this.setParseConfigurationProcessor(templateWriter);
					}
					this.setParseConfigurationProcessor(this.getParseConfigurationProcessor());
				} else if (endModule.equals(Talismane.Module.PosTagger)) {
					Reader templateReader = null;
					if (templatePath==null) {
						templateReader = new BufferedReader(new InputStreamReader(getInputStreamFromResource(posTaggerTemplateName)));
					} else {
						templateReader = new BufferedReader(new FileReader(new File(templatePath)));
					}
					FreemarkerTemplateWriter templateWriter = new FreemarkerTemplateWriter(writer, templateReader);
					this.setPosTagSequenceProcessor(templateWriter);
				} else if (endModule.equals(Talismane.Module.Tokeniser)) {
					Reader templateReader = null;
					if (templatePath==null) {
						templateReader = new BufferedReader(new InputStreamReader(getInputStreamFromResource(tokeniserTemplateName)));
					} else {
						templateReader = new BufferedReader(new FileReader(new File(templatePath)));
					}
					FreemarkerTemplateWriter templateWriter = new FreemarkerTemplateWriter(writer, templateReader);
					this.setTokenSequenceProcessor(templateWriter);
				} else if (endModule.equals(Talismane.Module.SentenceDetector)) {
					Reader templateReader = null;
					if (templatePath==null) {
						templateReader = new BufferedReader(new InputStreamReader(getInputStreamFromResource(sentenceTemplateName)));
					} else {
						templateReader = new BufferedReader(new FileReader(new File(templatePath)));
					}
					FreemarkerTemplateWriter templateWriter = new FreemarkerTemplateWriter(writer, templateReader);
					this.setSentenceProcessor(templateWriter);
				}
				
				try {
					this.process(reader);
				} finally {
					writer.flush();
					writer.close();
					
					if (parseConfigurationProcessor!=null) {
						parseConfigurationProcessor.onCompleteParse();
					}
				}
			} else if (command.equals(Command.evaluate)) {
				if (outDirPath.length()==0)
					throw new RuntimeException("Missing argument: outdir");
				
				File outDir = new File(outDirPath);
				outDir.mkdirs();

				if (module.equals(Talismane.Module.PosTagger)) {
					PosTagRegexBasedCorpusReader corpusReader = posTaggerService.getRegexBasedCorpusReader(reader);
					if (inputRegex!=null)
						corpusReader.setRegex(inputRegex);
					
					corpusReader.addTokenFilter(new NumberFilter());
					corpusReader.addTokenFilter(new PrettyQuotesFilter());
					corpusReader.addTokenFilter(new UpperCaseSeriesFilter());
					corpusReader.addTokenFilter(new LowercaseFirstWordFrenchFilter());
					corpusReader.addTokenFilter(new EmptyTokenAfterDuFilter());
					corpusReader.addTokenFilter(new EmptyTokenBeforeDuquelFilter());

					String baseName = "PosTaggerEval";
					if (inFilePath!=null) {
						if (inFilePath.indexOf('.')>0)
							baseName = inFilePath.substring(inFilePath.lastIndexOf('/')+1, inFilePath.indexOf('.'));
						else
							baseName = inFilePath.substring(inFilePath.lastIndexOf('/')+1);
					} else if (posTaggerModelFilePath!=null) {
						if (posTaggerModelFilePath.indexOf('.')>0)
							baseName = posTaggerModelFilePath.substring(posTaggerModelFilePath.lastIndexOf('/')+1, posTaggerModelFilePath.indexOf('.'));
						else
							baseName = posTaggerModelFilePath.substring(posTaggerModelFilePath.lastIndexOf('/')+1);
					} 

					Writer csvFileWriter = null;
					boolean includeSentences = true;
					if (includeSentences) {
						File csvFile = new File(outDir, baseName + "_sentences.csv");
						csvFile.delete();
						csvFile.createNewFile();
						csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false),"UTF8"));
					}
					FScoreCalculator<String> fScoreCalculator = null;
					
					try {
						MachineLearningModel<PosTag> posTaggerModel = null;
						if (posTaggerModelFilePath!=null) {
							posTaggerModel = machineLearningService.getModel(new ZipInputStream(new FileInputStream(posTaggerModelFilePath)));
						} else {
							posTaggerModel = machineLearningService.getModel(this.getDefaultPosTaggerModelStream());
						}
						Set<PosTaggerFeature<?>> posTaggerFeatures = posTaggerFeatureService.getFeatureSet(posTaggerModel.getFeatureDescriptors());
						PosTagger posTagger = posTaggerService.getPosTagger(posTaggerFeatures, posTagSet, posTaggerModel.getDecisionMaker(), beamWidth);

						posTagger.setPosTaggerRules(this.getPosTaggerRules());

						if (includeDetails) {
							String detailsFilePath = baseName + "_posTagger_details.txt";
							File detailsFile = new File(outDir, detailsFilePath);
							detailsFile.delete();
							AnalysisObserver observer = posTaggerModel.getDetailedAnalysisObserver(detailsFile);
							posTagger.addObserver(observer);
						}
						
						PosTaggerEvaluator evaluator = posTaggerService.getPosTaggerEvaluator(posTagger, csvFileWriter);
						evaluator.setPropagateBeam(propagateBeam);
						
						fScoreCalculator = evaluator.evaluate(corpusReader);
						
						double unknownLexiconFScore = evaluator.getFscoreUnknownInLexicon().getTotalFScore();
						LOG.debug("F-score for words unknown in lexicon " + posTaggerModelFilePath + ": " + unknownLexiconFScore);
						if (evaluator.getFscoreUnknownInCorpus()!=null) {
							double unknownCorpusFScore = evaluator.getFscoreUnknownInCorpus().getTotalFScore();
							LOG.debug("F-score for words unknown in corpus " + posTaggerModelFilePath + ": " + unknownCorpusFScore);
						}
						
						double fscore = fScoreCalculator.getTotalFScore();
						LOG.debug("F-score for " + posTaggerModelFilePath + ": " + fscore);
									
					} finally {
						if (csvFileWriter!=null) {
							csvFileWriter.flush();
							csvFileWriter.close();
						}
					}
					
					File fscoreFile = new File(outDir, baseName + ".fscores.csv");
					fScoreCalculator.writeScoresToCSVFile(fscoreFile);
				} else if (module.equals(Module.Parser)) {
					ParserRegexBasedCorpusReader corpusReader = parserService.getRegexBasedCorpusReader(reader);
					if (inputRegex!=null)
						corpusReader.setRegex(inputRegex);
					
					corpusReader.addTokenFilter(new NumberFilter());
					corpusReader.addTokenFilter(new PrettyQuotesFilter());
					corpusReader.addTokenFilter(new UpperCaseSeriesFilter());
					corpusReader.addTokenFilter(new LowercaseFirstWordFrenchFilter());
					corpusReader.addTokenFilter(new EmptyTokenAfterDuFilter());
					corpusReader.addTokenFilter(new EmptyTokenBeforeDuquelFilter());

					String baseName = "ParserEval";
					if (inFilePath!=null) {
						if (inFilePath.indexOf('.')>0)
							baseName = inFilePath.substring(inFilePath.lastIndexOf('/')+1, inFilePath.indexOf('.'));
						else
							baseName = inFilePath.substring(inFilePath.lastIndexOf('/')+1);
					} else if (parserModelFilePath!=null) {
						if (parserModelFilePath.indexOf('.')>0)
							baseName = parserModelFilePath.substring(parserModelFilePath.lastIndexOf('/')+1, parserModelFilePath.indexOf('.'));
						else
							baseName = parserModelFilePath.substring(parserModelFilePath.lastIndexOf('/')+1);
					} 

					Writer csvFileWriter = null;
					boolean includeSentences = true;
					if (includeSentences) {
						File csvFile = new File(outDir, baseName + "_sentences.csv");
						csvFile.delete();
						csvFile.createNewFile();
						csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false),"UTF8"));
					}
					FScoreCalculator<String> fScoreCalculator = null;
					
					try {
						MachineLearningModel<Transition> parserModel = null;
						if (posTaggerModelFilePath!=null) {
							parserModel = machineLearningService.getModel(new ZipInputStream(new FileInputStream(parserModelFilePath)));
						} else {
							parserModel = machineLearningService.getModel(this.getDefaultParserModelStream());
						}
						TalismaneSession.setTransitionSystem((TransitionSystem) parserModel.getDecisionMaker().getDecisionFactory());
						Set<ParseConfigurationFeature<?>> parserFeatures = parserFeatureService.getFeatures(parserModel.getFeatureDescriptors());
						Parser parser = parserService.getTransitionBasedParser(parserModel.getDecisionMaker(), TalismaneSession.getTransitionSystem(), parserFeatures, beamWidth);
						
						if (includeDetails) {
							String detailsFilePath = baseName + "_posTagger_details.txt";
							File detailsFile = new File(outDir, detailsFilePath);
							detailsFile.delete();
							AnalysisObserver observer = parserModel.getDetailedAnalysisObserver(detailsFile);
							parser.addObserver(observer);
						}
						
						ParserEvaluator evaluator = parserService.getParserEvaluator();
						evaluator.setParser(parser);
						evaluator.setCsvFileWriter(csvFileWriter);
						
						fScoreCalculator = evaluator.evaluate(corpusReader);
						
						double fscore = fScoreCalculator.getTotalFScore();
						LOG.debug("F-score for " + posTaggerModelFilePath + ": " + fscore);
									
					} finally {
						if (csvFileWriter!=null) {
							csvFileWriter.flush();
							csvFileWriter.close();
						}
					}
					
					File fscoreFile = new File(outDir, baseName + ".fscores.csv");
					fScoreCalculator.writeScoresToCSVFile(fscoreFile);
				} else {
					throw new TalismaneException("The module " + module + " is not yet supported for evaluation.");
				}
			}
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw e;
		} finally {
			PerformanceMonitor.end();
			
			if (logPerformance) {
				try {
					Writer csvFileWriter = null;
					File csvFile = null;
					if (outDirPath!=null) {
						File outDir = new File(outDirPath);
			
						csvFile = new File(outDir, "performance.csv");
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
	public boolean needsSentenceDetector() {
		return startModule.compareTo(Module.SentenceDetector)<=0 && endModule.compareTo(Module.SentenceDetector)>=0;
	}
	@Override
	public boolean needsTokeniser() {
		return startModule.compareTo(Module.Tokeniser)<=0 && endModule.compareTo(Module.Tokeniser)>=0;
	}
	@Override
	public boolean needsPosTagger() {
		return startModule.compareTo(Module.PosTagger)<=0 && endModule.compareTo(Module.PosTagger)>=0;
	}
	@Override
	public boolean needsParser() {
		return startModule.compareTo(Module.Parser)<=0 && endModule.compareTo(Module.Parser)>=0;
	}
	
	/**
	 * Process the reader from a given start module to a given end module, where the modules available are:
	 * Sentence detector, Tokeniser, Pos tagger, Parser.<br/>
	 * A fixed input format is expected depending on the start module:<br/>
	 * <li>Sentence detector: newlines indicate sentence breaks, but can have multiple sentences per paragraph.</li>
	 * <li>Tokeniser: expect exactly one sentence per newline.</li>
	 * <li>Pos tagger: expect one token per line and empty line to indicate sentence breaks. Empty tokens are indicated by an underscore.</li>
	 * <li>Parser: each line should start with token-tab-postag and end with a newline.</li>
	 * @param reader
	 */
	@Override
	public void process(Reader reader) {
		try {
			if (this.needsSentenceDetector()) {
				if (sentenceDetector==null) {
					throw new TalismaneException("Sentence detector not provided.");
				}
			}
			if (this.needsTokeniser()) {
				if (tokeniser==null) {
					throw new TalismaneException("Tokeniser not provided.");
				}
			}
			if (this.needsPosTagger()) {
				if (posTagger==null) {
					throw new TalismaneException("Pos-tagger not provided.");
				}
			}
			if (this.needsParser()) {
				if (parser==null) {
					throw new TalismaneException("Parser not provided.");
				}
			}
			
			if (endModule.equals(Module.SentenceDetector)) {
				if (sentenceProcessor==null) {
					throw new TalismaneException("No sentence processor provided with sentence detector end module, cannot generate output.");
				}
			}
			if (endModule.equals(Module.Tokeniser)) {
				if (tokenSequenceProcessor==null) {
					throw new TalismaneException("No token sequence processor provided with tokeniser end module, cannot generate output.");
				}
			}
			if (endModule.equals(Module.PosTagger)) {
				if (posTagSequenceProcessor==null) {
					throw new TalismaneException("No postag sequence processor provided with pos-tagger end module, cannot generate output.");
				}
			}
			if (endModule.equals(Module.Parser)) {
				if (parseConfigurationProcessor==null) {
					throw new TalismaneException("No parse configuration processor provided with parser end module, cannot generate output.");
				}
			}
			
			LinkedList<String> textSegments = new LinkedList<String>();
			LinkedList<String> sentences = new LinkedList<String>();
			TokenSequence tokenSequence = null;
			PosTagSequence posTagSequence = null;
			
			if (this.needsSentenceDetector()) {
				// prime the sentence detector with two text segments, to ensure everything gets processed
				textSegments.addLast("\n");
				textSegments.addLast("\n");
			}
			
			Scanner scanner = null;
			TokenRegexBasedCorpusReader tokenCorpusReader = null;
			PosTagRegexBasedCorpusReader posTagCorpusReader = null;
			if (startModule.equals(Module.Tokeniser)) {
				scanner = new Scanner(reader);
			} else if (startModule.equals(Module.PosTagger)) {
				tokenCorpusReader = this.tokeniserService.getRegexBasedCorpusReader(reader);
				if (inputRegex!=null)
					tokenCorpusReader.setRegex(inputRegex);

			} else if (startModule.equals(Module.Parser)) {
				posTagCorpusReader = this.posTaggerService.getRegexBasedCorpusReader(reader);
				if (inputRegex!=null)
					posTagCorpusReader.setRegex(inputRegex);
			}
			
		    StringBuilder stringBuilder = new StringBuilder();
		    String leftovers = null;
		    boolean finished = false;
		    
		    char lastChar = 0;
		    while (!finished) {
		    	if (startModule.equals(Module.SentenceDetector)) {
				    // read characters from the reader, one at a time
			    	char c;
			    	int r = reader.read();
			    	if (r==-1) {
			    		finished = true;
			    		c = '\n';
			    	} else {
		    			c = (char) r;
			    	}
			    	
	    			if (c=='\n' && lastChar=='\r') {
	    				// Windows newline character CR+LF - skip the second one.
				    	lastChar = c;
				    	continue;
	    			}
	    			
	    			lastChar = c;
	    			// normalise the newline character
	    			if (c=='\f' || c=='\r')
			    		c = '\n';
	    			
	    			// have sentence detector
		    		if (finished || (Character.isWhitespace(c) && stringBuilder.length()>MIN_BLOCK_SIZE)) {
		    			if (stringBuilder.length()>0) {
			    			String textSegment = stringBuilder.toString();
			    			stringBuilder = new StringBuilder();
			    			
		    				textSegments.add(textSegment);
		    			} // is the current block > 0 characters?
		    		} // is there a next block available?
		    		
		    		if (finished) {
		    			if (stringBuilder.length()>0) {
		    				textSegments.addLast(stringBuilder.toString());
		    				stringBuilder = new StringBuilder();
		    			}
						textSegments.addLast("\n");
						textSegments.addLast("\n");
		    		}
		    		
		    		stringBuilder.append(c);
		    		
					while (textSegments.size()>=3) {
						String prevText = textSegments.removeFirst();
						String text = textSegments.removeFirst();
						String nextText = textSegments.removeFirst();
						
						for (TextStreamFilter textStreamFilter : textStreamFilters) {
							List<String> result = textStreamFilter.apply(prevText, text, nextText);
							prevText = result.get(0);
							text = result.get(1);
							nextText = result.get(2);
						}
						
						// push the updated texts back onto the beginning of Deque
						textSegments.addFirst(nextText);
						textSegments.addFirst(text);
						
						List<Integer> sentenceBreaks = sentenceDetector.detectSentences(prevText, text, nextText);
						if (LOG.isTraceEnabled()) {
							LOG.trace("prevText: " + prevText);
							LOG.trace("text: " + text);
							LOG.trace("nextText: " + nextText);							
						}
						int lastBreak = 0;
						for (int sentenceBreak : sentenceBreaks) {
							int currentBreak = (sentenceBreak-prevText.length())+1;
							if (LOG.isTraceEnabled()) {
								LOG.trace("currentBreak = (sentenceBreak-prevText.length())+1 = (" + sentenceBreak + "-" + prevText.length() + ")+1 = " + currentBreak);
							}
							String sentence = text.substring(lastBreak, currentBreak).replace("\n", "");
							if (leftovers!=null) {
								sentence = leftovers + sentence;
								leftovers = null;
							}
							sentence = sentence.trim();
							if (sentence.length()>0)
								sentences.add(sentence);
							lastBreak = currentBreak;
						}
						if (lastBreak<text.length()) {
							// is there anything left to add?
							if (nextText.length()==0) {
								String leftover = text.substring(lastBreak).replace("\n", "").trim();
								if (leftover.length()>0)
									sentences.add(leftover);
							} else {
								leftovers = text.substring(lastBreak);
							}
						} // something left to add
					} // we have at least 3 text segments (should always be the case once we get started)
				} else if (startModule.equals(Module.Tokeniser)) {
	    			// assume there's one sentence per line
					if (scanner.hasNextLine()) {
						String sentence = scanner.nextLine();
						sentences.add(sentence);
					} else {
						finished = true;
					}
	    		} else if (startModule.equals(Module.PosTagger)) {
	    			if (tokenCorpusReader.hasNextTokenSequence()) {
	    				tokenSequence = tokenCorpusReader.nextTokenSequence();
	    			} else {
	    				tokenSequence = null;
	    				finished = true;
	    			}
	    		} else if (startModule.equals(Module.Parser)) {
	    			if (posTagCorpusReader.hasNextPosTagSequence()) {
	    				posTagSequence = posTagCorpusReader.nextPosTagSequence();
	    			} else {
	    				posTagSequence = null;
	    				finished = true;
	    			}
	    		} // which start module?
	    		
	    		boolean needToProcess = false;
	    		if (startModule.equals(Module.SentenceDetector)||startModule.equals(Module.Tokeniser))
	    			needToProcess = !sentences.isEmpty();
	    		else if (startModule.equals(Module.PosTagger))
	    			needToProcess = tokenSequence!=null;
	    		else if (startModule.equals(Module.Parser))
	    			needToProcess = posTagSequence!=null;
	    		
	    		while (needToProcess) {
	    			String sentence = null;
	    			if (startModule.compareTo(Module.Tokeniser)<=0 && endModule.compareTo(Module.SentenceDetector)>=0) {
		    			sentence = sentences.poll();
		    			LOG.debug("Sentence: " + sentence);
		    			if (sentenceProcessor!=null)
		    				sentenceProcessor.process(sentence);
	    			} // need to read next sentence
	    			
	    			List<TokenSequence> tokenSequences = null;
	    			if (this.needsTokeniser()) {
	    				tokenSequences = tokeniser.tokenise(sentence);
    					tokenSequence = tokenSequences.get(0);
	    				
    					if (tokenSequenceProcessor!=null) {
    						tokenSequenceProcessor.process(tokenSequence);
    					}
	    			} // need to tokenise ?
	    			
	    			List<PosTagSequence> posTagSequences = null;
 	    			if (this.needsPosTagger()) {
    					posTagSequence = null;
    					if (tokenSequences==null||!propagateBeam) {
    						tokenSequences = new ArrayList<TokenSequence>();
    						tokenSequences.add(tokenSequence);
    					}

	    				if (posTagger instanceof NonDeterministicPosTagger) {
	    					NonDeterministicPosTagger nonDeterministicPosTagger = (NonDeterministicPosTagger) posTagger;
	    					posTagSequences = nonDeterministicPosTagger.tagSentence(tokenSequences);
	    					posTagSequence = posTagSequences.get(0);
	    				} else {
	    					posTagSequence = posTagger.tagSentence(tokenSequence);
	    				}
	    				
	    				if (posTagSequenceProcessor!=null) {
	    					posTagSequenceProcessor.process(posTagSequence);
	    				}

	    				tokenSequence = null;
 	    			} // need to postag
 	    			
	    			if (this.needsParser()) {
    					if (posTagSequences==null||!propagateBeam) {
    						posTagSequences = new ArrayList<PosTagSequence>();
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
	    					
	    					if (parseConfigurationProcessor!=null) {
	    						parseConfigurationProcessor.onNextParseConfiguration(parseConfiguration);
	    					}
    					} catch (Exception e) {
    						LOG.error(e);
    						if (stopOnError)
    							throw new RuntimeException(e);
    					}
    					posTagSequence = null;
    				} // need to parse
	    			
		    		if (startModule.equals(Module.SentenceDetector)||startModule.equals(Module.Tokeniser))
		    			needToProcess = !sentences.isEmpty();
		    		else if (startModule.equals(Module.PosTagger))
		    			needToProcess = tokenSequence!=null;
		    		else if (startModule.equals(Module.Parser))
		    			needToProcess = posTagSequence!=null;
	    		} // next sentence
			} // next character
		} catch (IOException ioe) {
			LOG.error(ioe);
			throw new RuntimeException(ioe);
		} finally {
			try {
				reader.close();
			} catch (IOException ioe2) {
				LOG.error(ioe2);
				throw new RuntimeException(ioe2);
			}
		}
	}

	void addToken(PretokenisedSequence pretokenisedSequence, String token) {
		if (token.equals("_")) {
			pretokenisedSequence.addToken("");
		} else {
			if (pretokenisedSequence.size()==0) {
				// do nothing
			} else if (pretokenisedSequence.get(pretokenisedSequence.size()-1).getText().endsWith("'")) {
				// do nothing
			} else if (token.equals(".")||token.equals(",")||token.equals(")")||token.equals("]")) {
				// do nothing
			} else {
				// add a space
				pretokenisedSequence.addToken(" ");
			}
			pretokenisedSequence.addToken(token.replace("_", " "));
		}
	}
	
	@Override
	public SentenceDetector getSentenceDetector() {
		return sentenceDetector;
	}

	@Override
	public void setSentenceDetector(SentenceDetector sentenceDetector) {
		this.sentenceDetector = sentenceDetector;
	}

	@Override
	public Tokeniser getTokeniser() {
		return tokeniser;
	}

	@Override
	public void setTokeniser(Tokeniser tokeniser) {
		this.tokeniser = tokeniser;
	}

	@Override
	public PosTagger getPosTagger() {
		return posTagger;
	}

	@Override
	public void setPosTagger(PosTagger posTagger) {
		this.posTagger = posTagger;
	}

	@Override
	public Parser getParser() {
		return parser;
	}

	@Override
	public void setParser(Parser parser) {
		this.parser = parser;
	}

	@Override
	public List<TextStreamFilter> getTextStreamFilters() {
		return textStreamFilters;
	}

	@Override
	public void setTextStreamFilters(List<TextStreamFilter> textStreamFilters) {
		this.textStreamFilters = textStreamFilters;
	}

	@Override
	public void addTextStreamFilter(TextStreamFilter textStreamFilter) {
		this.textStreamFilters.add(textStreamFilter);
	}

	@Override
	public boolean isPropagateBeam() {
		return propagateBeam;
	}

	@Override
	public void setPropagateBeam(boolean propagateBeam) {
		this.propagateBeam = propagateBeam;
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

	public Module getStartModule() {
		return startModule;
	}

	public Module getEndModule() {
		return endModule;
	}

	public void setStartModule(Module startModule) {
		this.startModule = startModule;
	}

	public void setEndModule(Module endModule) {
		this.endModule = endModule;
	}

	public PosTaggerLexiconService getLexiconService() {
		if (this.lexiconService==null)
			this.lexiconService = this.getDefaultLexiconService();
		return lexiconService;
	}

	public void setLexiconService(PosTaggerLexiconService lexiconService) {
		this.lexiconService = lexiconService;
	}
	protected abstract PosTaggerLexiconService getDefaultLexiconService();
	protected abstract InputStream getDefaultPosTagSetFromStream();
	protected abstract InputStream getDefaultPosTaggerRulesFromStream();
	protected abstract ZipInputStream getDefaultSentenceModelStream();
	protected abstract ZipInputStream getDefaultTokeniserModelStream();
	protected abstract ZipInputStream getDefaultPosTaggerModelStream();
	protected abstract ZipInputStream getDefaultParserModelStream();
	
	private static InputStream getInputStreamFromResource(String resource) {
		String path = "/com/joliciel/talismane/output/" + resource;
		InputStream inputStream = Talismane.class.getResourceAsStream(path); 
		
		return inputStream;
	}

	public String getInputRegex() {
		return inputRegex;
	}

	public void setInputRegex(String inputRegex) {
		this.inputRegex = inputRegex;
	}

	public List<PosTaggerRule> getPosTaggerRules() {
		return posTaggerRules;
	}

	public void setPosTaggerRules(List<PosTaggerRule> posTaggerRules) {
		this.posTaggerRules = posTaggerRules;
	}
	
	
}
