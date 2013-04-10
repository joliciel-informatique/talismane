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
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.frenchTreebank.TreebankReader;
import com.joliciel.frenchTreebank.TreebankServiceLocator;
import com.joliciel.frenchTreebank.export.FtbPosTagMapper;
import com.joliciel.frenchTreebank.export.TreebankExportService;
import com.joliciel.frenchTreebank.upload.TreebankUploadService;
import com.joliciel.ftbDep.FtbDepReader;
import com.joliciel.talismane.Talismane;
import com.joliciel.talismane.TalismaneConfig;
import com.joliciel.talismane.TalismaneService;
import com.joliciel.talismane.TalismaneServiceLocator;
import com.joliciel.talismane.Talismane.Command;
import com.joliciel.talismane.lexicon.LexiconChain;
import com.joliciel.talismane.lexicon.LexiconDeserializer;
import com.joliciel.talismane.lexicon.PosTaggerLexicon;
import com.joliciel.talismane.parser.TransitionSystem;
import com.joliciel.talismane.posTagger.PosTagAnnotatedCorpusReader;
import com.joliciel.talismane.posTagger.filters.PosTagSequenceFilter;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorAnnotatedCorpusReader;
import com.joliciel.talismane.tokeniser.TokeniserAnnotatedCorpusReader;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;
import com.joliciel.talismane.tokeniser.filters.french.AllUppercaseFrenchFilter;
import com.joliciel.talismane.utils.LogUtils;

/**
 * The default French implementation of Talismane.
 * @author Assaf Urieli
 *
 */
public class TalismaneFrench extends TalismaneConfig {
	private static final Log LOG = LogFactory.getLog(TalismaneFrench.class);

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
    	Map<String,String> argsMap = TalismaneConfig.convertArgs(args);
    	String corpusReaderType = null;
    	String treebankDirPath = null;
		boolean keepCompoundPosTags = true;

    	if (argsMap.containsKey("corpusReader")) {
    		corpusReaderType = argsMap.get("corpusReader");
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
    	
    	TalismaneFrench config = new TalismaneFrench(argsMap);
    	if (config.getCommand()==null)
    		return;
    	
    	if (corpusReaderType!=null) {
    		if (corpusReaderType.equals("ftbDep")) {
    			File inputFile = new File(config.getInFilePath());
    			FtbDepReader ftbDepReader = new FtbDepReader(inputFile, config.getInputCharset());

    			ftbDepReader.setKeepCompoundPosTags(keepCompoundPosTags);
	  			config.setParserCorpusReader(ftbDepReader);
				config.setPosTagCorpusReader(ftbDepReader);
				config.setTokenCorpusReader(ftbDepReader);
				
				if (config.getCommand().equals(Command.compare)) {
					File evaluationFile = new File(config.getEvaluationFilePath());
					FtbDepReader ftbDepEvaluationReader = new FtbDepReader(evaluationFile, config.getInputCharset());
					ftbDepEvaluationReader.setKeepCompoundPosTags(keepCompoundPosTags);
		  			config.setParserEvaluationCorpusReader(ftbDepEvaluationReader);
					config.setPosTagEvaluationCorpusReader(ftbDepEvaluationReader);
				}
	  		} else if (corpusReaderType.equals("ftb")) {
	  			TalismaneServiceLocator talismaneServiceLocator = TalismaneServiceLocator.getInstance();
	  			TreebankServiceLocator treebankServiceLocator = TreebankServiceLocator.getInstance(talismaneServiceLocator);
	  			TreebankUploadService treebankUploadService = treebankServiceLocator.getTreebankUploadServiceLocator().getTreebankUploadService();
				TreebankExportService treebankExportService = treebankServiceLocator.getTreebankExportServiceLocator().getTreebankExportService();
	  			File treebankFile = new File(treebankDirPath);
				TreebankReader treebankReader = treebankUploadService.getXmlReader(treebankFile);
	  			
				// we prepare both the tokeniser and pos-tag readers, just in case they are needed
	  			InputStream posTagMapStream = config.getDefaultPosTagMapFromStream();
	  			Scanner scanner = new Scanner(posTagMapStream,"UTF-8");
	  			List<String> descriptors = new ArrayList<String>();
	  			while (scanner.hasNextLine())
	  				descriptors.add(scanner.nextLine());
				FtbPosTagMapper ftbPosTagMapper = treebankExportService.getFtbPosTagMapper(descriptors, config.getDefaultPosTagSet());
				PosTagAnnotatedCorpusReader posTagAnnotatedCorpusReader = treebankExportService.getPosTagAnnotatedCorpusReader(treebankReader, ftbPosTagMapper, keepCompoundPosTags);
				config.setPosTagCorpusReader(posTagAnnotatedCorpusReader);

				TokeniserAnnotatedCorpusReader tokenCorpusReader = treebankExportService.getTokeniserAnnotatedCorpusReader(treebankReader, keepCompoundPosTags);
  				config.setTokenCorpusReader(tokenCorpusReader);
  				
  				SentenceDetectorAnnotatedCorpusReader sentenceCorpusReader = treebankExportService.getSentenceDetectorAnnotatedCorpusReader(treebankReader);
  				config.setSentenceCorpusReader(sentenceCorpusReader);
	  		}
    	}
    	
    	TalismaneServiceLocator talismaneServiceLocator = TalismaneServiceLocator.getInstance();
    	TalismaneService talismaneService = talismaneServiceLocator.getTalismaneService();
    	Talismane talismane = talismaneService.getTalismane();
    	
    	talismane.runCommand(config);
	}

	public TalismaneFrench(Map<String, String> args) throws Exception {
		super(args);
	}

	public TalismaneFrench(String[] args) throws Exception {
		super(args);
	}

	private static ZipInputStream getZipInputStreamFromResource(String resource) {
		InputStream inputStream = getInputStreamFromResource(resource);
		ZipInputStream zis = new ZipInputStream(inputStream);
		
		return zis;
	}

	private static InputStream getInputStreamFromResource(String resource) {
		String path = "/com/joliciel/talismane/fr/resources/" + resource;
		LOG.debug("Getting " + path);
		InputStream inputStream = TalismaneFrench.class.getResourceAsStream(path); 
		
		return inputStream;
	}

	@Override
	public Scanner getDefaultPosTagSetScanner() {
		InputStream posTagInputStream = getInputStreamFromResource("CrabbeCanditoTagsetOriginal.txt");
		return new Scanner(posTagInputStream, "UTF-8");
	}
	

	@Override
	public Scanner getDefaultPosTaggerRulesScanner() {
		InputStream inputStream = getInputStreamFromResource("posTaggerConstraints_fr.txt");
		return new Scanner(inputStream, "UTF-8");
	}
	

	@Override
	public Scanner getDefaultParserRulesScanner() {
//		InputStream inputStream = getInputStreamFromResource("parserRules_fr.txt");
//		InputStream inputStream = null;
		return null;
	}


	@Override
	public PosTaggerLexicon getLexicon() {
		try {
			LexiconChain lexiconChain = new LexiconChain();
			
			LexiconDeserializer deserializer = new LexiconDeserializer();
			
			String lexiconPath = "/com/joliciel/talismane/fr/resources/lefff-2.1-additions.obj";
			ObjectInputStream ois = new ObjectInputStream(TalismaneFrench.class.getResourceAsStream(lexiconPath)); 
			PosTaggerLexicon lexicon = deserializer.deserializeLexiconFile(ois);
			lexiconChain.addLexicon(lexicon);
	
			lexiconPath = "/com/joliciel/talismane/fr/resources/lefff-2.1-specialCats-ftbDep.obj";
			ois = new ObjectInputStream(TalismaneFrench.class.getResourceAsStream(lexiconPath)); 
			lexicon = deserializer.deserializeLexiconFile(ois);
			lexiconChain.addLexicon(lexicon);
	
			lexiconPath = "/com/joliciel/talismane/fr/resources/lefffCC.zip";
			ZipInputStream zis = new ZipInputStream(TalismaneFrench.class.getResourceAsStream(lexiconPath)); 
			lexicon = deserializer.deserializeLexiconFile(zis);
			lexiconChain.addLexicon(lexicon);
	
			lexiconChain.setPosTagSet(this.getDefaultPosTagSet());
			return lexiconChain;
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}
	}

	@Override
	public ZipInputStream getDefaultSentenceModelStream() {
		String sentenceModelName = "ftbSentenceDetector_fr3.zip";
		return TalismaneFrench.getZipInputStreamFromResource(sentenceModelName);
	}

	@Override
	public ZipInputStream getDefaultTokeniserModelStream() {
		String tokeniserModelName = "ftbTokeniser_fr25_noCutoff.zip";
		return TalismaneFrench.getZipInputStreamFromResource(tokeniserModelName);
	}

	@Override
	public ZipInputStream getDefaultPosTaggerModelStream() {
		String posTaggerModelName = "ftbPosTagger_fr20_cutoff3.zip";
		return TalismaneFrench.getZipInputStreamFromResource(posTaggerModelName);
	}

	@Override
	public ZipInputStream getDefaultParserModelStream() {
		String parserModelName = "ftbDep_parser_arcEager_19_cutoff5.zip";
		return TalismaneFrench.getZipInputStreamFromResource(parserModelName);
	}

	@Override
	public List<TokenSequenceFilter> getTokenSequenceFilters() {
		List<TokenSequenceFilter> tokenFilters = new ArrayList<TokenSequenceFilter>();
		return tokenFilters;
	}

	@Override
	public List<TokenSequenceFilter> getDefaultPosTaggerPreProcessingFilters() {
		List<TokenSequenceFilter> tokenFilters = new ArrayList<TokenSequenceFilter>();
		tokenFilters.add(new AllUppercaseFrenchFilter());
		return tokenFilters;
	}

	@Override
	public Scanner getDefaultTextMarkerFiltersScanner() {
		InputStream inputStream = getInputStreamFromResource("text_marker_filters.txt");
		return new Scanner(inputStream, "UTF-8");
	}

	@Override
	public Scanner getDefaultTokenFiltersScanner() {
		InputStream inputStream = getInputStreamFromResource("token_filters.txt");
		return new Scanner(inputStream, "UTF-8");
	}
	
	public InputStream getDefaultPosTagMapFromStream() {
		InputStream inputStream = getInputStreamFromResource("ftbCrabbeCanditoTagsetMap.txt");
		return inputStream;
	}

	@Override
	public TransitionSystem getDefaultTransitionSystem() {
		return this.getParserService().getArcEagerTransitionSystem();
	}

	@Override
	public List<PosTagSequenceFilter> getPosTaggerPostProcessingFilters() {
		List<PosTagSequenceFilter> filters = new ArrayList<PosTagSequenceFilter>();
		return filters;
	}

}
