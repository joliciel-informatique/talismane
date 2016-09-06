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

import java.io.IOException;
import java.util.Map;

import com.joliciel.talismane.parser.ParserService;
import com.joliciel.talismane.parser.features.ParserFeatureService;
import com.joliciel.talismane.posTagger.PosTaggerService;
import com.typesafe.config.Config;

class TalismaneServiceImpl implements TalismaneServiceInternal {
	private PosTaggerService posTaggerService;
	private ParserService parserService;
	private ParserFeatureService parserFeatureService;

	private TalismaneSession talismaneSession;

	private final String sessionId;

	public TalismaneServiceImpl(String sessionId) {
		this.sessionId = sessionId;
	}

	@Override
	public Talismane getTalismane(TalismaneConfig config) {
		TalismaneImpl talismane = new TalismaneImpl(config, this.getTalismaneSession());
		talismane.setTalismaneService(this);
		talismane.setParserService(this.getParserService());
		talismane.setPosTaggerService(this.getPosTaggerService());
		return talismane;
	}

	@Override
	public TalismaneServer getTalismaneServer(TalismaneConfig config) {
		TalismaneServerImpl talismaneServer = new TalismaneServerImpl(config);
		talismaneServer.setTalismaneService(this);
		return talismaneServer;
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

	@Override
	public TalismaneSession getTalismaneSession() {
		if (talismaneSession == null) {
			TalismaneSession talismaneSession = new TalismaneSession(this.sessionId);
			this.talismaneSession = talismaneSession;
		}
		return talismaneSession;
	}

	@Override
	public TalismaneConfig getTalismaneConfig(Config config) throws ClassNotFoundException, IOException {
		return this.getTalismaneConfig(config, null);
	}

	@Override
	public TalismaneConfig getTalismaneConfig(Config myConfig, Map<String, String> args) throws ClassNotFoundException, IOException {
		TalismaneConfigImpl config = new TalismaneConfigImpl();

		config.setTalismaneService(this);
		config.setParserFeatureService(parserFeatureService);
		config.setParserService(parserService);
		config.setPosTaggerService(posTaggerService);

		if (args != null)
			config.loadParameters(args, myConfig);
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
}
