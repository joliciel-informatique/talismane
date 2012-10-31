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
package com.joliciel.talismane.sentenceDetector;

import com.joliciel.talismane.TalismaneServiceLocator;

public class SentenceDetectorServiceLocator {
	SentenceDetectorServiceImpl sentenceDetectorService = null;
	private TalismaneServiceLocator talismaneServiceLocator;
	
	public SentenceDetectorServiceLocator(TalismaneServiceLocator talismaneServiceLocator) {
		this.talismaneServiceLocator = talismaneServiceLocator;
	}
	
	public SentenceDetectorService getSentenceDetectorService() {
		if (sentenceDetectorService==null) {
			sentenceDetectorService = new SentenceDetectorServiceImpl();
			sentenceDetectorService.setSentenceDetectorFeatureService(this.talismaneServiceLocator.getSentenceDetectorFeatureServiceLocator().getSentenceDetectorFeatureService());
			sentenceDetectorService.setTokeniserService(talismaneServiceLocator.getTokeniserServiceLocator().getTokeniserService());
			sentenceDetectorService.setMachineLearningService(talismaneServiceLocator.getMachineLearningServiceLocator().getMachineLearningService());
			sentenceDetectorService.setFilterService(talismaneServiceLocator.getFilterServiceLocator().getFilterService());
		}
		return sentenceDetectorService;
	}

	public TalismaneServiceLocator getTalismaneServiceLocator() {
		return talismaneServiceLocator;
	}
}
