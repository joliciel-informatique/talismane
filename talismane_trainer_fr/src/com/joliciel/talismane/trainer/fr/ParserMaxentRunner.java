package com.joliciel.talismane.trainer.fr;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.frenchTreebank.TreebankService;
import com.joliciel.frenchTreebank.TreebankServiceLocator;
import com.joliciel.ftbDep.FtbDepReader;
import com.joliciel.lefff.LefffMemoryBase;
import com.joliciel.lefff.LefffMemoryLoader;
import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneServiceLocator;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.machineLearning.CorpusEventStream;
import com.joliciel.talismane.machineLearning.maxent.JolicielMaxentModel;
import com.joliciel.talismane.machineLearning.maxent.MaxentModelTrainer;
import com.joliciel.talismane.parser.NonDeterministicParser;
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
import com.joliciel.talismane.stats.FScoreCalculator;
import com.joliciel.talismane.tokeniser.TokeniserService;
import com.joliciel.talismane.tokeniser.TokeniserServiceLocator;
import com.joliciel.talismane.tokeniser.filters.NumberFilter;
import com.joliciel.talismane.tokeniser.filters.PrettyQuotesFilter;
import com.joliciel.talismane.tokeniser.filters.french.LowercaseFirstWordFrenchFilter;
import com.joliciel.talismane.tokeniser.filters.french.UpperCaseSeriesFilter;
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
		String lefffPath = "";
		int beamWidth = 10;
		boolean includeSentences = true;
		String transitionSystemStr = "ShiftReduce";
		boolean logPerformance = false;

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
			else if (argName.equals("lefff"))
				lefffPath = argValue;
			else if (argName.equals("beamWidth"))
				beamWidth = Integer.parseInt(argValue);
			else if (argName.equals("includeSentences"))
				includeSentences = argValue.equalsIgnoreCase("true");
			else if (argName.equals("transitionSystem"))
				transitionSystemStr = argValue;
			else if (argName.equals("logPerformance"))
				logPerformance = argValue.equalsIgnoreCase("true");
			else
				throw new RuntimeException("Unknown argument: " + argName);
		}
		
		if (posTagSetPath.length()==0) {
			throw new RuntimeException("Missing argument: posTagSet");
		}
		if (lefffPath.length()==0) {
			throw new RuntimeException("Missing argument: lefff");
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
	        
	      	LefffMemoryLoader loader = new LefffMemoryLoader();
        	File memoryBaseFile = new File(lefffPath);
        	LefffMemoryBase lefffMemoryBase = null;
        	lefffMemoryBase = loader.deserializeMemoryBase(memoryBaseFile);
        	lefffMemoryBase.setPosTagSet(posTagSet);
	        
        	TalismaneSession.setPosTagSet(posTagSet);
        	TalismaneSession.setLexiconService(lefffMemoryBase);
	 
	        TokeniserServiceLocator tokeniserServiceLocator = talismaneServiceLocator.getTokeniserServiceLocator();
	        TokeniserService tokeniserService = tokeniserServiceLocator.getTokeniserService();
			
			ParserServiceLocator parserServiceLocator = talismaneServiceLocator.getParserServiceLocator();
			ParserService parserService = parserServiceLocator.getParserService();
			ParserFeatureServiceLocator parserFeatureServiceLocator = talismaneServiceLocator.getParserFeatureServiceLocator();
			ParserFeatureService parserFeatureService = parserFeatureServiceLocator.getParserFeatureService();
			
			TreebankServiceLocator treebankServiceLocator = TreebankServiceLocator.getInstance();
			treebankServiceLocator.setTokeniserService(tokeniserService);
			treebankServiceLocator.setPosTaggerService(posTaggerService);
				
			TreebankService treebankService = treebankServiceLocator.getTreebankService();

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

			FtbDepReader corpusReader = new FtbDepReader(corpusFile);
			corpusReader.setMaxSentenceCount(sentenceCount);
			corpusReader.setParserService(parserService);
			corpusReader.setPosTaggerService(posTaggerService);
			corpusReader.setTokeniserService(tokeniserService);
			corpusReader.setPosTagSet(posTagSet);
			
			corpusReader.addTokenFilter(new NumberFilter());
			corpusReader.addTokenFilter(new PrettyQuotesFilter());
			corpusReader.addTokenFilter(new UpperCaseSeriesFilter());
			corpusReader.addTokenFilter(new LowercaseFirstWordFrenchFilter());
			
			if (command.equals("train")) {
				if (parserFeatureFilePath.length()==0)
					throw new RuntimeException("Missing argument: parserFeatures");
				
				File parserFeatureFile = new File(parserFeatureFilePath);
				Scanner scanner = new Scanner(parserFeatureFile);
				List<String> descriptors = new ArrayList<String>();
				while (scanner.hasNextLine()) {
					String descriptor = scanner.nextLine();
					descriptors.add(descriptor);
					LOG.debug(descriptor);
				}
				
				Set<ParseConfigurationFeature<?>> parseFeatures = parserFeatureService.getFeatures(descriptors);


				TransitionSystem transitionSystem = null;
				if (transitionSystemStr.equalsIgnoreCase("ShiftReduce")) {
					transitionSystem = parserService.getShiftReduceTransitionSystem();
				} else if (transitionSystemStr.equalsIgnoreCase("ArcEager")) {
					transitionSystem = parserService.getArcEagerTransitionSystem();
				} else {
					throw new TalismaneException("Unknown transition system: " + transitionSystemStr);
				}
				corpusReader.setTransitionSystem(transitionSystem);

				CorpusEventStream parseEventStream = parserService.getParseEventStream(corpusReader, parseFeatures);
				
				MaxentModelTrainer trainer = new MaxentModelTrainer(parseEventStream);
				trainer.setIterations(iterations);
				trainer.setCutoff(cutoff);
				
				JolicielMaxentModel<Transition> jolicielMaxentModel = new JolicielMaxentModel<Transition>(trainer, descriptors, transitionSystem);
				jolicielMaxentModel.persist(parserModelFile);
			} else if (command.equals("evaluate")) {
				if (outDirPath.length()==0)
					throw new RuntimeException("Missing argument: outdir");

				File outDir = new File(outDirPath);
				outDir.mkdirs();

				File parseModelFile = new File(parserModelFilePath);
				JolicielMaxentModel<Transition> jolicielMaxentModel = new JolicielMaxentModel<Transition>(parseModelFile);
				
				Writer csvFileWriter = null;
				if (includeSentences) {
					File csvFile = new File(outDir, modelName + "_sentences.csv");
					csvFile.delete();
					csvFile.createNewFile();
					csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false),"UTF8"));
				}


				NonDeterministicParser parser = parserService.getTransitionBasedParser(jolicielMaxentModel, beamWidth);
				
				ParserEvaluator evaluator = parserService.getParserEvaluator();
				evaluator.setParser(parser);
				evaluator.setLabeledEvaluation(true);
				evaluator.setCsvFileWriter(csvFileWriter);
				
				corpusReader.setTransitionSystem(parser.getTransitionSystem());

				FScoreCalculator<String> fscoreCalculator = evaluator.evaluate(corpusReader);
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
}
