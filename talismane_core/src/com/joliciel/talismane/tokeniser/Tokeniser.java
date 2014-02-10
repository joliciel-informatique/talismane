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
 * @author Assaf Urieli
 *
 */
public interface Tokeniser {
	/**
	 * A list of possible separators for tokens.
	 */
	public static final Pattern SEPARATORS = Pattern.compile("[\\s\\p{Punct}«»_‒–—―‛“”„‟′″‴‹›‘’‚*\u00a0\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2007\u2008\u2009\u200a\u200b\u202f\u205f\u3000\ufeff]");
	
	/**
	 * Tokenise a given sentence. More specifically, return up to N most likely tokeniser decision sequences,
	 * each of which breaks up the sentence into a different a list of tokens.
	 * Note: we assume duplicate white-space has already been removed from the sentence prior to calling the tokenise method,
	 * e.g. multiple spaces have been replaced by a single space.
	 * @param text the sentence to be tokenised
	 * @return a List of up to <i>n</i> TokeniserDecisionTagSequence, ordered from most probable to least probable
	 */
	public List<TokenisedAtomicTokenSequence> tokeniseWithDecisions(String text);
	
	/**
	 * Similar to tokeniseWithDecisions(String), but returns the token sequences inferred from
	 * the decisions, rather than the list of decisions themselves.
	 * @param text
	 * @return
	 */
	public List<TokenSequence> tokenise(String text);
	
	/**
	 * Similar to tokeniseWithDecisions(String), but the text to be tokenised is contained
	 * within a Sentence object.
	 * @param sentence
	 * @return
	 */
	public List<TokenisedAtomicTokenSequence> tokeniseWithDecisions(Sentence sentence);
	
	/**
	 * Similar to tokeniseWithDecisions(Sentence), but returns the token sequences inferred from
	 * the decisions, rather than the list of decisions themselves.
	 * @return
	 */
	public List<TokenSequence> tokenise(Sentence sentence);
	
	/**
	 * Filters to be applied to the atomic token sequences, prior to tokenising.
	 * These filters will either add empty tokens at given places, or change the token text.
	 * Note that these filters will be applied to the token sequences produced by the tokeniser as well.
	 * @return
	 */
	public List<TokenSequenceFilter> getTokenSequenceFilters();
	
	/**
	 * See getTokenSequenceFilters().
	 * @param tokenSequenceFilter
	 */
	public void addTokenSequenceFilter(TokenSequenceFilter tokenSequenceFilter);
	
	/**
	 * Filters to be applied prior to breaking the sentence up into atomic token sequences -
	 * these filters will mark certain portions of the sentence as entire tokens, and the tokeniser
	 * will not take any decisions inside these. It still may join them to other atomic tokens,
	 * to create larger tokens.
	 * @return
	 */
	public List<TokenFilter> getTokenFilters();

	/**
	 * See getTokenFilters().
	 * @param filter
	 */
	public void addTokenFilter(TokenFilter filter);

	public void addObserver(ClassificationObserver<TokeniserOutcome> observer);

}
