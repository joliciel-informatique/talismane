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
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.filters.FilterService;
import com.joliciel.talismane.filters.Sentence;
import com.joliciel.talismane.machineLearning.AnalysisObserver;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.tokeniser.filters.TokenFilter;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;


/**
 * A simplistic implementation of a Tokenizer, using general purpose heuristics for the tokenizing.
 * @author Assaf Urieli
 *
 */
class SimpleTokeniser implements Tokeniser {
	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(SimpleTokeniser.class);
	
	private TokeniserServiceInternal tokeniserServiceInternal;
	private FilterService filterService;
	private List<TokenSequenceFilter> preprocessingFilters = new ArrayList<TokenSequenceFilter>();
	private TokeniserDecisionFactory tokeniserDecisionFactory = new TokeniserDecisionFactory();

	private List<TokenFilter> preTokeniserFilters = new ArrayList<TokenFilter>();
	
	@Override
	public List<TokenSequence> tokenise(String text) {
		Sentence sentence = filterService.getSentence();
		sentence.setText(text);
		return this.tokenise(sentence);
	}
	
	@Override
	public List<TokenSequence> tokenise(Sentence sentence) {
		List<TokenisedAtomicTokenSequence> decisionSequences = this.tokeniseWithDecisions(sentence);
		List<TokenSequence> tokenSequences = new ArrayList<TokenSequence>();
		for (TokenisedAtomicTokenSequence decisionSequence : decisionSequences) {
			tokenSequences.add(decisionSequence.inferTokenSequence());
		}
		return tokenSequences;
	}
	
	@Override
	public List<TokenisedAtomicTokenSequence> tokeniseWithDecisions(String text) {
		Sentence sentence = filterService.getSentence();
		sentence.setText(text);
		return this.tokeniseWithDecisions(sentence);
	}
	
	@Override
	public List<TokenisedAtomicTokenSequence> tokeniseWithDecisions(Sentence sentence) {
		// Initially, separate the sentence into tokens using the separators provided
		TokenSequence tokenSequence = this.tokeniserServiceInternal.getTokenSequence(sentence, Tokeniser.SEPARATORS);
		TokenisedAtomicTokenSequence taggedTokenSequence = this.tokeniserServiceInternal.getTokenisedAtomicTokenSequence(sentence, tokenSequence.size());

		for (TokenSequenceFilter filter : preprocessingFilters)
			filter.apply(tokenSequence);
		
		for (Token token : tokenSequence) {
			Decision<TokeniserOutcome> decision;
			if (token.getText().equals("'")) {
				decision = tokeniserDecisionFactory.createDefaultDecision(TokeniserOutcome.JOIN);
			} else {
				decision = tokeniserDecisionFactory.createDefaultDecision(TokeniserOutcome.SEPARATE);
			}
			TaggedToken<TokeniserOutcome> taggedToken = this.tokeniserServiceInternal.getTaggedToken(token, decision);
			taggedTokenSequence.add(taggedToken);
		}
		
		List<TokenisedAtomicTokenSequence> sequences = new ArrayList<TokenisedAtomicTokenSequence>();
		sequences.add(taggedTokenSequence);
		
		for (TokenisedAtomicTokenSequence sequence : sequences) {
			TokenSequence newTokenSequence = sequence.inferTokenSequence();
			for (TokenSequenceFilter tokenFilter : this.preprocessingFilters) {
				tokenFilter.apply(newTokenSequence);
			}
		}
		return sequences;
	}
	
	public TokeniserServiceInternal getTokeniserServiceInternal() {
		return tokeniserServiceInternal;
	}
	public void setTokeniserServiceInternal(
			TokeniserServiceInternal tokeniserServiceInternal) {
		this.tokeniserServiceInternal = tokeniserServiceInternal;
	}
	
	/**
	 * Filters to be applied to the atoms, prior to tokenising.
	 * @return
	 */
	public List<TokenSequenceFilter> getTokenSequenceFilters() {
		return preprocessingFilters;
	}

	public void setTokenSequenceFilters(List<TokenSequenceFilter> tokenFilters) {
		this.preprocessingFilters = tokenFilters;
	}
	
	public void addTokenSequenceFilter(TokenSequenceFilter tokenFilter) {
		this.preprocessingFilters.add(tokenFilter);
	}
	
	public void addObserver(AnalysisObserver<TokeniserOutcome> observer) {
		// nothing to do here
	}
	
	public List<TokenFilter> getTokenFilters() {
		return preTokeniserFilters;
	}

	public void addTokenFilter(TokenFilter filter) {
		this.preTokeniserFilters.add(filter);
	}

	public FilterService getFilterService() {
		return filterService;
	}

	public void setFilterService(FilterService filterService) {
		this.filterService = filterService;
	}
	
	
}
