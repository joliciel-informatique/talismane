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
package com.joliciel.talismane.posTagger;

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
import com.joliciel.talismane.posTagger.features.PosTaggerFeature;
import com.joliciel.talismane.posTagger.features.PosTaggerFeatureParser;
import com.joliciel.talismane.sentenceAnnotators.SentenceAnnotatorLoader;
import com.joliciel.talismane.utils.ConfigUtils;
import com.typesafe.config.Config;

/**
 * Reads various configuration parameters and enables building a pos-tagger
 * model.
 * 
 * @author Assaf Urieli
 *
 */
public class PosTaggerTrainer {
	private static final Logger LOG = LoggerFactory.getLogger(PosTaggerTrainer.class);

	private final TalismaneSession session;
	private final Config posTaggerConfig;
	private final File modelFile;
	private final ClassificationEventStream eventStream;
	private final Map<String, List<String>> descriptors;

	public PosTaggerTrainer(Reader reader, TalismaneSession session) throws IOException, ClassNotFoundException, ReflectiveOperationException {
		Config config = session.getConfig();
		this.posTaggerConfig = config.getConfig("talismane.core.pos-tagger");
		this.session = session;
		this.modelFile = new File(posTaggerConfig.getString("model"));
		this.descriptors = new HashMap<>();

		String configPath = "talismane.core.pos-tagger.train.features";
		InputStream tokeniserFeatureFile = ConfigUtils.getFileFromConfig(config, configPath);
		List<String> featureDescriptors = new ArrayList<>();
		try (Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(tokeniserFeatureFile, "UTF-8")))) {

			while (scanner.hasNextLine()) {
				String descriptor = scanner.nextLine();
				featureDescriptors.add(descriptor);
				LOG.debug(descriptor);
			}
		}
		descriptors.put(MachineLearningModel.FEATURE_DESCRIPTOR_KEY, featureDescriptors);
		PosTagAnnotatedCorpusReader corpusReader = PosTagAnnotatedCorpusReader.getCorpusReader(reader, posTaggerConfig.getConfig("train"), session);

		// add descriptors for various filters
		// these are for reference purpose only, as we no longer read filters
		// out of the model
		List<List<String>> sentenceAnnotatorDescriptors = session.getSentenceAnnotatorDescriptors();
		for (int i = 0; i < sentenceAnnotatorDescriptors.size(); i++) {
			descriptors.put(SentenceAnnotatorLoader.SENTENCE_ANNOTATOR_DESCRIPTOR_KEY + i, sentenceAnnotatorDescriptors.get(i));
		}

		PosTaggerFeatureParser featureParser = new PosTaggerFeatureParser(session);
		Set<PosTaggerFeature<?>> features = featureParser.getFeatureSet(featureDescriptors);
		eventStream = new PosTagEventStream(corpusReader, features);
	}

	public ClassificationModel train() throws TalismaneException, IOException {
		ModelTrainerFactory factory = new ModelTrainerFactory();
		ClassificationModelTrainer trainer = factory.constructTrainer(posTaggerConfig.getConfig("train.machine-learning"));

		ClassificationModel tokeniserModel = trainer.trainModel(eventStream, descriptors);
		tokeniserModel.setExternalResources(session.getExternalResourceFinder().getExternalResources());

		File modelDir = modelFile.getParentFile();
		modelDir.mkdirs();
		tokeniserModel.persist(modelFile);
		return tokeniserModel;
	}
}
