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

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Trains multiple models instead of a single model.
 * This allows us to collect features a single time, and process them many times,
 * in cases where feature collection is a costly activity.
 * In order to avoid keeping all models in memory, models will be directly written to disk
 * in a given directory.
 * @author Assaf Urieli
 *
 * @param <T>
 */
public interface ClassificationMultiModelTrainer<T extends Outcome> extends
		ClassificationModelTrainer<T> {

	/**
	 * Return the ClassificationModel trained using the CorpusEventStream provided.
	 * @param corpusEventStream the event stream containing the events to be used for training
	 * @param decisionFactory the decision factory used to convert outcomes labels to Outcomes
	 * @param featureDescriptors the feature descriptors required to apply this model to new data.
	 * @return
	 */
	public void trainModels(ClassificationEventStream corpusEventStream, DecisionFactory<T> decisionFactory, List<String> featureDescriptors);

	/**
	 * Return the ClassificationModel trained using the CorpusEventStream provided.
	 * @param corpusEventStream the event stream containing the events to be used for training
	 * @param decisionFactory the decision factory used to convert outcomes labels to Outcomes
	 * @param descriptors all of the descriptors required to perform analysis using this model (e.g. feature descriptors, etc.)
	 * @return
	 */
	public void trainModels(ClassificationEventStream corpusEventStream, DecisionFactory<T> decisionFactory, Map<String,List<String>> descriptors);

	/**
	 * Set parameters for this trainer type.
	 * @param parameters
	 */
	public void setParameterSets(List<Map<String,Object>> parameterSets);
	
	/**
	 * The directory where models should be written.
	 * @param outDir
	 */
	public void setOutDir(File outDir);
	public File getOutDir();
}
