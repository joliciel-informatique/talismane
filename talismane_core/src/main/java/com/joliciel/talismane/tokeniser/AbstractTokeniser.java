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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.filters.Sentence;
import com.joliciel.talismane.machineLearning.ClassificationObserver;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.tokeniser.filters.TokenFilter;
import com.joliciel.talismane.tokeniser.filters.TokenPlaceholder;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;
import com.joliciel.talismane.utils.PerformanceMonitor;

/**
 * An abstract tokeniser, applying filters correctly, but leaving actual
 * tokenisation up to the implementation.
 * 
 * @author Assaf Urieli
 *
 */
public abstract class AbstractTokeniser implements Tokeniser {
	private static final Logger LOG = LoggerFactory.getLogger(AbstractTokeniser.class);
	private static final PerformanceMonitor MONITOR = PerformanceMonitor.getMonitor(AbstractTokeniser.class);

	private TokeniserService tokeniserService;
	private MachineLearningService machineLearningService;

	private List<TokenSequenceFilter> tokenSequenceFilters = new ArrayList<TokenSequenceFilter>();

	private List<TokenFilter> tokenFilters = new ArrayList<TokenFilter>();

	private final TalismaneSession talismaneSession;

	public AbstractTokeniser(TalismaneSession talismaneSession) {
		this.talismaneSession = talismaneSession;
	}

	@Override
	public TokenSequence tokeniseText(String text) {
		List<TokenSequence> tokenSequences = this.tokenise(text);
		return tokenSequences.get(0);
	}

	@Override
	public List<TokenSequence> tokenise(String text) {
		Sentence sentence = new Sentence(text, talismaneSession);
		return this.tokenise(sentence);
	}

	@Override
	public TokenSequence tokeniseSentence(Sentence sentence) {
		List<TokenSequence> tokenSequences = this.tokenise(sentence);
		return tokenSequences.get(0);
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
		Sentence sentence = new Sentence(text, talismaneSession);
		return this.tokeniseWithDecisions(sentence);
	}

	@Override
	public List<TokenisedAtomicTokenSequence> tokeniseWithDecisions(Sentence sentence) {
		MONITOR.startTask("tokeniseWithDecisions");
		try {
			// apply any pre-tokenisation decisions via filters
			// we only want one placeholder per start index - the first one that
			// gets added
			List<TokenPlaceholder> placeholders = new ArrayList<TokenPlaceholder>();

			for (TokenFilter tokenFilter : this.tokenFilters) {
				List<TokenPlaceholder> myPlaceholders = tokenFilter.apply(sentence.getText());
				placeholders.addAll(myPlaceholders);
				if (LOG.isTraceEnabled()) {
					if (myPlaceholders.size() > 0) {
						LOG.trace("TokenFilter: " + tokenFilter);
						LOG.trace("placeholders: " + myPlaceholders);
					}
				}
			}

			// Initially, separate the sentence into tokens using the separators
			// provided
			TokenSequence tokenSequence = new TokenSequence(sentence, Tokeniser.SEPARATORS, placeholders, this.talismaneSession);

			// apply any pre-processing filters that have been added
			for (TokenSequenceFilter tokenSequenceFilter : this.tokenSequenceFilters) {
				tokenSequenceFilter.apply(tokenSequence);
			}

			List<TokenisedAtomicTokenSequence> sequences = this.tokeniseInternal(tokenSequence, sentence);

			LOG.debug("####Final token sequences:");
			int j = 1;
			for (TokenisedAtomicTokenSequence sequence : sequences) {
				TokenSequence newTokenSequence = sequence.inferTokenSequence();
				if (LOG.isDebugEnabled()) {
					LOG.debug("Token sequence " + (j++));
					LOG.debug("Atomic sequence: " + sequence);
					LOG.debug("Resulting sequence: " + newTokenSequence);
				}
				// need to re-apply the pre-processing filters, because the
				// tokens are all new
				// Question: why can't we conserve the initial tokens when they
				// haven't changed at all?
				// Answer: because the tokenSequence and index in the sequence
				// is referenced by the token.
				// Question: should we create a separate class, Token and
				// TokenInSequence,
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
			MONITOR.endTask();
		}
	}

	protected abstract List<TokenisedAtomicTokenSequence> tokeniseInternal(TokenSequence initialSequence, Sentence sentence);

	public TokeniserService getTokeniserService() {
		return tokeniserService;
	}

	public void setTokeniserService(TokeniserService tokeniserServiceInternal) {
		this.tokeniserService = tokeniserServiceInternal;
	}

	/**
	 * Filters to be applied to the atoms, prior to tokenising.
	 */
	@Override
	public List<TokenSequenceFilter> getTokenSequenceFilters() {
		return tokenSequenceFilters;
	}

	public void setTokenSequenceFilters(List<TokenSequenceFilter> tokenSequenceFilters) {
		this.tokenSequenceFilters = tokenSequenceFilters;
	}

	@Override
	public void addTokenSequenceFilter(TokenSequenceFilter tokenSequenceFilter) {
		this.tokenSequenceFilters.add(tokenSequenceFilter);
	}

	@Override
	public void addObserver(ClassificationObserver observer) {
		// nothing to do here
	}

	@Override
	public List<TokenFilter> getTokenFilters() {
		return tokenFilters;
	}

	@Override
	public void addTokenFilter(TokenFilter filter) {
		if (LOG.isTraceEnabled())
			LOG.trace("Added filter: " + filter.toString());
		this.tokenFilters.add(filter);
	}

	public MachineLearningService getMachineLearningService() {
		return machineLearningService;
	}

	public void setMachineLearningService(MachineLearningService machineLearningService) {
		this.machineLearningService = machineLearningService;
	}

}
