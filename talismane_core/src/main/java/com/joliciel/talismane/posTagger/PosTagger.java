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

import java.util.List;
import java.util.Set;

import com.joliciel.talismane.machineLearning.ClassificationObserver;
import com.joliciel.talismane.posTagger.features.PosTaggerFeature;
import com.joliciel.talismane.posTagger.features.PosTaggerRule;
import com.joliciel.talismane.sentenceAnnotators.RegexAnnotator;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenAttribute;
import com.joliciel.talismane.tokeniser.TokenSequence;

/**
 * The PosTagger's task is to add part-of-speech tags to words within a
 * sentence. More specifically, the {@link #tagSentence(TokenSequence)} method
 * takes a {@link TokenSequence} as input and produces a {@link PosTagSequence},
 * which extends a List of {@link PosTaggedToken}.
 * 
 * @author Assaf Urieli
 *
 */
public interface PosTagger {
	/**
	 * If this attribute is added to a {@link Token} via
	 * {@link Token#addAttribute(String, TokenAttribute)} (typically using a
	 * {@link RegexAnnotator}), the token in question will get the pos-tag in
	 * the attribute value assigned to it without consulting the statistical
	 * model. This is an alternative to using pos-tagger rules.
	 */
	public static final String POS_TAG_ATTRIBUTE = "posTag";

	/**
	 * If this attribute is added to a {@link Token} via
	 * {@link Token#addAttribute(String, TokenAttribute)} (typically using a
	 * {@link RegexAnnotator}), and the value of this attribute is
	 * "originalLower", then the token's lemma will be set to the original
	 * value, forced into lowercase.
	 */
	public static final String LEMMA_TYPE_ATTRIBUTE = "lemmaType";

	/**
	 * If this attribute is added to a {@link Token} via
	 * {@link Token#addAttribute(String, TokenAttribute)} (typically using a
	 * {@link RegexAnnotator}), and the value of this attribute is
	 * "originalLower", then the token's lemma will be set to the value
	 * provided.
	 */
	public static final String LEMMA_ATTRIBUTE = "lemma";

	/**
	 * Apply PosTags to the tokens in a given sentence.
	 * 
	 * @param tokenSequence
	 *            the List of tokens comprising the sentence.
	 * @return a List of TaggedToken reflecting the PosTags applied to the
	 *         tokens.
	 */
	public PosTagSequence tagSentence(TokenSequence tokenSequence);

	/**
	 * Add an analysis observer to this pos tagger.
	 */
	public void addObserver(ClassificationObserver observer);

	/**
	 * The set of features used to describe the sequence of
	 * {@link PosTaggerContextImpl} encountered while pos-tagging. These have to
	 * be identical to the features used to train the previously trained
	 * pos-tagging model.
	 */
	public Set<PosTaggerFeature<?>> getPosTaggerFeatures();

	/**
	 * @see #getPosTaggerRules()
	 */
	public void setPosTaggerRules(List<PosTaggerRule> posTaggerRules);

	/**
	 * Rules to be applied during pos-tagging, used to override the statistical
	 * model for phenomena under-represented in the training corpus.
	 */
	public List<PosTaggerRule> getPosTaggerRules();

	public PosTagger clonePosTagger();
}
