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

import java.io.IOException;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.machineLearning.linearsvm.LinearSVMModel;
import com.joliciel.talismane.machineLearning.linearsvm.LinearSVMOneVsRestModel;
import com.joliciel.talismane.machineLearning.maxent.MaximumEntropyModel;
import com.joliciel.talismane.machineLearning.perceptron.PerceptronClassificationModel;
import com.joliciel.talismane.utils.JolicielException;
import com.joliciel.talismane.utils.LogUtils;

/**
 * A class for constructing models implementing AbstractMachineLearningModel.
 * 
 * @author Assaf Urieli
 *
 */
public class MachineLearningModelFactory {
	private static final Logger LOG = LoggerFactory.getLogger(MachineLearningModelFactory.class);

	public MachineLearningModelFactory() {
	}

	public ClassificationModel getClassificationModel(ZipInputStream zis) throws ClassNotFoundException {
		MachineLearningModel model = this.getMachineLearningModel(zis);
		if (!(model instanceof ClassificationModel))
			throw new JolicielException("Model in zip file not " + ClassificationModel.class.getSimpleName());
		return (ClassificationModel) model;
	}

	public MachineLearningModel getMachineLearningModel(ZipInputStream zis) throws ClassNotFoundException {
		try {
			MachineLearningModel machineLearningModel = null;
			ZipEntry ze = zis.getNextEntry();
			if (!ze.getName().equals("algorithm.txt")) {
				throw new JolicielException("Expected algorithm.txt as first entry in zip. Was: " + ze.getName());
			}

			// note: assuming the model type will always be the first entry
			@SuppressWarnings("resource")
			Scanner typeScanner = new Scanner(zis, "UTF-8");
			MachineLearningAlgorithm algorithm = MachineLearningAlgorithm.MaxEnt;
			if (typeScanner.hasNextLine()) {
				String algorithmString = typeScanner.nextLine();
				try {
					algorithm = MachineLearningAlgorithm.valueOf(algorithmString);
				} catch (IllegalArgumentException iae) {
					LogUtils.logError(LOG, iae);
					throw new JolicielException("Unknown algorithm: " + algorithmString);
				}
			} else {
				throw new JolicielException("Cannot find algorithm in zip file");
			}
			switch (algorithm) {
			case MaxEnt:
				machineLearningModel = new MaximumEntropyModel();
				break;
			case LinearSVM:
				machineLearningModel = new LinearSVMModel();
				break;
			case LinearSVMOneVsRest:
				machineLearningModel = new LinearSVMOneVsRestModel();
				break;
			case Perceptron:
				machineLearningModel = new PerceptronClassificationModel();
				break;
			default:
				throw new JolicielException("Machine learning algorithm not yet supported: " + algorithm);
			}

			while ((ze = zis.getNextEntry()) != null) {
				LOG.debug(ze.getName());
				machineLearningModel.loadZipEntry(zis, ze);
			} // next zip entry

			machineLearningModel.onLoadComplete();

			return machineLearningModel;
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		} finally {
			try {
				zis.close();
			} catch (IOException ioe) {
				LogUtils.logError(LOG, ioe);
			}
		}
	}

}
