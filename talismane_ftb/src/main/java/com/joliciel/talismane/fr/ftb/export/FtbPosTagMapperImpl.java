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
package com.joliciel.talismane.fr.ftb.export;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.fr.ftb.Category;
import com.joliciel.talismane.fr.ftb.Morphology;
import com.joliciel.talismane.fr.ftb.SubCategory;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.utils.LogUtils;

class FtbPosTagMapperImpl implements FtbPosTagMapper {
	private static final Logger LOG = LoggerFactory.getLogger(FtbPosTagMapperImpl.class);

	private Map<String, PosTag> posTagMap = new TreeMap<String, PosTag>();
	PosTagSet posTagSet;

	public FtbPosTagMapperImpl(File file, PosTagSet posTagSet) {
		this.posTagSet = posTagSet;
		try (Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8")))) {
			List<String> descriptors = new ArrayList<String>();
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				descriptors.add(line);
			}
			this.load(descriptors);
		} catch (IOException e) {
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
	public PosTag getPosTag(Category category, SubCategory subCategory, Morphology morphology) {
		String key = category.getCode() + "|" + subCategory.getCode() + "|" + morphology.getCode();
		PosTag posTag = posTagMap.get(key);
		if (posTag == null) {
			throw new TalismaneException("Could not find postag for: " + key);
		}
		return posTag;
	}

	@Override
	public PosTagSet getPosTagSet() {
		return posTagSet;
	}

	public void setPosTagSet(PosTagSet posTagSet) {
		this.posTagSet = posTagSet;
	}

}
