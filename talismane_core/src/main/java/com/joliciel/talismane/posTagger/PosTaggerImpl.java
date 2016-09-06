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
package com.joliciel.talismane.posTagger;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneService;
import com.joliciel.talismane.machineLearning.ClassificationObserver;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.posTagger.features.PosTaggerFeature;
import com.joliciel.talismane.posTagger.features.PosTaggerRule;
import com.joliciel.talismane.posTagger.filters.PosTagSequenceFilter;
import com.joliciel.talismane.tokeniser.StringAttribute;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;
import com.joliciel.talismane.utils.PerformanceMonitor;

/**
 * Performs POS tagging by applying a beam search to MaxEnt model results.
 * Incorporates various methods of using a lexicon to constrain results.
 * 
 * @author Assaf Urieli
 *
 */
class PosTaggerImpl implements PosTagger, NonDeterministicPosTagger {
	private static final Logger LOG = LoggerFactory.getLogger(PosTaggerImpl.class);
	private static final PerformanceMonitor MONITOR = PerformanceMonitor.getMonitor(PosTaggerImpl.class);
	private static final double MIN_PROB_TO_STORE = 0.001;
	private static final DecimalFormat df = new DecimalFormat("0.0000");

	private TalismaneService talismaneService;
	private PosTaggerServiceInternal posTaggerService;

	private DecisionMaker decisionMaker;

	private Set<PosTaggerFeature<?>> posTaggerFeatures;
	private List<PosTaggerRule> posTaggerRules;
	private List<PosTaggerRule> posTaggerPositiveRules;
	private List<PosTaggerRule> posTaggerNegativeRules;

	private List<TokenSequenceFilter> preProcessingFilters = new ArrayList<TokenSequenceFilter>();
	private List<PosTagSequenceFilter> postProcessingFilters = new ArrayList<PosTagSequenceFilter>();

	private int beamWidth;

	private List<ClassificationObserver> observers = new ArrayList<ClassificationObserver>();

	/**
	 * 
	 * @param posTaggerFeatures
	 *            the set of PosTaggerFeatures used by the model which provided
	 *            the decision maker
	 * @param decisionMaker
	 *            the decision maker used to make pos-tagging decisions
	 * @param beamWidth
	 *            the maximum beamwidth to consider during the beam search
	 * @param fScoreCalculator
	 *            an f-score calculator for evaluating results
	 */
	public PosTaggerImpl(Set<PosTaggerFeature<?>> posTaggerFeatures, DecisionMaker decisionMaker, int beamWidth) {
		this.posTaggerFeatures = posTaggerFeatures;
		this.beamWidth = beamWidth;
		this.decisionMaker = decisionMaker;
	}

	@Override
	public List<PosTagSequence> tagSentence(List<TokenSequence> tokenSequences) {
		MONITOR.startTask("tagSentence");
		try {
			MONITOR.startTask("apply filters");
			try {
				for (TokenSequence tokenSequence : tokenSequences) {
					for (TokenSequenceFilter tokenFilter : this.preProcessingFilters) {
						tokenFilter.apply(tokenSequence);
					}
				}
			} finally {
				MONITOR.endTask();
			}
			int sentenceLength = tokenSequences.get(0).getText().length();

			TreeMap<Double, PriorityQueue<PosTagSequence>> heaps = new TreeMap<Double, PriorityQueue<PosTagSequence>>();

			PriorityQueue<PosTagSequence> heap0 = new PriorityQueue<PosTagSequence>();
			for (TokenSequence tokenSequence : tokenSequences) {
				// add an empty PosTagSequence for each token sequence
				PosTagSequence emptySequence = new PosTagSequence(tokenSequence);
				emptySequence.setScoringStrategy(decisionMaker.getDefaultScoringStrategy());
				heap0.add(emptySequence);
			}
			heaps.put(0.0, heap0);

			PriorityQueue<PosTagSequence> finalHeap = null;
			while (heaps.size() > 0) {
				Entry<Double, PriorityQueue<PosTagSequence>> heapEntry = heaps.pollFirstEntry();
				if (LOG.isTraceEnabled()) {
					LOG.trace("heap key: " + heapEntry.getKey() + ", sentence length: " + sentenceLength);
				}
				if (heapEntry.getKey() == sentenceLength) {
					finalHeap = heapEntry.getValue();
					break;
				}
				PriorityQueue<PosTagSequence> previousHeap = heapEntry.getValue();

				// limit the breadth to K
				int maxSequences = previousHeap.size() > this.beamWidth ? this.beamWidth : previousHeap.size();

				for (int j = 0; j < maxSequences; j++) {
					PosTagSequence history = previousHeap.poll();
					Token token = history.getNextToken();
					if (LOG.isTraceEnabled()) {
						LOG.trace("#### Next history ( " + heapEntry.getKey() + "): " + history.toString());
						LOG.trace("Prob: " + df.format(history.getScore()));
						LOG.trace("Token: " + token.getText());

						StringBuilder sb = new StringBuilder();
						for (Token oneToken : history.getTokenSequence().listWithWhiteSpace()) {
							if (oneToken.equals(token))
								sb.append("[" + oneToken + "]");
							else
								sb.append(oneToken);
						}
						LOG.trace(sb.toString());
					}

					PosTaggerContext context = new PosTaggerContextImpl(token, history);
					List<Decision> decisions = new ArrayList<Decision>();

					boolean ruleApplied = false;

					// does this token have an explicit pos-tag already
					// assigned?
					if (token.getAttributes().containsKey(PosTagger.POS_TAG_ATTRIBUTE)) {
						StringAttribute posTagCodeAttribute = (StringAttribute) token.getAttributes().get(PosTagger.POS_TAG_ATTRIBUTE);
						String posTagCode = posTagCodeAttribute.getValue();
						Decision positiveRuleDecision = new Decision(posTagCode);
						decisions.add(positiveRuleDecision);
						positiveRuleDecision.addAuthority("tokenAttribute");
						ruleApplied = true;
						if (LOG.isTraceEnabled()) {
							LOG.trace("Token has attribute \"" + PosTagger.POS_TAG_ATTRIBUTE + "\". Setting posTag to: " + posTagCode);
						}
					}

					// test the positive rules on the current token
					if (!ruleApplied) {
						if (posTaggerPositiveRules != null) {
							MONITOR.startTask("check rules");
							try {
								for (PosTaggerRule rule : posTaggerPositiveRules) {
									if (LOG.isTraceEnabled()) {
										LOG.trace("Checking rule: " + rule.getCondition().getName());
									}
									RuntimeEnvironment env = new RuntimeEnvironment();
									FeatureResult<Boolean> ruleResult = rule.getCondition().check(context, env);
									if (ruleResult != null && ruleResult.getOutcome()) {
										Decision positiveRuleDecision = new Decision(rule.getTag().getCode());
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
								MONITOR.endTask();
							}
						}
					}

					if (!ruleApplied) {
						// test the features on the current token
						List<FeatureResult<?>> featureResults = new ArrayList<FeatureResult<?>>();
						MONITOR.startTask("analyse features");
						try {
							for (PosTaggerFeature<?> posTaggerFeature : posTaggerFeatures) {
								MONITOR.startTask(posTaggerFeature.getCollectionName());
								try {
									RuntimeEnvironment env = new RuntimeEnvironment();
									FeatureResult<?> featureResult = posTaggerFeature.check(context, env);
									if (featureResult != null)
										featureResults.add(featureResult);
								} finally {
									MONITOR.endTask();
								}
							}
							if (LOG.isTraceEnabled()) {
								for (FeatureResult<?> result : featureResults) {
									LOG.trace(result.toString());
								}
							}
						} finally {
							MONITOR.endTask();
						}

						// evaluate the feature results using the maxent model
						MONITOR.startTask("make decision");
						try {
							decisions = this.decisionMaker.decide(featureResults);
						} finally {
							MONITOR.endTask();
						}

						for (ClassificationObserver observer : this.observers) {
							observer.onAnalyse(token, featureResults, decisions);
						}

						// apply the negative rules
						Set<String> eliminatedPosTags = new TreeSet<String>();
						if (posTaggerNegativeRules != null) {
							MONITOR.startTask("check negative rules");
							try {
								for (PosTaggerRule rule : posTaggerNegativeRules) {
									if (LOG.isTraceEnabled()) {
										LOG.trace("Checking negative rule: " + rule.getCondition().getName());
									}
									RuntimeEnvironment env = new RuntimeEnvironment();
									FeatureResult<Boolean> ruleResult = rule.getCondition().check(context, env);
									if (ruleResult != null && ruleResult.getOutcome()) {
										eliminatedPosTags.add(rule.getTag().getCode());
										if (LOG.isTraceEnabled()) {
											LOG.trace("Rule applies. Eliminating posTag: " + rule.getTag().getCode());
										}
									}
								}

								if (eliminatedPosTags.size() > 0) {
									List<Decision> decisionShortList = new ArrayList<Decision>();
									for (Decision decision : decisions) {
										if (!eliminatedPosTags.contains(decision.getOutcome())) {
											decisionShortList.add(decision);
										} else {
											LOG.trace("Eliminating decision: " + decision.toString());
										}
									}
									if (decisionShortList.size() > 0) {
										decisions = decisionShortList;
									} else {
										LOG.debug("All decisions eliminated! Restoring original decisions.");
									}
								}
							} finally {
								MONITOR.endTask();
							}
						}

						// is this a known word in the lexicon?
						MONITOR.startTask("apply constraints");
						try {
							if (LOG.isTraceEnabled()) {
								String posTags = "";
								for (PosTag onePosTag : token.getPossiblePosTags()) {
									posTags += onePosTag.getCode() + ",";
								}
								LOG.trace("Token: " + token.getText() + ". PosTags: " + posTags);
							}

							List<Decision> decisionShortList = new ArrayList<Decision>();

							for (Decision decision : decisions) {
								if (decision.getProbability() >= MIN_PROB_TO_STORE) {
									decisionShortList.add(decision);
								}
							}
							if (decisionShortList.size() > 0) {
								decisions = decisionShortList;
							}
						} finally {
							MONITOR.endTask();
						}
					} // has a rule been applied?

					// add new TaggedTokenSequences to the heap, one for each
					// outcome provided by MaxEnt
					MONITOR.startTask("heap sort");
					for (Decision decision : decisions) {
						if (LOG.isTraceEnabled())
							LOG.trace("Outcome: " + decision.getOutcome() + ", " + decision.getProbability());

						PosTaggedToken posTaggedToken = new PosTaggedToken(token, decision, talismaneService.getTalismaneSession());
						PosTagSequence sequence = new PosTagSequence(history);
						sequence.addPosTaggedToken(posTaggedToken);
						if (decision.isStatistical())
							sequence.addDecision(decision);

						double heapIndex = token.getEndIndex();
						// add another half for an empty token, to differentiate
						// it from regular ones
						if (token.getStartIndex() == token.getEndIndex())
							heapIndex += 0.5;

						// if it's the last token, make sure we end
						if (token.getIndex() == sequence.getTokenSequence().size() - 1)
							heapIndex = sentenceLength;

						if (LOG.isTraceEnabled())
							LOG.trace("Heap index: " + heapIndex);

						PriorityQueue<PosTagSequence> heap = heaps.get(heapIndex);
						if (heap == null) {
							heap = new PriorityQueue<PosTagSequence>();
							heaps.put(heapIndex, heap);
						}
						heap.add(sequence);
					} // next outcome for this token
					MONITOR.endTask();
				} // next history
			} // next atomic index
			  // return the best sequence on the heap
			List<PosTagSequence> sequences = new ArrayList<PosTagSequence>();
			int i = 0;
			while (!finalHeap.isEmpty()) {
				sequences.add(finalHeap.poll());
				i++;
				if (i >= this.getBeamWidth())
					break;
			}

			// apply post-processing filters
			LOG.debug("####Final postag sequences:");
			int j = 1;
			for (PosTagSequence sequence : sequences) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Sequence " + (j++) + ", score=" + df.format(sequence.getScore()));
					LOG.debug("Sequence before filters: " + sequence);
				}
				for (PosTagSequenceFilter filter : this.postProcessingFilters)
					filter.apply(sequence);

				if (LOG.isDebugEnabled()) {
					LOG.debug("Sequence after filters: " + sequence);
				}
			}

			return sequences;
		} finally {
			MONITOR.endTask();
		}
	}

	@Override
	public PosTagSequence tagSentence(TokenSequence tokenSequence) {
		List<TokenSequence> tokenSequences = new ArrayList<TokenSequence>();
		tokenSequences.add(tokenSequence);
		List<PosTagSequence> posTagSequences = this.tagSentence(tokenSequences);
		return posTagSequences.get(0);
	}

	public DecisionMaker getDecisionMaker() {
		return decisionMaker;
	}

	public void setDecisionMaker(DecisionMaker decisionMaker) {
		this.decisionMaker = decisionMaker;
	}

	@Override
	public int getBeamWidth() {
		return beamWidth;
	}

	public void setBeamWidth(int beamWidth) {
		this.beamWidth = beamWidth;
	}

	public PosTaggerServiceInternal getPosTaggerService() {
		return posTaggerService;
	}

	public void setPosTaggerService(PosTaggerServiceInternal posTaggerService) {
		this.posTaggerService = posTaggerService;
	}

	@Override
	public void addObserver(ClassificationObserver observer) {
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

	@Override
	public void setPosTaggerFeatures(Set<PosTaggerFeature<?>> posTaggerFeatures) {
		this.posTaggerFeatures = posTaggerFeatures;
	}

	@Override
	public List<TokenSequenceFilter> getPreProcessingFilters() {
		return preProcessingFilters;
	}

	@Override
	public void setPreProcessingFilters(List<TokenSequenceFilter> tokenFilters) {
		this.preProcessingFilters = tokenFilters;
	}

	@Override
	public void addPreProcessingFilter(TokenSequenceFilter tokenFilter) {
		this.preProcessingFilters.add(tokenFilter);
	}

	@Override
	public List<PosTagSequenceFilter> getPostProcessingFilters() {
		return postProcessingFilters;
	}

	@Override
	public void setPostProcessingFilters(List<PosTagSequenceFilter> posTagFilters) {
		this.postProcessingFilters = posTagFilters;
	}

	@Override
	public void addPostProcessingFilter(PosTagSequenceFilter posTagFilter) {
		this.postProcessingFilters.add(posTagFilter);
	}

	public TalismaneService getTalismaneService() {
		return talismaneService;
	}

	public void setTalismaneService(TalismaneService talismaneService) {
		this.talismaneService = talismaneService;
	}

}
