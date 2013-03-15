///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
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
package com.joliciel.talismane.parser;

import com.joliciel.talismane.TalismaneServiceLocator;

public class ParserServiceLocator {
	ParserServiceImpl parserService = null;
	private TalismaneServiceLocator talismaneServiceLocator;
	
	public ParserServiceLocator(TalismaneServiceLocator talismaneServiceLocator) {
		this.talismaneServiceLocator = talismaneServiceLocator;
	}
	
	public ParserService getParserService() {
		if (parserService==null) {
			parserService = new ParserServiceImpl();
			parserService.setParseFeatureService(this.talismaneServiceLocator.getParserFeatureServiceLocator().getParserFeatureService());
			parserService.setPosTaggerService(this.talismaneServiceLocator.getPosTaggerServiceLocator().getPosTaggerService());
			parserService.setTokeniserService(this.talismaneServiceLocator.getTokeniserServiceLocator().getTokeniserService());
			parserService.setMachineLearningService(this.talismaneServiceLocator.getMachineLearningServiceLocator().getMachineLearningService());
			parserService.setTokenFilterService(this.talismaneServiceLocator.getTokenFilterServiceLocator().getTokenFilterService());
			parserService.setFeatureService(this.talismaneServiceLocator.getFeatureServiceLocator().getFeatureService());
		}
		return parserService;
	}

	public TalismaneServiceLocator getTalismaneServiceLocator() {
		return talismaneServiceLocator;
	}
}
