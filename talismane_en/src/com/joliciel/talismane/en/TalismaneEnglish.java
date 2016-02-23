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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;

import com.joliciel.talismane.GenericLanguageImplementation;
import com.joliciel.talismane.Talismane;
import com.joliciel.talismane.TalismaneConfig;
import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneMain;
import com.joliciel.talismane.TalismaneService;
import com.joliciel.talismane.TalismaneServiceLocator;
import com.joliciel.talismane.Talismane.Command;
import com.joliciel.talismane.extensions.Extensions;
import com.joliciel.talismane.parser.ParserRegexBasedCorpusReader;
import com.joliciel.talismane.parser.TransitionSystem;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;
import com.joliciel.talismane.utils.StringUtils;

/**
 * The default English implementation of Talismane.
 * @author Assaf Urieli
 *
 */
public class TalismaneEnglish extends GenericLanguageImplementation {
	private static final Log LOG = LogFactory.getLog(TalismaneEnglish.class);
	private static final String DEFAULT_CONLL_REGEX = "%INDEX%\\t%TOKEN%\\t.*\\t%POSTAG%\\t.*\\t.*\\t.*\\t.*\\t%GOVERNOR%\\t%LABEL%";

	private List<Class<? extends TokenSequenceFilter>> availableTokenSequenceFilters;

	private enum CorpusFormat {
		/** Penn-To-Dependency CoNLL-X format */
		pennDep,
	}
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
    	Map<String,String> argsMap = StringUtils.convertArgs(args);
    	CorpusFormat corpusReaderType = null;
    	
    	if (argsMap.containsKey("corpusReader")) {
    		corpusReaderType = CorpusFormat.valueOf(argsMap.get("corpusReader"));
    		argsMap.remove("corpusReader");
    	}
    	
    	// Configure log4j
		String logConfigPath = argsMap.get("logConfigFile");
		if (logConfigPath!=null) {
			argsMap.remove("logConfigFile");
			Properties props = new Properties();
			props.load(new FileInputStream(logConfigPath));
			PropertyConfigurator.configure(props);
		} else {
			Properties props = new Properties();
			InputStream stream = TalismaneMain.class.getResourceAsStream("/com/joliciel/talismane/default-log4j.properties");
			props.load(stream);
			PropertyConfigurator.configure(props);
		}
    	
    	Extensions extensions = new Extensions();
    	extensions.pluckParameters(argsMap);
    	
    	String sessionId = "";
       	TalismaneServiceLocator locator = TalismaneServiceLocator.getInstance(sessionId);
       	TalismaneService talismaneService = locator.getTalismaneService();
    	TalismaneEnglish talismaneEnglish = new TalismaneEnglish(sessionId);

    	TalismaneConfig config = talismaneService.getTalismaneConfig(argsMap, talismaneEnglish);
    	if (config.getCommand()==null)
    		return;
    	
    	if (corpusReaderType!=null) {
    		if (corpusReaderType==CorpusFormat.pennDep) {
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
 				
				if (config.getInputRegex()!=null) {
					corpusReader.setRegex(config.getInputRegex());
				}
				
				if (config.getCommand().equals(Command.compare)) {
					File evaluationFile = new File(config.getEvaluationFilePath());
	    			ParserRegexBasedCorpusReader evaluationReader = config.getParserService().getRegexBasedCorpusReader(evaluationFile, config.getInputCharset());
		  			config.setParserEvaluationCorpusReader(evaluationReader);
					config.setPosTagEvaluationCorpusReader(evaluationReader);
					
					evaluationReader.setRegex(DEFAULT_CONLL_REGEX);
	 				
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

	public TalismaneEnglish(String sessionId) {
		super(sessionId);
	}

	private static InputStream getInputStreamFromResource(String resource) {
		String path = "/com/joliciel/talismane/en/resources/" + resource;
		LOG.debug("Getting " + path);
		InputStream inputStream = TalismaneEnglish.class.getResourceAsStream(path); 
		
		return inputStream;
	}
	
	@Override
	public TransitionSystem getDefaultTransitionSystem() {
		TransitionSystem transitionSystem = this.getParserService().getArcEagerTransitionSystem();
		InputStream inputStream = getInputStreamFromResource("pennDependencyLabels.txt");
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
		scanner.close();
		transitionSystem.setDependencyLabels(dependencyLabels);
		return transitionSystem;
	}

	@Override
	public PosTagSet getDefaultPosTagSet() {
		InputStream posTagInputStream = getInputStreamFromResource("pennTagset.txt");
		Scanner posTagSetScanner = new Scanner(posTagInputStream, "UTF-8");

		PosTagSet posTagSet = this.getPosTaggerService().getPosTagSet(posTagSetScanner);
		return posTagSet;
	}
	
	@Override
	public List<Class<? extends TokenSequenceFilter>> getAvailableTokenSequenceFilters() {
		if (availableTokenSequenceFilters==null) {
			availableTokenSequenceFilters = new ArrayList<Class<? extends TokenSequenceFilter>>();
		}

		return availableTokenSequenceFilters;
	}

	@Override
	public Locale getLocale() {
		return Locale.ENGLISH;
	}
}
