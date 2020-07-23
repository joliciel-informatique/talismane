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

import java.io.Serializable;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.machineLearning.ClassificationSolution;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.GeometricMeanScoringStrategy;
import com.joliciel.talismane.machineLearning.ScoringStrategy;
import com.joliciel.talismane.machineLearning.Solution;
import com.joliciel.talismane.machineLearning.features.Feature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.HasFeatureCache;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.parser.features.ParseConfigurationWrapper;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.rawText.Sentence;

/**
 * A sequence of dependency arcs applied to a given sequence of pos-tagged
 * tokens, as well as the stack and buffer indicating the pos-tagged tokens
 * already visited and not yet visited.
 * 
 * @author Assaf Urieli
 */
public final class ParseConfiguration implements Comparable<ParseConfiguration>, ClassificationSolution, ParseConfigurationWrapper, HasFeatureCache, Serializable {
  private static final long serialVersionUID = 1L;
  
  private static final Logger LOG = LoggerFactory.getLogger(ParseConfiguration.class);

  private PosTagSequence posTagSequence;
  private double score;
  private double rankingScore;
  private boolean scoreCalculated = false;
  private boolean useGeometricMeanForProbs = true;

  private Deque<PosTaggedToken> buffer;
  private Deque<PosTaggedToken> stack;
  private List<Transition> transitions;

  // Projective dependency information
  private final NavigableSet<DependencyArc> dependencies;
  private final DependencyArc[] governingDependencyMap;
  private final int[] governorMap;
  private final String[] labelMap;
  private final Transition[] dependentTransitionMap;

  // Non-projective equivalents to above
  private boolean hasNonProjDependencies = false;
  private final NavigableSet<DependencyArc> dependenciesNonProj;
  private final DependencyArc[] governingDependencyNonProjMap;
  private final int[] governorNonProjMap;
  private final String[] labelNonProjMap;

  private List<Decision> decisions = new ArrayList<Decision>();
  private int lastProbApplied = 0;
  private List<Solution> underlyingSolutions = new ArrayList<Solution>();
  @SuppressWarnings("rawtypes")
  private ScoringStrategy scoringStrategy;

  private transient Map<String, FeatureResult<?>> featureCache = new HashMap<String, FeatureResult<?>>();

  private long createDate = System.currentTimeMillis();

  /**
   * Gets the initial configuration for a particular pos-tagged token sequence.
   */
  public ParseConfiguration(PosTagSequence posTagSequence) {
    this.posTagSequence = posTagSequence;
    PosTaggedToken rootToken = posTagSequence.prependRoot();
    this.underlyingSolutions.add(this.posTagSequence);

    this.buffer = new ArrayDeque<>(posTagSequence.size());
    for (PosTaggedToken posTaggedToken : posTagSequence)
      this.buffer.add(posTaggedToken);
    this.buffer.remove(rootToken);

    this.stack = new ArrayDeque<PosTaggedToken>();
    this.stack.push(rootToken);

    this.dependentTransitionMap = new Transition[posTagSequence.size()];

    this.dependencies = new TreeSet<>();
    this.governingDependencyMap = new DependencyArc[posTagSequence.size()];
    this.governorMap = new int[posTagSequence.size()];
    Arrays.fill(this.governorMap, -1);

    this.labelMap = new String[posTagSequence.size()];
    this.dependenciesNonProj = new TreeSet<>();
    this.governingDependencyNonProjMap = new DependencyArc[posTagSequence.size()];
    this.governorNonProjMap = new int[posTagSequence.size()];
    Arrays.fill(this.governorNonProjMap, -1);
    this.labelNonProjMap = new String[posTagSequence.size()];

    this.transitions = new ArrayList<Transition>();
    this.scoringStrategy = new GeometricMeanScoringStrategy();
  }

  /**
   * Clones an existing configuration.
   */
  public ParseConfiguration(ParseConfiguration history) {
    this.transitions = new ArrayList<>(history.transitions);
    this.posTagSequence = history.posTagSequence;
    posTagSequence.prependRoot();
    this.underlyingSolutions.add(this.posTagSequence);
    this.buffer = new ArrayDeque<>(history.buffer);
    this.stack = new ArrayDeque<>(history.stack);
    this.dependentTransitionMap = Arrays.copyOf(history.dependentTransitionMap, history.dependentTransitionMap.length);

    this.decisions = new ArrayList<Decision>(history.decisions);
    this.lastProbApplied = history.lastProbApplied;
    this.scoringStrategy = history.scoringStrategy;

    this.dependencies = new TreeSet<>(history.dependencies);
    this.governingDependencyMap = Arrays.copyOf(history.governingDependencyMap, history.governingDependencyMap.length);
    this.governorMap = Arrays.copyOf(history.governorMap, history.governorMap.length);
    this.labelMap = Arrays.copyOf(history.labelMap, history.labelMap.length);

    this.hasNonProjDependencies = history.hasNonProjDependencies;
    if (this.hasNonProjDependencies) {
      this.dependenciesNonProj = new TreeSet<>(history.dependenciesNonProj);
      this.governingDependencyNonProjMap = Arrays.copyOf(history.governingDependencyNonProjMap, history.governingDependencyNonProjMap.length);
      this.governorNonProjMap = Arrays.copyOf(history.governorNonProjMap, history.governorNonProjMap.length);
      this.labelNonProjMap = Arrays.copyOf(history.labelNonProjMap, history.labelNonProjMap.length);
    } else {
      this.dependenciesNonProj = new TreeSet<>();
      this.governingDependencyNonProjMap = new DependencyArc[posTagSequence.size()];
      this.governorNonProjMap = new int[posTagSequence.size()];
      Arrays.fill(this.governorNonProjMap, -1);
      this.labelNonProjMap = new String[posTagSequence.size()];
    }
  }

  /**
   * Get the PosTag Sequence on which this ParseSequence is based.
   */
  public PosTagSequence getPosTagSequence() {
    return this.posTagSequence;
  }

  /**
   * This parse configuration's score.
   */
  @Override
  @SuppressWarnings("unchecked")

  public double getScore() {
    if (!scoreCalculated) {
      score = this.scoringStrategy.calculateScore(this);
      scoreCalculated = true;
    }
    return score;
  }

  public double getRankingScore() {
    return rankingScore;
  }

  public void setRankingScore(double rankingScore) {
    this.rankingScore = rankingScore;
  }

  /**
   * A buffer of pos-tagged tokens that have not yet been processed.
   */
  public Deque<PosTaggedToken> getBuffer() {
    return this.buffer;
  }

  /**
   * A stack of pos-tagged tokens that have been partially processed.
   */
  public Deque<PosTaggedToken> getStack() {
    return this.stack;
  }

  @Override
  public int compareTo(ParseConfiguration o) {
    // order by descending score if possible, otherwise by create date,
    // otherwise by hash code
    if (this == o)
      return 0;
    else if (this.getScore() < o.getScore())
      return 1;
    else if (this.getScore() > o.getScore())
      return -1;

    return new Long(this.getCreateDate() - o.getCreateDate()).intValue();
  }

  /**
   * Is this a terminal configuration?
   */
  public boolean isTerminal() {
    return this.buffer.isEmpty();
  }

  /**
   * The list of transitions which generated the present parse configuration.
   */
  public List<Transition> getTransitions() {
    return transitions;
  }

  Set<DependencyArc> getDependenciesInternal() {
    return dependencies;
  }

  /**
   * A set of dependency arcs defined by the current configuration.
   */
  public Set<DependencyArc> getDependencies() {
    return dependencies;
  }

  private boolean hasNonProjectiveDependencies() {
    return this.hasNonProjDependencies;
  }

  /**
   * A set of potentially non-projective dependency arcs defined by the current
   * configuration, typically because they were read from a manually annotated
   * corpus. If non such manual dependency arcs were read, will return the
   * standard set of dependencies.
   */
  public Set<DependencyArc> getNonProjectiveDependencies() {
    if (this.hasNonProjectiveDependencies())
      return dependenciesNonProj;
    return dependencies;
  }

  /**
   * Get dependencies which are not unlabeled dependencies pointing at the root.
   */
  public Set<DependencyArc> getRealDependencies() {
    Set<DependencyArc> realDependencies = new TreeSet<DependencyArc>();
    for (DependencyArc arc : dependencies) {
      if (arc.getHead().getTag().equals(PosTag.ROOT_POS_TAG) && (arc.getLabel() == null || arc.getLabel().length() == 0)) {
        // do nothing
      } else {
        realDependencies.add(arc);
      }
    }
    return realDependencies;
  }

  /**
   * Same as {@link #getHead(PosTaggedToken, boolean)} for projective=true.
   */
  public PosTaggedToken getHead(PosTaggedToken dependent) {
    return this.getHead(dependent, true);
  }

  /**
   * Get the head of a given pos-tagged token in the current set of
   * dependencies, or null if none exists.
   * 
   * @param dependent
   *          the token whose governing arc we want
   * @param projective
   *          if true, consider projective set, if false consider non-projective
   *          set.
   */
  public PosTaggedToken getHead(PosTaggedToken dependent, boolean projective) {
    DependencyArc arc = this.getGoverningDependency(dependent, projective);
    PosTaggedToken head = null;
    if (arc != null)
      head = arc.getHead();
    return head;
  }

  /**
   * Same as {@link #getGoverningDependency(PosTaggedToken, boolean)} for
   * projective=true.
   */
  public DependencyArc getGoverningDependency(PosTaggedToken dependent) {
    return this.getGoverningDependency(dependent, true);
  }

  /**
   * Get the dependency arc governing a given pos-tagged token in the current
   * set of dependencies.
   * 
   * @param dependent
   *          the token whose governing arc we want
   * @param projective
   *          if true, consider projective set, if false consider non-projective
   *          set.
   * @return
   */
  public DependencyArc getGoverningDependency(PosTaggedToken dependent, boolean projective) {
    if (!projective && this.hasNonProjectiveDependencies()) {
      return this.governingDependencyNonProjMap[dependent.getIndex()];
    } else {
      return this.governingDependencyMap[dependent.getIndex()];
    }
  }

  /**
   * Get the transition which generated the dependency arc provided.
   */
  public Transition getTransition(DependencyArc arc) {
    PosTaggedToken dependent = arc.getDependent();
    Transition transition = this.dependentTransitionMap[dependent.getIndex()];
    return transition;
  }

  /**
   * Same as {@link #getLeftDependents(PosTaggedToken, boolean)} for
   * projective=true.
   */
  public List<PosTaggedToken> getLeftDependents(PosTaggedToken head) {
    return this.getLeftDependents(head, true);
  }

  /**
   * Get a list of left-hand dependents ordered from left-to-right of a given
   * head in the current set of dependencies.
   * 
   * @param projective
   *          if true, consider projective set, if false consider non-projective
   *          set.
   */
  public List<PosTaggedToken> getLeftDependents(PosTaggedToken head, boolean projective) {
    int[] govMap = this.governorMap;
    if (!projective && this.hasNonProjectiveDependencies()) {
      govMap = this.governorNonProjMap;
    }
    List<PosTaggedToken> deps = new ArrayList<>();
    for (int i = 0; i < head.getIndex(); i++) {
      if (govMap[i] == head.getIndex())
        deps.add(posTagSequence.get(i));
    }
    return deps;
  }

  /**
   * Same as {@link #getRightDependents(PosTaggedToken, boolean)} for
   * projective=true.
   */
  public List<PosTaggedToken> getRightDependents(PosTaggedToken head) {
    return this.getRightDependents(head, true);
  }

  /**
   * Get a list of right-hand dependents ordered from left-to-right of a given
   * head in the current set of dependencies.
   */
  public List<PosTaggedToken> getRightDependents(PosTaggedToken head, boolean projective) {
    int[] govMap = this.governorMap;
    if (!projective && this.hasNonProjectiveDependencies()) {
      govMap = this.governorNonProjMap;
    }
    List<PosTaggedToken> deps = new ArrayList<>();
    for (int i = head.getIndex(); i < govMap.length; i++) {
      if (govMap[i] == head.getIndex())
        deps.add(posTagSequence.get(i));
    }
    return deps;
  }

  /**
   * Same as {@link #getDependents(PosTaggedToken, boolean)} for
   * projective=true.
   */
  public List<PosTaggedToken> getDependents(PosTaggedToken head) {
    return this.getDependents(head, true);
  }

  /**
   * Get a list of all dependents ordered from left-to-right of a given head in
   * the current set of dependencies.
   * 
   * @param projective
   *          if true, consider projective set, if false consider non-projective
   *          set.
   */
  public List<PosTaggedToken> getDependents(PosTaggedToken head, boolean projective) {
    int[] govMap = this.governorMap;
    if (!projective && this.hasNonProjectiveDependencies()) {
      govMap = this.governorNonProjMap;
    }
    List<PosTaggedToken> deps = new ArrayList<>();
    for (int i = 0; i < govMap.length; i++) {
      if (govMap[i] == head.getIndex())
        deps.add(posTagSequence.get(i));
    }
    return deps;
  }

  /**
   * Same as {@link #getDependents(PosTaggedToken, String, boolean)} for
   * projective=true.
   */
  public List<PosTaggedToken> getDependents(PosTaggedToken head, String label) {
    return this.getDependents(head, label, true);
  }

  /**
   * Get all dependents ordered from left-to-right for a given head and given
   * dependency label.
   * 
   * @param projective
   *          if true, consider projective set, if false consider non-projective
   *          set.
   */
  public List<PosTaggedToken> getDependents(PosTaggedToken head, String label, boolean projective) {
    int[] govMap = this.governorMap;
    String[] labelMap = this.labelMap;
    if (!projective && this.hasNonProjectiveDependencies()) {
      govMap = this.governorNonProjMap;
      labelMap = this.labelNonProjMap;
    }
    List<PosTaggedToken> deps = new ArrayList<>();
    for (int i = 0; i < govMap.length; i++) {
      if (govMap[i] == head.getIndex() && label.equals(labelMap[i]))
        deps.add(posTagSequence.get(i));
    }
    return deps;
  }

  /**
   * Add the given dependency to the current configuration.
   * 
   * @param transition
   *          the transition generating this dependency
   * @throws CircularDependencyException
   *           if this would create a circular dependency
   */
  public DependencyArc addDependency(PosTaggedToken head, PosTaggedToken dependent, String label, Transition transition) throws CircularDependencyException {
    DependencyArc arc = new DependencyArc(head, dependent, label);
    if (LOG.isTraceEnabled())
      LOG.trace("Adding arc " + arc + " with transition " + transition);

    this.addDependency(arc);
    this.dependentTransitionMap[dependent.getIndex()] = transition;

    // calculate probability based on decisions
    if (LOG.isTraceEnabled())
      LOG.trace("Prob for " + arc.toString());

    double probLog = 0.0;
    int numDecisions = 0;
    for (int i = lastProbApplied; i < this.decisions.size(); i++) {
      Decision decision = decisions.get(i);
      probLog += decision.getProbabilityLog();
      if (LOG.isTraceEnabled()) {
        LOG.trace(decision.getOutcome() + ", *= " + decision.getProbability());
      }
      numDecisions++;
    }

    if (useGeometricMeanForProbs) {
      if (numDecisions > 0)
        probLog /= numDecisions;
    }

    arc.setProbability(Math.exp(probLog));
    this.lastProbApplied = this.decisions.size();

    if (LOG.isTraceEnabled())
      LOG.trace("prob=" + arc.getProbability());

    return arc;
  }

  /**
   * 
   * @param arc
   * @throws CircularDependencyException
   *           if this would create a circular dependency.
   */
  void addDependency(DependencyArc arc) throws CircularDependencyException {
    PosTaggedToken ancestor = arc.getHead();
    while (ancestor != null) {
      if (ancestor.equals(arc.getDependent())) {
        throw new CircularDependencyException(this, arc.getHead(), arc.getDependent());
      }
      ancestor = this.getHead(ancestor);
    }

    this.dependencies.add(arc);
    this.governingDependencyMap[arc.getDependent().getIndex()] = arc;
    this.governorMap[arc.getDependent().getIndex()] = arc.getHead().getIndex();
    this.labelMap[arc.getDependent().getIndex()] = arc.getLabel();

    if (LOG.isTraceEnabled()) {
      LOG.trace("Added arc: " + arc);
      LOG.trace("dependencies: " + this.dependencies);
      LOG.trace("governingDependencyMap: " + this.governingDependencyMap);
    }
  }

  public void removeDependency(DependencyArc arc) {
    if (this.dependencies.contains(arc)) {
      this.dependencies.remove(arc);
      this.governingDependencyMap[arc.getDependent().getIndex()] = null;
      this.governorMap[arc.getDependent().getIndex()] = -1;
      this.labelMap[arc.getDependent().getIndex()] = null;
    }
  }

  /**
   * Add the given dependency to the current configuration's non-projective
   * dependency set. This should only be used when reading a previously
   * annotated corpus, to indicate the projective and non-projective governor
   * for a given token. If the transition system is capable of producing its own
   * non-projective dependencies there should be no need to distinguish between
   * projective and non-projective.
   * 
   * @throws CircularDependencyException
   *           if this would create a circular dependency
   */
  public DependencyArc addManualNonProjectiveDependency(PosTaggedToken head, PosTaggedToken dependent, String label) throws CircularDependencyException {
    if (!this.hasNonProjDependencies) {
      this.hasNonProjDependencies = true;
    }

    DependencyArc arc = new DependencyArc(head, dependent, label);
    PosTaggedToken ancestor = arc.getHead();
    while (ancestor != null) {
      if (ancestor.equals(arc.getDependent())) {
        throw new CircularDependencyException(this, arc.getHead(), arc.getDependent());
      }
      ancestor = this.getHead(ancestor, false);
    }

    this.dependenciesNonProj.add(arc);

    this.governingDependencyNonProjMap[arc.getDependent().getIndex()] = arc;
    this.governorNonProjMap[arc.getDependent().getIndex()] = arc.getHead().getIndex();
    this.labelNonProjMap[arc.getDependent().getIndex()] = arc.getLabel();

    return arc;
  }

  public void removeNonProjectiveDependency(DependencyArc arc) {
    if (this.dependenciesNonProj.contains(arc)) {
      this.dependenciesNonProj.remove(arc);
      this.governingDependencyNonProjMap[arc.getDependent().getIndex()] = null;
      this.governorMap[arc.getDependent().getIndex()] = -1;
      this.labelNonProjMap[arc.getDependent().getIndex()] = null;
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    Iterator<PosTaggedToken> stackIterator = this.stack.iterator();
    if (stackIterator.hasNext())
      sb.insert(0, stackIterator.next().toString());
    if (stackIterator.hasNext())
      sb.insert(0, stackIterator.next().toString() + ",");
    if (stackIterator.hasNext())
      sb.insert(0, stackIterator.next().toString() + ",");
    if (stackIterator.hasNext())
      sb.insert(0, "...,");
    sb.insert(0, "Stack[");
    sb.append("]. Buffer[");
    Iterator<PosTaggedToken> bufferIterator = this.buffer.iterator();
    if (bufferIterator.hasNext())
      sb.append(bufferIterator.next().toString());
    if (bufferIterator.hasNext())
      sb.append("," + bufferIterator.next().toString());
    if (bufferIterator.hasNext())
      sb.append("," + bufferIterator.next().toString());
    if (bufferIterator.hasNext())
      sb.append(",...");
    sb.append("]");
    sb.append(" Deps[");
    for (DependencyArc arc : this.dependencies) {
      sb.append(arc.toString() + ",");
    }
    sb.append("]");
    return sb.toString();
  }

  /**
   * Returns a dependency node without any dependents attached, corresponding to
   * the pos-tagged token provided.
   */
  public DependencyNode getDetachedDependencyNode(PosTaggedToken posTaggedToken) {
    DependencyArc arc = this.getGoverningDependency(posTaggedToken);
    DependencyNode node = new DependencyNode(posTaggedToken, arc.getLabel(), this);
    return node;
  }

  @Override
  public List<Decision> getDecisions() {
    return decisions;
  }

  @Override
  public List<Solution> getUnderlyingSolutions() {
    return underlyingSolutions;
  }

  @Override
  public void addDecision(Decision decision) {
    this.decisions.add(decision);
  }

  @Override
  @SuppressWarnings("rawtypes")
  public ScoringStrategy getScoringStrategy() {
    return scoringStrategy;
  }

  @Override
  public void setScoringStrategy(@SuppressWarnings("rawtypes") ScoringStrategy scoringStrategy) {
    this.scoringStrategy = scoringStrategy;
  }

  @Override
  @SuppressWarnings("unchecked")

  public <T, Y> FeatureResult<Y> getResultFromCache(Feature<T, Y> feature, RuntimeEnvironment env) {
    FeatureResult<Y> result = null;

    String key = feature.getName() + env.getKey();
    if (this.featureCache.containsKey(key)) {
      result = (FeatureResult<Y>) this.featureCache.get(key);
    }
    return result;
  }

  @Override
  public <T, Y> void putResultInCache(Feature<T, Y> feature, FeatureResult<Y> featureResult, RuntimeEnvironment env) {
    String key = feature.getName() + env.getKey();
    this.featureCache.put(key, featureResult);
  }

  @Override
  public ParseConfiguration getParseConfiguration() {
    return this;
  }

  /**
   * Clear out any transitory dependents that can be recalculated if required.
   */
  public void clearMemory() {
  }

  /**
   * The sentence on which this parse configuration is based.
   */
  public Sentence getSentence() {
    return this.getPosTagSequence().getTokenSequence().getSentence();
  }

  long getCreateDate() {
    return createDate;
  }

  /**
   * True: use a geometric mean when calculating individual arc probabilities
   * (which multiply the probabilities for the transitions since the last arc
   * was added). False: use the simple product. Default is true.
   */
  public boolean isUseGeometricMeanForProbs() {
    return useGeometricMeanForProbs;
  }

  public void setUseGeometricMeanForProbs(boolean useGeometricMeanForProbs) {
    this.useGeometricMeanForProbs = useGeometricMeanForProbs;
  }

  int getLastProbApplied() {
    return lastProbApplied;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ParseConfiguration that = (ParseConfiguration) o;
    return useGeometricMeanForProbs == that.useGeometricMeanForProbs &&
      posTagSequence.equals(that.posTagSequence) &&
      transitions.equals(that.transitions) &&
      decisions.equals(that.decisions) &&
      scoringStrategy.equals(that.scoringStrategy);
  }

  @Override
  public int hashCode() {
    return Objects.hash(posTagSequence, useGeometricMeanForProbs, transitions, decisions, scoringStrategy);
  }
}
