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
package com.joliciel.talismane;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;

import com.joliciel.talismane.lexicon.LexicalEntry;
import com.joliciel.talismane.lexicon.LexiconDeserializer;
import com.joliciel.talismane.lexicon.LexiconSerializer;
import com.joliciel.talismane.lexicon.PosTaggerLexicon;
import com.joliciel.talismane.utils.StringUtils;

/**
 * Direct entry point for Talismane from the command line.
 * @author Assaf Urieli
 *
 */
public class TalismaneMain {
	private static final Log LOG = LogFactory.getLog(TalismaneMain.class);
	private enum OtherCommand {
		serializeLexicon,
		testLexicon
	}
	
	public static void main(String[] args) throws Exception {
    	Map<String,String> argsMap = StringUtils.convertArgs(args);
    	OtherCommand otherCommand = null;
    	if (argsMap.containsKey("command")) {
    		try {
	    		otherCommand = OtherCommand.valueOf(argsMap.get("command"));
	    		argsMap.remove("command");
    		} catch (IllegalArgumentException e) {
    			// not anotherCommand
    		}
    	}
    	
    	String sessionId = "";
       	TalismaneServiceLocator locator = TalismaneServiceLocator.getInstance(sessionId);
       	TalismaneService talismaneService = locator.getTalismaneService();
		TalismaneSession talismaneSession = talismaneService.getTalismaneSession();

    	if (otherCommand==null) {
    		// regular command
        	TalismaneConfig config = talismaneService.getTalismaneConfig(argsMap, sessionId);
        	if (config.getCommand()==null)
        		return;
         	
        	Talismane talismane = config.getTalismane();
        	
        	talismane.process();
    	} else {
    		// other command
			String logConfigPath = argsMap.get("logConfigFile");
			if (logConfigPath!=null) {
				argsMap.remove("logConfigFile");
				Properties props = new Properties();
				props.load(new FileInputStream(logConfigPath));
				PropertyConfigurator.configure(props);
			}
			
	    	switch (otherCommand) {
	    	case serializeLexicon: {
	    		LexiconSerializer serializer = new LexiconSerializer();
	    		serializer.serializeLexicons(argsMap);
	    		break;
	    	}
	    	case testLexicon: {
	    		String lexiconFilePath = null;
	    		String[] wordList = null;
	    		for (String argName : argsMap.keySet()) {
	    			String argValue = argsMap.get(argName);
	    			if (argName.equals("lexicon")) {
	    				lexiconFilePath = argValue;
	    			} else if (argName.equals("words")) {
	    				wordList = argValue.split(",");
	    			} else {
	    				throw new TalismaneException("Unknown argument: " + argName);
	    			}
	    		}
	    		File lexiconFile = new File(lexiconFilePath);
				LexiconDeserializer lexiconDeserializer = new LexiconDeserializer(talismaneSession);
				List<PosTaggerLexicon> lexicons = lexiconDeserializer.deserializeLexicons(lexiconFile);
				for (PosTaggerLexicon lexicon : lexicons)
					talismaneSession.addLexicon(lexicon);
				PosTaggerLexicon mergedLexicon = talismaneSession.getMergedLexicon();
				for (String word : wordList) {
					LOG.info("################");
					LOG.info("Word: " + word);
					List<LexicalEntry> entries = mergedLexicon.getEntries(word);
					for (LexicalEntry entry : entries) {
						LOG.info(entry + ", Full morph: " + entry.getMorphologyForCoNLL());
					}
				}
				break;
	    	}
	    	}
    	}
	}
}
