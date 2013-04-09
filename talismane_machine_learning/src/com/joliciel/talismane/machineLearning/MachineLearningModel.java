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

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;

/**
 * A machine learning model, capable of providing a DecisionMaker, and encapsluating
 * all of the information required to analyse mew data (e.g. feature descriptors).
 * @author Assaf Urieli
 *
 * @param <T>
 */
public interface MachineLearningModel<T extends Outcome> {
	/**
	 * Machine learning algorithms supported by Talimane.
	 *
	 */
	public enum MachineLearningAlgorithm {
		MaxEnt,
		LinearSVM,
		Perceptron
	}
	
	public static final String FEATURE_DESCRIPTOR_KEY = "feature";
	
	/**
	 * Persist this model to a file.
	 * @param modelFile
	 */
	public void persist(File modelFile);

	/**
	 * Get the decision maker for this model.
	 * @return
	 */
	public DecisionMaker<T> getDecisionMaker();

	/**
	 * Get this model's defining attributes.
	 * @return
	 */
	public Map<String, Object> getModelAttributes();

	/**
	 * Add a defining attribute to this model.
	 * @param name
	 * @param value
	 */
	public void addModelAttribute(String name, Object value);

	/**
	 * A map of all descriptors required to apply this model to new data.
	 * @return
	 */
	public Map<String, List<String>> getDescriptors();
	
	/**
	 * Get the list of feature descriptors for this model.
	 * @return
	 */
	public List<String> getFeatureDescriptors();
	
	/**
	 * An observer that will write low-level details of this model's analysis to a file.
	 * @param file
	 * @return
	 */
	public AnalysisObserver getDetailedAnalysisObserver(File file);

	/**
	 * The machine learning algorithm implemented by this model.
	 * @return
	 */
	public MachineLearningAlgorithm getAlgorithm();

	/**
	 * Load this model's internal binary representation from an input stream.
	 * @param inputStream
	 */
	public void loadModelFromStream(InputStream inputStream);

	/**
	 * Write this model's internal binary representation to an output stream.
	 * @param outputStream
	 */
	public void writeModelToStream(OutputStream outputStream);
	
	/**
	 * The decision factory associated with this model.
	 */
	public DecisionFactory<T> getDecisionFactory();
	public void setDecisionFactory(DecisionFactory<T> decisionFactory);
	
	/**
	 * Loads data from the input stream that is specific to this model type.
	 */
	public void loadDataFromStream(InputStream inputStream, ZipEntry zipEntry);
	
	/**
	 * External resources used by this model.
	 * @return
	 */
	public Collection<ExternalResource> getExternalResources();
	public void setExternalResources(Collection<ExternalResource> externalResources);
	
	public ExternalResourceFinder getExternalResourceFinder();
}