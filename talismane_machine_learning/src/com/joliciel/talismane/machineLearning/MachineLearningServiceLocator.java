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
package com.joliciel.talismane.machineLearning;

import com.joliciel.talismane.machineLearning.linearsvm.LinearSVMServiceLocator;
import com.joliciel.talismane.machineLearning.maxent.MaxentServiceLocator;
import com.joliciel.talismane.machineLearning.perceptron.PerceptronServiceLocator;

public class MachineLearningServiceLocator {
	private static MachineLearningServiceLocator instance;
	
	private MachineLearningServiceImpl machineLearningService;
	
	private MachineLearningServiceLocator() { }
	
	public synchronized static MachineLearningServiceLocator getInstance() {
		if (instance==null) {
			instance = new MachineLearningServiceLocator();
		}
		return instance;
	}
	
	public synchronized MachineLearningService getMachineLearningService() {
		if (machineLearningService == null) {
			machineLearningService = new MachineLearningServiceImpl();
			machineLearningService.setMaxentService(MaxentServiceLocator.getInstance(this).getMaxentService());
			machineLearningService.setLinearSVMService(LinearSVMServiceLocator.getInstance(this).getLinearSVMService());
			machineLearningService.setPerceptronService(PerceptronServiceLocator.getInstance(this).getPerceptronService());
		}
		return machineLearningService;
	}
}
