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
package com.joliciel.talismane.fr;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.Talismane;
import com.joliciel.talismane.Talismane.Command;
import com.joliciel.talismane.TalismaneConfig;
import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneService;
import com.joliciel.talismane.TalismaneServiceLocator;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.extensions.Extensions;
import com.joliciel.talismane.fr.ftb.TreebankReader;
import com.joliciel.talismane.fr.ftb.TreebankServiceLocator;
import com.joliciel.talismane.fr.ftb.export.FtbPosTagMapper;
import com.joliciel.talismane.fr.ftb.export.TreebankExportService;
import com.joliciel.talismane.fr.ftb.upload.TreebankUploadService;
import com.joliciel.talismane.fr.tokeniser.filters.EmptyTokenAfterDuFilter;
import com.joliciel.talismane.fr.tokeniser.filters.EmptyTokenBeforeDuquelFilter;
import com.joliciel.talismane.parser.ParserRegexBasedCorpusReader;
import com.joliciel.talismane.posTagger.PosTagAnnotatedCorpusReader;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorAnnotatedCorpusReader;
import com.joliciel.talismane.tokeniser.TokeniserAnnotatedCorpusReader;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilterFactory;
import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.StringUtils;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * The default French implementation of Talismane.
 * 
 * @author Assaf Urieli
 *
 */
public class TalismaneFrench {
	private static final Logger LOG = LoggerFactory.getLogger(TalismaneFrench.class);

	private enum CorpusFormat {
		/** CoNLL-X format */
		conll,
		/** French Treebank XML reader */
		ftb,
		/** French Treebank converted to dependencies */
		ftbDep,
		/** SPMRL format */
		spmrl
	}

	public static void main(String[] args) throws Exception {
		Map<String, String> argsMap = StringUtils.convertArgs(args);
		CorpusFormat corpusReaderType = null;
		String treebankDirPath = null;
		boolean keepCompoundPosTags = true;

		if (argsMap.containsKey("corpusReader")) {
			corpusReaderType = CorpusFormat.valueOf(argsMap.get("corpusReader"));
			argsMap.remove("corpusReader");
		}
		if (argsMap.containsKey("treebankDir")) {
			treebankDirPath = argsMap.get("treebankDir");
			argsMap.remove("treebankDir");
		}
		if (argsMap.containsKey("keepCompoundPosTags")) {
			keepCompoundPosTags = argsMap.get("keepCompoundPosTags").equalsIgnoreCase("true");
			argsMap.remove("keepCompoundPosTags");
		}

		String logConfigPath = argsMap.get("logConfigFile");
		argsMap.remove("logConfigFile");
		LogUtils.configureLogging(logConfigPath);

		Extensions extensions = new Extensions();
		extensions.pluckParameters(argsMap);

		String sessionId = "";
		TalismaneServiceLocator locator = TalismaneServiceLocator.getInstance(sessionId);
		TalismaneService talismaneService = locator.getTalismaneService();

		TokenSequenceFilterFactory tokenSequenceFilterFactory = TokenSequenceFilterFactory.getInstance(talismaneService.getTalismaneSession());
		tokenSequenceFilterFactory.getAvailableTokenSequenceFilters().add(EmptyTokenAfterDuFilter.class);
		tokenSequenceFilterFactory.getAvailableTokenSequenceFilters().add(EmptyTokenBeforeDuquelFilter.class);

		Map<String, Object> defaultConfigParams = new HashMap<>();
		defaultConfigParams.put("talismane.core.locale", "fr");

		Config conf = ConfigFactory.load().withFallback(ConfigFactory.parseMap(defaultConfigParams));

		TalismaneConfig config = talismaneService.getTalismaneConfig(conf, argsMap);
		if (config.getCommand() == null)
			return;

		TalismaneSession talismaneSession = talismaneService.getTalismaneSession();

		if (corpusReaderType != null) {
			if (corpusReaderType == CorpusFormat.ftbDep) {
				FtbDepReader ftbDepReader = new FtbDepReader(config.getReader(), talismaneSession);

				ftbDepReader.setKeepCompoundPosTags(keepCompoundPosTags);
				ftbDepReader.setPredictTransitions(config.isPredictTransitions());

				config.setParserCorpusReader(ftbDepReader);
				config.setPosTagCorpusReader(ftbDepReader);
				config.setTokenCorpusReader(ftbDepReader);
				config.setSentenceCorpusReader(ftbDepReader);

				if (config.getCommand().equals(Command.compare)) {
					FtbDepReader ftbDepEvaluationReader = new FtbDepReader(config.getEvaluationReader(), talismaneSession);
					ftbDepEvaluationReader.setKeepCompoundPosTags(keepCompoundPosTags);
					config.setParserEvaluationCorpusReader(ftbDepEvaluationReader);
					config.setPosTagEvaluationCorpusReader(ftbDepEvaluationReader);
				}
			} else if (corpusReaderType == CorpusFormat.ftb) {
				TreebankServiceLocator treebankServiceLocator = TreebankServiceLocator.getInstance(locator);
				TreebankUploadService treebankUploadService = treebankServiceLocator.getTreebankUploadServiceLocator().getTreebankUploadService();
				TreebankExportService treebankExportService = treebankServiceLocator.getTreebankExportServiceLocator().getTreebankExportService();
				File treebankFile = new File(treebankDirPath);
				TreebankReader treebankReader = treebankUploadService.getXmlReader(treebankFile);

				// we prepare both the tokeniser and pos-tag readers, just in
				// case they are needed
				List<String> descriptors = null;
				try (InputStream posTagMapStream = TalismaneFrench.getFtbPosTagMapFromStream(); Scanner scanner = new Scanner(posTagMapStream, "UTF-8")) {
					descriptors = new ArrayList<String>();
					while (scanner.hasNextLine())
						descriptors.add(scanner.nextLine());
				}

				FtbPosTagMapper ftbPosTagMapper = treebankExportService.getFtbPosTagMapper(descriptors, talismaneService.getTalismaneSession().getPosTagSet());
				PosTagAnnotatedCorpusReader posTagAnnotatedCorpusReader = treebankExportService.getPosTagAnnotatedCorpusReader(treebankReader, ftbPosTagMapper,
						keepCompoundPosTags);
				config.setPosTagCorpusReader(posTagAnnotatedCorpusReader);

				TokeniserAnnotatedCorpusReader tokenCorpusReader = treebankExportService.getTokeniserAnnotatedCorpusReader(treebankReader, ftbPosTagMapper,
						keepCompoundPosTags);
				config.setTokenCorpusReader(tokenCorpusReader);

				SentenceDetectorAnnotatedCorpusReader sentenceCorpusReader = treebankExportService.getSentenceDetectorAnnotatedCorpusReader(treebankReader);
				config.setSentenceCorpusReader(sentenceCorpusReader);
			} else if (corpusReaderType == CorpusFormat.conll || corpusReaderType == CorpusFormat.spmrl) {
				String regex = config.getParserReaderRegex();
				if (corpusReaderType == CorpusFormat.spmrl) {
					regex = "%INDEX%\\t%TOKEN%\\t.*\\t.*\\t%POSTAG%\\t.*\\t%NON_PROJ_GOVERNOR%\\t%NON_PROJ_LABEL%\\t%GOVERNOR%\\t%LABEL%";
				}

				ParserRegexBasedCorpusReader corpusReader = new ParserRegexBasedCorpusReader(regex, config.getReader(), talismaneSession);

				corpusReader.setPredictTransitions(config.isPredictTransitions());

				config.setParserCorpusReader(corpusReader);
				config.setPosTagCorpusReader(corpusReader);
				config.setTokenCorpusReader(corpusReader);
				config.setSentenceCorpusReader(corpusReader);

				if (config.getCommand().equals(Command.compare)) {
					String evalRegex = config.getParserEvluationReaderRegex();
					if (corpusReaderType == CorpusFormat.spmrl) {
						evalRegex = "%INDEX%\\t%TOKEN%\\t.*\\t.*\\t%POSTAG%\\t.*\\t%NON_PROJ_GOVERNOR%\\t%NON_PROJ_LABEL%\\t%GOVERNOR%\\t%LABEL%";
					}

					ParserRegexBasedCorpusReader evaluationReader = new ParserRegexBasedCorpusReader(evalRegex, config.getEvaluationReader(), talismaneSession);
					config.setParserEvaluationCorpusReader(evaluationReader);
					config.setPosTagEvaluationCorpusReader(evaluationReader);
				}
			} else {
				throw new TalismaneException("Unknown corpusReader: " + corpusReaderType);
			}
		}
		Talismane talismane = config.getTalismane();

		extensions.prepareCommand(config, talismane);

		talismane.process();
	}

	private static InputStream getInputStreamFromResource(String resource) {
		String path = "resources/" + resource;
		LOG.debug("Getting " + path);
		InputStream inputStream = TalismaneFrench.class.getResourceAsStream(path);

		return inputStream;
	}

	public static InputStream getFtbPosTagMapFromStream() {
		InputStream inputStream = getInputStreamFromResource("ftbCrabbeCanditoTagsetMap.txt");
		return inputStream;
	}
}
