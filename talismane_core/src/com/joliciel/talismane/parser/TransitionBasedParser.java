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
package com.joliciel.talismane.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.machineLearning.AnalysisObserver;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.parser.features.ParseConfigurationFeature;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.utils.PerformanceMonitor;

/**
 * A non-deterministic parser implementing transition based parsing,
 * using a Shift-Reduce algorithm.
 * See Nivre 2008 for details.
 * @author Assaf Urieli
 *
 */
class TransitionBasedParser implements NonDeterministicParser {
	private static final Log LOG = LogFactory.getLog(TransitionBasedParser.class);
	private static final double MIN_PROB_TO_STORE = 0.001;
	private int beamWidth;
	
	private Set<ParseConfigurationFeature<?>> parseFeatures;
	
	private ParserServiceInternal parserServiceInternal;
	private DecisionMaker<Transition> decisionMaker;
	private TransitionSystem transitionSystem;

	private List<AnalysisObserver> observers = new ArrayList<AnalysisObserver>();
	
	public TransitionBasedParser(DecisionMaker<Transition> decisionMaker, TransitionSystem transitionSystem, Set<ParseConfigurationFeature<?>> parseFeatures, int beamWidth) {
		super();
		this.decisionMaker = decisionMaker;
		this.transitionSystem = transitionSystem;
		this.parseFeatures = parseFeatures;
		this.beamWidth = beamWidth;
	}


	@Override
	public ParseConfiguration parseSentence(PosTagSequence posTagSequence) {
		List<PosTagSequence> posTagSequences = new ArrayList<PosTagSequence>();
		posTagSequences.add(posTagSequence);
		List<ParseConfiguration> parseConfigurations = this.parseSentence(posTagSequences);
		ParseConfiguration parseConfiguration = parseConfigurations.get(0);
		return parseConfiguration;
	}
	
	@Override
	public List<ParseConfiguration> parseSentence(List<PosTagSequence> posTagSequences) {
		PerformanceMonitor.startTask("TransitionBasedParser.parseSentence");
		try {
			TokenSequence tokenSequence = posTagSequences.get(0).getTokenSequence();
			int terminalConfigurationIndex = tokenSequence.getAtomicTokenCount() * 1000;
				
			TreeMap<Integer, PriorityQueue<ParseConfiguration>> heaps = new TreeMap<Integer, PriorityQueue<ParseConfiguration>>();
			
			PriorityQueue<ParseConfiguration> heap0 = new PriorityQueue<ParseConfiguration>();
			for (PosTagSequence posTagSequence : posTagSequences) {
				// add an initial ParseConfiguration for each postag sequence
				ParseConfiguration initialConfiguration = this.getParserServiceInternal().getInitialConfiguration(posTagSequence);
				heap0.add(initialConfiguration);
			}
			heaps.put(0, heap0);
			
			PriorityQueue<ParseConfiguration> finalHeap = null;
			while (heaps.size()>0) {
				Entry<Integer, PriorityQueue<ParseConfiguration>> heapEntry = heaps.pollFirstEntry();
				if (LOG.isTraceEnabled()) {
					LOG.trace("##### Polling next heap: " + heapEntry.getKey());
				}
				if (heapEntry.getKey()==terminalConfigurationIndex) {
					finalHeap = heapEntry.getValue();
					break;
				}
				PriorityQueue<ParseConfiguration> previousHeap = heapEntry.getValue();
				
				// limit the breadth to K
				int maxSequences = previousHeap.size() > this.beamWidth ? this.beamWidth : previousHeap.size();
				
				for (int j = 0; j<maxSequences; j++) {
					ParseConfiguration history = previousHeap.poll();
					if (LOG.isTraceEnabled()) {
						LOG.trace("### Next configuration on heap " + heapEntry.getKey() + ":");
						LOG.trace(history.toString());
						LOG.trace("Score: " + history.getScore());						
					}
					
					// test the features on the current token
					List<FeatureResult<?>> parseFeatureResults = new ArrayList<FeatureResult<?>>();
					PerformanceMonitor.startTask("feature analyse");
					try {
						for (ParseConfigurationFeature<?> feature : this.parseFeatures) {
							PerformanceMonitor.startTask(feature.getName());
							try {
								FeatureResult<?> featureResult = feature.check(history);
								if (featureResult!=null)
									parseFeatureResults.add(featureResult);
							} finally {
								PerformanceMonitor.endTask(feature.getName());
							}
						}
						if (LOG.isTraceEnabled()) {
							for (FeatureResult<?> featureResult : parseFeatureResults) {
								LOG.trace(featureResult.toString());
							}
						}
					} finally {
						PerformanceMonitor.endTask("feature analyse");
					}
					
					// evaluate the feature results using the decision maker
					List<Decision<Transition>> decisions = null;
					PerformanceMonitor.startTask("make decision");
					try {
						decisions = this.decisionMaker.decide(parseFeatureResults);
						
						for (AnalysisObserver observer : this.observers) {
							observer.onAnalyse(history, parseFeatureResults, decisions);
						}

						
						List<Decision<Transition>> decisionShortList = new ArrayList<Decision<Transition>>(decisions.size());
						for (Decision<Transition> decision : decisions) {
							if (decision.getProbability() > MIN_PROB_TO_STORE)
								decisionShortList.add(decision);
						}
						decisions = decisionShortList;
					} finally {
						PerformanceMonitor.endTask("make decision");
					}
					
					// add new TaggedTokenSequences to the heap, one for each outcome provided by MaxEnt
					PerformanceMonitor.startTask("heap sort");
					try {
						for (Decision<Transition> decision : decisions) {
							Transition transition = decision.getOutcome();
							if (LOG.isTraceEnabled())
								LOG.trace("Outcome: " + transition.getCode() + ", " + decision.getProbability());
							
							if (transition.checkPreconditions(history)) {
								ParseConfiguration configuration = this.parserServiceInternal.getConfiguration(history);
								transition.apply(configuration);
								if (decision.isStatistical())
									configuration.addDecision(decision);
								
								int heapIndex = configuration.getConfigurationComparisonIndex();
			
								if (LOG.isTraceEnabled()) {
									LOG.trace("Adding result with score " + configuration.getScore() + " to heap: " + heapIndex);
								}

								PriorityQueue<ParseConfiguration> heap = heaps.get(heapIndex);
								if (heap==null) {
									heap = new PriorityQueue<ParseConfiguration>();
									heaps.put(heapIndex, heap);
								}
								heap.add(configuration);
							} else {
								if (LOG.isTraceEnabled())
									LOG.trace("Cannot apply transition: doesn't meet pre-conditions");
							}
						} // next outcome for this token
					} finally {
						PerformanceMonitor.endTask("heap sort");
					}
				} // next history		
			} // next atomic index
			
			// return the best sequences on the heap
			List<ParseConfiguration> sequences = new ArrayList<ParseConfiguration>();
			int i = 0;
			while (!finalHeap.isEmpty()) {
				sequences.add(finalHeap.poll());
				i++;
				if (i>=this.getBeamWidth())
					break;
			}
			if (LOG.isDebugEnabled()) {
				for (ParseConfiguration finalConfiguration : sequences) {
					LOG.debug(finalConfiguration.getScore() + ": " + finalConfiguration.toString());
					if (LOG.isTraceEnabled()) {
						StringBuilder sb = new StringBuilder();
						for (Decision<Transition> decision : finalConfiguration.getDecisions()) {
							sb.append(" * ");
							sb.append(decision.getProbability());
						}
						sb.append(" root ");
						sb.append(finalConfiguration.getTransitions().size());
						LOG.trace(sb.toString());
						
						sb = new StringBuilder();
						sb.append(" * PosTag sequence score ");
						sb.append(finalConfiguration.getPosTagSequence().getScore());
						sb.append(" = ");
						for (PosTaggedToken posTaggedToken : finalConfiguration.getPosTagSequence()) {
							sb.append(" * ");
							sb.append(posTaggedToken.getDecision().getProbability());
						}
						sb.append(" root ");
						sb.append(finalConfiguration.getPosTagSequence().size());
						LOG.trace(sb.toString());
						
						sb = new StringBuilder();
						sb.append(" * Token sequence score = ");
						sb.append(finalConfiguration.getPosTagSequence().getTokenSequence().getScore());
						LOG.trace(sb.toString());
						
					}
				}
			}
			return sequences;
		} finally {
			PerformanceMonitor.endTask("TransitionBasedParser.parseSentence");
		}
	}

	@Override
	public int getBeamWidth() {
		return beamWidth;
	}

	public ParserServiceInternal getParserServiceInternal() {
		return parserServiceInternal;
	}

	public void setParserServiceInternal(ParserServiceInternal parserServiceInternal) {
		this.parserServiceInternal = parserServiceInternal;
	}

	@Override
	public void addObserver(AnalysisObserver observer) {
		this.observers.add(observer);
	}

	public TransitionSystem getTransitionSystem() {
		return transitionSystem;
	}

	public void setTransitionSystem(TransitionSystem transitionSystem) {
		this.transitionSystem = transitionSystem;
	}
	
}
