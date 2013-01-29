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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.lexicon.LexicalEntry;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagSet;

public class LefffMemoryBaseImpl implements LefffMemoryBase {
    private static final Log LOG = LogFactory.getLog(LefffMemoryBaseImpl.class);

	private static final long serialVersionUID = -6687868826900071372L;
	Map<String,List<LexicalEntry>> entryMap = null;
	Map<PosTagSet,LefffPosTagMapper> posTagMappers = null;
	Map<String,List<LexicalEntry>> lemmaEntryMap = null;
	
	transient private PosTagSet posTagSet;
	
	public LefffMemoryBaseImpl(Map<String,List<LexicalEntry>> entryMap, Map<PosTagSet,LefffPosTagMapper> posTagMappers) {
		this.entryMap = entryMap;
		this.posTagMappers = posTagMappers;
		
        double requiredCapacity = 300000;
 		this.lemmaEntryMap = new HashMap<String, List<LexicalEntry>>(((int) Math.ceil(requiredCapacity / 0.75)));
		LOG.debug("Constructing lemma entry map");
		for (List<LexicalEntry> entries : entryMap.values()) {
			for (LexicalEntry entry : entries) {
				LefffEntry lefffEntry = (LefffEntry) entry;
				String lemma = lefffEntry.getLefffLemma().getText();
				String complement = lefffEntry.getLefffLemma().getComplement();
				String key = lemma;
				if (complement.length()>0)
					key += "|" + complement;
				List<LexicalEntry> entriesPerLemma = lemmaEntryMap.get(key);
				if (entriesPerLemma==null) {
					entriesPerLemma = new ArrayList<LexicalEntry>();
					lemmaEntryMap.put(key, entriesPerLemma);
				}
				entriesPerLemma.add(entry);
			}
		}
	}
	
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
		if (complement.length()>0)
			key += "|" + complement;
		List<LexicalEntry> entries = new ArrayList<LexicalEntry>();
		List<LexicalEntry> lefffEntries = this.lemmaEntryMap.get(key);
		for (LexicalEntry lefffEntry : lefffEntries)
			entries.add(lefffEntry);

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
		LefffPosTagMapper posTagMapper = this.posTagMappers.get(this.posTagSet);
		String checkWord = this.getCheckWord(word);
		List<LexicalEntry> entries = this.getEntries(checkWord);
		Set<PosTag> posTags = new TreeSet<PosTag>();
		for (LexicalEntry entry : entries) {
			Set<PosTag> posTagsOnePos = posTagMapper.getPosTags(entry.getCategory(), entry.getMorphology());

			if (posTagsOnePos!=null)
				posTags.addAll(posTagsOnePos);
		}
		
		boolean addNullTag = false;
		if (word.equals("[[du]]")) {
			addNullTag = true;
		} else if (word.equals("[[des]]")) {
			addNullTag = true;
		}
		if (addNullTag)
			posTags.add(PosTag.NULL_POS_TAG);
		return posTags;
	}

	@Override
	public List<LexicalEntry> findLexicalEntries(String word, PosTag posTag) {
		LefffPosTagMapper posTagMapper = this.posTagMappers.get(this.posTagSet);
		String checkWord = this.getCheckWord(word);
		List<LexicalEntry> entries = this.getEntries(checkWord);
		List<LexicalEntry> entriesToReturn = new ArrayList<LexicalEntry>();
		
		for (LexicalEntry entry : entries) {
			if (!entry.getCategory().equals("auxEtre")&&!entry.getCategory().equals("auxAvoir")) {
				Set<PosTag> posTagsOnePos = posTagMapper.getPosTags(entry.getCategory(), entry.getMorphology());
	
				if (posTagsOnePos!=null && posTagsOnePos.contains(posTag)) {
					entriesToReturn.add(entry);
				}
			}
		}
		return entriesToReturn;
	}
	

	@Override
	public List<LexicalEntry> getEntriesForLemma(String lemma, String complement, PosTag posTag) {
		LefffPosTagMapper posTagMapper = this.posTagMappers.get(this.posTagSet);
		List<LexicalEntry> entries = this.getEntriesForLemma(lemma, complement);
		Set<LexicalEntry> entriesToReturn = new TreeSet<LexicalEntry>();

		for (LexicalEntry entry : entries) {
			if (!entry.getCategory().equals("auxEtre")&&!entry.getCategory().equals("auxAvoir")) {
				Set<PosTag> posTagsOnePos = posTagMapper.getPosTags(entry.getCategory(), entry.getMorphology());
	
				if (posTagsOnePos!=null && posTagsOnePos.contains(posTag)) {
					entriesToReturn.add(entry);
				}
			}
		}
		
		List<LexicalEntry> entryList = new ArrayList<LexicalEntry>(entriesToReturn);
		return entryList;

	}

	private String getCheckWord(String word) {
		String checkWord = word;
		if (word.equals("[[au]]")) {
			checkWord = "le";
		} else if (word.equals("[[du]]")) {
			checkWord = "le";
		} else if (word.equals("[[aux]]")) {
			checkWord = "les";
		} else if (word.equals("[[des]]")) {
			checkWord = "les";
		}
		return checkWord;
	}
	
	@Override
	public PosTagSet getPosTagSet() {
		return posTagSet;
	}

	@Override
	public void setPosTagSet(PosTagSet posTagSet) {
		this.posTagSet = posTagSet;
	}

	public Map<PosTagSet, LefffPosTagMapper> getPosTagMappers() {
		return posTagMappers;
	}
	
}
