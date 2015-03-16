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
package com.joliciel.talismane.machineLearning.maxent;

import com.joliciel.talismane.machineLearning.ClassificationModelTrainer;

/**
 * Trains a Perceptron machine learning model for a given CorpusEventStream, as described in:<br/>
 * Discriminative Training Methods for Hidden Markov Models: Theory and Experiments
 * with the Perceptron Algorithm. Michael Collins, EMNLP 2002.<br/>
 * Uses the Apache OpenNLP OpenMaxent implementation.
 * @author Assaf Urieli
 *
 */
public interface OpenNLPPerceptronModelTrainer extends ClassificationModelTrainer {
	/**
	 * A parameter accepted by the perceptron model trainer.
	 * @author Assaf Urieli
	 *
	 */
	public enum OpenNLPPerceptronModelParameter {
		Iterations(Integer.class),
		Cutoff(Integer.class),
		UseAverage(Boolean.class),
		UseSkippedAverage(Boolean.class),
		StepSizeDecrease(Double.class),
		Tolerance(Double.class);

		private Class<?> parameterType;
		private OpenNLPPerceptronModelParameter(Class<?> parameterType) {
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
	public void setIterations(int iterations);
	public int getIterations();

	/**
	 * Specifies the tolerance. If the change in training set accuracy
	 * is less than this, stop iterating.
	 * <br/>Copied from Apache OpenNLP OpenMaxent.
	 */
	public void setTolerance(double tolerance);
	public double getTolerance();

	/**
	 * Enables and sets step size decrease. The step size is
	 * decreased every iteration by the specified value.
	 * <br/>Copied from Apache OpenNLP OpenMaxent.
	 * 
	 * @param decrease - step size decrease in percent
	 */
	public void setStepSizeDecrease(double stepSizeDecrease);
	public double getStepSizeDecrease();

	/**
	 * Use averaged weighting instead of standard (integer) weighting.
	 */
	public boolean isUseAverage();
	public void setUseAverage(boolean useAverage);

	/**
	 * Enables skipped averaging, this flag changes the standard
	 * averaging to special averaging instead.
	 * <p>
	 * If we are doing averaging, and the current iteration is one
	 * of the first 20 or it is a perfect square, then updated the
	 * summed parameters. 
	 * <p>
	 * The reason we don't take all of them is that the parameters change
	 * less toward the end of training, so they drown out the contributions
	 * of the more volatile early iterations. The use of perfect
	 * squares allows us to sample from successively farther apart iterations.
	 * <br/>Copied from Apache OpenNLP OpenMaxent.
	 */
	public boolean isUseSkippedAverage();
	public void setUseSkippedAverage(boolean useSkippedAverage);
}
