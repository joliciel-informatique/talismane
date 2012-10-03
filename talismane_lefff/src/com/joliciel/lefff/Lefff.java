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
        	        	
        	List<? extends LexicalEntry> entriesForAvoir = memoryBase.getEntries("avoir");
        	LOG.debug("##### Entries for 'avoir': " + entriesForAvoir.size());
        	for (LexicalEntry entry : entriesForAvoir) {
        		LOG.debug("### Entry " + entry.getWord());
        		LOG.debug("Category " + entry.getCategory());
        		LOG.debug("Predicate " + entry.getPredicate());
        		LOG.debug("Lemma " + entry.getLemma());
        		LOG.debug("Morphology " + entry.getMorphology());
        	}
        	List<? extends LexicalEntry> entriesForBase = memoryBase.getEntries("base");
        	LOG.debug("##### Entries for 'base': " + entriesForBase.size());
        	for (LexicalEntry entry : entriesForBase) {
        		LOG.debug("### Entry " + entry.getWord());
        		LOG.debug("Category " + entry.getCategory());
        		LOG.debug("Predicate " + entry.getPredicate());
        		LOG.debug("Lemma " + entry.getLemma());
        		LOG.debug("Morphology " + entry.getMorphology());
        	}
        	
        	List<? extends LexicalEntry> entriesForBaserLemma = memoryBase.getEntriesForLemma("baser", "");
        	LOG.debug("##### Entries for 'baser' lemma: " + entriesForBaserLemma.size());
        	for (LexicalEntry entry : entriesForBaserLemma) {
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

        	Set<PosTag> posTagsBase = memoryBase.findPossiblePosTags("base");
           	LOG.debug("##### PosTags for 'base' (CRABBE_CANDITO): " + posTagsBase.size());
	       	for (PosTag postag : posTagsBase) {
	        		LOG.debug(postag.getCode());
	       	}
	       	
	       	String[] testWords = new String[] { "suis", "fils", "aille", "ados" };
	       	String[] testCategories = new String[] { "V", "NC", "V", "NC" };
	       	
	       	for (int i = 0 ; i< testWords.length; i++) {
		       	Set<? extends LexicalEntry> testWordEntries = memoryBase.findLexicalEntries(testWords[i], memoryBase.getPosTagSet().getPosTag(testCategories[i]));
	        	LOG.debug("##### Entries for '" + testWords[i] + "', '" + testCategories[i] + "': " + testWordEntries.size());
	        	for (LexicalEntry entry : testWordEntries) {
	        		LOG.debug("### Entry " + entry.getWord());
	        		LOG.debug("Category " + entry.getCategory());
	        		LOG.debug("Predicate " + entry.getPredicate());
	        		LOG.debug("Lemma " + entry.getLemma());
	        		LOG.debug("Morphology " + entry.getMorphology());
	        		LOG.debug("Status " + entry.getStatus());
	        	}
	       	}

        	
        } else {
            System.out.println("Usage : Lefff load filepath");
        }
		long endTime = (new Date()).getTime() - startTime;
		LOG.debug("Total runtime: " + ((double)endTime / 1000) + " seconds");
   }

}
