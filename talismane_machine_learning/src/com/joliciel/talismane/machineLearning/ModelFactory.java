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
package com.joliciel.talismane.machineLearning;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.machineLearning.MachineLearningModel.MachineLearningAlgorithm;
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
	private static final Log LOG = LogFactory.getLog(ModelFactory.class);
	
	private PerceptronService perceptronService;
	private MaxentService maxentService;
	private LinearSVMService linearSVMService;
	
	@SuppressWarnings("unchecked")
	public<T extends Outcome> MachineLearningModel<T> getModel(ZipInputStream zis) {
		try {
			MachineLearningModel<T> machineLearningModel = null;
			ZipEntry ze = zis.getNextEntry();
			if (!ze.getName().equals("algorithm.txt")) {
				throw new JolicielException("Expected algorithm.txt as first entry in zip. Was: " + ze.getName());
			}
			// note: assuming the model type will always be the first entry
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
			case Perceptron:
				machineLearningModel = perceptronService.getPerceptronModel();
				break;
			case OpenNLPPerceptron:
				machineLearningModel = maxentService.getPerceptronModel();
				break;
			default:
				throw new JolicielException("Machine learning algorithm not yet supported: " + algorithm);
			}

		    while ((ze = zis.getNextEntry()) != null) {
		    	LOG.debug(ze.getName());
		    	if (ze.getName().equals("model.bin")) {
		    	    machineLearningModel.loadModelFromStream(zis);
		    	} else if (ze.getName().equals("decisionFactory.obj")) {
		    		ObjectInputStream in = new ObjectInputStream(zis);
					try {
						DecisionFactory<T> decisionFactory = (DecisionFactory<T>) in.readObject();
			    		machineLearningModel.setDecisionFactory(decisionFactory);
					} catch (ClassNotFoundException e) {
						LogUtils.logError(LOG, e);
						throw new RuntimeException(e);
					}
		    	} else if (ze.getName().equals("externalResources.obj")) {
		    		ObjectInputStream in = new ObjectInputStream(zis);
					try {
						List<ExternalResource> externalResources = (List<ExternalResource>) in.readObject();
			    		machineLearningModel.setExternalResources(externalResources);
					} catch (ClassNotFoundException e) {
						LogUtils.logError(LOG, e);
						throw new RuntimeException(e);
					}
		    	} else if (ze.getName().endsWith("_descriptors.txt")) {
		    		String key = ze.getName().substring(0, ze.getName().length() - "_descriptors.txt".length());
		    		Scanner scanner = new Scanner(zis, "UTF-8");
		    		List<String> descriptorList = new ArrayList<String>();
		    		while (scanner.hasNextLine()) {
		    			String descriptor = scanner.nextLine();
		    			descriptorList.add(descriptor);
		    		}
		    		machineLearningModel.getDescriptors().put(key, descriptorList);
		    	} else if (ze.getName().equals("attributes.txt")) {
		    		Scanner scanner = new Scanner(zis, "UTF-8");
		    		while (scanner.hasNextLine()) {
		    			String line = scanner.nextLine();
		    			if (line.length()>0) {	
			    			String[] parts = line.split("\t");
			    			String name = parts[0];
			    			if (parts.length>2) {
			    				List<String> collection = new ArrayList<String>();
			    				for (int i = 1; i<parts.length; i++) {
			    					collection.add(parts[i]);
			    				}
			    				machineLearningModel.addModelAttribute(name, collection);
			    			} else {
			    				String value = "";
			    				if (parts.length>1)
			    					value = parts[1];
			    				machineLearningModel.addModelAttribute(name, value);
			    			}
		    			}
		    		}
		    	} else {
		    		machineLearningModel.loadDataFromStream(zis, ze);
		    	}
		    } // next zip entry

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
