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

import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.linearsvm.LinearSVMService;
import com.joliciel.talismane.machineLearning.maxent.MaxentService;
import com.joliciel.talismane.machineLearning.perceptron.PerceptronService;

class MachineLearningServiceImpl implements MachineLearningService {
	MaxentService maxentService;
	LinearSVMService linearSVMService;
	PerceptronService perceptronService;
	
	@Override
	public ClassificationEvent getClassificationEvent(List<FeatureResult<?>> featureResults,
			String classification) {
		ClassificationEventImpl corpusEvent = new ClassificationEventImpl(featureResults, classification);
		return corpusEvent;
	}


	@Override
	public MachineLearningModel getMachineLearningModel(ZipInputStream zis) {
		ModelFactory modelFactory = new ModelFactory();
		modelFactory.setMaxentService(this.getMaxentService());
		modelFactory.setLinearSVMService(this.getLinearSVMService());
		modelFactory.setPerceptronService(this.getPerceptronService());
		return modelFactory.getMachineLearningModel(zis);
	}

	@Override
	public <T extends Outcome> ClassificationModel<T> getClassificationModel(
			ZipInputStream zis) {
		@SuppressWarnings("unchecked")
		ClassificationModel<T> model = (ClassificationModel<T>) this.getMachineLearningModel(zis);
		return model;
	}

	@Override
	public <T extends Outcome> ClassificationModelTrainer<T> getClassificationModelTrainer(
			MachineLearningAlgorithm algorithm, Map<String,Object> parameters) {
		ModelTrainerFactory modelTrainerFactory = new ModelTrainerFactory();
		modelTrainerFactory.setMaxentService(this.getMaxentService());
		modelTrainerFactory.setLinearSVMService(this.getLinearSVMService());
		modelTrainerFactory.setPerceptronService(this.getPerceptronService());
		ClassificationModelTrainer<T> modelTrainer = modelTrainerFactory.makeClassificationModelTrainer(algorithm, parameters);
		return modelTrainer;
	}


	@Override
	public <T> RankingModelTrainer<T> getRankingModelTrainer(
			MachineLearningAlgorithm algorithm, Map<String, Object> parameters) {
		ModelTrainerFactory modelTrainerFactory = new ModelTrainerFactory();
		modelTrainerFactory.setMaxentService(this.getMaxentService());
		modelTrainerFactory.setLinearSVMService(this.getLinearSVMService());
		modelTrainerFactory.setPerceptronService(this.getPerceptronService());
		RankingModelTrainer<T> modelTrainer = modelTrainerFactory.makeRankingModelTrainer(algorithm, parameters);
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

	@Override
	public ExternalResourceFinder getExternalResourceFinder() {
		ExternalResourceFinderImpl finder = new ExternalResourceFinderImpl();
		return finder;
	}

	public PerceptronService getPerceptronService() {
		return perceptronService;
	}

	public void setPerceptronService(PerceptronService perceptronService) {
		this.perceptronService = perceptronService;
	}

	@Override
	public <T> RankingEvent<T> getRankingEvent(T input, RankingSolution solution) {
		RankingEventImpl<T> event = new RankingEventImpl<T>(input, solution);
		return event;
	}


}
