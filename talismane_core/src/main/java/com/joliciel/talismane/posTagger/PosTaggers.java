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
package com.joliciel.talismane.posTagger;

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

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.ClassificationObserver;
import com.joliciel.talismane.machineLearning.MachineLearningModelFactory;
import com.joliciel.talismane.posTagger.features.PosTaggerFeatureParser;
import com.joliciel.talismane.posTagger.features.PosTaggerRule;
import com.joliciel.talismane.utils.ArrayListNoNulls;
import com.joliciel.talismane.utils.ConfigUtils;
import com.typesafe.config.Config;

/**
 * A factory class for getting a pos-tagger from the configuration.
 * 
 * @author Assaf Urieli
 *
 */
public class PosTaggers {
	private static final Logger LOG = LoggerFactory.getLogger(PosTaggers.class);
	private static final Map<String, ClassificationModel> modelMap = new HashMap<>();
	private static final Map<String, PosTagger> posTaggerMap = new HashMap<>();

	public static PosTagger getPosTagger(TalismaneSession session) throws IOException {
		PosTagger posTagger = null;
		if (session.getSessionId() != null)
			posTagger = posTaggerMap.get(session.getSessionId());
		if (posTagger == null) {
			Config config = session.getConfig();
			Config posTaggerConfig = config.getConfig("talismane.core.pos-tagger");
			int beamWidth = posTaggerConfig.getInt("beam-width");
			boolean propagateTokeniserBeam = posTaggerConfig.getBoolean("propagate-tokeniser-beam");

			String configPath = "talismane.core.pos-tagger.model";
			String modelFilePath = config.getString(configPath);
			LOG.debug("Getting pos-tagger model from " + modelFilePath);
			ClassificationModel model = modelMap.get(modelFilePath);
			if (model == null) {
				InputStream tokeniserModelFile = ConfigUtils.getFileFromConfig(config, configPath);
				MachineLearningModelFactory factory = new MachineLearningModelFactory();
				model = factory.getClassificationModel(new ZipInputStream(tokeniserModelFile));
				modelMap.put(modelFilePath, model);
			}

			posTagger = new ForwardStatisticalPosTagger(model, beamWidth, propagateTokeniserBeam, session);

			boolean includeDetails = posTaggerConfig.getBoolean("output.include-details");
			if (includeDetails) {
				String detailsFilePath = session.getBaseName() + "_posTagger_details.txt";
				File detailsFile = new File(detailsFilePath);
				detailsFile.delete();
				ClassificationObserver observer = model.getDetailedAnalysisObserver(detailsFile);
				posTagger.addObserver(observer);
			}

			List<PosTaggerRule> posTaggerRules = new ArrayList<>();
			PosTaggerFeatureParser featureParser = new PosTaggerFeatureParser(session);

			configPath = "talismane.core.pos-tagger.rules";
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
					List<PosTaggerRule> rules = featureParser.getRules(ruleDescriptors);
					posTaggerRules.addAll(rules);
				}
			}
			posTagger.setPosTaggerRules(posTaggerRules);

			if (session.getSessionId() != null)
				posTaggerMap.put(session.getSessionId(), posTagger);
		}
		return posTagger.clonePosTagger();
	}
}
