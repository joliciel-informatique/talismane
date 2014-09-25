///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2011 Assaf Urieli
//
//This file is part of Talismane.
//
//Talismane is free software: you can redistribute it and/or modify
//it under the terms of the GNU Affero General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//Talismane is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Affero General Public License for more details.
//
//You should have received a copy of the GNU Affero General Public License
//along with Talismane.  If not, see <http://www.gnu.org/licenses/>.
//////////////////////////////////////////////////////////////////////////////
package com.joliciel.talismane.en;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.LanguageSpecificImplementation;
import com.joliciel.talismane.LinguisticRules;
import com.joliciel.talismane.NeedsTalismaneSession;
import com.joliciel.talismane.Talismane;
import com.joliciel.talismane.TalismaneConfig;
import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneService;
import com.joliciel.talismane.TalismaneServiceLocator;
import com.joliciel.talismane.Talismane.Command;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.en.tokeniser.filters.AllUppercaseEnglishFilter;
import com.joliciel.talismane.en.tokeniser.filters.LowercaseFirstWordEnglishFilter;
import com.joliciel.talismane.en.tokeniser.filters.UpperCaseSeriesEnglishFilter;
import com.joliciel.talismane.extensions.Extensions;
import com.joliciel.talismane.lexicon.DefaultPosTagMapper;
import com.joliciel.talismane.lexicon.LexiconDeserializer;
import com.joliciel.talismane.lexicon.PosTagMapper;
import com.joliciel.talismane.lexicon.PosTaggerLexicon;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.MachineLearningModel;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.parser.ParserRegexBasedCorpusReader;
import com.joliciel.talismane.parser.ParserService;
import com.joliciel.talismane.parser.TransitionSystem;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.posTagger.PosTaggerService;
import com.joliciel.talismane.posTagger.filters.PosTagSequenceFilter;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorOutcome;
import com.joliciel.talismane.tokeniser.TokeniserOutcome;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;
import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.StringUtils;

/**
 * The default English implementation of Talismane.
 * @author Assaf Urieli
 *
 */
public class TalismaneEnglish implements LanguageSpecificImplementation {
	private static final Log LOG = LogFactory.getLog(TalismaneEnglish.class);
	private static final String DEFAULT_CONLL_REGEX = "%INDEX%\\t%TOKEN%\\t.*\\t%POSTAG%\\t.*\\t.*\\t.*\\t.*\\t%GOVERNOR%\\t%LABEL%";
	private TalismaneServiceLocator talismaneServiceLocator = null;
	private PosTaggerService posTaggerService;
	private ParserService parserService;
	private MachineLearningService machineLearningService;
	private TalismaneService talismaneService;
	private TalismaneSession talismaneSession;
	
	private List<Class<? extends TokenSequenceFilter>> availableTokenSequenceFilters;
	
	private static ClassificationModel<SentenceDetectorOutcome> sentenceModel;
	private static ClassificationModel<TokeniserOutcome> tokeniserModel;
	private static ClassificationModel<PosTag> posTaggerModel;
	private static MachineLearningModel parserModel;
	private static PosTaggerLexicon lexicon;

	private enum CorpusFormat {
		/** Penn-To-Dependency CoNLL-X format */
		pennDep,
	}
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
    	Map<String,String> argsMap = StringUtils.convertArgs(args);
    	CorpusFormat corpusReaderType = null;
    	
    	if (argsMap.containsKey("corpusReader")) {
    		corpusReaderType = CorpusFormat.valueOf(argsMap.get("corpusReader"));
    		argsMap.remove("corpusReader");
    	}
    	
    	Extensions extensions = new Extensions();
    	extensions.pluckParameters(argsMap);
    	
    	String sessionId = "";
       	TalismaneServiceLocator locator = TalismaneServiceLocator.getInstance(sessionId);
       	TalismaneService talismaneService = locator.getTalismaneService();
    	TalismaneEnglish talismaneEnglish = new TalismaneEnglish(sessionId);

    	TalismaneConfig config = talismaneService.getTalismaneConfig(argsMap, talismaneEnglish);
    	if (config.getCommand()==null)
    		return;
    	
    	if (corpusReaderType!=null) {
    		if (corpusReaderType==CorpusFormat.pennDep) {
    			PennDepReader corpusReader = new PennDepReader(config.getReader());
    			corpusReader.setParserService(config.getParserService());
    			corpusReader.setPosTaggerService(config.getPosTaggerService());
    			corpusReader.setTokeniserService(config.getTokeniserService());
    			corpusReader.setTokenFilterService(config.getTokenFilterService());
    			corpusReader.setTalismaneService(config.getTalismaneService());
    			
    			corpusReader.setPredictTransitions(config.isPredictTransitions());
    			
	  			config.setParserCorpusReader(corpusReader);
				config.setPosTagCorpusReader(corpusReader);
				config.setTokenCorpusReader(corpusReader);
				config.setSentenceCorpusReader(corpusReader);
				
				corpusReader.setRegex(DEFAULT_CONLL_REGEX);
 				
				if (config.getInputRegex()!=null) {
					corpusReader.setRegex(config.getInputRegex());
				}
				
				if (config.getCommand().equals(Command.compare)) {
					File evaluationFile = new File(config.getEvaluationFilePath());
	    			ParserRegexBasedCorpusReader evaluationReader = config.getParserService().getRegexBasedCorpusReader(evaluationFile, config.getInputCharset());
		  			config.setParserEvaluationCorpusReader(evaluationReader);
					config.setPosTagEvaluationCorpusReader(evaluationReader);
					
					evaluationReader.setRegex(DEFAULT_CONLL_REGEX);
	 				
					if (config.getInputRegex()!=null) {
						evaluationReader.setRegex(config.getInputRegex());
					}
				}
	  		} else {
	  			throw new TalismaneException("Unknown corpusReader: " + corpusReaderType);
	  		}
    	}
    	Talismane talismane = config.getTalismane();
    	
    	extensions.prepareCommand(config, talismane);
    	
    	talismane.process();
	}

	public TalismaneEnglish(String sessionId) {
		talismaneServiceLocator = TalismaneServiceLocator.getInstance(sessionId);
	}
	
	private static ZipInputStream getZipInputStreamFromResource(String resource) {
		InputStream inputStream = getInputStreamFromResource(resource);
		ZipInputStream zis = new ZipInputStream(inputStream);
		
		return zis;
	}

	private static InputStream getInputStreamFromResource(String resource) {
		String path = "/com/joliciel/talismane/en/resources/" + resource;
		LOG.debug("Getting " + path);
		InputStream inputStream = TalismaneEnglish.class.getResourceAsStream(path); 
		
		return inputStream;
	}

	@Override
	public Scanner getDefaultPosTagSetScanner() {
		InputStream posTagInputStream = getInputStreamFromResource("pennTagset.txt");
		return new Scanner(posTagInputStream, "UTF-8");
	}
	

	@Override
	public Scanner getDefaultPosTaggerRulesScanner() {
		return null;
	}
	

	@Override
	public Scanner getDefaultParserRulesScanner() {
		return null;
	}


	@Override
	public PosTaggerLexicon getDefaultLexicon() {
		try {
			if (lexicon==null) {
				PosTagSet posTagSet = this.getDefaultPosTagSet();
				this.getTalismaneSession().setPosTagSet(posTagSet);
				
				InputStream posTagMapStream = this.getDefaultPosTagMapFromStream();
				Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(posTagMapStream, "UTF-8")));
				
				PosTagMapper posTagMapper = new DefaultPosTagMapper(scanner, posTagSet);
				
				LexiconDeserializer deserializer = new LexiconDeserializer(this.getTalismaneSession());
				
				ZipInputStream zis = getZipInputStreamFromResource("dela-en.zip");
	
				lexicon = deserializer.deserializeLexiconFile(zis);
				lexicon.setPosTagSet(posTagSet);
				lexicon.setPosTagMapper(posTagMapper);
			}
			return lexicon;
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}
	}

	@Override
	public ClassificationModel<SentenceDetectorOutcome> getDefaultSentenceModel() {
		if (sentenceModel==null) {
			String sentenceModelName = "sentence_penn_train_baseline_maxent_cut5.zip";
			ZipInputStream zis = TalismaneEnglish.getZipInputStreamFromResource(sentenceModelName);
			sentenceModel = this.getMachineLearningService().getClassificationModel(zis);
		}
		return sentenceModel;
	}

	@Override
	public ClassificationModel<TokeniserOutcome> getDefaultTokeniserModel() {
		if (tokeniserModel==null) {
			String tokeniserModelName = "tokeniser_penn_train_baseline_maxent_cut5.zip";
			ZipInputStream zis = TalismaneEnglish.getZipInputStreamFromResource(tokeniserModelName);
			tokeniserModel = this.getMachineLearningService().getClassificationModel(zis);
		}
		return tokeniserModel;
	}

	@Override
	public ClassificationModel<PosTag> getDefaultPosTaggerModel() {
		if (posTaggerModel==null) {
			String posTaggerModelName = "posTagger_penn_train_baseline_maxent_cut5.zip";
			ZipInputStream zis = TalismaneEnglish.getZipInputStreamFromResource(posTaggerModelName);
			posTaggerModel = this.getMachineLearningService().getClassificationModel(zis);
		}
		return posTaggerModel;
	}

	@Override
	public MachineLearningModel getDefaultParserModel() {
		if (parserModel==null) {
			String parserModelName = "parser_penn_train_baseline_maxent_cut10.zip";
			ZipInputStream zis = TalismaneEnglish.getZipInputStreamFromResource(parserModelName);
			parserModel = this.getMachineLearningService().getMachineLearningModel(zis);
		}
		return parserModel;
	}

	@Override
	public List<TokenSequenceFilter> getDefaultTokenSequenceFilters() {
		List<TokenSequenceFilter> tokenSequenceFilters = new ArrayList<TokenSequenceFilter>();
		tokenSequenceFilters.add(new LowercaseFirstWordEnglishFilter());
		tokenSequenceFilters.add(new UpperCaseSeriesEnglishFilter());
		
		for (TokenSequenceFilter filter : tokenSequenceFilters) {
			if (filter instanceof NeedsTalismaneSession) {
				((NeedsTalismaneSession) filter).setTalismaneSession(this.getTalismaneSession());
			}
		}
		
		return tokenSequenceFilters;
	}

	@Override
	public Scanner getDefaultTextMarkerFiltersScanner() {
		InputStream inputStream = getInputStreamFromResource("text_marker_filters.txt");
		return new Scanner(inputStream, "UTF-8");
	}

	@Override
	public Scanner getDefaultTokenFiltersScanner() {
		InputStream inputStream = getInputStreamFromResource("token_filters.txt");
		return new Scanner(inputStream, "UTF-8");
	}
	
	public InputStream getDefaultPosTagMapFromStream() {
		InputStream inputStream = getInputStreamFromResource("delaPennPosTagMap.txt");
		return inputStream;
	}

	@Override
	public TransitionSystem getDefaultTransitionSystem() {
		TransitionSystem transitionSystem = this.getParserService().getArcEagerTransitionSystem();
		InputStream inputStream = getInputStreamFromResource("pennDependencyLabels.txt");
		Scanner scanner = new Scanner(inputStream, "UTF-8");
		List<String> dependencyLabels = new ArrayList<String>();
		while (scanner.hasNextLine()) {
			String dependencyLabel = scanner.nextLine();
			if (!dependencyLabel.startsWith("#")) {
				if (dependencyLabel.indexOf('\t')>0)
					dependencyLabel = dependencyLabel.substring(0, dependencyLabel.indexOf('\t'));
				dependencyLabels.add(dependencyLabel);
			}
		}
		transitionSystem.setDependencyLabels(dependencyLabels);
		return transitionSystem;
	}

	@Override
	public List<PosTagSequenceFilter> getDefaultPosTagSequenceFilters() {
		List<PosTagSequenceFilter> filters = new ArrayList<PosTagSequenceFilter>();
		return filters;
	}


	@Override
	public PosTagSet getDefaultPosTagSet() {
		Scanner posTagSetScanner = this.getDefaultPosTagSetScanner();
		PosTagSet posTagSet = this.getPosTaggerService().getPosTagSet(posTagSetScanner);
		return posTagSet;
	}
	

	public PosTaggerService getPosTaggerService() {
		if (posTaggerService==null) {
			posTaggerService = talismaneServiceLocator.getPosTaggerServiceLocator().getPosTaggerService();
		}
		return posTaggerService;
	}

	public void setPosTaggerService(PosTaggerService posTaggerService) {
		this.posTaggerService = posTaggerService;
	}

	public ParserService getParserService() {
		if (parserService==null) {
			parserService = talismaneServiceLocator.getParserServiceLocator().getParserService();
		}
		return parserService;
	}

	public void setParserService(ParserService parserService) {
		this.parserService = parserService;
	}

	@Override
	public List<Class<? extends TokenSequenceFilter>> getAvailableTokenSequenceFilters() {
		if (availableTokenSequenceFilters==null) {
			availableTokenSequenceFilters = new ArrayList<Class<? extends TokenSequenceFilter>>();
			availableTokenSequenceFilters.add(LowercaseFirstWordEnglishFilter.class);
			availableTokenSequenceFilters.add(UpperCaseSeriesEnglishFilter.class);
			availableTokenSequenceFilters.add(AllUppercaseEnglishFilter.class);
		}

		return availableTokenSequenceFilters;
	}

	public TalismaneService getTalismaneService() {
		if (talismaneService==null) {
			this.setTalismaneService(talismaneServiceLocator.getTalismaneService());
		}
		return talismaneService;
	}

	public void setTalismaneService(TalismaneService talismaneService) {
		this.talismaneService = talismaneService;
		this.talismaneSession = talismaneService.getTalismaneSession();
	}

	@Override
	public LinguisticRules getDefaultLinguisticRules() {
		return new EnglishRules();
	}

	public TalismaneSession getTalismaneSession() {
		if (talismaneSession==null) {
			talismaneSession = this.getTalismaneService().getTalismaneSession();
		}
		return talismaneSession;
	}
	
	public MachineLearningService getMachineLearningService() {
		if (machineLearningService==null) {
			machineLearningService = talismaneServiceLocator.getMachineLearningServiceLocator().getMachineLearningService();
		}
		return machineLearningService;
	}

	public void setMachineLearningService(
			MachineLearningService machineLearningService) {
		this.machineLearningService = machineLearningService;
	}
}
