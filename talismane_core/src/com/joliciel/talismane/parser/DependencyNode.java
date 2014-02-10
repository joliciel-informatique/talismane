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

import java.util.Set;

import com.joliciel.talismane.posTagger.PosTaggedToken;

/**
 * A node inside a dependency tree.
 * @author Assaf Urieli
 *
 */
public interface DependencyNode {
	/**
	 * The actual pos-tagged token in this node.
	 * @return
	 */
	public PosTaggedToken getPosTaggedToken();
	
	/**
	 * The dependency label tying this node to its parent.
	 * @return
	 */
	public String getLabel();
	
	/**
	 * This node's parent.
	 * @return
	 */
	public DependencyNode getParent();
	public void setParent(DependencyNode parent);
	
	/**
	 * This node's dependents.
	 * @return
	 */
	public Set<DependencyNode> getDependents();
	
	/**
	 * Add an existing dependent of the current node's token to the current dependency node.
	 * The dependent must already exist in the parse configuration.
	 * This is useful for constructing sub-trees out of the existing parse tree.
	 * @param token
	 * @param label
	 * @return
	 */
	public DependencyNode addDependent(PosTaggedToken token);
	
	/**
	 * Add a dependent in the form of an existing dependency node.
	 * @param dependent
	 */
	public void addDependent(DependencyNode dependent);
	
	/**
	 * The parse configuration from which this node was derived.
	 * @return
	 */
	public ParseConfiguration getParseConfiguration();
	
	/**
	 * Populate this node's dependents directly from the parse configuration.
	 */
	public void autoPopulate();
	
	/**
	 * Clone the current dependency node.
	 * @return
	 */
	public DependencyNode cloneNode();
	
	/**
	 * Return the depth of this tree starting at its head (1).
	 * @return
	 */
	public int getDepth();
	
	/**
	 * Language-specific depth as a typical user would expect to see it.<br/>
	 * In French, for example, a preposition and its object would thus be a single layer of depth, equivalent to
	 * a single adjective when modifying a noun.
	 * A determinent would not add to its governor's depth.
	 * @param zeroDepthLabels labels which shouldn't be counted for depth calculation
	 * @return
	 */
	public int getPerceivedDepth(Set<String> zeroDepthLabels);
	
	/**
	 * Removes a node (and all of its children) from this dependency node's tree, wherever it may be located.
	 * @param node
	 * @returns true if the node was removed.
	 */
	public boolean removeNode(DependencyNode node);
	
	/**
	 * The first token comprising this dependency node, if viewed in linear order within a sentence.
	 * @return
	 */
	public PosTaggedToken getFirstToken();
	
	/**
	 * The last token comprising this dependency node, if viewed in linear order within a sentence.
	 * @return
	 */
	public PosTaggedToken getLastToken();
	
	/**
	 * Return true if this node is contiguous, false if it contains gaps.
	 * @return
	 */
	public boolean isContiguous();

	public void setDirty();
}
