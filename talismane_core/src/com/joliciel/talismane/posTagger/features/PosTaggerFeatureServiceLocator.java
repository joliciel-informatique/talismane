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
package com.joliciel.talismane.posTagger.features;

import com.joliciel.talismane.TalismaneServiceLocator;

public class PosTaggerFeatureServiceLocator {
	PosTaggerFeatureServiceImpl posTaggerFeatureService = null;
	private TalismaneServiceLocator talismaneServiceLocator;
	
	public PosTaggerFeatureServiceLocator(TalismaneServiceLocator talismaneServiceLocator) {
		this.talismaneServiceLocator = talismaneServiceLocator;
	}
	
	public PosTaggerFeatureService getPosTaggerFeatureService() {
		if (posTaggerFeatureService==null) {
			posTaggerFeatureService = new PosTaggerFeatureServiceImpl();
			posTaggerFeatureService.setFeatureService(this.talismaneServiceLocator.getFeatureServiceLocator().getFeatureService());
			posTaggerFeatureService.setTokenFeatureService(this.talismaneServiceLocator.getTokenFeatureServiceLocator().getTokenFeatureService());
			posTaggerFeatureService.setMachineLearningService(this.talismaneServiceLocator.getMachineLearningServiceLocator().getMachineLearningService());
		}
		return posTaggerFeatureService;
	}

	public TalismaneServiceLocator getTalismaneServiceLocator() {
		return talismaneServiceLocator;
	}
}
