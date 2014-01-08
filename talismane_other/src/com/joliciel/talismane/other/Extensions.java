///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2013 Assaf Urieli
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
package com.joliciel.talismane.other;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.Talismane;
import com.joliciel.talismane.TalismaneConfig;
import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.other.corpus.CorpusProjectifier;
import com.joliciel.talismane.other.corpus.CorpusStatistics;
import com.joliciel.talismane.other.corpus.CorpusModifier;
import com.joliciel.talismane.other.standoff.StandoffReader;
import com.joliciel.talismane.other.standoff.StandoffWriter;
import com.joliciel.talismane.output.FreemarkerTemplateWriter;
import com.joliciel.talismane.parser.ParserRegexBasedCorpusReader;
import com.joliciel.talismane.utils.LogUtils;

public class Extensions {
	private static final Log LOG = LogFactory.getLog(Extensions.class);
	String referenceStatsPath = null;
	String corpusRulesPath = null;
	ExtendedCommand command = null;
	
	public enum ExtendedCommand {
		toStandoff,
		toStandoffSentences,
		fromStandoff,
		splitConllFile,
		corpusStatistics,
		modifyCorpus,
		projectify
	}

	/**
	 * To be called initially, so that any parameters specific to the extensions can be removed
	 * and/or replaced in the argument map.
	 * @param args
	 */
	public void pluckParameters(Map<String,String> args) {
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
    			args.put("module", "parse");
    		} catch (IllegalArgumentException iae)  {
    			// do nothing
    		}
    		
    	}
	}
	
	/**
	 * To be called just before running the Talismane command, to
	 * prepare anything specifically required for extensions to function correctly.
	 * @param config
	 * @param talismane
	 */
	public void prepareCommand(TalismaneConfig config, Talismane talismane) {
		try {
			if (command==null)
				return;
			
			if (command.equals(ExtendedCommand.toStandoff)) {
				StandoffWriter standoffWriter = new StandoffWriter();
				talismane.setParseConfigurationProcessor(standoffWriter);
			} else if (command.equals(ExtendedCommand.toStandoffSentences)) {
				InputStream inputStream = StandoffWriter.class.getResourceAsStream("standoffSentences.ftl"); 
				Reader templateReader = new BufferedReader(new InputStreamReader(inputStream));
				FreemarkerTemplateWriter templateWriter = new FreemarkerTemplateWriter(templateReader);
				
				talismane.setParseConfigurationProcessor(templateWriter);
			} else if (command.equals(ExtendedCommand.fromStandoff)) {			
				Scanner scanner = new Scanner(config.getReader());
				StandoffReader standoffReader = new StandoffReader(scanner);
				config.setParserCorpusReader(standoffReader);
			} else if (command.equals(ExtendedCommand.corpusStatistics)) {
				CorpusStatistics stats = new CorpusStatistics();
				
				if (referenceStatsPath!=null) {
					File referenceStatsFile = new File(referenceStatsPath);
					CorpusStatistics referenceStats = CorpusStatistics.loadFromFile(referenceStatsFile);
					stats.setReferenceWords(referenceStats.getWords());
					stats.setReferenceLowercaseWords(referenceStats.getLowerCaseWords());
				}
	
				File csvFile = new File(config.getOutDir(), config.getBaseName() + "_stats.csv");
				csvFile.delete();
				csvFile.createNewFile();
				Writer csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false),"UTF8"));
				stats.setWriter(csvFileWriter);
				
				File serializationFile = new File(config.getOutDir(), config.getBaseName() + "_stats.zip");
				serializationFile.delete();
				stats.setSerializationFile(serializationFile);
				
				ParserRegexBasedCorpusReader corpusReader = (ParserRegexBasedCorpusReader) config.getParserCorpusReader();
				corpusReader.setPredictTransitions(false);
				
				talismane.setParseConfigurationProcessor(stats);
			} else if (command.equals(ExtendedCommand.modifyCorpus)) {
				if (corpusRulesPath==null)
					throw new TalismaneException("corpusRules is required for modifyCorpus command");
				
				List<String> corpusRules = new ArrayList<String>();
				File corpusRulesFile = new File(corpusRulesPath);
				Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(corpusRulesFile), "UTF-8")));

				while (scanner.hasNextLine()) {
					corpusRules.add(scanner.nextLine());
				}
				CorpusModifier corpusModifier = new CorpusModifier(config.getParseConfigurationProcessor(), corpusRules);
				talismane.setParseConfigurationProcessor(corpusModifier);
				
			} else if (command.equals(ExtendedCommand.projectify)) {
				CorpusProjectifier projectifier = new CorpusProjectifier(config.getParseConfigurationProcessor());
				talismane.setParseConfigurationProcessor(projectifier);
			} else {
				throw new RuntimeException("Unknown command: " + command);
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
