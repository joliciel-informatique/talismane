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
package com.joliciel.talismane.tokeniser.patterns;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.ArrayList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.filters.FilterService;
import com.joliciel.talismane.filters.Sentence;
import com.joliciel.talismane.machineLearning.ClassificationObserver;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.FeatureService;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.tokeniser.TaggedToken;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.Tokeniser;
import com.joliciel.talismane.tokeniser.TokeniserDecisionFactory;
import com.joliciel.talismane.tokeniser.TokeniserOutcome;
import com.joliciel.talismane.tokeniser.TokenisedAtomicTokenSequence;
import com.joliciel.talismane.tokeniser.TokeniserService;
import com.joliciel.talismane.tokeniser.features.TokenFeatureService;
import com.joliciel.talismane.tokeniser.features.TokeniserContext;
import com.joliciel.talismane.tokeniser.features.TokeniserContextFeature;
import com.joliciel.talismane.tokeniser.filters.TokenFilter;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;
import com.joliciel.talismane.tokeniser.filters.TokenPlaceholder;
import com.joliciel.talismane.utils.PerformanceMonitor;

/**
 * The interval pattern tokeniser first splits the text into individual tokens based on a list of separators,
 * each of which is assigned a default value for that separator.
 * 
 * The tokeniser then takes a list of patterns, and for each pattern in the list, tries to match it to a sequence of tokens within the sentence.
 * If a match is found, the final decision for each token interval in this sequence is deferred to a TokeniserDecisionMaker.
 * If not, the default values are retained.
 * 
 * Overlapping sequences are handled gracefully: if a given interval is 2nd in sequence A, but 1st in sequence B, it will receive the
 * n-gram feature from sequence A and a bunch of contextual features from sequence B, and the final decision will be taken based on the
 * combination of all features. However, this can result in a strange compound that doesn't exist in any pattern nor in the training corpus.
 * 
 * The motivation for this pattern tokeniser is to concentrate training and decisions on difficult cases, rather than blurring the
 * training model with oodles of obvious cases.
 * 
 * @author Assaf Urieli
 *
 */
class IntervalPatternTokeniser implements PatternTokeniser {
	private static final Log LOG = LogFactory.getLog(IntervalPatternTokeniser.class);
	private static final PerformanceMonitor MONITOR = PerformanceMonitor.getMonitor(IntervalPatternTokeniser.class);

	private static final DecimalFormat df = new DecimalFormat("0.0000");
	
	private DecisionMaker<TokeniserOutcome> decisionMaker;
	
	private TokeniserService tokeniserService;
	private TokeniserPatternService tokeniserPatternService;
	private TokenFeatureService tokenFeatureService;
	private FilterService filterService;
	private FeatureService featureService;
	
	private TokeniserPatternManager tokeniserPatternManager;
	private int beamWidth;
	private Set<TokeniserContextFeature<?>> tokeniserContextFeatures;
	private List<TokenSequenceFilter> tokenSequenceFilters = new ArrayList<TokenSequenceFilter>();
	
	private List<ClassificationObserver<TokeniserOutcome>> observers = new ArrayList<ClassificationObserver<TokeniserOutcome>>();
	
	private List<TokenFilter> tokenFilters = new ArrayList<TokenFilter>();
	private TokeniserDecisionFactory tokeniserDecisionFactory = new TokeniserDecisionFactory();

	/**
	 * Reads separator defaults and test patterns from the default file for this locale.
	 * @param locale
	 */
	public IntervalPatternTokeniser(TokeniserPatternManager tokeniserPatternManager,
			Set<TokeniserContextFeature<?>> tokeniserContextFeatures, int beamWidth) {
		this.tokeniserPatternManager = tokeniserPatternManager;
		this.beamWidth = beamWidth;
		this.tokeniserContextFeatures = tokeniserContextFeatures;
	}
	
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
			
			// Assign each separator its default value
			List<TokeniserOutcome> defaultOutcomes = this.tokeniserPatternManager.getDefaultOutcomes(tokenSequence);
			List<Decision<TokeniserOutcome>> defaultDecisions = new ArrayList<Decision<TokeniserOutcome>>(defaultOutcomes.size());
			for (TokeniserOutcome outcome : defaultOutcomes) {
				Decision<TokeniserOutcome> tokeniserDecision = this.tokeniserDecisionFactory.createDefaultDecision(outcome);
				tokeniserDecision.addAuthority("_" + this.getClass().getSimpleName());
				tokeniserDecision.addAuthority("_" + "DefaultDecision");
				defaultDecisions.add(tokeniserDecision);
			}
			List<TokenisedAtomicTokenSequence> sequences = null;
			
			// For each test pattern, see if anything in the sentence matches it
			if (this.decisionMaker!=null) {
				Set<Token> tokensToCheck = new HashSet<Token>();
				MONITOR.startTask("pattern matching");
				try {
					for (TokenPattern parsedPattern : this.getTokeniserPatternManager().getParsedTestPatterns()) {
						Set<Token> tokensToCheckForThisPattern = new HashSet<Token>();
						List<TokenPatternMatchSequence> matchesForThisPattern = parsedPattern.match(tokenSequence);
						for (TokenPatternMatchSequence tokenPatternMatch : matchesForThisPattern) {
							if (LOG.isTraceEnabled())
								tokensToCheckForThisPattern.addAll(tokenPatternMatch.getTokensToCheck());
							tokensToCheck.addAll(tokenPatternMatch.getTokensToCheck());
						}
						if (LOG.isTraceEnabled()) {
							if (tokensToCheckForThisPattern.size()>0) {
								LOG.trace("Parsed pattern: " + parsedPattern);
								LOG.trace("tokensToCheck: " + tokensToCheckForThisPattern);
							}
						}
					}
				} finally {
					MONITOR.endTask("pattern matching");
				}
				
				// we want to create the n most likely token sequences
				// the sequence has to correspond to a token pattern
	
				// initially create a heap with a single, empty sequence
				PriorityQueue<TokenisedAtomicTokenSequence> heap = new PriorityQueue<TokenisedAtomicTokenSequence>();
				TokenisedAtomicTokenSequence emptySequence = this.getTokeniserService().getTokenisedAtomicTokenSequence(sentence, 0);
				heap.add(emptySequence);
				int i = 0;
				for (Token token : tokenSequence.listWithWhiteSpace()) {
					if (LOG.isTraceEnabled()) {
						LOG.trace("Token : \"" + token.getText() + "\"");
					}
					// build a new heap for this iteration
					PriorityQueue<TokenisedAtomicTokenSequence> previousHeap = heap;
					heap = new PriorityQueue<TokenisedAtomicTokenSequence>();
					
					// limit the heap breadth to K
					int maxSequences = previousHeap.size() > this.getBeamWidth() ? this.getBeamWidth() : previousHeap.size();
					for (int j = 0; j<maxSequences; j++) {
						TokenisedAtomicTokenSequence history = previousHeap.poll();
						
						// Find the separating & non-separating decisions
						List<Decision<TokeniserOutcome>> decisions = null;
						if (tokensToCheck.contains(token)) {
							// test the features on the current token
							TokeniserContext context = new TokeniserContext(token, history);
							List<FeatureResult<?>> tokenFeatureResults = new ArrayList<FeatureResult<?>>();
							MONITOR.startTask("analyse features");
							try {
								for (TokeniserContextFeature<?> feature : tokeniserContextFeatures) {
									RuntimeEnvironment env = this.featureService.getRuntimeEnvironment();
									FeatureResult<?> featureResult = feature.check(context, env);
									if (featureResult!=null) {
										tokenFeatureResults.add(featureResult);
									}
								}
								
								if (LOG.isTraceEnabled()) {
									for (FeatureResult<?> featureResult : tokenFeatureResults) {
										LOG.trace(featureResult.toString());
									}
								}
							} finally {
								MONITOR.endTask("analyse features");
							}
							
							MONITOR.startTask("make decision");
							try {
								decisions = this.decisionMaker.decide(tokenFeatureResults);
								
								for (ClassificationObserver<TokeniserOutcome> observer : this.observers)
									observer.onAnalyse(token, tokenFeatureResults, decisions);
								
								for (Decision<TokeniserOutcome> decision: decisions) {
									decision.addAuthority(this.getClass().getSimpleName());
									for (TokenPatternMatch tokenMatch : token.getMatches()) {
										decision.addAuthority(tokenMatch.getPattern().toString());
									}
								}
							} finally {
								MONITOR.endTask("make decision");
							}
						} else {
							decisions = new ArrayList<Decision<TokeniserOutcome>>();
							decisions.add(defaultDecisions.get(i));
						}
	
						MONITOR.startTask("heap sort");
						try {
							for (Decision<TokeniserOutcome> decision : decisions) {
								TaggedToken<TokeniserOutcome> taggedToken = this.tokeniserService.getTaggedToken(token, decision);

								TokenisedAtomicTokenSequence tokenisedSequence = this.getTokeniserService().getTokenisedAtomicTokenSequence(history);
								tokenisedSequence.add(taggedToken);
								if (decision.isStatistical())
									tokenisedSequence.addDecision(decision);
								heap.add(tokenisedSequence);
							}
						} finally {
							MONITOR.endTask("heap sort");
						}
	
					} // next sequence in the old heap
					i++;
				} // next token
				
				sequences = new ArrayList<TokenisedAtomicTokenSequence>();
				i = 0;
				while (!heap.isEmpty()) {
					sequences.add(heap.poll());
					i++;
					if (i>=this.getBeamWidth())
						break;
				}
			} else {
				sequences = new ArrayList<TokenisedAtomicTokenSequence>();
				TokenisedAtomicTokenSequence defaultSequence = this.getTokeniserService().getTokenisedAtomicTokenSequence(sentence, 0);
				int i = 0;
				for (Token token : tokenSequence.listWithWhiteSpace()) {
					TaggedToken<TokeniserOutcome> taggedToken = this.tokeniserService.getTaggedToken(token, defaultDecisions.get(i++));
					defaultSequence.add(taggedToken);
				}
				sequences.add(defaultSequence);
			} // have decision maker?
			
			LOG.debug("####Final token sequences:");
			int j=1;
			for (TokenisedAtomicTokenSequence sequence : sequences) {
				TokenSequence newTokenSequence = sequence.inferTokenSequence();
				if (LOG.isDebugEnabled()) {
					LOG.debug("Token sequence " + (j++) + ", score=" + df.format(sequence.getScore()));
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

	/**
	 * The test patterns - only token sequences matching these patterns will
	 * be submitted to further decision.
	 * @return
	 */
	public List<String> getTestPatterns() {
		return this.getTokeniserPatternManager().getTestPatterns();
	}

	/**
	 * The decision maker to make decisions for any separators within token
	 * sub-sequences that need further testing.
	 * @return
	 */
	public DecisionMaker<TokeniserOutcome> getDecisionMaker() {
		return decisionMaker;
	}

	public void setDecisionMaker(
			DecisionMaker<TokeniserOutcome> decisionMaker) {
		this.decisionMaker = decisionMaker;
	}

	public TokeniserService getTokeniserService() {
		return tokeniserService;
	}

	public void setTokeniserService(TokeniserService tokeniserService) {
		this.tokeniserService = tokeniserService;
	}

	public TokeniserPatternManager getTokeniserPatternManager() {
		return tokeniserPatternManager;
	}

	public void setTokeniserPatternManager(
			TokeniserPatternManager tokeniserPatternManager) {
		this.tokeniserPatternManager = tokeniserPatternManager;
	}

	public int getBeamWidth() {
		return beamWidth;
	}

	public TokeniserPatternService getTokeniserPatternService() {
		return tokeniserPatternService;
	}

	public void setTokeniserPatternService(
			TokeniserPatternService tokeniserPatternService) {
		this.tokeniserPatternService = tokeniserPatternService;
	}

	public TokenFeatureService getTokenFeatureService() {
		return tokenFeatureService;
	}

	public void setTokenFeatureService(TokenFeatureService tokenFeatureService) {
		this.tokenFeatureService = tokenFeatureService;
	}

	/**
	 * Filters to be applied to the atoms, prior to tokenising.
	 * @return
	 */
	public List<TokenSequenceFilter> getTokenSequenceFilters() {
		return tokenSequenceFilters;
	}
	
	public void addTokenSequenceFilter(TokenSequenceFilter tokenSequenceFilter) {
		this.tokenSequenceFilters.add(tokenSequenceFilter);
	}

	@Override
	public void addObserver(ClassificationObserver<TokeniserOutcome> observer) {
		this.observers.add(observer);
	}
	

	public List<TokenFilter> getTokenFilters() {
		return tokenFilters;
	}

	public void addTokenFilter(TokenFilter tokenFilter) {
		this.tokenFilters.add(tokenFilter);
	}

	public FilterService getFilterService() {
		return filterService;
	}

	public void setFilterService(FilterService filterService) {
		this.filterService = filterService;
	}

	public FeatureService getFeatureService() {
		return featureService;
	}

	public void setFeatureService(FeatureService featureService) {
		this.featureService = featureService;
	}
	
	
}
