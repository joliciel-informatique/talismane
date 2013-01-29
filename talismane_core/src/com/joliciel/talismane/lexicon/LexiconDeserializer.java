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

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.utils.PerformanceMonitor;

/**
 * Used to deserialize a set of lexicon files found in a given directory.
 * @author Assaf
 *
 */
public class LexiconDeserializer {
	private static final Log LOG = LogFactory.getLog(LexiconDeserializer.class);
	
	public List<PosTaggerLexicon> deserializeLexicons(File lexiconDir) {
		PerformanceMonitor.startTask("LexiconDeserializer.deserializeLexicons");
		try {
			File[] inFiles = lexiconDir.listFiles();
			List<PosTaggerLexicon> lexicons = new ArrayList<PosTaggerLexicon>();
			for (File inFile : inFiles) {
				PosTaggerLexicon lexicon = this.deserializeLexiconFile(inFile);
				if (lexicon.getPosTagSet()==null)
					lexicon.setPosTagSet(TalismaneSession.getPosTagSet());
				lexicons.add(lexicon);
			}
			return lexicons;
		} finally {
			PerformanceMonitor.endTask("LexiconDeserializer.deserializeLexicons");
		}
	}
	
	public PosTaggerLexicon deserializeLexiconFile(File lexiconFile) {
		LOG.debug("deserializing " + lexiconFile.getName());
		boolean isZip = false;
		if (lexiconFile.getName().endsWith(".zip"))
			isZip = true;

		PosTaggerLexicon memoryBase = null;
		ZipInputStream zis = null;
		FileInputStream fis = null;
		ObjectInputStream in = null;
	
		try {
			fis = new FileInputStream(lexiconFile);
			if (isZip) {
				zis = new ZipInputStream(fis);
				memoryBase = this.deserializeLexiconFile(zis);
			} else {
				in = new ObjectInputStream(fis);
				memoryBase = (PosTaggerLexicon)in.readObject();
				in.close();
			}
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		} catch (ClassNotFoundException cnfe) {
			throw new RuntimeException(cnfe);
		}
		
		return memoryBase;
	}
	
	public PosTaggerLexicon deserializeLexiconFile(ZipInputStream zis) {
		PosTaggerLexicon memoryBase = null;
		try {
			ZipEntry zipEntry;
			if ((zipEntry = zis.getNextEntry()) != null) {
				LOG.debug("Scanning zip entry " + zipEntry.getName());

				ObjectInputStream in = new ObjectInputStream(zis);
				memoryBase = (PosTaggerLexicon) in.readObject();
				zis.closeEntry();
				in.close();
			} else {
				throw new RuntimeException("No zip entry in input stream");
			}
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		} catch (ClassNotFoundException cnfe) {
			throw new RuntimeException(cnfe);
		} finally {
			PerformanceMonitor.endTask("LefffMemoryLoader.deserializeMemoryBase");
		}

		return memoryBase;
	}
}
