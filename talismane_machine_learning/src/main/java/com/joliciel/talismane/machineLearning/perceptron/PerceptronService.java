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

import com.joliciel.talismane.machineLearning.ClassificationModel;

/**
 * A service for retrieving implementations of the perceptron package.
 * 
 * @author Assaf Urieli
 *
 */
public interface PerceptronService {

	/**
	 * Different methods of scoring perceptron classifiers.
	 * 
	 * @author Assaf Urieli
	 *
	 */
	public enum PerceptronScoring {
		/**
		 * Use standard additive perceptron scoring, where each state's score is
		 * the sum of scores of incremental states.
		 */
		additive,
		/**
		 * Use a geometric mean of state probabilities, where the probability is
		 * calculated by first transforming all scores to positive (minimum =
		 * 1), and then dividing by the total.
		 */
		normalisedLinear,
		/**
		 * Use a geometric mean of state probabilities, where the probability is
		 * e^{score/absmax(scores)}, where absmax is the maximum absolute value
		 * of scores. This gives us positive scores from 1/e to e. We then
		 * divide by the total.
		 */
		normalisedExponential
	}

	/**
	 * Returns a perceptron classification model trainer.
	 */
	public PerceptronClassificationModelTrainer getPerceptronModelTrainer();

	/**
	 * Get an "empty" perceptron classification model.
	 */
	public ClassificationModel getPerceptronModel();
}
