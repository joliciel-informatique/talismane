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
package com.joliciel.talismane.machineLearning.maxent;

import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.MachineLearningService;

class MaxentServiceImpl implements MaxentService {
	private MachineLearningService machineLearningService;

	@Override
	public MaxentModelTrainer getMaxentModelTrainer() {
		MaxentModelTrainerImpl maxentModelTrainer = new MaxentModelTrainerImpl();
		maxentModelTrainer.setMachineLearningService(this.getMachineLearningService());
		return maxentModelTrainer;
	}

	@Override
	public ClassificationModel getMaxentModel() {
		MaximumEntropyModel maxentModel = new MaximumEntropyModel();
		maxentModel.setMachineLearningService(this.getMachineLearningService());
		return maxentModel;
	}

	public MachineLearningService getMachineLearningService() {
		return machineLearningService;
	}

	public void setMachineLearningService(MachineLearningService machineLearningService) {
		this.machineLearningService = machineLearningService;
	}

}
