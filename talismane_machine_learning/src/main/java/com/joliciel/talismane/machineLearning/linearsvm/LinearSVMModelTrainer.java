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
package com.joliciel.talismane.machineLearning.linearsvm;

import com.joliciel.talismane.machineLearning.ClassificationModelTrainer;

/**
 * Trains a Linear SVM machine learning model for a given CorpusEventStream.
 * Note: if the same feature is encountered multiple times in a feature vector, the value fed to the trainer
 * will be the sum of values for all of the feature occurrences.
 * @author Assaf Urieli
 *
 */
public interface LinearSVMModelTrainer extends ClassificationModelTrainer {
	/**
	 * A parameter accepted by the linear SVM model trainer.
	 * @author Assaf Urieli
	 *
	 */
	public enum LinearSVMModelParameter {
		/**
		 * Linear SVM parameter C.
		 */
		ConstraintViolationCost(Double.class),
		/**
		 * Linear SVM parameter epsilon.
		 */
		Epsilon(Double.class),
		/**
		 * In how many distinct events should a feature appear in order to get included in the model?
		 */
		Cutoff(Integer.class),
		/**
		 * Linear SVM solver type, selected from {@link LinearSVMSolverType}.
		 */
		SolverType(LinearSVMSolverType.class),
		/**
		 * Should we train multiple models, one per class, as one vs. rest models ?
		 * */
		OneVsRest(Boolean.class),
		/**
		 * If one vs. rest is used, should we balance the event counts so that the current outcome events
		 * are approximately proportional to the other outcome events?
		 */
		BalanceEventCounts(Boolean.class)
		;
		
		private Class<?> parameterType;
		private LinearSVMModelParameter(Class<?> parameterType) {
			this.parameterType = parameterType;
		}
		public Class<?> getParameterType() {
			return parameterType;
		}
	}
	
	/**
	 * The Linear SVM solver algorithms.
	 * Since we want a probabilistic classifier, our options here are limited to logistic regression.
	 * @author Assaf Urieli
	 *
	 */
	public enum LinearSVMSolverType {
		/**
		 * L2-regularized logistic regression (primal)
		 */
		L2R_LR,
		/**
		 * L1-regularized logistic regression
		 */
		L1R_LR,
		/**
		 * L2-regularized logistic regression (dual)
		 */
		L2R_LR_DUAL
	}

	
	/**
	 * The cost of constraint violation (typically referred to as "C" in SVM literature).
	 * Default value: 1.0.
	 */
	public double getConstraintViolationCost();
	public void setConstraintViolationCost(double constraintViolationCost);

	/**
	 * The stopping criterion. Default value: 0.01.
	 */
	public double getEpsilon();
	public void setEpsilon(double epsilon);
	
	/**
	 * The solver algorithm used by this linear SVM. Default: L2R_LR.
	 */
	public LinearSVMSolverType getSolverType();
	public void setSolverType(LinearSVMSolverType solverType);
	
	/**
	 * Should we train multiple models, one per class, as one vs. rest models ?
	 * */
	public boolean isOneVsRest();
	public void setOneVsRest(boolean oneVsRest);

	/**
	 * If one vs. rest is used, should we balance the event counts so that the current outcome events
	 * are approximately proportional to the other outcome events?
	 */
	public boolean isBalanceEventCounts();
	public void setBalanceEventCounts(boolean balanceEventCounts);
}
