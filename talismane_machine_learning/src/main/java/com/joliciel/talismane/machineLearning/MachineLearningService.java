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
import java.util.zip.ZipInputStream;

import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.typesafe.config.Config;

/**
 * A service for retrieving implementations of the machineLearning package.
 * 
 * @author Assaf Urieli
 *
 */
public interface MachineLearningService {
	/**
	 * Get a CorpusEvent corresponding to the featureResults and classification
	 * provided.
	 */
	public ClassificationEvent getClassificationEvent(List<FeatureResult<?>> featureResults, String classification);

	/**
	 * Get the machine learning model stored in a given ZipInputStream.
	 * 
	 * @param zis
	 *            the zip input stream
	 */
	public MachineLearningModel getMachineLearningModel(ZipInputStream zis);

	/**
	 * Get the machine learning model stored in a given ZipInputStream.
	 * 
	 * @param zis
	 *            the zip input stream
	 */
	public ClassificationModel getClassificationModel(ZipInputStream zis);

	/**
	 * Get a classification model trainer corresponding to a given outcome type
	 * and a given algorithm.
	 */
	public ClassificationModelTrainer getClassificationModelTrainer(Config config);

	/**
	 * Get a default implementation of the ExternalResourceFinder.
	 */
	public ExternalResourceFinder getExternalResourceFinder();

	/**
	 * Create the decision corresponding to a particular name. This decision
	 * will be considered statistical.
	 */
	public Decision createDecision(String code, double probability);

	/**
	 * Create the decision corresponding to a particular name. This decision
	 * will be considered statistical.
	 */
	public Decision createDecision(String code, double score, double probability);

	/**
	 * Create a default decision with a probability of 1.0, for a given outcome.
	 * This decision will not be considered statistical.
	 */
	public Decision createDefaultDecision(String code);
}
