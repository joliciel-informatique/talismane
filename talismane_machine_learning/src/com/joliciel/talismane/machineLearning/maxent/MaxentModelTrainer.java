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

import com.joliciel.talismane.machineLearning.ClassificationModelTrainer;
import com.joliciel.talismane.machineLearning.Outcome;

/**
 * Trains a MaxEnt machine learning model for a given CorpusEventStream.<br/>
 * Uses the Apache OpenNLP OpenMaxent implementation.
 * @author Assaf Urieli
 *
 */
public interface MaxentModelTrainer<T extends Outcome> extends ClassificationModelTrainer<T> {
	/**
	 * A parameter accepted by the maxent model trainer.
	 * @author Assaf Urieli
	 *
	 */
	public enum MaxentModelParameter {
		Iterations(Integer.class),
		Cutoff(Integer.class),
		Sigma(Double.class),
		Smoothing(Double.class);
		
		private Class<?> parameterType;
		private MaxentModelParameter(Class<?> parameterType) {
			this.parameterType = parameterType;
		}
		public Class<?> getParameterType() {
			return parameterType;
		}
	}
	/**
	 * The number of training iterations to run.
	 * @return
	 */
	public int getIterations();
	public void setIterations(int iterations);
	
	/**
	 * Sigma for Gaussian smoothing on maxent training.
	 * @return
	 */
	public double getSigma();
	public void setSigma(double sigma);

	/**
	 * Additive smoothing parameter during maxent training.
	 * @return
	 */
	public double getSmoothing();
	public void setSmoothing(double smoothing);
}
