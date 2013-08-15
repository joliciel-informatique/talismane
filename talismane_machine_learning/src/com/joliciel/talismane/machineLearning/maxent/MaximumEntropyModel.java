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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.machineLearning.ClassificationObserver;
import com.joliciel.talismane.machineLearning.DecisionFactory;
import com.joliciel.talismane.machineLearning.MachineLearningAlgorithm;
import com.joliciel.talismane.machineLearning.Outcome;
import com.joliciel.talismane.utils.LogUtils;

import opennlp.model.MaxentModel;

/**
 * A wrapper for a maxent model and the features used to train it -
 * useful since the same features need to be used when evaluating on the basis on this model.
 * Also contains the attributes describing how the model was trained, for reference purposes.
 * 
 * @param T the decision type to be made by this model
 * @author Assaf Urieli
 *
 */
class MaximumEntropyModel<T extends Outcome> extends AbstractOpenNLPModel<T> {
	private static final Log LOG = LogFactory.getLog(MaximumEntropyModel.class);
	
	/**
	 * Default constructor for factory.
	 */
	MaximumEntropyModel() {}
	
	/**
	 * Construct from a newly trained model including the feature descriptors.
	 * @param model
	 * @param featureDescriptors
	 */
	MaximumEntropyModel(MaxentModel model,
			Map<String,List<String>> descriptors,
			DecisionFactory<T> decisionFactory) {
		super(model, descriptors, decisionFactory);
	}
	
	@Override
	public MachineLearningAlgorithm getAlgorithm() {
		return MachineLearningAlgorithm.MaxEnt;
	}

	@Override
	public ClassificationObserver<T> getDetailedAnalysisObserver(File file) {
		MaxentDetailedAnalysisWriter<T> observer = new MaxentDetailedAnalysisWriter<T>(this.getModel(), file);
		return observer;
	}
	
	@Override
	public void writeModelToStream(OutputStream outputStream) {
		try {
			new MaxentModelWriterWrapper(this.getModel(), outputStream).persist();
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void loadModelFromStream(InputStream inputStream) {
		MaxentModelReaderWrapper maxentModelReader = new MaxentModelReaderWrapper(inputStream);
		try {
			this.setModel(maxentModelReader.getModel());
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
}
