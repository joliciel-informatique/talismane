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

import java.util.Deque;
import java.util.List;
import java.util.Set;

import com.joliciel.talismane.filters.Sentence;
import com.joliciel.talismane.machineLearning.ClassificationSolution;
import com.joliciel.talismane.machineLearning.RankingSolution;
import com.joliciel.talismane.machineLearning.features.HasFeatureCache;
import com.joliciel.talismane.parser.features.ParseConfigurationWrapper;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTaggedToken;

/**
 * A sequence of dependency arcs applied to a given sequence of pos-tagged tokens,
 * as well as the stack and buffer indicating the pos-tagged tokens already visited and not yet visited.
 * @author Assaf Urieli
 */
public interface ParseConfiguration extends Comparable<ParseConfiguration>, ClassificationSolution<Transition>, RankingSolution, ParseConfigurationWrapper, HasFeatureCache {
	/**
	 * Get the PosTag Sequence on which this ParseSequence is based.
	 * @return
	 */
	public PosTagSequence getPosTagSequence();
	
	/**
	 * This parse configuration's score.
	 */
	public double getScore();
	public void setScore(double score);
	
	/**
	 * The list of transitions which generated the present parse configuration.
	 * @return
	 */
	public List<Transition> getTransitions();
	
	/**
	 * A set of dependency arcs defined by the current configuration.
	 * @return
	 */
	public Set<DependencyArc> getDependencies();
	
	/**
	 * Add the given dependency to the current configuration.
	 * @param head
	 * @param dependent
	 * @param label
	 * @param transition the transition generating this dependency
	 * @return
	 */
	public DependencyArc addDependency(PosTaggedToken head, PosTaggedToken dependent,
			String label, Transition transition);
	
	/**
	 * A buffer of pos-tagged tokens that have not yet been processed.
	 * @return
	 */
	public Deque<PosTaggedToken> getBuffer();
	
	/**
	 * A stack of pos-tagged tokens that have been partially processed.
	 * @return
	 */
	public Deque<PosTaggedToken> getStack();
	
	/**
	 * Is this a terminal configuration?
	 * @return
	 */
	public boolean isTerminal();

	/**
	 * Get the head of a given pos-tagged token in the current set of dependencies,
	 * or null if none exists.
	 */
	public PosTaggedToken getHead(PosTaggedToken dependent);
	
	/**
	 * Get the dependency arc governing a given pos-tagged token in the current set of dependencies.
	 */
	public DependencyArc getGoverningDependency(PosTaggedToken dependent);

	
	/**
	 * Get a list of left-hand dependents from left-to-right of a given head in the current set of dependencies.
	 */
	public List<PosTaggedToken> getLeftDependents(PosTaggedToken head);
	
	/**
	 * Get a list of right-hand dependents from left-to-right of a given head in the current set of dependencies.
	 */
	public List<PosTaggedToken> getRightDependents(PosTaggedToken head);
	
	/**
	 * Get a list of all dependents from left-to-right of a given head in the current set of dependencies.
	 * @param head
	 * @return
	 */
	public List<PosTaggedToken> getDependents(PosTaggedToken head);
	
	/**
	 * Get the transition which generated the dependency arc provided.
	 * @param arc
	 * @return
	 */
	public Transition getTransition(DependencyArc arc);
	
	/**
	 * Get the dependency tree represented by this parse configuration, where the node returned is root.
	 * @return
	 */
	public DependencyNode getParseTree();

	/**
	 * Returns a dependency node without any dependents attached, corresponding to the pos-tagged token provided.
	 * @param posTaggedToken
	 * @return
	 */
	public DependencyNode getDetachedDependencyNode(PosTaggedToken posTaggedToken);
	
	/**
	 * Clear out any transitory dependents that can be recalculated if required.
	 */
	public void clearMemory();
	
	/**
	 * The sentence on which this parse configuration is based.
	 * @return
	 */
	public Sentence getSentence();
	
	/**
	 * Get dependencies which are not unlabeled dependencies pointing at the root.
	 * @return
	 */
	public Set<DependencyArc> getRealDependencies();
}
