///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2016 Joliciel Informatique
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.ClassificationObserver;
import com.joliciel.talismane.machineLearning.MachineLearningModelFactory;
import com.joliciel.talismane.parser.Parser.ParseComparisonStrategyType;
import com.joliciel.talismane.parser.features.ParserFeatureParser;
import com.joliciel.talismane.parser.features.ParserRule;
import com.joliciel.talismane.utils.ArrayListNoNulls;
import com.joliciel.talismane.utils.ConfigUtils;
import com.typesafe.config.Config;

/**
 * A factory class for getting a dependency parser from the configuration.
 * 
 * @author Assaf Urieli
 *
 */
public class Parsers {
	private static final Logger LOG = LoggerFactory.getLogger(Parsers.class);
	private static final Map<String, ClassificationModel> modelMap = new HashMap<>();
	private static final Map<String, Parser> parserMap = new HashMap<>();

	public static Parser getParser(TalismaneSession session) throws IOException, TalismaneException {
		Parser parser = null;
		if (session.getSessionId() != null)
			parser = parserMap.get(session.getSessionId());
		if (parser == null) {
			Config config = session.getConfig();
			Config parserConfig = config.getConfig("talismane.core.parser");

			String configPath = "talismane.core.parser.model";
			String modelFilePath = config.getString(configPath);
			LOG.debug("Getting parser model from " + modelFilePath);
			ClassificationModel model = modelMap.get(modelFilePath);
			if (model == null) {
				InputStream tokeniserModelFile = ConfigUtils.getFileFromConfig(config, configPath);
				MachineLearningModelFactory factory = new MachineLearningModelFactory();
				model = factory.getClassificationModel(new ZipInputStream(tokeniserModelFile));
				modelMap.put(modelFilePath, model);
			}

			int beamWidth = parserConfig.getInt("beam-width");
			boolean propagatePosTaggerBeam = parserConfig.getBoolean("propagate-pos-tagger-beam");

			ParseComparisonStrategyType parseComparisonStrategyType = ParseComparisonStrategyType.valueOf(parserConfig.getString("comparison-strategy"));
			ParseComparisonStrategy parseComparisonStrategy = ParseComparisonStrategy.forType(parseComparisonStrategyType);

			int maxAnalysisTimePerSentence = parserConfig.getInt("max-analysis-time");
			int minFreeMemory = parserConfig.getInt("min-free-memory");

			TransitionBasedParser transitionBasedParser = new TransitionBasedParser(model, beamWidth, propagatePosTaggerBeam, parseComparisonStrategy,
					maxAnalysisTimePerSentence, minFreeMemory, session);
			parser = transitionBasedParser;

			transitionBasedParser.setEarlyStop(parserConfig.getBoolean("early-stop"));
			parser.setParseComparisonStrategy(parseComparisonStrategy);

			boolean includeDetails = parserConfig.getBoolean("output.include-details");
			if (includeDetails) {
				String detailsFilePath = session.getBaseName() + "_posTagger_details.txt";
				File detailsFile = new File(detailsFilePath);
				detailsFile.delete();
				ClassificationObserver observer = model.getDetailedAnalysisObserver(detailsFile);
				parser.addObserver(observer);
			}

			List<ParserRule> parserRules = new ArrayList<>();
			ParserFeatureParser featureParser = new ParserFeatureParser(session);

			configPath = "talismane.core.parser.rules";
			List<String> textFilterPaths = config.getStringList(configPath);
			for (String path : textFilterPaths) {
				LOG.debug("From: " + path);
				InputStream textFilterFile = ConfigUtils.getFile(config, configPath, path);
				try (Scanner scanner = new Scanner(textFilterFile, session.getInputCharset().name())) {
					List<String> ruleDescriptors = new ArrayListNoNulls<String>();
					while (scanner.hasNextLine()) {
						String ruleDescriptor = scanner.nextLine();
						if (ruleDescriptor.length() > 0) {
							ruleDescriptors.add(ruleDescriptor);
							LOG.trace(ruleDescriptor);
						}
					}
					List<ParserRule> rules = featureParser.getRules(ruleDescriptors);
					parserRules.addAll(rules);
				}
			}
			parser.setParserRules(parserRules);

			if (session.getSessionId() != null)
				parserMap.put(session.getSessionId(), parser);
		}
		return parser.cloneParser();
	}
}
