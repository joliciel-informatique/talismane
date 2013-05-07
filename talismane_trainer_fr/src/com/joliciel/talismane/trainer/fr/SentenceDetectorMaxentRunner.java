package com.joliciel.talismane.trainer.fr;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Map.Entry;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.frenchTreebank.TreebankReader;
import com.joliciel.frenchTreebank.TreebankService;
import com.joliciel.frenchTreebank.TreebankServiceLocator;
import com.joliciel.frenchTreebank.TreebankSubSet;
import com.joliciel.frenchTreebank.export.TreebankExportService;
import com.joliciel.frenchTreebank.upload.TreebankUploadService;
import com.joliciel.talismane.TalismaneServiceLocator;
import com.joliciel.talismane.machineLearning.CorpusEventStream;
import com.joliciel.talismane.machineLearning.DecisionFactory;
import com.joliciel.talismane.machineLearning.ExternalResourceFinder;
import com.joliciel.talismane.machineLearning.MachineLearningModel;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.machineLearning.ModelTrainer;
import com.joliciel.talismane.machineLearning.TextFileResource;
import com.joliciel.talismane.machineLearning.MachineLearningModel.MachineLearningAlgorithm;
import com.joliciel.talismane.machineLearning.linearsvm.LinearSVMModelTrainer;
import com.joliciel.talismane.machineLearning.linearsvm.LinearSVMModelTrainer.LinearSVMSolverType;
import com.joliciel.talismane.machineLearning.maxent.MaxentModelTrainer;
import com.joliciel.talismane.machineLearning.perceptron.PerceptronModelTrainer;
import com.joliciel.talismane.sentenceDetector.SentenceDetector;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorOutcome;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorEvaluator;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorService;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorAnnotatedCorpusReader;
import com.joliciel.talismane.sentenceDetector.features.SentenceDetectorFeature;
import com.joliciel.talismane.sentenceDetector.features.SentenceDetectorFeatureService;
import com.joliciel.talismane.stats.FScoreCalculator;
import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.PerformanceMonitor;

public class SentenceDetectorMaxentRunner {
    private static final Log LOG = LogFactory.getLog(SentenceDetectorMaxentRunner.class);

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
		SentenceDetectorMaxentRunner maxentRunner = new SentenceDetectorMaxentRunner(argMap);
	}

    public SentenceDetectorMaxentRunner(Map<String,String> argMap) throws Exception {
		String command = null;

		String sentenceModelFilePath = "";
		String sentenceFeatureFilePath = "";
		String outDirPath = "";
		String treebankPath = "";
		int iterations = 0;
		int cutoff = 0;
		int sentenceCount = 0;
		int startSentence = 0;
		MachineLearningAlgorithm algorithm = MachineLearningAlgorithm.MaxEnt;
		
		double constraintViolationCost = -1;
		double epsilon = -1;
		LinearSVMSolverType solverType = null;
		double perceptronTolerance = -1;
		String externalResourcePath = null;

		File performanceConfigFile = null;

		for (Entry<String, String> argEntry : argMap.entrySet()) {
			String argName = argEntry.getKey();
			String argValue = argEntry.getValue();
			if (argName.equals("command"))
				command = argValue;
			else if (argName.equals("sentenceModel"))
				sentenceModelFilePath = argValue;
			else if (argName.equals("sentenceFeatures"))
				sentenceFeatureFilePath = argValue;
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
			else if (argName.equals("corpus"))
				treebankPath = argValue;
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
			else if (argName.equals("externalResources"))
				externalResourcePath = argValue;
			else if (argName.equals("performanceConfigFile"))
				performanceConfigFile = new File(argValue);
			else
				throw new RuntimeException("Unknown argument: " + argName);
		}
		
		PerformanceMonitor.start(performanceConfigFile);
		try {
			TalismaneServiceLocator talismaneServiceLocator = TalismaneServiceLocator.getInstance();

			TreebankServiceLocator treebankServiceLocator = TreebankServiceLocator.getInstance(talismaneServiceLocator);
			if (treebankPath.length()==0)
				treebankServiceLocator.setDataSourcePropertiesFile("jdbc-ftb.properties");
	
			TreebankService treebankService = treebankServiceLocator.getTreebankService();
	        TreebankUploadService treebankUploadService = treebankServiceLocator.getTreebankUploadServiceLocator().getTreebankUploadService();
			TreebankExportService treebankExportService = treebankServiceLocator.getTreebankExportServiceLocator().getTreebankExportService();
	        
	        SentenceDetectorService sentenceDetectorService = talismaneServiceLocator.getSentenceDetectorServiceLocator().getSentenceDetectorService();
	        SentenceDetectorFeatureService sentenceDetectorFeatureService = talismaneServiceLocator.getSentenceDetectorFeatureServiceLocator().getSentenceDetectorFeatureService();

			MachineLearningService machineLearningService = talismaneServiceLocator.getMachineLearningServiceLocator().getMachineLearningService();

			if (command.equals("train")) {
				if (sentenceModelFilePath.length()==0)
					throw new RuntimeException("Missing argument: sentenceModel");
	
				String modelDirPath = sentenceModelFilePath.substring(0, sentenceModelFilePath.lastIndexOf("/"));
				File modelDir = new File(modelDirPath);
				modelDir.mkdirs();
				File sentenceModelFile = new File(sentenceModelFilePath);				
				
				TreebankReader treebankReader = null;
				
				if (treebankPath.length()>0) {
					File treebankFile = new File(treebankPath);
					treebankReader = treebankUploadService.getXmlReader(treebankFile);
				} else {
					TreebankSubSet trainingSet = TreebankSubSet.TRAINING;
					treebankReader = treebankService.getDatabaseReader(trainingSet, startSentence);
				}
				treebankReader.setSentenceCount(sentenceCount);

				SentenceDetectorAnnotatedCorpusReader reader = treebankExportService.getSentenceDetectorAnnotatedCorpusReader(treebankReader);
				
				ExternalResourceFinder externalResourceFinder = null;
				if (externalResourcePath!=null) {
					externalResourceFinder = sentenceDetectorFeatureService.getExternalResourceFinder();
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
				
				File sentenceFeatureFile = new File(sentenceFeatureFilePath);
				Scanner scanner = new Scanner(sentenceFeatureFile);
				List<String> featureDescriptors = new ArrayList<String>();
				while (scanner.hasNextLine()) {
					String descriptor = scanner.nextLine();
					featureDescriptors.add(descriptor);
					LOG.debug(descriptor);
				}
				scanner.close();

				Set<SentenceDetectorFeature<?>> features = sentenceDetectorFeatureService.getFeatureSet(featureDescriptors);

				CorpusEventStream tokeniserEventStream = sentenceDetectorService.getSentenceDetectorEventStream(reader, features);

				Map<String,Object> trainParameters = new HashMap<String, Object>();
				if (algorithm.equals(MachineLearningAlgorithm.MaxEnt)) {
					trainParameters.put(MaxentModelTrainer.MaxentModelParameter.Iterations.name(), iterations);
					trainParameters.put(MaxentModelTrainer.MaxentModelParameter.Cutoff.name(), cutoff);
				} else if (algorithm.equals(MachineLearningAlgorithm.Perceptron)) {
					trainParameters.put(PerceptronModelTrainer.PerceptronModelParameter.Iterations.name(), iterations);
					trainParameters.put(PerceptronModelTrainer.PerceptronModelParameter.Cutoff.name(), cutoff);
					
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
				

				ModelTrainer<SentenceDetectorOutcome> trainer = machineLearningService.getModelTrainer(algorithm, trainParameters);
				
				DecisionFactory<SentenceDetectorOutcome> decisionFactory = sentenceDetectorService.getDecisionFactory();
				MachineLearningModel<SentenceDetectorOutcome> sentenceModel = trainer.trainModel(tokeniserEventStream, decisionFactory, featureDescriptors);
				if (externalResourceFinder!=null)
					sentenceModel.setExternalResources(externalResourceFinder.getExternalResources());
				sentenceModel.persist(sentenceModelFile);

			} else if (command.equals("evaluate")) {
				if (sentenceModelFilePath.length()==0)
					throw new RuntimeException("Missing argument: sentenceModel");
				
				TreebankReader treebankReader = null;
				
				if (treebankPath.length()>0) {
					File treebankFile = new File(treebankPath);
					treebankReader = treebankUploadService.getXmlReader(treebankFile);
				} else {
					TreebankSubSet testSection = TreebankSubSet.DEV;
					treebankReader = treebankService.getDatabaseReader(testSection, startSentence);
				}
				treebankReader.setSentenceCount(sentenceCount);

				SentenceDetectorAnnotatedCorpusReader reader = treebankExportService.getSentenceDetectorAnnotatedCorpusReader(treebankReader);

				Writer errorFileWriter = null;
				if (outDirPath!=null&&outDirPath.length()>0) {
					File outDir = new File(outDirPath);
					outDir.mkdirs();
					String modelName = sentenceModelFilePath.substring(sentenceModelFilePath.lastIndexOf('/')+1, sentenceModelFilePath.indexOf('.'));
					File errorFile = new File(outDir, modelName + "_errors.txt");
					errorFile.delete();
					errorFile.createNewFile();
					errorFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(errorFile, false),"UTF8"));
				}
				FScoreCalculator<SentenceDetectorOutcome> fScoreCalculator = null;
				
				try {
					ZipInputStream zis = new ZipInputStream(new FileInputStream(sentenceModelFilePath));
					MachineLearningModel<SentenceDetectorOutcome> sentenceModel = machineLearningService.getModel(zis);

					Set<SentenceDetectorFeature<?>> features = sentenceDetectorFeatureService.getFeatureSet(sentenceModel.getFeatureDescriptors());

					SentenceDetector sentenceDetector = sentenceDetectorService.getSentenceDetector(sentenceModel.getDecisionMaker(), features);
					SentenceDetectorEvaluator evaluator = sentenceDetectorService.getEvaluator(sentenceDetector);
					
					fScoreCalculator = evaluator.evaluate(reader, errorFileWriter);
					
					double fscore = fScoreCalculator.getTotalFScore();
					LOG.debug("F-score for " + sentenceModelFilePath + ": " + fscore);
					
					
				} finally {
					if (errorFileWriter!=null) {
						errorFileWriter.flush();
						errorFileWriter.close();
					}
				}
				
				File fscoreFile = new File(sentenceModelFilePath + ".fscores.csv");
				fScoreCalculator.writeScoresToCSVFile(fscoreFile);	
			}
			
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw e;
		} finally {
			PerformanceMonitor.end();
		}
	}
}
