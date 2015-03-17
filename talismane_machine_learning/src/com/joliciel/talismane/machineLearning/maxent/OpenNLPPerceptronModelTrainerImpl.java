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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.ClassificationEventStream;
import com.joliciel.talismane.machineLearning.MachineLearningModel;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.utils.JolicielException;
import com.joliciel.talismane.utils.PerformanceMonitor;

import opennlp.model.DataIndexer;
import opennlp.model.EventStream;
import opennlp.model.MaxentModel;
import opennlp.model.TwoPassRealValueDataIndexer;
import opennlp.perceptron.PerceptronTrainer;

/**
 * A class for training and persisting Maxent Models.
 * @author Assaf Urieli
 *
 */
class OpenNLPPerceptronModelTrainerImpl implements OpenNLPPerceptronModelTrainer {
	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(OpenNLPPerceptronModelTrainerImpl.class);
	private static final PerformanceMonitor MONITOR = PerformanceMonitor.getMonitor(OpenNLPPerceptronModelTrainerImpl.class);
	
	private int iterations = 100;
	private int cutoff = 5;
	private double tolerance = PerceptronTrainer.TOLERANCE_DEFAULT;
	private double stepSizeDecrease = 0;
	private boolean useAverage = false;
	private boolean useSkippedAverage = false;
	
	private MachineLearningService machineLearningService;

	private Map<String,Object> trainingParameters = new HashMap<String, Object>();
	
	@Override
	public ClassificationModel trainModel(
			ClassificationEventStream corpusEventStream,
			List<String> featureDescriptors) {
		Map<String,List<String>> descriptors = new HashMap<String, List<String>>();
		descriptors.put(MachineLearningModel.FEATURE_DESCRIPTOR_KEY, featureDescriptors);
		return this.trainModel(corpusEventStream, descriptors);
	}

	@Override
	public ClassificationModel trainModel(
			ClassificationEventStream corpusEventStream,
			Map<String,List<String>> descriptors) {
		MaxentModel perceptronModel = null;
		EventStream eventStream = new OpenNLPEventStream(corpusEventStream);
		try {
	    	DataIndexer dataIndexer = new TwoPassRealValueDataIndexer(eventStream, cutoff);
			PerceptronTrainer trainer = new PerceptronTrainer();
			trainer.setSkippedAveraging(useSkippedAverage);
			trainer.setStepSizeDecrease(stepSizeDecrease);
			trainer.setTolerance(tolerance);

			MONITOR.startTask("train");
			try {
				perceptronModel =  trainer.trainModel(iterations, dataIndexer, cutoff, useAverage);
			} finally {
				MONITOR.endTask();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		OpenNLPPerceptronModel model = new OpenNLPPerceptronModel(perceptronModel, descriptors, this.trainingParameters);
		model.setMachineLearningService(machineLearningService);
		model.addModelAttribute("cutoff", "" + this.getCutoff());
		model.addModelAttribute("iterations", "" + this.getIterations());
		model.addModelAttribute("averaging", "" + this.isUseAverage());
		model.addModelAttribute("skippedAveraging", "" + this.isUseSkippedAverage());
		model.addModelAttribute("tolerance", "" + this.getTolerance());
		model.addModelAttribute("stepSizeDecrease", "" + this.getStepSizeDecrease());
		
		model.getModelAttributes().putAll(corpusEventStream.getAttributes());

		return model;
	}

	public int getIterations() {
		return iterations;
	}


	public void setIterations(int iterations) {
		this.iterations = iterations;
	}

	public int getCutoff() {
		return cutoff;
	}

	public void setCutoff(int cutoff) {
		this.cutoff = cutoff;
	}

	public double getTolerance() {
		return tolerance;
	}

	public void setTolerance(double tolerance) {
		this.tolerance = tolerance;
	}

	public double getStepSizeDecrease() {
		return stepSizeDecrease;
	}

	public void setStepSizeDecrease(double stepSizeDecrease) {
		this.stepSizeDecrease = stepSizeDecrease;
	}

	public boolean isUseAverage() {
		return useAverage;
	}

	public void setUseAverage(boolean useAverage) {
		this.useAverage = useAverage;
	}

	public boolean isUseSkippedAverage() {
		return useSkippedAverage;
	}

	public void setUseSkippedAverage(boolean useSkippedAverage) {
		this.useSkippedAverage = useSkippedAverage;
	}

	@Override
	public void setParameters(Map<String, Object> parameters) {
		if (parameters!=null) {
			this.trainingParameters = parameters;
			for (String parameter : parameters.keySet()) {
				OpenNLPPerceptronModelParameter modelParameter = OpenNLPPerceptronModelParameter.valueOf(parameter);
				Object value = parameters.get(parameter);
				if (!modelParameter.getParameterType().isAssignableFrom(value.getClass())) {
					throw new JolicielException("Parameter of wrong type: " + parameter + ". Expected: " + modelParameter.getParameterType().getSimpleName());
				}
				switch (modelParameter) {
				case Iterations:
					this.setIterations((Integer)value);
					break;
				case Cutoff:
					this.setCutoff((Integer)value);
					break;
				case UseAverage:
					this.setUseAverage((Boolean)value);
					break;
				case UseSkippedAverage:
					this.setUseSkippedAverage((Boolean)value);
					break;
				case Tolerance:
					this.setTolerance((Double)value);
					break;
				case StepSizeDecrease:
					this.setStepSizeDecrease((Double)value);
					break;
				default:
					throw new JolicielException("Unknown parameter type: " + modelParameter);
				}
			}
		}
	}

	public MachineLearningService getMachineLearningService() {
		return machineLearningService;
	}

	public void setMachineLearningService(
			MachineLearningService machineLearningService) {
		this.machineLearningService = machineLearningService;
	}
	
	
}
