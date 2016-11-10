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
package com.joliciel.talismane;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.Talismane.Module;
import com.joliciel.talismane.lexicon.Diacriticizer;
import com.joliciel.talismane.lexicon.LexiconDeserializer;
import com.joliciel.talismane.lexicon.LexiconSerializer;
import com.joliciel.talismane.tokeniser.TokenComparator;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.TokenSequenceProcessor;
import com.joliciel.talismane.tokeniser.TokeniserAnnotatedCorpusReader;
import com.joliciel.talismane.tokeniser.TokeniserEvaluator;
import com.joliciel.talismane.tokeniser.patterns.PatternTokeniserTrainer;
import com.joliciel.talismane.utils.LogUtils;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

/**
 * Direct entry point for Talismane from the command line.
 * 
 * @author Assaf Urieli
 *
 */
public class TalismaneMain {
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(TalismaneMain.class);

	public static void main(String[] args) throws Exception {
		if (args.length > 0) {
			Set<String> argSet = new HashSet<>(Arrays.asList(args));
			if (argSet.contains("--serializeLexicon")) {
				LexiconSerializer.main(args);
				return;
			}
			if (argSet.contains("--testLexicon")) {
				LexiconDeserializer.main(args);
				return;
			}
			if (argSet.contains("--serializeDiacriticizer")) {
				Diacriticizer.main(args);
				return;
			}
			if (argSet.contains("--testDiacriticizer")) {
				Diacriticizer.main(args);
				return;
			}
		}

		OptionParser parser = new OptionParser();
		parser.accepts("analyse", "analyse text");
		parser.accepts("train", "train model").availableUnless("analyse");
		parser.accepts("evaluate", "evaluate annotated corpus").availableUnless("analyse", "train");
		parser.accepts("compare", "compare two annotated corpora").availableUnless("analyse", "train", "evaluate");
		parser.accepts("process", "process annotated corpus").availableUnless("analyse", "train", "evaluate", "compare");

		OptionSpec<Module> moduleOption = parser.accepts("module", Arrays.toString(Module.values())).availableIf("train", "evaluate", "compare", "process")
				.withRequiredArg().ofType(Module.class);
		OptionSpec<Module> startModuleOption = parser.accepts("startModule", "where to start analysis: " + Arrays.toString(Module.values()))
				.availableIf("analyse").withRequiredArg().ofType(Module.class);
		OptionSpec<Module> endModuleOption = parser.accepts("endModule", "where to end analysis: " + Arrays.toString(Module.values())).availableIf("analyse")
				.withRequiredArg().ofType(Module.class);

		OptionSpec<File> logConfigFileSpec = parser.accepts("logConfigFile", "logback configuration file").withRequiredArg().ofType(File.class);

		if (args.length == 0) {
			parser.printHelpOn(System.out);
			return;
		}

		OptionSet options = parser.parse(args);
		Map<String, Object> values = new HashMap<>();
		if (options.has("analyse"))
			values.put("talismane.core.command", "analyse");
		if (options.has("train"))
			values.put("talismane.core.command", "train");
		if (options.has("evaluate"))
			values.put("talismane.core.command", "evaluate");
		if (options.has("compare"))
			values.put("talismane.core.command", "compare");
		if (options.has("process"))
			values.put("talismane.core.command", "process");
		if (options.has(moduleOption))
			values.put("talismane.core.module", options.valueOf(moduleOption).name());
		if (options.has(startModuleOption))
			values.put("talismane.core.analysis.start-module", options.valueOf(startModuleOption).name());
		if (options.has(endModuleOption))
			values.put("talismane.core.analysis.end-module", options.valueOf(endModuleOption).name());
		if (options.has(logConfigFileSpec))
			LogUtils.configureLogging(options.valueOf(logConfigFileSpec));

		Config config = ConfigFactory.parseMap(values).withFallback(ConfigFactory.load());

		String sessionId = "";
		TalismaneSession session = new TalismaneSession(config, sessionId);

		switch (session.getCommand()) {
		case analyse: {
			TalismaneConfig talismaneConfig = new TalismaneConfig(config, session);
			Talismane talismane = talismaneConfig.getTalismane();
			talismane.process();

			break;
		}
		case train: {
			switch (session.getModule()) {
			case tokeniser: {
				PatternTokeniserTrainer trainer = new PatternTokeniserTrainer(session);
				trainer.train();
				break;
			}
			}
			break;
		}
		case evaluate: {
			switch (session.getModule()) {
			case tokeniser: {
				TokeniserEvaluator evaluator = new TokeniserEvaluator(session);
				evaluator.evaluate();
			}
			}
			break;
		}
		case compare: {
			switch (session.getModule()) {
			case tokeniser: {
				TokenComparator comparator = new TokenComparator(session);
				comparator.compare();
			}
			}
			break;
		}
		case process: {
			switch (session.getModule()) {
			case tokeniser: {
				TokenSequenceProcessor tokenSequenceProcessor = TokenSequenceProcessor.getProcessor(session);
				TokeniserAnnotatedCorpusReader tokenCorpusReader = TokeniserAnnotatedCorpusReader.getCorpusReader(session.getReader(),
						config.getConfig("talismane.core.tokeniser.input"), session);
				while (tokenCorpusReader.hasNextTokenSequence()) {
					TokenSequence tokenSequence = tokenCorpusReader.nextTokenSequence();
					tokenSequenceProcessor.onNextTokenSequence(tokenSequence, session.getWriter());
				}

				break;
			}
			}
			break;
		}
		}
	}
}
