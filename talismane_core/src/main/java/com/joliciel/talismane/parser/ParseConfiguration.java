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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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
import com.joliciel.talismane.posTagger.PosTaggedTokenLeftToRightComparator;
import com.joliciel.talismane.rawText.Sentence;

/**
 * A sequence of dependency arcs applied to a given sequence of pos-tagged
 * tokens, as well as the stack and buffer indicating the pos-tagged tokens
 * already visited and not yet visited.
 * 
 * @author Assaf Urieli
 */
public final class ParseConfiguration implements Comparable<ParseConfiguration>, ClassificationSolution, ParseConfigurationWrapper, HasFeatureCache {
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
	private Set<DependencyArc> dependencies;
	private Map<PosTaggedToken, DependencyArc> governingDependencyMap = null;
	private Map<PosTaggedToken, List<PosTaggedToken>> leftDependentMap = null;
	private Map<PosTaggedToken, List<PosTaggedToken>> rightDependentMap = null;
	private Map<PosTaggedToken, List<PosTaggedToken>> dependentMap = null;
	private Map<PosTaggedToken, Map<String, List<PosTaggedToken>>> dependentByLabelMap = null;
	private Map<PosTaggedToken, Transition> dependentTransitionMap = new HashMap<PosTaggedToken, Transition>();

	// Non-projective equivalents to above
	private Set<DependencyArc> dependenciesNonProj;

	private DependencyNode parseTree = null;

	private List<Decision> decisions = new ArrayList<Decision>();
	private int lastProbApplied = 0;
	private List<Solution> underlyingSolutions = new ArrayList<Solution>();
	@SuppressWarnings("rawtypes")
	private ScoringStrategy scoringStrategy;

	private Map<String, FeatureResult<?>> featureCache = new HashMap<String, FeatureResult<?>>();

	private long createDate = System.currentTimeMillis();

	/**
	 * Gets the initial configuration for a particular pos-tagged token
	 * sequence.
	 */
	public ParseConfiguration(PosTagSequence posTagSequence) {
		this.posTagSequence = posTagSequence;
		PosTaggedToken rootToken = posTagSequence.prependRoot();
		this.underlyingSolutions.add(this.posTagSequence);

		this.buffer = new ArrayDeque<PosTaggedToken>(posTagSequence.size());
		for (PosTaggedToken posTaggedToken : posTagSequence)
			this.buffer.add(posTaggedToken);
		this.buffer.remove(rootToken);

		this.stack = new ArrayDeque<PosTaggedToken>();
		this.stack.push(rootToken);

		this.dependencies = new TreeSet<DependencyArc>();
		this.transitions = new ArrayList<Transition>();
		this.scoringStrategy = new GeometricMeanScoringStrategy();
	}

	/**
	 * Clones an existing configuration.
	 */
	public ParseConfiguration(ParseConfiguration history) {
		this.transitions = new ArrayList<Transition>(history.getTransitions());
		this.dependencies = new TreeSet<DependencyArc>(history.getDependenciesInternal());
		this.posTagSequence = history.getPosTagSequence();
		posTagSequence.prependRoot();
		this.underlyingSolutions.add(this.posTagSequence);
		this.buffer = new ArrayDeque<PosTaggedToken>(history.getBuffer());
		this.stack = new ArrayDeque<PosTaggedToken>(history.getStack());
		this.dependentTransitionMap = new HashMap<PosTaggedToken, Transition>(history.getDependentTransitionMap());

		this.decisions = new ArrayList<Decision>(history.getDecisions());
		this.lastProbApplied = history.getLastProbApplied();
		this.scoringStrategy = history.getScoringStrategy();
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

	/**
	 * A set of potentially non-projective dependency arcs defined by the
	 * current configuration, typically because they were read from a manually
	 * annotated corpus. If non such manual dependency arcs were read, will
	 * return the standard set of dependencies.
	 */
	public Set<DependencyArc> getNonProjectiveDependencies() {
		if (dependenciesNonProj == null)
			return dependencies;
		return dependenciesNonProj;
	}

	/**
	 * Get dependencies which are not unlabeled dependencies pointing at the
	 * root.
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
	 * Get the head of a given pos-tagged token in the current set of
	 * dependencies, or null if none exists.
	 */
	public PosTaggedToken getHead(PosTaggedToken dependent) {
		this.updateDependencyMaps();
		DependencyArc arc = this.governingDependencyMap.get(dependent);
		PosTaggedToken head = null;
		if (arc != null)
			head = arc.getHead();
		return head;
	}

	/**
	 * Get the dependency arc governing a given pos-tagged token in the current
	 * set of dependencies.
	 */
	public DependencyArc getGoverningDependency(PosTaggedToken dependent) {
		this.updateDependencyMaps();
		DependencyArc arc = this.governingDependencyMap.get(dependent);
		return arc;
	}

	/**
	 * Get the transition which generated the dependency arc provided.
	 */
	public Transition getTransition(DependencyArc arc) {
		PosTaggedToken dependent = arc.getDependent();
		Transition transition = this.dependentTransitionMap.get(dependent);
		return transition;
	}

	/**
	 * Get a list of left-hand dependents from left-to-right of a given head in
	 * the current set of dependencies.
	 */
	public List<PosTaggedToken> getLeftDependents(PosTaggedToken head) {
		this.updateDependencyMaps();
		List<PosTaggedToken> dependents = this.leftDependentMap.get(head);
		return dependents;
	}

	/**
	 * Get a list of right-hand dependents from left-to-right of a given head in
	 * the current set of dependencies.
	 */
	public List<PosTaggedToken> getRightDependents(PosTaggedToken head) {
		this.updateDependencyMaps();
		List<PosTaggedToken> dependents = this.rightDependentMap.get(head);
		return dependents;
	}

	/**
	 * Get a list of all dependents from left-to-right of a given head in the
	 * current set of dependencies.
	 */
	public List<PosTaggedToken> getDependents(PosTaggedToken head) {
		this.updateDependencyMaps();
		List<PosTaggedToken> dependentList = this.dependentMap.get(head);
		return dependentList;
	}

	/**
	 * Get all dependents from left-to-rigth for a given head and given
	 * dependency label.
	 */
	public List<PosTaggedToken> getDependents(PosTaggedToken head, String label) {
		this.updateDependencyMaps();
		List<PosTaggedToken> deps = null;
		Map<String, List<PosTaggedToken>> labelMap = this.dependentByLabelMap.get(head);
		if (labelMap != null) {
			deps = labelMap.get(label);
		}
		if (deps == null)
			deps = new ArrayList<PosTaggedToken>(0);
		return deps;
	}

	void updateDependencyMaps() {
		if (this.governingDependencyMap == null) {
			this.governingDependencyMap = new HashMap<PosTaggedToken, DependencyArc>();
			this.rightDependentMap = new HashMap<PosTaggedToken, List<PosTaggedToken>>();
			this.leftDependentMap = new HashMap<PosTaggedToken, List<PosTaggedToken>>();
			this.dependentMap = new HashMap<PosTaggedToken, List<PosTaggedToken>>();
			this.dependentByLabelMap = new HashMap<PosTaggedToken, Map<String, List<PosTaggedToken>>>();

			Map<PosTaggedToken, Set<PosTaggedToken>> leftDependentSetMap = new HashMap<PosTaggedToken, Set<PosTaggedToken>>();
			Map<PosTaggedToken, Set<PosTaggedToken>> rightDependentSetMap = new HashMap<PosTaggedToken, Set<PosTaggedToken>>();
			Map<PosTaggedToken, Map<String, Set<PosTaggedToken>>> dependentSetByLabelMap = new HashMap<PosTaggedToken, Map<String, Set<PosTaggedToken>>>();

			for (DependencyArc arc : this.dependencies) {
				this.governingDependencyMap.put(arc.getDependent(), arc);
				Map<PosTaggedToken, Set<PosTaggedToken>> dependentMap = null;
				if (arc.getDependent().getToken().getIndex() < arc.getHead().getToken().getIndex())
					dependentMap = leftDependentSetMap;
				else
					dependentMap = rightDependentSetMap;

				Set<PosTaggedToken> dependents = dependentMap.get(arc.getHead());
				if (dependents == null) {
					dependents = new TreeSet<PosTaggedToken>(new PosTaggedTokenLeftToRightComparator());
					dependentMap.put(arc.getHead(), dependents);
				}
				dependents.add(arc.getDependent());

				Map<String, Set<PosTaggedToken>> labelMap = dependentSetByLabelMap.get(arc.getHead());
				if (labelMap == null) {
					labelMap = new HashMap<String, Set<PosTaggedToken>>();
					dependentSetByLabelMap.put(arc.getHead(), labelMap);
				}

				Set<PosTaggedToken> dependentsByLabel = labelMap.get(arc.getLabel());
				if (dependentsByLabel == null) {
					dependentsByLabel = new TreeSet<PosTaggedToken>(new PosTaggedTokenLeftToRightComparator());
					labelMap.put(arc.getLabel(), dependentsByLabel);
				}
				dependentsByLabel.add(arc.getDependent());
			}

			for (PosTaggedToken head : leftDependentSetMap.keySet()) {
				List<PosTaggedToken> leftDeps = new ArrayList<PosTaggedToken>(leftDependentSetMap.get(head));
				this.leftDependentMap.put(head, leftDeps);
			}

			for (PosTaggedToken head : rightDependentSetMap.keySet()) {
				List<PosTaggedToken> rightDeps = new ArrayList<PosTaggedToken>(rightDependentSetMap.get(head));
				this.rightDependentMap.put(head, rightDeps);
			}

			for (PosTaggedToken head : this.getPosTagSequence()) {
				List<PosTaggedToken> leftDeps = this.leftDependentMap.get(head);
				if (leftDeps == null) {
					leftDeps = new ArrayList<PosTaggedToken>(0);
					this.leftDependentMap.put(head, leftDeps);
				}
				List<PosTaggedToken> rightDeps = this.rightDependentMap.get(head);
				if (rightDeps == null) {
					rightDeps = new ArrayList<PosTaggedToken>(0);
					this.rightDependentMap.put(head, rightDeps);
				}
				List<PosTaggedToken> allDeps = new ArrayList<PosTaggedToken>(leftDeps.size() + rightDeps.size());
				allDeps.addAll(leftDeps);
				allDeps.addAll(rightDeps);
				this.dependentMap.put(head, allDeps);
			}

			for (PosTaggedToken head : dependentSetByLabelMap.keySet()) {
				Map<String, Set<PosTaggedToken>> depSetMap = dependentSetByLabelMap.get(head);
				Map<String, List<PosTaggedToken>> labelMap = new HashMap<String, List<PosTaggedToken>>(depSetMap.size());
				this.dependentByLabelMap.put(head, labelMap);
				for (String label : depSetMap.keySet()) {
					List<PosTaggedToken> deps = new ArrayList<PosTaggedToken>(depSetMap.get(label));
					labelMap.put(label, deps);
				}
			}
		}
	}

	/**
	 * Add the given dependency to the current configuration.
	 * 
	 * @param transition
	 *            the transition generating this dependency
	 * @throws CircularDependencyException
	 *             if this would create a circular dependency
	 */
	public DependencyArc addDependency(PosTaggedToken head, PosTaggedToken dependent, String label, Transition transition) throws CircularDependencyException {
		DependencyArc arc = new DependencyArc(head, dependent, label);
		this.addDependency(arc);
		this.dependentTransitionMap.put(dependent, transition);

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
	 *             if this would create a circular dependency.
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
		// force update of dependency maps
		this.governingDependencyMap = null;
	}

	/**
	 * Add the given dependency to the current configuration's non-projective
	 * dependency set. This should only be used when reading a previously
	 * annotated corpus, to indicate the projective and non-projective governor
	 * for a given token. If the transition system is capable of producing its
	 * own non-projective dependencies there should be no need to distinguish
	 * between projective and non-projective.
	 * 
	 * @throws CircularDependencyException
	 *             if this would create a circular dependency
	 */
	public DependencyArc addManualNonProjectiveDependency(PosTaggedToken head, PosTaggedToken dependent, String label) throws CircularDependencyException {
		DependencyArc arc = new DependencyArc(head, dependent, label);
		PosTaggedToken ancestor = arc.getHead();
		while (ancestor != null) {
			if (ancestor.equals(arc.getDependent())) {
				throw new CircularDependencyException(this, arc.getHead(), arc.getDependent());
			}
			ancestor = this.getHead(ancestor);
		}

		if (this.dependenciesNonProj == null)
			this.dependenciesNonProj = new TreeSet<DependencyArc>();
		this.dependenciesNonProj.add(arc);

		return arc;
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

	Map<PosTaggedToken, Transition> getDependentTransitionMap() {
		return dependentTransitionMap;
	}

	/**
	 * Get the dependency tree represented by this parse configuration, where
	 * the node returned is root.
	 */
	public DependencyNode getParseTree() {
		if (parseTree == null) {
			PosTaggedToken root = null;
			for (PosTaggedToken token : this.posTagSequence) {
				if (token.getTag().equals(PosTag.ROOT_POS_TAG)) {
					root = token;
					break;
				}
			}
			parseTree = new DependencyNode(root, "", this);
			parseTree.autoPopulate();
		}
		return parseTree;
	}

	/**
	 * Returns a dependency node without any dependents attached, corresponding
	 * to the pos-tagged token provided.
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
		this.governingDependencyMap = null;
		this.rightDependentMap = null;
		this.leftDependentMap = null;
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

}
