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
import com.joliciel.talismane.lexicon.LexicalAttribute;
import com.joliciel.talismane.lexicon.LexicalEntry;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.features.Feature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.HasFeatureCache;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.posTagger.features.PosTaggedTokenWrapper;
import com.joliciel.talismane.tokeniser.StringAttribute;
import com.joliciel.talismane.tokeniser.TaggedToken;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.utils.CoNLLFormatter;

/**
 * A token with a postag tagged onto it.
 * 
 * @author Assaf Urieli
 *
 */
public class PosTaggedToken extends TaggedToken<PosTag> implements PosTaggedTokenWrapper, HasFeatureCache, PosTaggerContext {
	private Map<String, FeatureResult<?>> featureResults = new HashMap<String, FeatureResult<?>>();

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
	private PosTagSequence posTagSequence;

	private TalismaneSession talismaneSession;

	PosTaggedToken(PosTaggedToken taggedTokenToClone) {
		super(taggedTokenToClone);
		this.featureResults = taggedTokenToClone.featureResults;
		this.lexicalEntries = taggedTokenToClone.lexicalEntries;

		this.talismaneSession = taggedTokenToClone.talismaneSession;
	}

	/**
	 * Construct a pos-tagged token for a given token and given decision - the
	 * {@link Decision#getOutcome()} must be a valid {@link PosTag#getCode()}
	 * from the current {@link PosTagSet}.
	 * 
	 * @param token
	 *            the token to be tagged
	 * @param decision
	 *            the decision used to tag it
	 */
	public PosTaggedToken(Token token, Decision decision, TalismaneSession talismaneSession) {
		super(token, decision, talismaneSession.getPosTagSet().getPosTag(decision.getOutcome()));
		this.talismaneSession = talismaneSession;
	}

	/**
	 * All lexical entries for this token/postag combination.
	 */
	public List<LexicalEntry> getLexicalEntries() {
		if (lexicalEntries == null) {
			lexicalEntries = this.getTalismaneSession().getMergedLexicon().findLexicalEntries(this.getToken().getText(), this.getTag());
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

	/**
	 * The "best" lexical entry for this token/postag combination if one exists,
	 * or null otherwise.
	 */
	public LexicalEntry getLexicalEntry() {
		if (!this.bestLexicalEntryLoaded) {
			List<LexicalEntry> lexicalEntries = this.getLexicalEntries();
			this.bestLexicalEntry = null;
			if (lexicalEntries.size() > 0) {
				this.bestLexicalEntry = lexicalEntries.get(0);
				gender = "";
				if (bestLexicalEntry.hasAttribute(LexicalAttribute.Gender))
					for (String oneGender : bestLexicalEntry.getGender())
						gender += oneGender;
				if (gender.length() == 0)
					gender = null;
				number = "";
				if (bestLexicalEntry.hasAttribute(LexicalAttribute.Number))
					for (String oneNumber : bestLexicalEntry.getNumber())
						number += oneNumber;
				if (number.length() == 0)
					number = null;
				tense = "";
				if (bestLexicalEntry.hasAttribute(LexicalAttribute.Tense))
					for (String oneTense : bestLexicalEntry.getTense())
						tense += oneTense;
				if (tense.length() == 0)
					tense = null;
				person = "";
				if (bestLexicalEntry.hasAttribute(LexicalAttribute.Person))
					for (String onePerson : bestLexicalEntry.getPerson())
						person += onePerson;
				if (person.length() == 0)
					person = null;
				possessorNumber = "";
				if (bestLexicalEntry.hasAttribute(LexicalAttribute.PossessorNumber))
					for (String onePossessorNumber : bestLexicalEntry.getPossessorNumber())
						possessorNumber += onePossessorNumber;
				if (possessorNumber.length() == 0)
					possessorNumber = null;
			}
			this.bestLexicalEntryLoaded = true;
		}
		return this.bestLexicalEntry;
	}

	@Override
	@SuppressWarnings("unchecked")

	public <T, Y> FeatureResult<Y> getResultFromCache(Feature<T, Y> feature, RuntimeEnvironment env) {
		FeatureResult<Y> result = null;

		String key = feature.getName() + env.getKey();
		if (this.featureResults.containsKey(key)) {
			result = (FeatureResult<Y>) this.featureResults.get(key);
		}
		return result;
	}

	@Override
	public <T, Y> void putResultInCache(Feature<T, Y> feature, FeatureResult<Y> featureResult, RuntimeEnvironment env) {
		String key = feature.getName() + env.getKey();
		this.featureResults.put(key, featureResult);
	}

	@Override
	public PosTaggedToken getPosTaggedToken() {
		return this;
	}

	public PosTaggedToken clonePosTaggedToken() {
		PosTaggedToken posTaggedToken = new PosTaggedToken(this);
		return posTaggedToken;
	}

	/**
	 * The lemma of the "best" lexical entry as encoded for the CoNLL output
	 * format.
	 */
	public String getLemmaForCoNLL() {
		if (conllLemma == null) {
			String lemma = "";
			String lemmaType = null;
			StringAttribute lemmaTypeAttribute = (StringAttribute) this.getToken().getAttributes().get(PosTagger.LEMMA_TYPE_ATTRIBUTE);
			if (lemmaTypeAttribute != null)
				lemmaType = lemmaTypeAttribute.getValue();
			String explicitLemma = null;
			StringAttribute explicitLemmaAttribute = (StringAttribute) this.getToken().getAttributes().get(PosTagger.LEMMA_ATTRIBUTE);
			if (explicitLemmaAttribute != null)
				explicitLemma = explicitLemmaAttribute.getValue();
			if (explicitLemma != null) {
				lemma = explicitLemma;
			} else if (lemmaType != null && lemmaType.equals("originalLower")) {
				lemma = this.getToken().getOriginalText().toLowerCase(this.getTalismaneSession().getLocale());
			} else if (this.getToken().getText().equals(this.getToken().getOriginalText())) {
				LexicalEntry lexicalEntry = this.getLexicalEntry();
				if (lexicalEntry != null) {
					lemma = lexicalEntry.getLemma();
				}
			} else {
				LexicalEntry lexicalEntry = null;
				List<LexicalEntry> entries = this.getTalismaneSession().getMergedLexicon().findLexicalEntries(this.getToken().getOriginalText(), this.getTag());
				if (entries.size() > 0)
					lexicalEntry = entries.get(0);
				if (lexicalEntry == null) {
					entries = this.getTalismaneSession().getMergedLexicon()
							.findLexicalEntries(this.getToken().getOriginalText().toLowerCase(this.getTalismaneSession().getLocale()), this.getTag());
					if (entries.size() > 0)
						lexicalEntry = entries.get(0);
				}
				if (lexicalEntry == null)
					lexicalEntry = this.getLexicalEntry();

				if (lexicalEntry != null)
					lemma = lexicalEntry.getLemma();
			}
			conllLemma = CoNLLFormatter.toCoNLL(lemma);

		}
		return conllLemma;
	}

	/**
	 * A list of possible (language-specific) genders for this entry. In French,
	 * this will include entries such as "masculine", "feminine". If gender
	 * unknown, will return null.
	 */
	public String getGender() {
		this.getLexicalEntry();
		return gender;
	}

	/**
	 * A list of possible (language-specific) numbers for this entry. In French,
	 * this will include entries such as "singular", "plural". If number
	 * unknown, will return null.
	 */
	public String getNumber() {
		this.getLexicalEntry();
		return number;
	}

	/**
	 * A list of possible (language-specific) tenses/moods for this entry, when
	 * the entry is a verb. If tense unknown, will return null.
	 */
	public String getTense() {
		this.getLexicalEntry();
		return tense;
	}

	/**
	 * A list of possible persons for this entry. In French, this will inlude
	 * entries such as "1st person", "2nd person", "3rd person". If person
	 * unknown, will return null.
	 */
	public String getPerson() {
		this.getLexicalEntry();
		return person;
	}

	/**
	 * A list of possible (language-specific) numbers for the possessor in this
	 * entry, when the entry is a possessive determinant or pronoun. If
	 * possessor number unknown, will return null.
	 */
	public String getPossessorNumber() {
		this.getLexicalEntry();
		return possessorNumber;
	}

	/**
	 * A comment regarding this pos-tag annotation.
	 */
	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	/**
	 * This token's index in the containing sentence.
	 */
	public int getIndex() {
		return this.getToken().getIndex();
	}

	/**
	 * The sequence containing this pos-tagged token.
	 */
	public PosTagSequence getPosTagSequence() {
		return posTagSequence;
	}

	public void setPosTagSequence(PosTagSequence posTagSequence) {
		this.posTagSequence = posTagSequence;
	}

	@Override
	public PosTagSequence getHistory() {
		return this.posTagSequence;
	}

	public TalismaneSession getTalismaneSession() {
		return talismaneSession;
	}

	public void setTalismaneSession(TalismaneSession talismaneSession) {
		this.talismaneSession = talismaneSession;
	}

}
