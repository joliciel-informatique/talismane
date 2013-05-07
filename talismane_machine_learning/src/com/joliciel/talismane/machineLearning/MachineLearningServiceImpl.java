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

import com.joliciel.talismane.machineLearning.MachineLearningModel.MachineLearningAlgorithm;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.linearsvm.LinearSVMService;
import com.joliciel.talismane.machineLearning.maxent.MaxentService;
import com.joliciel.talismane.machineLearning.perceptron.PerceptronService;

class MachineLearningServiceImpl implements MachineLearningService {
	MaxentService maxentService;
	LinearSVMService linearSVMService;
	PerceptronService perceptronService;
	
	@Override
	public CorpusEvent getCorpusEvent(List<FeatureResult<?>> featureResults,
			String classification) {
		CorpusEventImpl corpusEvent = new CorpusEventImpl(featureResults, classification);
		return corpusEvent;
	}

	@Override
	public <T extends Outcome> MachineLearningModel<T> getModel(
			ZipInputStream zis) {
		ModelFactory modelFactory = new ModelFactory();
		modelFactory.setMaxentService(this.getMaxentService());
		modelFactory.setLinearSVMService(this.getLinearSVMService());
		modelFactory.setPerceptronService(this.getPerceptronService());
		MachineLearningModel<T> model = modelFactory.getModel(zis);
		return model;
	}

	@Override
	public <T extends Outcome> ModelTrainer<T> getModelTrainer(
			MachineLearningAlgorithm algorithm, Map<String,Object> parameters) {
		ModelTrainerFactory modelTrainerFactory = new ModelTrainerFactory();
		modelTrainerFactory.setMaxentService(this.getMaxentService());
		modelTrainerFactory.setLinearSVMService(this.getLinearSVMService());
		modelTrainerFactory.setPerceptronService(this.getPerceptronService());
		ModelTrainer<T> modelTrainer = modelTrainerFactory.makeModelTrainer(algorithm, parameters);
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

}
