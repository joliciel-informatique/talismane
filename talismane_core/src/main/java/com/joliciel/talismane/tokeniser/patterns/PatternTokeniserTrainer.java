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
package com.joliciel.talismane.tokeniser.patterns;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.machineLearning.ClassificationEventStream;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.ClassificationModelTrainer;
import com.joliciel.talismane.machineLearning.MachineLearningModel;
import com.joliciel.talismane.machineLearning.ModelTrainerFactory;
import com.joliciel.talismane.tokeniser.TokeniserAnnotatedCorpusReader;
import com.joliciel.talismane.tokeniser.features.TokenPatternMatchFeature;
import com.joliciel.talismane.tokeniser.features.TokenPatternMatchFeatureParser;
import com.joliciel.talismane.tokeniser.features.TokeniserContextFeature;
import com.joliciel.talismane.tokeniser.features.TokeniserContextFeatureParser;
import com.joliciel.talismane.tokeniser.patterns.PatternTokeniserFactory.PatternTokeniserType;
import com.joliciel.talismane.utils.ConfigUtils;
import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.StringUtils;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * Reads various configuration parameters and enables building a pattern
 * tokeniser model.
 * 
 * @author Assaf Urieli
 *
 */
public class PatternTokeniserTrainer {
	private static final Logger LOG = LoggerFactory.getLogger(PatternTokeniserTrainer.class);

	private final TalismaneSession talismaneSession;
	private final Config tokeniserConfig;
	private final File modelFile;
	private final PatternTokeniserType patternTokeniserType;
	private final ClassificationEventStream eventStream;
	private final Map<String, List<String>> descriptors;

	public PatternTokeniserTrainer(Config config, TalismaneSession talismaneSession) throws IOException {
		this.tokeniserConfig = config.getConfig("talismane.core.tokeniser");
		this.talismaneSession = talismaneSession;
		this.modelFile = new File(tokeniserConfig.getString("model"));
		this.patternTokeniserType = PatternTokeniserType.valueOf(tokeniserConfig.getString("pattern-tokeniser-type"));

		this.descriptors = new HashMap<>();

		String configPath = "talismane.core.tokeniser.train.patterns";
		InputStream tokeniserPatternFile = ConfigUtils.getFileFromConfig(config, configPath);
		List<String> patternDescriptors = new ArrayList<>();
		try (Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(tokeniserPatternFile, "UTF-8")))) {
			while (scanner.hasNextLine()) {
				String descriptor = scanner.nextLine();
				patternDescriptors.add(descriptor);
				LOG.debug(descriptor);
			}
		}

		descriptors.put(PatternTokeniserFactory.PATTERN_DESCRIPTOR_KEY, patternDescriptors);

		TokeniserPatternManager tokeniserPatternManager = new TokeniserPatternManager(patternDescriptors);

		configPath = "talismane.core.tokeniser.train.features";
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
		TokeniserAnnotatedCorpusReader tokenCorpusReader = TokeniserAnnotatedCorpusReader.getCorpusReader(talismaneSession.getTrainingReader(),
				tokeniserConfig.getConfig("train"), talismaneSession);
		if (patternTokeniserType == PatternTokeniserType.Interval) {
			TokeniserContextFeatureParser featureParser = new TokeniserContextFeatureParser(talismaneSession, tokeniserPatternManager.getParsedTestPatterns());
			Set<TokeniserContextFeature<?>> features = featureParser.getTokeniserContextFeatureSet(featureDescriptors);
			eventStream = new IntervalPatternEventStream(tokenCorpusReader, features, tokeniserPatternManager, this.talismaneSession);
		} else {
			TokenPatternMatchFeatureParser featureParser = new TokenPatternMatchFeatureParser(talismaneSession);
			Set<TokenPatternMatchFeature<?>> features = featureParser.getTokenPatternMatchFeatureSet(featureDescriptors);
			eventStream = new CompoundPatternEventStream(tokenCorpusReader, features, tokeniserPatternManager, this.talismaneSession);
		}
	}

	public ClassificationModel trainModel() {
		ModelTrainerFactory factory = new ModelTrainerFactory();
		ClassificationModelTrainer trainer = factory.constructTrainer(tokeniserConfig.getConfig("train.machine-learning"));

		ClassificationModel tokeniserModel = trainer.trainModel(eventStream, descriptors);
		tokeniserModel.setExternalResources(talismaneSession.getExternalResourceFinder().getExternalResources());
		tokeniserModel.getModelAttributes().put(PatternTokeniserType.class.getSimpleName(), patternTokeniserType.toString());

		File modelDir = modelFile.getParentFile();
		modelDir.mkdirs();
		tokeniserModel.persist(modelFile);
		return tokeniserModel;
	}

	public static void main(String[] args) throws Exception {
		Map<String, String> argsMap = StringUtils.convertArgs(args);

		String logConfigPath = argsMap.get("logConfigFile");
		argsMap.remove("logConfigFile");
		LogUtils.configureLogging(logConfigPath);

		Config config = ConfigFactory.load();
		String sessionId = "";
		TalismaneSession talismaneSession = new TalismaneSession(config, sessionId);

		PatternTokeniserTrainer trainer = new PatternTokeniserTrainer(config, talismaneSession);
		trainer.trainModel();
	}
}
