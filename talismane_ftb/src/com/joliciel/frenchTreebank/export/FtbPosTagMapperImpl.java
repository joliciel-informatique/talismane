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
package com.joliciel.frenchTreebank.export;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.frenchTreebank.Category;
import com.joliciel.frenchTreebank.Morphology;
import com.joliciel.frenchTreebank.SubCategory;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.utils.util.LogUtils;

class FtbPosTagMapperImpl implements FtbPosTagMapper {
	private static final Log LOG = LogFactory.getLog(FtbPosTagMapperImpl.class);
	
	private Map<String,PosTag> posTagMap = new TreeMap<String, PosTag>();
	PosTagSet posTagSet;
	
	public FtbPosTagMapperImpl(File file, PosTagSet posTagSet) {
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
	
	public FtbPosTagMapperImpl(List<String> descriptors, PosTagSet posTagSet) {
		this.posTagSet = posTagSet;
		this.load(descriptors);
	}
	
	void load(List<String> descriptors) {
		for (String descriptor : descriptors) {
			if (descriptor.startsWith("#"))
				continue;
			String[] parts = descriptor.split("\t");
			String key = parts[0] + "|" + parts[1] + "|" + parts[2];
			PosTag posTag = posTagSet.getPosTag(parts[3]);
			posTagMap.put(key, posTag);
		}
	}
	
	@Override
	public PosTag getPosTag(Category category, SubCategory subCategory,
			Morphology morphology) {
		String key = category.getCode() + "|" + subCategory.getCode() + "|" + morphology.getCode();
		return posTagMap.get(key);
	}

	public PosTagSet getPosTagSet() {
		return posTagSet;
	}

	public void setPosTagSet(PosTagSet posTagSet) {
		this.posTagSet = posTagSet;
	}

}
