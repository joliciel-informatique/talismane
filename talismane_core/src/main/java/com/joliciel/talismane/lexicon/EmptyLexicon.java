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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagSet;

/**
 * An Empty lexicon to use as a placeholder when no lexicon is available.
 * @author Assaf Urieli
 *
 */
public class EmptyLexicon implements PosTaggerLexicon {
	private static final long serialVersionUID = 1L;
	PosTagSet posTagSet;
	PosTagMapper posTagMapper;

	@Override
	public List<LexicalEntry> getEntries(String word) {
		return new ArrayList<LexicalEntry>();
	}

	@Override
	public List<LexicalEntry> getEntriesForLemma(String lemma) {
		return new ArrayList<LexicalEntry>();
	}

	@Override
	public Set<PosTag> findPossiblePosTags(String word) {
		return new HashSet<PosTag>();
	}

	@Override
	public List<LexicalEntry> findLexicalEntries(String word, PosTag posTag) {
		return new ArrayList<LexicalEntry>();
	}

	@Override
	public List<LexicalEntry> getEntriesForLemma(String lemma, PosTag posTag) {
		return new ArrayList<LexicalEntry>();
	}

	@Override
	public List<LexicalEntry> getEntriesMatchingCriteria(
			LexicalEntry lexicalEntry, PosTag posTag, String gender,
			String number) {
		return new ArrayList<LexicalEntry>();
	}

	public PosTagSet getPosTagSet() {
		return posTagSet;
	}

	public void setPosTagSet(PosTagSet posTagSet) {
		this.posTagSet = posTagSet;
	}

	public PosTagMapper getPosTagMapper() {
		return posTagMapper;
	}

	public void setPosTagMapper(PosTagMapper posTagMapper) {
		this.posTagMapper = posTagMapper;
	}

	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public Iterator<LexicalEntry> getAllEntries() {
		return new ArrayList<LexicalEntry>().iterator();
	}


}
