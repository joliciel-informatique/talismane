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
package com.joliciel.talismane.tokeniser;

import java.util.List;
import java.util.regex.Pattern;

import com.joliciel.talismane.filters.Sentence;
import com.joliciel.talismane.machineLearning.ClassificationObserver;
import com.joliciel.talismane.tokeniser.filters.TokenFilter;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;

/**
 * A Tokeniser splits a sentence up into tokens (parsing units).
 * 
 * @author Assaf Urieli
 *
 */
public interface Tokeniser {
	public static enum TokeniserType {
		simple, pattern
	};

	/**
	 * A list of possible separators for tokens.
	 */
	public static final Pattern SEPARATORS = Pattern.compile("[\\s\\p{Punct}«»_‒–—―‛“”„‟′″‴‹›‘’‚*\ufeff]", Pattern.UNICODE_CHARACTER_CLASS);

	/**
	 * Tokenise a given sentence. More specifically, return up to N most likely
	 * tokeniser decision sequences, each of which breaks up the sentence into a
	 * different a list of tokens. Note: we assume duplicate white-space has
	 * already been removed from the sentence prior to calling the tokenise
	 * method, e.g. multiple spaces have been replaced by a single space.
	 * 
	 * @param text
	 *            the sentence to be tokenised
	 * @return a List of up to <i>n</i> TokeniserDecisionTagSequence, ordered
	 *         from most probable to least probable
	 */
	public List<TokenisedAtomicTokenSequence> tokeniseWithDecisions(String text);

	/**
	 * Similar to {@link #tokenise(String)}, but returns only the best token
	 * sequence.
	 */
	public TokenSequence tokeniseText(String text);

	/**
	 * Similar to {@link #tokenise(Sentence)}, but returns only the best token
	 * sequence.
	 */
	public TokenSequence tokeniseSentence(Sentence sentence);

	/**
	 * Similar to {@link #tokeniseWithDecisions(String)}, but returns the token
	 * sequences inferred from the decisions, rather than the list of decisions
	 * themselves.
	 */
	public List<TokenSequence> tokenise(String text);

	/**
	 * Similar to {@link #tokeniseWithDecisions(String)}, but the text to be
	 * tokenised is contained within a Sentence object.
	 */
	public List<TokenisedAtomicTokenSequence> tokeniseWithDecisions(Sentence sentence);

	/**
	 * Similar to {@link #tokeniseWithDecisions(Sentence)}, but returns the
	 * token sequences inferred from the decisions, rather than the list of
	 * decisions themselves.
	 */
	public List<TokenSequence> tokenise(Sentence sentence);

	/**
	 * Filters to be applied to the atomic token sequences, prior to tokenising.
	 * These filters will either add empty tokens at given places, or change the
	 * token text. Note that these filters will be applied to the token
	 * sequences produced by the tokeniser as well.
	 */
	public List<TokenSequenceFilter> getTokenSequenceFilters();

	/**
	 * See {@link #getTokenSequenceFilters()}.
	 */
	public void addTokenSequenceFilter(TokenSequenceFilter tokenSequenceFilter);

	/**
	 * Filters to be applied prior to breaking the sentence up into atomic token
	 * sequences - these filters will mark certain portions of the sentence as
	 * entire tokens, and the tokeniser will not take any decisions inside
	 * these. It still may join them to other atomic tokens, to create larger
	 * tokens.
	 */
	public List<TokenFilter> getTokenFilters();

	/**
	 * See {@link #getTokenFilters()}.
	 */
	public void addTokenFilter(TokenFilter filter);

	public void addObserver(ClassificationObserver observer);

	public Tokeniser cloneTokeniser();

}
