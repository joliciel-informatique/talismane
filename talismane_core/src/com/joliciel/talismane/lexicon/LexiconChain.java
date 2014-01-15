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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagSet;

/**
 * A chain of lexicons that can be used to pool lexical information.
 * @author Assaf Urieli
 *
 */
public class LexiconChain implements PosTaggerLexicon {
	private List<PosTaggerLexicon> lexicons = new ArrayList<PosTaggerLexicon>();
	
	
	@Override
	public List<LexicalEntry> getEntries(String word) {
		List<LexicalEntry> entries = new ArrayList<LexicalEntry>();
		for (PosTaggerLexicon lexicon : lexicons) {
			entries.addAll(lexicon.getEntries(word));
		}
		return entries;
	}

	@Override
	public List<LexicalEntry> getEntriesForLemma(String lemma,
			String complement) {
		List<LexicalEntry> entries = new ArrayList<LexicalEntry>();
		for (PosTaggerLexicon lexicon : lexicons) {
			entries.addAll(lexicon.getEntriesForLemma(lemma, complement));
		}
		return entries;
	}

	@Override
	public Set<PosTag> findPossiblePosTags(String word) {
		// Using TreeSet as set must be ordered
		Set<PosTag> posTags = new TreeSet<PosTag>();
		for (PosTaggerLexicon lexicon : lexicons) {
			posTags.addAll(lexicon.findPossiblePosTags(word));
		}
		return posTags;
	}

	@Override
	public List<LexicalEntry> findLexicalEntries(String word, PosTag posTag) {
		List<LexicalEntry> entries = new ArrayList<LexicalEntry>();
		for (PosTaggerLexicon lexicon : lexicons) {
			entries.addAll(lexicon.findLexicalEntries(word, posTag));
		}
		return entries;
	}

	@Override
	public List<LexicalEntry> getEntriesForLemma(String lemma,
			String complement, PosTag posTag) {
		List<LexicalEntry> entries = new ArrayList<LexicalEntry>();
		for (PosTaggerLexicon lexicon : lexicons) {
			entries.addAll(lexicon.getEntriesForLemma(lemma, complement, posTag));
		}
		return entries;
	}

	@Override
	public List<LexicalEntry> getEntriesMatchingCriteria(
			LexicalEntry lexicalEntry, PosTag posTag, String gender,
			String number) {
		List<LexicalEntry> entries = new ArrayList<LexicalEntry>();
		for (PosTaggerLexicon lexicon : lexicons) {
			entries.addAll(lexicon.getEntriesMatchingCriteria(lexicalEntry, posTag, gender, number));
		}
		return entries;
	}

	@Override
	public PosTagSet getPosTagSet() {
		if (this.lexicons.size()>0) {
			PosTaggerLexicon lexicon = this.lexicons.get(0);
			return lexicon.getPosTagSet();
		}
		return null;
	}

	@Override
	public void setPosTagSet(PosTagSet posTagSet) {
		for (PosTaggerLexicon lexicon : lexicons)
			lexicon.setPosTagSet(posTagSet);
	}

	public void addLexicon(PosTaggerLexicon lexicon) {
		this.lexicons.add(lexicon);
	}

	@Override
	public PosTagMapper getPosTagMapper() {
		if (this.lexicons.size()>0) {
			PosTaggerLexicon lexicon = this.lexicons.get(0);
			return lexicon.getPosTagMapper();
		}
		return null;
	}

	@Override
	public void setPosTagMapper(PosTagMapper posTagMapper) {
		for (PosTaggerLexicon lexicon : lexicons)
			lexicon.setPosTagMapper(posTagMapper);
	}
}
