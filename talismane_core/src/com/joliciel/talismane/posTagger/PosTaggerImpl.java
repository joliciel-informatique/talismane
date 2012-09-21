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
package com.joliciel.talismane.posTagger;

import java.util.List;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.machineLearning.AnalysisObserver;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.posTagger.PosTaggerLexiconService;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagger;
import com.joliciel.talismane.posTagger.features.PosTaggerContext;
import com.joliciel.talismane.posTagger.features.PosTaggerFeature;
import com.joliciel.talismane.posTagger.features.PosTaggerFeatureService;
import com.joliciel.talismane.posTagger.features.PosTaggerRule;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.TokeniserService;
import com.joliciel.talismane.tokeniser.filters.TokenFilter;
import com.joliciel.talismane.utils.PerformanceMonitor;

/**
 * Performs POS tagging by applying a beam search to MaxEnt model results.
 * Incorporates various methods of using a lexicon to constrain results.
 * @author Assaf Urieli
 *
 */
class PosTaggerImpl implements PosTagger, NonDeterministicPosTagger {
	private static final Log LOG = LogFactory.getLog(PosTaggerImpl.class);
	private static final double MIN_PROB_TO_STORE = 0.001;
	
	private PosTaggerService posTaggerService;
	private PosTaggerFeatureService posTaggerFeatureService;
	private TokeniserService tokeniserService;
	private PosTaggerLexiconService lexiconService;
	private DecisionMaker<PosTag> decisionMaker;
	
	private Set<PosTaggerFeature<?>> posTaggerFeatures;
	private List<PosTaggerRule> posTaggerRules;
	private List<PosTaggerRule> posTaggerPositiveRules;
	private List<PosTaggerRule> posTaggerNegativeRules;
	
	private List<TokenFilter> preprocessingFilters = new ArrayList<TokenFilter>();

	private int beamWidth;
	private PosTagSet posTagSet;

	private List<AnalysisObserver> observers = new ArrayList<AnalysisObserver>();
	
	/**
	 * 
	 * @param model the MaxEnt model to use
	 * @param posTaggerFeatures the set of PosTaggerFeatures used by this model
	 * @param tagSet the tagset used by this model
	 * @param beamWidth the maximum beamwidth to consider during the beam search
	 * @param fScoreCalculator an f-score calculator for evaluating results
	 */
	public PosTaggerImpl(Set<PosTaggerFeature<?>> posTaggerFeatures,
			PosTagSet posTagSet,
			DecisionMaker<PosTag> decisionMaker,
			int beamWidth) {
		this.posTaggerFeatures = posTaggerFeatures;
		this.posTagSet = posTagSet;
		this.beamWidth = beamWidth;
		this.decisionMaker = decisionMaker;
	}

	@Override
	public List<PosTagSequence> tagSentence(List<TokenSequence> tokenSequences) {
		PerformanceMonitor.startTask("PosTaggerImpl.tagSentence");
		try {
			PerformanceMonitor.startTask("PosTaggerImpl.apply filters");
			try {
				for (TokenSequence tokenSequence : tokenSequences) {
					for (TokenFilter tokenFilter : this.preprocessingFilters) {
						tokenFilter.apply(tokenSequence);
					}
				}
			} finally {
				PerformanceMonitor.endTask("PosTaggerImpl.apply filters");
			}
			int sentenceLength = tokenSequences.get(0).getSentence().length();
			
			TreeMap<Double, PriorityQueue<PosTagSequence>> heaps = new TreeMap<Double, PriorityQueue<PosTagSequence>>();
			
			PriorityQueue<PosTagSequence> heap0 = new PriorityQueue<PosTagSequence>();
			for (TokenSequence tokenSequence : tokenSequences) {
				// add an empty PosTagSequence for each token sequence
				PosTagSequence emptySequence = this.getPosTaggerService().getPosTagSequence(tokenSequence, 0);
				heap0.add(emptySequence);
			}
			heaps.put(0.0, heap0);
			
			PriorityQueue<PosTagSequence> finalHeap = null;
			while (heaps.size()>0) {
				Entry<Double, PriorityQueue<PosTagSequence>> heapEntry = heaps.pollFirstEntry();
				if (LOG.isTraceEnabled()) {
					LOG.trace("heap key: " + heapEntry.getKey() + ", sentence length: " + sentenceLength);
				}
				if (heapEntry.getKey()==sentenceLength) {
					finalHeap = heapEntry.getValue();
					break;
				}
				PriorityQueue<PosTagSequence> previousHeap = heapEntry.getValue();
				
				// limit the breadth to K
				int maxSequences = previousHeap.size() > this.beamWidth ? this.beamWidth : previousHeap.size();
				
				for (int j = 0; j<maxSequences; j++) {
					PosTagSequence history = previousHeap.poll();
					Token token = history.getNextToken();
					if (LOG.isTraceEnabled())
						LOG.trace("Token: " + token.getText());
					
					PosTaggerContext context = this.getPosTaggerFeatureService().getContext(token, history);
					List<Decision<PosTag>> decisions = new ArrayList<Decision<PosTag>>();
					
					// test the positive rules on the current token
					boolean ruleApplied = false;
					if (posTaggerPositiveRules!=null) {
						PerformanceMonitor.startTask("PosTaggerImpl.check rules");
						try {
							for (PosTaggerRule rule : posTaggerPositiveRules) {
								if (LOG.isTraceEnabled()) {
									LOG.trace("Checking rule: " + rule.getCondition().getName());
								}
								FeatureResult<Boolean> ruleResult = rule.getCondition().check(context);
								if (ruleResult!=null && ruleResult.getOutcome()) {
									Decision<PosTag> positiveRuleDecision = this.posTagSet.createDefaultDecision(rule.getTag());
									decisions.add(positiveRuleDecision);
									positiveRuleDecision.addAuthority(rule.getCondition().getName());
									ruleApplied = true;
									if (LOG.isTraceEnabled()) {
										LOG.trace("Rule applies. Setting posTag to: " + rule.getTag().getCode());
									}
									break;
								}
							}
						} finally {
							PerformanceMonitor.endTask("PosTaggerImpl.check rules");
						}
					}
					
					if (!ruleApplied) {
						// test the features on the current token
						List<FeatureResult<?>> featureResults = new ArrayList<FeatureResult<?>>();
						PerformanceMonitor.startTask("PosTaggerImpl.analyse features");
						try {
							for (PosTaggerFeature<?> posTaggerFeature : posTaggerFeatures) {
								PerformanceMonitor.startTask(posTaggerFeature.getGroupName());
								try {
									FeatureResult<?> featureResult = posTaggerFeature.check(context);
									if (featureResult!=null)
										featureResults.add(featureResult);
								} finally {
									PerformanceMonitor.endTask(posTaggerFeature.getGroupName());
								}
							}
							if (LOG.isTraceEnabled()) {
								for (FeatureResult<?> result : featureResults) {
									LOG.trace(result.toString());
								}
							}	
						} finally {
							PerformanceMonitor.endTask("PosTaggerImpl.analyse features");
						}
						
						// evaluate the feature results using the maxent model
						PerformanceMonitor.startTask("PosTaggerImpl.decision maker");
						decisions = this.decisionMaker.decide(featureResults);
						PerformanceMonitor.endTask("PosTaggerImpl.decision maker");
						
						for (AnalysisObserver observer : this.observers) {
							observer.onAnalyse(token, featureResults, decisions);
						}
		
						// apply the negative rules
						Set<PosTag> eliminatedPosTags = new TreeSet<PosTag>();
						if (posTaggerNegativeRules!=null) {
							PerformanceMonitor.startTask("PosTaggerImpl.check negative rules");
							try {
								for (PosTaggerRule rule : posTaggerNegativeRules) {
									if (LOG.isTraceEnabled()) {
										LOG.trace("Checking negative rule: " + rule.getCondition().getName());
									}
									FeatureResult<Boolean> ruleResult = rule.getCondition().check(context);
									if (ruleResult!=null && ruleResult.getOutcome()) {
										eliminatedPosTags.add(rule.getTag());
										if (LOG.isTraceEnabled()) {
											LOG.trace("Rule applies. Eliminating posTag: " + rule.getTag().getCode());
										}
									}
								}
								
								if (eliminatedPosTags.size()>0) {
									List<Decision<PosTag>> decisionShortList = new ArrayList<Decision<PosTag>>();
									for (Decision<PosTag> decision : decisions) {
										if (!eliminatedPosTags.contains(decision.getOutcome())) {
											decisionShortList.add(decision);
										} else {
											LOG.trace("Eliminating decision: " + decision.toString());
										}
									}
									if (decisionShortList.size()>0) {
										decisions = decisionShortList;
									} else {
										LOG.debug("All decisions eliminated! Restoring original decisions.");
									}
								}
							} finally {
								PerformanceMonitor.endTask("PosTaggerImpl.check negative rules");
							}
						}
						
						// is this a known word in the lexicon?
						PerformanceMonitor.startTask("PosTaggerImpl.apply constraints");
						try {
							if (LOG.isTraceEnabled()) {
								String posTags = "";
								for (PosTag onePosTag : token.getPossiblePosTags()) {
									posTags += onePosTag.getCode() + ",";
								}
								LOG.trace("Token: " + token.getText() + ". PosTags: " + posTags);
							}
							
							List<Decision<PosTag>> decisionShortList = new ArrayList<Decision<PosTag>>();
							
							for (Decision<PosTag> decision : decisions) {
								if (decision.getProbability()>=MIN_PROB_TO_STORE) {
									decisionShortList.add(decision);
								}
							}
							if (decisionShortList.size()>0) {
								decisions = decisionShortList;
							}
						} finally {
							PerformanceMonitor.endTask("PosTaggerImpl.apply constraints");		
						}
					} // has a rule been applied?
					
					// add new TaggedTokenSequences to the heap, one for each outcome provided by MaxEnt
					PerformanceMonitor.startTask("PosTaggerImpl.heap sort");
					for (Decision<PosTag> decision : decisions) {
						if (LOG.isTraceEnabled())
							LOG.trace("Outcome: " + decision.getOutcome() + ", " + decision.getProbability());
	
						PosTaggedToken posTaggedToken = this.getPosTaggerService().getPosTaggedToken(token, decision);
						PosTagSequence sequence = this.getPosTaggerService().getPosTagSequence(history);
						sequence.add(posTaggedToken);
						if (decision.isStatistical())
							sequence.addDecision(decision);
						
						double heapIndex = token.getEndIndex();
						// add another half for an empty token, to differentiate it from regular ones
						if (token.getStartIndex()==token.getEndIndex())
							heapIndex += 0.5;
	
						if (LOG.isTraceEnabled())
							LOG.trace("Heap index: " + heapIndex);
						
						PriorityQueue<PosTagSequence> heap = heaps.get(heapIndex);
						if (heap==null) {
							heap = new PriorityQueue<PosTagSequence>();
							heaps.put(heapIndex, heap);
						}
						heap.add(sequence);
					} // next outcome for this token
					PerformanceMonitor.endTask("PosTaggerImpl.heap sort");
				} // next history		
			} // next atomic index
			// return the best sequence on the heap
			List<PosTagSequence> sequences = new ArrayList<PosTagSequence>();
			int i = 0;
			while (!finalHeap.isEmpty()) {
				sequences.add(finalHeap.poll());
				i++;
				if (i>=this.getBeamWidth())
					break;
			}
			return sequences;
		} finally {
			PerformanceMonitor.endTask("PosTaggerImpl.tagSentence");
		}
	}

	@Override
	public PosTagSequence tagSentence(TokenSequence tokenSequence) {
		List<TokenSequence> tokenSequences = new ArrayList<TokenSequence>();
		tokenSequences.add(tokenSequence);
		List<PosTagSequence> posTagSequences = this.tagSentence(tokenSequences);
		return posTagSequences.get(0);
	}
	

	public TokeniserService getTokeniserService() {
		return tokeniserService;
	}

	public void setTokeniserService(TokeniserService tokeniserService) {
		this.tokeniserService = tokeniserService;
	}

	public PosTaggerFeatureService getPosTaggerFeatureService() {
		return posTaggerFeatureService;
	}

	public void setPosTaggerFeatureService(
			PosTaggerFeatureService posTaggerFeatureService) {
		this.posTaggerFeatureService = posTaggerFeatureService;
	}

	public PosTaggerLexiconService getLexiconService() {
		return lexiconService;
	}

	public void setLexiconService(PosTaggerLexiconService lexiconService) {
		this.lexiconService = lexiconService;
	}

	public DecisionMaker<PosTag> getDecisionMaker() {
		return decisionMaker;
	}

	public void setDecisionMaker(DecisionMaker<PosTag> decisionMaker) {
		this.decisionMaker = decisionMaker;
	}

	@Override
	public int getBeamWidth() {
		return beamWidth;
	}

	public void setBeamWidth(int beamWidth) {
		this.beamWidth = beamWidth;
	}

	public PosTaggerService getPosTaggerService() {
		return posTaggerService;
	}

	public void setPosTaggerService(PosTaggerService posTaggerService) {
		this.posTaggerService = posTaggerService;
	}

	public PosTagSet getPosTagSet() {
		return posTagSet;
	}

	public void setPosTagSet(PosTagSet posTagSet) {
		this.posTagSet = posTagSet;
	}

	@Override
	public void addObserver(AnalysisObserver observer) {
		this.observers.add(observer);
	}

	@Override
	public List<PosTaggerRule> getPosTaggerRules() {
		return posTaggerRules;
	}

	@Override
	public void setPosTaggerRules(List<PosTaggerRule> posTaggerRules) {
		this.posTaggerRules = posTaggerRules;
		this.posTaggerPositiveRules = new ArrayList<PosTaggerRule>();
		this.posTaggerNegativeRules = new ArrayList<PosTaggerRule>();
		for (PosTaggerRule rule : posTaggerRules) {
			if (rule.isNegative())
				posTaggerNegativeRules.add(rule);
			else
				posTaggerPositiveRules.add(rule);
		}
	}

	@Override
	public Set<PosTaggerFeature<?>> getPosTaggerFeatures() {
		return posTaggerFeatures;
	}
	
	public List<TokenFilter> getPreprocessingFilters() {
		return preprocessingFilters;
	}

	public void setPreprocessingFilters(List<TokenFilter> tokenFilters) {
		this.preprocessingFilters = tokenFilters;
	}
	
	public void addPreprocessingFilter(TokenFilter tokenFilter) {
		this.preprocessingFilters.add(tokenFilter);
	}
	
}
