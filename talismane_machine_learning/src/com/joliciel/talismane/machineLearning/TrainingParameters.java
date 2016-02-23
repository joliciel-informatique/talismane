///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2015 Joliciel Informatique
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.machineLearning.MachineLearningAlgorithm;
import com.joliciel.talismane.machineLearning.linearsvm.LinearSVMModelTrainer;
import com.joliciel.talismane.machineLearning.linearsvm.LinearSVMModelTrainer.LinearSVMSolverType;
import com.joliciel.talismane.machineLearning.maxent.MaxentModelTrainer;
import com.joliciel.talismane.utils.LogUtils;

/**
 * A class storing everything required to run a machine learning training session.
 * Possible keys in props:<br/>
 * <ul>
 * <li><i>prefix</i>.algorithm*: training algorithm</li>
 * <li><i>prefix</i>.iterations: training iterations</li>
 * <li><i>prefix</i>.cutoff: feature cutoff</li>
 * <li><i>prefix</i>.linearSVMSolver: linear SVM solver type</li>
 * <li><i>prefix</i>.linearSVMCost: linear SVM cost parameter</li>
 * <li><i>prefix</i>.linearSVMEpsilon: linear SVM epsilon parameter</li>
 * <li><i>prefix</i>.balanceEventCounts: whether or not event counts should be balanced (linear SVM only)</li>
 * <li><i>prefix</i>.features: points at a relative file path where feature desciptors can be found</li>
 * </ul>
 * The prefix is optional - if no prefix is provided, no . is added before the property name.
 * @author Assaf Urieli
 *
 */
public class TrainingParameters {
	private static final Log LOG = LogFactory.getLog(TrainingParameters.class);
	
	private MachineLearningAlgorithm algorithm;
	private Map<String,Object> parameters;
	private List<String> featureDescriptors;
	
	private TrainingParameters defaultParameters;
	private String prefix;
	private String featureProperty = "features";
	
	/**
	 * Load properties.
	 * @param prefix if not null and not empty, will be prefixed to each property name with an additional "."
	 */
	public TrainingParameters(String prefix) {
		this(null, prefix);
	}
	
	/**
	 * Load with respect to a set of default parameters.
	 * @param defaultParameters if not null, these will give default parameters which can be overridden by the current set of properties.
	 * @param prefix if not null and not empty, will be prefixed to each property name with an additional "."
	 */
	public TrainingParameters(TrainingParameters defaultParameters, String prefix) {
		this.defaultParameters = defaultParameters;
		this.prefix = prefix;
	}
	
	public TrainingParameters(String prefix, File baseDir, Map<String, String> props) {
		this(prefix);
		this.load(baseDir, props);
	}
	
	public TrainingParameters(TrainingParameters defaultParameters, String prefix, File baseDir, Map<String, String> props) {
		this(defaultParameters, prefix);
		this.load(baseDir, props);
	}
	
	/**
	 * Load a given set of properties with paths relative to a given base directory,
	 * and return a set of all property names processed.
	 * @param baseDir The directory relative to which paths in the property list should be interpreted. If null, current working directory is used.
	 * @param props The list of properties to interpret.
	 * @return the set of all property names processed.
	 */
	public Set<String> load(File baseDir, Map<String, String> props) {
		try {
			Set<String> propNames = new HashSet<String>();
			
			String propPrefix = "";
			if (prefix!=null && prefix.length()>0) {
				propPrefix = prefix + ".";
			}
			algorithm = null;
			if (defaultParameters!=null)
				algorithm = defaultParameters.getAlgorithm();
			if (props.containsKey(propPrefix + "algorithm")) {
				algorithm = MachineLearningAlgorithm.valueOf(props.get(propPrefix + "algorithm"));
				propNames.add(propPrefix + "algorithm");
			}
			
			parameters = new HashMap<String, Object>();
			if (defaultParameters!=null)
				parameters = new HashMap<String,Object>(defaultParameters.getParameters());
			if (algorithm!=null) {
				if (algorithm==MachineLearningAlgorithm.MaxEnt) {
					if (props.containsKey(propPrefix + "iterations")) {
						parameters.put(MaxentModelTrainer.MaxentModelParameter.Iterations.name(), Integer.parseInt(props.get(propPrefix + "iterations")));
						propNames.add(propPrefix + "iterations");
					}
					if (props.containsKey(propPrefix + "cutoff")) {
						parameters.put(MaxentModelTrainer.MaxentModelParameter.Cutoff.name(), Integer.parseInt(props.get(propPrefix + "cutoff")));
						propNames.add(propPrefix + "cutoff");
					}
				} else if (algorithm==MachineLearningAlgorithm.LinearSVM) {
					if (props.containsKey(propPrefix + "cutoff")) {
						parameters.put(LinearSVMModelTrainer.LinearSVMModelParameter.Cutoff.name(), Integer.parseInt(props.get(propPrefix + "cutoff")));
						propNames.add(propPrefix + "cutoff");
					}
					if (props.containsKey(propPrefix + "linearSVMSolver")) {
						parameters.put(LinearSVMModelTrainer.LinearSVMModelParameter.SolverType.name(), LinearSVMSolverType.valueOf(props.get(propPrefix + "linearSVMSolver")));
						propNames.add(propPrefix + "linearSVMSolver");
					}
					if (props.containsKey(propPrefix + "linearSVMCost")) {
						parameters.put(LinearSVMModelTrainer.LinearSVMModelParameter.ConstraintViolationCost.name(), Double.parseDouble(props.get(propPrefix + "linearSVMCost")));
						propNames.add(propPrefix + "linearSVMCost");
					}
					if (props.containsKey(propPrefix + "linearSVMEpsilon")) {
						parameters.put(LinearSVMModelTrainer.LinearSVMModelParameter.Epsilon.name(), Double.parseDouble(props.get(propPrefix + "linearSVMEpsilon")));
						propNames.add(propPrefix + "linearSVMEpsilon");
					}
					if (props.containsKey(propPrefix + "balanceEventCounts")) {
						parameters.put(LinearSVMModelTrainer.LinearSVMModelParameter.BalanceEventCounts.name(), "true".equalsIgnoreCase(props.get(propPrefix + "balanceEventCounts")));
						propNames.add(propPrefix + "balanceEventCounts");
					}
				}
			}
			
			if (defaultParameters!=null && defaultParameters.getFeatureDescriptors()!=null)
				featureDescriptors = new ArrayList<String>(defaultParameters.getFeatureDescriptors());
			if (props.containsKey(propPrefix + featureProperty)) {
				featureDescriptors = new ArrayList<String>();
				File featureFile = new File(baseDir, props.get(propPrefix + featureProperty));
				propNames.add(propPrefix + featureProperty);
				Scanner featureScanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(featureFile), "UTF-8")));
				featureDescriptors = new ArrayList<String>();
				while (featureScanner.hasNextLine()) {
					String descriptor = featureScanner.nextLine();
					featureDescriptors.add(descriptor);
				}
				featureScanner.close();
			}
			
			return propNames;
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * The machine learning algorithm to use.
	 * @return
	 */
	public MachineLearningAlgorithm getAlgorithm() {
		return algorithm;
	}
	public void setAlgorithm(MachineLearningAlgorithm algorithm) {
		this.algorithm = algorithm;
	}
	
	/**
	 * The parameters to be provided to this algorithm.
	 * @return
	 */
	public Map<String, Object> getParameters() {
		return parameters;
	}
	public void setParameters(Map<String, Object> parameters) {
		this.parameters = parameters;
	}
	
	/**
	 * The feature descriptors used to describe each training event.
	 * @return
	 */
	public List<String> getFeatureDescriptors() {
		return featureDescriptors;
	}
	public void setFeatureDescriptors(List<String> featureDescriptors) {
		this.featureDescriptors = featureDescriptors;
	}

	/**
	 * The name of the property used to read the feature descriptor path.
	 * Default is "features".
	 * @return
	 */
	public String getFeatureProperty() {
		return featureProperty;
	}
	public void setFeatureProperty(String featureProperty) {
		this.featureProperty = featureProperty;
	}
	
	
}
