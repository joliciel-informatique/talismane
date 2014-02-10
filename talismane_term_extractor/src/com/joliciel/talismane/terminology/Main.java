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
package com.joliciel.talismane.terminology;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;

import com.joliciel.talismane.Talismane;
import com.joliciel.talismane.TalismaneConfig;
import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.fr.TalismaneFrench;
import com.joliciel.talismane.posTagger.PosTagSet;

public class Main {
	private static final Log LOG = LogFactory.getLog(Main.class);

	private enum Command {
		analyse,
		extract,
		list
	}

	public static void main(String[] args) throws Exception {
		String termFilePath = null;
		String outFilePath = null;
		Command command = Command.extract;
		int depth = -1;
		String databasePropertiesPath = null;
		String projectCode = null;

		
		Map<String, String> argMap = TalismaneConfig.convertArgs(args);

		String logConfigPath = argMap.get("logConfigFile");
		if (logConfigPath!=null) {
			argMap.remove("logConfigFile");
			Properties props = new Properties();
			props.load(new FileInputStream(logConfigPath));
			PropertyConfigurator.configure(props);
		}
		
		Map<String,String> innerArgs = new HashMap<String, String>();
		for (Entry<String, String> argEntry : argMap.entrySet()) {
			String argName = argEntry.getKey();
			String argValue = argEntry.getValue();

			if (argName.equals("command"))
				command = Command.valueOf(argValue);
			else if (argName.equals("termFile")) 
				termFilePath = argValue;
			else if (argName.equals("outFile")) 
				outFilePath = argValue;
			else if (argName.equals("depth"))
				depth = Integer.parseInt(argValue);
			else if (argName.equals("databaseProperties"))
				databasePropertiesPath = argValue;
			else if (argName.equals("projectCode"))
				projectCode = argValue;
			else
				innerArgs.put(argName, argValue);
		}
		if (termFilePath==null && databasePropertiesPath==null)
			throw new TalismaneException("Required argument: termFile or databasePropertiesPath");

		if (termFilePath!=null) {
			String currentDirPath = System.getProperty("user.dir");
			File termFileDir = new File(currentDirPath);
			if (termFilePath.lastIndexOf("/")>=0) {
				String termFileDirPath = termFilePath.substring(0, termFilePath.lastIndexOf("/"));
				termFileDir = new File(termFileDirPath);
				termFileDir.mkdirs();
			}
		}

		long startTime = new Date().getTime();
		try {
			TerminologyServiceLocator terminologyServiceLocator = TerminologyServiceLocator.getInstance();
			TerminologyService terminologyService = terminologyServiceLocator.getTerminologyService();
			TerminologyBase terminologyBase = null;
			
			if (projectCode==null)
				throw new TalismaneException("Required argument: projectCode");

			File file = new File(databasePropertiesPath);
			FileInputStream fis = new FileInputStream(file);
			Properties dataSourceProperties = new Properties();
			dataSourceProperties.load(fis);
			terminologyBase = terminologyService.getPostGresTerminologyBase(projectCode, dataSourceProperties);

			if (command.equals(Command.analyse)||command.equals(Command.extract)) {
				if (depth<0)
					throw new TalismaneException("Required argument: depth");

				if (command.equals(Command.analyse)) {
					innerArgs.put("command","analyse");
				} else {
					innerArgs.put("command", "process");
				}
				
				TalismaneFrench talismaneFrench = new TalismaneFrench();
				TalismaneConfig config = new TalismaneConfig(innerArgs, talismaneFrench);

				PosTagSet tagSet = TalismaneSession.getPosTagSet();
				Charset outputCharset = config.getOutputCharset();

				TermExtractor termExtractor = terminologyService.getTermExtractor(terminologyBase);
				termExtractor.setMaxDepth(depth);
				termExtractor.setOutFilePath(termFilePath);
				termExtractor.getIncludeChildren().add(tagSet.getPosTag("P"));
				termExtractor.getIncludeChildren().add(tagSet.getPosTag("P+D"));
				termExtractor.getIncludeChildren().add(tagSet.getPosTag("CC"));

				termExtractor.getIncludeWithParent().add(tagSet.getPosTag("DET"));

				if (outFilePath!=null) {
					if (outFilePath.lastIndexOf("/")>=0) {
						String outFileDirPath = outFilePath.substring(0, outFilePath.lastIndexOf("/"));
						File outFileDir = new File(outFileDirPath);
						outFileDir.mkdirs();
					}
					File outFile = new File(outFilePath);
					outFile.delete();
					outFile.createNewFile();

					Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFilePath), outputCharset));
					TermAnalysisWriter termAnalysisWriter = new TermAnalysisWriter(writer);
					termExtractor.addTermObserver(termAnalysisWriter);
				}

				Talismane talismane = config.getTalismane();
				talismane.setParseConfigurationProcessor(termExtractor);
				talismane.process();
			} else if (command.equals(Command.list)) {

				List<Term> terms = terminologyBase.getTermsByFrequency(2);
				for (Term term : terms) {
					LOG.debug("Term: " + term.getText());
					LOG.debug("Frequency: " + term.getFrequency());
					LOG.debug("Heads: " + term.getHeads());
					LOG.debug("Expansions: " + term.getExpansions());
					LOG.debug("Contexts: " + term.getContexts());
				}
			}
		} finally {
			long endTime = new Date().getTime();
			long totalTime = endTime - startTime;
			LOG.info("Total time: " + totalTime);
		}
	}

}
