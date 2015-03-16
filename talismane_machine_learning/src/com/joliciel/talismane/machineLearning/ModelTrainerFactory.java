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

import java.util.Map;

import com.joliciel.talismane.machineLearning.linearsvm.LinearSVMModelTrainer;
import com.joliciel.talismane.machineLearning.linearsvm.LinearSVMService;
import com.joliciel.talismane.machineLearning.maxent.MaxentModelTrainer;
import com.joliciel.talismane.machineLearning.maxent.MaxentService;
import com.joliciel.talismane.machineLearning.maxent.OpenNLPPerceptronModelTrainer;
import com.joliciel.talismane.machineLearning.perceptron.PerceptronClassificationModelTrainer;
import com.joliciel.talismane.machineLearning.perceptron.PerceptronRankingModelTrainer;
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
	 * Get a classification model trainer corresponding to a given outcome type and a given algorithm.
	 * @param <T>
	 * @param algorithm
	 * @return
	 */
	public ClassificationModelTrainer makeClassificationModelTrainer(MachineLearningAlgorithm algorithm, Map<String,Object> parameters) {
		ClassificationModelTrainer modelTrainer = null;
		switch (algorithm) {
		case MaxEnt:
			MaxentModelTrainer maxentModelTrainer = maxentService.getMaxentModelTrainer();
			modelTrainer = maxentModelTrainer;
			break;
		case LinearSVM:
		case LinearSVMOneVsRest:
			LinearSVMModelTrainer linearSVMModelTrainer = linearSVMService.getLinearSVMModelTrainer();
			modelTrainer = linearSVMModelTrainer;
			break;
		case Perceptron:
			PerceptronClassificationModelTrainer perceptronModelTrainer = perceptronService.getPerceptronModelTrainer();
			modelTrainer = perceptronModelTrainer;
			break;
		case OpenNLPPerceptron:
			OpenNLPPerceptronModelTrainer openNLPPerceptronModelTrainer = maxentService.getPerceptronModelTrainer();
			modelTrainer = openNLPPerceptronModelTrainer;
			break;
		default:
			throw new JolicielException("Machine learning algorithm not yet supported: " + algorithm);
		}

		modelTrainer.setParameters(parameters);
		return modelTrainer;
	}
	
	/**
	 * Get a ranking model trainer corresponding to a given input type and a given algorithm.
	 * @param <T>
	 * @param algorithm
	 * @return
	 */
	public<T> RankingModelTrainer<T> makeRankingModelTrainer(MachineLearningAlgorithm algorithm, Map<String,Object> parameters) {
		RankingModelTrainer<T> modelTrainer = null;
		switch (algorithm) {
		case PerceptronRanking:
			PerceptronRankingModelTrainer<T> perceptronModelTrainer = perceptronService.getPerceptronRankingModelTrainer();
			modelTrainer = perceptronModelTrainer;
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
