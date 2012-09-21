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
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.utils.LogUtils;

public class LefffPosTagMapperImpl implements LefffPosTagMapper {
	private static final long serialVersionUID = 4354513469099747673L;

	private static final Log LOG = LogFactory.getLog(LefffPosTagMapperImpl.class);
	
	private Map<String,Set<PosTag>> posTagMap = new HashMap<String, Set<PosTag>>();
	PosTagSet posTagSet;

	public LefffPosTagMapperImpl(File file, PosTagSet posTagSet) {
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
	
	public LefffPosTagMapperImpl(List<String> descriptors, PosTagSet posTagSet) {
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
	public Set<PosTag> getPosTags(String category, String morphology) {
		String key = category + "|" + morphology;
		return posTagMap.get(key);
	}

	@Override
	public Set<PosTag> getPosTags(Category category, Attribute morphology) {
		return this.getPosTags(category.getCode(), morphology.getValue());
	}

}
