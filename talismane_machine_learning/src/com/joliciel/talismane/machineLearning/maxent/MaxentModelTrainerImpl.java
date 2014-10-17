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
import com.joliciel.talismane.machineLearning.DecisionFactory;
import com.joliciel.talismane.machineLearning.MachineLearningModel;
import com.joliciel.talismane.machineLearning.Outcome;
import com.joliciel.talismane.utils.JolicielException;
import com.joliciel.talismane.utils.PerformanceMonitor;

import opennlp.maxent.GISTrainer;
import opennlp.model.DataIndexer;
import opennlp.model.EventStream;
import opennlp.model.MaxentModel;
import opennlp.model.TwoPassRealValueDataIndexer;

/**
 * A class for training and persisting Maxent Models.
 * @author Assaf Urieli
 *
 */
class MaxentModelTrainerImpl<T extends Outcome> implements MaxentModelTrainer<T> {
	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(MaxentModelTrainerImpl.class);
	private static final PerformanceMonitor MONITOR = PerformanceMonitor.getMonitor(MaxentModelTrainerImpl.class);
	
	private int iterations = 100;
	private int cutoff = 5;
	private double sigma = 0;
	private double smoothing = 0;

	
	@Override
	public ClassificationModel<T> trainModel(
			ClassificationEventStream corpusEventStream,
			DecisionFactory<T> decisionFactory, List<String> featureDescriptors) {
		Map<String,List<String>> descriptors = new HashMap<String, List<String>>();
		descriptors.put(MachineLearningModel.FEATURE_DESCRIPTOR_KEY, featureDescriptors);
		return this.trainModel(corpusEventStream, decisionFactory, descriptors);
	}

	@Override
	public ClassificationModel<T> trainModel(
			ClassificationEventStream corpusEventStream,
			DecisionFactory<T> decisionFactory,
			Map<String,List<String>> descriptors) {
		MaxentModel maxentModel = null;
		EventStream eventStream = new OpenNLPEventStream(corpusEventStream);
		try {
	    	DataIndexer dataIndexer = new TwoPassRealValueDataIndexer(eventStream, cutoff);
			GISTrainer trainer = new GISTrainer(true);
			if (this.getSmoothing()>0) {
				trainer.setSmoothing(true);
				trainer.setSmoothingObservation(this.getSmoothing());
			} else if (this.getSigma()>0) {
				trainer.setGaussianSigma(this.getSigma());
			}
			MONITOR.startTask("train");
			try {
				maxentModel =  trainer.trainModel(iterations, dataIndexer, cutoff);
			} finally {
				MONITOR.endTask();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		MaximumEntropyModel<T> model = new MaximumEntropyModel<T>(maxentModel, descriptors, decisionFactory);
		model.addModelAttribute("cutoff", "" + this.getCutoff());
		model.addModelAttribute("iterations", "" + this.getIterations());
		model.addModelAttribute("sigma", "" + this.getSigma());
		model.addModelAttribute("smoothing", "" + this.getSmoothing());
		
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

	/**
	 * Sigma for Gaussian smoothing on maxent training.
	 * @return
	 */
	public double getSigma() {
		return sigma;
	}

	public void setSigma(double sigma) {
		this.sigma = sigma;
	}

	/**
	 * Additive smoothing parameter during maxent training.
	 * @return
	 */
	public double getSmoothing() {
		return smoothing;
	}

	public void setSmoothing(double smoothing) {
		this.smoothing = smoothing;
	}

	@Override
	public void setParameters(Map<String, Object> parameters) {
		if (parameters!=null) {
			for (String parameter : parameters.keySet()) {
				MaxentModelParameter modelParameter = MaxentModelParameter.valueOf(parameter);
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
				case Sigma:
					this.setSigma((Double)value);
					break;
				case Smoothing:
					this.setSmoothing((Double)value);
					break;
				default:
					throw new JolicielException("Unknown parameter type: " + modelParameter);
				}
			}
		}
	}
}
