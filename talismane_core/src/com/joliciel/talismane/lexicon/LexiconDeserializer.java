///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2013 Assaf Urieli
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.utils.PerformanceMonitor;

/**
 * Used to deserialize a set of lexicon files found in a given directory.
 * @author Assaf Urieli
 *
 */
public class LexiconDeserializer {
	private static final Log LOG = LogFactory.getLog(LexiconDeserializer.class);
	private static final PerformanceMonitor MONITOR = PerformanceMonitor.getMonitor(LexiconDeserializer.class);
	
	public List<PosTaggerLexicon> deserializeLexicons(File lexiconDir) {
		if (!lexiconDir.exists())
			throw new TalismaneException("Lexicon dir does not exist: " + lexiconDir.getPath());
		File[] inFiles = lexiconDir.listFiles();
		
		List<PosTaggerLexicon> lexicons = new ArrayList<PosTaggerLexicon>();
		
		if (inFiles!=null) {
			for (File inFile : inFiles) {
				PosTaggerLexicon lexicon = this.deserializeLexiconFile(inFile);
				if (lexicon.getPosTagSet()==null)
					lexicon.setPosTagSet(TalismaneSession.getPosTagSet());
				lexicons.add(lexicon);
			}
		}
		return lexicons;
	}
	
	public PosTaggerLexicon deserializeLexiconFile(File lexiconFile) {
		MONITOR.startTask("deserializeLexiconFile(File)");
		try {
			LOG.debug("deserializing " + lexiconFile.getName());
			boolean isZip = false;
			if (lexiconFile.getName().endsWith(".zip"))
				isZip = true;
	
			PosTaggerLexicon lexicon = null;
			ZipInputStream zis = null;
			FileInputStream fis = null;
			ObjectInputStream in = null;
		
			try {
				fis = new FileInputStream(lexiconFile);
				if (isZip) {
					zis = new ZipInputStream(fis);
					lexicon = this.deserializeLexiconFile(zis);
				} else {
					in = new ObjectInputStream(fis);
					lexicon = (PosTaggerLexicon)in.readObject();
					in.close();
				}
			} catch (IOException ioe) {
				throw new RuntimeException(ioe);
			} catch (ClassNotFoundException cnfe) {
				throw new RuntimeException(cnfe);
			}
			
			return lexicon;
		} finally {
			MONITOR.endTask("deserializeLexiconFile(File)");
		}
	}
	
	public PosTaggerLexicon deserializeLexiconFile(ZipInputStream zis) {
		MONITOR.startTask("deserializeLexiconFile(ZipInputStream)");
		try {
			PosTaggerLexicon lexicon = null;
			try {
				ZipEntry zipEntry;
				if ((zipEntry = zis.getNextEntry()) != null) {
					LOG.debug("Scanning zip entry " + zipEntry.getName());
	
					ObjectInputStream in = new ObjectInputStream(zis);
					lexicon = (PosTaggerLexicon) in.readObject();
					zis.closeEntry();
					in.close();
				} else {
					throw new RuntimeException("No zip entry in input stream");
				}
			} catch (IOException ioe) {
				throw new RuntimeException(ioe);
			} catch (ClassNotFoundException cnfe) {
				throw new RuntimeException(cnfe);
			}
	
			return lexicon;
		} finally {
			MONITOR.endTask("deserializeLexiconFile(ZipInputStream)");
		}
	}
	
	public PosTaggerLexicon deserializeLexiconFile(ObjectInputStream ois) {
		PosTaggerLexicon memoryBase = null;
		try {
			memoryBase = (PosTaggerLexicon) ois.readObject();
			ois.close();
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		} catch (ClassNotFoundException cnfe) {
			throw new RuntimeException(cnfe);
		}

		return memoryBase;
	}
}
