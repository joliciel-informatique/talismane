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


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagSet;

/**
 * Pos-tag mapper that maps Category, Subcategory and morphology to postags using a
 * tab-delimited file as input.
 * File format is:
 * category\tsubcategory\tmorphology\tpostag
 * @author Assaf Urieli
 *
 */
public class DefaultPosTagMapper implements PosTagMapper {
	private PosTagSet posTagSet;
	private Map<String, Set<PosTag>> posTagMap = new HashMap<String, Set<PosTag>>();
	
	public DefaultPosTagMapper(Scanner scanner, PosTagSet posTagSet) {
		super();
		this.posTagSet = posTagSet;
		
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			String[] parts = line.split("\t");
			String key = parts[0] + "|" + parts[1] + "|" + parts[2];
			String value = parts[3];
			PosTag posTag = posTagSet.getPosTag(value);
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
		return this.posTagSet;
	}

	@Override
	public Set<PosTag> getPosTags(LexicalEntry lexicalEntry) {
		String key = lexicalEntry.getCategory() + "|"
			+ (lexicalEntry.getSubCategory()==null ? "" : lexicalEntry.getSubCategory())  + "|"
			+ (lexicalEntry.getMorphology()==null ? "" : lexicalEntry.getMorphology());
		Set<PosTag> posTags = posTagMap.get(key);
		if (posTags==null)
			posTags = new HashSet<PosTag>();
		return posTags;
	}

}
