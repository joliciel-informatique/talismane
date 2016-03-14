package com.joliciel.talismane.lexicon;

import gnu.trove.map.hash.THashMap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.NeedsTalismaneSession;
import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.posTagger.UnknownPosTagException;

/**
 * <p>A lexicon contained in a text file with one line per entry.</p>
 * <p>All empty lines are lines starting with a # will be ignored.</p>
 * <p>All other lines are assumed to be lexicon entries, to be interpreted using the provided LexiconEntryReader</p>
 * 
 * <p>If the posTagMapper is null, assumes that the lexical entry categories are already set to pos-tag codes for the current
 * posTagSet.</p>
 * @author Assaf Urieli
 *
 */
public class LexiconFile implements PosTaggerLexicon, Serializable, NeedsTalismaneSession, LexicalEntryFactory {
	private static final long serialVersionUID = 3L;
	private static final Log LOG = LogFactory.getLog(LexiconFile.class);
	private Map<String,List<LexicalEntry>> entryMap = new THashMap<String, List<LexicalEntry>>();
	private Map<String,List<LexicalEntry>> lemmaEntryMap = new THashMap<String, List<LexicalEntry>>();
	private TalismaneSession talismaneSession;
	private PosTagSet posTagSet;
	private PosTagMapper posTagMapper;
	private String name;
	private transient LexicalEntryReader reader = null;
	private transient List<List<String>> exclusions;
	private transient List<String> exclusionAttributes;
	private transient Set<String> categories;
	
	private static final int INITIAL_CAPACITY = 1000;
	private Map<LexicalAttribute,Map<String,Byte>> attributeStringToByteMap = new THashMap<LexicalAttribute, Map<String,Byte>>(INITIAL_CAPACITY, 0.75f);
	private Map<LexicalAttribute,Map<Byte,String>> attributeByteToStringMap = new THashMap<LexicalAttribute, Map<Byte,String>>(INITIAL_CAPACITY, 0.75f);
	private Map<String,LexicalAttribute> nameToAttributeMap = new THashMap<String, LexicalAttribute>();
	private int otherAttributeIndex = 0;
	private List<LexicalAttribute> uniqueKeyAttributes;
	
	private transient Scanner lexiconScanner;
	
	public LexiconFile(String name, Scanner lexiconScanner, RegexLexicalEntryReader reader) {
		this.name = name;
		this.lexiconScanner = lexiconScanner;
		this.reader = reader;
		reader.setLexicalEntryFactory(this);
	}
	
	public void load() {
		Map<String, List<List<String>>> exclusionMap = null;
		if (this.exclusions!=null) {
			exclusionMap = new HashMap<String, List<List<String>>>();
			for (List<String> exclusion : exclusions) {
				List<List<String>> myExclusions = exclusionMap.get(exclusion.get(0));
				if (myExclusions==null) {
					myExclusions = new ArrayList<List<String>>();
					exclusionMap.put(exclusion.get(0), myExclusions);
				}
				myExclusions.add(exclusion);
			}
		}
		int entryCount = 0;
		int categoryExcludeCount = 0;
		int exclusionCount = 0;
		int duplicateCount = 0;
		int addedCount = 0;
		while (lexiconScanner.hasNextLine()) {
			String line = lexiconScanner.nextLine();
			if (line.length()>0 && !line.startsWith("#")) {
				LexicalEntry lexicalEntry = reader.readEntry(line);
				
				entryCount++;
				
				if (entryCount % 1000 == 0) {
					LOG.debug("Read " + entryCount + " entries");
				}
				
				if (this.categories!=null) {
					if (!this.categories.contains(lexicalEntry.getCategory())) {
						categoryExcludeCount++;
						continue;
					}
				}
				
				boolean exclude = false;
				if (exclusionMap!=null && this.exclusionAttributes!=null) {
					String firstAttribute = this.exclusionAttributes.get(0);
					String myFirstAttribute = lexicalEntry.getAttribute(firstAttribute);
					if (exclusionMap.containsKey(myFirstAttribute)) {
						for (List<String> exclusion : exclusionMap.get(myFirstAttribute)) {
							boolean foundExclusion = true;
							for (int i=1; i<this.exclusionAttributes.size(); i++) {
								if (!exclusion.get(i).equals(lexicalEntry.getAttribute(this.exclusionAttributes.get(i)))) {
									foundExclusion = false;
									break;
								}
							} // next attribute in exclusion
							if (foundExclusion) {
								exclude = true;
								break;
							} // do we have a match
						} // next exclusion
					} // first attribute is on exclusion map
				} // have exclusions
				
				if (exclude) {
					LOG.debug("Excluding " + lexicalEntry.toString());
					exclusionCount++;
					continue;
				}
	
				List<LexicalEntry> entries = entryMap.get(lexicalEntry.getWord());
				if (entries==null) {
					entries = new ArrayList<LexicalEntry>();
					entryMap.put(lexicalEntry.getWord(), entries);
				}
				
				boolean addEntry = true;
				if (uniqueKeyAttributes!=null) {
					for (LexicalEntry lexicalEntry2 : entries) {
						boolean fullMatch = true;
						for (LexicalAttribute lexicalAttribute : uniqueKeyAttributes) {
							boolean entry1Has = lexicalEntry.hasAttribute(lexicalAttribute);
							boolean entry2Has = lexicalEntry2.hasAttribute(lexicalAttribute);
							if ((!entry1Has && !entry2Has) ||
									(entry1Has && entry2Has
									&& lexicalEntry.getAttribute(lexicalAttribute.name()).equals(lexicalEntry2.getAttribute(lexicalAttribute.name())))) {
								// do nothing, we have a match
							} else {
								fullMatch = false;
								break;
							}
						}
						if (fullMatch) {
							addEntry = false;
							duplicateCount++;
							break;
						}
					}
				}
				
				if (addEntry) {
					addedCount++;
					entries.add(lexicalEntry);
					String key = lexicalEntry.getLemma();
					List<LexicalEntry> entriesForLemma = lemmaEntryMap.get(key);
					if (entriesForLemma==null) {
						entriesForLemma = new ArrayList<LexicalEntry>();
						lemmaEntryMap.put(key, entriesForLemma);
					}
					entriesForLemma.add(lexicalEntry);
				}
			} // valid line
		} // next line
		
		LOG.debug("Read " + entryCount + " entries");
		LOG.debug("Skipped " + categoryExcludeCount + " entries for categories");
		LOG.debug("Skipped " + exclusionCount + " entries for exclusions");
		LOG.debug("Skipped " + duplicateCount + " entries for duplicates");
		LOG.debug("Added " + addedCount + " entries");
	}

	@Override
	public List<LexicalEntry> getEntries(String word) {
		List<LexicalEntry> entries = this.entryMap.get(word);
		if (entries==null)
			entries = new ArrayList<LexicalEntry>();
		return entries;
	}

	@Override
	public List<LexicalEntry> getEntriesForLemma(String lemma) {
		List<LexicalEntry> entries = this.lemmaEntryMap.get(lemma);
		if (entries==null)
			entries = new ArrayList<LexicalEntry>();
		return entries;
	}

	@Override
	public Set<PosTag> findPossiblePosTags(String word) {
		// Using TreeSet as set must be ordered
		Set<PosTag> posTags = new TreeSet<PosTag>();
		List<LexicalEntry> entries = this.getEntries(word);
		PosTagSet posTagSet = this.getPosTagSet();
		for (LexicalEntry entry : entries) {
			if (this.getPosTagMapper()==null) {
				try {
					PosTag posTag = posTagSet.getPosTag(entry.getCategory());
					posTags.add(posTag);
				} catch (UnknownPosTagException upte) {
					throw new TalismaneException("Unknown postag " + upte.getPosTagCode() + " for word: " + word + " in lexicon " + this.name);
				}
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
	public List<LexicalEntry> getEntriesForLemma(String lemma, PosTag posTag) {
		List<LexicalEntry> entries = this.getEntriesForLemma(lemma);
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
			lemmaEntries = this.getEntriesForLemma(lexicalEntry.getLemma(), posTag);
		else
			lemmaEntries = this.getEntriesForLemma(lexicalEntry.getLemma());
		List<LexicalEntry> entryList = new ArrayList<LexicalEntry>();
		for (LexicalEntry lemmaEntry : lemmaEntries) {
			if ((number==null || number.length()==0 || lemmaEntry.getNumber().contains(number))
					&& (gender==null || gender.length()==0 || lemmaEntry.getGender().contains(gender))) {
				entryList.add(lemmaEntry);
			}
		}
		
		return entryList;
	}

	public PosTagSet getPosTagSet() {
		if (posTagSet==null && talismaneSession!=null) {
			posTagSet = talismaneSession.getPosTagSet();
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

	public TalismaneSession getTalismaneSession() {
		return talismaneSession;
	}

	public void setTalismaneSession(TalismaneSession talismaneSession) {
		this.talismaneSession = talismaneSession;
	}

	public String getName() {
		return name;
	}

	public List<List<String>> getExclusions() {
		return exclusions;
	}

	/**
	 * A list of specific items to exclude when loading the lexicon.
	 * Each internal list is a list of attributes, all of which must match for the provided
	 * {@link #getExclusionAttributes()} for the entry to get excluded.
	 * @return
	 */
	public void setExclusions(List<List<String>> exclusions) {
		this.exclusions = exclusions;
	}

	public List<String> getExclusionAttributes() {
		return exclusionAttributes;
	}

	/**
	 * A list of attributes to match based on the exclusion list in
	 * {@link #getExclusions()}.
	 * @param exclusionAttributes
	 */
	public void setExclusionAttributes(List<String> exclusionAttributes) {
		this.exclusionAttributes = exclusionAttributes;
	}

	/**
	 * A set of categories to include when loading.
	 * If null, all categories are loaded.
	 * @return
	 */
	public Set<String> getCategories() {
		return categories;
	}

	public void setCategories(Set<String> categories) {
		this.categories = categories;
	}
	
	public byte getOrCreateAttributeCode(LexicalAttribute attribute, String value) {
		Map<String,Byte> attributeCodes = attributeStringToByteMap.get(attribute);
		Map<Byte,String> attributeValues = attributeByteToStringMap.get(attribute);
		byte code = 0;
		if (attributeCodes==null) {
			attributeCodes = new THashMap<String, Byte>();
			attributeStringToByteMap.put(attribute, attributeCodes);
			attributeValues = new THashMap<Byte, String>();
			attributeByteToStringMap.put(attribute, attributeValues);
			
		}
		
		Byte codeObj = attributeCodes.get(value);
		code = codeObj==null ? 0 : codeObj.byteValue();
		
		if (code==0) {
			code = (byte) (attributeCodes.size()+1);
			attributeCodes.put(value, code);
			attributeValues.put(code, value);
		}
		
		return code;
	}
	
	public byte getAttributeCode(LexicalAttribute attribute, String value) {
		Map<String,Byte> attributeCodes = attributeStringToByteMap.get(attribute);
		byte code = 0;
		if (attributeCodes!=null) {
			Byte codeObj = attributeCodes.get(value);
			code = codeObj==null ? 0 : codeObj.byteValue();
		}
		return code;
	}
	
	public String getAttributeValue(LexicalAttribute attribute, byte code) {
		Map<Byte,String> attributeValues = attributeByteToStringMap.get(attribute);
		String value = null;
		if (attributeValues!=null) {
			value = attributeValues.get(code);
		}
		if (value==null)
			value = "";
		return value;
	}
	
	public LexicalAttribute getAttributeForName(String name) {
		LexicalAttribute attribute = this.nameToAttributeMap.get(name);
		if (attribute==null) {
			otherAttributeIndex++;
			attribute = LexicalAttribute.valueOf("OtherAttribute" + otherAttributeIndex);
			nameToAttributeMap.put(name, attribute);
		}
		return attribute;
	}

	@Override
	public WritableLexicalEntry newLexicalEntry() {
		CompactLexicalEntry lexicalEntry = new CompactLexicalEntry(this);
		return lexicalEntry;
	}

	/**
	 * A list of attributes defining the uniqueness of a given entry.
	 * If another entry exists with the identical set of attributes, it won't get added.
	 * @return
	 */
	public List<LexicalAttribute> getUniqueKeyAttributes() {
		return uniqueKeyAttributes;
	}

	public void setUniqueKeyAttributes(List<LexicalAttribute> uniqueKeyAttributes) {
		this.uniqueKeyAttributes = uniqueKeyAttributes;
	}

	@Override
	public Iterator<LexicalEntry> getAllEntries() {
		return new Iterator<LexicalEntry>() {
			Iterator<String> keys = entryMap.keySet().iterator();
			Iterator<LexicalEntry> entries = null;
			
			@Override
			public boolean hasNext() {
				while (entries==null) {
					String key = null;
					if (keys.hasNext()) {
						key = keys.next();
					} else {
						return false;
					}
					
					entries = entryMap.get(key).iterator();
					if (!entries.hasNext())
						entries=null;
				}
				return true;
			}

			@Override
			public LexicalEntry next() {
				LexicalEntry entry = null;
				if (this.hasNext()) {
					entry = entries.next();
					if (!entries.hasNext())
						entries = null;
				}
				return entry;
			}

			@Override
			public void remove() {
				throw new TalismaneException("remove not supported");
			}
		};
	}
	
}
