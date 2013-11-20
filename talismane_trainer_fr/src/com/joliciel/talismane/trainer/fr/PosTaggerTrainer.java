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

import com.joliciel.frenchTreebank.TreebankReader;
import com.joliciel.frenchTreebank.TreebankServiceLocator;
import com.joliciel.frenchTreebank.export.FtbPosTagMapper;
import com.joliciel.frenchTreebank.export.TreebankExportService;
import com.joliciel.frenchTreebank.upload.TreebankUploadService;
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
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.machineLearning.ClassificationModelTrainer;
import com.joliciel.talismane.machineLearning.linearsvm.LinearSVMModelTrainer;
import com.joliciel.talismane.machineLearning.linearsvm.LinearSVMModelTrainer.LinearSVMSolverType;
import com.joliciel.talismane.machineLearning.maxent.MaxentModelTrainer;
import com.joliciel.talismane.machineLearning.perceptron.PerceptronClassificationModelTrainer;
import com.joliciel.talismane.machineLearning.perceptron.PerceptronModelTrainerObserver;
import com.joliciel.talismane.machineLearning.perceptron.PerceptronService;
import com.joliciel.talismane.machineLearning.perceptron.PerceptronServiceLocator;
import com.joliciel.talismane.parser.ParserRegexBasedCorpusReader;
import com.joliciel.talismane.parser.ParserService;
import com.joliciel.talismane.parser.ParserServiceLocator;
import com.joliciel.talismane.parser.TransitionSystem;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.posTagger.PosTagAnnotatedCorpusReader;
import com.joliciel.talismane.posTagger.PosTaggerService;
import com.joliciel.talismane.posTagger.features.PosTaggerFeature;
import com.joliciel.talismane.posTagger.features.PosTaggerFeatureService;
import com.joliciel.talismane.posTagger.filters.PosTagFilterService;
import com.joliciel.talismane.posTagger.filters.PosTagSequenceFilter;
import com.joliciel.talismane.tokeniser.TokeniserService;
import com.joliciel.talismane.tokeniser.TokeniserServiceLocator;
import com.joliciel.talismane.tokeniser.filters.TokenFilter;
import com.joliciel.talismane.tokeniser.filters.TokenFilterService;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;
import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.PerformanceMonitor;

public class PosTaggerTrainer {
    private static final Log LOG = LogFactory.getLog(PosTaggerTrainer.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		Map<String, String> argMap = TalismaneConfig.convertArgs(args);

		@SuppressWarnings("unused")
		PosTaggerTrainer maxentRunner = new PosTaggerTrainer(argMap);
	}

    public PosTaggerTrainer(Map<String,String> argMap) throws Exception {
		String corpusType = "ftb";
		String posTaggerModelFilePath = "";
		String posTaggerFeatureFilePath = "";
		String tokenFilterPath = "";
		String tokenSequenceFilterPath = "";
		String posTaggerPreProcessingFilterPath = "";
		String posTaggerPostProcessingFilterPath = "";
		int iterations = 0;
		int cutoff = 0;
		int sentenceCount = 0;
		String posTagSetPath = "";
		String posTagMapPath = "";
		String corpusPath = "";
		String lexiconDirPath = "";

		MachineLearningAlgorithm algorithm = MachineLearningAlgorithm.MaxEnt;
		
		double constraintViolationCost = -1;
		double epsilon = -1;
		LinearSVMSolverType solverType = null;
		double perceptronTolerance = -1;
		boolean averageAtIntervals = false;
		List<Integer> perceptronObservationPoints = null;

		boolean useCompoundPosTags = false;
		String externalResourcePath = null;
		String excludeFileName = null;
		String inputRegex = null;
		String inputPatternFilePath = null;
		
		File performanceConfigFile = null;

		for (Entry<String, String> argEntry : argMap.entrySet()) {
			String argName = argEntry.getKey();
			String argValue = argEntry.getValue();
			if (argName.equals("posTaggerModel"))
				posTaggerModelFilePath = argValue;
			else if (argName.equals("posTaggerFeatures")) 
				posTaggerFeatureFilePath = argValue;
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
			else if (argName.equals("iterations"))
				iterations = Integer.parseInt(argValue);
			else if (argName.equals("cutoff"))
				cutoff = Integer.parseInt(argValue);
			else if (argName.equals("sentenceCount"))
				sentenceCount = Integer.parseInt(argValue);
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
			else if (argName.equals("corpusReader"))
				corpusType = argValue;
			else if (argName.equals("useCompoundPosTags"))
				useCompoundPosTags = argValue.equalsIgnoreCase("true");
			else if (argName.equals("externalResources"))
				externalResourcePath = argValue;
			else if (argName.equals("performanceConfigFile"))
				performanceConfigFile = new File(argValue);
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
			else if (argName.equals("inputPatternFile"))
				inputPatternFilePath = argValue;
			else if (argName.equals("inputPattern"))
				inputRegex = argValue;
			else
				throw new RuntimeException("Unknown argument: " + argName);
		}
		
		File modelFile = new File(posTaggerModelFilePath);
		File modelDir = modelFile.getParentFile();
		modelDir.mkdirs();
		
		String modelName = modelFile.getName().substring(0, modelFile.getName().lastIndexOf('.'));

		
		long startTime = new Date().getTime();
		PerformanceMonitor.start(performanceConfigFile);
		try {

			if (lexiconDirPath.length()==0)
				throw new RuntimeException("Missing argument: lexiconDir");
			if (posTagSetPath.length()==0)
				throw new RuntimeException("Missing argument: posTagSet");
			if (posTaggerModelFilePath.length()==0)
				throw new RuntimeException("Missing argument: posTaggerModel");
			if (posTaggerFeatureFilePath.length()==0)
				throw new RuntimeException("Missing argument: posTaggerFeatures");
						
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
	 
	        TokenFilterService tokenFilterService = talismaneServiceLocator.getTokenFilterServiceLocator().getTokenFilterService();
	        TokeniserServiceLocator tokeniserServiceLocator = talismaneServiceLocator.getTokeniserServiceLocator();
	        TokeniserService tokeniserService = tokeniserServiceLocator.getTokeniserService();
			
			ParserServiceLocator parserServiceLocator = talismaneServiceLocator.getParserServiceLocator();
			ParserService parserService = parserServiceLocator.getParserService();

			PosTaggerFeatureService posTaggerFeatureService = talismaneServiceLocator.getPosTaggerFeatureServiceLocator().getPosTaggerFeatureService();

			TreebankServiceLocator treebankServiceLocator = TreebankServiceLocator.getInstance(talismaneServiceLocator);
			if (corpusPath.length()==0)
				treebankServiceLocator.setDataSourcePropertiesFile("jdbc-ftb.properties");
	        
	        TreebankUploadService treebankUploadService = treebankServiceLocator.getTreebankUploadServiceLocator().getTreebankUploadService();
			TreebankExportService treebankExportService = treebankServiceLocator.getTreebankExportServiceLocator().getTreebankExportService();
			
			MachineLearningService machineLearningService = talismaneServiceLocator.getMachineLearningServiceLocator().getMachineLearningService();

			// doesn't really matter which transition system, we're just reading postags
			TransitionSystem transitionSystem = parserService.getArcEagerTransitionSystem();
			TalismaneSession.setTransitionSystem(transitionSystem);

			PosTagAnnotatedCorpusReader corpusReader = null;
			File corpusFile = new File(corpusPath);
			if (!corpusFile.exists())
				throw new TalismaneException("Training corpus not found: " + corpusPath);
			
			if (corpusType.equals("ftb")) {
				if (posTagMapPath.length()==0)
					throw new RuntimeException("Missing argument: posTagMap");
				
				TreebankReader treebankReader = null;
				
				treebankReader = treebankUploadService.getXmlReader(corpusFile);
				
				File posTagMapFile = new File(posTagMapPath);
				FtbPosTagMapper ftbPosTagMapper = treebankExportService.getFtbPosTagMapper(posTagMapFile, posTagSet);
				corpusReader = treebankExportService.getPosTagAnnotatedCorpusReader(treebankReader, ftbPosTagMapper, useCompoundPosTags);
			} else if (corpusType.equals("ftbDep")) {
				FtbDepReader ftbDepReader = new FtbDepReader(corpusFile, "UTF-8");
				ftbDepReader.setParserService(parserService);
				ftbDepReader.setPosTaggerService(posTaggerService);
				ftbDepReader.setTokeniserService(tokeniserService);
				ftbDepReader.setKeepCompoundPosTags(useCompoundPosTags);
				ftbDepReader.setPredictTransitions(false);
				
				corpusReader = ftbDepReader;
	  		} else if (corpusType.equals("conll")) {
    			ParserRegexBasedCorpusReader conllReader = parserService.getRegexBasedCorpusReader(corpusFile, Charset.forName("UTF-8"));
    			conllReader.setPredictTransitions(false);
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
			
			ExternalResourceFinder externalResourceFinder = null;
			if (externalResourcePath!=null) {
				externalResourceFinder = posTaggerFeatureService.getExternalResourceFinder();
				File externalResourceFile = new File (externalResourcePath);
				externalResourceFinder.addExternalResources(externalResourceFile);
			}
			File posTaggerTokenFeatureFile = new File(posTaggerFeatureFilePath);
			Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(posTaggerTokenFeatureFile), "UTF-8")));
//				Scanner scanner = new Scanner(posTaggerTokenFeatureFile, "UTF-8");
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
			
			ClassificationEventStream posTagEventStream = posTaggerService.getPosTagEventStream(corpusReader, posTaggerFeatures);
			
			Map<String,Object> trainParameters = new HashMap<String, Object>();
			if (algorithm.equals(MachineLearningAlgorithm.MaxEnt)) {
				trainParameters.put(MaxentModelTrainer.MaxentModelParameter.Iterations.name(), iterations);
				trainParameters.put(MaxentModelTrainer.MaxentModelParameter.Cutoff.name(), cutoff);
			} else if (algorithm.equals(MachineLearningAlgorithm.Perceptron)) {
				trainParameters.put(PerceptronClassificationModelTrainer.PerceptronModelParameter.Iterations.name(), iterations);
				trainParameters.put(PerceptronClassificationModelTrainer.PerceptronModelParameter.Cutoff.name(), cutoff);
				trainParameters.put(PerceptronClassificationModelTrainer.PerceptronModelParameter.AverageAtIntervals.name(), averageAtIntervals);
				
				if (perceptronTolerance>=0)
					trainParameters.put(PerceptronClassificationModelTrainer.PerceptronModelParameter.Tolerance.name(), perceptronTolerance);					
			} else if (algorithm.equals(MachineLearningAlgorithm.LinearSVM)) {
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
			descriptors.put(PosTagFilterService.POSTAG_PREPROCESSING_FILTER_DESCRIPTOR_KEY, posTaggerPreprocessingFilterDescriptors);
			descriptors.put(PosTagFilterService.POSTAG_POSTPROCESSING_FILTER_DESCRIPTOR_KEY, posTaggerPostProcessingFilterDescriptors);

			if (perceptronObservationPoints==null) {
				ClassificationModelTrainer<PosTag> trainer = machineLearningService.getClassificationModelTrainer(algorithm, trainParameters);

				ClassificationModel<PosTag> posTaggerModel = trainer.trainModel(posTagEventStream, posTagSet, descriptors);			
				if (externalResourceFinder!=null)
					posTaggerModel.setExternalResources(externalResourceFinder.getExternalResources());
				posTaggerModel.persist(modelFile);
			} else {
				if (algorithm!=MachineLearningAlgorithm.Perceptron)
					throw new RuntimeException("Incompatible argument perceptronTrainingInterval with algorithm " + algorithm);
				PerceptronServiceLocator perceptronServiceLocator = PerceptronServiceLocator.getInstance();
				PerceptronService perceptronService = perceptronServiceLocator.getPerceptronService();
				PerceptronClassificationModelTrainer<PosTag> trainer = perceptronService.getPerceptronModelTrainer();
				trainer.setParameters(trainParameters);
				PerceptronModelTrainerObserver<PosTag> observer = new PosTaggerPerceptronModelPersister(modelDir, modelName, externalResourceFinder);
				trainer.trainModelsWithObserver(posTagEventStream, posTagSet, descriptors, observer, perceptronObservationPoints);
			}

		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw e;
		} finally {
			PerformanceMonitor.end();

			if (PerformanceMonitor.isActivated()) {
				Writer csvFileWriter = null;
				File csvFile = new File(modelDir, modelName + "_performance.csv");
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
	
    private static final class PosTaggerPerceptronModelPersister implements PerceptronModelTrainerObserver<PosTag> {
    	File outDir;
    	ExternalResourceFinder externalResourceFinder;
    	String baseName;
    	
		public PosTaggerPerceptronModelPersister(File outDir, String baseName,
				ExternalResourceFinder externalResourceFinder) {
			super();
			this.outDir = outDir;
			this.baseName = baseName;
			this.externalResourceFinder = externalResourceFinder;
		}

		@Override
		public void onNextModel(ClassificationModel<PosTag> model,
				int iterations) {
			this.outDir.mkdirs();
			File posTaggerModelFile = new File(outDir, baseName + "_i" + iterations + ".zip");
			if (externalResourceFinder!=null)
				model.setExternalResources(externalResourceFinder.getExternalResources());
			LOG.info("Writing model " + posTaggerModelFile.getName());
			model.persist(posTaggerModelFile);
		}
    }
}
