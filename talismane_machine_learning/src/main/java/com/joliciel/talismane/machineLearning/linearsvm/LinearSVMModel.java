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
package com.joliciel.talismane.machineLearning.linearsvm;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.machineLearning.AbstractMachineLearningModel;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.ClassificationObserver;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.MachineLearningAlgorithm;
import com.joliciel.talismane.utils.JolicielException;
import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.io.UnclosableWriter;
import com.typesafe.config.Config;

import de.bwaldvogel.liblinear.Model;
import gnu.trove.map.TObjectIntMap;

public class LinearSVMModel extends AbstractMachineLearningModel implements ClassificationModel {
  private static final Logger LOG = LoggerFactory.getLogger(LinearSVMModel.class);

  private Model model;
  private TObjectIntMap<String> featureIndexMap = null;
  private List<String> outcomes = null;
  private transient Set<String> outcomeNames = null;

  /**
   * Default constructor for factory.
   */
  public LinearSVMModel() {
  }

  /**
   * Construct from a newly trained model including the feature descriptors.
   */
  LinearSVMModel(Model model, Config config, Map<String, List<String>> descriptors) {
    super(config, descriptors);
    this.model = model;
  }

  @Override
  public DecisionMaker getDecisionMaker() {
    LinearSVMDecisionMaker decisionMaker = new LinearSVMDecisionMaker(model, this.featureIndexMap, this.outcomes);
    return decisionMaker;
  }

  @Override
  public ClassificationObserver getDetailedAnalysisObserver(File file) {
    throw new JolicielException("No detailed analysis observer currently available for linear SVM.");
  }

  @Override
  public void writeModelToStream(OutputStream outputStream) {
    try {
      Writer writer = new OutputStreamWriter(outputStream, "UTF-8");
      Writer unclosableWriter = new UnclosableWriter(writer);
      model.save(unclosableWriter);
    } catch (UnsupportedEncodingException e) {
      LogUtils.logError(LOG, e);
      throw new RuntimeException(e);
    } catch (IOException e) {
      LogUtils.logError(LOG, e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void loadModelFromStream(InputStream inputStream) throws IOException {
    // load model or use it directly
    Reader reader = new InputStreamReader(inputStream, "UTF-8");
    model = Model.load(reader);
  }

  @Override
  public MachineLearningAlgorithm getAlgorithm() {
    return MachineLearningAlgorithm.LinearSVM;
  }

  /**
   * A map of feature names to unique indexes.
   */
  public TObjectIntMap<String> getFeatureIndexMap() {
    return featureIndexMap;
  }

  public void setFeatureIndexMap(TObjectIntMap<String> featureIndexMap) {
    this.featureIndexMap = featureIndexMap;
  }

  /**
   * A list of outcomes, where the indexes are the ones used by the binary
   * model.
   */
  public List<String> getOutcomes() {
    return outcomes;
  }

  public void setOutcomes(List<String> outcomes) {
    this.outcomes = outcomes;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected boolean loadDataFromStream(InputStream inputStream, ZipEntry zipEntry) throws IOException, ClassNotFoundException {
    boolean loaded = true;
    if (zipEntry.getName().equals("featureIndexMap.obj")) {
      ObjectInputStream in = new ObjectInputStream(inputStream);
      featureIndexMap = (TObjectIntMap<String>) in.readObject();
    } else if (zipEntry.getName().equals("outcomes.obj")) {
      ObjectInputStream in = new ObjectInputStream(inputStream);
      outcomes = (List<String>) in.readObject();
    } else {
      loaded = false;
    }
    return loaded;
  }

  @Override
  public void writeDataToStream(ZipOutputStream zos) throws IOException {
    zos.putNextEntry(new ZipEntry("featureIndexMap.obj"));
    ObjectOutputStream out = new ObjectOutputStream(zos);

    try {
      out.writeObject(featureIndexMap);
    } finally {
      out.flush();
    }

    zos.flush();

    zos.putNextEntry(new ZipEntry("outcomes.obj"));
    out = new ObjectOutputStream(zos);
    try {
      out.writeObject(outcomes);
    } finally {
      out.flush();
    }

    zos.flush();

  }

  @Override
  public Set<String> getOutcomeNames() {
    if (this.outcomeNames == null) {
      this.outcomeNames = new TreeSet<String>(this.outcomes);
    }
    return this.outcomeNames;
  }

  @Override
  protected void persistOtherEntries(ZipOutputStream zos) throws IOException {
  }

  @Override
  public void onLoadComplete() {
  }

}
