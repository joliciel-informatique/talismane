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
package com.joliciel.lefff;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.lexicon.LexicalEntry;
import com.joliciel.talismane.lexicon.PosTagMapper;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.utils.LogUtils;

public class LefffPosTagMapper implements PosTagMapper, Serializable {
	private static final long serialVersionUID = 4354513469099747673L;

	private static final Log LOG = LogFactory.getLog(LefffPosTagMapper.class);
	
	private Map<String,Set<PosTag>> posTagMap = new HashMap<String, Set<PosTag>>();
	PosTagSet posTagSet;

	public LefffPosTagMapper(File file, PosTagSet posTagSet) {
		this.posTagSet = posTagSet;
		Scanner scanner = null;
		try {
			scanner = new Scanner(file);
			List<String> descriptors = new ArrayList<String>();
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				descriptors.add(line);
			}
			this.load(descriptors);
		} catch (FileNotFoundException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
	
	public LefffPosTagMapper(Scanner scanner, PosTagSet posTagSet) {
		this.posTagSet = posTagSet;

		List<String> descriptors = new ArrayList<String>();
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			descriptors.add(line);
		}
		this.load(descriptors);

	}
	
	public LefffPosTagMapper(List<String> descriptors, PosTagSet posTagSet) {
		this.posTagSet = posTagSet;
		this.load(descriptors);
	}
	
	void load(List<String> descriptors) {
		for (String descriptor : descriptors) {
			if (descriptor.startsWith("#"))
				continue;
			String[] parts = descriptor.split("\t");
			String key = parts[0] + "|" + parts[1];
			PosTag posTag = posTagSet.getPosTag(parts[2]);
			Set<PosTag> posTags = posTagMap.get(key);
			if (posTags==null) {
				posTags = new HashSet<PosTag>();
				posTagMap.put(key, posTags);
			}
			posTags.add(posTag);
		}
	}
	
	@Override
	public PosTagSet getPosTagSet() {
		return posTagSet;
	}

	@Override
	public Set<PosTag> getPosTags(LexicalEntry lexicalEntry) {
		String key = lexicalEntry.getCategory() + "|" + lexicalEntry.getMorphology();
		return posTagMap.get(key);
	}

	public void serialize(File memoryBaseFile) {
		LOG.debug("serialize");
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
				zos.putNextEntry(new ZipEntry("posTagMap.obj"));
				out = new ObjectOutputStream(zos);
			} else {
				out = new ObjectOutputStream(fos);
			}
			
			try {
				out.writeObject(this);
			} finally {
				out.flush();
				out.close();
			}
		} catch(IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}
}
