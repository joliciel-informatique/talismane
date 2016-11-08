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
package com.joliciel.talismane.tokeniser.patterns;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.filters.Sentence;
import com.joliciel.talismane.machineLearning.ClassificationObserver;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.tokeniser.TaggedToken;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.TokenisedAtomicTokenSequence;
import com.joliciel.talismane.tokeniser.Tokeniser;
import com.joliciel.talismane.tokeniser.TokeniserOutcome;
import com.joliciel.talismane.tokeniser.features.TokeniserContext;
import com.joliciel.talismane.tokeniser.features.TokeniserContextFeature;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;

/**
 * The interval pattern tokeniser first splits the text into individual tokens
 * based on a list of separators, each of which is assigned a default value for
 * that separator.
 * 
 * The tokeniser then takes a list of patterns, and for each pattern in the
 * list, tries to match it to a sequence of tokens within the sentence. If a
 * match is found, the final decision for each token interval in this sequence
 * is deferred to a TokeniserDecisionMaker. If not, the default values are
 * retained.
 * 
 * Overlapping sequences are handled gracefully: if a given interval is 2nd in
 * sequence A, but 1st in sequence B, it will receive the n-gram feature from
 * sequence A and a bunch of contextual features from sequence B, and the final
 * decision will be taken based on the combination of all features. However,
 * this can result in a strange compound that doesn't exist in any pattern nor
 * in the training corpus.
 * 
 * The motivation for this pattern tokeniser is to concentrate training and
 * decisions on difficult cases, rather than blurring the training model with
 * oodles of obvious cases.
 * 
 * @author Assaf Urieli
 *
 */
class IntervalPatternTokeniser extends PatternTokeniser {
	private static final Logger LOG = LoggerFactory.getLogger(IntervalPatternTokeniser.class);

	private final DecisionMaker decisionMaker;
	private final int beamWidth;
	private final Set<TokeniserContextFeature<?>> tokeniserContextFeatures;
	private final List<TokenSequenceFilter> tokenSequenceFilters;

	private final List<ClassificationObserver> observers;

	/**
	 * Reads separator defaults and test patterns from the default file for this
	 * locale.
	 */
	public IntervalPatternTokeniser(DecisionMaker decisionMaker, TokeniserPatternManager tokeniserPatternManager,
			Set<TokeniserContextFeature<?>> tokeniserContextFeatures, int beamWidth, TalismaneSession talismaneSession) {
		super(talismaneSession, tokeniserPatternManager);
		this.decisionMaker = decisionMaker;
		this.beamWidth = beamWidth;
		this.tokeniserContextFeatures = tokeniserContextFeatures;
		this.tokenSequenceFilters = new ArrayList<>();
		this.observers = new ArrayList<>();
	}

	IntervalPatternTokeniser(IntervalPatternTokeniser tokeniser) {
		super(tokeniser);
		this.decisionMaker = tokeniser.decisionMaker;
		this.beamWidth = tokeniser.beamWidth;
		this.tokeniserContextFeatures = new HashSet<>(tokeniser.tokeniserContextFeatures);
		this.tokenSequenceFilters = new ArrayList<>(tokeniser.tokenSequenceFilters);
		this.observers = new ArrayList<>(tokeniser.observers);
	}

	/**
	 * The test patterns - only token sequences matching these patterns will be
	 * submitted to further decision.
	 */
	public List<String> getTestPatterns() {
		return this.getTokeniserPatternManager().getTestPatterns();
	}

	/**
	 * The decision maker to make decisions for any separators within token
	 * sub-sequences that need further testing.
	 */
	public DecisionMaker getDecisionMaker() {
		return decisionMaker;
	}

	@Override
	public TokeniserPatternManager getTokeniserPatternManager() {
		return tokeniserPatternManager;
	}

	public int getBeamWidth() {
		return beamWidth;
	}

	/**
	 * Filters to be applied to the atoms, prior to tokenising.
	 */
	@Override
	public List<TokenSequenceFilter> getTokenSequenceFilters() {
		return tokenSequenceFilters;
	}

	@Override
	public void addTokenSequenceFilter(TokenSequenceFilter tokenSequenceFilter) {
		this.tokenSequenceFilters.add(tokenSequenceFilter);
	}

	@Override
	public void addObserver(ClassificationObserver observer) {
		this.observers.add(observer);
	}

	@Override
	protected List<TokenisedAtomicTokenSequence> tokeniseInternal(TokenSequence initialSequence, Sentence sentence) {
		// Assign each separator its default value
		List<TokeniserOutcome> defaultOutcomes = this.tokeniserPatternManager.getDefaultOutcomes(initialSequence);
		List<Decision> defaultDecisions = new ArrayList<Decision>(defaultOutcomes.size());
		for (TokeniserOutcome outcome : defaultOutcomes) {
			Decision tokeniserDecision = new Decision(outcome.name());
			tokeniserDecision.addAuthority("_" + this.getClass().getSimpleName());
			tokeniserDecision.addAuthority("_" + "DefaultDecision");
			defaultDecisions.add(tokeniserDecision);
		}
		List<TokenisedAtomicTokenSequence> sequences = null;

		// For each test pattern, see if anything in the sentence matches it
		if (this.decisionMaker != null) {
			Set<Token> tokensToCheck = new HashSet<Token>();
			for (TokenPattern parsedPattern : this.getTokeniserPatternManager().getParsedTestPatterns()) {
				Set<Token> tokensToCheckForThisPattern = new HashSet<Token>();
				List<TokenPatternMatchSequence> matchesForThisPattern = parsedPattern.match(initialSequence);
				for (TokenPatternMatchSequence tokenPatternMatch : matchesForThisPattern) {
					if (LOG.isTraceEnabled())
						tokensToCheckForThisPattern.addAll(tokenPatternMatch.getTokensToCheck());
					tokensToCheck.addAll(tokenPatternMatch.getTokensToCheck());
				}
				if (LOG.isTraceEnabled()) {
					if (tokensToCheckForThisPattern.size() > 0) {
						LOG.trace("Parsed pattern: " + parsedPattern);
						LOG.trace("tokensToCheck: " + tokensToCheckForThisPattern);
					}
				}
			}

			// we want to create the n most likely token sequences
			// the sequence has to correspond to a token pattern

			// initially create a heap with a single, empty sequence
			PriorityQueue<TokenisedAtomicTokenSequence> heap = new PriorityQueue<TokenisedAtomicTokenSequence>();
			TokenisedAtomicTokenSequence emptySequence = new TokenisedAtomicTokenSequence(sentence, 0, this.getTalismaneSession());
			heap.add(emptySequence);
			int i = 0;
			for (Token token : initialSequence.listWithWhiteSpace()) {
				if (LOG.isTraceEnabled()) {
					LOG.trace("Token : \"" + token.getText() + "\"");
				}
				// build a new heap for this iteration
				PriorityQueue<TokenisedAtomicTokenSequence> previousHeap = heap;
				heap = new PriorityQueue<TokenisedAtomicTokenSequence>();

				// limit the heap breadth to K
				int maxSequences = previousHeap.size() > this.getBeamWidth() ? this.getBeamWidth() : previousHeap.size();
				for (int j = 0; j < maxSequences; j++) {
					TokenisedAtomicTokenSequence history = previousHeap.poll();

					// Find the separating & non-separating decisions
					List<Decision> decisions = null;
					if (tokensToCheck.contains(token)) {
						// test the features on the current token
						TokeniserContext context = new TokeniserContext(token, history);
						List<FeatureResult<?>> tokenFeatureResults = new ArrayList<FeatureResult<?>>();
						for (TokeniserContextFeature<?> feature : tokeniserContextFeatures) {
							RuntimeEnvironment env = new RuntimeEnvironment();
							FeatureResult<?> featureResult = feature.check(context, env);
							if (featureResult != null) {
								tokenFeatureResults.add(featureResult);
							}
						}

						if (LOG.isTraceEnabled()) {
							for (FeatureResult<?> featureResult : tokenFeatureResults) {
								LOG.trace(featureResult.toString());
							}
						}

						decisions = this.decisionMaker.decide(tokenFeatureResults);

						for (ClassificationObserver observer : this.observers)
							observer.onAnalyse(token, tokenFeatureResults, decisions);

						for (Decision decision : decisions) {
							decision.addAuthority(this.getClass().getSimpleName());
							for (TokenPatternMatch tokenMatch : token.getMatches()) {
								decision.addAuthority(tokenMatch.getPattern().toString());
							}
						}
					} else {
						decisions = new ArrayList<Decision>();
						decisions.add(defaultDecisions.get(i));
					}

					for (Decision decision : decisions) {
						TaggedToken<TokeniserOutcome> taggedToken = new TaggedToken<>(token, decision, TokeniserOutcome.valueOf(decision.getOutcome()));

						TokenisedAtomicTokenSequence tokenisedSequence = new TokenisedAtomicTokenSequence(history);
						tokenisedSequence.add(taggedToken);
						if (decision.isStatistical())
							tokenisedSequence.addDecision(decision);
						heap.add(tokenisedSequence);
					}

				} // next sequence in the old heap
				i++;
			} // next token

			sequences = new ArrayList<TokenisedAtomicTokenSequence>();
			i = 0;
			while (!heap.isEmpty()) {
				sequences.add(heap.poll());
				i++;
				if (i >= this.getBeamWidth())
					break;
			}
		} else {
			sequences = new ArrayList<TokenisedAtomicTokenSequence>();
			TokenisedAtomicTokenSequence defaultSequence = new TokenisedAtomicTokenSequence(sentence, 0, this.getTalismaneSession());
			int i = 0;
			for (Token token : initialSequence.listWithWhiteSpace()) {
				Decision decision = defaultDecisions.get(i++);
				TaggedToken<TokeniserOutcome> taggedToken = new TaggedToken<>(token, decision, TokeniserOutcome.valueOf(decision.getOutcome()));

				defaultSequence.add(taggedToken);
			}
			sequences.add(defaultSequence);
		} // have decision maker?
		return sequences;
	}

	@Override
	public Tokeniser cloneTokeniser() {
		return new IntervalPatternTokeniser(this);
	}

}
