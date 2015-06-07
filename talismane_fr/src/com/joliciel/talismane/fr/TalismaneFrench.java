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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.frenchTreebank.TreebankReader;
import com.joliciel.frenchTreebank.TreebankServiceLocator;
import com.joliciel.frenchTreebank.export.FtbPosTagMapper;
import com.joliciel.frenchTreebank.export.TreebankExportService;
import com.joliciel.frenchTreebank.upload.TreebankUploadService;

import com.joliciel.talismane.GenericLanguageImplementation;
import com.joliciel.talismane.Talismane;
import com.joliciel.talismane.TalismaneConfig;
import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneService;
import com.joliciel.talismane.TalismaneServiceLocator;
import com.joliciel.talismane.Talismane.Command;
import com.joliciel.talismane.extensions.Extensions;
import com.joliciel.talismane.fr.tokeniser.filters.AllUppercaseFrenchFilter;
import com.joliciel.talismane.fr.tokeniser.filters.EmptyTokenAfterDuFilter;
import com.joliciel.talismane.fr.tokeniser.filters.EmptyTokenBeforeDuquelFilter;
import com.joliciel.talismane.fr.tokeniser.filters.LowercaseFirstWordFrenchFilter;
import com.joliciel.talismane.fr.tokeniser.filters.UpperCaseSeriesFrenchFilter;
import com.joliciel.talismane.lexicon.LexicalEntryReader;
import com.joliciel.talismane.lexicon.RegexLexicalEntryReader;
import com.joliciel.talismane.parser.ParserRegexBasedCorpusReader;
import com.joliciel.talismane.parser.TransitionSystem;
import com.joliciel.talismane.posTagger.PosTagAnnotatedCorpusReader;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorAnnotatedCorpusReader;
import com.joliciel.talismane.tokeniser.TokeniserAnnotatedCorpusReader;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;
import com.joliciel.talismane.utils.StringUtils;

/**
 * The default French implementation of Talismane.
 * @author Assaf Urieli
 *
 */
public class TalismaneFrench extends GenericLanguageImplementation {
	private static final Log LOG = LogFactory.getLog(TalismaneFrench.class);
	private List<Class<? extends TokenSequenceFilter>> availableTokenSequenceFilters;

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
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
    	Map<String,String> argsMap = StringUtils.convertArgs(args);
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
    	
    	Extensions extensions = new Extensions();
    	extensions.pluckParameters(argsMap);
    	
    	String sessionId = "";
       	TalismaneServiceLocator locator = TalismaneServiceLocator.getInstance(sessionId);
       	TalismaneService talismaneService = locator.getTalismaneService();
    	TalismaneFrench talismaneFrench = new TalismaneFrench(sessionId);
    	
    	TalismaneConfig config = talismaneService.getTalismaneConfig(argsMap, talismaneFrench);
    	if (config.getCommand()==null)
    		return;
    	
    	if (corpusReaderType!=null) {
    		if (corpusReaderType==CorpusFormat.ftbDep) {
    			File inputFile = new File(config.getInFilePath());
    			FtbDepReader ftbDepReader = new FtbDepReader(inputFile, config.getInputCharset());
    			ftbDepReader.setParserService(config.getParserService());
    			ftbDepReader.setPosTaggerService(config.getPosTaggerService());
    			ftbDepReader.setTokeniserService(config.getTokeniserService());
    			ftbDepReader.setTokenFilterService(config.getTokenFilterService());
    			ftbDepReader.setTalismaneService(config.getTalismaneService());
    			
    			ftbDepReader.setKeepCompoundPosTags(keepCompoundPosTags);
    			ftbDepReader.setPredictTransitions(config.isPredictTransitions());
    			
	  			config.setParserCorpusReader(ftbDepReader);
				config.setPosTagCorpusReader(ftbDepReader);
				config.setTokenCorpusReader(ftbDepReader);
				config.setSentenceCorpusReader(ftbDepReader);
				
				if (config.getCommand().equals(Command.compare)) {
					File evaluationFile = new File(config.getEvaluationFilePath());
					FtbDepReader ftbDepEvaluationReader = new FtbDepReader(evaluationFile, config.getInputCharset());
					ftbDepEvaluationReader.setKeepCompoundPosTags(keepCompoundPosTags);
		  			config.setParserEvaluationCorpusReader(ftbDepEvaluationReader);
					config.setPosTagEvaluationCorpusReader(ftbDepEvaluationReader);
				}
	  		} else if (corpusReaderType==CorpusFormat.ftb) {
	  			TreebankServiceLocator treebankServiceLocator = TreebankServiceLocator.getInstance(locator);
	  			TreebankUploadService treebankUploadService = treebankServiceLocator.getTreebankUploadServiceLocator().getTreebankUploadService();
				TreebankExportService treebankExportService = treebankServiceLocator.getTreebankExportServiceLocator().getTreebankExportService();
	  			File treebankFile = new File(treebankDirPath);
				TreebankReader treebankReader = treebankUploadService.getXmlReader(treebankFile);
	  			
				// we prepare both the tokeniser and pos-tag readers, just in case they are needed
	  			InputStream posTagMapStream = talismaneFrench.getFtbPosTagMapFromStream();
	  			Scanner scanner = new Scanner(posTagMapStream,"UTF-8");
	  			List<String> descriptors = new ArrayList<String>();
	  			while (scanner.hasNextLine())
	  				descriptors.add(scanner.nextLine());
				FtbPosTagMapper ftbPosTagMapper = treebankExportService.getFtbPosTagMapper(descriptors, talismaneFrench.getDefaultPosTagSet());
				PosTagAnnotatedCorpusReader posTagAnnotatedCorpusReader = treebankExportService.getPosTagAnnotatedCorpusReader(treebankReader, ftbPosTagMapper, keepCompoundPosTags);
				config.setPosTagCorpusReader(posTagAnnotatedCorpusReader);

				TokeniserAnnotatedCorpusReader tokenCorpusReader = treebankExportService.getTokeniserAnnotatedCorpusReader(treebankReader, ftbPosTagMapper, keepCompoundPosTags);
  				config.setTokenCorpusReader(tokenCorpusReader);
  				
  				SentenceDetectorAnnotatedCorpusReader sentenceCorpusReader = treebankExportService.getSentenceDetectorAnnotatedCorpusReader(treebankReader);
  				config.setSentenceCorpusReader(sentenceCorpusReader);
	  		} else if (corpusReaderType==CorpusFormat.conll || corpusReaderType==CorpusFormat.spmrl) {
    			File inputFile = new File(config.getInFilePath());
    			
    			ParserRegexBasedCorpusReader corpusReader = config.getParserService().getRegexBasedCorpusReader(inputFile, config.getInputCharset());
    			
    			corpusReader.setPredictTransitions(config.isPredictTransitions());
    			
	  			config.setParserCorpusReader(corpusReader);
				config.setPosTagCorpusReader(corpusReader);
				config.setTokenCorpusReader(corpusReader);
				config.setSentenceCorpusReader(corpusReader);
				
				if (corpusReaderType==CorpusFormat.spmrl) {
					corpusReader.setRegex("%INDEX%\\t%TOKEN%\\t.*\\t.*\\t%POSTAG%\\t.*\\t%NON_PROJ_GOVERNOR%\\t%NON_PROJ_LABEL%\\t%GOVERNOR%\\t%LABEL%");
				}
 				
				if (config.getInputRegex()!=null) {
					corpusReader.setRegex(config.getInputRegex());
				}
				
				if (config.getCommand().equals(Command.compare)) {
					File evaluationFile = new File(config.getEvaluationFilePath());
	    			ParserRegexBasedCorpusReader evaluationReader = config.getParserService().getRegexBasedCorpusReader(evaluationFile, config.getInputCharset());
		  			config.setParserEvaluationCorpusReader(evaluationReader);
					config.setPosTagEvaluationCorpusReader(evaluationReader);
					
					if (corpusReaderType==CorpusFormat.spmrl) {
						evaluationReader.setRegex("%INDEX%\\t%TOKEN%\\t.*\\t.*\\t%POSTAG%\\t.*\\t.*\\t.*\\t%GOVERNOR%\\t%LABEL%");
					}
	 				
					if (config.getInputRegex()!=null) {
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

	public TalismaneFrench(String sessionId) {
		super(sessionId);
	}

	private static InputStream getInputStreamFromResource(String resource) {
		String path = "/com/joliciel/talismane/fr/resources/" + resource;
		LOG.debug("Getting " + path);
		InputStream inputStream = TalismaneFrench.class.getResourceAsStream(path); 
		
		return inputStream;
	}
	
	
	public InputStream getFtbPosTagMapFromStream() {
		InputStream inputStream = getInputStreamFromResource("ftbCrabbeCanditoTagsetMap.txt");
		return inputStream;
	}

	@Override
	public TransitionSystem getDefaultTransitionSystem() {
		TransitionSystem transitionSystem = this.getParserService().getArcEagerTransitionSystem();
		InputStream inputStream = getInputStreamFromResource("talismaneDependencyLabels.txt");
		Scanner scanner = new Scanner(inputStream, "UTF-8");
		Set<String> dependencyLabels = new HashSet<String>();
		while (scanner.hasNextLine()) {
			String dependencyLabel = scanner.nextLine();
			if (!dependencyLabel.startsWith("#")) {
				if (dependencyLabel.indexOf('\t')>0)
					dependencyLabel = dependencyLabel.substring(0, dependencyLabel.indexOf('\t'));
				dependencyLabels.add(dependencyLabel);
			}
		}
		transitionSystem.setDependencyLabels(dependencyLabels);
		return transitionSystem;
	}

	@Override
	public PosTagSet getDefaultPosTagSet() {
		InputStream posTagInputStream = getInputStreamFromResource("talismaneTagset.txt");
		Scanner posTagSetScanner =  new Scanner(posTagInputStream, "UTF-8");

		PosTagSet posTagSet = this.getPosTaggerService().getPosTagSet(posTagSetScanner);
		return posTagSet;
	}
	
	@Override
	public List<Class<? extends TokenSequenceFilter>> getAvailableTokenSequenceFilters() {
		if (availableTokenSequenceFilters==null) {
			availableTokenSequenceFilters = new ArrayList<Class<? extends TokenSequenceFilter>>();
			availableTokenSequenceFilters.add(EmptyTokenAfterDuFilter.class);
			availableTokenSequenceFilters.add(EmptyTokenBeforeDuquelFilter.class);
			availableTokenSequenceFilters.add(LowercaseFirstWordFrenchFilter.class);
			availableTokenSequenceFilters.add(UpperCaseSeriesFrenchFilter.class);
			availableTokenSequenceFilters.add(AllUppercaseFrenchFilter.class);
		}
		return availableTokenSequenceFilters;
	}
	
	@Override
	public LexicalEntryReader getDefaultCorpusLexicalEntryReader() {
		InputStream inputStream = getInputStreamFromResource("talismane_conll_morph_regex.txt");
		Scanner regexScanner = new Scanner(inputStream, "UTF-8");
		LexicalEntryReader reader = new RegexLexicalEntryReader(regexScanner);
		return reader;
	}

	@Override
	public Locale getLocale() {
		return Locale.FRENCH;
	}
}
