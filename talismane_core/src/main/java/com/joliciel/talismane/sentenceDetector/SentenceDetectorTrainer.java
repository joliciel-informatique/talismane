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
package com.joliciel.talismane.sentenceDetector;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.machineLearning.ClassificationEventStream;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.ClassificationModelTrainer;
import com.joliciel.talismane.machineLearning.MachineLearningModel;
import com.joliciel.talismane.machineLearning.ModelTrainerFactory;
import com.joliciel.talismane.sentenceDetector.features.SentenceDetectorFeature;
import com.joliciel.talismane.sentenceDetector.features.SentenceDetectorFeatureParser;
import com.joliciel.talismane.utils.ConfigUtils;
import com.typesafe.config.Config;

/**
 * Reads various configuration parameters and trains a sentence-detector model.
 * 
 * @author Assaf Urieli
 *
 */
public class SentenceDetectorTrainer {
  private static final Logger LOG = LoggerFactory.getLogger(SentenceDetectorTrainer.class);

  private final TalismaneSession session;
  private final Config sentenceConfig;
  private final File modelFile;
  private final ClassificationEventStream eventStream;
  private final Map<String, List<String>> descriptors;

  public SentenceDetectorTrainer(Reader reader, TalismaneSession session) throws IOException, ClassNotFoundException, ReflectiveOperationException {
    Config config = session.getConfig();
    this.sentenceConfig = config.getConfig("talismane.core.sentence-detector");
    this.session = session;
    this.modelFile = new File(sentenceConfig.getString("model"));

    this.descriptors = new HashMap<>();

    String configPath = "talismane.core.sentence-detector.train.features";
    InputStream featureFile = ConfigUtils.getFileFromConfig(config, configPath);
    List<String> featureDescriptors = new ArrayList<>();
    try (Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(featureFile, "UTF-8")))) {

      while (scanner.hasNextLine()) {
        String descriptor = scanner.nextLine();
        featureDescriptors.add(descriptor);
        LOG.debug(descriptor);
      }
    }
    descriptors.put(MachineLearningModel.FEATURE_DESCRIPTOR_KEY, featureDescriptors);
    SentenceDetectorAnnotatedCorpusReader corpusReader = SentenceDetectorAnnotatedCorpusReader.getCorpusReader(reader, sentenceConfig.getConfig("train"),
        session);

    SentenceDetectorFeatureParser featureParser = new SentenceDetectorFeatureParser(session);
    Set<SentenceDetectorFeature<?>> features = featureParser.getFeatureSet(featureDescriptors);
    eventStream = new SentenceDetectorEventStream(corpusReader, features, this.session);
  }

  public ClassificationModel train() throws TalismaneException, IOException {
    ModelTrainerFactory factory = new ModelTrainerFactory();
    ClassificationModelTrainer trainer = factory.constructTrainer(sentenceConfig.getConfig("train.machine-learning"));

    ClassificationModel model = trainer.trainModel(eventStream, descriptors);
    model.setExternalResources(session.getExternalResourceFinder().getExternalResources());

    File modelDir = modelFile.getParentFile();
    if (modelDir != null)
      modelDir.mkdirs();
    model.persist(modelFile);
    return model;
  }
}
