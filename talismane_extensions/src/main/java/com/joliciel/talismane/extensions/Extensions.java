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
package com.joliciel.talismane.extensions;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.Talismane;
import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.extensions.corpus.CorpusModifier;
import com.joliciel.talismane.extensions.corpus.CorpusProjectifier;
import com.joliciel.talismane.extensions.corpus.CorpusStatistics;
import com.joliciel.talismane.extensions.corpus.PosTaggerStatistics;
import com.joliciel.talismane.extensions.standoff.ConllFileSplitter;
import com.joliciel.talismane.extensions.standoff.StandoffReader;
import com.joliciel.talismane.extensions.standoff.StandoffWriter;
import com.joliciel.talismane.output.FreemarkerTemplateWriter;
import com.joliciel.talismane.parser.ParseConfigurationProcessor;
import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.StringUtils;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class Extensions {
	private static final Logger LOG = LoggerFactory.getLogger(Extensions.class);
	String referenceStatsPath = null;
	String corpusRulesPath = null;
	ExtendedCommand command = null;

	public enum ExtendedCommand {
		toStandoff,
		toStandoffSentences,
		fromStandoff,
		splitConllFile,
		corpusStatistics,
		posTaggerStatistics,
		modifyCorpus,
		projectify
	}

	public static void main(String[] args) throws Exception {
		Map<String, String> argsMap = StringUtils.convertArgs(args);

		Extensions extensions = new Extensions();
		extensions.pluckParameters(argsMap);

		boolean commandRun = extensions.runCommand(argsMap);
		if (!commandRun) {
			String sessionId = "";

			Config conf = ConfigFactory.load();
			TalismaneSession session = new TalismaneSession(conf, sessionId);

			Talismane talismane = new Talismane(session);

			extensions.prepareCommand(session, talismane);

			talismane.analyse();
		}
	}

	public boolean runCommand(Map<String, String> args) {
		boolean isRecognised = true;
		if (command == ExtendedCommand.splitConllFile) {
			ConllFileSplitter splitter = new ConllFileSplitter();
			splitter.process(args);
		} else {
			isRecognised = false;
		}
		return isRecognised;
	}

	/**
	 * To be called initially, so that any parameters specific to the extensions
	 * can be removed and/or replaced in the argument map.
	 */
	public void pluckParameters(Map<String, String> args) {
		if (args.containsKey("referenceStats")) {
			referenceStatsPath = args.get("referenceStats");
			args.remove("referenceStats");
		}
		if (args.containsKey("corpusRules")) {
			corpusRulesPath = args.get("corpusRules");
			args.remove("corpusRules");
		}

		if (args.containsKey("command")) {
			try {
				command = ExtendedCommand.valueOf(args.get("command"));
				args.remove("command");
				args.put("command", "process");
			} catch (IllegalArgumentException iae) {
				// do nothing
			}

		}
	}

	/**
	 * To be called just before running the Talismane command, to prepare
	 * anything specifically required for extensions to function correctly.
	 */
	public void prepareCommand(TalismaneSession session, Talismane talismane) {
		try {
			if (command == null)
				return;

			switch (command) {
			case toStandoff: {
				StandoffWriter standoffWriter = new StandoffWriter();
				talismane.setParseConfigurationProcessor(standoffWriter);
				break;
			}
			case toStandoffSentences: {
				InputStream inputStream = StandoffWriter.class.getResourceAsStream("standoffSentences.ftl");
				Reader templateReader = new BufferedReader(new InputStreamReader(inputStream));
				FreemarkerTemplateWriter templateWriter = new FreemarkerTemplateWriter(templateReader);

				talismane.setParseConfigurationProcessor(templateWriter);
				break;
			}
			case fromStandoff: {
				StandoffReader standoffReader = new StandoffReader(session.getReader(), session.getConfig(), session);
				// config.setParserCorpusReader(standoffReader);
				break;
			}
			case corpusStatistics: {
				CorpusStatistics stats = new CorpusStatistics(session);

				if (referenceStatsPath != null) {
					File referenceStatsFile = new File(referenceStatsPath);
					CorpusStatistics referenceStats = CorpusStatistics.loadFromFile(referenceStatsFile);
					stats.setReferenceWords(referenceStats.getWords());
					stats.setReferenceLowercaseWords(referenceStats.getLowerCaseWords());
				}

				File csvFile = new File(session.getOutDir(), session.getBaseName() + "_stats.csv");
				csvFile.delete();
				csvFile.createNewFile();
				Writer csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false), "UTF8"));
				stats.setWriter(csvFileWriter);

				File serializationFile = new File(session.getOutDir(), session.getBaseName() + "_stats.zip");
				serializationFile.delete();
				stats.setSerializationFile(serializationFile);

				talismane.setParseConfigurationProcessor(stats);
				break;
			}
			case posTaggerStatistics: {
				PosTaggerStatistics stats = new PosTaggerStatistics(session);

				if (referenceStatsPath != null) {
					File referenceStatsFile = new File(referenceStatsPath);
					PosTaggerStatistics referenceStats = PosTaggerStatistics.loadFromFile(referenceStatsFile);
					stats.setReferenceWords(referenceStats.getWords());
					stats.setReferenceLowercaseWords(referenceStats.getLowerCaseWords());
				}

				File csvFile = new File(session.getOutDir(), session.getBaseName() + "_stats.csv");
				csvFile.delete();
				csvFile.createNewFile();
				Writer csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false), "UTF8"));
				stats.setWriter(csvFileWriter);

				File serializationFile = new File(session.getOutDir(), session.getBaseName() + "_stats.zip");
				serializationFile.delete();
				stats.setSerializationFile(serializationFile);

				talismane.setPosTagSequenceProcessor(stats);
				break;
			}
			case modifyCorpus: {
				if (corpusRulesPath == null)
					throw new TalismaneException("corpusRules is required for modifyCorpus command");

				List<String> corpusRules = new ArrayList<String>();
				File corpusRulesFile = new File(corpusRulesPath);
				Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(corpusRulesFile), "UTF-8")));

				while (scanner.hasNextLine()) {
					corpusRules.add(scanner.nextLine());
				}
				scanner.close();
				CorpusModifier corpusModifier = new CorpusModifier(ParseConfigurationProcessor.getProcessor(session), corpusRules);
				talismane.setParseConfigurationProcessor(corpusModifier);
				break;
			}
			case projectify: {
				CorpusProjectifier projectifier = new CorpusProjectifier(ParseConfigurationProcessor.getProcessor(session));
				talismane.setParseConfigurationProcessor(projectifier);
				break;
			}
			default: {
				throw new RuntimeException("Unknown command: " + command);
			}
			}
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	public ExtendedCommand getCommand() {
		return command;
	}

}
