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
import java.util.TreeSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.frenchTreebank.TreebankReader;
import com.joliciel.frenchTreebank.TreebankService;
import com.joliciel.frenchTreebank.TreebankServiceLocator;
import com.joliciel.frenchTreebank.TreebankSubSet;
import com.joliciel.frenchTreebank.export.FtbPosTagMapper;
import com.joliciel.frenchTreebank.export.TreebankExportService;
import com.joliciel.frenchTreebank.upload.TreebankUploadService;
import com.joliciel.lefff.LefffMemoryBase;
import com.joliciel.lefff.LefffMemoryLoader;
import com.joliciel.talismane.TalismaneServiceLocator;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.machineLearning.CorpusEventStream;
import com.joliciel.talismane.machineLearning.maxent.JolicielMaxentModel;
import com.joliciel.talismane.machineLearning.maxent.MaxentDetailedAnalysisWriter;
import com.joliciel.talismane.machineLearning.maxent.MaxentModelTrainer;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.posTagger.PosTagAnnotatedCorpusReader;
import com.joliciel.talismane.posTagger.PosTagger;
import com.joliciel.talismane.posTagger.PosTaggerEvaluator;
import com.joliciel.talismane.posTagger.PosTaggerService;
import com.joliciel.talismane.posTagger.PosTaggerServiceLocator;
import com.joliciel.talismane.posTagger.features.PosTaggerFeature;
import com.joliciel.talismane.posTagger.features.PosTaggerFeatureService;
import com.joliciel.talismane.posTagger.features.PosTaggerRule;
import com.joliciel.talismane.stats.FScoreCalculator;
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

public class PosTaggerMaxentRunner {
    private static final Log LOG = LogFactory.getLog(PosTaggerMaxentRunner.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		if (args.length==0) {
			System.out.println("Missing command.");
			return;
		}
		PerformanceMonitor.start();
		try {
			String command = args[0];
	
			String posTaggerModelFilePath = "";
			String tokeniserModelFilePath = "";
			String posTaggerFeatureFilePath = "";
			String posTaggerRuleFilePath = "";
			String outDirPath = "";
			int iterations = 0;
			int cutoff = 0;
			int sentenceCount = 0;
			int startSentence = 0;
			int beamWidth = 10;
			boolean propagateBeam = true;
			String posTagSetPath = "";
			String posTagMapPath = "";
			String treebankPath = "";
			String lefffPath = "";
			boolean includeDetails = false;
			boolean includeSentences = true;
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
				if (argName.equals("posTaggerModel"))
					posTaggerModelFilePath = argValue;
				else if (argName.equals("posTaggerFeatures")) 
					posTaggerFeatureFilePath = argValue;
				else if (argName.equals("posTaggerRules")) 
					posTaggerRuleFilePath = argValue;
				else if (argName.equals("posTagSet"))
					posTagSetPath = argValue;
				else if (argName.equals("posTagMap"))
					posTagMapPath = argValue;
				else if (argName.equals("treebank"))
					treebankPath = argValue;
				else if (argName.equals("lefff"))
					lefffPath = argValue;
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
				else if (argName.equals("sentence"))
					sentenceNumber = argValue;
				else
					throw new RuntimeException("Unknown argument: " + argName);
			}
			
			if (lefffPath.length()==0)
				throw new RuntimeException("Missing argument: lefff");
			if (posTagSetPath.length()==0)
				throw new RuntimeException("Missing argument: posTagSet");
			if (posTagMapPath.length()==0)
				throw new RuntimeException("Missing argument: posTagMap");
			
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
	 
	        TokeniserService tokeniserService = talismaneServiceLocator.getTokeniserServiceLocator().getTokeniserService();
	        TokenFeatureService tokenFeatureService = talismaneServiceLocator.getTokeniserFeatureServiceLocator().getTokenFeatureService();
	        TokeniserPatternService tokeniserPatternService = talismaneServiceLocator.getTokenPatternServiceLocator().getTokeniserPatternService();

			PosTaggerFeatureService posTaggerFeatureService = talismaneServiceLocator.getPosTaggerFeatureServiceLocator().getPosTaggerFeatureService();

			TreebankServiceLocator treebankServiceLocator = TreebankServiceLocator.getInstance();
	        treebankServiceLocator.setTokeniserService(tokeniserService);
	        treebankServiceLocator.setPosTaggerService(posTaggerService);
			if (treebankPath.length()==0)
				treebankServiceLocator.setDataSourcePropertiesFile("jdbc-ftb.properties");
	        
			TreebankService treebankService = treebankServiceLocator.getTreebankService();
	        TreebankUploadService treebankUploadService = treebankServiceLocator.getTreebankUploadServiceLocator().getTreebankUploadService();
			TreebankExportService treebankExportService = treebankServiceLocator.getTreebankExportServiceLocator().getTreebankExportService();
			
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
				
				File posTaggerTokenFeatureFile = new File(posTaggerFeatureFilePath);
				Scanner scanner = new Scanner(posTaggerTokenFeatureFile);
				List<String> descriptors = new ArrayList<String>();
				while (scanner.hasNextLine()) {
					String descriptor = scanner.nextLine();
					descriptors.add(descriptor);
					LOG.debug(descriptor);
				}
				Set<PosTaggerFeature<?>> posTaggerFeatures = posTaggerFeatureService.getFeatureSet(descriptors);
			
				TreebankReader treebankReader = null;
				
				if (treebankPath.length()>0) {
					File treebankFile = new File(treebankPath);
					treebankReader = treebankUploadService.getXmlReader(treebankFile);
				} else {
					TreebankSubSet trainingSet = TreebankSubSet.TRAINING;
					treebankReader = treebankService.getDatabaseReader(trainingSet, startSentence);
				}
				treebankReader.setSentenceCount(sentenceCount);
				
				File posTagMapFile = new File(posTagMapPath);
				FtbPosTagMapper ftbPosTagMapper = treebankExportService.getFtbPosTagMapper(posTagMapFile, posTagSet);
				PosTagAnnotatedCorpusReader reader = treebankExportService.getPosTagAnnotatedCorpusReader(treebankReader, ftbPosTagMapper);
				
				reader.addTokenFilter(new NumberFilter());
				reader.addTokenFilter(new PrettyQuotesFilter());
				reader.addTokenFilter(new UpperCaseSeriesFilter());
				reader.addTokenFilter(new LowercaseFirstWordFrenchFilter());
				reader.addTokenFilter(new EmptyTokenAfterDuFilter());
				reader.addTokenFilter(new EmptyTokenBeforeDuquelFilter());
				
				CorpusEventStream posTagEventStream = posTaggerService.getPosTagEventStream(reader, posTaggerFeatures);
				
				MaxentModelTrainer modelTrainer = new MaxentModelTrainer(posTagEventStream);
				modelTrainer.setCutoff(cutoff);
				modelTrainer.setIterations(iterations);

				JolicielMaxentModel<PosTag> jolicielMaxentModel = new JolicielMaxentModel<PosTag>(modelTrainer, descriptors, posTagSet);
				jolicielMaxentModel.persist(modelFile);

			} else if (command.equals("evaluate")) {
				if (posTaggerModelFilePath.length()==0)
					throw new RuntimeException("Missing argument: posTaggerModel");
				if (outDirPath.length()==0)
					throw new RuntimeException("Missing argument: outdir");
				
				File posTaggerModelFile = new File(posTaggerModelFilePath);
				JolicielMaxentModel<PosTag> posTaggerJolicielMaxentModel = new JolicielMaxentModel<PosTag>(posTaggerModelFile);
				
				Tokeniser tokeniser = null;
				if (tokeniserModelFilePath.length()>0) {
					File tokeniserModelFile = new File(tokeniserModelFilePath);
					JolicielMaxentModel<TokeniserOutcome> tokeniserJolicielMaxentModel = new JolicielMaxentModel<TokeniserOutcome>(tokeniserModelFile);
					TokeniserPatternManager tokeniserPatternManager =
						tokeniserPatternService.getPatternManager(tokeniserJolicielMaxentModel.getPatternDescriptors());
					Set<TokeniserContextFeature<?>> tokeniserContextFeatures = tokenFeatureService.getTokeniserContextFeatureSet(tokeniserJolicielMaxentModel.getFeatureDescriptors(), tokeniserPatternManager.getParsedTestPatterns());
					tokeniser = tokeniserPatternService.getPatternTokeniser(tokeniserPatternManager, tokeniserContextFeatures, tokeniserJolicielMaxentModel.getDecisionMaker(), beamWidth);
				}
				
				TreebankReader treebankReader = null;
				
				if (treebankPath.length()>0) {
					File treebankFile = new File(treebankPath);
					treebankReader = treebankUploadService.getXmlReader(treebankFile, sentenceNumber);
				} else {
					TreebankSubSet testSection = TreebankSubSet.DEV;
					treebankReader = treebankService.getDatabaseReader(testSection, startSentence);
				}
				treebankReader.setSentenceCount(sentenceCount);
				
				File posTagMapFile = new File(posTagMapPath);
				FtbPosTagMapper ftbPosTagMapper = treebankExportService.getFtbPosTagMapper(posTagMapFile, posTagSet);
				PosTagAnnotatedCorpusReader reader = treebankExportService.getPosTagAnnotatedCorpusReader(treebankReader, ftbPosTagMapper);
				
				if (tokeniser!=null) {
					tokeniser.addTokenFilter(new NumberFilter());
					tokeniser.addTokenFilter(new PrettyQuotesFilter());
					tokeniser.addTokenFilter(new UpperCaseSeriesFilter());
					tokeniser.addTokenFilter(new LowercaseFirstWordFrenchFilter());

				} else {
					reader.addTokenFilter(new NumberFilter());
					reader.addTokenFilter(new PrettyQuotesFilter());
					reader.addTokenFilter(new UpperCaseSeriesFilter());
					reader.addTokenFilter(new LowercaseFirstWordFrenchFilter());
					reader.addTokenFilter(new EmptyTokenAfterDuFilter());
					reader.addTokenFilter(new EmptyTokenBeforeDuquelFilter());
				}

//				Set<String> unknownWords = treebankService.findUnknownWords(trainingSection, testSection);
				Set<String> unknownWords = new TreeSet<String>();
				String modelName = posTaggerModelFilePath.substring(posTaggerModelFilePath.lastIndexOf('/')+1, posTaggerModelFilePath.indexOf('.'));
				File outDir = new File(outDirPath);
				outDir.mkdirs();

				Writer csvFileWriter = null;
				if (includeSentences) {
					File csvFile = new File(outDir, modelName + "_sentences.csv");
					csvFile.delete();
					csvFile.createNewFile();
					csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false),"UTF8"));
				}
				FScoreCalculator<String> fScoreCalculator = null;
				
				try {
					Set<PosTaggerFeature<?>> posTaggerFeatures = posTaggerFeatureService.getFeatureSet(posTaggerJolicielMaxentModel.getFeatureDescriptors());
					
					PosTagger posTagger = posTaggerService.getPosTagger(posTaggerFeatures, posTagSet, posTaggerJolicielMaxentModel.getDecisionMaker(), beamWidth);
					if (tokeniser!=null) {
						posTagger.addPreprocessingFilter(new EmptyTokenAfterDuFilter());
						posTagger.addPreprocessingFilter(new EmptyTokenBeforeDuquelFilter());
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
						MaxentDetailedAnalysisWriter observer = new MaxentDetailedAnalysisWriter(posTaggerJolicielMaxentModel.getModel(), detailsFile);
						posTagger.addObserver(observer);
					}
					
					PosTaggerEvaluator evaluator = posTaggerService.getPosTaggerEvaluator(posTagger, csvFileWriter);
					evaluator.setUnknownWords(unknownWords);
					if (tokeniser!=null) {
						evaluator.setTokeniser(tokeniser);
					}
					evaluator.setPropagateBeam(propagateBeam);
					
					fScoreCalculator = evaluator.evaluate(reader);
					
					double unknownLexiconFScore = evaluator.getFscoreUnknownInLexicon().getTotalFScore();
					LOG.debug("F-score for words unknown in lexicon " + posTaggerModelFilePath + ": " + unknownLexiconFScore);
					double unknownCorpusFScore = evaluator.getFscoreUnknownInCorpus().getTotalFScore();
					LOG.debug("F-score for words unknown in corpus " + posTaggerModelFilePath + ": " + unknownCorpusFScore);
					
					double fscore = fScoreCalculator.getTotalFScore();
					LOG.debug("F-score for " + posTaggerModelFilePath + ": " + fscore);
					
					
				} finally {
					if (csvFileWriter!=null) {
						csvFileWriter.flush();
						csvFileWriter.close();
					}
				}
				
				File fscoreFile = new File(outDir, modelName + ".fscores.csv");
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
