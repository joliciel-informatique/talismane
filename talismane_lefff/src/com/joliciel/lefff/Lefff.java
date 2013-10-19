///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
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
package com.joliciel.lefff;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.TalismaneServiceLocator;
import com.joliciel.talismane.lexicon.LexicalEntry;
import com.joliciel.talismane.lexicon.PredicateArgument;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.posTagger.PosTaggerService;
import com.joliciel.talismane.posTagger.PosTaggerServiceLocator;

public class Lefff {
	private static final Log LOG = LogFactory.getLog(Lefff.class);

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
		long startTime = (new Date()).getTime();
        String command = args[0];
       
        String memoryBaseFilePath = "";
        String lefffFilePath = "";
        String posTagSetPath = "";
        String posTagMapPath = "";
        String word = null;
        int startLine = -1;
        int stopLine = -1;
        
        boolean firstArg = true;
		for (String arg : args) {
			if (firstArg) {
				firstArg = false;
				continue;
			}
			int equalsPos = arg.indexOf('=');
			String argName = arg.substring(0, equalsPos);
			String argValue = arg.substring(equalsPos+1);
			if (argName.equals("memoryBase")) 
				memoryBaseFilePath = argValue;
			else if (argName.equals("lefffFile")) 
				lefffFilePath = argValue;
			else if (argName.equals("startLine")) 
				startLine = Integer.parseInt(argValue);
			else if (argName.equals("stopLine")) 
				stopLine = Integer.parseInt(argValue);
			else if (argName.equals("posTagSet")) 
				posTagSetPath = argValue;
			else if (argName.equals("posTagMap")) 
				posTagMapPath = argValue;
			else if (argName.equals("word"))
				word = argValue;
			else
				throw new RuntimeException("Unknown argument: " + argName);
		}
		
        final LefffServiceLocator locator = new LefffServiceLocator();
        locator.setDataSourcePropertiesFile("jdbc-live.properties");
        
        TalismaneServiceLocator talismaneServiceLocator = TalismaneServiceLocator.getInstance();
        
        
        final LefffService lefffService = locator.getLefffService();
        if (command.equals("load")) {
        	if (lefffFilePath.length()==0)
        		throw new RuntimeException("Required argument: lefffFile");
            final LefffLoader loader = lefffService.getLefffLoader();
            File file = new File(lefffFilePath);
            if (startLine>0)
            	loader.setStartLine(startLine);
            if (stopLine>0)
            	loader.setStopLine(stopLine);
            	
            loader.LoadFile(file);
        } else if (command.equals("serialiseBase")) {
           	if (memoryBaseFilePath.length()==0)
        		throw new RuntimeException("Required argument: memoryBase");
          	if (posTagSetPath.length()==0)
        		throw new RuntimeException("Required argument: posTagSet");
         	if (posTagMapPath.length()==0)
        		throw new RuntimeException("Required argument: posTagMap");

	        PosTaggerServiceLocator posTaggerServiceLocator = talismaneServiceLocator.getPosTaggerServiceLocator();
	        PosTaggerService posTaggerService = posTaggerServiceLocator.getPosTaggerService();
	        File posTagSetFile = new File(posTagSetPath);
			PosTagSet posTagSet = posTaggerService.getPosTagSet(posTagSetFile);

			File posTagMapFile = new File(posTagMapPath);
			LefffPosTagMapper posTagMapper = lefffService.getPosTagMapper(posTagMapFile, posTagSet);
			
			Map<PosTagSet, LefffPosTagMapper> posTagMappers = new HashMap<PosTagSet, LefffPosTagMapper>();
			posTagMappers.put(posTagSet, posTagMapper);
			
        	LefffMemoryLoader loader = new LefffMemoryLoader();
        	LefffMemoryBase memoryBase = loader.loadMemoryBaseFromDatabase(lefffService, posTagMappers);
        	File memoryBaseFile = new File(memoryBaseFilePath);
        	memoryBaseFile.delete();
        	loader.serializeMemoryBase(memoryBase, memoryBaseFile);
        } else if (command.equals("deserialiseBase")) {
           	if (memoryBaseFilePath.length()==0)
        		throw new RuntimeException("Required argument: memoryBase");

           	LefffMemoryLoader loader = new LefffMemoryLoader();
        	File memoryBaseFile = new File(memoryBaseFilePath);
        	LefffMemoryBase memoryBase = loader.deserializeMemoryBase(memoryBaseFile);
        	
        	String[] testWords = new String[] {"avoir"};
        	if (word!=null) {
        		testWords = word.split(",");
        	}
      
        	for (String testWord : testWords) {
        		Set<PosTag> possiblePosTags = memoryBase.findPossiblePosTags(testWord);
	        	LOG.debug("##### PosTags for '" + testWord + "': " + possiblePosTags.size());
	        	int i=1;
	        	for (PosTag posTag : possiblePosTags) {
	        		LOG.debug("### PosTag " + (i++) + ":" + posTag);
	        	}
       		
	        	List<? extends LexicalEntry> entriesForWord = memoryBase.getEntries(testWord);
	        	LOG.debug("##### Entries for '" + testWord + "': " + entriesForWord.size());
	        	i = 1;
	        	for (LexicalEntry entry : entriesForWord) {
	        		LOG.debug("### Entry " + (i++) + ":" + entry.getWord());
	        		LOG.debug("Category " + entry.getCategory());
	        		LOG.debug("Predicate " + entry.getPredicate());
	        		LOG.debug("Lemma " + entry.getLemma());
	        		LOG.debug("Morphology " + entry.getMorphology());
	        	}
	        	
	        	List<? extends LexicalEntry> entriesForLemma = memoryBase.getEntriesForLemma(testWord, "");
	        	LOG.debug("##### Entries for '" + testWord + "' lemma: " + entriesForLemma.size());
	        	for (LexicalEntry entry : entriesForLemma) {
	        		LOG.debug("### Entry " + entry.getWord());
	        		LOG.debug("Category " + entry.getCategory());
	        		LOG.debug("Predicate " + entry.getPredicate());
	        		LOG.debug("Lemma " + entry.getLemma());
	        		LOG.debug("Morphology " + entry.getMorphology());
	        		for (PredicateArgument argument : entry.getPredicateArguments()) {
	        			LOG.debug("Argument: " + argument.getFunction() + ",Optional? " + argument.isOptional());
	        			for (String realisation : argument.getRealisations()) {
	        				LOG.debug("Realisation: " + realisation);
	        			}
	        		}
	        	}
        	}
        	
        } else {
            System.out.println("Usage : Lefff load filepath");
        }
		long endTime = (new Date()).getTime() - startTime;
		LOG.debug("Total runtime: " + ((double)endTime / 1000) + " seconds");
   }

}
