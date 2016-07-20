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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.machineLearning.AbstractMachineLearningModel;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.MachineLearningService;

import opennlp.model.MaxentModel;

/**
 * A wrapper for a OpenNLP model and the features used to train it - useful
 * since the same features need to be used when evaluating on the basis on this
 * model. Also contains the attributes describing how the model was trained, for
 * reference purposes.
 * 
 * @param T
 *            the decision type to be made by this model
 * @author Assaf Urieli
 *
 */
abstract class AbstractOpenNLPModel extends AbstractMachineLearningModel implements OpenNLPModel {
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(AbstractOpenNLPModel.class);

	private MachineLearningService machineLearningService;

	private MaxentModel model;
	private transient Set<String> outcomeNames = null;

	/**
	 * Default constructor for factory.
	 */
	AbstractOpenNLPModel() {
	}

	/**
	 * Construct from a newly trained model including the feature descriptors.
	 */
	AbstractOpenNLPModel(MaxentModel model, Map<String, List<String>> descriptors) {
		super();
		this.model = model;
		this.setDescriptors(descriptors);
	}

	@Override
	public DecisionMaker getDecisionMaker() {
		OpenNLPDecisionMaker decisionMaker = new OpenNLPDecisionMaker(this.getModel());
		decisionMaker.setMachineLearningService(this.getMachineLearningService());
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

	@Override
	public Set<String> getOutcomeNames() {
		if (outcomeNames == null) {
			outcomeNames = new TreeSet<String>();
			for (int i = 0; i < this.model.getNumOutcomes(); i++) {
				outcomeNames.add(this.model.getOutcome(i));
			}
		}
		return outcomeNames;
	}

	public MachineLearningService getMachineLearningService() {
		return machineLearningService;
	}

	public void setMachineLearningService(MachineLearningService machineLearningService) {
		this.machineLearningService = machineLearningService;
	}

	@Override
	protected void persistOtherEntries(ZipOutputStream zos) throws IOException {
	}

}
