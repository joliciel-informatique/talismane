package com.joliciel.talismane.lexicon;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.utils.LogUtils;

/**
 * A lexicon contained in a file.
 * Assumes that the lexical entry categories are already set to pos-tag codes for the current
 * posTagSet.
 * @author Assaf Urieli
 *
 */
public class LexiconFile implements PosTaggerLexicon, Serializable {
	private static final long serialVersionUID = -1288465773172734665L;
	private static final Log LOG = LogFactory.getLog(LexiconFile.class);
	Map<String,List<LexicalEntry>> entryMap = new HashMap<String, List<LexicalEntry>>();
	Map<String,List<LexicalEntry>> lemmaEntryMap = new HashMap<String, List<LexicalEntry>>();
	PosTagSet posTagSet;
	PosTagMapper posTagMapper;
	
	public LexiconFile(LexicalEntryReader reader, File file) throws IOException {
		this(reader, new FileInputStream(file));
	}
	
	public LexiconFile(LexicalEntryReader reader, InputStream inputStream) {
		super();

		try {
			Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(inputStream, "UTF-8")));

			int entryCount = 0;
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if (line.length()>0 && !line.startsWith("#")) {
					LexicalEntry lexicalEntry = reader.readEntry(line);
					entryCount++;
					
					if (entryCount % 1000 == 0) {
						LOG.debug("Read " + entryCount + " entries");
					}
					
					List<LexicalEntry> entries = entryMap.get(lexicalEntry.getWord());
					if (entries==null) {
						entries = new ArrayList<LexicalEntry>();
						entryMap.put(lexicalEntry.getWord(), entries);
					}
					entries.add(lexicalEntry);
					String key = lexicalEntry.getLemma() + "|" + lexicalEntry.getLemmaComplement();
					List<LexicalEntry> entriesForLemma = lemmaEntryMap.get(key);
					if (entriesForLemma==null) {
						entriesForLemma = new ArrayList<LexicalEntry>();
						lemmaEntryMap.put(key, entriesForLemma);
					}
					entriesForLemma.add(lexicalEntry);
				}
			}
			LOG.debug("Read " + entryCount + " entries");
			scanner.close();
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
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
	public List<LexicalEntry> getEntriesForLemma(String lemma,
			String complement) {
		String key = lemma + "|" + complement;
		List<LexicalEntry> entries = this.lemmaEntryMap.get(key);
		if (entries==null)
			entries = new ArrayList<LexicalEntry>();
		return entries;
	}

	@Override
	public Set<PosTag> findPossiblePosTags(String word) {
		// Using TreeSet as set must be ordered
		Set<PosTag> posTags = new TreeSet<PosTag>();
		List<LexicalEntry> entries = this.getEntries(word);
		PosTagSet posTagSet = TalismaneSession.getPosTagSet();
		for (LexicalEntry entry : entries) {
			if (this.getPosTagMapper()==null) {
				PosTag posTag = posTagSet.getPosTag(entry.getCategory());
				posTags.add(posTag);
			} else {
				posTags.addAll(posTagMapper.getPosTags(entry));
			}
		}
		return posTags;
	}

	@Override
	public List<LexicalEntry> findLexicalEntries(String word, PosTag posTag) {
		List<LexicalEntry> entries = this.getEntries(word);
		List<LexicalEntry> entriesForPosTag = new ArrayList<LexicalEntry>();
		for (LexicalEntry entry : entries) {
			if (posTagMapper==null) {
				if (posTag.getCode().equals(entry.getCategory())) {
					entriesForPosTag.add(entry);
				}
			} else {
				Set<PosTag> posTags = posTagMapper.getPosTags(entry);
				if (posTags.contains(posTag))
					entriesForPosTag.add(entry);
			}
		}
		return entriesForPosTag;
	}

	@Override
	public List<LexicalEntry> getEntriesForLemma(String lemma,
			String complement, PosTag posTag) {
		List<LexicalEntry> entries = this.getEntriesForLemma(lemma, complement);
		List<LexicalEntry> entriesForPosTag = new ArrayList<LexicalEntry>();
		for (LexicalEntry entry : entries) {
			if (posTagMapper==null) {
				if (posTag.getCode().equals(entry.getCategory())) {
					entriesForPosTag.add(entry);
				}
			} else {
				Set<PosTag> posTags = posTagMapper.getPosTags(entry);
				if (posTags.contains(posTag))
					entriesForPosTag.add(entry);
			}
		}
		return entriesForPosTag;
	}

	@Override
	public List<LexicalEntry> getEntriesMatchingCriteria(
			LexicalEntry lexicalEntry, PosTag posTag, String gender,
			String number) {
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

	public PosTagSet getPosTagSet() {
		if (posTagSet==null) {
			posTagSet = TalismaneSession.getPosTagSet();
		}
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

}
