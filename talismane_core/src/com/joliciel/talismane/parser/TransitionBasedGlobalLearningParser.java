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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.machineLearning.ClassificationObserver;
import com.joliciel.talismane.machineLearning.FeatureWeightVector;
import com.joliciel.talismane.machineLearning.Ranker;
import com.joliciel.talismane.machineLearning.RankingSolution;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.FeatureService;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.parser.features.ParseConfigurationFeature;
import com.joliciel.talismane.parser.features.ParserRule;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.utils.PerformanceMonitor;

/**
 * A non-deterministic parser implementing transition based parsing,
 * using a Shift-Reduce algorithm, but applying global learning.</br>
 * The features are thus used to rank parse configurations
 * after all valid transitions have been applied, rather than being used
 * to select the next transition for an existing configuration.
 * @author Assaf Urieli
 *
 */
class TransitionBasedGlobalLearningParser implements NonDeterministicParser, Ranker<PosTagSequence> {
	private static final Log LOG = LogFactory.getLog(TransitionBasedGlobalLearningParser.class);
	private static final PerformanceMonitor MONITOR = PerformanceMonitor.getMonitor(TransitionBasedGlobalLearningParser.class);
	private static final DecimalFormat df = new DecimalFormat("0.0000");
	private int beamWidth;
	
	private Set<ParseConfigurationFeature<?>> parseFeatures;
	
	private ParserServiceInternal parserServiceInternal;
	private FeatureService featureService;
	private FeatureWeightVector featureWeightVector;
	private ParsingConstrainer parsingConstrainer;
	private ParseComparisonStrategy parseComparisonStrategy = new TransitionCountComparisonStrategy();

	private List<ClassificationObserver<Transition>> observers = new ArrayList<ClassificationObserver<Transition>>();
	private int maxAnalysisTimePerSentence = 60;
	private int minFreeMemory = 64;
	private static final int KILOBYTE = 1024;
	
	private List<ParserRule> parserRules;
	private List<ParserRule> parserPositiveRules;
	private List<ParserRule> parserNegativeRules;
	
	public TransitionBasedGlobalLearningParser(ParsingConstrainer parsingConstrainer, Set<ParseConfigurationFeature<?>> parseFeatures, int beamWidth) {
		this(null, parsingConstrainer, parseFeatures, beamWidth);
	}
	
	public TransitionBasedGlobalLearningParser(FeatureWeightVector featureWeightVector, ParsingConstrainer parsingConstrainer, Set<ParseConfigurationFeature<?>> parseFeatures, int beamWidth) {
		super();
		this.featureWeightVector = featureWeightVector;
		this.parsingConstrainer = parsingConstrainer;
		this.parseFeatures = parseFeatures;
		this.beamWidth = beamWidth;
	}

	@Override
	public List<RankingSolution> rank(PosTagSequence posTagSequence,
			FeatureWeightVector weightVector, RankingSolution correctSolution) {
		List<PosTagSequence> posTagSequences = new ArrayList<PosTagSequence>();
		posTagSequences.add(posTagSequence);
		List<ParseConfiguration> parseConfigurations = this.parseSentence(posTagSequences, weightVector, correctSolution);
		List<RankingSolution> rankingSolutions = new ArrayList<RankingSolution>(parseConfigurations);
		return rankingSolutions;
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
		return this.parseSentence(posTagSequences, this.getFeatureWeightVector(), null);
	}
	
	public List<ParseConfiguration> parseSentence(List<PosTagSequence> posTagSequences, FeatureWeightVector weightVector, RankingSolution correctSolution) {
		MONITOR.startTask("parseSentence");
		try {
			long startTime = (new Date()).getTime();
			int maxAnalysisTimeMilliseconds = maxAnalysisTimePerSentence * 1000;
			int minFreeMemoryBytes = minFreeMemory * KILOBYTE;
			
			TokenSequence tokenSequence = posTagSequences.get(0).getTokenSequence();
				
			TreeMap<Integer, TreeSet<ParseConfiguration>> heaps = new TreeMap<Integer, TreeSet<ParseConfiguration>>();
			
			TreeSet<ParseConfiguration> heap0 = new TreeSet<ParseConfiguration>();
			for (PosTagSequence posTagSequence : posTagSequences) {
				// add an initial ParseConfiguration for each postag sequence
				ParseConfiguration initialConfiguration = this.getParserServiceInternal().getInitialConfiguration(posTagSequence);
				initialConfiguration.setScore(0.0);
				heap0.add(initialConfiguration);
				if (LOG.isDebugEnabled()) {
					LOG.debug("Adding initial posTagSequence: " + posTagSequence);
				}
			}
			heaps.put(0, heap0);
			TreeSet<ParseConfiguration> backupHeap = null;
			
			TreeSet<ParseConfiguration> finalHeap = null;
			while (heaps.size()>0) {
				Entry<Integer, TreeSet<ParseConfiguration>> heapEntry = heaps.firstEntry();
				TreeSet<ParseConfiguration> currentHeap = heapEntry.getValue();
				int currentHeapIndex = heapEntry.getKey();
				if (LOG.isTraceEnabled()) {
					LOG.trace("##### Polling next heap: " + heapEntry.getKey() + ", size: " + heapEntry.getValue().size());
				}
				
				boolean finished = false;
				// systematically set the final heap here, just in case we exit "naturally" with no more heaps
				finalHeap = heapEntry.getValue();
				backupHeap = new TreeSet<ParseConfiguration>();
				
				// we jump out when either (a) all tokens have been attached or (b) we go over the max alloted time
				ParseConfiguration topConf = currentHeap.first();
				if (topConf.isTerminal()) {
					LOG.trace("Exiting with terminal heap: " + heapEntry.getKey() + ", size: " + heapEntry.getValue().size());
					finished = true;
				}
				
				// check if we've gone over alloted time for this sentence
				long analysisTime = (new Date()).getTime() - startTime;
				if (maxAnalysisTimePerSentence > 0 && analysisTime > maxAnalysisTimeMilliseconds) {
					LOG.info("Parse tree analysis took too long for sentence: " + tokenSequence.getText());
					LOG.info("Breaking out after " +  maxAnalysisTimePerSentence + " seconds.");
					finished = true;
				}
				
				// check if we've enough memory to process this sentence
				if (minFreeMemory > 0) {
					long freeMemory = Runtime.getRuntime().freeMemory();
					if (freeMemory < minFreeMemoryBytes) {
						LOG.info("Not enough memory left to parse sentence: " + tokenSequence.getText());
						LOG.info("Min free memory (bytes):" +  minFreeMemoryBytes);
						LOG.info("Current free memory (bytes): " +  freeMemory);
						finished = true;
					}
				}
				
				// check if any of the remaining top-N solutions on any heap can lead to the correct solution
				if (correctSolution!=null) {
					boolean canReachCorrectSolution = false;
					for (TreeSet<ParseConfiguration> heap : heaps.values()) {
						int j=1;
						for (ParseConfiguration solution : heap) {
							if (j>beamWidth)
								break;
							if (solution.canReach(correctSolution)) {
								canReachCorrectSolution = true;
								break;
							}
							j++;
						}
						if (canReachCorrectSolution)
							break;
					}
					if (!canReachCorrectSolution) {
						LOG.debug("None of the solutions on the heap can reach the gold solution. Exiting.");
						finished = true;
					}
				}
				
				if (finished) {
					// combine any remaining heaps
					for (TreeSet<ParseConfiguration> heap : heaps.values()) {
						if (finalHeap!=heap) {
							finalHeap.addAll(heap);
						}
					}			
					break;
				}
				
				// remove heap from set of heaps
				heapEntry = heaps.pollFirstEntry();
				
				// limit the breadth to K
				int maxSolutions = currentHeap.size() > this.beamWidth ? this.beamWidth : currentHeap.size();
				
				int j=0;
				while (currentHeap.size()>0) {
					ParseConfiguration history = currentHeap.pollFirst();
					backupHeap.add(history);
					if (LOG.isTraceEnabled()) {
						LOG.trace("### Next configuration on heap " + heapEntry.getKey() + ":");
						LOG.trace(history.toString());
						LOG.trace("Score: " + df.format(history.getScore()));
						LOG.trace(history.getPosTagSequence());
					}
					
					Set<Transition> transitions = new HashSet<Transition>();
					
					// test the positive rules on the current configuration
					boolean ruleApplied = false;
					if (parserPositiveRules!=null) {
						MONITOR.startTask("check rules");
						try {
							for (ParserRule rule : parserPositiveRules) {
								if (LOG.isTraceEnabled()) {
									LOG.trace("Checking rule: " + rule.getCondition().getName());
								}
								RuntimeEnvironment env = this.featureService.getRuntimeEnvironment();
								FeatureResult<Boolean> ruleResult = rule.getCondition().check(history, env);
								if (ruleResult!=null && ruleResult.getOutcome()) {
									transitions.add(rule.getTransition());
									ruleApplied = true;
									if (LOG.isTraceEnabled()) {
										LOG.trace("Rule applies. Setting transition to: " + rule.getTransition().getCode());
									}
									
									if (!rule.getTransition().checkPreconditions(history)) {
										LOG.error("Cannot apply rule, preconditions not met.");
										ruleApplied = false;
									}
									break;
								}
							}
						} finally {
							MONITOR.endTask("check rules");
						}
					}
					
					if (!ruleApplied) {
						transitions = parsingConstrainer.getPossibleTransitions(history);
						
						Set<Transition> eliminatedTransitions = new HashSet<Transition>();
						for (Transition transition : transitions) {
							if (!transition.checkPreconditions(history)) {
								eliminatedTransitions.add(transition);
							}
						}
						transitions.removeAll(eliminatedTransitions);
						
						// apply the negative rules
						eliminatedTransitions = new HashSet<Transition>();
						if (parserNegativeRules!=null) {
							MONITOR.startTask("check negative rules");
							try {
								for (ParserRule rule : parserNegativeRules) {
									if (LOG.isTraceEnabled()) {
										LOG.trace("Checking negative rule: " + rule.getCondition().getName());
									}
									RuntimeEnvironment env = this.featureService.getRuntimeEnvironment();
									FeatureResult<Boolean> ruleResult = rule.getCondition().check(history, env);
									if (ruleResult!=null && ruleResult.getOutcome()) {
										eliminatedTransitions.add(rule.getTransition());
										if (LOG.isTraceEnabled()) {
											LOG.debug("Rule applies. Eliminating transition: " + rule.getTransition().getCode());
										}
									}
								}
								
								if (eliminatedTransitions.size()==transitions.size()) {
									LOG.debug("All transitions eliminated! Restoring original transitions.");
								} else {
									transitions.removeAll(eliminatedTransitions);
								}
							} finally {
								MONITOR.endTask("check negative rules");
							}
						}
					} // has a positive rule been applied?
					
					if (transitions.size()==0) {
						// just in case the we run out of both heaps and analyses, we build this backup heap
						backupHeap.add(history);
						if (LOG.isTraceEnabled())
							LOG.trace("No transitions could be applied: not counting this solution as part of the beam");
					} else {
						// up the counter, since we will count this solution towards the heap
						j++;
						// add solutions to the heap, one per valid transition
						MONITOR.startTask("heap sort");
						try {
							for (Transition transition : transitions) {
								if (LOG.isTraceEnabled()) {
									LOG.trace("Applying transition: " + transition.getCode());
								}
								ParseConfiguration configuration = this.parserServiceInternal.getConfiguration(history);
								transition.apply(configuration);
								configuration.setScore(history.getScore());
								configuration.getIncrementalFeatureResults().addAll(history.getIncrementalFeatureResults());
								
								// test the features on the new configuration
								double scoreDelta = 0.0;
								MONITOR.startTask("feature analyse");
								List<FeatureResult<?>> featureResults = new ArrayList<FeatureResult<?>>();
								try {
									for (ParseConfigurationFeature<?> feature : this.parseFeatures) {
										MONITOR.startTask(feature.getName());
										try {
											RuntimeEnvironment env = this.featureService.getRuntimeEnvironment();
											FeatureResult<?> featureResult = feature.check(configuration, env);
											if (featureResult!=null) {
												featureResults.add(featureResult);
												double weight = weightVector.getWeight(featureResult);
												scoreDelta += weight;
												if (LOG.isTraceEnabled()) {
													LOG.trace(featureResult.toString() + " = " + weight);
												}
											}
										} finally {
											MONITOR.endTask(feature.getName());
										}
									}
									configuration.getIncrementalFeatureResults().add(featureResults);
									if (LOG.isTraceEnabled()) {
										LOG.trace("Score = " + configuration.getScore() + " + " + scoreDelta + " = " + (configuration.getScore() + scoreDelta));
									}
									configuration.setScore(configuration.getScore() + scoreDelta);
									

								} finally {
									MONITOR.endTask("feature analyse");
								}
								
								int nextHeapIndex = parseComparisonStrategy.getComparisonIndex(configuration) * 1000;
								while (nextHeapIndex<=currentHeapIndex)
									nextHeapIndex++;
								
								TreeSet<ParseConfiguration> nextHeap = heaps.get(nextHeapIndex);
								if (nextHeap==null) {
									nextHeap = new TreeSet<ParseConfiguration>();
									heaps.put(nextHeapIndex, nextHeap);
									if (LOG.isTraceEnabled())
										LOG.trace("Created heap with index: " + nextHeapIndex);
								}
								nextHeap.add(configuration);
								if (LOG.isTraceEnabled()) {
									LOG.trace("Added configuration with score " + configuration.getScore() + " to heap: " + nextHeapIndex + ", total size: " + nextHeap.size());
								}
								
								configuration.clearMemory();
							} // next transition
						} finally {
							MONITOR.endTask("heap sort");
						}
					} // have we any transitions?
					
					// beam width test
					if (j==maxSolutions)
						break;
				} // next history	
			} // next atomic index
			
			// return the best sequences on the heap
			List<ParseConfiguration> bestConfigurations = new ArrayList<ParseConfiguration>();
			int i = 0;
			
			if (finalHeap.isEmpty())
				finalHeap = backupHeap;
			
			while (!finalHeap.isEmpty()) {
				bestConfigurations.add(finalHeap.pollFirst());
				i++;
				if (i>=this.getBeamWidth())
					break;
			}
			if (LOG.isDebugEnabled()) {
				if (correctSolution!=null) {
					LOG.debug("Gold transitions: " + correctSolution.getIncrementalOutcomes());
				}
				for (ParseConfiguration finalConfiguration : bestConfigurations) {
					LOG.debug(df.format(finalConfiguration.getScore()) + ": " + finalConfiguration.toString());
					LOG.debug("Pos tag sequence: " + finalConfiguration.getPosTagSequence());
					LOG.debug("Transitions: " + finalConfiguration.getTransitions());
					if (LOG.isTraceEnabled()) {
						StringBuilder sb = new StringBuilder();
						sb.append(" * PosTag sequence score ");
						sb.append(df.format(finalConfiguration.getPosTagSequence().getScore()));
						sb.append(" = ");
						for (PosTaggedToken posTaggedToken : finalConfiguration.getPosTagSequence()) {
							sb.append(" * ");
							sb.append(df.format(posTaggedToken.getDecision().getProbability()));
						}
						sb.append(" root ");
						sb.append(finalConfiguration.getPosTagSequence().size());
						LOG.trace(sb.toString());
						
						sb = new StringBuilder();
						sb.append(" * Token sequence score = ");
						sb.append(df.format(finalConfiguration.getPosTagSequence().getTokenSequence().getScore()));
						LOG.trace(sb.toString());
						
					}
				}
			}
			return bestConfigurations;
		} finally {
			MONITOR.endTask("parseSentence");
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
	public void addObserver(ClassificationObserver<Transition> observer) {
		this.observers.add(observer);
	}

	public int getMaxAnalysisTimePerSentence() {
		return maxAnalysisTimePerSentence;
	}


	public void setMaxAnalysisTimePerSentence(int maxAnalysisTimePerSentence) {
		this.maxAnalysisTimePerSentence = maxAnalysisTimePerSentence;
	}
	
	
	public int getMinFreeMemory() {
		return minFreeMemory;
	}


	public void setMinFreeMemory(int minFreeMemory) {
		this.minFreeMemory = minFreeMemory;
	}


	@Override
	public void setParserRules(List<ParserRule> parserRules) {
		this.parserRules = parserRules;
		this.parserPositiveRules = new ArrayList<ParserRule>();
		this.parserNegativeRules = new ArrayList<ParserRule>();
		for (ParserRule rule : parserRules) {
			if (rule.isNegative())
				parserNegativeRules.add(rule);
			else
				parserPositiveRules.add(rule);
		}
	}

	public List<ParserRule> getParserRules() {
		return parserRules;
	}

	public FeatureService getFeatureService() {
		return featureService;
	}

	public void setFeatureService(FeatureService featureService) {
		this.featureService = featureService;
	}

	public ParsingConstrainer getParsingConstrainer() {
		return parsingConstrainer;
	}

	public void setParsingConstrainer(ParsingConstrainer parsingConstrainer) {
		this.parsingConstrainer = parsingConstrainer;
	}


	@Override
	public TransitionSystem getTransitionSystem() {
		return this.parsingConstrainer.getTransitionSystem();
	}

	public FeatureWeightVector getFeatureWeightVector() {
		return featureWeightVector;
	}
	
	
}
