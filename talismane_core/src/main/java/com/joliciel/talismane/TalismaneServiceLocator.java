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

import com.joliciel.talismane.parser.ParserServiceLocator;
import com.joliciel.talismane.parser.features.ParserFeatureServiceLocator;
import com.joliciel.talismane.posTagger.PosTaggerServiceLocator;
import com.joliciel.talismane.posTagger.filters.PosTagFilterServiceLocator;
import com.joliciel.talismane.tokeniser.TokeniserServiceLocator;

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

	private PosTaggerServiceLocator posTaggerServiceLocator;
	private PosTagFilterServiceLocator posTagFilterServiceLocator;
	private TokeniserServiceLocator tokeniserServiceLocator;
	private ParserServiceLocator parserServiceLocator;
	private ParserFeatureServiceLocator parserFeatureServiceLocator;

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
			this.talismaneService = new TalismaneServiceImpl(this.sessionId);
			talismaneService.setParserService(this.getParserServiceLocator().getParserService());
			talismaneService.setPosTaggerService(this.getPosTaggerServiceLocator().getPosTaggerService());
			talismaneService.setTokeniserService(this.getTokeniserServiceLocator().getTokeniserService());
			talismaneService.setParserFeatureService(this.getParserFeatureServiceLocator().getParserFeatureService());
		}
		return this.talismaneService;
	}

	public synchronized TalismaneService getTalismaneService() {
		return this.getTalismaneServiceInternal();
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

	public String getSessionId() {
		return sessionId;
	}
}
