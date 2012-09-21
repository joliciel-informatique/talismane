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
import com.joliciel.lefff.LefffMemoryBase;
import com.joliciel.lefff.LefffMemoryLoader;
import com.joliciel.talismane.TalismaneServiceLocator;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.machineLearning.CorpusEventStream;
import com.joliciel.talismane.machineLearning.DecisionFactory;
import com.joliciel.talismane.machineLearning.maxent.JolicielMaxentModel;
import com.joliciel.talismane.machineLearning.maxent.MaxentModelTrainer;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.posTagger.PosTaggerService;
import com.joliciel.talismane.posTagger.PosTaggerServiceLocator;
import com.joliciel.talismane.stats.FScoreCalculator;
import com.joliciel.talismane.tokeniser.Tokeniser;
import com.joliciel.talismane.tokeniser.TokeniserOutcome;
import com.joliciel.talismane.tokeniser.TokeniserEvaluator;
import com.joliciel.talismane.tokeniser.TokeniserService;
import com.joliciel.talismane.tokeniser.TokeniserServiceLocator;
import com.joliciel.talismane.tokeniser.TokeniserAnnotatedCorpusReader;
import com.joliciel.talismane.tokeniser.features.TokenFeatureService;
import com.joliciel.talismane.tokeniser.features.TokeniserContextFeature;
import com.joliciel.talismane.tokeniser.features.TokeniserFeatureServiceLocator;
import com.joliciel.talismane.tokeniser.filters.NumberFilter;
import com.joliciel.talismane.tokeniser.filters.PrettyQuotesFilter;
import com.joliciel.talismane.tokeniser.filters.french.LowercaseFirstWordFrenchFilter;
import com.joliciel.talismane.tokeniser.filters.french.UpperCaseSeriesFilter;
import com.joliciel.talismane.tokeniser.patterns.TokeniserPatternManager;
import com.joliciel.talismane.tokeniser.patterns.TokeniserPatternService;
import com.joliciel.talismane.tokeniser.patterns.TokeniserPatternServiceLocator;
import com.joliciel.talismane.utils.PerformanceMonitor;

public class TokeniserMaxentRunner {
    private static final Log LOG = LogFactory.getLog(TokeniserMaxentRunner.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		String command = args[0];

		String tokeniserModelFilePath = "";
		String tokeniserFeatureFilePath = "";
		String tokeniserPatternFilePath = "";
		String lefffPath = "";
		String treebankPath = "";
		String outDirPath = "";
		String tokeniserType = "maxent";
		int iterations = 0;
		int cutoff = 0;
		int sentenceCount = 0;
		int beamWidth = 10;
		int startSentence = 0;
		String posTagSetPath = "";
		String sentenceNumber = "";

		boolean firstArg = true;
		for (String arg : args) {
			if (firstArg) {
				firstArg = false;
				continue;
			}
			int equalsPos = arg.indexOf('=');
			String argName = arg.substring(0, equalsPos);
			String argValue = arg.substring(equalsPos+1);
			if (argName.equals("tokeniserModel"))
				tokeniserModelFilePath = argValue;
			else if (argName.equals("tokeniserFeatures"))
				tokeniserFeatureFilePath = argValue;
			else if (argName.equals("tokeniserPatterns"))
				tokeniserPatternFilePath = argValue;
			else if (argName.equals("posTagSet"))
				posTagSetPath = argValue;
			else if (argName.equals("iterations"))
				iterations = Integer.parseInt(argValue);
			else if (argName.equals("cutoff"))
				cutoff = Integer.parseInt(argValue);
			else if (argName.equals("outDir")) 
				outDirPath = argValue;
			else if (argName.equals("tokeniser")) 
				tokeniserType = argValue;
			else if (argName.equals("treebank"))
				treebankPath = argValue;
			else if (argName.equals("lefff"))
				lefffPath = argValue;
			else if (argName.equals("sentenceCount"))
				sentenceCount = Integer.parseInt(argValue);
			else if (argName.equals("startSentence"))
				startSentence = Integer.parseInt(argValue);
			else if (argName.equals("sentence"))
				sentenceNumber = argValue;
			else if (argName.equals("beamWidth"))
				beamWidth = Integer.parseInt(argValue);
			else
				throw new RuntimeException("Unknown argument: " + argName);
		}
		
		if (lefffPath.length()==0)
			throw new RuntimeException("Missing argument: lefff");
		if (posTagSetPath.length()==0)
			throw new RuntimeException("Missing argument: posTagSet");
		
		PerformanceMonitor.start();
		try {
			TalismaneServiceLocator talismaneServiceLocator = TalismaneServiceLocator.getInstance();

	        PosTaggerServiceLocator posTaggerServiceLocator = talismaneServiceLocator.getPosTaggerServiceLocator();
	        PosTaggerService posTaggerService = posTaggerServiceLocator.getPosTaggerService();
	        File posTagSetFile = new File(posTagSetPath);
			PosTagSet posTagSet = posTaggerService.getPosTagSet(posTagSetFile);
			
	       	TalismaneSession.setPosTagSet(posTagSet);
	        
	    	LefffMemoryLoader loader = new LefffMemoryLoader();
	    	File memoryBaseFile = new File(lefffPath);
	    	LefffMemoryBase lefffMemoryBase = null;
	    	lefffMemoryBase = loader.deserializeMemoryBase(memoryBaseFile);
	    	lefffMemoryBase.setPosTagSet(posTagSet);

	    	TalismaneSession.setLexiconService(lefffMemoryBase);
	 
	        TokeniserServiceLocator tokeniserServiceLocator = talismaneServiceLocator.getTokeniserServiceLocator();
	        TokeniserService tokeniserService = tokeniserServiceLocator.getTokeniserService();
	        TokeniserFeatureServiceLocator tokeniserFeatureServiceLocator = talismaneServiceLocator.getTokeniserFeatureServiceLocator();
	        TokenFeatureService tokenFeatureService = tokeniserFeatureServiceLocator.getTokenFeatureService();
	        TokeniserPatternServiceLocator tokeniserPatternServiceLocator = talismaneServiceLocator.getTokenPatternServiceLocator();
	        TokeniserPatternService tokeniserPatternService = tokeniserPatternServiceLocator.getTokeniserPatternService();
	
			TreebankServiceLocator treebankServiceLocator = TreebankServiceLocator.getInstance();
	        treebankServiceLocator.setTokeniserService(tokeniserService);
	        treebankServiceLocator.setPosTaggerService(posTaggerService);
			if (treebankPath.length()==0)
				treebankServiceLocator.setDataSourcePropertiesFile("jdbc-ftb.properties");
	
			TreebankService treebankService = treebankServiceLocator.getTreebankService();
	        TreebankUploadService treebankUploadService = treebankServiceLocator.getTreebankUploadServiceLocator().getTreebankUploadService();
	        TreebankExportService treebankExportService = treebankServiceLocator.getTreebankExportServiceLocator().getTreebankExportService();

			if (command.equals("train")) {
				if (tokeniserModelFilePath.length()==0)
					throw new RuntimeException("Missing argument: tokeniserModel");
				if (tokeniserFeatureFilePath.length()==0)
					throw new RuntimeException("Missing argument: tokeniserFeatures");
				if (tokeniserPatternFilePath.length()==0)
					throw new RuntimeException("Missing argument: tokeniserPatterns");
				String modelDirPath = tokeniserModelFilePath.substring(0, tokeniserModelFilePath.lastIndexOf("/"));
				File modelDir = new File(modelDirPath);
				modelDir.mkdirs();
				
				File tokeniserModelFile = new File(tokeniserModelFilePath);
				
				TreebankReader treebankReader = null;
				
				if (treebankPath.length()>0) {
					File treebankFile = new File(treebankPath);
					treebankReader = treebankUploadService.getXmlReader(treebankFile, sentenceNumber);
				} else {
					TreebankSubSet testSection = TreebankSubSet.TRAINING;
					treebankReader = treebankService.getDatabaseReader(testSection, startSentence);
				}
				treebankReader.setSentenceCount(sentenceCount);

				TokeniserAnnotatedCorpusReader reader = treebankExportService.getTokeniserAnnotatedCorpusReader(treebankReader);
				
				reader.addTokenFilter(new NumberFilter());
				reader.addTokenFilter(new PrettyQuotesFilter());
				reader.addTokenFilter(new UpperCaseSeriesFilter());
				reader.addTokenFilter(new LowercaseFirstWordFrenchFilter());
	
				File tokeniserPatternFile = new File(tokeniserPatternFilePath);
				Scanner scanner = new Scanner(tokeniserPatternFile);
				List<String> patternDescriptors = new ArrayList<String>();
				while (scanner.hasNextLine()) {
					String descriptor = scanner.nextLine();
					patternDescriptors.add(descriptor);
					LOG.debug(descriptor);
				}
				scanner.close();
				
				TokeniserPatternManager tokeniserPatternManager =
					tokeniserPatternService.getPatternManager(patternDescriptors);
	
				File tokeniserFeatureFile = new File(tokeniserFeatureFilePath);
				scanner = new Scanner(tokeniserFeatureFile);
				List<String> featureDescriptors = new ArrayList<String>();
				while (scanner.hasNextLine()) {
					String descriptor = scanner.nextLine();
					featureDescriptors.add(descriptor);
					LOG.debug(descriptor);
				}
				scanner.close();
				Set<TokeniserContextFeature<?>> tokeniserContextFeatures = tokenFeatureService.getTokeniserContextFeatureSet(featureDescriptors, tokeniserPatternManager.getParsedTestPatterns());
	
				CorpusEventStream tokeniserEventStream = tokeniserService.getTokeniserEventStream(reader, tokeniserContextFeatures, tokeniserPatternManager);
				MaxentModelTrainer modelTrainer = new MaxentModelTrainer(tokeniserEventStream);
				modelTrainer.setCutoff(cutoff);
				modelTrainer.setIterations(iterations);
	
				DecisionFactory<TokeniserOutcome> decisionFactory = tokeniserService.getDecisionFactory();
				JolicielMaxentModel<TokeniserOutcome> jolicielMaxentModel = new JolicielMaxentModel<TokeniserOutcome>(modelTrainer, featureDescriptors, decisionFactory);
				jolicielMaxentModel.setPatternDescriptors(patternDescriptors);
				jolicielMaxentModel.persist(tokeniserModelFile);
	
			} else if (command.equals("evaluate")) {
				TreebankReader treebankReader = null;
				
				if (treebankPath.length()>0) {
					File treebankFile = new File(treebankPath);
					treebankReader = treebankUploadService.getXmlReader(treebankFile, sentenceNumber);
				} else {
					TreebankSubSet testSection = TreebankSubSet.DEV;
					treebankReader = treebankService.getDatabaseReader(testSection, startSentence);
				}
				treebankReader.setSentenceCount(sentenceCount);				
				TokeniserAnnotatedCorpusReader reader = treebankExportService.getTokeniserAnnotatedCorpusReader(treebankReader);
				
				reader.addTokenFilter(new NumberFilter());
				reader.addTokenFilter(new PrettyQuotesFilter());
				reader.addTokenFilter(new UpperCaseSeriesFilter());
				reader.addTokenFilter(new LowercaseFirstWordFrenchFilter());
				
				File outDir = new File(outDirPath);
				outDir.mkdirs();
				
				Writer errorFileWriter = null;
				String filebase = "results";
				if (tokeniserModelFilePath.length()>0)
					filebase = tokeniserModelFilePath.substring(tokeniserModelFilePath.lastIndexOf('/'));
				File errorFile = new File(outDir, filebase + ".errors.txt");
				errorFile.delete();
				errorFile.createNewFile();
				errorFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(errorFile, false),"UTF8"));
				
				FScoreCalculator<TokeniserOutcome> fScoreCalculator = null;
				try {
					Tokeniser tokeniser = null;
					if (tokeniserType.equalsIgnoreCase("simple")) {
						tokeniser = tokeniserService.getSimpleTokeniser();
					} else {
						if (tokeniserModelFilePath.length()==0)
							throw new RuntimeException("Missing argument: tokeniserModel");
						File tokeniserModelFile = new File(tokeniserModelFilePath);
						JolicielMaxentModel<TokeniserOutcome> tokeniserJolicielMaxentModel = new JolicielMaxentModel<TokeniserOutcome>(tokeniserModelFile);
						TokeniserPatternManager tokeniserPatternManager =
							tokeniserPatternService.getPatternManager(tokeniserJolicielMaxentModel.getPatternDescriptors());
						Set<TokeniserContextFeature<?>> tokeniserContextFeatures = tokenFeatureService.getTokeniserContextFeatureSet(tokeniserJolicielMaxentModel.getFeatureDescriptors(), tokeniserPatternManager.getParsedTestPatterns());
	
						if (tokeniserType.equalsIgnoreCase("pattern")) {
							tokeniser = tokeniserPatternService.getPatternTokeniser(tokeniserPatternManager, tokeniserContextFeatures, null, beamWidth);
						} else if (tokeniserType.equalsIgnoreCase("maxent")) {
							tokeniser = tokeniserPatternService.getPatternTokeniser(tokeniserPatternManager, tokeniserContextFeatures, tokeniserJolicielMaxentModel.getDecisionMaker(), beamWidth);
							
						} else {
							throw new RuntimeException("Unknown tokeniser type: " + tokeniserType);
						}
					}
					
					tokeniser.addTokenFilter(new NumberFilter());
					tokeniser.addTokenFilter(new PrettyQuotesFilter());
					tokeniser.addTokenFilter(new UpperCaseSeriesFilter());
					tokeniser.addTokenFilter(new LowercaseFirstWordFrenchFilter());
					
					TokeniserEvaluator evaluator = tokeniserService.getTokeniserEvaluator(tokeniser, Tokeniser.SEPARATORS);
					
					fScoreCalculator = evaluator.evaluate(reader, errorFileWriter);
					
					double fscore = fScoreCalculator.getTotalFScore();
					LOG.debug("F-score for " + tokeniserModelFilePath + ": " + fscore);
					
					
				} finally {
					if (errorFileWriter!=null) {
						errorFileWriter.flush();
						errorFileWriter.close();
					}
				}
				
				File fscoreFile = new File(outDir, filebase + ".fscores.csv");
				fScoreCalculator.writeScoresToCSVFile(fscoreFile);	
			}
		} finally {
			PerformanceMonitor.end();
		}
	}
}
