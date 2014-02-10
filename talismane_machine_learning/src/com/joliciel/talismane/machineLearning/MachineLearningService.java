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

import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

import com.joliciel.talismane.machineLearning.features.FeatureResult;

/**
 * A service for retrieving implementations of the machineLearning package.
 * @author Assaf Urieli
 *
 */
public interface MachineLearningService {
	/**
	 * Get a CorpusEvent corresponding to the featureResults and classification provided.
	 * @param featureResults
	 * @param classification
	 * @return
	 */
	public ClassificationEvent getClassificationEvent(List<FeatureResult<?>> featureResults,
			String classification);
	
	/**
	 * Get a RankingEvent corresponding to a particular input and solution.
	 * @param <T>
	 * @param input
	 * @param solution
	 * @return
	 */
	public<T> RankingEvent<T> getRankingEvent(T input, RankingSolution solution);
	
	/**
	 * Get the machine learning model stored in a given ZipInputStream.
	 * @param <T> the outcome type for this model
	 * @param zis the zip input stream
	 * @return
	 */
	public MachineLearningModel getMachineLearningModel(ZipInputStream zis);

	/**
	 * Get the machine learning model stored in a given ZipInputStream.
	 * @param <T> the outcome type for this model
	 * @param zis the zip input stream
	 * @return
	 */
	public<T extends Outcome> ClassificationModel<T> getClassificationModel(ZipInputStream zis);
	
	/**
	 * Get a classification model trainer corresponding to a given outcome type and a given algorithm.
	 * @param <T>
	 * @param algorithm
	 * @return
	 */
	public<T extends Outcome> ClassificationModelTrainer<T> getClassificationModelTrainer(MachineLearningAlgorithm algorithm, Map<String,Object> parameters);
	
	/**
	 * Get a ranking model trainer corresponding to a given input type and a given algorithm.
	 * @param <T>
	 * @param algorithm
	 * @return
	 */
	public<T> RankingModelTrainer<T> getRankingModelTrainer(MachineLearningAlgorithm algorithm, Map<String,Object> parameters);

	/**
	 * Get a default implementation of the ExternalResourceFinder.
	 * @return
	 */
	public ExternalResourceFinder getExternalResourceFinder();
	
	/**
	 * Create a decision as though it were made by a statistical engine.
	 * @param <T>
	 * @param outcome
	 * @param probability
	 * @return
	 */
	public <T extends Outcome> Decision<T> createDecision(T outcome, double probability);
}
