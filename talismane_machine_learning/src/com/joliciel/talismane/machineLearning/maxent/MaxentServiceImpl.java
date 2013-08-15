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
package com.joliciel.talismane.machineLearning.maxent;

import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.Outcome;

class MaxentServiceImpl implements MaxentService {

	@Override
	public <T extends Outcome> MaxentModelTrainer<T> getMaxentModelTrainer() {
		MaxentModelTrainerImpl<T> maxentModelTrainer = new MaxentModelTrainerImpl<T>();
		return maxentModelTrainer;
	}

	@Override
	public <T extends Outcome> ClassificationModel<T> getMaxentModel() {
		MaximumEntropyModel<T> maxentModel = new MaximumEntropyModel<T>();
		return maxentModel;
	}

	@Override
	public <T extends Outcome> OpenNLPPerceptronModelTrainer<T> getPerceptronModelTrainer() {
		OpenNLPPerceptronModelTrainerImpl<T> trainer = new OpenNLPPerceptronModelTrainerImpl<T>();
		return trainer;
	}

	@Override
	public <T extends Outcome> ClassificationModel<T> getPerceptronModel() {
		OpenNLPPerceptronModel<T> model = new OpenNLPPerceptronModel<T>();
		return model;
	}


}
