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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.lexicon.LexicalEntry;
import com.joliciel.talismane.lexicon.PosTagMapper;
import com.joliciel.talismane.lexicon.PosTaggerLexicon;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.utils.PerformanceMonitor;

public class LefffMemoryBase implements Serializable, PosTaggerLexicon {
    private static final Log LOG = LogFactory.getLog(LefffMemoryBase.class);
	private static final PerformanceMonitor MONITOR = PerformanceMonitor.getMonitor(LefffMemoryBase.class);

	private static final long serialVersionUID = -6687868826900071372L;
	Map<String,List<LexicalEntry>> entryMap = null;
	Map<String,List<LexicalEntry>> lemmaEntryMap = null;
	
	transient private PosTagSet posTagSet;
	transient private PosTagMapper posTagMapper;
	
	public LefffMemoryBase(Map<String,List<LexicalEntry>> entryMap) {
		this.entryMap = entryMap;
		
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
		String checkWord = this.getCheckWord(word);
		List<LexicalEntry> entries = this.getEntries(checkWord);
		Set<PosTag> posTags = new TreeSet<PosTag>();
		for (LexicalEntry entry : entries) {
			Set<PosTag> posTagsOnePos = posTagMapper.getPosTags(entry);

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
		String checkWord = this.getCheckWord(word);
		List<LexicalEntry> entries = this.getEntries(checkWord);
		List<LexicalEntry> entriesToReturn = new ArrayList<LexicalEntry>();
		
		for (LexicalEntry entry : entries) {
			if (!entry.getCategory().equals("auxEtre")&&!entry.getCategory().equals("auxAvoir")) {
				Set<PosTag> posTagsOnePos = posTagMapper.getPosTags(entry);
	
				if (posTagsOnePos!=null && posTagsOnePos.contains(posTag)) {
					entriesToReturn.add(entry);
				}
			}
		}
		return entriesToReturn;
	}
	

	@Override
	public List<LexicalEntry> getEntriesForLemma(String lemma, String complement, PosTag posTag) {
		List<LexicalEntry> entries = this.getEntriesForLemma(lemma, complement);
		Set<LexicalEntry> entriesToReturn = new TreeSet<LexicalEntry>();

		for (LexicalEntry entry : entries) {
			if (!entry.getCategory().equals("auxEtre")&&!entry.getCategory().equals("auxAvoir")) {
				Set<PosTag> posTagsOnePos = posTagMapper.getPosTags(entry);
	
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

	public PosTagMapper getPosTagMapper() {
		return posTagMapper;
	}

	public void setPosTagMapper(PosTagMapper posTagMapper) {
		this.posTagMapper = posTagMapper;
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
				zos.putNextEntry(new ZipEntry("lefff.obj"));
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
	

	public static PosTaggerLexicon deserializeMemoryBase(ZipInputStream zis) {
		LefffMemoryBase memoryBase = null;
		MONITOR.startTask("deserializeMemoryBase");
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
			MONITOR.endTask("deserializeMemoryBase");
		}

		return memoryBase;
	}
	
	public static PosTaggerLexicon deserializeMemoryBase(File memoryBaseFile) {
		LOG.debug("deserializeMemoryBase");
		boolean isZip = false;
		if (memoryBaseFile.getName().endsWith(".zip"))
			isZip = true;

		PosTaggerLexicon memoryBase = null;
		ZipInputStream zis = null;
		FileInputStream fis = null;
		ObjectInputStream in = null;
	
		try {
			fis = new FileInputStream(memoryBaseFile);
			if (isZip) {
				zis = new ZipInputStream(fis);
				memoryBase = LefffMemoryBase.deserializeMemoryBase(zis);
			} else {
				in = new ObjectInputStream(fis);
				memoryBase = (PosTaggerLexicon)in.readObject();
				in.close();
			}
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		} catch (ClassNotFoundException cnfe) {
			throw new RuntimeException(cnfe);
		}
		
		return memoryBase;
	}
}
