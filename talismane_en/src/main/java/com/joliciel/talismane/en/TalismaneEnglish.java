///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2011 Assaf Urieli
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
package com.joliciel.talismane.en;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.Talismane;
import com.joliciel.talismane.Talismane.Command;
import com.joliciel.talismane.TalismaneConfig;
import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneService;
import com.joliciel.talismane.TalismaneServiceLocator;
import com.joliciel.talismane.extensions.Extensions;
import com.joliciel.talismane.parser.ParserRegexBasedCorpusReader;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;
import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.StringUtils;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * The default English implementation of Talismane.
 * 
 * @author Assaf Urieli
 *
 */
public class TalismaneEnglish {
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(TalismaneEnglish.class);
	private static final String DEFAULT_CONLL_REGEX = "%INDEX%\\t%TOKEN%\\t.*\\t%POSTAG%\\t.*\\t.*\\t.*\\t.*\\t%GOVERNOR%\\t%LABEL%";

	private List<Class<? extends TokenSequenceFilter>> availableTokenSequenceFilters;

	private enum CorpusFormat {
		/** Penn-To-Dependency CoNLL-X format */
		pennDep,
	}

	public static void main(String[] args) throws Exception {
		Map<String, String> argsMap = StringUtils.convertArgs(args);
		CorpusFormat corpusReaderType = null;

		if (argsMap.containsKey("corpusReader")) {
			corpusReaderType = CorpusFormat.valueOf(argsMap.get("corpusReader"));
			argsMap.remove("corpusReader");
		}

		String logConfigPath = argsMap.get("logConfigFile");
		argsMap.remove("logConfigFile");
		LogUtils.configureLogging(logConfigPath);

		Extensions extensions = new Extensions();
		extensions.pluckParameters(argsMap);

		String sessionId = "";
		TalismaneServiceLocator locator = TalismaneServiceLocator.getInstance(sessionId);
		TalismaneService talismaneService = locator.getTalismaneService();

		Map<String, Object> defaultConfigParams = new HashMap<>();
		defaultConfigParams.put("talismane.core.locale", "en");

		Config conf = ConfigFactory.load().withFallback(ConfigFactory.parseMap(defaultConfigParams));
		TalismaneConfig config = talismaneService.getTalismaneConfig(conf, argsMap);
		if (config.getCommand() == null)
			return;

		if (corpusReaderType != null) {
			if (corpusReaderType == CorpusFormat.pennDep) {
				PennDepReader corpusReader = new PennDepReader(config.getReader());
				corpusReader.setParserService(config.getParserService());
				corpusReader.setPosTaggerService(config.getPosTaggerService());
				corpusReader.setTokeniserService(config.getTokeniserService());
				corpusReader.setTokenFilterService(config.getTokenFilterService());
				corpusReader.setTalismaneService(config.getTalismaneService());

				corpusReader.setPredictTransitions(config.isPredictTransitions());

				config.setParserCorpusReader(corpusReader);
				config.setPosTagCorpusReader(corpusReader);
				config.setTokenCorpusReader(corpusReader);
				config.setSentenceCorpusReader(corpusReader);

				corpusReader.setRegex(DEFAULT_CONLL_REGEX);

				if (config.getInputRegex() != null) {
					corpusReader.setRegex(config.getInputRegex());
				}

				if (config.getCommand().equals(Command.compare)) {
					ParserRegexBasedCorpusReader evaluationReader = config.getParserService().getRegexBasedCorpusReader(config.getEvaluationReader());
					config.setParserEvaluationCorpusReader(evaluationReader);
					config.setPosTagEvaluationCorpusReader(evaluationReader);

					evaluationReader.setRegex(DEFAULT_CONLL_REGEX);

					if (config.getInputRegex() != null) {
						evaluationReader.setRegex(config.getInputRegex());
					}
				}
			} else {
				throw new TalismaneException("Unknown corpusReader: " + corpusReaderType);
			}
		}
		Talismane talismane = config.getTalismane();

		extensions.prepareCommand(config, talismane);

		talismane.process();
	}
}
