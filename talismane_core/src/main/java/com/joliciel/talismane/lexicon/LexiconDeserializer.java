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
package com.joliciel.talismane.lexicon;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.NeedsTalismaneSession;
import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.PerformanceMonitor;
import com.joliciel.talismane.utils.StringUtils;

/**
 * Used to deserialize a zip file containing an ordered set of lexicons serialized by the {@link LexiconSerializer}.
 * @author Assaf Urieli
 *
 */
public class LexiconDeserializer {
	private static final Log LOG = LogFactory.getLog(LexiconDeserializer.class);
	private static final PerformanceMonitor MONITOR = PerformanceMonitor.getMonitor(LexiconDeserializer.class);
	
	private TalismaneSession talismaneSession;
	
	public LexiconDeserializer(TalismaneSession talismaneSession) {
		this.talismaneSession = talismaneSession;
	}
	

	public List<PosTaggerLexicon> deserializeLexicons(File lexiconFile) {
		if (!lexiconFile.exists())
			throw new TalismaneException("LexiconFile does not exist: " + lexiconFile.getPath());
		try {
			FileInputStream fis = new FileInputStream(lexiconFile);
			ZipInputStream zis = new ZipInputStream(fis);
			return this.deserializeLexicons(zis);
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
	
	public List<PosTaggerLexicon> deserializeLexicons(ZipInputStream zis) {
		MONITOR.startTask("deserializeLexicons");
		try {
			List<PosTaggerLexicon> lexicons = new ArrayList<PosTaggerLexicon>();
			Map<String,PosTaggerLexicon> lexiconMap = new HashMap<String, PosTaggerLexicon>();
			String[] lexiconNames = null;
			
			ZipEntry ze = null;
		    while ((ze = zis.getNextEntry()) != null) {
		    	LOG.debug(ze.getName());
		    	if (ze.getName().endsWith(".obj")) {
		    		LOG.debug("deserializing " + ze.getName());
					ObjectInputStream in = new ObjectInputStream(zis);
					PosTaggerLexicon lexicon = (PosTaggerLexicon)in.readObject();
					lexiconMap.put(lexicon.getName(), lexicon);
		    	} else if (ze.getName().equals("lexicon.properties")) {
		    		Reader reader = new BufferedReader(new InputStreamReader(zis, "UTF-8"));
		    		Properties props = new Properties();
		    		props.load(reader);
		    		Map<String,String> properties = StringUtils.getArgMap(props);
		    		lexiconNames = properties.get("lexicons").split(",");
		    	}
		    }
			
		    for (String lexiconName : lexiconNames) {
		    	PosTaggerLexicon lexicon = lexiconMap.get(lexiconName);
				if (lexicon instanceof NeedsTalismaneSession)
					((NeedsTalismaneSession) lexicon).setTalismaneSession(talismaneSession);
		    	lexicons.add(lexicon);
		    }
		    
			return lexicons;
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		} catch (ClassNotFoundException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		} finally {
			MONITOR.endTask();
		}
	}
}
