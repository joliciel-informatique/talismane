package com.joliciel.talismane.trainer.fr;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.frenchTreebank.TreebankReader;
import com.joliciel.frenchTreebank.TreebankService;
import com.joliciel.frenchTreebank.TreebankServiceLocator;
import com.joliciel.frenchTreebank.TreebankSubSet;
import com.joliciel.frenchTreebank.export.TreebankExportService;
import com.joliciel.frenchTreebank.upload.TreebankUploadService;
import com.joliciel.talismane.TalismaneServiceLocator;
import com.joliciel.talismane.filters.DuplicateWhiteSpaceFilter;
import com.joliciel.talismane.filters.TextFilter;
import com.joliciel.talismane.sentenceDetector.SentenceDetector;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorDecision;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorEvaluator;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorService;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorAnnotatedCorpusReader;
import com.joliciel.talismane.sentenceDetector.features.SentenceDetectorFeature;
import com.joliciel.talismane.sentenceDetector.features.SentenceDetectorFeatureService;
import com.joliciel.talismane.utils.CorpusEventStream;
import com.joliciel.talismane.utils.maxent.JolicielMaxentModel;
import com.joliciel.talismane.utils.maxent.MaxentModelTrainer;
import com.joliciel.talismane.utils.stats.FScoreCalculator;
import com.joliciel.talismane.utils.util.LogUtils;
import com.joliciel.talismane.utils.util.PerformanceMonitor;

public class SentenceDetectorMaxentRunner {
    private static final Log LOG = LogFactory.getLog(SentenceDetectorMaxentRunner.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		PerformanceMonitor.start();
		try {
			String command = args[0];
	
			String sentenceModelFilePath = "";
			String sentenceFeatureFilePath = "";
			String outDirPath = "";
			String treebankPath = "";
			int iterations = 0;
			int cutoff = 0;
			int sentenceCount = 0;
			int startSentence = 0;
	
			boolean firstArg = true;
			for (String arg : args) {
				if (firstArg) {
					firstArg = false;
					continue;
				}
				int equalsPos = arg.indexOf('=');
				String argName = arg.substring(0, equalsPos);
				String argValue = arg.substring(equalsPos+1);
				if (argName.equals("sentenceModel"))
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
				else if (argName.equals("treebank"))
					treebankPath = argValue;
				else
					throw new RuntimeException("Unknown argument: " + argName);
			}
			
			TalismaneServiceLocator talismaneServiceLocator = TalismaneServiceLocator.getInstance();

			TreebankServiceLocator treebankServiceLocator = TreebankServiceLocator.getInstance();
			if (treebankPath.length()==0)
				treebankServiceLocator.setDataSourcePropertiesFile("jdbc-ftb.properties");
	
			TreebankService treebankService = treebankServiceLocator.getTreebankService();
	        TreebankUploadService treebankUploadService = treebankServiceLocator.getTreebankUploadServiceLocator().getTreebankUploadService();
			TreebankExportService treebankExportService = treebankServiceLocator.getTreebankExportServiceLocator().getTreebankExportService();
	        
	        SentenceDetectorService sentenceDetectorService = talismaneServiceLocator.getSentenceDetectorServiceLocator().getSentenceDetectorService();
	        SentenceDetectorFeatureService sentenceDetectorFeatureService = talismaneServiceLocator.getSentenceDetectorFeatureServiceLocator().getSentenceDetectorFeatureService();

			if (command.equals("train")) {
				if (sentenceModelFilePath.length()==0)
					throw new RuntimeException("Missing argument: model");
	
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

				List<TextFilter> textFilters = new ArrayList<TextFilter>();
				textFilters.add(new DuplicateWhiteSpaceFilter());
				CorpusEventStream tokeniserEventStream = sentenceDetectorService.getSentenceDetectorEventStream(reader, features, textFilters);
				MaxentModelTrainer modelTrainer = new MaxentModelTrainer(tokeniserEventStream);
				modelTrainer.setCutoff(cutoff);
				modelTrainer.setIterations(iterations);
				
				JolicielMaxentModel jolicielMaxentModel = new JolicielMaxentModel(modelTrainer, featureDescriptors);
				jolicielMaxentModel.persist(sentenceModelFile);

			} else if (command.equals("evaluate")) {
				if (sentenceModelFilePath.length()==0)
					throw new RuntimeException("Missing argument: model");
				
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
				FScoreCalculator<SentenceDetectorDecision> fScoreCalculator = null;
				
				try {
					File sentenceModelFile = new File(sentenceModelFilePath);
					JolicielMaxentModel sentenceModelFileMaxentModel = new JolicielMaxentModel(sentenceModelFile);
					Set<SentenceDetectorFeature<?>> features = sentenceDetectorFeatureService.getFeatureSet(sentenceModelFileMaxentModel.getFeatureDescriptors());

					SentenceDetector sentenceDetector = sentenceDetectorService.getSentenceDetector(sentenceModelFileMaxentModel.getDecisionMaker(), features);
					SentenceDetectorEvaluator evaluator = sentenceDetectorService.getEvaluator(sentenceDetector);
					evaluator.addTextFilter(new DuplicateWhiteSpaceFilter());
					
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
