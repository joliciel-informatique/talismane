///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2013 Assaf Urieli
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

import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.Outcome;
import com.joliciel.talismane.machineLearning.RankingModel;

class PerceptronServiceImpl implements PerceptronService {
	@Override
	public <T extends Outcome> PerceptronClassificationModelTrainer<T> getPerceptronModelTrainer() {
		PerceptronClassifactionModelTrainerImpl<T> trainer = new PerceptronClassifactionModelTrainerImpl<T>();
		return trainer;
	}

	@Override
	public <T extends Outcome> ClassificationModel<T> getPerceptronModel() {
		PerceptronClassificationModel<T> model = new PerceptronClassificationModel<T>();
		return model;
	}

	@Override
	public <T> PerceptronRankingModelTrainer<T> getPerceptronRankingModelTrainer() {
		PerceptronRankingModelTrainerImpl<T> trainer = new PerceptronRankingModelTrainerImpl<T>();
		return trainer;
	}

	@Override
	public RankingModel getPerceptronRankingModel() {
		PerceptronRankingModel model = new PerceptronRankingModel();
		return model;
	}
}
