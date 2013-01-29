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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.frenchTreebank.TreebankService;
import com.joliciel.frenchTreebank.TreebankServiceLocator;
import com.joliciel.ftbDep.FtbDepReader;
import com.joliciel.talismane.Talismane;
import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneServiceLocator;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.lexicon.LexiconChain;
import com.joliciel.talismane.lexicon.LexiconDeserializer;
import com.joliciel.talismane.lexicon.PosTaggerLexicon;
import com.joliciel.talismane.machineLearning.CorpusEventStream;
import com.joliciel.talismane.machineLearning.MachineLearningModel;
import com.joliciel.talismane.machineLearning.MachineLearningModel.MachineLearningAlgorithm;
import com.joliciel.talismane.machineLearning.linearsvm.LinearSVMModelTrainer;
import com.joliciel.talismane.machineLearning.linearsvm.LinearSVMModelTrainer.LinearSVMSolverType;
import com.joliciel.talismane.machineLearning.maxent.MaxentModelTrainer;
import com.joliciel.talismane.machineLearning.maxent.PerceptronModelTrainer;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.machineLearning.ModelTrainer;
import com.joliciel.talismane.parser.NonDeterministicParser;
import com.joliciel.talismane.parser.ParseEvaluationFScoreCalculator;
import com.joliciel.talismane.parser.ParseEvaluationGuessTemplateWriter;
import com.joliciel.talismane.parser.ParseEvaluationSentenceWriter;
import com.joliciel.talismane.parser.ParserEvaluator;
import com.joliciel.talismane.parser.ParserService;
import com.joliciel.talismane.parser.ParserServiceLocator;
import com.joliciel.talismane.parser.Transition;
import com.joliciel.talismane.parser.TransitionSystem;
import com.joliciel.talismane.parser.features.ParseConfigurationFeature;
import com.joliciel.talismane.parser.features.ParserFeatureService;
import com.joliciel.talismane.parser.features.ParserFeatureServiceLocator;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.posTagger.PosTaggerService;
import com.joliciel.talismane.posTagger.PosTaggerServiceLocator;
import com.joliciel.talismane.posTagger.filters.PosTagFilterService;
import com.joliciel.talismane.posTagger.filters.PosTagSequenceFilter;
import com.joliciel.talismane.stats.FScoreCalculator;
import com.joliciel.talismane.tokeniser.TokeniserService;
import com.joliciel.talismane.tokeniser.TokeniserServiceLocator;
import com.joliciel.talismane.tokeniser.filters.TokenFilter;
import com.joliciel.talismane.tokeniser.filters.TokenFilterService;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;
import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.PerformanceMonitor;

public class ParserMaxentRunner {
    private static final Log LOG = LogFactory.getLog(ParserMaxentRunner.class);

	/**
	 * @param args
	 */
	@SuppressWarnings("unused")
	public static void main(String[] args) throws Exception {
		String command = args[0];

		String parserModelFilePath = "";
		String parserFeatureFilePath = "";
		String posTagSetPath = "";
		String corpusFilePath = "";
		int iterations = 0;
		int cutoff = 0;
		int sentenceCount = 0;
		String outDirPath = "";
		String lexiconDirPath = "";
		int beamWidth = 10;
		boolean includeSentences = true;
		String transitionSystemStr = "ShiftReduce";
		boolean logPerformance = false;
		MachineLearningAlgorithm algorithm = MachineLearningAlgorithm.MaxEnt;
		
		int outputGuessCount = 0;
		int maxParseAnalysisTime = 60;
		
		double constraintViolationCost = -1;
		double epsilon = -1;
		LinearSVMSolverType solverType = null;
		boolean perceptronAveraging = false;
		boolean perceptronSkippedAveraging = false;
		double perceptronTolerance = -1;
		
		String tokenFilterPath = "";
		String tokenSequenceFilterPath = "";
		String posTaggerPreProcessingFilterPath = "";
		String posTaggerPostProcessingFilterPath = "";

		boolean keepCompoundPosTags = false;
		
		boolean firstArg = true;
		for (String arg : args) {
			if (firstArg) {
				firstArg = false;
				continue;
			}
			int equalsPos = arg.indexOf('=');
			String argName = arg.substring(0, equalsPos);
			String argValue = arg.substring(equalsPos+1);
			if (argName.equals("parserModel"))
				parserModelFilePath = argValue;
			else if (argName.equals("parserFeatures"))
				parserFeatureFilePath = argValue;
			else if (argName.equals("posTagSet"))
				posTagSetPath = argValue;
			else if (argName.equals("corpus")) 
				corpusFilePath = argValue;
			else if (argName.equals("outDir")) 
				outDirPath = argValue;
			else if (argName.equals("iterations"))
				iterations = Integer.parseInt(argValue);
			else if (argName.equals("cutoff"))
				cutoff = Integer.parseInt(argValue);
			else if (argName.equals("sentenceCount"))
				sentenceCount = Integer.parseInt(argValue);
			else if (argName.equals("lexiconDir"))
				lexiconDirPath = argValue;
			else if (argName.equals("beamWidth"))
				beamWidth = Integer.parseInt(argValue);
			else if (argName.equals("includeSentences"))
				includeSentences = argValue.equalsIgnoreCase("true");
			else if (argName.equals("transitionSystem"))
				transitionSystemStr = argValue;
			else if (argName.equals("logPerformance"))
				logPerformance = argValue.equalsIgnoreCase("true");
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
			else if (argName.equals("tokenFilters"))
				tokenFilterPath = argValue;
			else if (argName.equals("tokenSequenceFilters"))
				tokenSequenceFilterPath = argValue;
			else if (argName.equals("posTaggerPreProcessingFilters"))
				posTaggerPreProcessingFilterPath = argValue;
			else if (argName.equals("posTaggerPostProcessingFilters"))
				posTaggerPostProcessingFilterPath = argValue;
			else if (argName.equals("outputGuessCount"))
				outputGuessCount = Integer.parseInt(argValue);
			else if (argName.equals("maxParseAnalysisTime"))
				maxParseAnalysisTime = Integer.parseInt(argValue);
			else if (argName.equals("keepCompoundPosTags"))
				keepCompoundPosTags = argValue.equalsIgnoreCase("true");
			else
				throw new RuntimeException("Unknown argument: " + argName);
		}
		
		if (posTagSetPath.length()==0) {
			throw new RuntimeException("Missing argument: posTagSet");
		}
		if (lexiconDirPath.length()==0) {
			throw new RuntimeException("Missing argument: lexiconDir");
		}
		String modelPath = parserModelFilePath.substring(0, parserModelFilePath.lastIndexOf('/')+1);
		String modelName = parserModelFilePath.substring(parserModelFilePath.lastIndexOf('/')+1, parserModelFilePath.indexOf('.'));
		
		PerformanceMonitor.setActive(logPerformance);
		long startTime = new Date().getTime();
		PerformanceMonitor.start();
		try {
			TalismaneServiceLocator talismaneServiceLocator = TalismaneServiceLocator.getInstance();

	        PosTaggerServiceLocator posTaggerServiceLocator = talismaneServiceLocator.getPosTaggerServiceLocator();
	        PosTaggerService posTaggerService = posTaggerServiceLocator.getPosTaggerService();
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
	 
	        TokeniserServiceLocator tokeniserServiceLocator = talismaneServiceLocator.getTokeniserServiceLocator();
	        TokeniserService tokeniserService = tokeniserServiceLocator.getTokeniserService();
	        TokenFilterService tokenFilterService = talismaneServiceLocator.getTokenFilterServiceLocator().getTokenFilterService();
			
			ParserServiceLocator parserServiceLocator = talismaneServiceLocator.getParserServiceLocator();
			ParserService parserService = parserServiceLocator.getParserService();
			ParserFeatureServiceLocator parserFeatureServiceLocator = talismaneServiceLocator.getParserFeatureServiceLocator();
			ParserFeatureService parserFeatureService = parserFeatureServiceLocator.getParserFeatureService();
			
			TreebankServiceLocator treebankServiceLocator = TreebankServiceLocator.getInstance(talismaneServiceLocator);
				
			TreebankService treebankService = treebankServiceLocator.getTreebankService();
			
			MachineLearningService machineLearningService = talismaneServiceLocator.getMachineLearningServiceLocator().getMachineLearningService();

			boolean createEmptyTokensIfMissing = true;
				
			if (parserModelFilePath.length()==0)
				throw new RuntimeException("Missing argument: parserModel");
			if (corpusFilePath.length()==0)
				throw new RuntimeException("Missing argument: corpus");

			String modelDirPath = parserModelFilePath.substring(0, parserModelFilePath.lastIndexOf("/"));
			File modelDir = new File(modelDirPath);
			modelDir.mkdirs();
			
			File parserModelFile = new File(parserModelFilePath);
			
			File corpusFile = new File(corpusFilePath);

			FtbDepReader corpusReader = new FtbDepReader(corpusFile, "UTF-8");
			corpusReader.setMaxSentenceCount(sentenceCount);
			corpusReader.setParserService(parserService);
			corpusReader.setPosTaggerService(posTaggerService);
			corpusReader.setTokeniserService(tokeniserService);
			corpusReader.setKeepCompoundPosTags(keepCompoundPosTags);
			
			if (command.equals("train")) {
				if (parserFeatureFilePath.length()==0)
					throw new RuntimeException("Missing argument: parserFeatures");
				
				File parserFeatureFile = new File(parserFeatureFilePath);
				Scanner scanner = new Scanner(parserFeatureFile);
				List<String> featureDescriptors = new ArrayList<String>();
				while (scanner.hasNextLine()) {
					String descriptor = scanner.nextLine();
					featureDescriptors.add(descriptor);
					LOG.debug(descriptor);
				}
				
				Set<ParseConfigurationFeature<?>> parseFeatures = parserFeatureService.getFeatures(featureDescriptors);


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
				
				List<String> posTaggerPreProcessingFilterDescriptors = new ArrayList<String>();
				if (posTaggerPreProcessingFilterPath!=null && posTaggerPreProcessingFilterPath.length()>0) {
					LOG.debug("From: " + posTaggerPreProcessingFilterPath);
					File posTaggerPreprocessingFilterFile = new File(posTaggerPreProcessingFilterPath);
					Scanner posTaggerPreprocessingFilterScanner = new Scanner(posTaggerPreprocessingFilterFile);
					while (posTaggerPreprocessingFilterScanner.hasNextLine()) {
						String descriptor = posTaggerPreprocessingFilterScanner.nextLine();
						posTaggerPreProcessingFilterDescriptors.add(descriptor);
					}
				}
				
				for (String descriptor : posTaggerPreProcessingFilterDescriptors) {
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
				
				TransitionSystem transitionSystem = null;
				if (transitionSystemStr.equalsIgnoreCase("ShiftReduce")) {
					transitionSystem = parserService.getShiftReduceTransitionSystem();
				} else if (transitionSystemStr.equalsIgnoreCase("ArcEager")) {
					transitionSystem = parserService.getArcEagerTransitionSystem();
				} else {
					throw new TalismaneException("Unknown transition system: " + transitionSystemStr);
				}
				TalismaneSession.setTransitionSystem(transitionSystem);

				CorpusEventStream parseEventStream = parserService.getParseEventStream(corpusReader, parseFeatures);
				
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

				ModelTrainer<Transition> trainer = machineLearningService.getModelTrainer(algorithm, trainParameters);
				
				Map<String,List<String>> descriptors = new HashMap<String, List<String>>();
				descriptors.put(MachineLearningModel.FEATURE_DESCRIPTOR_KEY, featureDescriptors);
				descriptors.put(TokenFilterService.TOKEN_FILTER_DESCRIPTOR_KEY, tokenFilterDescriptors);
				descriptors.put(TokenFilterService.TOKEN_SEQUENCE_FILTER_DESCRIPTOR_KEY, tokenSequenceFilterDescriptors);
				descriptors.put(PosTagFilterService.POSTAG_PREPROCESSING_FILTER_DESCRIPTOR_KEY, posTaggerPreProcessingFilterDescriptors);
				descriptors.put(PosTagFilterService.POSTAG_POSTPROCESSING_FILTER_DESCRIPTOR_KEY, posTaggerPostProcessingFilterDescriptors);

				MachineLearningModel<Transition> parserModel = trainer.trainModel(parseEventStream, transitionSystem, descriptors);
				parserModel.persist(parserModelFile);
			} else if (command.equals("evaluate")) {
				if (outDirPath.length()==0)
					throw new RuntimeException("Missing argument: outdir");

				File outDir = new File(outDirPath);
				outDir.mkdirs();

				ZipInputStream zis = new ZipInputStream(new FileInputStream(parserModelFilePath));
				MachineLearningModel<Transition> parserModel = machineLearningService.getModel(zis);

				List<String> tokenFilterDescriptors = parserModel.getDescriptors().get(TokenFilterService.TOKEN_FILTER_DESCRIPTOR_KEY);
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
				
				List<String> tokenSequenceFilterDescriptors = parserModel.getDescriptors().get(TokenFilterService.TOKEN_SEQUENCE_FILTER_DESCRIPTOR_KEY);
				if (tokenSequenceFilterDescriptors!=null) {
					for (String descriptor : tokenSequenceFilterDescriptors) {
						if (descriptor.length()>0 && !descriptor.startsWith("#")) {
							TokenSequenceFilter tokenSequenceFilter = tokenFilterService.getTokenSequenceFilter(descriptor);
							corpusReader.addTokenSequenceFilter(tokenSequenceFilter);
						}
					}
				}
				
				List<String> posTaggerPreProcessingFilters = parserModel.getDescriptors().get(PosTagFilterService.POSTAG_PREPROCESSING_FILTER_DESCRIPTOR_KEY);
				if (posTaggerPreProcessingFilters!=null) {
					for (String descriptor : posTaggerPreProcessingFilters) {
						if (descriptor.length()>0 && !descriptor.startsWith("#")) {
							TokenSequenceFilter tokenSequenceFilter = tokenFilterService.getTokenSequenceFilter(descriptor);
							corpusReader.addTokenSequenceFilter(tokenSequenceFilter);
						}
					}
				}
				
				List<String> posTaggerPostProcessingFilters = parserModel.getDescriptors().get(PosTagFilterService.POSTAG_PREPROCESSING_FILTER_DESCRIPTOR_KEY);
				if (posTaggerPostProcessingFilters!=null) {
					for (String descriptor : posTaggerPostProcessingFilters) {
						if (descriptor.length()>0 && !descriptor.startsWith("#")) {
							PosTagSequenceFilter posTagSequenceFilter = posTagFilterService.getPosTagSequenceFilter(descriptor);
							corpusReader.addPosTagSequenceFilter(posTagSequenceFilter);
						}
					}
				}
				
				NonDeterministicParser parser = parserService.getTransitionBasedParser(parserModel, beamWidth);
				TalismaneSession.setTransitionSystem(parser.getTransitionSystem());
				parser.setMaxAnalysisTimePerSentence(maxParseAnalysisTime);
				
				ParserEvaluator evaluator = parserService.getParserEvaluator();
				evaluator.setParser(parser);
				
				if (includeSentences) {
					File csvFile = new File(outDir, modelName + "_sentences.csv");
					csvFile.delete();
					csvFile.createNewFile();
					Writer csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false),"UTF8"));
					int guessCount = 1;
					if (outputGuessCount>0)
						guessCount = outputGuessCount;
					else
						guessCount = parser.getBeamWidth();

					ParseEvaluationSentenceWriter sentenceWriter = new ParseEvaluationSentenceWriter(csvFileWriter, guessCount);
					evaluator.addObserver(sentenceWriter);
				}

				ParseEvaluationFScoreCalculator parseFScoreCalculator = new ParseEvaluationFScoreCalculator();
				parseFScoreCalculator.setLabeledEvaluation(true);
				evaluator.addObserver(parseFScoreCalculator);
				
				Reader templateReader = new BufferedReader(new InputStreamReader(getInputStreamFromResource("parser_conll_template.ftl")));
				
				File freemarkerFile = new File(outDir, modelName + "_output.txt");
				freemarkerFile.delete();
				freemarkerFile.createNewFile();
				Writer freemakerFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(freemarkerFile, false),"UTF8"));
				ParseEvaluationGuessTemplateWriter templateWriter = new ParseEvaluationGuessTemplateWriter(freemakerFileWriter, templateReader);
				evaluator.addObserver(templateWriter);
				

				evaluator.evaluate(corpusReader);
				
				FScoreCalculator<String> fscoreCalculator = parseFScoreCalculator.getFscoreCalculator();
				LOG.debug(fscoreCalculator.getTotalFScore());
				
				File fscoreFile = new File(outDir, modelName + "_fscores.csv");
				fscoreCalculator.writeScoresToCSVFile(fscoreFile);

			}
			
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw e;
		} finally {
			PerformanceMonitor.end();
			
			if (logPerformance) {
				Writer csvFileWriter = null;
				File csvFile = null;
				if (outDirPath!=null) {
					File outDir = new File(outDirPath);
		
					csvFile = new File(outDir, modelName + "_performance.csv");
				} else {
					csvFile = new File(modelPath + modelName + "_performance.csv");
				}
				csvFile.delete();
				csvFile.createNewFile();
				csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false),"UTF8"));
				PerformanceMonitor.writePerformanceCSV(csvFileWriter);
				csvFileWriter.flush();
				csvFileWriter.close();
			}
			long endTime = new Date().getTime();
			long totalTime = endTime - startTime;
			LOG.info("Total time: " + totalTime);
		}
	}
	

	private static InputStream getInputStreamFromResource(String resource) {
		String path = "/com/joliciel/talismane/output/" + resource;
		InputStream inputStream = Talismane.class.getResourceAsStream(path); 
		
		return inputStream;
	}
}
