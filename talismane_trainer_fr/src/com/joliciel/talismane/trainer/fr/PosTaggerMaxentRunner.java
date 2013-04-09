package com.joliciel.talismane.trainer.fr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.frenchTreebank.TreebankReader;
import com.joliciel.frenchTreebank.TreebankService;
import com.joliciel.frenchTreebank.TreebankServiceLocator;
import com.joliciel.frenchTreebank.TreebankSubSet;
import com.joliciel.frenchTreebank.export.FtbPosTagMapper;
import com.joliciel.frenchTreebank.export.TreebankExportService;
import com.joliciel.frenchTreebank.upload.TreebankUploadService;
import com.joliciel.ftbDep.FtbDepReader;
import com.joliciel.talismane.Talismane;
import com.joliciel.talismane.TalismaneServiceLocator;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.lexicon.LexiconChain;
import com.joliciel.talismane.lexicon.LexiconDeserializer;
import com.joliciel.talismane.lexicon.PosTaggerLexicon;
import com.joliciel.talismane.machineLearning.AnalysisObserver;
import com.joliciel.talismane.machineLearning.CorpusEventStream;
import com.joliciel.talismane.machineLearning.ExternalResourceFinder;
import com.joliciel.talismane.machineLearning.MachineLearningModel;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.machineLearning.ModelTrainer;
import com.joliciel.talismane.machineLearning.TextFileResource;
import com.joliciel.talismane.machineLearning.MachineLearningModel.MachineLearningAlgorithm;
import com.joliciel.talismane.machineLearning.linearsvm.LinearSVMModelTrainer;
import com.joliciel.talismane.machineLearning.linearsvm.LinearSVMModelTrainer.LinearSVMSolverType;
import com.joliciel.talismane.machineLearning.maxent.MaxentModelTrainer;
import com.joliciel.talismane.machineLearning.maxent.PerceptronModelTrainer;
import com.joliciel.talismane.parser.ParserService;
import com.joliciel.talismane.parser.ParserServiceLocator;
import com.joliciel.talismane.parser.TransitionSystem;
import com.joliciel.talismane.posTagger.NonDeterministicPosTagger;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagEvaluationFScoreCalculator;
import com.joliciel.talismane.posTagger.PosTagEvaluationSentenceWriter;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.posTagger.PosTagAnnotatedCorpusReader;
import com.joliciel.talismane.posTagger.PosTagger;
import com.joliciel.talismane.posTagger.PosTaggerEvaluator;
import com.joliciel.talismane.posTagger.PosTaggerGuessTemplateWriter;
import com.joliciel.talismane.posTagger.PosTaggerService;
import com.joliciel.talismane.posTagger.features.PosTaggerFeature;
import com.joliciel.talismane.posTagger.features.PosTaggerFeatureService;
import com.joliciel.talismane.posTagger.features.PosTaggerRule;
import com.joliciel.talismane.posTagger.filters.PosTagFilterService;
import com.joliciel.talismane.posTagger.filters.PosTagSequenceFilter;
import com.joliciel.talismane.stats.FScoreCalculator;
import com.joliciel.talismane.tokeniser.Tokeniser;
import com.joliciel.talismane.tokeniser.TokeniserOutcome;
import com.joliciel.talismane.tokeniser.TokeniserService;
import com.joliciel.talismane.tokeniser.TokeniserServiceLocator;
import com.joliciel.talismane.tokeniser.features.TokenFeatureService;
import com.joliciel.talismane.tokeniser.features.TokeniserContextFeature;
import com.joliciel.talismane.tokeniser.filters.TokenFilter;
import com.joliciel.talismane.tokeniser.filters.TokenFilterService;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;
import com.joliciel.talismane.tokeniser.patterns.TokeniserPatternManager;
import com.joliciel.talismane.tokeniser.patterns.TokeniserPatternService;
import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.PerformanceMonitor;

public class PosTaggerMaxentRunner {
    private static final Log LOG = LogFactory.getLog(PosTaggerMaxentRunner.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		Map<String, String> argMap = new HashMap<String, String>();
		
		for (String arg : args) {
			int equalsPos = arg.indexOf('=');
			String argName = arg.substring(0, equalsPos);
			String argValue = arg.substring(equalsPos+1);
			argMap.put(argName, argValue);
		}
		
		@SuppressWarnings("unused")
		PosTaggerMaxentRunner maxentRunner = new PosTaggerMaxentRunner(argMap);
	}

    public PosTaggerMaxentRunner(Map<String,String> argMap) throws Exception {
		String command = null;

		String corpusType = "ftb";
		String posTaggerModelFilePath = "";
		String tokeniserModelFilePath = "";
		String posTaggerFeatureFilePath = "";
		String posTaggerRuleFilePath = "";
		String tokenFilterPath = "";
		String tokenSequenceFilterPath = "";
		String posTaggerPreProcessingFilterPath = "";
		String posTaggerPostProcessingFilterPath = "";
		String outDirPath = "";
		int iterations = 0;
		int cutoff = 0;
		int sentenceCount = 0;
		int startSentence = 0;
		int beamWidth = 10;
		boolean propagateBeam = true;
		String posTagSetPath = "";
		String posTagMapPath = "";
		String corpusPath = "";
		String lexiconDirPath = "";
		boolean includeDetails = false;
		boolean includeSentences = true;
		String suffix = "";

		MachineLearningAlgorithm algorithm = MachineLearningAlgorithm.MaxEnt;
		int outputGuessCount = 0;
		
		double constraintViolationCost = -1;
		double epsilon = -1;
		LinearSVMSolverType solverType = null;
		boolean perceptronAveraging = false;
		boolean perceptronSkippedAveraging = false;
		double perceptronTolerance = -1;

		boolean useCompoundPosTags = false;
		String externalResourcePath = null;
		
		File performanceConfigFile = null;

		for (Entry<String, String> argEntry : argMap.entrySet()) {
			String argName = argEntry.getKey();
			String argValue = argEntry.getValue();
			if (argName.equals("command"))
				command = argValue;
			else if (argName.equals("posTaggerModel"))
				posTaggerModelFilePath = argValue;
			else if (argName.equals("posTaggerFeatures")) 
				posTaggerFeatureFilePath = argValue;
			else if (argName.equals("posTaggerRules")) 
				posTaggerRuleFilePath = argValue;
			else if (argName.equals("tokenFilters"))
				tokenFilterPath = argValue;
			else if (argName.equals("tokenSequenceFilters"))
				tokenSequenceFilterPath = argValue;
			else if (argName.equals("posTaggerPreProcessingFilters"))
				posTaggerPreProcessingFilterPath = argValue;
			else if (argName.equals("posTaggerPostProcessingFilters"))
				posTaggerPostProcessingFilterPath = argValue;
			else if (argName.equals("posTagSet"))
				posTagSetPath = argValue;
			else if (argName.equals("posTagMap"))
				posTagMapPath = argValue;
			else if (argName.equals("corpus"))
				corpusPath = argValue;
			else if (argName.equals("lexiconDir"))
				lexiconDirPath = argValue;
			else if (argName.equals("tokeniserModel")) 
				tokeniserModelFilePath = argValue;
			else if (argName.equals("iterations"))
				iterations = Integer.parseInt(argValue);
			else if (argName.equals("cutoff"))
				cutoff = Integer.parseInt(argValue);
			else if (argName.equals("outdir")) 
				outDirPath = argValue;
			else if (argName.equals("sentenceCount"))
				sentenceCount = Integer.parseInt(argValue);
			else if (argName.equals("startSentence"))
				startSentence = Integer.parseInt(argValue);
			else if (argName.equals("beamWidth"))
				beamWidth = Integer.parseInt(argValue);
			else if (argName.equals("propagateBeam"))
				propagateBeam = argValue.equals("true");
			else if (argName.equals("includeSentences"))
				includeSentences = argValue.equalsIgnoreCase("true");
			else if (argName.equals("includeDetails"))
				includeDetails = argValue.equalsIgnoreCase("true");
			else if (argName.equals("algorithm"))
				algorithm = MachineLearningAlgorithm.valueOf(argValue);
			else if (argName.equals("linearSVMSolver"))
				solverType = LinearSVMSolverType.valueOf(argValue);
			else if (argName.equals("linearSVMCost"))
				constraintViolationCost = Double.parseDouble(argValue);
			else if (argName.equals("linearSVMEpsilon"))
				epsilon = Double.parseDouble(argValue);
			else if (argName.equals("perceptronAveraging"))
				perceptronAveraging = argValue.equalsIgnoreCase("true");
			else if (argName.equals("perceptronSkippedAveraging"))
				perceptronSkippedAveraging = argValue.equalsIgnoreCase("true");
			else if (argName.equals("perceptronTolerance"))
				perceptronTolerance = Double.parseDouble(argValue);
			else if (argName.equals("outputGuessCount"))
				outputGuessCount = Integer.parseInt(argValue);
			else if (argName.equals("corpusReader"))
				corpusType = argValue;
			else if (argName.equals("suffix"))
				suffix = argValue;
			else if (argName.equals("useCompoundPosTags"))
				useCompoundPosTags = argValue.equalsIgnoreCase("true");
			else if (argName.equals("externalResources"))
				externalResourcePath = argValue;
			else if (argName.equals("performanceConfigFile"))
				performanceConfigFile = new File(argValue);
			else
				throw new RuntimeException("Unknown argument: " + argName);
		}
		
		PerformanceMonitor.start(performanceConfigFile);
		try {

			if (lexiconDirPath.length()==0)
				throw new RuntimeException("Missing argument: lexiconDir");
			if (posTagSetPath.length()==0)
				throw new RuntimeException("Missing argument: posTagSet");
			if (posTagMapPath.length()==0)
				throw new RuntimeException("Missing argument: posTagMap");
			
			TalismaneServiceLocator talismaneServiceLocator = TalismaneServiceLocator.getInstance();
			
	        PosTaggerService posTaggerService = talismaneServiceLocator.getPosTaggerServiceLocator().getPosTaggerService();
	        File posTagSetFile = new File(posTagSetPath);
			PosTagSet posTagSet = posTaggerService.getPosTagSet(posTagSetFile);
			PosTagFilterService posTagFilterService = talismaneServiceLocator.getPosTagFilterServiceLocator().getPosTagFilterService();
			
			TalismaneSession.setPosTagSet(posTagSet);
			
			File lexiconDir = new File(lexiconDirPath);
			LexiconDeserializer lexiconDeserializer = new LexiconDeserializer();
			List<PosTaggerLexicon> lexicons = lexiconDeserializer.deserializeLexicons(lexiconDir);
			LexiconChain lexiconChain = new LexiconChain();
			for (PosTaggerLexicon lexicon : lexicons) {
				lexiconChain.addLexicon(lexicon);
			}
	        
        	TalismaneSession.setLexicon(lexiconChain);
	 
	        TokenFeatureService tokenFeatureService = talismaneServiceLocator.getTokenFeatureServiceLocator().getTokenFeatureService();
	        TokeniserPatternService tokeniserPatternService = talismaneServiceLocator.getTokenPatternServiceLocator().getTokeniserPatternService();
	        TokenFilterService tokenFilterService = talismaneServiceLocator.getTokenFilterServiceLocator().getTokenFilterService();
	        TokeniserServiceLocator tokeniserServiceLocator = talismaneServiceLocator.getTokeniserServiceLocator();
	        TokeniserService tokeniserService = tokeniserServiceLocator.getTokeniserService();
			
			ParserServiceLocator parserServiceLocator = talismaneServiceLocator.getParserServiceLocator();
			ParserService parserService = parserServiceLocator.getParserService();

			PosTaggerFeatureService posTaggerFeatureService = talismaneServiceLocator.getPosTaggerFeatureServiceLocator().getPosTaggerFeatureService();

			TreebankServiceLocator treebankServiceLocator = TreebankServiceLocator.getInstance(talismaneServiceLocator);
			if (corpusPath.length()==0)
				treebankServiceLocator.setDataSourcePropertiesFile("jdbc-ftb.properties");
	        
			TreebankService treebankService = treebankServiceLocator.getTreebankService();
	        TreebankUploadService treebankUploadService = treebankServiceLocator.getTreebankUploadServiceLocator().getTreebankUploadService();
			TreebankExportService treebankExportService = treebankServiceLocator.getTreebankExportServiceLocator().getTreebankExportService();
			
			MachineLearningService machineLearningService = talismaneServiceLocator.getMachineLearningServiceLocator().getMachineLearningService();

			PosTagAnnotatedCorpusReader corpusReader = null;
			if (corpusType.equals("ftb")) {
				TreebankReader treebankReader = null;
				
				if (corpusPath.length()>0) {
					File treebankFile = new File(corpusPath);
					treebankReader = treebankUploadService.getXmlReader(treebankFile);
				} else {
					TreebankSubSet trainingSet = TreebankSubSet.TRAINING;
					treebankReader = treebankService.getDatabaseReader(trainingSet, startSentence);
				}
				
				File posTagMapFile = new File(posTagMapPath);
				FtbPosTagMapper ftbPosTagMapper = treebankExportService.getFtbPosTagMapper(posTagMapFile, posTagSet);
				corpusReader = treebankExportService.getPosTagAnnotatedCorpusReader(treebankReader, ftbPosTagMapper, useCompoundPosTags);
			} else if (corpusType.equals("ftbDep")) {
				File corpusFile = new File(corpusPath);
				FtbDepReader ftbDepReader = new FtbDepReader(corpusFile, "UTF-8");
				ftbDepReader.setMaxSentenceCount(sentenceCount);
				ftbDepReader.setParserService(parserService);
				ftbDepReader.setPosTaggerService(posTaggerService);
				ftbDepReader.setTokeniserService(tokeniserService);
				ftbDepReader.setKeepCompoundPosTags(useCompoundPosTags);
				
				// doesn't really matter which transition system, we're just reading postags
				TransitionSystem transitionSystem = parserService.getArcEagerTransitionSystem();
				TalismaneSession.setTransitionSystem(transitionSystem);
				corpusReader = ftbDepReader;
			} else {
				throw new RuntimeException("Unknown corpusReader: " + corpusType);
			}
			corpusReader.setMaxSentenceCount(sentenceCount);
			
			if (command.equals("train")) {
				if (posTaggerModelFilePath.length()==0)
					throw new RuntimeException("Missing argument: posTaggerModel");
				if (posTaggerFeatureFilePath.length()==0)
					throw new RuntimeException("Missing argument: posTaggerFeatures");
				if (posTagMapPath.length()==0)
					throw new RuntimeException("Missing argument: posTagMap");

				String modelDirPath = posTaggerModelFilePath.substring(0, posTaggerModelFilePath.lastIndexOf("/"));
				File modelDir = new File(modelDirPath);
				modelDir.mkdirs();
				File modelFile = new File(posTaggerModelFilePath);
				
				ExternalResourceFinder externalResourceFinder = null;
				if (externalResourcePath!=null) {
					externalResourceFinder = posTaggerFeatureService.getExternalResourceFinder();
					File externalResourceFile = new File (externalResourcePath);
					if (externalResourceFile.isDirectory()) {
						File[] files = externalResourceFile.listFiles();
						for (File resourceFile : files) {
							TextFileResource textFileResource = new TextFileResource(resourceFile);
							externalResourceFinder.addExternalResource(textFileResource);
						}
					} else {
						TextFileResource textFileResource = new TextFileResource(externalResourceFile);
						externalResourceFinder.addExternalResource(textFileResource);
					}
				}
				File posTaggerTokenFeatureFile = new File(posTaggerFeatureFilePath);
				Scanner scanner = new Scanner(posTaggerTokenFeatureFile);
				List<String> featureDescriptors = new ArrayList<String>();
				while (scanner.hasNextLine()) {
					String descriptor = scanner.nextLine();
					featureDescriptors.add(descriptor);
					LOG.debug(descriptor);
				}
				Set<PosTaggerFeature<?>> posTaggerFeatures = posTaggerFeatureService.getFeatureSet(featureDescriptors);
			
				
				List<String> tokenFilterDescriptors = new ArrayList<String>();
				if (tokenFilterPath!=null && tokenFilterPath.length()>0) {
					LOG.debug("From: " + tokenFilterPath);
					File tokenFilterFile = new File(tokenFilterPath);
					Scanner tokenFilterScanner = new Scanner(tokenFilterFile);
					while (tokenFilterScanner.hasNextLine()) {
						String descriptor = tokenFilterScanner.nextLine();
						tokenFilterDescriptors.add(descriptor);
					}
				}
				
				List<TokenFilter> tokenFilters = new ArrayList<TokenFilter>();
				for (String descriptor : tokenFilterDescriptors) {
					LOG.debug(descriptor);
					if (descriptor.length()>0 && !descriptor.startsWith("#")) {
						TokenFilter tokenFilter = tokenFilterService.getTokenFilter(descriptor);
						tokenFilters.add(tokenFilter);
					}
				}
				if (tokenFilters.size()>0) {
					TokenSequenceFilter tokenFilterWrapper = tokenFilterService.getTokenSequenceFilter(tokenFilters);
					corpusReader.addTokenSequenceFilter(tokenFilterWrapper);
				}
				
				List<String> tokenSequenceFilterDescriptors = new ArrayList<String>();
				if (tokenSequenceFilterPath!=null && tokenSequenceFilterPath.length()>0) {
					LOG.debug("From: " + tokenSequenceFilterPath);
					File tokenSequenceFilterFile = new File(tokenSequenceFilterPath);
					Scanner tokenSequenceFilterScanner = new Scanner(tokenSequenceFilterFile);
					while (tokenSequenceFilterScanner.hasNextLine()) {
						String descriptor = tokenSequenceFilterScanner.nextLine();
						tokenSequenceFilterDescriptors.add(descriptor);
					}
				}
				
				for (String descriptor : tokenSequenceFilterDescriptors) {
					LOG.debug(descriptor);
					if (descriptor.length()>0 && !descriptor.startsWith("#")) {
						TokenSequenceFilter tokenSequenceFilter = tokenFilterService.getTokenSequenceFilter(descriptor);
						corpusReader.addTokenSequenceFilter(tokenSequenceFilter);
					}
				}
				
				List<String> posTaggerPreprocessingFilterDescriptors = new ArrayList<String>();
				if (posTaggerPreProcessingFilterPath!=null && posTaggerPreProcessingFilterPath.length()>0) {
					LOG.debug("From: " + tokenSequenceFilterPath);
					File posTaggerPreprocessingFilterFile = new File(posTaggerPreProcessingFilterPath);
					Scanner posTaggerPreprocessingFilterScanner = new Scanner(posTaggerPreprocessingFilterFile);
					while (posTaggerPreprocessingFilterScanner.hasNextLine()) {
						String descriptor = posTaggerPreprocessingFilterScanner.nextLine();
						posTaggerPreprocessingFilterDescriptors.add(descriptor);
					}
				}
				
				for (String descriptor : posTaggerPreprocessingFilterDescriptors) {
					LOG.debug(descriptor);
					if (descriptor.length()>0 && !descriptor.startsWith("#")) {
						TokenSequenceFilter tokenSequenceFilter = tokenFilterService.getTokenSequenceFilter(descriptor);
						corpusReader.addTokenSequenceFilter(tokenSequenceFilter);
					}
				}
				

				List<String> posTaggerPostProcessingFilterDescriptors = new ArrayList<String>();
				if (posTaggerPostProcessingFilterPath!=null && posTaggerPostProcessingFilterPath.length()>0) {
					LOG.debug("From: " + posTaggerPostProcessingFilterPath);
					File posTaggerPostProcessingFilterFile = new File(posTaggerPostProcessingFilterPath);
					Scanner posTaggerPostProcessingFilterScanner = new Scanner(posTaggerPostProcessingFilterFile);
					while (posTaggerPostProcessingFilterScanner.hasNextLine()) {
						String descriptor = posTaggerPostProcessingFilterScanner.nextLine();
						posTaggerPostProcessingFilterDescriptors.add(descriptor);
					}
				}
				
				for (String descriptor : posTaggerPostProcessingFilterDescriptors) {
					LOG.debug(descriptor);
					if (descriptor.length()>0 && !descriptor.startsWith("#")) {
						PosTagSequenceFilter posTagSequenceFilter = posTagFilterService.getPosTagSequenceFilter(descriptor);
						corpusReader.addPosTagSequenceFilter(posTagSequenceFilter);
					}
				}
				
				CorpusEventStream posTagEventStream = posTaggerService.getPosTagEventStream(corpusReader, posTaggerFeatures);
				
				Map<String,Object> trainParameters = new HashMap<String, Object>();
				if (algorithm.equals(MachineLearningAlgorithm.MaxEnt)) {
					trainParameters.put(MaxentModelTrainer.MaxentModelParameter.Iterations.name(), iterations);
					trainParameters.put(MaxentModelTrainer.MaxentModelParameter.Cutoff.name(), cutoff);
				} else if (algorithm.equals(MachineLearningAlgorithm.Perceptron)) {
					trainParameters.put(PerceptronModelTrainer.PerceptronModelParameter.Iterations.name(), iterations);
					trainParameters.put(PerceptronModelTrainer.PerceptronModelParameter.Cutoff.name(), cutoff);
					trainParameters.put(PerceptronModelTrainer.PerceptronModelParameter.UseAverage.name(), perceptronAveraging);
					trainParameters.put(PerceptronModelTrainer.PerceptronModelParameter.UseSkippedAverage.name(), perceptronSkippedAveraging);					
					if (perceptronTolerance>=0)
						trainParameters.put(PerceptronModelTrainer.PerceptronModelParameter.Tolerance.name(), perceptronTolerance);					
				} else if (algorithm.equals(MachineLearningAlgorithm.LinearSVM)) {
					trainParameters.put(LinearSVMModelTrainer.LinearSVMModelParameter.Cutoff.name(), cutoff);
					if (solverType!=null)
						trainParameters.put(LinearSVMModelTrainer.LinearSVMModelParameter.SolverType.name(), solverType);
					if (constraintViolationCost>=0)
						trainParameters.put(LinearSVMModelTrainer.LinearSVMModelParameter.ConstraintViolationCost.name(), constraintViolationCost);
					if (epsilon>=0)
						trainParameters.put(LinearSVMModelTrainer.LinearSVMModelParameter.Epsilon.name(), epsilon);
				}
				
				ModelTrainer<PosTag> trainer = machineLearningService.getModelTrainer(algorithm, trainParameters);

				Map<String,List<String>> descriptors = new HashMap<String, List<String>>();
				descriptors.put(MachineLearningModel.FEATURE_DESCRIPTOR_KEY, featureDescriptors);
				descriptors.put(TokenFilterService.TOKEN_FILTER_DESCRIPTOR_KEY, tokenFilterDescriptors);
				descriptors.put(TokenFilterService.TOKEN_SEQUENCE_FILTER_DESCRIPTOR_KEY, tokenSequenceFilterDescriptors);
				descriptors.put(PosTagFilterService.POSTAG_PREPROCESSING_FILTER_DESCRIPTOR_KEY, posTaggerPreprocessingFilterDescriptors);
				descriptors.put(PosTagFilterService.POSTAG_POSTPROCESSING_FILTER_DESCRIPTOR_KEY, posTaggerPostProcessingFilterDescriptors);
				MachineLearningModel<PosTag> posTaggerModel = trainer.trainModel(posTagEventStream, posTagSet, descriptors);			
				if (externalResourceFinder!=null)
					posTaggerModel.setExternalResources(externalResourceFinder.getExternalResources());
				posTaggerModel.persist(modelFile);

			} else if (command.equals("evaluate")) {
				if (posTaggerModelFilePath.length()==0)
					throw new RuntimeException("Missing argument: posTaggerModel");
				if (outDirPath.length()==0)
					throw new RuntimeException("Missing argument: outdir");
				
				ZipInputStream zis = new ZipInputStream(new FileInputStream(posTaggerModelFilePath));
				MachineLearningModel<PosTag> posTaggerModel = machineLearningService.getModel(zis);
				
				Tokeniser tokeniser = null;
				if (tokeniserModelFilePath.length()>0) {
					ZipInputStream tokeniserZis = new ZipInputStream(new FileInputStream(posTaggerModelFilePath));
					MachineLearningModel<TokeniserOutcome> tokeniserModel = machineLearningService.getModel(tokeniserZis);
					TokeniserPatternManager tokeniserPatternManager =
						tokeniserPatternService.getPatternManager(tokeniserModel.getDescriptors().get("patterns"));
					Set<TokeniserContextFeature<?>> tokeniserContextFeatures = tokenFeatureService.getTokeniserContextFeatureSet(tokeniserModel.getFeatureDescriptors(), tokeniserPatternManager.getParsedTestPatterns());
					tokeniser = tokeniserPatternService.getPatternTokeniser(tokeniserPatternManager, tokeniserContextFeatures, tokeniserModel.getDecisionMaker(), beamWidth);
				
					List<String> tokenFilterDescriptors = tokeniserModel.getDescriptors().get(TokenFilterService.TOKEN_FILTER_DESCRIPTOR_KEY);
					if (tokenFilterDescriptors!=null) {
						for (String descriptor : tokenFilterDescriptors) {
							if (descriptor.length()>0 && !descriptor.startsWith("#")) {
								TokenFilter tokenFilter = tokenFilterService.getTokenFilter(descriptor);
								tokeniser.addTokenFilter(tokenFilter);
							}
						}
					}

					List<String> tokenSequenceFilterDescriptors = tokeniserModel.getDescriptors().get(TokenFilterService.TOKEN_SEQUENCE_FILTER_DESCRIPTOR_KEY);
					if (tokenSequenceFilterDescriptors!=null) {
						for (String descriptor : tokenSequenceFilterDescriptors) {
							if (descriptor.length()>0 && !descriptor.startsWith("#")) {
								TokenSequenceFilter tokenSequenceFilter = tokenFilterService.getTokenSequenceFilter(descriptor);
								tokeniser.addTokenSequenceFilter(tokenSequenceFilter);
							}
						}
					}
				}
					
//				Set<String> unknownWords = treebankService.findUnknownWords(trainingSection, testSection);
				Set<String> unknownWords = new TreeSet<String>();
				String modelName = posTaggerModelFilePath.substring(posTaggerModelFilePath.lastIndexOf('/')+1, posTaggerModelFilePath.indexOf('.'));
				File outDir = new File(outDirPath);
				outDir.mkdirs();

				FScoreCalculator<String> fScoreCalculator = null;
				
				Set<PosTaggerFeature<?>> posTaggerFeatures = posTaggerFeatureService.getFeatureSet(posTaggerModel.getFeatureDescriptors());
				
				PosTagger posTagger = posTaggerService.getPosTagger(posTaggerFeatures, posTaggerModel.getDecisionMaker(), beamWidth);
				
				if (tokeniser==null) {
					// add these filters to the reader as if the tokeniser applied them
					List<String> tokenFilterDescriptors = posTaggerModel.getDescriptors().get(TokenFilterService.TOKEN_FILTER_DESCRIPTOR_KEY);
					if (tokenFilterDescriptors!=null) {
						List<TokenFilter> tokenFilters = new ArrayList<TokenFilter>();
						for (String descriptor : tokenFilterDescriptors) {
							if (descriptor.length()>0 && !descriptor.startsWith("#")) {
								TokenFilter tokenFilter = tokenFilterService.getTokenFilter(descriptor);
								tokenFilters.add(tokenFilter);
							}
						}
						TokenSequenceFilter tokenFilterWrapper = tokenFilterService.getTokenSequenceFilter(tokenFilters);
						corpusReader.addTokenSequenceFilter(tokenFilterWrapper);
					}
					
					List<String> tokenSequenceFilterDescriptors = posTaggerModel.getDescriptors().get(TokenFilterService.TOKEN_SEQUENCE_FILTER_DESCRIPTOR_KEY);
					if (tokenSequenceFilterDescriptors!=null) {
						for (String descriptor : tokenSequenceFilterDescriptors) {
							if (descriptor.length()>0 && !descriptor.startsWith("#")) {
								TokenSequenceFilter tokenSequenceFilter = tokenFilterService.getTokenSequenceFilter(descriptor);
								corpusReader.addTokenSequenceFilter(tokenSequenceFilter);
							}
						}
					}
				}
				
				List<String> posTaggerPreprocessingFilters = posTaggerModel.getDescriptors().get(PosTagFilterService.POSTAG_PREPROCESSING_FILTER_DESCRIPTOR_KEY);
				if (posTaggerPreprocessingFilters!=null) {
					for (String descriptor : posTaggerPreprocessingFilters) {
						if (descriptor.length()>0 && !descriptor.startsWith("#")) {
							TokenSequenceFilter tokenSequenceFilter = tokenFilterService.getTokenSequenceFilter(descriptor);
							corpusReader.addTokenSequenceFilter(tokenSequenceFilter);
							posTagger.addPreProcessingFilter(tokenSequenceFilter);
						}
					}
				}
				

				List<String> posTaggerPostProcessingFilters = posTaggerModel.getDescriptors().get(PosTagFilterService.POSTAG_PREPROCESSING_FILTER_DESCRIPTOR_KEY);
				if (posTaggerPostProcessingFilters!=null) {
					for (String descriptor : posTaggerPostProcessingFilters) {
						if (descriptor.length()>0 && !descriptor.startsWith("#")) {
							PosTagSequenceFilter posTagSequenceFilter = posTagFilterService.getPosTagSequenceFilter(descriptor);
							corpusReader.addPosTagSequenceFilter(posTagSequenceFilter);
							posTagger.addPostProcessingFilter(posTagSequenceFilter);
						}
					}
				}

				if (posTaggerRuleFilePath.length()>0) {
					File posTaggerRuleFile = new File(posTaggerRuleFilePath);
					Scanner scanner = new Scanner(posTaggerRuleFile);
					List<String> ruleDescriptors = new ArrayList<String>();
					while (scanner.hasNextLine()) {
						String ruleDescriptor = scanner.nextLine();
						ruleDescriptors.add(ruleDescriptor);
						LOG.debug(ruleDescriptor);
					}
					List<PosTaggerRule> rules = posTaggerFeatureService.getRules(ruleDescriptors);
					posTagger.setPosTaggerRules(rules);
				}

				if (includeDetails) {
					String detailsFilePath = modelName + "_posTagger_details.txt";
					File detailsFile = new File(outDir, detailsFilePath);
					detailsFile.delete();
					AnalysisObserver observer = posTaggerModel.getDetailedAnalysisObserver(detailsFile);
					posTagger.addObserver(observer);
				}
				
				PosTaggerEvaluator evaluator = posTaggerService.getPosTaggerEvaluator(posTagger);
				
				if (includeSentences) {
					File csvFile = new File(outDir, modelName + "_sentences" + suffix + ".csv");
					csvFile.delete();
					csvFile.createNewFile();
					Writer csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false),"UTF8"));
					int guessCount = 1;
					if (outputGuessCount>0)
						guessCount = outputGuessCount;
					else if (posTagger instanceof NonDeterministicPosTagger)
						guessCount = ((NonDeterministicPosTagger) posTagger).getBeamWidth();
					
					PosTagEvaluationSentenceWriter sentenceWriter = new PosTagEvaluationSentenceWriter(csvFileWriter, guessCount);
					evaluator.addObserver(sentenceWriter);
				}
				
				PosTagEvaluationFScoreCalculator posTagFScoreCalculator = new PosTagEvaluationFScoreCalculator();
				posTagFScoreCalculator.setUnknownWords(unknownWords);
				evaluator.addObserver(posTagFScoreCalculator);

				File freemarkerFile = new File(outDir, modelName + suffix + ".output.txt");
				freemarkerFile.delete();
				freemarkerFile.createNewFile();
				Reader templateReader = new BufferedReader(new InputStreamReader(getInputStreamFromResource("posTagger_template.ftl")));
				Writer freemakerFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(freemarkerFile, false),"UTF8"));
				PosTaggerGuessTemplateWriter templateWriter = new PosTaggerGuessTemplateWriter(freemakerFileWriter, templateReader);
				evaluator.addObserver(templateWriter);
				
				if (tokeniser!=null) {
					evaluator.setTokeniser(tokeniser);
				}
				evaluator.setPropagateBeam(propagateBeam);
				
				evaluator.evaluate(corpusReader);
				
				fScoreCalculator = posTagFScoreCalculator.getFScoreCalculator();
				
				double unknownLexiconFScore = posTagFScoreCalculator.getFscoreUnknownInLexicon().getTotalFScore();
				LOG.debug("F-score for words unknown in lexicon " + posTaggerModelFilePath + ": " + unknownLexiconFScore);
				double unknownCorpusFScore = posTagFScoreCalculator.getFscoreUnknownInCorpus().getTotalFScore();
				LOG.debug("F-score for words unknown in corpus " + posTaggerModelFilePath + ": " + unknownCorpusFScore);
				
				double fscore = fScoreCalculator.getTotalFScore();
				LOG.debug("F-score for " + posTaggerModelFilePath + ": " + fscore);
				
				File fscoreFile = new File(outDir, modelName + suffix + ".fscores.csv");
				fScoreCalculator.writeScoresToCSVFile(fscoreFile);
			}
			
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw e;
		} finally {
			PerformanceMonitor.end();
		}
	}
	
	private static InputStream getInputStreamFromResource(String resource) {
		String path = "/com/joliciel/talismane/output/" + resource;
		InputStream inputStream = Talismane.class.getResourceAsStream(path); 
		
		return inputStream;
	}
}
