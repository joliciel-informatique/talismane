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
package com.joliciel.talismane.machineLearning;

import java.util.Map;

import com.joliciel.talismane.machineLearning.MachineLearningModel.MachineLearningAlgorithm;
import com.joliciel.talismane.machineLearning.linearsvm.LinearSVMModelTrainer;
import com.joliciel.talismane.machineLearning.linearsvm.LinearSVMService;
import com.joliciel.talismane.machineLearning.maxent.MaxentModelTrainer;
import com.joliciel.talismane.machineLearning.maxent.MaxentService;
import com.joliciel.talismane.machineLearning.maxent.OpenNLPPerceptronModelTrainer;
import com.joliciel.talismane.machineLearning.perceptron.PerceptronModelTrainer;
import com.joliciel.talismane.machineLearning.perceptron.PerceptronService;
import com.joliciel.talismane.utils.JolicielException;

/**
 * A class for constructing model trainers implementing ModelTrainer.
 * @author Assaf Urieli
 *
 */
class ModelTrainerFactory {
	PerceptronService perceptronService;
	MaxentService maxentService;
	LinearSVMService linearSVMService;

	/**
	 * Get a model trainer corresponding to a given outcome type and a given algorithm.
	 * @param <T>
	 * @param algorithm
	 * @return
	 */
	public<T extends Outcome> ModelTrainer<T> makeModelTrainer(MachineLearningAlgorithm algorithm, Map<String,Object> parameters) {
		ModelTrainer<T> modelTrainer = null;
		switch (algorithm) {
		case MaxEnt:
			MaxentModelTrainer<T> maxentModelTrainer = maxentService.getMaxentModelTrainer();
			modelTrainer = maxentModelTrainer;
			break;
		case LinearSVM:
			LinearSVMModelTrainer<T> linearSVMModelTrainer = linearSVMService.getLinearSVMModelTrainer();
			modelTrainer = linearSVMModelTrainer;
			break;
		case Perceptron:
			PerceptronModelTrainer<T> perceptronModelTrainer = perceptronService.getPerceptronModelTrainer();
			modelTrainer = perceptronModelTrainer;
			break;
		case OpenNLPPerceptron:
			OpenNLPPerceptronModelTrainer<T> openNLPPerceptronModelTrainer = maxentService.getPerceptronModelTrainer();
			modelTrainer = openNLPPerceptronModelTrainer;
			break;
		default:
			throw new JolicielException("Machine learning algorithm not yet supported: " + algorithm);
		}

		modelTrainer.setParameters(parameters);
		return modelTrainer;
	}

	public MaxentService getMaxentService() {
		return maxentService;
	}

	public void setMaxentService(MaxentService maxentService) {
		this.maxentService = maxentService;
	}

	public LinearSVMService getLinearSVMService() {
		return linearSVMService;
	}

	public void setLinearSVMService(LinearSVMService linearSVMService) {
		this.linearSVMService = linearSVMService;
	}

	public PerceptronService getPerceptronService() {
		return perceptronService;
	}

	public void setPerceptronService(PerceptronService perceptronService) {
		this.perceptronService = perceptronService;
	}

}
