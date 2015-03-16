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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.machineLearning.ClassificationObserver;
import com.joliciel.talismane.machineLearning.MachineLearningAlgorithm;
import com.joliciel.talismane.utils.JolicielException;
import com.joliciel.talismane.utils.LogUtils;

import opennlp.model.MaxentModel;

/**
 * A wrapper for a perceptron model and the features used to train it -
 * useful since the same features need to be used when evaluating on the basis on this model.
 * Also contains the attributes describing how the model was trained, for reference purposes.
 * 
 * @param T the decision type to be made by this model
 * @author Assaf Urieli
 *
 */
class OpenNLPPerceptronModel extends AbstractOpenNLPModel {
	private static final Log LOG = LogFactory.getLog(OpenNLPPerceptronModel.class);
	
	/**
	 * Default constructor for factory.
	 */
	OpenNLPPerceptronModel() {}
	
	/**
	 * Construct from a newly trained model including the feature descriptors.
	 * @param model
	 * @param featureDescriptors
	 */
	OpenNLPPerceptronModel(MaxentModel model,
			Map<String,List<String>> descriptors,
			Map<String,Object> trainingParameters) {
		super(model, descriptors, trainingParameters);
	}
	
	@Override
	public MachineLearningAlgorithm getAlgorithm() {
		return MachineLearningAlgorithm.OpenNLPPerceptron;
	}

	@Override
	public ClassificationObserver getDetailedAnalysisObserver(File file) {
		throw new JolicielException("No detailed analysis observer currently available for perceptrons.");
	}
	

	@Override
	public void writeModelToStream(OutputStream outputStream) {
		try {
			new OpenNLPPerceptronModelWriterWrapper(this.getModel(), outputStream).persist();
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void loadModelFromStream(InputStream inputStream) {
		OpenNLPPerceptronModelReaderWrapper maxentModelReader = new OpenNLPPerceptronModelReaderWrapper(inputStream);
		try {
			this.setModel(maxentModelReader.getModel());
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
}
