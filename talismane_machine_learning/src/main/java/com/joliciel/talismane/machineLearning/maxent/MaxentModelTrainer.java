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
package com.joliciel.talismane.machineLearning.maxent;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.machineLearning.ClassificationEventStream;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.ClassificationModelTrainer;
import com.joliciel.talismane.machineLearning.MachineLearningModel;
import com.joliciel.talismane.machineLearning.maxent.custom.GISTrainer;
import com.joliciel.talismane.machineLearning.maxent.custom.TwoPassRealValueDataIndexer;
import com.typesafe.config.Config;

import opennlp.model.DataIndexer;
import opennlp.model.EventStream;
import opennlp.model.MaxentModel;

/**
 * Trains a MaxEnt machine learning model for a given CorpusEventStream.<br>
 * Uses the Apache OpenNLP OpenMaxent implementation.
 * 
 * @author Assaf Urieli
 *
 */
public class MaxentModelTrainer implements ClassificationModelTrainer {
  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(MaxentModelTrainer.class);

  private int iterations;
  private int cutoff;
  private double sigma;
  private double smoothing;

  private Config config;

  @Override
  public ClassificationModel trainModel(ClassificationEventStream corpusEventStream, List<String> featureDescriptors) throws IOException {
    Map<String, List<String>> descriptors = new HashMap<String, List<String>>();
    descriptors.put(MachineLearningModel.FEATURE_DESCRIPTOR_KEY, featureDescriptors);
    return this.trainModel(corpusEventStream, descriptors);
  }

  @Override
  public ClassificationModel trainModel(ClassificationEventStream corpusEventStream, Map<String, List<String>> descriptors) throws IOException {
    MaxentModel maxentModel = null;
    EventStream eventStream = new OpenNLPEventStream(corpusEventStream);

    DataIndexer dataIndexer = new TwoPassRealValueDataIndexer(eventStream, cutoff);
    GISTrainer trainer = new GISTrainer(true);
    if (this.getSmoothing() > 0) {
      trainer.setSmoothing(true);
      trainer.setSmoothingObservation(this.getSmoothing());
    } else if (this.getSigma() > 0) {
      trainer.setGaussianSigma(this.getSigma());
    }

    maxentModel = trainer.trainModel(iterations, dataIndexer, cutoff);

    MaximumEntropyModel model = new MaximumEntropyModel(maxentModel, config, descriptors);
    model.addModelAttribute("cutoff", this.getCutoff());
    model.addModelAttribute("iterations", this.getIterations());
    model.addModelAttribute("sigma", this.getSigma());
    model.addModelAttribute("smoothing", this.getSmoothing());

    model.getModelAttributes().putAll(corpusEventStream.getAttributes());

    return model;
  }

  /**
   * The number of training iterations to run.
   */
  public int getIterations() {
    return iterations;
  }

  public void setIterations(int iterations) {
    this.iterations = iterations;
  }

  @Override
  public int getCutoff() {
    return cutoff;
  }

  @Override
  public void setCutoff(int cutoff) {
    this.cutoff = cutoff;
  }

  /**
   * Sigma for Gaussian smoothing on maxent training.
   */

  public double getSigma() {
    return sigma;
  }

  public void setSigma(double sigma) {
    this.sigma = sigma;
  }

  /**
   * Additive smoothing parameter during maxent training.
   */

  public double getSmoothing() {
    return smoothing;
  }

  public void setSmoothing(double smoothing) {
    this.smoothing = smoothing;
  }

  @Override
  public void setParameters(Config config) {
    this.config = config;
    Config maxentConfig = config.getConfig("MaxEnt");

    this.setCutoff(config.getInt("cutoff"));
    this.setIterations(config.getInt("iterations"));
    this.setSigma(maxentConfig.getDouble("sigma"));
    this.setSmoothing(maxentConfig.getDouble("smoothing"));
  }
}
