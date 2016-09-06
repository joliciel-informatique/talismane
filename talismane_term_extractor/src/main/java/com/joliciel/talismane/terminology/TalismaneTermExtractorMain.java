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
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.Talismane;
import com.joliciel.talismane.TalismaneConfig;
import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneService;
import com.joliciel.talismane.TalismaneServiceLocator;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.terminology.TermExtractor.TerminologyProperty;
import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.StringUtils;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class TalismaneTermExtractorMain {
	private static final Logger LOG = LoggerFactory.getLogger(TalismaneTermExtractorMain.class);

	private enum Command {
		analyse, extract, list
	}

	public static void main(String[] args) throws Exception {
		String termFilePath = null;
		String outFilePath = null;
		Command command = Command.extract;
		int depth = -1;
		String databasePropertiesPath = null;
		String projectCode = null;
		String terminologyPropertiesPath = null;

		Map<String, String> argMap = StringUtils.convertArgs(args);

		String logConfigPath = argMap.get("logConfigFile");
		argMap.remove("logConfigFile");
		LogUtils.configureLogging(logConfigPath);

		Map<String, String> innerArgs = new HashMap<String, String>();
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
			else if (argName.equals("terminologyProperties"))
				terminologyPropertiesPath = argValue;
			else if (argName.equals("projectCode"))
				projectCode = argValue;
			else
				innerArgs.put(argName, argValue);
		}
		if (termFilePath == null && databasePropertiesPath == null)
			throw new TalismaneException("Required argument: termFile or databasePropertiesPath");

		if (termFilePath != null) {
			String currentDirPath = System.getProperty("user.dir");
			File termFileDir = new File(currentDirPath);
			if (termFilePath.lastIndexOf("/") >= 0) {
				String termFileDirPath = termFilePath.substring(0, termFilePath.lastIndexOf("/"));
				termFileDir = new File(termFileDirPath);
				termFileDir.mkdirs();
			}
		}

		long startTime = new Date().getTime();
		try {
			if (command.equals(Command.analyse)) {
				innerArgs.put("command", "analyse");
			} else {
				innerArgs.put("command", "process");
			}

			String sessionId = "";
			TalismaneServiceLocator locator = TalismaneServiceLocator.getInstance(sessionId);
			TalismaneService talismaneService = locator.getTalismaneService();

			String inputRegex = null;
			InputStream regexInputStream = getInputStreamFromResource("parser_conll_with_location_input_regex.txt");
			try (Scanner regexScanner = new Scanner(regexInputStream, "UTF-8")) {
				inputRegex = regexScanner.nextLine();
			}
			Map<String, Object> configValues = new HashMap<>();
			configValues.put("talismane.core.train.parser.readerRegex", inputRegex);

			Config conf = ConfigFactory.parseMap(configValues).withFallback(ConfigFactory.load());

			TalismaneConfig config = talismaneService.getTalismaneConfig(conf, innerArgs);

			TerminologyServiceLocator terminologyServiceLocator = TerminologyServiceLocator.getInstance(locator);
			TerminologyService terminologyService = terminologyServiceLocator.getTerminologyService();
			TerminologyBase terminologyBase = null;

			if (projectCode == null)
				throw new TalismaneException("Required argument: projectCode");

			File file = new File(databasePropertiesPath);
			FileInputStream fis = new FileInputStream(file);
			Properties dataSourceProperties = new Properties();
			dataSourceProperties.load(fis);
			terminologyBase = terminologyService.getPostGresTerminologyBase(projectCode, dataSourceProperties);

			TalismaneSession talismaneSession = talismaneService.getTalismaneSession();

			if (command.equals(Command.analyse) || command.equals(Command.extract)) {
				Locale locale = talismaneSession.getLocale();
				Map<TerminologyProperty, String> terminologyProperties = new HashMap<TerminologyProperty, String>();
				if (terminologyPropertiesPath != null) {
					Map<String, String> terminologyPropertiesStr = StringUtils.getArgMap(terminologyPropertiesPath);
					for (String key : terminologyPropertiesStr.keySet()) {
						try {
							TerminologyProperty property = TerminologyProperty.valueOf(key);
							terminologyProperties.put(property, terminologyPropertiesStr.get(key));
						} catch (IllegalArgumentException e) {
							throw new TalismaneException("Unknown terminology property: " + key);
						}
					}
				} else {
					terminologyProperties = getDefaultTerminologyProperties(locale);
				}
				if (depth <= 0 && !terminologyProperties.containsKey(TerminologyProperty.maxDepth))
					throw new TalismaneException("Required argument: depth");

				Charset outputCharset = config.getOutputCharset();

				TermExtractor termExtractor = terminologyService.getTermExtractor(terminologyBase, terminologyProperties);
				if (depth > 0)
					termExtractor.setMaxDepth(depth);
				termExtractor.setOutFilePath(termFilePath);

				if (outFilePath != null) {
					if (outFilePath.lastIndexOf("/") >= 0) {
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

				List<Term> terms = terminologyBase.findTerms(2, null, 0, null, null);
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

	public static Map<TerminologyProperty, String> getDefaultTerminologyProperties(Locale locale) {
		Map<TerminologyProperty, String> terminologyProperties = new HashMap<TerminologyProperty, String>();
		if (locale.getLanguage().equals("fr")) {
			terminologyProperties.put(TerminologyProperty.adjectivalTags, "ADJ,VPP");
			terminologyProperties.put(TerminologyProperty.coordinationLabels, "coord,dep_coord");
			terminologyProperties.put(TerminologyProperty.determinentTags, "DET");
			terminologyProperties.put(TerminologyProperty.nominalTags, "NC,NPP");
			terminologyProperties.put(TerminologyProperty.nonStandaloneIfHasDependents, "VPR");
			terminologyProperties.put(TerminologyProperty.nonStandaloneTags, "P,CC,CS,PONCT,P+D");
			terminologyProperties.put(TerminologyProperty.nonTopLevelLabels, "det,coord,dep_coord");
			terminologyProperties.put(TerminologyProperty.prepositionalTags, "P,P+D");
			terminologyProperties.put(TerminologyProperty.termStopTags, "V,VS,VIMP,PRO,P+PRO,PROREL,PROWH,PONCT");
			terminologyProperties.put(TerminologyProperty.zeroDepthLabels, "prep,det,coord,dep_coord");
			terminologyProperties.put(TerminologyProperty.lemmaGender, "m");
			terminologyProperties.put(TerminologyProperty.lemmaNumber, "s");
		} else {
			throw new TalismaneException("Terminology extraction properties unknown for language: " + locale.getLanguage());
		}
		return terminologyProperties;
	}

	public static InputStream getInputStreamFromResource(String resource) {
		String path = "resources/" + resource;
		LOG.debug("Getting " + path);
		InputStream inputStream = TalismaneTermExtractorMain.class.getResourceAsStream(path);

		return inputStream;
	}
}
