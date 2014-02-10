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

final class PosTaggedTokenImpl extends TaggedTokenImpl<PosTag> implements PosTaggedToken {
	private Map<String,FeatureResult<?>> featureResults = new HashMap<String, FeatureResult<?>>();

	private List<LexicalEntry> lexicalEntries = null;
	private LexicalEntry bestLexicalEntry = null;
	private boolean bestLexicalEntryLoaded = false;
	private static final DecimalFormat df = new DecimalFormat("0.0000");
	private String conllLemma = null;
	private String gender = null;
	private String number = null;
	private String tense = null;
	private String person = null;
	private String possessorNumber = null;
	private String comment = "";
	
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
		if (!this.bestLexicalEntryLoaded) {
			List<LexicalEntry> lexicalEntries = this.getLexicalEntries();
			this.bestLexicalEntry = null;
			if (lexicalEntries.size()>0) {
				this.bestLexicalEntry = lexicalEntries.get(0);
				gender = "";
				for (String oneGender : bestLexicalEntry.getGender())
					gender += oneGender;
				if (gender.length()==0) gender = null;
				number = "";
				for (String oneNumber : bestLexicalEntry.getNumber())
					number += oneNumber;
				if (number.length()==0) number = null;
				tense = "";
				for (String oneTense : bestLexicalEntry.getTense())
					tense += oneTense;
				if (tense.length()==0) tense = null;
				person = "";
				for (String onePerson : bestLexicalEntry.getPerson())
					person += onePerson;
				if (person.length()==0) person = null;
				possessorNumber = "";
				for (String onePossessorNumber : bestLexicalEntry.getPossessorNumber())
					possessorNumber += onePossessorNumber;
				if (possessorNumber.length()==0) possessorNumber = null;
			}
			this.bestLexicalEntryLoaded = true;
		}
		return this.bestLexicalEntry;
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

	public String getGender() {
		this.getLexicalEntry();
		return gender;
	}

	public String getNumber() {
		this.getLexicalEntry();
		return number;
	}

	public String getTense() {
		this.getLexicalEntry();
		return tense;
	}

	public String getPerson() {
		this.getLexicalEntry();
		return person;
	}

	public String getPossessorNumber() {
		this.getLexicalEntry();
		return possessorNumber;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	@Override
	public int getIndex() {
		return this.getToken().getIndex();
	}
	
	
}
