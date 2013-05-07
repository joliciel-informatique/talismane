///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2013 Assaf Urieli
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.machineLearning.AbstractMachineLearningModel;
import com.joliciel.talismane.machineLearning.AnalysisObserver;
import com.joliciel.talismane.machineLearning.DecisionFactory;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.Outcome;
import com.joliciel.talismane.utils.LogUtils;

class PerceptronModel<T extends Outcome> extends AbstractMachineLearningModel<T> {
	private static final Log LOG = LogFactory.getLog(PerceptronModel.class);
	PerceptronModelParameters params = null;
	PerceptronDecisionMaker<T> decisionMaker;
	
	PerceptronModel() { }
	
	public PerceptronModel(PerceptronModelParameters params,
			Map<String, List<String>> descriptors,
			DecisionFactory<T> decisionFactory) {
		this.params = params;
		this.setDecisionFactory(decisionFactory);
		this.setDescriptors(descriptors);
	}

	@Override
	public DecisionMaker<T> getDecisionMaker() {
		if (decisionMaker==null) {
			decisionMaker = new PerceptronDecisionMaker<T>(params, this.getDecisionFactory());
		}
		return decisionMaker;
	}

	@Override
	public AnalysisObserver<T> getDetailedAnalysisObserver(File file) {
		return new PerceptronDetailedAnalysisWriter<T>(decisionMaker, file);
	}

	@Override
	public MachineLearningAlgorithm getAlgorithm() {
		return MachineLearningAlgorithm.Perceptron;
	}

	@Override
	public void loadModelFromStream(InputStream inputStream) {
		try {
			ObjectInputStream in = new ObjectInputStream(inputStream);
			params = (PerceptronModelParameters) in.readObject();
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		} catch (ClassNotFoundException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
		
	}

	@Override
	public void writeModelToStream(OutputStream outputStream) {
		try {
			ObjectOutputStream out = new ObjectOutputStream(outputStream);

			out.writeObject(params);
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void loadDataFromStream(InputStream inputStream, ZipEntry zipEntry) {
		// nothing to do
	}

	@Override
	public void writeDataToStream(ZipOutputStream zos) {
		// nothing to do
	}

}
