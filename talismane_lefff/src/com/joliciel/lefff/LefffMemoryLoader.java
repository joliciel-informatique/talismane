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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.lexicon.LexicalEntry;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.utils.PerformanceMonitor;

public class LefffMemoryLoader {
    private static final Log LOG = LogFactory.getLog(LefffMemoryLoader.class);
    
	public LefffMemoryBase loadMemoryBaseFromDatabase(LefffService lefffService, Map<PosTagSet,LefffPosTagMapper> posTagMappers) {		
		Map<String,List<LexicalEntry>> entryMap = lefffService.findEntryMap();
		LefffMemoryBase memoryBase = new LefffMemoryBaseImpl(entryMap, posTagMappers);
		return memoryBase;
	}
	
	public LefffMemoryBase deserializeMemoryBase(ZipInputStream zis) {
		LefffMemoryBase memoryBase = null;
		PerformanceMonitor.startTask("LefffMemoryLoader.deserializeMemoryBase");
		try {
			ZipEntry zipEntry;
			if ((zipEntry = zis.getNextEntry()) != null) {
				LOG.debug("Scanning zip entry " + zipEntry.getName());

				ObjectInputStream in = new ObjectInputStream(zis);
				memoryBase = (LefffMemoryBase) in.readObject();
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
		
       	Map<PosTagSet, LefffPosTagMapper> posTagMappers = memoryBase.getPosTagMappers();
    	PosTagSet posTagSet = posTagMappers.keySet().iterator().next();
    	memoryBase.setPosTagSet(posTagSet);

		return memoryBase;
	}
	
	public LefffMemoryBase deserializeMemoryBase(File memoryBaseFile) {
		LOG.debug("deserializeMemoryBase");
		boolean isZip = false;
		if (memoryBaseFile.getName().endsWith(".zip"))
			isZip = true;

		LefffMemoryBase memoryBase = null;
		ZipInputStream zis = null;
		FileInputStream fis = null;
		ObjectInputStream in = null;
	
		try {
			fis = new FileInputStream(memoryBaseFile);
			if (isZip) {
				zis = new ZipInputStream(fis);
				memoryBase = this.deserializeMemoryBase(zis);
			} else {
				in = new ObjectInputStream(fis);
				memoryBase = (LefffMemoryBase)in.readObject();
				in.close();
			}
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		} catch (ClassNotFoundException cnfe) {
			throw new RuntimeException(cnfe);
		}
		
		return memoryBase;
	}
	
	public void serializeMemoryBase(LefffMemoryBase memoryBase, File memoryBaseFile) {
		LOG.debug("serializeMemoryBase");
		boolean isZip = false;
		if (memoryBaseFile.getName().endsWith(".zip"))
			isZip = true;

		FileOutputStream fos = null;
		ObjectOutputStream out = null;
		ZipOutputStream zos = null;
		try
		{
			fos = new FileOutputStream(memoryBaseFile);
			if (isZip) {
				zos = new ZipOutputStream(fos);
				zos.putNextEntry(new ZipEntry("lefff.obj"));
				out = new ObjectOutputStream(zos);
			} else {
				out = new ObjectOutputStream(fos);
			}
			
			try {
				out.writeObject(memoryBase);
			} finally {
				out.flush();
				out.close();
			}
		} catch(IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}
	
}
