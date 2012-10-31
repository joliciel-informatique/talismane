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
import java.util.TreeSet;
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
import com.joliciel.lefff.LefffMemoryBase;
import com.joliciel.lefff.LefffMemoryLoader;
import com.joliciel.talismane.TalismaneServiceLocator;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.machineLearning.AnalysisObserver;
import com.joliciel.talismane.machineLearning.CorpusEventStream;
import com.joliciel.talismane.machineLearning.MachineLearningModel;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.machineLearning.ModelTrainer;
import com.joliciel.talismane.machineLearning.MachineLearningModel.MachineLearningAlgorithm;
import com.joliciel.talismane.machineLearning.linearsvm.LinearSVMModelTrainer;
import com.joliciel.talismane.machineLearning.linearsvm.LinearSVMModelTrainer.LinearSVMSolverType;
import com.joliciel.talismane.machineLearning.maxent.MaxentModelTrainer;
import com.joliciel.talismane.machineLearning.maxent.PerceptronModelTrainer;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.posTagger.PosTagAnnotatedCorpusReader;
import com.joliciel.talismane.posTagger.PosTagger;
import com.joliciel.talismane.posTagger.PosTaggerEvaluator;
import com.joliciel.talismane.posTagger.PosTaggerService;
import com.joliciel.talismane.posTagger.features.PosTaggerFeature;
import com.joliciel.talismane.posTagger.features.PosTaggerFeatureService;
import com.joliciel.talismane.posTagger.features.PosTaggerRule;
import com.joliciel.talismane.stats.FScoreCalculator;
import com.joliciel.talismane.tokeniser.Tokeniser;
import com.joliciel.talismane.tokeniser.TokeniserOutcome;
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
			String tokenFilterPath = "";
			String tokenSequenceFilterPath = "";
			String posTaggerPreprocessingFilterPath = "";
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
			MachineLearningAlgorithm algorithm = MachineLearningAlgorithm.MaxEnt;
			
			double constraintViolationCost = -1;
			double epsilon = -1;
			LinearSVMSolverType solverType = null;
			boolean perceptronAveraging = false;
			boolean perceptronSkippedAveraging = false;
			double perceptronTolerance = -1;
			
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
				else if (argName.equals("tokenFilters"))
					tokenFilterPath = argValue;
				else if (argName.equals("tokenSequenceFilters"))
					tokenSequenceFilterPath = argValue;
				else if (argName.equals("posTaggerPreprocessingFilters"))
					posTaggerPreprocessingFilterPath = argValue;
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
			
	        PosTaggerService posTaggerService = talismaneServiceLocator.getPosTaggerServiceLocator().getPosTaggerService();
	        File posTagSetFile = new File(posTagSetPath);
			PosTagSet posTagSet = posTaggerService.getPosTagSet(posTagSetFile);
			
			TalismaneSession.setPosTagSet(posTagSet);

			LefffMemoryLoader loader = new LefffMemoryLoader();
        	File memoryBaseFile = new File(lefffPath);
        	LefffMemoryBase lefffMemoryBase = null;
        	lefffMemoryBase = loader.deserializeMemoryBase(memoryBaseFile);
	       	lefffMemoryBase.setPosTagSet(posTagSet);
	        
        	TalismaneSession.setLexicon(lefffMemoryBase);
	 
	        TokenFeatureService tokenFeatureService = talismaneServiceLocator.getTokenFeatureServiceLocator().getTokenFeatureService();
	        TokeniserPatternService tokeniserPatternService = talismaneServiceLocator.getTokenPatternServiceLocator().getTokeniserPatternService();
	        TokenFilterService tokenFilterService = talismaneServiceLocator.getTokenFilterServiceLocator().getTokenFilterService();

			PosTaggerFeatureService posTaggerFeatureService = talismaneServiceLocator.getPosTaggerFeatureServiceLocator().getPosTaggerFeatureService();

			TreebankServiceLocator treebankServiceLocator = TreebankServiceLocator.getInstance(talismaneServiceLocator);
			if (treebankPath.length()==0)
				treebankServiceLocator.setDataSourcePropertiesFile("jdbc-ftb.properties");
	        
			TreebankService treebankService = treebankServiceLocator.getTreebankService();
	        TreebankUploadService treebankUploadService = treebankServiceLocator.getTreebankUploadServiceLocator().getTreebankUploadService();
			TreebankExportService treebankExportService = treebankServiceLocator.getTreebankExportServiceLocator().getTreebankExportService();
			
			MachineLearningService machineLearningService = talismaneServiceLocator.getMachineLearningServiceLocator().getMachineLearningService();

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
				List<String> featureDescriptors = new ArrayList<String>();
				while (scanner.hasNextLine()) {
					String descriptor = scanner.nextLine();
					featureDescriptors.add(descriptor);
					LOG.debug(descriptor);
				}
				Set<PosTaggerFeature<?>> posTaggerFeatures = posTaggerFeatureService.getFeatureSet(featureDescriptors);
			
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
					reader.addTokenSequenceFilter(tokenFilterWrapper);
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
						reader.addTokenSequenceFilter(tokenSequenceFilter);
					}
				}
				
				List<String> posTaggerPreprocessingFilterDescriptors = new ArrayList<String>();
				if (posTaggerPreprocessingFilterPath!=null && posTaggerPreprocessingFilterPath.length()>0) {
					LOG.debug("From: " + tokenSequenceFilterPath);
					File posTaggerPreprocessingFilterFile = new File(posTaggerPreprocessingFilterPath);
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
						reader.addTokenSequenceFilter(tokenSequenceFilter);
					}
				}
				
				CorpusEventStream posTagEventStream = posTaggerService.getPosTagEventStream(reader, posTaggerFeatures);
				
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
				descriptors.put(PosTagger.POSTAG_PREPROCESSING_FILTER_DESCRIPTOR_KEY, posTaggerPreprocessingFilterDescriptors);
				MachineLearningModel<PosTag> posTaggerModel = trainer.trainModel(posTagEventStream, posTagSet, descriptors);			
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
					Set<PosTaggerFeature<?>> posTaggerFeatures = posTaggerFeatureService.getFeatureSet(posTaggerModel.getFeatureDescriptors());
					
					PosTagger posTagger = posTaggerService.getPosTagger(posTaggerFeatures, posTagSet, posTaggerModel.getDecisionMaker(), beamWidth);
					
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
							reader.addTokenSequenceFilter(tokenFilterWrapper);
						}
						
						List<String> tokenSequenceFilterDescriptors = posTaggerModel.getDescriptors().get(TokenFilterService.TOKEN_SEQUENCE_FILTER_DESCRIPTOR_KEY);
						if (tokenSequenceFilterDescriptors!=null) {
							for (String descriptor : tokenSequenceFilterDescriptors) {
								if (descriptor.length()>0 && !descriptor.startsWith("#")) {
									TokenSequenceFilter tokenSequenceFilter = tokenFilterService.getTokenSequenceFilter(descriptor);
									reader.addTokenSequenceFilter(tokenSequenceFilter);
								}
							}
						}
					}
					
					List<String> posTaggerPreprocessingFilters = posTaggerModel.getDescriptors().get(PosTagger.POSTAG_PREPROCESSING_FILTER_DESCRIPTOR_KEY);
					if (posTaggerPreprocessingFilters!=null) {
						for (String descriptor : posTaggerPreprocessingFilters) {
							if (descriptor.length()>0 && !descriptor.startsWith("#")) {
								TokenSequenceFilter tokenSequenceFilter = tokenFilterService.getTokenSequenceFilter(descriptor);
								reader.addTokenSequenceFilter(tokenSequenceFilter);
								posTagger.addPreprocessingFilter(tokenSequenceFilter);
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
