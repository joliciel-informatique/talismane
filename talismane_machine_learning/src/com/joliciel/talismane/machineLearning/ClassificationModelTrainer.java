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

/**
 * Trains a machine learning classification model for a given CorpusEventStream.
 * @author Assaf Urieli
 *
 */
public interface ClassificationModelTrainer {
	/**
	 * Return the ClassificationModel trained using the CorpusEventStream provided.
	 * @param corpusEventStream the event stream containing the events to be used for training
	 * @param featureDescriptors the feature descriptors required to apply this model to new data.
	 * @return
	 */
	public ClassificationModel trainModel(ClassificationEventStream corpusEventStream, List<String> featureDescriptors);

	/**
	 * Return the ClassificationModel trained using the CorpusEventStream provided.
	 * @param corpusEventStream the event stream containing the events to be used for training
	 * @param descriptors all of the descriptors required to perform analysis using this model (e.g. feature descriptors, etc.)
	 * @return
	 */
	public ClassificationModel trainModel(ClassificationEventStream corpusEventStream, Map<String,List<String>> descriptors);

	/**
	 * Statistical cutoff for feature inclusion: features must appear at least this many times to be included in the model.
	 * Note that for numeric features, any value > 0 counts as 1 occurrence for cutoff purposes.
	 * @return
	 */
	public int getCutoff();
	public void setCutoff(int cutoff);
	
	/**
	 * Set parameters for this trainer type.
	 * @param parameters
	 */
	public void setParameters(Map<String,Object> parameters);
}
