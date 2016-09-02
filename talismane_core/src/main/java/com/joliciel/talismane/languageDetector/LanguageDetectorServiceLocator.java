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
package com.joliciel.talismane.languageDetector;

import com.joliciel.talismane.TalismaneServiceLocator;

public class LanguageDetectorServiceLocator {
	LanguageDetectorServiceImpl languageDetectorService = null;
	private TalismaneServiceLocator talismaneServiceLocator;

	public LanguageDetectorServiceLocator(TalismaneServiceLocator talismaneServiceLocator) {
		this.talismaneServiceLocator = talismaneServiceLocator;
	}

	public synchronized LanguageDetectorService getLanguageDetectorService() {
		if (languageDetectorService == null) {
			languageDetectorService = new LanguageDetectorServiceImpl();
			languageDetectorService.setTokeniserService(talismaneServiceLocator.getTokeniserServiceLocator().getTokeniserService());
			languageDetectorService.setMachineLearningService(talismaneServiceLocator.getMachineLearningServiceLocator().getMachineLearningService());
			languageDetectorService.setFeatureService(talismaneServiceLocator.getFeatureServiceLocator().getFeatureService());
		}
		return languageDetectorService;
	}

	public TalismaneServiceLocator getTalismaneServiceLocator() {
		return talismaneServiceLocator;
	}
}
