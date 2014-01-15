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
package com.joliciel.talismane.fr.lexicon;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import com.joliciel.talismane.lexicon.LexicalEntry;
import com.joliciel.talismane.lexicon.PosTagMapper;
import com.joliciel.talismane.lexicon.PosTaggerLexicon;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagSet;

/**
 * A lexicon in Grace format, see http://www.limsi.fr/TLP/grace/www/gracdoc.html
 * @author Assaf Urieli
 *
 */
public class GraceLexicon implements PosTaggerLexicon, Serializable {
	private static final long serialVersionUID = 1L;
	Map<String,List<LexicalEntry>> entryMap = new HashMap<String, List<LexicalEntry>>();
	Map<String,List<LexicalEntry>> lemmaEntryMap = new HashMap<String, List<LexicalEntry>>();
	PosTagSet posTagSet;
	PosTagMapper posTagMapper;
	private Map<String,PosTag> posTagMap;

	@Override
	public List<LexicalEntry> getEntries(String word) {
		List<LexicalEntry> entries = this.entryMap.get(word);
		if (entries==null)
			entries = new ArrayList<LexicalEntry>();
		return entries;
	}
	
	@Override
	public List<LexicalEntry> getEntriesForLemma(String lemma, String complement) {
		String key = lemma;
		if (complement!=null && complement.length()>0)
			key += "|" + complement;
		List<LexicalEntry> entries = new ArrayList<LexicalEntry>();
		List<LexicalEntry> lefffEntries = this.lemmaEntryMap.get(key);
		if (lefffEntries!=null) {
			for (LexicalEntry lefffEntry : lefffEntries)
				entries.add(lefffEntry);
		}

		return entries;
	}
	
	public List<LexicalEntry> getEntriesMatchingCriteria(LexicalEntry lexicalEntry, PosTag posTag, String gender, String number) {
		List<LexicalEntry> lemmaEntries = null;
		if (posTag!=null)
			lemmaEntries = this.getEntriesForLemma(lexicalEntry.getLemma(), lexicalEntry.getLemmaComplement(), posTag);
		else
			lemmaEntries = this.getEntriesForLemma(lexicalEntry.getLemma(), lexicalEntry.getLemmaComplement());
		Set<LexicalEntry> entriesToReturn = new TreeSet<LexicalEntry>();
		for (LexicalEntry lemmaEntry : lemmaEntries) {
			if ((number==null || number.length()==0 || lemmaEntry.getNumber().contains(number))
					&& (gender==null || gender.length()==0 || lemmaEntry.getGender().contains(gender))) {
				entriesToReturn.add(lemmaEntry);
			}
		}
		
		List<LexicalEntry> entryList = new ArrayList<LexicalEntry>(entriesToReturn);
		return entryList;
	}

	@Override
	public Set<PosTag> findPossiblePosTags(String word) {
		List<LexicalEntry> entries = this.getEntries(word);
		Set<PosTag> posTags = new TreeSet<PosTag>();
		for (LexicalEntry entry : entries) {
			posTags.addAll(this.getPosTagMapper().getPosTags(entry));
		}
		
		return posTags;
	}

	@Override
	public List<LexicalEntry> findLexicalEntries(String word, PosTag posTag) {
		List<LexicalEntry> entries = this.getEntries(word);
		List<LexicalEntry> entriesToReturn = new ArrayList<LexicalEntry>();
		
		for (LexicalEntry entry : entries) {
			Set<PosTag> posTags = this.getPosTagMapper().getPosTags(entry);

			if (posTags.contains(posTag)) {
				entriesToReturn.add(entry);
			}
		}
		return entriesToReturn;
	}
	

	@Override
	public List<LexicalEntry> getEntriesForLemma(String lemma, String complement, PosTag posTag) {
		List<LexicalEntry> entries = this.getEntriesForLemma(lemma, complement);
		Set<LexicalEntry> entriesToReturn = new TreeSet<LexicalEntry>();

		for (LexicalEntry entry : entries) {
			Set<PosTag> posTags = this.getPosTagMapper().getPosTags(entry);

			if (posTags.contains(posTag)) {
				entriesToReturn.add(entry);
			}
		}
		
		List<LexicalEntry> entryList = new ArrayList<LexicalEntry>(entriesToReturn);
		return entryList;

	}

	public PosTagSet getPosTagSet() {
		return posTagSet;
	}

	public void setPosTagSet(PosTagSet posTagSet) {
		this.posTagSet = posTagSet;
	}

	public Map<String, PosTag> getPosTagMap() {
		return posTagMap;
	}

	public void setPosTagMap(Map<String, PosTag> posTagMap) {
		this.posTagMap = posTagMap;
	}

	public void addEntry(String word, String morphology, String lemma) {
		GraceEntry entry = new GraceEntry(word, morphology, lemma);
		List<LexicalEntry> entries = entryMap.get(word);
		if (entries==null) {
			entries = new ArrayList<LexicalEntry>();
			entryMap.put(word, entries);
		}
		entries.add(entry);
		entries = lemmaEntryMap.get(lemma);
		if (entries==null) {
			entries = new ArrayList<LexicalEntry>();
			lemmaEntryMap.put(lemma, entries);
		}
		entries.add(entry);
	}

	public PosTagMapper getPosTagMapper() {
		if (posTagMapper==null) {
			posTagMapper = new GracePosTagMapper(posTagSet, posTagMap);
		}
		return posTagMapper;
	}

	public void setPosTagMapper(PosTagMapper posTagMapper) {
		this.posTagMapper = posTagMapper;
	}
	
	
} 
