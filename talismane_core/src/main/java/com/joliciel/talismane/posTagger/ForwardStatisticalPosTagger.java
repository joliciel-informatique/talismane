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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.ClassificationObserver;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.ExternalResource;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.posTagger.features.PosTaggerFeature;
import com.joliciel.talismane.posTagger.features.PosTaggerFeatureParser;
import com.joliciel.talismane.posTagger.features.PosTaggerRule;
import com.joliciel.talismane.posTagger.filters.PosTagSequenceFilter;
import com.joliciel.talismane.tokeniser.StringAttribute;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;

/**
 * Performs part-of-speech tagging by applying a beam search to statistical
 * model results. Incorporates various methods of using a lexicon to constrain
 * results.
 * 
 * @author Assaf Urieli
 *
 */
public class ForwardStatisticalPosTagger implements PosTagger, NonDeterministicPosTagger {
	private static final Logger LOG = LoggerFactory.getLogger(ForwardStatisticalPosTagger.class);
	private static final double MIN_PROB_TO_STORE = 0.001;
	private static final DecimalFormat df = new DecimalFormat("0.0000");

	private List<PosTaggerRule> posTaggerRules;
	private List<PosTaggerRule> posTaggerPositiveRules;
	private List<PosTaggerRule> posTaggerNegativeRules;

	private final List<ClassificationObserver> observers;

	private final Set<PosTaggerFeature<?>> posTaggerFeatures;
	private final DecisionMaker decisionMaker;
	private final int beamWidth;
	private final boolean propagateTokeniserBeam;

	private final TalismaneSession session;

	/**
	 * 
	 * @param posTaggerFeatures
	 *            the set of PosTaggerFeatures used by the model which provided
	 *            the decision maker
	 * @param decisionMaker
	 *            the decision maker used to make pos-tagging decisions
	 * @param beamWidth
	 *            the maximum beamwidth to consider during the beam search
	 */
	public ForwardStatisticalPosTagger(Set<PosTaggerFeature<?>> posTaggerFeatures, DecisionMaker decisionMaker, int beamWidth, boolean propagateTokeniserBeam,
			TalismaneSession talismaneSession) {
		this.posTaggerFeatures = posTaggerFeatures;
		this.beamWidth = beamWidth;
		this.propagateTokeniserBeam = propagateTokeniserBeam;
		this.decisionMaker = decisionMaker;
		this.session = talismaneSession;
		this.observers = new ArrayList<>();
	}

	ForwardStatisticalPosTagger(ForwardStatisticalPosTagger posTagger) {
		this.posTaggerFeatures = new HashSet<>(posTagger.posTaggerFeatures);
		this.beamWidth = posTagger.beamWidth;
		this.propagateTokeniserBeam = posTagger.propagateTokeniserBeam;
		this.decisionMaker = posTagger.decisionMaker;
		this.session = posTagger.session;
		this.observers = posTagger.observers;
		this.posTaggerRules = new ArrayList<>(posTagger.posTaggerRules);
		this.posTaggerPositiveRules = new ArrayList<>(posTagger.posTaggerPositiveRules);
		this.posTaggerNegativeRules = new ArrayList<>(posTagger.posTaggerNegativeRules);
	}

	/**
	 * Get a pos-tagger defined by a particular machine learning model.
	 * 
	 * @param beamWidth
	 *            the maximum beamwidth to consider during the beam search
	 */
	public ForwardStatisticalPosTagger(ClassificationModel model, int beamWidth, boolean propagateTokeniserBeam, TalismaneSession session) {
		PosTaggerFeatureParser featureParser = new PosTaggerFeatureParser(session);
		Collection<ExternalResource<?>> externalResources = model.getExternalResources();
		if (externalResources != null) {
			for (ExternalResource<?> externalResource : externalResources) {
				featureParser.getExternalResourceFinder().addExternalResource(externalResource);
			}
		}

		Set<PosTaggerFeature<?>> posTaggerFeatures = featureParser.getFeatureSet(model.getFeatureDescriptors());
		this.posTaggerFeatures = posTaggerFeatures;
		this.beamWidth = beamWidth;
		this.propagateTokeniserBeam = propagateTokeniserBeam;
		this.decisionMaker = model.getDecisionMaker();
		this.session = session;
		this.observers = new ArrayList<>();
	}

	@Override
	public List<PosTagSequence> tagSentence(List<TokenSequence> input) {
		List<TokenSequence> tokenSequences = null;
		if (this.propagateTokeniserBeam) {
			tokenSequences = input;
		} else {
			tokenSequences = new ArrayList<>(1);
			tokenSequences.add(input.get(0));
		}

		int sentenceLength = tokenSequences.get(0).getSentence().getText().length();

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
					}
				}

				if (!ruleApplied) {
					// test the features on the current token
					List<FeatureResult<?>> featureResults = new ArrayList<FeatureResult<?>>();
					for (PosTaggerFeature<?> posTaggerFeature : posTaggerFeatures) {
						RuntimeEnvironment env = new RuntimeEnvironment();
						FeatureResult<?> featureResult = posTaggerFeature.check(context, env);
						if (featureResult != null)
							featureResults.add(featureResult);
					}
					if (LOG.isTraceEnabled()) {
						for (FeatureResult<?> result : featureResults) {
							LOG.trace(result.toString());
						}
					}

					// evaluate the feature results using the maxent model
					decisions = this.decisionMaker.decide(featureResults);

					for (ClassificationObserver observer : this.observers) {
						observer.onAnalyse(token, featureResults, decisions);
					}

					// apply the negative rules
					Set<String> eliminatedPosTags = new TreeSet<String>();
					if (posTaggerNegativeRules != null) {
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
					}

					// is this a known word in the lexicon?
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
				} // has a rule been applied?

				// add new TaggedTokenSequences to the heap, one for each
				// outcome provided by MaxEnt
				for (Decision decision : decisions) {
					if (LOG.isTraceEnabled())
						LOG.trace("Outcome: " + decision.getOutcome() + ", " + decision.getProbability());

					PosTaggedToken posTaggedToken = new PosTaggedToken(token, decision, this.session);
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
			for (PosTagSequenceFilter filter : session.getPosTagSequenceFilters())
				filter.apply(sequence);

			if (LOG.isDebugEnabled()) {
				LOG.debug("Sequence after filters: " + sequence);
			}
		}

		return sequences;
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

	@Override
	public int getBeamWidth() {
		return beamWidth;
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
	public PosTagger clonePosTagger() {
		return new ForwardStatisticalPosTagger(this);
	}

	@Override
	public boolean isPropagateTokeniserBeam() {
		return propagateTokeniserBeam;
	}

}
