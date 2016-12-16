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
package com.joliciel.talismane.parser;

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
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.machineLearning.ClassificationEventStream;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.ClassificationModelTrainer;
import com.joliciel.talismane.machineLearning.MachineLearningModel;
import com.joliciel.talismane.machineLearning.ModelTrainerFactory;
import com.joliciel.talismane.parser.features.ParseConfigurationFeature;
import com.joliciel.talismane.parser.features.ParserFeatureParser;
import com.joliciel.talismane.sentenceAnnotators.SentenceAnnotatorFactory;
import com.joliciel.talismane.utils.ConfigUtils;
import com.typesafe.config.Config;

/**
 * Reads various configuration parameters and enables building a dependency
 * parser model.
 * 
 * @author Assaf Urieli
 *
 */
public class ParserTrainer {
	private static final Logger LOG = LoggerFactory.getLogger(ParserTrainer.class);

	private final TalismaneSession session;
	private final Config parserConfig;
	private final File modelFile;
	private final ClassificationEventStream eventStream;
	private final Map<String, List<String>> descriptors;

	public ParserTrainer(Reader reader, TalismaneSession session) throws IOException, ClassNotFoundException, ReflectiveOperationException {
		Config config = session.getConfig();
		this.parserConfig = config.getConfig("talismane.core.parser");
		this.session = session;
		this.modelFile = new File(parserConfig.getString("model"));
		this.descriptors = new HashMap<>();

		boolean dynamiseFeatures = parserConfig.getBoolean("dynamise-features");

		String configPath = "talismane.core.parser.train.features";
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
		ParserAnnotatedCorpusReader corpusReader = ParserAnnotatedCorpusReader.getCorpusReader(reader, parserConfig.getConfig("train"), session);

		// add descriptors for various filters
		// these are for reference purpose only, as we no longer read filters
		// out of the model
		descriptors.put(SentenceAnnotatorFactory.TOKEN_FILTER_DESCRIPTOR_KEY,
				session.getSentenceAnnotatorsWithDescriptors().stream().map(f -> f.getLeft()).collect(Collectors.toList()));

		ParserFeatureParser featureParser = new ParserFeatureParser(session, dynamiseFeatures);
		Set<ParseConfigurationFeature<?>> features = featureParser.getFeatures(featureDescriptors);
		eventStream = new ParseEventStream(corpusReader, features);
	}

	public ClassificationModel train() {
		ModelTrainerFactory factory = new ModelTrainerFactory();
		ClassificationModelTrainer trainer = factory.constructTrainer(parserConfig.getConfig("train.machine-learning"));

		ClassificationModel tokeniserModel = trainer.trainModel(eventStream, descriptors);
		tokeniserModel.setExternalResources(session.getExternalResourceFinder().getExternalResources());

		File modelDir = modelFile.getParentFile();
		modelDir.mkdirs();
		tokeniserModel.persist(modelFile);
		return tokeniserModel;
	}
}
