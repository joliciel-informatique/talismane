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
package com.joliciel.talismane.machineLearning.perceptron;

import com.joliciel.talismane.machineLearning.MachineLearningServiceLocator;

public class PerceptronServiceLocator {
	private static PerceptronServiceLocator instance;

	private MachineLearningServiceLocator machineLearningServiceLocator;

	private PerceptronServiceImpl perceptronService;
	
	private PerceptronServiceLocator(MachineLearningServiceLocator machineLearningServiceLocator) {
		this.machineLearningServiceLocator = machineLearningServiceLocator;
	}
	
	public synchronized static PerceptronServiceLocator getInstance(MachineLearningServiceLocator machineLearningServiceLocator) {
		if (instance==null) {
			instance = new PerceptronServiceLocator(machineLearningServiceLocator);
		}
		return instance;
	}
	
	public synchronized PerceptronService getPerceptronService() {
		if (perceptronService == null) {
			perceptronService = new PerceptronServiceImpl();
			perceptronService.setMachineLearningService(this.machineLearningServiceLocator.getMachineLearningService());
		}
		return perceptronService;
	}
}
