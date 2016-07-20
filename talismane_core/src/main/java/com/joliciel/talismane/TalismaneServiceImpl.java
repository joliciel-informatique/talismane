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
package com.joliciel.talismane;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.joliciel.talismane.filters.FilterService;
import com.joliciel.talismane.languageDetector.LanguageDetectorService;
import com.joliciel.talismane.lexicon.LexiconService;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.parser.ParserService;
import com.joliciel.talismane.parser.features.ParserFeatureService;
import com.joliciel.talismane.posTagger.PosTaggerService;
import com.joliciel.talismane.posTagger.features.PosTaggerFeatureService;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorService;
import com.joliciel.talismane.sentenceDetector.features.SentenceDetectorFeatureService;
import com.joliciel.talismane.tokeniser.TokeniserService;
import com.joliciel.talismane.tokeniser.features.TokenFeatureService;
import com.joliciel.talismane.tokeniser.filters.TokenFilterService;
import com.joliciel.talismane.tokeniser.patterns.TokeniserPatternService;
import com.typesafe.config.Config;

class TalismaneServiceImpl implements TalismaneServiceInternal {
	private TokeniserService tokeniserService;
	private PosTaggerService posTaggerService;
	private ParserService parserService;
	private FilterService filterService;
	private MachineLearningService machineLearningService;
	private SentenceDetectorService sentenceDetectorService;
	private ParserFeatureService parserFeatureService;
	private PosTaggerFeatureService posTaggerFeatureService;
	private SentenceDetectorFeatureService sentenceDetectorFeatureService;
	private TokenFeatureService tokenFeatureService;
	private TokenFilterService tokenFilterService;
	private TokeniserPatternService tokeniserPatternService;
	private LanguageDetectorService languageDetectorService;
	private LexiconService lexiconService;

	private TalismaneSession talismaneSession;

	@Override
	public Talismane getTalismane(TalismaneConfig config) {
		TalismaneImpl talismane = new TalismaneImpl(config);
		talismane.setTalismaneService(this);
		talismane.setFilterService(this.getFilterService());
		talismane.setParserService(this.getParserService());
		talismane.setPosTaggerService(this.getPosTaggerService());
		talismane.setTokeniserService(this.getTokeniserService());
		talismane.setMachineLearningService(this.getMachineLearningService());
		talismane.setSentenceDetectorService(this.getSentenceDetectorService());
		talismane.setLanguageDetectorService(this.getLanguageDetectorService());
		return talismane;
	}

	@Override
	public TalismaneServer getTalismaneServer(TalismaneConfig config) {
		TalismaneServerImpl talismaneServer = new TalismaneServerImpl(config);
		talismaneServer.setTalismaneService(this);
		return talismaneServer;
	}

	public TokeniserService getTokeniserService() {
		return tokeniserService;
	}

	public void setTokeniserService(TokeniserService tokeniserService) {
		this.tokeniserService = tokeniserService;
	}

	public PosTaggerService getPosTaggerService() {
		return posTaggerService;
	}

	public void setPosTaggerService(PosTaggerService posTaggerService) {
		this.posTaggerService = posTaggerService;
	}

	public ParserService getParserService() {
		return parserService;
	}

	public void setParserService(ParserService parserService) {
		this.parserService = parserService;
	}

	public FilterService getFilterService() {
		return filterService;
	}

	public void setFilterService(FilterService filterService) {
		this.filterService = filterService;
	}

	public MachineLearningService getMachineLearningService() {
		return machineLearningService;
	}

	public void setMachineLearningService(MachineLearningService machineLearningService) {
		this.machineLearningService = machineLearningService;
	}

	public SentenceDetectorService getSentenceDetectorService() {
		return sentenceDetectorService;
	}

	public void setSentenceDetectorService(SentenceDetectorService sentenceDetectorService) {
		this.sentenceDetectorService = sentenceDetectorService;
	}

	@Override
	public TalismaneSession getTalismaneSession() {
		if (talismaneSession == null) {
			TalismaneSessionImpl talismaneSession = new TalismaneSessionImpl();
			talismaneSession.setLexiconService(lexiconService);
			this.talismaneSession = talismaneSession;
		}
		return talismaneSession;
	}

	@Override
	public TalismaneConfig getTalismaneConfig(Map<String, String> args, LanguageImplementation implementation) {
		return this.getTalismaneConfig(args, null, implementation);
	}

	@Override
	public TalismaneConfig getTalismaneConfig(LanguageImplementation implementation) {
		Map<String, String> args = new HashMap<String, String>();
		return this.getTalismaneConfig(args, null, implementation);
	}

	@Override
	public TalismaneConfig getTalismaneConfig(Map<String, String> args, File baseDir, LanguageImplementation implementation) {
		return this.getTalismaneConfig(null, args, baseDir, implementation, null);
	}

	@Override
	public TalismaneConfig getTalismaneConfig(String sessionId) {
		Map<String, String> args = new HashMap<String, String>();
		return this.getTalismaneConfig(args, null, sessionId);
	}

	@Override
	public TalismaneConfig getTalismaneConfig(Map<String, String> args, String sessionId) {
		return this.getTalismaneConfig(args, null, sessionId);
	}

	@Override
	public TalismaneConfig getTalismaneConfig(Map<String, String> args, File baseDir, String sessionId) {
		return this.getTalismaneConfig(null, args, baseDir, null, sessionId);
	}

	@Override
	public TalismaneConfig getTalismaneConfig(Config config, LanguageImplementation implementation) {
		return this.getTalismaneConfig(config, null, implementation);
	}

	@Override
	public TalismaneConfig getTalismaneConfig(Config config, String sessionId) {
		return this.getTalismaneConfig(config, null, sessionId);
	}

	@Override
	public TalismaneConfig getTalismaneConfig(Config config, File baseDir, LanguageImplementation implementation) {
		return this.getTalismaneConfig(config, null, baseDir, implementation, null);
	}

	@Override
	public TalismaneConfig getTalismaneConfig(Config config, File baseDir, String sessionId) {
		return this.getTalismaneConfig(config, null, baseDir, null, sessionId);
	}

	private TalismaneConfig getTalismaneConfig(Config myConfig, Map<String, String> args, File baseDir, LanguageImplementation implementation,
			String sessionId) {
		TalismaneConfigImpl config = null;
		if (implementation != null)
			config = new TalismaneConfigImpl(implementation);
		else
			config = new TalismaneConfigImpl(sessionId);

		config.setTalismaneService(this);
		config.setFilterService(filterService);
		config.setMachineLearningService(machineLearningService);
		config.setParserFeatureService(parserFeatureService);
		config.setParserService(parserService);
		config.setPosTaggerFeatureService(posTaggerFeatureService);
		config.setPosTaggerService(posTaggerService);
		config.setSentenceDetectorFeatureService(sentenceDetectorFeatureService);
		config.setSentenceDetectorService(sentenceDetectorService);
		config.setTokenFeatureService(tokenFeatureService);
		config.setTokenFilterService(tokenFilterService);
		config.setTokeniserPatternService(tokeniserPatternService);
		config.setTokeniserService(tokeniserService);
		config.setLanguageDetectorService(languageDetectorService);
		config.setBaseDir(baseDir);

		if (args != null)
			config.loadParameters(args);
		else
			config.loadParameters(myConfig);
		return config;
	}

	public ParserFeatureService getParserFeatureService() {
		return parserFeatureService;
	}

	public void setParserFeatureService(ParserFeatureService parserFeatureService) {
		this.parserFeatureService = parserFeatureService;
	}

	public PosTaggerFeatureService getPosTaggerFeatureService() {
		return posTaggerFeatureService;
	}

	public void setPosTaggerFeatureService(PosTaggerFeatureService posTaggerFeatureService) {
		this.posTaggerFeatureService = posTaggerFeatureService;
	}

	public SentenceDetectorFeatureService getSentenceDetectorFeatureService() {
		return sentenceDetectorFeatureService;
	}

	public void setSentenceDetectorFeatureService(SentenceDetectorFeatureService sentenceDetectorFeatureService) {
		this.sentenceDetectorFeatureService = sentenceDetectorFeatureService;
	}

	public TokenFeatureService getTokenFeatureService() {
		return tokenFeatureService;
	}

	public void setTokenFeatureService(TokenFeatureService tokenFeatureService) {
		this.tokenFeatureService = tokenFeatureService;
	}

	public TokenFilterService getTokenFilterService() {
		return tokenFilterService;
	}

	public void setTokenFilterService(TokenFilterService tokenFilterService) {
		this.tokenFilterService = tokenFilterService;
	}

	public TokeniserPatternService getTokeniserPatternService() {
		return tokeniserPatternService;
	}

	public void setTokeniserPatternService(TokeniserPatternService tokeniserPatternService) {
		this.tokeniserPatternService = tokeniserPatternService;
	}

	public LanguageDetectorService getLanguageDetectorService() {
		return languageDetectorService;
	}

	public void setLanguageDetectorService(LanguageDetectorService languageDetectorService) {
		this.languageDetectorService = languageDetectorService;
	}

	public LexiconService getLexiconService() {
		return lexiconService;
	}

	public void setLexiconService(LexiconService lexiconService) {
		this.lexiconService = lexiconService;
	}

}
