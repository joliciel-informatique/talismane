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
	public PosTaggedToken getToken();
	
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
}
