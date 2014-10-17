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

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.ArrayList;
import java.util.TreeSet;

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
import com.joliciel.talismane.tokeniser.features.TokenPatternMatchFeature;
import com.joliciel.talismane.tokeniser.filters.TokenFilter;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;
import com.joliciel.talismane.tokeniser.filters.TokenPlaceholder;
import com.joliciel.talismane.utils.PerformanceMonitor;

/**
 * The compound pattern tokeniser first splits the text into individual tokens based on a list of separators,
 * each of which is assigned a default value for that separator.
 * 
 * The tokeniser then takes a list of patterns, and for each pattern in the list, tries to match it to a sequence of tokens within the sentence.
 * If a match is found, a join/separate decision is taken for the sequence as a whole. If not, the default values are retained.
 * However, to allow for rare overlapping sequences, if the join/separate decision would result in default decisions for the entire sequence, we only mark the first interval
 * in the sequence, and allow another pattern to match the remaining tokens.
 * Otherwise, we skip all tokens in this sequence before trying to match.
 * 
 * The motivation for this pattern tokeniser is to concentrate training and decisions on difficult cases, rather than blurring the
 * training model with oodles of obvious cases.
 * Furthermore, we have virtually eliminated strange broken compounds, which was possible lower-down in the beam using the interval approach,
 * because the n-gram features used in that approach generally contained no counter-examples, leading to the "missing category" phenomenon with a
 * relatively high score for the missing category.
 * 
 * @author Assaf Urieli
 *
 */
class CompoundPatternTokeniser implements PatternTokeniser {
	private static final Log LOG = LogFactory.getLog(CompoundPatternTokeniser.class);
	private static final PerformanceMonitor MONITOR = PerformanceMonitor.getMonitor(CompoundPatternTokeniser.class);

	private static final DecimalFormat df = new DecimalFormat("0.0000");
	
	private DecisionMaker<TokeniserOutcome> decisionMaker;
	
	private TokeniserService tokeniserService;
	private TokeniserPatternService tokeniserPatternService;
	private TokenFeatureService tokenFeatureService;
	private FilterService filterService;
	private FeatureService featureService;
	
	private TokeniserPatternManager tokeniserPatternManager;
	private int beamWidth;
	private Set<TokenPatternMatchFeature<?>> features;
	private List<TokenSequenceFilter> tokenSequenceFilters = new ArrayList<TokenSequenceFilter>();
	
	private List<ClassificationObserver<TokeniserOutcome>> observers = new ArrayList<ClassificationObserver<TokeniserOutcome>>();
	private TokeniserDecisionFactory tokeniserDecisionFactory = new TokeniserDecisionFactory();
	
	private List<TokenFilter> tokenFilters = new ArrayList<TokenFilter>();

	/**
	 * Reads separator defaults and test patterns from the default file for this locale.
	 * @param locale
	 */
	public CompoundPatternTokeniser(TokeniserPatternManager tokeniserPatternManager,
			Set<TokenPatternMatchFeature<?>> features, int beamWidth) {
		this.tokeniserPatternManager = tokeniserPatternManager;
		this.beamWidth = beamWidth;
		this.features = features;
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
				List<TokenPatternMatchSequence> matchingSequences = new ArrayList<TokenPatternMatchSequence>();
				Map<Token,Set<TokenPatternMatchSequence>> tokenMatchSequenceMap = new HashMap<Token, Set<TokenPatternMatchSequence>>();
				Map<TokenPatternMatchSequence,TokenPatternMatch> primaryMatchMap = new HashMap<TokenPatternMatchSequence, TokenPatternMatch>();
				Set<Token> matchedTokens = new HashSet<Token>();
				
				MONITOR.startTask("pattern matching");
				try {
					for (TokenPattern parsedPattern : this.getTokeniserPatternManager().getParsedTestPatterns()) {
						List<TokenPatternMatchSequence> matchesForThisPattern = parsedPattern.match(tokenSequence);
						for (TokenPatternMatchSequence matchSequence : matchesForThisPattern) {
							matchingSequences.add(matchSequence);
							matchedTokens.addAll(matchSequence.getTokensToCheck());
							
							TokenPatternMatch primaryMatch = null;
							Token token = matchSequence.getTokensToCheck().get(0);
							
							Set<TokenPatternMatchSequence> matchSequences = tokenMatchSequenceMap.get(token);
							if (matchSequences==null) {
								matchSequences = new TreeSet<TokenPatternMatchSequence>();
								tokenMatchSequenceMap.put(token, matchSequences);
							}
							matchSequences.add(matchSequence);
							
							for (TokenPatternMatch patternMatch : matchSequence.getTokenPatternMatches()) {
								if (patternMatch.getToken().equals(token)) {
									primaryMatch = patternMatch;
									break;
								}
							}
							
							if (LOG.isTraceEnabled()) {
								LOG.trace("Found match: " + primaryMatch);
							}
							primaryMatchMap.put(matchSequence, primaryMatch);
						}
					}
				} finally {
					MONITOR.endTask();
				}
				
				// we want to create the n most likely token sequences
				// the sequence has to correspond to a token pattern
				Map<TokenPatternMatchSequence,List<Decision<TokeniserOutcome>>> matchSequenceDecisionMap = new HashMap<TokenPatternMatchSequence, List<Decision<TokeniserOutcome>>>();
				
				for (TokenPatternMatchSequence matchSequence : matchingSequences) {
					TokenPatternMatch match = primaryMatchMap.get(matchSequence);
					LOG.debug("next pattern match: " + match.toString());
					List<FeatureResult<?>> tokenFeatureResults = new ArrayList<FeatureResult<?>>();
					MONITOR.startTask("analyse features");
					try {
						for (TokenPatternMatchFeature<?> feature : features) {
							RuntimeEnvironment env = this.featureService.getRuntimeEnvironment();
							FeatureResult<?> featureResult = feature.check(match, env);
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
						MONITOR.endTask();
					}
					
					List<Decision<TokeniserOutcome>> decisions = null;
					MONITOR.startTask("make decision");
					try {
						decisions = this.decisionMaker.decide(tokenFeatureResults);
						
						for (ClassificationObserver<TokeniserOutcome> observer : this.observers)
							observer.onAnalyse(match.getToken(), tokenFeatureResults, decisions);
						
						for (Decision<TokeniserOutcome> decision: decisions) {
							decision.addAuthority("_" + this.getClass().getSimpleName());
							decision.addAuthority("_" + "Patterns");
							decision.addAuthority(match.getPattern().getName());
						}
					} finally {
						MONITOR.endTask();
					}
					
					matchSequenceDecisionMap.put(matchSequence, decisions);
				}
				
				// initially create a heap with a single, empty sequence
				PriorityQueue<TokenisedAtomicTokenSequence> heap = new PriorityQueue<TokenisedAtomicTokenSequence>();
				TokenisedAtomicTokenSequence emptySequence = this.getTokeniserService().getTokenisedAtomicTokenSequence(sentence, 0);
				heap.add(emptySequence);
				
				for (int i=0; i<tokenSequence.listWithWhiteSpace().size(); i++) {
					Token token = tokenSequence.listWithWhiteSpace().get(i);
					if (LOG.isTraceEnabled()) {
						LOG.trace("Token : \"" + token.getText() + "\"");
					}
					
					// build a new heap for this iteration
					PriorityQueue<TokenisedAtomicTokenSequence> previousHeap = heap;
					heap = new PriorityQueue<TokenisedAtomicTokenSequence>();
					
					if (i==0) {
						// first token is always "separate" from the outside world
						Decision<TokeniserOutcome> decision = this.tokeniserDecisionFactory.createDefaultDecision(TokeniserOutcome.SEPARATE);
						decision.addAuthority("_" + this.getClass().getSimpleName());
						decision.addAuthority("_" + "DefaultDecision");
						
						TaggedToken<TokeniserOutcome> taggedToken = this.tokeniserService.getTaggedToken(token, decision);

						TokenisedAtomicTokenSequence newSequence = this.getTokeniserService().getTokenisedAtomicTokenSequence(emptySequence);
						newSequence.add(taggedToken);
						heap.add(newSequence);
						continue;
					}
					
					// limit the heap breadth to K
					int maxSequences = previousHeap.size() > this.getBeamWidth() ? this.getBeamWidth() : previousHeap.size();
					MONITOR.startTask("heap sort");
					try {
						for (int j = 0; j<maxSequences; j++) {
							TokenisedAtomicTokenSequence history = previousHeap.poll();

							// Find the separating & non-separating decisions
							if (history.size()>i) {
								// token already added as part of a sequence introduced by another token
								heap.add(history);
							} else if (tokenMatchSequenceMap.containsKey(token)) {
								// token begins one or more match sequences
								// these are ordered from shortest to longest (via TreeSet)
								List<TokenPatternMatchSequence> matchSequences = new ArrayList<TokenPatternMatchSequence>(tokenMatchSequenceMap.get(token));
								
								// Since sequences P1..Pn contain each other,
								// there can be exactly matchSequences.size() consistent solutions
								// Assume the default is separate
								// 0: all separate
								// 1: join P1, separate rest
								// 2: join P2, separate rest
								// ...
								// n: join Pn
								// We need to add each of these to the heap
								// by taking the product of all probabilities consistent with each solution
								// The probabities for each solution are (j=join, s=separate)
								// All separate: s1 x s2 x ... x sn
								// P1: j1 x s2 x ... x sn
								// P2: j1 x j2 x ... x sn
								// ...
								// Pn: j1 x j2 x ... x jn
								// Any solution of the form s1 x j2 would be inconsistent, and is not considered
								// If Pi and Pj start and end on the exact same token, then the solution for both is
								// Pi: j1 x ... x ji x jj x sj+1 ... x sn
								// Pj: j1 x ... x ji x jj x sj+1 ... x sn
								// Note of course that we're never likely to have more than two Ps here,
								// but we need a solution for more just to be sure to be sure
								TokeniserOutcome defaultOutcome = defaultDecisions.get(token.getIndexWithWhiteSpace()).getOutcome();
								TokeniserOutcome otherOutcome = null;
								if (defaultOutcome==TokeniserOutcome.SEPARATE)
									otherOutcome = TokeniserOutcome.JOIN;
								else
									otherOutcome = TokeniserOutcome.SEPARATE;
								
								double[] decisionProbs = new double[matchSequences.size()+1];
								for (int k=0; k<decisionProbs.length; k++)
									decisionProbs[k] = 1;
								
								// Note: k0 = default decision (e.g. separate all), k1=first pattern
								// p1 = first pattern
								int p=1;
								int prevEndIndex = -1;
								for (TokenPatternMatchSequence matchSequence : matchSequences) {
									int endIndex = matchSequence.getTokensToCheck().get(matchSequence.getTokensToCheck().size()-1).getEndIndex();
									List<Decision<TokeniserOutcome>> decisions = matchSequenceDecisionMap.get(matchSequence);
									for (Decision<TokeniserOutcome> decision : decisions) {
										for (int k=0; k<decisionProbs.length; k++) {
											if (decision.getOutcome()==defaultOutcome) {
												// e.g. separate in most cases
												if (k<p && endIndex > prevEndIndex)
													decisionProbs[k] *= decision.getProbability();
												else if (k+1<p && endIndex <= prevEndIndex)
													decisionProbs[k] *= decision.getProbability();
											} else {
												// e.g. join in most cases
												if (k>=p && endIndex > prevEndIndex)
													decisionProbs[k] *= decision.getProbability();
												else if (k+1>=p && endIndex <= prevEndIndex)
													decisionProbs[k] *= decision.getProbability();
											}
										} // next k
									} // next decision (only 2 of these)
									prevEndIndex = endIndex;
									p++;
								}
								
								// transform to probability distribution
								double sumProbs = 0;
								for (int k=0; k<decisionProbs.length; k++)
									sumProbs += decisionProbs[k];
								
								if (sumProbs>0)
									for (int k=0; k<decisionProbs.length; k++)
										decisionProbs[k] /= sumProbs;
								
								// Apply default decision
								// Since this is the default decision for all tokens in the sequence, we don't add the other tokens for now,
								// so as to allow them
								// to get examined one at a time, just in case one of them starts its own separate sequence
								Decision<TokeniserOutcome> defaultDecision = this.tokeniserDecisionFactory.createDecision(defaultOutcome.getCode(), decisionProbs[0]);
								defaultDecision.addAuthority("_" + this.getClass().getSimpleName());
								defaultDecision.addAuthority("_" + "Patterns");
								for (TokenPatternMatchSequence matchSequence : matchSequences) {
									defaultDecision.addAuthority(matchSequence.getTokenPattern().getName());
								}
								
								TaggedToken<TokeniserOutcome> defaultTaggedToken = this.tokeniserService.getTaggedToken(token, defaultDecision);
								TokenisedAtomicTokenSequence defaultSequence = this.getTokeniserService().getTokenisedAtomicTokenSequence(history);
								defaultSequence.add(defaultTaggedToken);
								defaultSequence.addDecision(defaultDecision);
								heap.add(defaultSequence);
								
								// Apply one non-default decision per match sequence
								for (int k=0; k<matchSequences.size(); k++) {
									TokenPatternMatchSequence matchSequence = matchSequences.get(k);
									double prob = decisionProbs[k+1];
									Decision<TokeniserOutcome> decision = this.tokeniserDecisionFactory.createDecision(otherOutcome.getCode(), prob);
									decision.addAuthority("_" + this.getClass().getSimpleName());
									decision.addAuthority("_" + "Patterns");
									decision.addAuthority(matchSequence.getTokenPattern().getName());
									
									TaggedToken<TokeniserOutcome> taggedToken = this.tokeniserService.getTaggedToken(token, decision);

									TokenisedAtomicTokenSequence newSequence = this.getTokeniserService().getTokenisedAtomicTokenSequence(history);
									newSequence.add(taggedToken);
									newSequence.addDecision(decision);
									
									// The decision is NOT the default decision for all tokens in the sequence, add all other tokens
									// in this sequence to the solution
									for (Token tokenInSequence : matchSequence.getTokensToCheck()) {
										if (tokenInSequence.equals(token)) {
											continue;
										}
										Decision<TokeniserOutcome> decisionInSequence = this.tokeniserDecisionFactory.createDefaultDecision(decision.getOutcome());
										decisionInSequence.addAuthority("_" + this.getClass().getSimpleName());
										decisionInSequence.addAuthority("_" + "DecisionInSequence");
										decisionInSequence.addAuthority("_" + "DecisionInSequence_non_default");
										decisionInSequence.addAuthority("_" + "Patterns");
										TaggedToken<TokeniserOutcome> taggedTokenInSequence = this.tokeniserService.getTaggedToken(tokenInSequence, decisionInSequence);
										newSequence.add(taggedTokenInSequence);
									}
									
									heap.add(newSequence);
									
								} // next sequence
							} else {
								// token doesn't start match sequence, and hasn't already been added to the current sequence
								Decision<TokeniserOutcome> decision = defaultDecisions.get(i);
								if (matchedTokens.contains(token)) {
									decision = this.tokeniserDecisionFactory.createDefaultDecision(decision.getOutcome());
									decision.addAuthority("_" + this.getClass().getSimpleName());
									decision.addAuthority("_" + "DecisionInSequence");
									decision.addAuthority("_" + "DecisionInSequence_default");
									decision.addAuthority("_" + "Patterns");
								}
								TaggedToken<TokeniserOutcome> taggedToken = this.tokeniserService.getTaggedToken(token, decision);

								TokenisedAtomicTokenSequence newSequence = this.getTokeniserService().getTokenisedAtomicTokenSequence(history);
								newSequence.add(taggedToken);
								heap.add(newSequence);
							}
		
						} // next sequence in the old heap
					} finally {
						MONITOR.endTask();
					}
				} // next token
				
				sequences = new ArrayList<TokenisedAtomicTokenSequence>();
				int k = 0;
				while (!heap.isEmpty()) {
					sequences.add(heap.poll());
					k++;
					if (k>=this.getBeamWidth())
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
					LOG.debug("After filters:      " + newTokenSequence);
				}
			}
	
			return sequences;
		} finally {
			MONITOR.endTask();
		}
	}
	
	TokenisedAtomicTokenSequence applyDecision(Token token, Decision<TokeniserOutcome> decision, TokenisedAtomicTokenSequence history, TokenPatternMatchSequence matchSequence, Decision<TokeniserOutcome> defaultDecision) {
		TaggedToken<TokeniserOutcome> taggedToken = this.tokeniserService.getTaggedToken(token, decision);

		TokenisedAtomicTokenSequence tokenisedSequence = this.getTokeniserService().getTokenisedAtomicTokenSequence(history);
		tokenisedSequence.add(taggedToken);
		if (decision.isStatistical())
			tokenisedSequence.addDecision(decision);
		
		if (matchSequence!=null) {
			for (Token otherToken : matchSequence.getTokensToCheck()) {
				if (otherToken.equals(token)) {
					continue;
				}
				TaggedToken<TokeniserOutcome> anotherTaggedToken = this.tokeniserService.getTaggedToken(otherToken, decision);
				tokenisedSequence.add(anotherTaggedToken);
			}
		}
		
		return tokenisedSequence;

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
