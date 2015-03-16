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
package com.joliciel.talismane.machineLearning.perceptron;

import java.util.List;
import java.util.Map;

import com.joliciel.talismane.machineLearning.ClassificationEventStream;
import com.joliciel.talismane.machineLearning.ClassificationModelTrainer;

public interface PerceptronClassificationModelTrainer extends ClassificationModelTrainer {
	/**
	 * A parameter accepted by the perceptron model trainer.
	 * @author Assaf Urieli
	 *
	 */
	public enum PerceptronModelParameter {
		Iterations(Integer.class),
		Cutoff(Integer.class),
		Tolerance(Double.class),
		AverageAtIntervals(Boolean.class);
		
		private Class<?> parameterType;
		private PerceptronModelParameter(Class<?> parameterType) {
			this.parameterType = parameterType;
		}
		public Class<?> getParameterType() {
			return parameterType;
		}
	}
	/**
	 * The maximum number of training iterations to run.
	 * @return
	 */
	public int getIterations();
	public void setIterations(int iterations);
	
	
	public void trainModelsWithObserver(ClassificationEventStream corpusEventStream, Map<String, List<String>> descriptors, PerceptronModelTrainerObserver observer, List<Integer> observationPoints);
	
	public void trainModelsWithObserver(ClassificationEventStream corpusEventStream,
			List<String> featureDescriptors, PerceptronModelTrainerObserver observer, List<Integer> observationPoints);
}
