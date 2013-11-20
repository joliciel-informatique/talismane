package com.joliciel.talismane.trainer.fr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Map.Entry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.ftbDep.FtbDepReader;
import com.joliciel.talismane.TalismaneConfig;
import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneServiceLocator;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.lexicon.LexiconChain;
import com.joliciel.talismane.lexicon.LexiconDeserializer;
import com.joliciel.talismane.lexicon.PosTaggerLexicon;
import com.joliciel.talismane.machineLearning.ClassificationEventStream;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.ExternalResourceFinder;
import com.joliciel.talismane.machineLearning.MachineLearningAlgorithm;
import com.joliciel.talismane.machineLearning.MachineLearningModel;
import com.joliciel.talismane.machineLearning.MachineLearningModel.MachineLearningModelType;
import com.joliciel.talismane.machineLearning.Ranker;
import com.joliciel.talismane.machineLearning.RankingEventStream;
import com.joliciel.talismane.machineLearning.RankingModelTrainer;
import com.joliciel.talismane.machineLearning.linearsvm.LinearSVMModelTrainer;
import com.joliciel.talismane.machineLearning.linearsvm.LinearSVMModelTrainer.LinearSVMSolverType;
import com.joliciel.talismane.machineLearning.maxent.MaxentModelTrainer;
import com.joliciel.talismane.machineLearning.perceptron.PerceptronClassificationModelTrainer;
import com.joliciel.talismane.machineLearning.perceptron.PerceptronModelTrainerObserver;
import com.joliciel.talismane.machineLearning.perceptron.PerceptronService;
import com.joliciel.talismane.machineLearning.perceptron.PerceptronServiceLocator;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.machineLearning.ClassificationModelTrainer;
import com.joliciel.talismane.parser.ParserAnnotatedCorpusReader;
import com.joliciel.talismane.parser.ParserRegexBasedCorpusReader;
import com.joliciel.talismane.parser.ParserService;
import com.joliciel.talismane.parser.ParserServiceLocator;
import com.joliciel.talismane.parser.ParsingConstrainer;
import com.joliciel.talismane.parser.Transition;
import com.joliciel.talismane.parser.TransitionSystem;
import com.joliciel.talismane.parser.features.ParseConfigurationFeature;
import com.joliciel.talismane.parser.features.ParserFeatureService;
import com.joliciel.talismane.parser.features.ParserFeatureServiceLocator;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.posTagger.PosTaggerService;
import com.joliciel.talismane.posTagger.PosTaggerServiceLocator;
import com.joliciel.talismane.posTagger.filters.PosTagFilterService;
import com.joliciel.talismane.posTagger.filters.PosTagSequenceFilter;
import com.joliciel.talismane.tokeniser.TokeniserService;
import com.joliciel.talismane.tokeniser.TokeniserServiceLocator;
import com.joliciel.talismane.tokeniser.filters.TokenFilter;
import com.joliciel.talismane.tokeniser.filters.TokenFilterService;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;
import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.PerformanceMonitor;

public class ParserTrainer {
    private static final Log LOG = LogFactory.getLog(ParserTrainer.class);
    
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		Map<String, String> argMap = TalismaneConfig.convertArgs(args);
		
		@SuppressWarnings("unused")
		ParserTrainer maxentRunner = new ParserTrainer(argMap);
	}

    public ParserTrainer(Map<String,String> argMap) throws Exception {
    	String command = null;

		String corpusType = "conll";

		String parserModelFilePath = "";
		String parserFeatureFilePath = "";
		String posTagSetPath = "";
		String corpusFilePath = "";
		int iterations = 0;
		int cutoff = 0;
		int sentenceCount = 0;
		String outDirPath = "";
		String lexiconDirPath = "";
		int beamWidth = 1;
		String transitionSystemStr = "ShiftReduce";
		MachineLearningAlgorithm algorithm = MachineLearningAlgorithm.MaxEnt;
		
		double constraintViolationCost = -1;
		double epsilon = -1;
		LinearSVMSolverType solverType = null;
		double perceptronTolerance = -1;
		boolean averageAtIntervals = false;
		List<Integer> perceptronObservationPoints = null;
		
		String tokenFilterPath = "";
		String tokenSequenceFilterPath = "";
		String posTaggerPreProcessingFilterPath = "";
		String posTaggerPostProcessingFilterPath = "";
		String externalResourcePath = null;
		String dependencyLabelPath = null;
		String parsingConstrainerPath = null;
		
		int crossValidationSize = -1;
		int includeIndex = -1;
		int excludeIndex = -1;

		File performanceConfigFile = null;
		String excludeFileName = null;
		String inputRegex = null;
		String inputPatternFilePath = null;

		boolean keepCompoundPosTags = false;

		for (Entry<String, String> argEntry : argMap.entrySet()) {
			String argName = argEntry.getKey();
			String argValue = argEntry.getValue();
			if (argName.equals("command"))
				command = argValue;
			else if (argName.equals("parserModel"))
				parserModelFilePath = argValue;
			else if (argName.equals("parserFeatures"))
				parserFeatureFilePath = argValue;
			else if (argName.equals("posTagSet"))
				posTagSetPath = argValue;
			else if (argName.equals("corpus")) 
				corpusFilePath = argValue;
			else if (argName.equals("outdir")) 
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
			else if (argName.equals("transitionSystem"))
				transitionSystemStr = argValue;
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
			else if (argName.equals("tokenFilters"))
				tokenFilterPath = argValue;
			else if (argName.equals("tokenSequenceFilters"))
				tokenSequenceFilterPath = argValue;
			else if (argName.equals("posTaggerPreProcessingFilters"))
				posTaggerPreProcessingFilterPath = argValue;
			else if (argName.equals("posTaggerPostProcessingFilters"))
				posTaggerPostProcessingFilterPath = argValue;
			else if (argName.equals("keepCompoundPosTags"))
				keepCompoundPosTags = argValue.equalsIgnoreCase("true");
			else if (argName.equals("externalResources"))
				externalResourcePath = argValue;
			else if (argName.equals("performanceConfigFile"))
				performanceConfigFile = new File(argValue);
			else if (argName.equals("crossValidationSize"))
				crossValidationSize = Integer.parseInt(argValue);
			else if (argName.equals("includeIndex"))
				includeIndex = Integer.parseInt(argValue);
			else if (argName.equals("excludeIndex"))
				excludeIndex = Integer.parseInt(argValue);
			else if (argName.equals("averageAtIntervals"))
				averageAtIntervals = argValue.equalsIgnoreCase("true");
			else if (argName.equals("perceptronObservationPoints")) {
				String[] points = argValue.split(",");
				perceptronObservationPoints = new ArrayList<Integer>();
				for (String point : points)
					perceptronObservationPoints.add(Integer.parseInt(point));
			}
			else if (argName.equals("excludeFile"))
				excludeFileName = argValue;
			else if (argName.equals("corpusReader"))
				corpusType = argValue;
			else if (argName.equals("inputPatternFile"))
				inputPatternFilePath = argValue;
			else if (argName.equals("inputPattern"))
				inputRegex = argValue;
			else
				throw new RuntimeException("Unknown argument: " + argName);
		}
		
		if (posTagSetPath.length()==0) {
			throw new RuntimeException("Missing argument: posTagSet");
		}
		if (lexiconDirPath.length()==0) {
			throw new RuntimeException("Missing argument: lexiconDir");
		}
		
		File modelFile = new File(parserModelFilePath);
		File modelDir = modelFile.getParentFile();
		modelDir.mkdirs();
		
		String modelName = modelFile.getName().substring(0, modelFile.getName().lastIndexOf('.'));
		
		long startTime = new Date().getTime();
		PerformanceMonitor.start(performanceConfigFile);
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
				
			MachineLearningService machineLearningService = talismaneServiceLocator.getMachineLearningServiceLocator().getMachineLearningService();
				
			if (parserModelFilePath.length()==0)
				throw new RuntimeException("Missing argument: parserModel");
			if (corpusFilePath.length()==0)
				throw new RuntimeException("Missing argument: corpus");

			ParserAnnotatedCorpusReader corpusReader = null;
			File corpusFile = new File(corpusFilePath);
			if (!corpusFile.exists())
				throw new TalismaneException("Training corpus not found: " + corpusFilePath);

			if (corpusType.equals("ftbDep")) {
				FtbDepReader ftbDepReader = new FtbDepReader(corpusFile, "UTF-8");
				ftbDepReader.setParserService(parserService);
				ftbDepReader.setPosTaggerService(posTaggerService);
				ftbDepReader.setTokeniserService(tokeniserService);
				ftbDepReader.setKeepCompoundPosTags(keepCompoundPosTags);
				ftbDepReader.setPredictTransitions(true);
				
				corpusReader = ftbDepReader;
	  		} else if (corpusType.equals("conll")) {
    			ParserRegexBasedCorpusReader conllReader = parserService.getRegexBasedCorpusReader(corpusFile, Charset.forName("UTF-8"));
    			conllReader.setPredictTransitions(true);
    			conllReader.setExcludeFileName(excludeFileName);
    			
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
    			
    			if (inputRegex!=null)
    				conllReader.setRegex(inputRegex);
 				
				corpusReader = conllReader;
			} else {
				throw new RuntimeException("Unknown corpusReader: " + corpusType);
			}
			corpusReader.setMaxSentenceCount(sentenceCount);

			
			if (crossValidationSize>0)
				corpusReader.setCrossValidationSize(crossValidationSize);
			if (includeIndex>=0)
				corpusReader.setIncludeIndex(includeIndex);
			if (excludeIndex>=0)
				corpusReader.setExcludeIndex(excludeIndex);
			
			if (command.equals("train")) {
				if (parserFeatureFilePath.length()==0)
					throw new RuntimeException("Missing argument: parserFeatures");
				if (dependencyLabelPath==null)
					throw new RuntimeException("Missing argument: dependencyLabels");
				
				
				ExternalResourceFinder externalResourceFinder = null;
				if (externalResourcePath!=null) {
					externalResourceFinder = parserFeatureService.getExternalResourceFinder();
					File externalResourceFile = new File (externalResourcePath);
					externalResourceFinder.addExternalResources(externalResourceFile);
				}
					
				File parserFeatureFile = new File(parserFeatureFilePath);
				Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(parserFeatureFile), "UTF-8")));
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
					Scanner tokenFilterScanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(tokenFilterFile), "UTF-8")));
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
					Scanner tokenSequenceFilterScanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(tokenSequenceFilterFile), "UTF-8")));
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
					Scanner posTaggerPreprocessingFilterScanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(posTaggerPreprocessingFilterFile), "UTF-8")));
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
					Scanner posTaggerPostProcessingFilterScanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(posTaggerPostProcessingFilterFile), "UTF-8")));
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
				
				
				File dependencyLabelFile = new File(dependencyLabelPath);
				Scanner depLabelScanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(dependencyLabelFile), "UTF-8")));
				List<String> dependencyLabels = new ArrayList<String>();
				while (depLabelScanner.hasNextLine()) {
					String dependencyLabel = depLabelScanner.nextLine();
					if (!dependencyLabel.startsWith("#"))
						dependencyLabels.add(dependencyLabel);
				}

				TransitionSystem transitionSystem = null;
				if (transitionSystemStr.equalsIgnoreCase("ShiftReduce")) {
					transitionSystem = parserService.getShiftReduceTransitionSystem();
				} else if (transitionSystemStr.equalsIgnoreCase("ArcEager")) {
					transitionSystem = parserService.getArcEagerTransitionSystem();
				} else {
					throw new TalismaneException("Unknown transition system: " + transitionSystemStr);
				}
				transitionSystem.setDependencyLabels(dependencyLabels);
				TalismaneSession.setTransitionSystem(transitionSystem);
				
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

				Map<String,List<String>> descriptors = new HashMap<String, List<String>>();
				descriptors.put(MachineLearningModel.FEATURE_DESCRIPTOR_KEY, featureDescriptors);
				descriptors.put(TokenFilterService.TOKEN_FILTER_DESCRIPTOR_KEY, tokenFilterDescriptors);
				descriptors.put(TokenFilterService.TOKEN_SEQUENCE_FILTER_DESCRIPTOR_KEY, tokenSequenceFilterDescriptors);
				descriptors.put(PosTagFilterService.POSTAG_PREPROCESSING_FILTER_DESCRIPTOR_KEY, posTaggerPreProcessingFilterDescriptors);
				descriptors.put(PosTagFilterService.POSTAG_POSTPROCESSING_FILTER_DESCRIPTOR_KEY, posTaggerPostProcessingFilterDescriptors);

				MachineLearningModel parserModel = null;
				boolean needToPersist = true;
				if (algorithm.getModelType()==MachineLearningModelType.Classification) {
					ClassificationEventStream parseEventStream = parserService.getParseEventStream(corpusReader, parseFeatures);
					if (perceptronObservationPoints==null) {
						ClassificationModelTrainer<Transition> trainer = machineLearningService.getClassificationModelTrainer(algorithm, trainParameters);
	
						parserModel = trainer.trainModel(parseEventStream, transitionSystem, descriptors);
					} else {
						if (algorithm!=MachineLearningAlgorithm.Perceptron)
							throw new RuntimeException("Incompatible argument perceptronTrainingInterval with algorithm " + algorithm);
						PerceptronServiceLocator perceptronServiceLocator = PerceptronServiceLocator.getInstance();
						PerceptronService perceptronService = perceptronServiceLocator.getPerceptronService();
						PerceptronClassificationModelTrainer<Transition> trainer = perceptronService.getPerceptronModelTrainer();
						trainer.setParameters(trainParameters);
						PerceptronModelTrainerObserver<Transition> observer = new ParserPerceptronModelPersister(modelDir, modelName, externalResourceFinder);
						trainer.trainModelsWithObserver(parseEventStream, transitionSystem, descriptors, observer, perceptronObservationPoints);
						needToPersist = false;
					}
				} else if (algorithm.getModelType()==MachineLearningModelType.Ranking) {
					if (parsingConstrainerPath==null) {
						throw new RuntimeException("Missing argument: parsingConstrainer");
					}
					RankingEventStream<PosTagSequence> parseEventStream = parserService.getGlobalParseEventStream(corpusReader, parseFeatures);
					RankingModelTrainer<PosTagSequence> trainer = machineLearningService.getRankingModelTrainer(algorithm, trainParameters);
					ParsingConstrainer parsingConstrainer = parserService.getParsingConstrainer(new File(parsingConstrainerPath));
					Ranker<PosTagSequence> ranker = parserService.getRanker(parsingConstrainer, parseFeatures, beamWidth);
					parserModel = trainer.trainModel(parseEventStream, ranker, descriptors);
					parserModel.addDependency(ParsingConstrainer.class.getSimpleName(), parsingConstrainer);
				} else {
					throw new TalismaneException("Unknown model type: " + algorithm.getModelType());
				}
				
				if (needToPersist) {
					if (externalResourceFinder!=null)
						parserModel.setExternalResources(externalResourceFinder.getExternalResources());
					parserModel.persist(modelFile);
				}
			}
			
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw e;
		} finally {
			PerformanceMonitor.end();
			
			if (PerformanceMonitor.isActivated()) {
				Writer csvFileWriter = null;
				File csvFile = null;
				if (outDirPath!=null && outDirPath.length()>0) {
					File outDir = new File(outDirPath);
					outDir.mkdirs();
		
					csvFile = new File(outDir, modelName + "_performance.csv");
				} else {
					csvFile = new File(modelDir, modelName + "_performance.csv");
				}
				csvFile.delete();

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
	
    private static final class ParserPerceptronModelPersister implements PerceptronModelTrainerObserver<Transition> {
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
		public void onNextModel(ClassificationModel<Transition> model,
				int iterations) {
			this.outDir.mkdirs();
			File parserModelFile = new File(outDir, baseName + "_i" + iterations + ".zip");
			if (externalResourceFinder!=null)
				model.setExternalResources(externalResourceFinder.getExternalResources());
			LOG.info("Writing model " + parserModelFile.getName());
			model.persist(parserModelFile);
		}
    }
}
