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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.typesafe.config.Config;

/**
 * A machine learning model, capable of providing a DecisionMaker, and
 * encapsluating all of the information required to analyse mew data (e.g.
 * feature descriptors).
 * 
 * @author Assaf Urieli
 *
 */
public interface MachineLearningModel {
  public static final String FEATURE_DESCRIPTOR_KEY = "feature";

  /**
   * Persist this model to an OutputStream.
   * 
   * @throws IOException
   */
  public void persist(OutputStream outputStream) throws IOException;

  /**
   * Persist this model to a file.
   * 
   * @throws IOException
   */
  public void persist(File modelFile) throws IOException;

  /**
   * Get this model's defining attributes.
   */
  public Map<String, Object> getModelAttributes();

  /**
   * Add a defining attribute to this model.
   */
  public void addModelAttribute(String name, Object value);

  /**
   * Get this model's dependencies.
   */
  public Map<String, Object> getDependencies();

  /**
   * Add a dependency to this model.
   */
  public void addDependency(String name, Serializable dependency);

  /**
   * A map of all descriptors required to apply this model to new data.
   */
  public Map<String, List<String>> getDescriptors();

  /**
   * Get the list of feature descriptors for this model.
   */
  public List<String> getFeatureDescriptors();

  /**
   * The machine learning algorithm implemented by this model.
   */
  public MachineLearningAlgorithm getAlgorithm();

  /**
   * The configuration used to construct this model, useful for retraining a
   * new model using the identical config.
   */
  public Config getConfig();

  /**
   * External resources used by this model.
   */
  public Collection<ExternalResource<?>> getExternalResources();

  public void setExternalResources(Collection<ExternalResource<?>> externalResources);

  public ExternalResourceFinder getExternalResourceFinder();

  /**
   * Load some aspect of this model from a zip entry.
   * 
   * @return true if entry loaded, false otherwise
   * @throws ClassNotFoundException
   */
  boolean loadZipEntry(ZipInputStream zis, ZipEntry ze) throws IOException, ClassNotFoundException;

  /**
   * Called when load from a zip file has been completed.
   */
  public void onLoadComplete();
}
