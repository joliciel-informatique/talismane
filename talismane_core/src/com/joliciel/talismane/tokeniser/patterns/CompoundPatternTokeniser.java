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
import java.util.Map.Entry;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.filters.FilterService;
import com.joliciel.talismane.filters.Sentence;
import com.joliciel.talismane.machineLearning.AnalysisObserver;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.FeatureService;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.tokeniser.SeparatorDecision;
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
class CompoundPatternTokeniser implements Tokeniser {
	private static final Log LOG = LogFactory.getLog(CompoundPatternTokeniser.class);
	private static final PerformanceMonitor MONITOR = PerformanceMonitor.getMonitor(CompoundPatternTokeniser.class);

	private static final DecimalFormat df = new DecimalFormat("0.0000");
	
	private Map<SeparatorDecision, String> separatorDefaults;
	private Map<SeparatorDecision, Pattern> separatorDefaultPatterns;
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
	
	private List<AnalysisObserver<TokeniserOutcome>> observers = new ArrayList<AnalysisObserver<TokeniserOutcome>>();
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
			List<Decision<TokeniserOutcome>> defaultDecisions = this.makeDefaultDecisions(tokenSequence);
			List<TokenisedAtomicTokenSequence> sequences = null;
			
			// For each test pattern, see if anything in the sentence matches it
			if (this.decisionMaker!=null) {
				List<TokenPatternMatchSequence> matchingSequences = new ArrayList<TokenPatternMatchSequence>();
				Map<Token,List<TokenPatternMatchSequence>> tokenMatchSequenceMap = new HashMap<Token, List<TokenPatternMatchSequence>>();
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
							
							List<TokenPatternMatchSequence> matchSequences = tokenMatchSequenceMap.get(token);
							if (matchSequences==null) {
								matchSequences = new ArrayList<TokenPatternMatchSequence>();
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
					MONITOR.endTask("pattern matching");
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
						MONITOR.endTask("analyse features");
					}
					
					List<Decision<TokeniserOutcome>> decisions = null;
					MONITOR.startTask("make decision");
					try {
						decisions = this.decisionMaker.decide(tokenFeatureResults);
						
						for (AnalysisObserver<TokeniserOutcome> observer : this.observers)
							observer.onAnalyse(match.getToken(), tokenFeatureResults, decisions);
						
						for (Decision<TokeniserOutcome> decision: decisions) {
							decision.addAuthority(this.getClass().getSimpleName());
							decision.addAuthority(match.getPattern().toString());
							decision.addAuthority("Patterns");
						}
					} finally {
						MONITOR.endTask("make decision");
					}
					
					matchSequenceDecisionMap.put(matchSequence, decisions);
				}
				
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
					MONITOR.startTask("heap sort");
					try {
						for (int j = 0; j<maxSequences; j++) {
							TokenisedAtomicTokenSequence history = previousHeap.poll();

							// Find the separating & non-separating decisions
							if (history.size()>i) {
								// token already added as part of a sequence introduced by another token
								heap.add(history);
							} else if (tokenMatchSequenceMap.containsKey(token)) {
								for (TokenPatternMatchSequence matchSequence : tokenMatchSequenceMap.get(token)) {
									List<Decision<TokeniserOutcome>> decisions = matchSequenceDecisionMap.get(matchSequence);
									
									for (Decision<TokeniserOutcome> decision : decisions) {
										TaggedToken<TokeniserOutcome> taggedToken = this.tokeniserService.getTaggedToken(token, decision);

										TokenisedAtomicTokenSequence newSequence = this.getTokeniserService().getTokenisedAtomicTokenSequence(history);
										newSequence.add(taggedToken);
										if (decision.isStatistical())
											newSequence.addDecision(decision);
										
										boolean defaultDecision = true;
										for (Token tokenInSequence : matchSequence.getTokensToCheck()) {
											if (decision.getOutcome()!=defaultDecisions.get(tokenInSequence.getIndexWithWhiteSpace()).getOutcome()) {
												defaultDecision = false;
												break;
											}
										}
										
										// If the decision is NOT the default decision for all tokens in the sequence, add all other tokens
										// in this sequence to the solution
										// If it IS the default decision for all tokens in the sequence, we don't add the other tokens for now, so as to allow them
										// to get examined one at a time, just in case one of them starts its own separate sequence
										if (!defaultDecision) {
											for (Token tokenInSequence : matchSequence.getTokensToCheck()) {
												if (tokenInSequence.equals(token)) {
													continue;
												}
												Decision<TokeniserOutcome> decisionInSequence = this.tokeniserDecisionFactory.createDefaultDecision(decision.getOutcome());
												decisionInSequence.addAuthority(this.getClass().getSimpleName());
												decisionInSequence.addAuthority("DecisionInSequence");
												decisionInSequence.addAuthority("DecisionInSequence_non_default");
												decisionInSequence.addAuthority("Patterns");
												TaggedToken<TokeniserOutcome> taggedTokenInSequence = this.tokeniserService.getTaggedToken(tokenInSequence, decisionInSequence);
												newSequence.add(taggedTokenInSequence);
											}
										}
										
										heap.add(newSequence);
									} // next decision
								} // next sequence
							} else {
								// token doesn't start match sequence, and hasn't already been added to the current sequence
								Decision<TokeniserOutcome> decision = defaultDecisions.get(i);
								if (matchedTokens.contains(token)) {
									decision = this.tokeniserDecisionFactory.createDefaultDecision(decision.getOutcome());
									decision.addAuthority(this.getClass().getSimpleName());
									decision.addAuthority("DecisionInSequence");
									decision.addAuthority("DecisionInSequence_default");
									decision.addAuthority("Patterns");
								}
								TaggedToken<TokeniserOutcome> taggedToken = this.tokeniserService.getTaggedToken(token, decision);

								TokenisedAtomicTokenSequence newSequence = this.getTokeniserService().getTokenisedAtomicTokenSequence(history);
								newSequence.add(taggedToken);
								heap.add(newSequence);
							}
		
						} // next sequence in the old heap
					} finally {
						MONITOR.endTask("heap sort");
					}
					i++;
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
			MONITOR.endTask("tokeniseWithDecisions");
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
	
	protected List<Decision<TokeniserOutcome>> makeDefaultDecisions(TokenSequence tokenSequence) {
		List<Decision<TokeniserOutcome>> defaultDecisions = new ArrayList<Decision<TokeniserOutcome>>();
		
		// Assign each separator its default value
		TokeniserOutcome nextOutcome = TokeniserOutcome.DOES_SEPARATE;
		for (Token token : tokenSequence.listWithWhiteSpace()) {
			Decision<TokeniserOutcome> tokeniserDecision = null;
			
			if (Tokeniser.SEPARATORS.matcher(token.getText()).matches()) {
				boolean defaultValueFound = false;
				TokeniserOutcome outcome = null;
				for (Entry<SeparatorDecision, Pattern> entry : this.getSeparatorDefaultPatterns().entrySet()) {
					if (entry.getValue().matcher(token.getText()).matches()) {
						defaultValueFound = true;
						SeparatorDecision defaultSeparatorDecision = entry.getKey();
						switch (defaultSeparatorDecision) {
						case IS_SEPARATOR:
							outcome = TokeniserOutcome.DOES_SEPARATE;
							nextOutcome = TokeniserOutcome.DOES_SEPARATE;
							break;
						case IS_NOT_SEPARATOR:
							outcome = TokeniserOutcome.DOES_NOT_SEPARATE;
							nextOutcome = TokeniserOutcome.DOES_NOT_SEPARATE;
							break;
						case IS_SEPARATOR_BEFORE:
							outcome = TokeniserOutcome.DOES_SEPARATE;
							nextOutcome = TokeniserOutcome.DOES_NOT_SEPARATE;
						case IS_SEPARATOR_AFTER:
							outcome = TokeniserOutcome.DOES_NOT_SEPARATE;
							nextOutcome = TokeniserOutcome.DOES_SEPARATE;
						}
						break;
					}
				}
				if (!defaultValueFound) {
					outcome = TokeniserOutcome.DOES_SEPARATE;
					nextOutcome = TokeniserOutcome.DOES_SEPARATE;
				}
				tokeniserDecision = this.tokeniserDecisionFactory.createDefaultDecision(outcome);
			} else {
				tokeniserDecision = this.tokeniserDecisionFactory.createDefaultDecision(nextOutcome);
			}
			tokeniserDecision.addAuthority(this.getClass().getSimpleName());
			tokeniserDecision.addAuthority("PatternSeparator:DefaultSeparatorDecison");
			defaultDecisions.add(tokeniserDecision);
		}
		return defaultDecisions;
	}
	/**
	 * Default values for various separators.
	 * All separators not in this map will default to TokeniserDecision.IS_SEPARATOR.
	 * The Strings should be simple lists of separators for which the TokeniserDecision is the default one.
	 * @return
	 */
	public Map<SeparatorDecision, String> getSeparatorDefaults() {
		if (this.separatorDefaults==null) {
			this.setSeparatorDefaults(this.getTokeniserPatternManager().getSeparatorDefaults());
		}
		return separatorDefaults;
	}

	public void setSeparatorDefaults(
			Map<SeparatorDecision, String> separatorDefaults) {
		this.separatorDefaults = separatorDefaults;
	}

	protected Map<SeparatorDecision, Pattern> getSeparatorDefaultPatterns() {
		if (this.separatorDefaultPatterns==null) {
			this.separatorDefaultPatterns = new HashMap<SeparatorDecision, Pattern>();
			for (Entry<SeparatorDecision, String> entry : this.getSeparatorDefaults().entrySet()) {
				String separators = entry.getValue();
				StringBuilder sb = new StringBuilder();
				for (int i=0; i<separators.length(); i++) {
					char c = separators.charAt(i);
					sb.append('\\');
					sb.append(c);
				}
				Pattern pattern = Pattern.compile("[" + sb.toString() + "]");
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
	public void addObserver(AnalysisObserver<TokeniserOutcome> observer) {
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
