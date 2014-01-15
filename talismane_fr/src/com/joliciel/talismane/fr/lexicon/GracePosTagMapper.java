package com.joliciel.talismane.fr.lexicon;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.joliciel.talismane.lexicon.LexicalEntry;
import com.joliciel.talismane.lexicon.PosTagMapper;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagSet;

public class GracePosTagMapper implements PosTagMapper {
	private PosTagSet posTagSet;
	private Map<String,PosTag> posTagMap;

	public GracePosTagMapper(PosTagSet posTagSet, Map<String,PosTag> posTagMap) {
		super();
		this.posTagSet = posTagSet;
		this.posTagMap = posTagMap;
	}

	@Override
	public PosTagSet getPosTagSet() {
		return posTagSet;
	}

	@Override
	public Set<PosTag> getPosTags(LexicalEntry lexicalEntry) {
		Set<PosTag> posTags = new HashSet<PosTag>();
		PosTag myPosTag = posTagMap.get(lexicalEntry.getMorphology());
		posTags.add(myPosTag);
		return posTags;
	}

	public Map<String, PosTag> getPosTagMap() {
		return posTagMap;
	}

}
