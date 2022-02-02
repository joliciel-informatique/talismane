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
package com.joliciel.talismane.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;

import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.ClassificationObserver;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.ExternalResource;
import com.joliciel.talismane.machineLearning.MachineLearningModelFactory;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.parser.features.ParseConfigurationFeature;
import com.joliciel.talismane.parser.features.ParserFeatureParser;
import com.joliciel.talismane.parser.features.ParserRule;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.utils.ArrayListNoNulls;
import com.joliciel.talismane.utils.ConfigUtils;
import com.typesafe.config.Config;

/**
 * A non-deterministic parser implementing transition based parsing, using a
 * Shift-Reduce algorithm.<br>
 * See Nivre 2008 for details on the algorithm for the deterministic case.
 * 
 * @author Assaf Urieli
 *
 */
public class TransitionBasedParser implements NonDeterministicParser {
  private static final Logger LOG = LoggerFactory.getLogger(TransitionBasedParser.class);
  private static final Map<String, ClassificationModel> modelMap = new HashMap<>();

  private static final Logger LOG_FEATURES = LoggerFactory.getLogger(TransitionBasedParser.class.getName() + ".features");
  private static final double MIN_PROB_TO_STORE = 0.0001;
  private static final DecimalFormat df = new DecimalFormat("0.0000");
  private boolean earlyStop = false;

  private final DecisionMaker decisionMaker;
  private final TransitionSystem transitionSystem;
  private final Set<ParseConfigurationFeature<?>> parseFeatures;
  private final int beamWidth;
  private final boolean propagatePosTaggerBeam;
  private final String sessionId;

  private final ParseComparisonStrategy parseComparisonStrategy;

  private final List<ClassificationObserver> observers;
  private final int maxAnalysisTimePerSentence;
  private final int minFreeMemory;
  private static final int KILOBYTE = 1024;

  private List<ParserRule> parserRules;
  private List<ParserRule> parserPositiveRules;
  private List<ParserRule> parserNegativeRules;

  public TransitionBasedParser(String sessionId) throws IOException, ClassNotFoundException, TalismaneException {
    Config config = ConfigFactory.load();
    Config parserConfig = config.getConfig("talismane.core." + sessionId + ".parser");

    String configPath = "talismane.core." + sessionId + ".parser.model";
    String modelFilePath = config.getString(configPath);
    LOG.debug("Getting parser model from " + modelFilePath);
    ClassificationModel model = modelMap.get(modelFilePath);
    if (model == null) {
      InputStream tokeniserModelFile = ConfigUtils.getFileFromConfig(config, configPath);
      MachineLearningModelFactory factory = new MachineLearningModelFactory();
      model = factory.getClassificationModel(new ZipInputStream(tokeniserModelFile));
      modelMap.put(modelFilePath, model);
    }

    int beamWidth = parserConfig.getInt("beam-width");
    boolean propagatePosTaggerBeam = parserConfig.getBoolean("propagate-pos-tagger-beam");

    ParseComparisonStrategyType parseComparisonStrategyType = ParseComparisonStrategyType.valueOf(parserConfig.getString("comparison-strategy"));
    ParseComparisonStrategy parseComparisonStrategy = ParseComparisonStrategy.forType(parseComparisonStrategyType);

    int maxAnalysisTimePerSentence = parserConfig.getInt("max-analysis-time");
    int minFreeMemory = parserConfig.getInt("min-free-memory");

    this.sessionId = sessionId;
    this.beamWidth = beamWidth;
    this.propagatePosTaggerBeam = propagatePosTaggerBeam;
    this.parseComparisonStrategy = parseComparisonStrategy;
    this.maxAnalysisTimePerSentence = maxAnalysisTimePerSentence;
    this.minFreeMemory = minFreeMemory;
    this.transitionSystem = TransitionSystem.getTransitionSystem(model);
    this.decisionMaker = model.getDecisionMaker();

    ParserFeatureParser parserFeatureParser = new ParserFeatureParser(sessionId);
    Collection<ExternalResource<?>> externalResources = model.getExternalResources();
    if (externalResources != null) {
      for (ExternalResource<?> externalResource : externalResources) {
        parserFeatureParser.getExternalResourceFinder().addExternalResource(externalResource);
      }
    }

    this.parseFeatures = parserFeatureParser.getFeatures(model.getFeatureDescriptors());
    this.observers = new ArrayList<>();

    this.setEarlyStop(parserConfig.getBoolean("early-stop"));

    boolean includeDetails = parserConfig.getBoolean("output.include-details");
    if (includeDetails) {
      String detailsFilePath = TalismaneSession.get(sessionId).getBaseName() + "_posTagger_details.txt";
      File detailsFile = new File(detailsFilePath);
      detailsFile.delete();
      ClassificationObserver observer = model.getDetailedAnalysisObserver(detailsFile);
      this.addObserver(observer);
    }

    List<ParserRule> parserRules = new ArrayList<>();
    ParserFeatureParser featureParser = new ParserFeatureParser(sessionId);

    configPath = "talismane.core." + sessionId + ".parser.rules";
    List<String> textFilterPaths = config.getStringList(configPath);
    for (String path : textFilterPaths) {
      LOG.debug("From: " + path);
      InputStream textFilterFile = ConfigUtils.getFile(config, configPath, path);
      try (Scanner scanner = new Scanner(textFilterFile, TalismaneSession.get(sessionId).getInputCharset().name())) {
        List<String> ruleDescriptors = new ArrayListNoNulls<>();
        while (scanner.hasNextLine()) {
          String ruleDescriptor = scanner.nextLine();
          if (ruleDescriptor.length() > 0) {
            ruleDescriptors.add(ruleDescriptor);
            LOG.trace(ruleDescriptor);
          }
        }
        List<ParserRule> rules = featureParser.getRules(ruleDescriptors);
        parserRules.addAll(rules);
      }
    }
    this.setParserRules(parserRules);
  }

  TransitionBasedParser(DecisionMaker decisionMaker, TransitionSystem transitionSystem, Set<ParseConfigurationFeature<?>> parseFeatures, int beamWidth,
      boolean propagatePosTaggerBeam, ParseComparisonStrategy parseComparisonStrategy, int maxAnalysisTimePerSentence, int minFreeMemory,
      String sessionId) {
    this.decisionMaker = decisionMaker;
    this.transitionSystem = transitionSystem;
    this.parseFeatures = parseFeatures;
    this.beamWidth = beamWidth;
    this.propagatePosTaggerBeam = propagatePosTaggerBeam;
    this.parseComparisonStrategy = parseComparisonStrategy;
    this.maxAnalysisTimePerSentence = maxAnalysisTimePerSentence;
    this.minFreeMemory = minFreeMemory;
    this.sessionId = sessionId;
    this.observers = new ArrayList<>();
  }

  TransitionBasedParser(TransitionBasedParser parser) {
    this.decisionMaker = parser.decisionMaker;
    this.transitionSystem = parser.transitionSystem;
    this.parseFeatures = new HashSet<>(parser.parseFeatures);
    this.beamWidth = parser.beamWidth;
    this.propagatePosTaggerBeam = parser.propagatePosTaggerBeam;
    this.sessionId = parser.sessionId;
    this.observers = new ArrayList<>(parser.observers);
    this.parserRules = new ArrayList<>(parser.parserRules);
    this.parserPositiveRules = new ArrayList<>(parser.parserPositiveRules);
    this.parserNegativeRules = new ArrayList<>(parser.parserNegativeRules);

    this.parseComparisonStrategy = parser.parseComparisonStrategy;

    this.maxAnalysisTimePerSentence = parser.maxAnalysisTimePerSentence;
    this.minFreeMemory = parser.minFreeMemory;
  }

  @Override
  public ParseConfiguration parseSentence(PosTagSequence posTagSequence) throws TalismaneException, IOException {
    List<PosTagSequence> posTagSequences = new ArrayList<>();
    posTagSequences.add(posTagSequence);
    List<ParseConfiguration> parseConfigurations = this.parseSentence(posTagSequences);
    ParseConfiguration parseConfiguration = parseConfigurations.get(0);
    return parseConfiguration;
  }

  @Override
  public List<ParseConfiguration> parseSentence(List<PosTagSequence> input) throws TalismaneException, IOException {
    List<PosTagSequence> posTagSequences = null;
    if (this.propagatePosTaggerBeam) {
      posTagSequences = input;
    } else {
      posTagSequences = new ArrayList<>(1);
      posTagSequences.add(input.get(0));
    }

    long startTime = System.currentTimeMillis();
    int maxAnalysisTimeMilliseconds = maxAnalysisTimePerSentence * 1000;
    int minFreeMemoryBytes = minFreeMemory * KILOBYTE;

    TokenSequence tokenSequence = posTagSequences.get(0).getTokenSequence();

    TreeMap<Integer, PriorityQueue<ParseConfiguration>> heaps = new TreeMap<>();

    PriorityQueue<ParseConfiguration> heap0 = new PriorityQueue<>();
    for (PosTagSequence posTagSequence : posTagSequences) {
      // add an initial ParseConfiguration for each postag sequence
      ParseConfiguration initialConfiguration = new ParseConfiguration(posTagSequence);
      initialConfiguration.setScoringStrategy(decisionMaker.getDefaultScoringStrategy());
      heap0.add(initialConfiguration);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Adding initial posTagSequence: " + posTagSequence);
      }
    }
    heaps.put(0, heap0);
    PriorityQueue<ParseConfiguration> backupHeap = null;

    PriorityQueue<ParseConfiguration> finalHeap = null;
    PriorityQueue<ParseConfiguration> terminalHeap = new PriorityQueue<>();
    while (heaps.size() > 0) {
      Entry<Integer, PriorityQueue<ParseConfiguration>> heapEntry = heaps.pollFirstEntry();
      PriorityQueue<ParseConfiguration> currentHeap = heapEntry.getValue();
      int currentHeapIndex = heapEntry.getKey();
      if (LOG.isTraceEnabled()) {
        LOG.trace("##### Polling next heap: " + heapEntry.getKey() + ", size: " + heapEntry.getValue().size());
      }

      boolean finished = false;
      // systematically set the final heap here, just in case we exit
      // "naturally" with no more heaps
      finalHeap = heapEntry.getValue();
      backupHeap = new PriorityQueue<>();

      // we jump out when either (a) all tokens have been attached or
      // (b) we go over the max alloted time
      ParseConfiguration topConf = currentHeap.peek();
      if (topConf.isTerminal()) {
        LOG.trace("Exiting with terminal heap: " + heapEntry.getKey() + ", size: " + heapEntry.getValue().size());
        finished = true;
      }

      if (earlyStop && terminalHeap.size() >= beamWidth) {
        LOG.debug("Early stop activated and terminal heap contains " + beamWidth + " entries. Exiting.");
        finalHeap = terminalHeap;
        finished = true;
      }

      long analysisTime = System.currentTimeMillis() - startTime;
      if (maxAnalysisTimePerSentence > 0 && analysisTime > maxAnalysisTimeMilliseconds) {
        LOG.info("Parse tree analysis took too long for sentence: " + tokenSequence.getSentence().getText());
        LOG.info("Breaking out after " + maxAnalysisTimePerSentence + " seconds.");
        finished = true;
      }

      if (minFreeMemory > 0) {
        long freeMemory = Runtime.getRuntime().freeMemory();
        if (freeMemory < minFreeMemoryBytes) {
          LOG.info("Not enough memory left to parse sentence: " + tokenSequence.getSentence().getText());
          LOG.info("Min free memory (bytes):" + minFreeMemoryBytes);
          LOG.info("Current free memory (bytes): " + freeMemory);
          finished = true;
        }
      }

      if (finished) {
        break;
      }

      // limit the breadth to K
      int maxSequences = currentHeap.size() > this.beamWidth ? this.beamWidth : currentHeap.size();

      int j = 0;
      while (currentHeap.size() > 0) {
        ParseConfiguration history = currentHeap.poll();
        if (LOG.isTraceEnabled()) {
          LOG.trace("### Next configuration on heap " + heapEntry.getKey() + ":");
          LOG.trace(history.toString());
          LOG.trace("Score: " + df.format(history.getScore()));
          LOG.trace(history.getPosTagSequence().toString());
        }

        List<Decision> decisions = new ArrayList<>();

        // test the positive rules on the current configuration
        boolean ruleApplied = false;
        if (parserPositiveRules != null) {
          for (ParserRule rule : parserPositiveRules) {
            if (LOG.isTraceEnabled()) {
              LOG.trace("Checking rule: " + rule.toString());
            }
            RuntimeEnvironment env = new RuntimeEnvironment();
            FeatureResult<Boolean> ruleResult = rule.getCondition().check(history, env);
            if (ruleResult != null && ruleResult.getOutcome()) {
              Decision positiveRuleDecision = new Decision(rule.getTransition().getCode());
              decisions.add(positiveRuleDecision);
              positiveRuleDecision.addAuthority(rule.getCondition().getName());
              ruleApplied = true;
              if (LOG.isTraceEnabled()) {
                LOG.trace("Rule applies. Setting transition to: " + rule.getTransition().getCode());
              }
              break;
            }
          }
        }

        if (!ruleApplied) {
          // test the features on the current configuration
          List<FeatureResult<?>> parseFeatureResults = new ArrayList<>();
          for (ParseConfigurationFeature<?> feature : this.parseFeatures) {
            RuntimeEnvironment env = new RuntimeEnvironment();
            FeatureResult<?> featureResult = feature.check(history, env);
            if (featureResult != null)
              parseFeatureResults.add(featureResult);

          }
          if (LOG_FEATURES.isTraceEnabled()) {
            SortedSet<String> featureResultSet = parseFeatureResults.stream().map(f -> f.toString())
                .collect(Collectors.toCollection(() -> new TreeSet<>()));
            for (String featureResultString : featureResultSet) {
              LOG_FEATURES.trace(featureResultString);
            }
          }

          // evaluate the feature results using the decision maker
          decisions = this.decisionMaker.decide(parseFeatureResults);

          for (ClassificationObserver observer : this.observers) {
            observer.onAnalyse(history, parseFeatureResults, decisions);
          }

          List<Decision> decisionShortList = new ArrayList<>(decisions.size());
          for (Decision decision : decisions) {
            if (decision.getProbability() > MIN_PROB_TO_STORE)
              decisionShortList.add(decision);
          }
          decisions = decisionShortList;

          // apply the negative rules
          Set<String> eliminatedTransitions = new HashSet<>();
          if (parserNegativeRules != null) {
            for (ParserRule rule : parserNegativeRules) {
              if (LOG.isTraceEnabled()) {
                LOG.trace("Checking negative rule: " + rule.toString());
              }
              RuntimeEnvironment env = new RuntimeEnvironment();
              FeatureResult<Boolean> ruleResult = rule.getCondition().check(history, env);
              if (ruleResult != null && ruleResult.getOutcome()) {
                for (Transition transition : rule.getTransitions()) {
                  eliminatedTransitions.add(transition.getCode());
                  if (LOG.isTraceEnabled())
                    LOG.trace("Rule applies. Eliminating transition: " + transition.getCode());
                }
              }
            }

            if (eliminatedTransitions.size() > 0) {
              decisionShortList = new ArrayList<>();
              for (Decision decision : decisions) {
                if (!eliminatedTransitions.contains(decision.getOutcome())) {
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
        } // has a positive rule been applied?

        boolean transitionApplied = false;
        TransitionSystem transitionSystem = TalismaneSession.get(sessionId).getTransitionSystem();

        // add new configuration to the heap, one for each valid
        // transition

        // Why apply all decisions here? Why not just the top N
        // (where N = beamwidth)?
        // Answer: because we're not always adding solutions to
        // the same heap
        // And yet: a decision here can only do one of two
        // things: process a token (heap+1000), or add a
        // non-processing transition (heap+1)
        // So, if we've already applied N decisions of each
        // type, we should be able to stop
        for (Decision decision : decisions) {
          Transition transition = transitionSystem.getTransitionForCode(decision.getOutcome());
          if (LOG.isTraceEnabled())
            LOG.trace("Outcome: " + transition.getCode() + ", " + decision.getProbability());

          if (transition.checkPreconditions(history)) {
            transitionApplied = true;
            ParseConfiguration configuration = new ParseConfiguration(history);
            if (decision.isStatistical())
              configuration.addDecision(decision);
            transition.apply(configuration);

            int nextHeapIndex = parseComparisonStrategy.getComparisonIndex(configuration) * 1000;
            if (configuration.isTerminal()) {
              nextHeapIndex = Integer.MAX_VALUE;
            } else {
              while (nextHeapIndex <= currentHeapIndex)
                nextHeapIndex++;
            }

            PriorityQueue<ParseConfiguration> nextHeap = heaps.get(nextHeapIndex);
            if (nextHeap == null) {
              if (configuration.isTerminal())
                nextHeap = terminalHeap;
              else
                nextHeap = new PriorityQueue<>();
              heaps.put(nextHeapIndex, nextHeap);
              if (LOG.isTraceEnabled())
                LOG.trace("Created heap with index: " + nextHeapIndex);
            }
            nextHeap.add(configuration);
            if (LOG.isTraceEnabled()) {
              LOG.trace("Added configuration with score " + configuration.getScore() + " to heap: " + nextHeapIndex + ", total size: " + nextHeap.size());
            }

            configuration.clearMemory();
          } else {
            if (LOG.isTraceEnabled())
              LOG.trace("Cannot apply transition: doesn't meet pre-conditions");
            // just in case the we run out of both heaps and
            // analyses, we build this backup heap
            backupHeap.add(history);
          } // does transition meet pre-conditions?
        } // next transition

        if (transitionApplied) {
          j++;
        } else {
          LOG.trace("No transitions could be applied: not counting this history as part of the beam");
        }

        // beam width test
        if (j == maxSequences)
          break;
      } // next history
    } // next atomic index

    // return the best sequences on the heap
    List<ParseConfiguration> bestConfigurations = new ArrayList<>();
    int i = 0;

    if (finalHeap.isEmpty())
      finalHeap = backupHeap;

    while (!finalHeap.isEmpty()) {
      bestConfigurations.add(finalHeap.poll());
      i++;
      if (i >= this.getBeamWidth())
        break;
    }
    if (LOG.isDebugEnabled()) {
      for (ParseConfiguration finalConfiguration : bestConfigurations) {
        LOG.debug(df.format(finalConfiguration.getScore()) + ": " + finalConfiguration.toString());
        LOG.debug("Pos tag sequence: " + finalConfiguration.getPosTagSequence());
        LOG.debug("Transitions: " + finalConfiguration.getTransitions());
        LOG.debug("Decisions: " + finalConfiguration.getDecisions());
        if (LOG.isTraceEnabled()) {
          StringBuilder sb = new StringBuilder();
          for (Decision decision : finalConfiguration.getDecisions()) {
            sb.append(" * ");
            sb.append(df.format(decision.getProbability()));
          }
          sb.append(" root ");
          sb.append(finalConfiguration.getTransitions().size());
          LOG.trace(sb.toString());

          sb = new StringBuilder();
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
  public TransitionSystem getTransitionSystem() {
    return transitionSystem;
  }

  @Override
  public int getMaxAnalysisTimePerSentence() {
    return maxAnalysisTimePerSentence;
  }

  @Override
  public int getMinFreeMemory() {
    return minFreeMemory;
  }

  private void setParserRules(List<ParserRule> parserRules) {
    this.parserRules = parserRules;
    this.parserPositiveRules = new ArrayList<>();
    this.parserNegativeRules = new ArrayList<>();
    for (ParserRule rule : parserRules) {
      if (rule.isNegative())
        parserNegativeRules.add(rule);
      else
        parserPositiveRules.add(rule);
    }
  }

  @Override
  public List<ParserRule> getParserRules() {
    return parserRules;
  }

  @Override
  public ParseComparisonStrategy getParseComparisonStrategy() {
    return parseComparisonStrategy;
  }

  /**
   * If set, we stop as soon as the beam contains <i>n</i> terminal
   * configurations, where <i>n</i> is the beam width.
   */
  public boolean isEarlyStop() {
    return earlyStop;
  }

  public void setEarlyStop(boolean earlyStop) {
    this.earlyStop = earlyStop;
  }

  @Override
  public Set<ParseConfigurationFeature<?>> getParseFeatures() {
    return parseFeatures;
  }

  @Override
  public Parser cloneParser() {
    return new TransitionBasedParser(this);
  }

  @Override
  public boolean isPropagatePosTaggerBeam() {
    return propagatePosTaggerBeam;
  }

}
