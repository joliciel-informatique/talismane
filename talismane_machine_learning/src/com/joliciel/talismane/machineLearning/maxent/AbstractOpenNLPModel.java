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

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.machineLearning.AbstractClassificationModel;
import com.joliciel.talismane.machineLearning.DecisionFactory;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.Outcome;
import opennlp.model.MaxentModel;

/**
 * A wrapper for a OpenNLP model and the features used to train it -
 * useful since the same features need to be used when evaluating on the basis on this model.
 * Also contains the attributes describing how the model was trained, for reference purposes.
 * 
 * @param T the decision type to be made by this model
 * @author Assaf Urieli
 *
 */
abstract class AbstractOpenNLPModel<T extends Outcome> extends AbstractClassificationModel<T> implements OpenNLPModel {
	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(AbstractOpenNLPModel.class);
	private MaxentModel model;
	
	/**
	 * Default constructor for factory.
	 */
	AbstractOpenNLPModel() {}
	
	/**
	 * Construct from a newly trained model including the feature descriptors.
	 * @param model
	 * @param featureDescriptors
	 */
	AbstractOpenNLPModel(MaxentModel model,
			Map<String,List<String>> descriptors,
			DecisionFactory<T> decisionFactory) {
		super();
		this.model = model;
		this.setDescriptors(descriptors);
		this.setDecisionFactory(decisionFactory);
	}

	@Override
	public DecisionMaker<T> getDecisionMaker() {
		OpenNLPDecisionMaker<T> decisionMaker = new OpenNLPDecisionMaker<T>(this.getModel());
		decisionMaker.setDecisionFactory(this.getDecisionFactory());
		return decisionMaker;
	}
	
	@Override
	public MaxentModel getModel() {
		return model;
	}
	public void setModel(MaxentModel model) {
		this.model = model;
	}

	@Override
	public boolean loadDataFromStream(InputStream inputStream, ZipEntry zipEntry) {
		return false;
	}

	@Override
	public void writeDataToStream(ZipOutputStream zos) {
		// no model-specific data
	}

	
}
