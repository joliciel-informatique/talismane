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
package com.joliciel.talismane.tokeniser;

import java.util.List;
import java.util.regex.Pattern;

import com.joliciel.talismane.tokeniser.filters.TokenFilter;
import com.joliciel.talismane.utils.AnalysisObserver;


/**
 * A Tokenizer's task is to split a sentence up into tokens.
 * @author Assaf Urieli
 *
 */
public interface Tokeniser {
	/**
	 * A list of possible separators for tokens.
	 */
	public static final Pattern SEPARATORS = Pattern.compile("[\\s\\p{Punct}«»_‒–—―‛“”„‟′″‴‹›‘’‚*]");
	
	/**
	 * Tokenise a given sentence. More specifically, return up to N most likely tokeniser decision sequences,
	 * each of which breaks up the sentence into a different a list of tokens.
	 * Note: we assume duplicate white-space has already been removed from the sentence prior to calling the tokenise method,
	 * e.g. multiple spaces have been replaced by a single space.
	 * @param sentence the sentence to be tokenised
	 * @return a List of up to <i>n</i> TokeniserDecisionTagSequence, ordered from most probable to least probable
	 */
	public List<TokeniserDecisionTagSequence> tokeniseWithDecisions(String sentence);
	
	public List<TokenSequence> tokenise(String sentence);
	
	/**
	 * Filters to be applied to the atomic token sequences, prior to tokenising.
	 * Note that these filters will be applied to the token sequences produced by the tokeniser as well.
	 * @return
	 */
	public List<TokenFilter> getTokenFilters();

	public void setTokenFilters(List<TokenFilter> tokenFilters);
	
	public void addTokenFilter(TokenFilter tokenFilter);
	
	public void addObserver(AnalysisObserver observer);
}
