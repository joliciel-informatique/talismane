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

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.languageDetector.LanguageDetectorServiceLocator;
import com.joliciel.talismane.machineLearning.features.FeatureServiceLocator;
import com.joliciel.talismane.parser.ParserServiceLocator;
import com.joliciel.talismane.parser.features.ParserFeatureServiceLocator;
import com.joliciel.talismane.posTagger.PosTaggerServiceLocator;
import com.joliciel.talismane.posTagger.features.PosTaggerFeatureServiceLocator;
import com.joliciel.talismane.posTagger.filters.PosTagFilterServiceLocator;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorServiceLocator;
import com.joliciel.talismane.sentenceDetector.features.SentenceDetectorFeatureServiceLocator;
import com.joliciel.talismane.tokeniser.TokeniserServiceLocator;
import com.joliciel.talismane.tokeniser.features.TokeniserFeatureServiceLocator;
import com.joliciel.talismane.tokeniser.filters.TokenFilterServiceLocator;
import com.joliciel.talismane.tokeniser.patterns.TokeniserPatternServiceLocator;

/**
 * Top-level locator for implementations of Talismane interfaces.
 * 
 * @author Assaf Urieli
 *
 */
public class TalismaneServiceLocator {
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(TalismaneServiceLocator.class);
	private TalismaneServiceImpl talismaneService;

	private PosTaggerFeatureServiceLocator posTaggerFeatureServiceLocator;
	private PosTaggerServiceLocator posTaggerServiceLocator;
	private PosTagFilterServiceLocator posTagFilterServiceLocator;
	private TokeniserServiceLocator tokeniserServiceLocator;
	private TokeniserFeatureServiceLocator tokeniserFeatureServiceLocator;
	private TokenFilterServiceLocator tokenFilterServiceLocator;
	private TokeniserPatternServiceLocator tokeniserPatternServiceLocator;
	private SentenceDetectorServiceLocator sentenceDetectorServiceLocator;
	private SentenceDetectorFeatureServiceLocator sentenceDetectorFeatureServiceLocator;
	private ParserServiceLocator parserServiceLocator;
	private ParserFeatureServiceLocator parserFeatureServiceLocator;
	private FeatureServiceLocator featureServiceLocator;
	private LanguageDetectorServiceLocator languageDetectorServiceLocator;

	private static Map<String, TalismaneServiceLocator> instances = new HashMap<String, TalismaneServiceLocator>();
	private String sessionId;

	private TalismaneServiceLocator(String sessionId) {
		this.sessionId = sessionId;
	}

	public synchronized static TalismaneServiceLocator getInstance(String sessionId) {
		TalismaneServiceLocator instance = instances.get(sessionId);
		if (instance == null) {
			instance = new TalismaneServiceLocator(sessionId);
			instances.put(sessionId, instance);
		}
		return instance;
	}

	public synchronized static void purgeInstance(String sessionId) {
		instances.remove(sessionId);
	}

	TalismaneServiceInternal getTalismaneServiceInternal() {
		if (this.talismaneService == null) {
			this.talismaneService = new TalismaneServiceImpl();
			talismaneService.setParserService(this.getParserServiceLocator().getParserService());
			talismaneService.setPosTaggerService(this.getPosTaggerServiceLocator().getPosTaggerService());
			talismaneService.setTokeniserService(this.getTokeniserServiceLocator().getTokeniserService());
			talismaneService.setSentenceDetectorService(this.getSentenceDetectorServiceLocator().getSentenceDetectorService());
			talismaneService.setParserFeatureService(this.getParserFeatureServiceLocator().getParserFeatureService());
			talismaneService.setPosTaggerFeatureService(this.getPosTaggerFeatureServiceLocator().getPosTaggerFeatureService());
			talismaneService.setSentenceDetectorFeatureService(this.getSentenceDetectorFeatureServiceLocator().getSentenceDetectorFeatureService());
			talismaneService.setTokenFeatureService(this.getTokenFeatureServiceLocator().getTokenFeatureService());
			talismaneService.setTokenFilterService(this.getTokenFilterServiceLocator().getTokenFilterService());
			talismaneService.setTokeniserPatternService(this.getTokenPatternServiceLocator().getTokeniserPatternService());
			talismaneService.setLanguageDetectorService(this.getLanguageDetectorServiceLocator().getLanguageDetectorService());
		}
		return this.talismaneService;
	}

	public synchronized TalismaneService getTalismaneService() {
		return this.getTalismaneServiceInternal();
	}

	public synchronized PosTaggerFeatureServiceLocator getPosTaggerFeatureServiceLocator() {
		if (this.posTaggerFeatureServiceLocator == null) {
			this.posTaggerFeatureServiceLocator = new PosTaggerFeatureServiceLocator(this);
		}
		return posTaggerFeatureServiceLocator;
	}

	public synchronized PosTaggerServiceLocator getPosTaggerServiceLocator() {
		if (this.posTaggerServiceLocator == null) {
			this.posTaggerServiceLocator = new PosTaggerServiceLocator(this);
		}
		return posTaggerServiceLocator;
	}

	public synchronized PosTagFilterServiceLocator getPosTagFilterServiceLocator() {
		if (this.posTagFilterServiceLocator == null) {
			this.posTagFilterServiceLocator = new PosTagFilterServiceLocator(this);
		}
		return posTagFilterServiceLocator;
	}

	public synchronized TokeniserServiceLocator getTokeniserServiceLocator() {
		if (this.tokeniserServiceLocator == null) {
			this.tokeniserServiceLocator = new TokeniserServiceLocator(this);
		}
		return tokeniserServiceLocator;
	}

	public synchronized SentenceDetectorServiceLocator getSentenceDetectorServiceLocator() {
		if (this.sentenceDetectorServiceLocator == null) {
			this.sentenceDetectorServiceLocator = new SentenceDetectorServiceLocator(this);
		}
		return sentenceDetectorServiceLocator;
	}

	public synchronized LanguageDetectorServiceLocator getLanguageDetectorServiceLocator() {
		if (this.languageDetectorServiceLocator == null) {
			this.languageDetectorServiceLocator = new LanguageDetectorServiceLocator(this);
		}
		return languageDetectorServiceLocator;
	}

	public synchronized SentenceDetectorFeatureServiceLocator getSentenceDetectorFeatureServiceLocator() {
		if (this.sentenceDetectorFeatureServiceLocator == null) {
			this.sentenceDetectorFeatureServiceLocator = new SentenceDetectorFeatureServiceLocator(this);
		}
		return sentenceDetectorFeatureServiceLocator;
	}

	public synchronized ParserServiceLocator getParserServiceLocator() {
		if (this.parserServiceLocator == null) {
			this.parserServiceLocator = new ParserServiceLocator(this);
		}
		return parserServiceLocator;
	}

	public synchronized ParserFeatureServiceLocator getParserFeatureServiceLocator() {
		if (this.parserFeatureServiceLocator == null) {
			this.parserFeatureServiceLocator = new ParserFeatureServiceLocator(this);
		}
		return parserFeatureServiceLocator;
	}

	public synchronized FeatureServiceLocator getFeatureServiceLocator() {
		if (this.featureServiceLocator == null) {
			this.featureServiceLocator = FeatureServiceLocator.getInstance();
		}
		return featureServiceLocator;
	}

	public synchronized TokeniserFeatureServiceLocator getTokenFeatureServiceLocator() {
		if (this.tokeniserFeatureServiceLocator == null) {
			this.tokeniserFeatureServiceLocator = new TokeniserFeatureServiceLocator(this);
		}
		return tokeniserFeatureServiceLocator;

	}

	public synchronized TokenFilterServiceLocator getTokenFilterServiceLocator() {
		if (this.tokenFilterServiceLocator == null) {
			this.tokenFilterServiceLocator = new TokenFilterServiceLocator(this);
		}
		return tokenFilterServiceLocator;
	}

	public synchronized TokeniserPatternServiceLocator getTokenPatternServiceLocator() {
		if (this.tokeniserPatternServiceLocator == null) {
			this.tokeniserPatternServiceLocator = new TokeniserPatternServiceLocator(this);
		}
		return tokeniserPatternServiceLocator;

	}

	public String getSessionId() {
		return sessionId;
	}
}
