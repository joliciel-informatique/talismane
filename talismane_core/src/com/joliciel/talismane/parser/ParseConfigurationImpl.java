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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.HarmonicMeanScoringStrategy;
import com.joliciel.talismane.machineLearning.ScoringStrategy;
import com.joliciel.talismane.machineLearning.Solution;
import com.joliciel.talismane.machineLearning.features.Feature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.posTagger.PosTaggedTokenLeftToRightComparator;
import com.joliciel.talismane.tokeniser.Token;

class ParseConfigurationImpl implements ParseConfigurationInternal {
    @SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(ParseConfigurationImpl.class);
	/**
	 * 
	 */
	private static final long serialVersionUID = -5934683538208892981L;
	
	private PosTagSequence posTagSequence;
	private double score;
	private boolean scoreCalculated = false;
	private Deque<PosTaggedToken> buffer;
	private Deque<PosTaggedToken> stack;
	private List<Transition> transitions;
	private Set<DependencyArc> dependencies;
	private Map<PosTaggedToken,DependencyArc> governingDependencyMap = null;
	private Map<PosTaggedToken, Set<PosTaggedToken>> leftDependentMap = null;
	private Map<PosTaggedToken, Set<PosTaggedToken>> rightDependentMap = null;
	private Map<PosTaggedToken,Transition> dependentTransitionMap = new HashMap<PosTaggedToken, Transition>();
	
	private ParserServiceInternal parserServiceInternal;
	private boolean comparisonIndexCalculated = false;
	private int configurationComparisonIndex = 0;
	
	private DependencyNode parseTree = null;
	
	private List<Decision<Transition>> decisions = new ArrayList<Decision<Transition>>();
	private List<Solution<?>> underlyingSolutions = new ArrayList<Solution<?>>();
	private ScoringStrategy scoringStrategy = new HarmonicMeanScoringStrategy();
	
	private Map<String,FeatureResult<?>> featureResults = new HashMap<String, FeatureResult<?>>();

	public ParseConfigurationImpl(PosTagSequence posTagSequence) {
		super();
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
	}
	
	public ParseConfigurationImpl(ParseConfiguration history) {
		super();
		this.transitions = new ArrayList<Transition>(history.getTransitions());
		this.dependencies = new TreeSet<DependencyArc>(history.getDependencies());
		this.posTagSequence = history.getPosTagSequence();
		posTagSequence.prependRoot();
		this.underlyingSolutions.add(this.posTagSequence);
		this.buffer = new ArrayDeque<PosTaggedToken>(history.getBuffer());
		this.stack = new ArrayDeque<PosTaggedToken>(history.getStack());
		this.dependentTransitionMap = new HashMap<PosTaggedToken, Transition>(((ParseConfigurationInternal)history).getDependentTransitionMap());
		
		this.decisions = new ArrayList<Decision<Transition>>(history.getDecisions());
	}
		
	@Override
	public PosTagSequence getPosTagSequence() {
		return this.posTagSequence;
	}

	@Override
	public double getScore() {
		if (!scoreCalculated) {
			score = this.scoringStrategy.calculateScore(this);
			scoreCalculated = true;
		}
		return score;
	}

	@Override
	public Deque<PosTaggedToken> getBuffer() {
		return this.buffer;
	}

	@Override
	public Deque<PosTaggedToken> getStack() {
		return this.stack;
	}

	
	@Override
	public int compareTo(ParseConfiguration o) {
		if (this.getScore()<o.getScore()) {
			return 1;
		} else if (this.getScore()>o.getScore()) {
			return -1;
		} else {
			return 0;
		}
	}

	@Override
	public boolean isTerminal() {
		return this.buffer.isEmpty();
	}

	public List<Transition> getTransitions() {
		return transitions;
	}

	public Set<DependencyArc> getDependencies() {
		return dependencies;
	}

	@Override
	public int getConfigurationComparisonIndex() {
		if (!comparisonIndexCalculated) {
			configurationComparisonIndex = this.getPosTagSequence().getTokenSequence().getAtomicTokenCount() * 1000;
			// if the buffer's empty, this is a terminal configuration, and needs to be given the full token count
			if (this.buffer.size()>0) {
				// remove the atomic tokens of each element still to be processed in the buffer
				for (PosTaggedToken posTaggedToken : this.buffer) {
					Token token = posTaggedToken.getToken();
					if (token.getAtomicParts().size()==0)
						configurationComparisonIndex -= 1000;
					else
						configurationComparisonIndex -= (token.getAtomicParts().size() * 1000);
				}
				
				// remove the atomic tokens of each element still to be processed in the stack
				for (PosTaggedToken posTaggedToken : this.stack) {
					Token token = posTaggedToken.getToken();
					if (token.getAtomicParts().size()==0)
						configurationComparisonIndex -= 1000;
					else
						configurationComparisonIndex -= (token.getAtomicParts().size() * 1000);
				}
				
				// For transitions that don't affect the total count of buffer+stack
				// add a small amount, to ensure the configuration
				// after the transition gets compared with a new set of configurations
				// and not the same set
				for (int i = this.transitions.size()-1; i>=0; i--) {
					Transition transition = this.transitions.get(i);
					if (!transition.doesReduce()) {
						configurationComparisonIndex += 1;
					} else {
						break;
					}
				}
			} // is the buffer empty?
			comparisonIndexCalculated = true;
		}
		return configurationComparisonIndex;
	}


	@Override
	public PosTaggedToken getHead(PosTaggedToken dependent) {
		this.updateDependencyMaps();
		DependencyArc arc = this.governingDependencyMap.get(dependent);
		PosTaggedToken head = null;
		if (arc!=null)
			head = arc.getHead();
		return head;
	}


	@Override
	public DependencyArc getGoverningDependency(PosTaggedToken dependent) {
		this.updateDependencyMaps();
		DependencyArc arc = this.governingDependencyMap.get(dependent);
		return arc;
	}
	
	public Transition getTransition(DependencyArc arc) {
		PosTaggedToken dependent = arc.getDependent();
		Transition transition = this.dependentTransitionMap.get(dependent);
		return transition;
	}
	
	@Override
	public List<PosTaggedToken> getLeftDependents(PosTaggedToken head) {
		this.updateDependencyMaps();
		Set<PosTaggedToken> dependents = this.leftDependentMap.get(head);
		List<PosTaggedToken> dependentList = new ArrayList<PosTaggedToken>();
		if (dependents!=null)
			dependentList.addAll(dependents);
		return dependentList;
	}
	
	@Override
	public List<PosTaggedToken> getRightDependents(PosTaggedToken head) {
		this.updateDependencyMaps();
		Set<PosTaggedToken> dependents = this.rightDependentMap.get(head);
		List<PosTaggedToken> dependentList = new ArrayList<PosTaggedToken>();
		if (dependents!=null)
			dependentList.addAll(dependents);
		return dependentList;
	}

	
	@Override
	public List<PosTaggedToken> getDependents(PosTaggedToken head) {
		this.updateDependencyMaps();
		Set<PosTaggedToken> dependents = new TreeSet<PosTaggedToken>();
		dependents.addAll(this.getLeftDependents(head));
		dependents.addAll(this.getRightDependents(head));
		List<PosTaggedToken> dependentList = new ArrayList<PosTaggedToken>(dependents);
		return dependentList;
	}

	void updateDependencyMaps() {
		if (this.governingDependencyMap==null) {
			this.governingDependencyMap = new HashMap<PosTaggedToken, DependencyArc>();
			this.rightDependentMap = new HashMap<PosTaggedToken, Set<PosTaggedToken>>();
			this.leftDependentMap = new HashMap<PosTaggedToken, Set<PosTaggedToken>>();
			
			for (DependencyArc arc : this.dependencies) {
				this.governingDependencyMap.put(arc.getDependent(), arc);
				Map<PosTaggedToken, Set<PosTaggedToken>> dependentMap = null;
				if (arc.getDependent().getToken().getIndex()<arc.getHead().getToken().getIndex())
					dependentMap = this.leftDependentMap;
				else
					dependentMap = this.rightDependentMap;
				
				Set<PosTaggedToken> dependents = dependentMap.get(arc.getHead());
				if (dependents==null) {
					dependents = new TreeSet<PosTaggedToken>(new PosTaggedTokenLeftToRightComparator());
					dependentMap.put(arc.getHead(), dependents);
				}
				dependents.add(arc.getDependent());
			}
		}
	}
	@Override
	public DependencyArc addDependency(PosTaggedToken head,
			PosTaggedToken dependent, String label, Transition transition) {
		DependencyArc arc = this.parserServiceInternal.getDependencyArc(head, dependent, label);
		this.addDependency(arc);
		this.dependentTransitionMap.put(dependent, transition);
		return arc;
	}
	
	void addDependency(DependencyArc arc) {
		PosTaggedToken ancestor = arc.getHead();
		while (ancestor!=null) {
			if (ancestor.equals(arc.getDependent())) {
				throw new CircularDependencyException(this, arc.getHead(), arc.getDependent());
			}
			ancestor = this.getHead(ancestor);
		}
		
		this.dependencies.add(arc);
		
		if (this.governingDependencyMap!=null) {
			this.governingDependencyMap.put(arc.getDependent(), arc);
			Map<PosTaggedToken, Set<PosTaggedToken>> dependentMap = null;
			if (arc.getDependent().getToken().getIndex()<arc.getHead().getToken().getIndex())
				dependentMap = this.leftDependentMap;
			else
				dependentMap = this.rightDependentMap;
			
			Set<PosTaggedToken> dependents = dependentMap.get(arc.getHead());
			if (dependents==null) {
				dependents = new TreeSet<PosTaggedToken>(new PosTaggedTokenLeftToRightComparator());
				dependentMap.put(arc.getHead(), dependents);
			}
			dependents.add(arc.getDependent());
		}
	}

	public ParserServiceInternal getParserServiceInternal() {
		return parserServiceInternal;
	}

	public void setParserServiceInternal(ParserServiceInternal parserServiceInternal) {
		this.parserServiceInternal = parserServiceInternal;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		Iterator<PosTaggedToken> stackIterator = this.stack.iterator();
		if (stackIterator.hasNext())
			sb.insert(0, stackIterator.next().toString());
		if (stackIterator.hasNext())
			sb.insert(0, stackIterator.next().toString()+ ",");
		if (stackIterator.hasNext())
			sb.insert(0, stackIterator.next().toString()+ ",");
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

	public Map<PosTaggedToken, Transition> getDependentTransitionMap() {
		return dependentTransitionMap;
	}

	@Override
	public DependencyNode getParseTree() {
		if (parseTree==null) {
			PosTaggedToken root = null;
			for (PosTaggedToken token : this.posTagSequence) {
				if (token.getTag().equals(PosTag.ROOT_POS_TAG)) {
					root = token;
					break;
				}
			}
			parseTree = this.parserServiceInternal.getDependencyNode(root, "", this);
			parseTree.autoPopulate();
		}
		return parseTree;
	}

	@Override
	public DependencyNode getDetachedDependencyNode(
			PosTaggedToken posTaggedToken) {
		DependencyNode node = this.parserServiceInternal.getDependencyNode(posTaggedToken, "", this);
		return node;
	}

	@Override
	public List<Decision<Transition>> getDecisions() {
		return decisions;
	}

	@Override
	public List<Solution<?>> getUnderlyingSolutions() {
		return underlyingSolutions;
	}

	@Override
	public void addDecision(Decision<Transition> decision) {
		this.decisions.add(decision);
	}

	public ScoringStrategy getScoringStrategy() {
		return scoringStrategy;
	}

	public void setScoringStrategy(ScoringStrategy scoringStrategy) {
		this.scoringStrategy = scoringStrategy;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T,Y> FeatureResult<Y> getResultFromCache(
			Feature<T, Y> feature) {
		FeatureResult<Y> result = null;
	
		if (this.featureResults.containsKey(feature.getName())) {
			result = (FeatureResult<Y>) this.featureResults.get(feature.getName());
		}
		return result;
	}

	
	@Override
	public <T,Y> void putResultInCache(
			Feature<T, Y> feature,
			FeatureResult<Y> featureResult) {
		this.featureResults.put(feature.getName(), featureResult);	
	}

	@Override
	public ParseConfiguration getParseConfiguration() {
		return this;
	}


}
