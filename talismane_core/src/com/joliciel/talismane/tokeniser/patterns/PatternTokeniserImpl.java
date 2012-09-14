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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Map.Entry;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.tokeniser.TaggedToken;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.Tokeniser;
import com.joliciel.talismane.tokeniser.TokeniserDecision;
import com.joliciel.talismane.tokeniser.TokeniserDecisionTagSequence;
import com.joliciel.talismane.tokeniser.TokeniserService;
import com.joliciel.talismane.tokeniser.features.TokenFeatureService;
import com.joliciel.talismane.tokeniser.features.TokeniserContext;
import com.joliciel.talismane.tokeniser.features.TokeniserContextFeature;
import com.joliciel.talismane.tokeniser.filters.TokenFilter;
import com.joliciel.talismane.utils.AnalysisObserver;
import com.joliciel.talismane.utils.DecisionMaker;
import com.joliciel.talismane.utils.features.FeatureResult;
import com.joliciel.talismane.utils.util.PerformanceMonitor;
import com.joliciel.talismane.utils.util.WeightedOutcome;

/**
 * The pattern tokeniser first splits the text into individual tokens based on a list of separators,
 * each of which is assigned a default value for that separator.
 * 
 * The tokeniser then takes a list of patterns, and for each pattern in the list, tries to match it to a sequence of tokens within the sentence.
 * If a match is found, the final decision for all separators in this sequence is deferred to a TokeniserDecisionMaker.
 * If not, the default values are retained.
 * 
 * The motivation for this pattern tokeniser is to concentrate training and decisions on difficult cases, rather than blurring the
 * training model with oodles of obvious cases.
 * 
 * @author Assaf Urieli
 *
 */
class PatternTokeniserImpl implements Tokeniser {
	private static final Log LOG = LogFactory.getLog(PatternTokeniserImpl.class);
	
	private Map<TokeniserDecision, String> separatorDefaults;
	private Map<TokeniserDecision, Pattern> separatorDefaultPatterns;
	private DecisionMaker decisionMaker;
	protected Pattern whitespace = Pattern.compile("\\s+");
	
	private TokeniserService tokeniserService;
	private TokeniserPatternService tokeniserPatternService;
	private TokenFeatureService tokenFeatureService;
	
	private TokeniserPatternManager tokeniserPatternManager;
	private int beamWidth;
	private Set<TokeniserContextFeature<?>> tokeniserContextFeatures;
	private List<TokenFilter> preprocessingFilters = new ArrayList<TokenFilter>();
	
	private List<AnalysisObserver> observers = new ArrayList<AnalysisObserver>();

	/**
	 * Reads separator defaults and test patterns from the default file for this locale.
	 * @param locale
	 */
	public PatternTokeniserImpl(TokeniserPatternManager tokeniserPatternManager,
			Set<TokeniserContextFeature<?>> tokeniserContextFeatures, int beamWidth) {
		this.tokeniserPatternManager = tokeniserPatternManager;
		this.beamWidth = beamWidth;
		this.tokeniserContextFeatures = tokeniserContextFeatures;
	}
	
	@Override
	public List<TokenSequence> tokenise(String sentence) {
		List<TokeniserDecisionTagSequence> decisionSequences = this.tokeniseWithDecisions(sentence);
		List<TokenSequence> tokenSequences = new ArrayList<TokenSequence>();
		for (TokeniserDecisionTagSequence decisionSequence : decisionSequences) {
			tokenSequences.add(decisionSequence.getTokenSequence());
		}
		return tokenSequences;
	}

	@Override
	public List<TokeniserDecisionTagSequence> tokeniseWithDecisions(String sentence) {
		PerformanceMonitor.startTask("PatternTokeniserImpl.tokeniseWithDecisions");
		try {
			// Initially, separate the sentence into tokens using the separators provided
			TokenSequence tokenSequence = this.tokeniserService.getTokenSequence(sentence, Tokeniser.SEPARATORS);
			
			// apply any pre-processing filters that have been added
			for (TokenFilter tokenFilter : this.preprocessingFilters) {
				tokenFilter.apply(tokenSequence);
			}
			
			// Assign each separator its default value
			TokeniserDecisionTagSequence defaultDecisions = this.makeDefaultDecisions(sentence, tokenSequence);
			List<TokeniserDecisionTagSequence> sequences = null;
			
			// For each test pattern, see if anything in the sentence matches it
			if (this.decisionMaker!=null) {
				Set<Token> tokensToCheck = new HashSet<Token>();
				List<TokenPatternMatch> tokenPatternMatches = new ArrayList<TokenPatternMatch>();
				PerformanceMonitor.startTask("pattern matching");
				try {
					for (TokenPattern parsedPattern : this.getTokeniserPatternManager().getParsedTestPatterns()) {
						if (LOG.isTraceEnabled())
							LOG.trace("Parsed pattern: " + parsedPattern);
						List<TokenPatternMatch> matchesForThisPattern = parsedPattern.match(tokenSequence);
						for (TokenPatternMatch tokenPatternMatch : matchesForThisPattern) {
							tokenPatternMatches.add(tokenPatternMatch);
							tokensToCheck.addAll(tokenPatternMatch.getTokensToCheck());
						}
					}
				} finally {
					PerformanceMonitor.endTask("pattern matching");
				}
				
				// we want to create the n most likely token sequences
				// the sequence has to correspond to a token pattern
	
				// initially create a heap with a single, empty sequence
				PriorityQueue<TokeniserDecisionTagSequence> heap = new PriorityQueue<TokeniserDecisionTagSequence>();
				TokeniserDecisionTagSequence emptySequence = this.getTokeniserService().getTokeniserDecisionTagSequence(sentence, 0);
				heap.add(emptySequence);
				int i = 0;
				for (Token token : tokenSequence.listWithWhiteSpace()) {
					if (LOG.isTraceEnabled()) {
						LOG.trace("Token : \"" + token.getText() + "\"");
					}
					// build a new heap for this iteration
					PriorityQueue<TokeniserDecisionTagSequence> previousHeap = heap;
					heap = new PriorityQueue<TokeniserDecisionTagSequence>();
					
					// limit the heap breadth to K
					int maxSequences = previousHeap.size() > this.getBeamWidth() ? this.getBeamWidth() : previousHeap.size();
					for (int j = 0; j<maxSequences; j++) {
						TokeniserDecisionTagSequence history = previousHeap.poll();
						
						// Find the separating & non-separating decisions
						List<TaggedToken<TokeniserDecision>> tokenDecisions = null;
						boolean defaultDecision = false;
						if (tokensToCheck.contains(token)) {
							
							// test the features on the current token
							TokeniserContext context = new TokeniserContext(token, history);
							List<FeatureResult<?>> tokenFeatureResults = new ArrayList<FeatureResult<?>>();
							PerformanceMonitor.startTask("analyse features");
							try {
								for (TokeniserContextFeature<?> tokeniserContextFeature : tokeniserContextFeatures) {
									FeatureResult<?> featureResult = tokeniserContextFeature.check(context);
									if (featureResult!=null)
										tokenFeatureResults.add(featureResult);
								}
							} finally {
								PerformanceMonitor.endTask("analyse features");
							}
							
							PerformanceMonitor.startTask("make decision");
							try {
								List<WeightedOutcome<String>> weightedOutcomes = this.decisionMaker.decide(tokenFeatureResults);
								
								for (AnalysisObserver observer : this.observers) {
									observer.onAnalyse(token, tokenFeatureResults, weightedOutcomes);
								}
		
								tokenDecisions = new ArrayList<TaggedToken<TokeniserDecision>>();
								for (WeightedOutcome<String> weightedOutcome : weightedOutcomes) {
									TokeniserDecision decision = TokeniserDecision.valueOf(weightedOutcome.getOutcome());
									TaggedToken<TokeniserDecision> taggedToken = this.tokeniserService.getTaggedToken(token, decision, weightedOutcome.getWeight());
									taggedToken.addTagger(this.getClass().getSimpleName());
									for (TokenMatch tokenMatch : token.getMatches()) {
										taggedToken.addTagger(tokenMatch.getPattern().toString());
									}
									tokenDecisions.add(taggedToken);
		
								}
							} finally {
								PerformanceMonitor.endTask("make decision");
							}
						} else {
							tokenDecisions = new ArrayList<TaggedToken<TokeniserDecision>>();
							tokenDecisions.add(defaultDecisions.get(i));
							defaultDecision = true;
						}
	
						PerformanceMonitor.startTask("heap sort");
						try {
							for (TaggedToken<TokeniserDecision> tokenDecision : tokenDecisions) {
								TokeniserDecisionTagSequence sequence = this.getTokeniserService().getTokeniserDecisionTagSequence(history);
								sequence.add(tokenDecision);
								if (!defaultDecision)
									sequence.addDecision(tokenDecision.getProbability());
								heap.add(sequence);
							}
						} finally {
							PerformanceMonitor.endTask("heap sort");
						}
	
					} // next sequence in the old heap
					i++;
				} // next token
				
				sequences = new ArrayList<TokeniserDecisionTagSequence>();
				i = 0;
				while (!heap.isEmpty()) {
					sequences.add(heap.poll());
					i++;
					if (i>=this.getBeamWidth())
						break;
				}
	
				
			} else {
				sequences = new ArrayList<TokeniserDecisionTagSequence>();
				sequences.add(defaultDecisions);
			}
			
			for (TokeniserDecisionTagSequence sequence : sequences) {
				TokenSequence newTokenSequence = sequence.getTokenSequence();
				// need to re-apply the pre-processing filters, because the tokens are all new
				//TODO: why can't we conserve the initial tokens when they haven't changed at all?
				// is it because the tokenSequence is referenced by the token?
				// should we create a separate class, Token and TokenInSequence,
				// one with index & sequence access & one without?
				for (TokenFilter tokenFilter : this.preprocessingFilters) {
					tokenFilter.apply(newTokenSequence);
				}
			}
	
			return sequences;
		} finally {
			PerformanceMonitor.endTask("PatternTokeniserImpl.tokeniseWithDecisions");
		}
	}
	
	protected TokeniserDecisionTagSequence makeDefaultDecisions(String sentence, TokenSequence tokenSequence) {
		TokeniserDecisionTagSequence decisions = this.getTokeniserService().getTokeniserDecisionTagSequence(sentence, tokenSequence.listWithWhiteSpace().size());
		
		// Assign each separator its default value
		TokeniserDecision nextDecision = TokeniserDecision.DOES_SEPARATE;
		for (Token token : tokenSequence.listWithWhiteSpace()) {
			TaggedToken<TokeniserDecision> taggedToken;
			
			if (Tokeniser.SEPARATORS.matcher(token.getText()).matches()) {
				boolean defaultValueFound = false;
				TokeniserDecision decision = null;
				for (Entry<TokeniserDecision, Pattern> entry : this.getSeparatorDefaultPatterns().entrySet()) {
					if (entry.getValue().matcher(token.getText()).matches()) {
						defaultValueFound = true;
						TokeniserDecision defaultDecision = entry.getKey();
						switch (defaultDecision) {
						case IS_SEPARATOR:
							decision = TokeniserDecision.DOES_SEPARATE;
							nextDecision = TokeniserDecision.DOES_SEPARATE;
							break;
						case IS_NOT_SEPARATOR:
							decision = TokeniserDecision.DOES_NOT_SEPARATE;
							nextDecision = TokeniserDecision.DOES_NOT_SEPARATE;
							break;
						case IS_SEPARATOR_BEFORE:
							decision = TokeniserDecision.DOES_SEPARATE;
							nextDecision = TokeniserDecision.DOES_NOT_SEPARATE;
						case IS_SEPARATOR_AFTER:
							decision = TokeniserDecision.DOES_NOT_SEPARATE;
							nextDecision = TokeniserDecision.DOES_SEPARATE;
						}
						break;
					}
				}
				if (!defaultValueFound) {
					decision = TokeniserDecision.DOES_SEPARATE;
					nextDecision = TokeniserDecision.DOES_SEPARATE;
				}
				taggedToken = this.getTokeniserService().getTaggedToken(token, decision, 1);
			} else {
				taggedToken = this.getTokeniserService().getTaggedToken(token, nextDecision, 1);
			}
			taggedToken.addTagger("PatternSeparator:DefaultSeparatorDecison");
			decisions.add(taggedToken);
		}
		return decisions;
	}
	/**
	 * Default values for various separators.
	 * All separators not in this map will default to TokeniserDecision.IS_SEPARATOR.
	 * The Strings should be simple lists of separators for which the TokeniserDecision is the default one.
	 * @return
	 */
	public Map<TokeniserDecision, String> getSeparatorDefaults() {
		if (this.separatorDefaults==null) {
			this.setSeparatorDefaults(this.getTokeniserPatternManager().getSeparatorDefaults());
		}
		return separatorDefaults;
	}

	public void setSeparatorDefaults(
			Map<TokeniserDecision, String> separatorDefaults) {
		this.separatorDefaults = separatorDefaults;
	}

	protected Map<TokeniserDecision, Pattern> getSeparatorDefaultPatterns() {
		if (this.separatorDefaultPatterns==null) {
			this.separatorDefaultPatterns = new HashMap<TokeniserDecision, Pattern>();
			for (Entry<TokeniserDecision, String> entry : this.getSeparatorDefaults().entrySet()) {
				Pattern pattern = Pattern.compile("[" + entry.getValue() + "]");
				this.separatorDefaultPatterns.put(entry.getKey(), pattern);
			}
			
		}
		return separatorDefaultPatterns;
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
	public DecisionMaker getDecisionMaker() {
		return decisionMaker;
	}

	public void setDecisionMaker(
			DecisionMaker decisionMaker) {
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
	public List<TokenFilter> getTokenFilters() {
		return preprocessingFilters;
	}

	public void setTokenFilters(List<TokenFilter> tokenFilters) {
		this.preprocessingFilters = tokenFilters;
	}
	
	public void addTokenFilter(TokenFilter tokenFilter) {
		this.preprocessingFilters.add(tokenFilter);
	}

	@Override
	public void addObserver(AnalysisObserver observer) {
		this.observers.add(observer);
	}
}
