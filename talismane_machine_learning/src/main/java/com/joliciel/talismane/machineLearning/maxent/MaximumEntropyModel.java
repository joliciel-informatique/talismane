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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.machineLearning.ClassificationObserver;
import com.joliciel.talismane.machineLearning.MachineLearningAlgorithm;
import com.typesafe.config.Config;

import opennlp.model.MaxentModel;

/**
 * A wrapper for a maxent model and the features used to train it - useful since
 * the same features need to be used when evaluating on the basis on this model.
 * Also contains the attributes describing how the model was trained, for
 * reference purposes.
 * 
 * @author Assaf Urieli
 *
 */
public class MaximumEntropyModel extends AbstractOpenNLPModel {
  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(MaximumEntropyModel.class);

  /**
   * Default constructor for factory.
   */
  public MaximumEntropyModel() {
  }

  /**
   * Construct from a newly trained model including the feature descriptors.
   */
  MaximumEntropyModel(MaxentModel model, Config config, Map<String, List<String>> descriptors) {
    super(model, config, descriptors);
  }

  @Override
  public MachineLearningAlgorithm getAlgorithm() {
    return MachineLearningAlgorithm.MaxEnt;
  }

  @Override
  public ClassificationObserver getDetailedAnalysisObserver(File file) throws IOException {
    MaxentDetailedAnalysisWriter observer = new MaxentDetailedAnalysisWriter(this.getModel(), file);
    return observer;
  }

  @Override
  public void writeModelToStream(OutputStream outputStream) throws IOException {
    new MaxentModelWriterWrapper(this.getModel(), outputStream).persist();
  }

  @Override
  public void loadModelFromStream(InputStream inputStream) throws IOException {
    MaxentModelReaderWrapper maxentModelReader = new MaxentModelReaderWrapper(inputStream);
    this.setModel(maxentModelReader.getModel());
  }

  @Override
  public void onLoadComplete() {
  }
}
