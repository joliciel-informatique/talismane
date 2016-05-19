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

import com.joliciel.talismane.machineLearning.linearsvm.LinearSVMService;
import com.joliciel.talismane.machineLearning.maxent.MaxentService;
import com.joliciel.talismane.machineLearning.perceptron.PerceptronService;
import com.joliciel.talismane.utils.JolicielException;
import com.joliciel.talismane.utils.LogUtils;

/**
 * A class for constructing models implementing AbstractMachineLearningModel.
 * @author Assaf Urieli
 *
 */
class ModelFactory {
	private static final Logger LOG = LoggerFactory.getLogger(ModelFactory.class);
	
	private PerceptronService perceptronService;
	private MaxentService maxentService;
	private LinearSVMService linearSVMService;
	
	public MachineLearningModel getMachineLearningModel(ZipInputStream zis) {
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
				machineLearningModel = maxentService.getMaxentModel();
				break;
			case LinearSVM:
				machineLearningModel = linearSVMService.getLinearSVMModel();
				break;
			case LinearSVMOneVsRest:
				machineLearningModel = linearSVMService.getLinearSVMOneVsRestModel();
				break;
			case Perceptron:
				machineLearningModel = perceptronService.getPerceptronModel();
				break;
			case PerceptronRanking:
				machineLearningModel = perceptronService.getPerceptronRankingModel();
				break;
			case OpenNLPPerceptron:
				machineLearningModel = maxentService.getPerceptronModel();
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
	

	public MaxentService getMaxentService() {
		return maxentService;
	}

	public void setMaxentService(MaxentService maxentService) {
		this.maxentService = maxentService;
	}

	public LinearSVMService getLinearSVMService() {
		return linearSVMService;
	}

	public void setLinearSVMService(LinearSVMService linearSVMService) {
		this.linearSVMService = linearSVMService;
	}

	public PerceptronService getPerceptronService() {
		return perceptronService;
	}

	public void setPerceptronService(PerceptronService perceptronService) {
		this.perceptronService = perceptronService;
	}
	
	
}
