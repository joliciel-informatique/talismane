///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2014 Joliciel Informatique
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
package com.joliciel.talismane.lang;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.LanguageSpecificImplementation;
import com.joliciel.talismane.LinguisticRules;
import com.joliciel.talismane.Talismane;
import com.joliciel.talismane.TalismaneConfig;
import com.joliciel.talismane.TalismaneService;
import com.joliciel.talismane.TalismaneServiceLocator;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.extensions.Extensions;
import com.joliciel.talismane.lexicon.EmptyLexicon;
import com.joliciel.talismane.lexicon.PosTaggerLexicon;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.MachineLearningModel;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.parser.ParserService;
import com.joliciel.talismane.parser.TransitionSystem;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.posTagger.PosTaggerService;
import com.joliciel.talismane.posTagger.filters.PosTagSequenceFilter;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorOutcome;
import com.joliciel.talismane.tokeniser.TokeniserOutcome;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;
import com.joliciel.talismane.utils.StringUtils;

/**
 * Generic implementation of Talismane for all languages without a language-specific default implementation.
 * This means of course that all linguisitc resources have to be indicated via the command line.
 * @author Assaf Urieli
 *
 */
public class TalismaneGeneric implements LanguageSpecificImplementation {
	private static final Log LOG = LogFactory.getLog(TalismaneGeneric.class);
	private TalismaneServiceLocator talismaneServiceLocator = null;
	private PosTaggerService posTaggerService;
	private ParserService parserService;
	private MachineLearningService machineLearningService;
	
	private List<Class<? extends TokenSequenceFilter>> availableTokenSequenceFilters;
	private TalismaneSession talismaneSession;

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
    	Map<String,String> argsMap = StringUtils.convertArgs(args);
    	
    	Extensions extensions = new Extensions();
    	extensions.pluckParameters(argsMap);
    	
    	String sessionId = "";
       	TalismaneServiceLocator locator = TalismaneServiceLocator.getInstance(sessionId);
       	TalismaneService talismaneService = locator.getTalismaneService();
    	TalismaneGeneric talismaneGeneric = new TalismaneGeneric(sessionId);
    	TalismaneConfig config = talismaneService.getTalismaneConfig(argsMap, talismaneGeneric);
    	if (config.getCommand()==null)
    		return;
     	
    	Talismane talismane = config.getTalismane();
    	
    	extensions.prepareCommand(config, talismane);
    	
    	talismane.process();
	}

	public TalismaneGeneric(String sessionId) {
		talismaneServiceLocator = TalismaneServiceLocator.getInstance(sessionId);
		talismaneSession = talismaneServiceLocator.getTalismaneService().getTalismaneSession();
	}
	
	@SuppressWarnings("unused")
	private static ZipInputStream getZipInputStreamFromResource(String resource) {
		InputStream inputStream = getInputStreamFromResource(resource);
		ZipInputStream zis = new ZipInputStream(inputStream);
		
		return zis;
	}

	private static InputStream getInputStreamFromResource(String resource) {
		String path = "/com/joliciel/talismane/lang/resources/" + resource;
		LOG.debug("Getting " + path);
		InputStream inputStream = TalismaneGeneric.class.getResourceAsStream(path); 
		
		return inputStream;
	}

	@Override
	public Scanner getDefaultPosTagSetScanner() {
		return null;
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
	public List<PosTaggerLexicon> getDefaultLexicons() {
		List<PosTaggerLexicon> lexicons = new ArrayList<PosTaggerLexicon>();
		lexicons.add(new EmptyLexicon());
		return lexicons;
	}

	@Override
	public ClassificationModel<SentenceDetectorOutcome> getDefaultSentenceModel() {
		return null;
	}

	@Override
	public ClassificationModel<TokeniserOutcome> getDefaultTokeniserModel() {
		return null;
	}

	@Override
	public ClassificationModel<PosTag> getDefaultPosTaggerModel() {
		return null;
	}

	@Override
	public MachineLearningModel getDefaultParserModel() {
		return null;
	}

	@Override
	public List<TokenSequenceFilter> getDefaultTokenSequenceFilters() {
		List<TokenSequenceFilter> tokenFilters = new ArrayList<TokenSequenceFilter>();

		return tokenFilters;
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
		return null;
	}

	@Override
	public TransitionSystem getDefaultTransitionSystem() {
		return null;
	}

	@Override
	public List<PosTagSequenceFilter> getDefaultPosTagSequenceFilters() {
		List<PosTagSequenceFilter> filters = new ArrayList<PosTagSequenceFilter>();
		return filters;
	}


	@Override
	public PosTagSet getDefaultPosTagSet() {
		return null;
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
		}
		return availableTokenSequenceFilters;
	}

	@Override
	public LinguisticRules getDefaultLinguisticRules() {
		return new GenericRules();
	}

	public TalismaneSession getTalismaneSession() {
		if (talismaneSession==null) {
			talismaneSession = talismaneServiceLocator.getTalismaneService().getTalismaneSession();
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
