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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.filters.FilterService;
import com.joliciel.talismane.filters.Sentence;
import com.joliciel.talismane.machineLearning.ClassificationObserver;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.tokeniser.filters.TokenFilter;
import com.joliciel.talismane.tokeniser.filters.TokenPlaceholder;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;
import com.joliciel.talismane.utils.PerformanceMonitor;


/**
 * A simplistic implementation of a Tokenizer, using only TokenFilters and default decisions.
 * @author Assaf Urieli
 *
 */
class SimpleTokeniser implements Tokeniser {
	private static final Log LOG = LogFactory.getLog(SimpleTokeniser.class);
	private static final PerformanceMonitor MONITOR = PerformanceMonitor.getMonitor(SimpleTokeniser.class);
	
	private TokeniserServiceInternal tokeniserService;
	private FilterService filterService;
	private TokeniserDecisionFactory tokeniserDecisionFactory = new TokeniserDecisionFactory();
	
	private List<TokenSequenceFilter> tokenSequenceFilters = new ArrayList<TokenSequenceFilter>();
	
	private List<TokenFilter> tokenFilters = new ArrayList<TokenFilter>();

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
		MONITOR.startTask("tokeniseWithDecisions");
		try {
			// apply any pre-tokenisation decisions via filters
			// we only want one placeholder per start index - the first one that gets added
			Map<Integer,TokenPlaceholder> placeholderMap = new HashMap<Integer, TokenPlaceholder>();
			for (TokenFilter tokenFilter : this.tokenFilters) {
				Set<TokenPlaceholder> myPlaceholders = tokenFilter.apply(sentence.getText());
				for (TokenPlaceholder placeholder : myPlaceholders) {
					if (!placeholderMap.containsKey(placeholder.getStartIndex())) {
						placeholderMap.put(placeholder.getStartIndex(), placeholder);
					}
				}
				if (LOG.isTraceEnabled()) {
					if (myPlaceholders.size()>0) {
						LOG.trace("TokenFilter: " + tokenFilter);
						LOG.trace("placeholders: " + myPlaceholders);
					}
				}
			}
			
			Set<TokenPlaceholder> placeholders = new HashSet<TokenPlaceholder>(placeholderMap.values());
			
			// Initially, separate the sentence into tokens using the separators provided
			TokenSequence tokenSequence = this.tokeniserService.getTokenSequence(sentence, Tokeniser.SEPARATORS, placeholders);
			
			// apply any pre-processing filters that have been added
			for (TokenSequenceFilter tokenSequenceFilter : this.tokenSequenceFilters) {
				tokenSequenceFilter.apply(tokenSequence);
			}
			
			List<TokenisedAtomicTokenSequence> sequences = null;

			sequences = new ArrayList<TokenisedAtomicTokenSequence>();
			TokenisedAtomicTokenSequence defaultSequence = tokeniserService.getTokenisedAtomicTokenSequence(sentence, 0);
			for (Token token : tokenSequence.listWithWhiteSpace()) {
				Decision<TokeniserOutcome> tokeniserDecision = this.tokeniserDecisionFactory.createDefaultDecision(TokeniserOutcome.SEPARATE);
				TaggedToken<TokeniserOutcome> taggedToken = this.tokeniserService.getTaggedToken(token, tokeniserDecision);
				defaultSequence.add(taggedToken);
			}
			sequences.add(defaultSequence);
			
			LOG.debug("####Final token sequences:");
			int j=1;
			for (TokenisedAtomicTokenSequence sequence : sequences) {
				TokenSequence newTokenSequence = sequence.inferTokenSequence();
				if (LOG.isDebugEnabled()) {
					LOG.debug("Token sequence " + (j++));
					LOG.debug("Atomic sequence: " + sequence);
					LOG.debug("Resulting sequence: " + newTokenSequence);
				}
				// need to re-apply the pre-processing filters, because the tokens are all new
				// Question: why can't we conserve the initial tokens when they haven't changed at all?
				// Answer: because the tokenSequence and index in the sequence is referenced by the token.
				// Question: should we create a separate class, Token and TokenInSequence,
				// one with index & sequence access & one without?
				for (TokenSequenceFilter tokenSequenceFilter : this.tokenSequenceFilters) {
					tokenSequenceFilter.apply(newTokenSequence);
				}
				if (LOG.isDebugEnabled()) {
					LOG.debug("After filters: " + newTokenSequence);
				}
			}
	
			return sequences;
		} finally {
			MONITOR.endTask("tokeniseWithDecisions");
		}
	}
	
	public TokeniserServiceInternal getTokeniserServiceInternal() {
		return tokeniserService;
	}
	public void setTokeniserServiceInternal(
			TokeniserServiceInternal tokeniserServiceInternal) {
		this.tokeniserService = tokeniserServiceInternal;
	}
	
	/**
	 * Filters to be applied to the atoms, prior to tokenising.
	 * @return
	 */
	public List<TokenSequenceFilter> getTokenSequenceFilters() {
		return tokenSequenceFilters;
	}

	public void setTokenSequenceFilters(List<TokenSequenceFilter> tokenSequenceFilters) {
		this.tokenSequenceFilters = tokenSequenceFilters;
	}
	
	public void addTokenSequenceFilter(TokenSequenceFilter tokenSequenceFilter) {
		this.tokenSequenceFilters.add(tokenSequenceFilter);
	}
	
	public void addObserver(ClassificationObserver<TokeniserOutcome> observer) {
		// nothing to do here
	}
	
	public List<TokenFilter> getTokenFilters() {
		return tokenFilters;
	}

	public void addTokenFilter(TokenFilter filter) {
		this.tokenFilters.add(filter);
	}

	public FilterService getFilterService() {
		return filterService;
	}

	public void setFilterService(FilterService filterService) {
		this.filterService = filterService;
	}
	
	
}
