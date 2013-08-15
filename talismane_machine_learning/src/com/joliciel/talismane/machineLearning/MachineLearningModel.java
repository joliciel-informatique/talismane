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
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * A machine learning model, capable of providing a DecisionMaker, and encapsluating
 * all of the information required to analyse mew data (e.g. feature descriptors).
 * @author Assaf Urieli
 *
 * @param <T>
 */
public interface MachineLearningModel {
	public enum MachineLearningModelType {
		Classification,
		Ranking
	}
	
	public static final String FEATURE_DESCRIPTOR_KEY = "feature";
	
	/**
	 * Persist this model to a file.
	 * @param modelFile
	 */
	public void persist(File modelFile);

	/**
	 * Get this model's defining attributes.
	 * @return
	 */
	public Map<String, String> getModelAttributes();

	/**
	 * Add a defining attribute to this model.
	 * @param name
	 * @param value
	 */
	public void addModelAttribute(String name, String value);
	
	/**
	 * Get this model's dependencies.
	 * @return
	 */
	public Map<String, Object> getDependencies();

	/**
	 * Add a dependency to this model.
	 * @param name
	 * @param value
	 */
	public void addDependency(String name, Serializable dependency);

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
	 * The machine learning algorithm implemented by this model.
	 * @return
	 */
	public MachineLearningAlgorithm getAlgorithm();
	
	/**
	 * External resources used by this model.
	 * @return
	 */
	public Collection<ExternalResource> getExternalResources();
	public void setExternalResources(Collection<ExternalResource> externalResources);
	
	public ExternalResourceFinder getExternalResourceFinder();
	
	/**
	 * Load some aspect of this model from a zip entry.
	 * @param zis
	 * @param ze
	 * @throws IOException
	 * @return true if entry loaded, false otherwise
	 */
	boolean loadZipEntry(ZipInputStream zis, ZipEntry ze) throws IOException;
	
	/**
	 * Called when load from a zip file has been completed.
	 */
	public void onLoadComplete();
}