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
package com.joliciel.talismane.standoff;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.Talismane;
import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneService;
import com.joliciel.talismane.TalismaneServiceLocator;
import com.joliciel.talismane.fr.TalismaneFrench;
import com.joliciel.talismane.output.FreemarkerTemplateWriter;
import com.joliciel.talismane.parser.ParserRegexBasedCorpusReader;

public class Main {
	private static final Log LOG = LogFactory.getLog(Main.class);

	private enum Command {
		toStandoff,
		fromStandoff,
		splitConllFile
	}

	public static void main(String[] args) throws Exception {

		Command command = Command.toStandoff;

		Map<String,String> innerArgs = new HashMap<String, String>();
		for (String arg : args) {
			int equalsPos = arg.indexOf('=');
			String argName = arg.substring(0, equalsPos);
			String argValue = arg.substring(equalsPos+1);

			if (argName.equals("command"))
				command = Command.valueOf(argValue);
			else if (arg.length()>0) {
				innerArgs.put(argName, argValue);
			}
		}

		long startTime = new Date().getTime();
		try {
			TalismaneServiceLocator talismaneServiceLocator = TalismaneServiceLocator.getInstance();
			TalismaneService talismaneService = talismaneServiceLocator.getTalismaneService();
			
			innerArgs.put("command", "process");
			innerArgs.put("module", "parse");
			

			if (command.equals(Command.toStandoff)) {
				// first the annotations
				TalismaneFrench talismaneFrench = new TalismaneFrench(innerArgs);
				
				ParserRegexBasedCorpusReader corpusReader = (ParserRegexBasedCorpusReader) talismaneFrench.getParserCorpusReader();
				corpusReader.setPredictTransitions(false);
				
				StandoffWriter standoffWriter = new StandoffWriter(talismaneFrench.getWriter());
				
				Talismane talismane = talismaneService.getTalismane();
				talismane.setParseConfigurationProcessor(standoffWriter);
				talismane.runCommand(talismaneFrench);
				
				// then the sentences
				String outFilePath = innerArgs.get("outFile");
				outFilePath = outFilePath.substring(0, outFilePath.lastIndexOf('.')) + ".txt";
				innerArgs.put("outFile", outFilePath);
				talismaneFrench = new TalismaneFrench(innerArgs);
				corpusReader = (ParserRegexBasedCorpusReader) talismaneFrench.getParserCorpusReader();
				corpusReader.setPredictTransitions(false);

				InputStream inputStream = StandoffWriter.class.getResourceAsStream("standoffSentences.ftl"); 
				Reader templateReader = new BufferedReader(new InputStreamReader(inputStream));
				FreemarkerTemplateWriter templateWriter = new FreemarkerTemplateWriter(talismaneFrench.getWriter(), templateReader);
				
				talismane.setParseConfigurationProcessor(templateWriter);
				talismane.runCommand(talismaneFrench);
				
			} else if (command.equals(Command.fromStandoff)) {
				TalismaneFrench talismaneFrench = new TalismaneFrench(innerArgs);
				
				Scanner scanner = new Scanner(talismaneFrench.getReader());
				StandoffReader standoffReader = new StandoffReader(scanner);
				talismaneFrench.setParserCorpusReader(standoffReader);
				
				Talismane talismane = talismaneService.getTalismane();
				talismane.runCommand(talismaneFrench);
			} else if (command.equals(Command.splitConllFile)) {
				String filePath = null;
				if (innerArgs.containsKey("inFile"))
					filePath = innerArgs.get("inFile");
				else
					throw new TalismaneException("Missing option: inFile");
				
				int startIndex = 1;
				if (innerArgs.containsKey("startIndex"))  {
					startIndex = Integer.parseInt(innerArgs.get("startIndex"));
				}
				
				String encoding = "UTF-8";
				if (innerArgs.containsKey("encoding"))
					encoding = innerArgs.get("encoding");
				ConllFileSplitter splitter = new ConllFileSplitter();
				splitter.split(filePath, startIndex, encoding);
			} else {
				throw new RuntimeException("Unknown command: " + command);
			}
		} finally {
			long endTime = new Date().getTime();
			long totalTime = endTime - startTime;
			LOG.info("Total time: " + totalTime);
		}
	}

}
