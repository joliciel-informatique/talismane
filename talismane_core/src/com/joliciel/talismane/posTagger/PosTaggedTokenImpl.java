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
package com.joliciel.talismane.posTagger;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.lexicon.LexicalEntry;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.features.Feature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.tokeniser.TaggedTokenImpl;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.utils.CoNLLFormatter;

class PosTaggedTokenImpl extends TaggedTokenImpl<PosTag> implements PosTaggedToken {
	private Map<String,FeatureResult<?>> featureResults = new HashMap<String, FeatureResult<?>>();

	private List<LexicalEntry> lexicalEntries = null;
	private static final DecimalFormat df = new DecimalFormat("0.0000");
	private String conllLemma = null;
	
	PosTaggedTokenImpl(PosTaggedTokenImpl taggedTokenToClone) {
		super(taggedTokenToClone);
		this.featureResults = taggedTokenToClone.featureResults;
		this.lexicalEntries = taggedTokenToClone.lexicalEntries;
	}
	
	public PosTaggedTokenImpl(Token token, Decision<PosTag> decision) {
		super(token, decision);
	}

	@Override
	public List<LexicalEntry> getLexicalEntries() {
		if (lexicalEntries==null) {
			lexicalEntries = TalismaneSession.getLexicon().findLexicalEntries(this.getToken().getText(), this.getTag());
		}
		return lexicalEntries;
	}

	public void setLexicalEntries(List<LexicalEntry> lexicalEntries) {
		this.lexicalEntries = lexicalEntries;
	}

	@Override
	public String toString() {
		return this.getToken().getText() + "|" + this.getTag() + "|" + this.getToken().getIndex() + "| prob=" + df.format(this.getDecision().getProbability());
	}

	@Override
	public LexicalEntry getLexicalEntry() {
		List<LexicalEntry> lexicalEntries = this.getLexicalEntries();
		LexicalEntry bestLexicalEntry = null;
		if (lexicalEntries.size()>0) {
			bestLexicalEntry = lexicalEntries.get(0);
		}
		return bestLexicalEntry;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T, Y> FeatureResult<Y> getResultFromCache(Feature<T, Y> feature, RuntimeEnvironment env) {
		FeatureResult<Y> result = null;
		
		String key = feature.getName() + env.getKey();
		if (this.featureResults.containsKey(key)) {
			result = (FeatureResult<Y>) this.featureResults.get(key);
		}
		return result;
	}

	@Override
	public <T, Y> void putResultInCache(Feature<T, Y> feature,
			FeatureResult<Y> featureResult, RuntimeEnvironment env) {
		String key = feature.getName() + env.getKey();
		this.featureResults.put(key, featureResult);	
	}

	@Override
	public PosTaggedToken getPosTaggedToken() {
		return this;
	}

	@Override
	public PosTaggedToken clonePosTaggedToken() {
		PosTaggedTokenImpl posTaggedToken = new PosTaggedTokenImpl(this);
		return posTaggedToken;
	}

	@Override
	public String getLemmaForCoNLL() {
		if (conllLemma==null) {
			String lemma = "";
			LexicalEntry lexicalEntry = this.getLexicalEntry();
			if (lexicalEntry!=null) {
				lemma = lexicalEntry.getLemma();
			}
			conllLemma = CoNLLFormatter.toCoNLL(lemma);
		}
		return conllLemma;
	}
}
