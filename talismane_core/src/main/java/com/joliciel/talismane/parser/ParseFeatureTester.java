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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.parser.features.ParseConfigurationFeature;
import com.joliciel.talismane.parser.features.ParserFeatureParser;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.utils.ConfigUtils;
import com.joliciel.talismane.utils.LogUtils;
import com.typesafe.config.Config;

public class ParseFeatureTester implements ParseConfigurationProcessor {
	private static final Logger LOG = LoggerFactory.getLogger(ParseFeatureTester.class);

	private final Set<ParseConfigurationFeature<?>> parseFeatures;

	private final Map<String, Map<String, List<String>>> featureResultMap = new TreeMap<>();
	private final Writer writer;

	public ParseFeatureTester(TalismaneSession session, Writer writer) throws IOException {
		Config config = session.getConfig();
		Config parserConfig = config.getConfig("talismane.core.parser");

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

		ParserFeatureParser featureParser = new ParserFeatureParser(session, dynamiseFeatures);
		this.parseFeatures = featureParser.getFeatures(featureDescriptors);
		this.writer = writer;
	}

	public ParseFeatureTester(Set<ParseConfigurationFeature<?>> parseFeatures, Writer writer) {
		this.parseFeatures = parseFeatures;
		this.writer = writer;
	}

	@Override
	public void onNextParseConfiguration(ParseConfiguration parseConfiguration) {

		ParseConfiguration currentConfiguration = new ParseConfiguration(parseConfiguration.getPosTagSequence());

		for (Transition transition : parseConfiguration.getTransitions()) {
			StringBuilder sb = new StringBuilder();
			for (PosTaggedToken taggedToken : currentConfiguration.getPosTagSequence()) {
				if (taggedToken.equals(currentConfiguration.getStack().getFirst())) {
					sb.append(" #[" + taggedToken.getToken().getOriginalText().replace(' ', '_') + "/" + taggedToken.getTag().toString() + "]#");
				} else if (taggedToken.equals(currentConfiguration.getBuffer().getFirst())) {
					sb.append(" #[" + taggedToken.getToken().getOriginalText().replace(' ', '_') + "/" + taggedToken.getTag().toString() + "]#");
				} else {
					sb.append(" " + taggedToken.getToken().getOriginalText().replace(' ', '_') + "/" + taggedToken.getTag().toString());
				}
			}

			sb.append(" ## Line: " + parseConfiguration.getSentence().getStartLineNumber());

			if (LOG.isTraceEnabled())
				LOG.trace(sb.toString());

			List<FeatureResult<?>> parseFeatureResults = new ArrayList<FeatureResult<?>>();
			for (ParseConfigurationFeature<?> parseFeature : parseFeatures) {
				RuntimeEnvironment env = new RuntimeEnvironment();
				FeatureResult<?> featureResult = parseFeature.check(currentConfiguration, env);
				if (featureResult != null) {
					parseFeatureResults.add(featureResult);
					if (LOG.isTraceEnabled()) {
						LOG.trace(featureResult.toString());
					}
				}
			}

			String classification = transition.getCode();

			for (FeatureResult<?> featureResult : parseFeatureResults) {
				Map<String, List<String>> classificationMap = featureResultMap.get(featureResult.toString());
				if (classificationMap == null) {
					classificationMap = new TreeMap<String, List<String>>();
					featureResultMap.put(featureResult.toString(), classificationMap);
				}
				List<String> sentences = classificationMap.get(classification);
				if (sentences == null) {
					sentences = new ArrayList<String>();
					classificationMap.put(classification, sentences);
				}
				sentences.add(sb.toString());
			}

			// apply the transition and up the index
			currentConfiguration = new ParseConfiguration(currentConfiguration);
			transition.apply(currentConfiguration);
		}
	}

	@Override
	public void onCompleteParse() {
		try {
			for (String featureResult : this.featureResultMap.keySet()) {
				writer.write("###################\n");
				writer.write(featureResult + "\n");
				int totalCount = 0;
				Map<String, List<String>> classificationMap = featureResultMap.get(featureResult);
				for (String classification : classificationMap.keySet()) {
					totalCount += classificationMap.get(classification).size();
				}
				writer.write("Total count: " + totalCount + "\n");
				for (String classification : classificationMap.keySet()) {
					writer.write(classification + " count:" + classificationMap.get(classification).size() + "\n");
				}
				for (String classification : classificationMap.keySet()) {
					writer.write("Transition: " + classification + "\t" + classificationMap.get(classification).size() + "\n");
					for (String sentence : classificationMap.get(classification)) {
						writer.write(sentence + "\n");
					}
				}
				writer.flush();
			}
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close() throws IOException {
		writer.close();
	}
}
