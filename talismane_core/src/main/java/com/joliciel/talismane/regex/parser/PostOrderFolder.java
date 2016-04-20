///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2016 Safety Data -CFH
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
package com.joliciel.talismane.regex.parser;

import java.util.Stack;

/**
 * A tree post-order folder computing some value for each {@link Node} in the
 * tree.
 * 
 * @author Lucas Satabin
 *
 */
public abstract class PostOrderFolder {

	/** Walk through the tree, computing result as it goes. */
	public void fold(Node node) {

		Stack<Node> nodes = new Stack<>();

		// call the action on the nodes in post-order traversal
		do {
			while (node != null) {
				if (node.right != null) {
					nodes.push(node.right);
				}
				nodes.push(node);
				node = node.left;
			}

			node = nodes.pop();

			if (node.right != null && !nodes.isEmpty() && node.right == nodes.peek()) {
				Node right = nodes.pop();
				nodes.push(node);
				node = right;
			} else {
				doNode(node);
				node = null;
			}
		} while (!nodes.isEmpty());

	}

	/**
	 * Process a node. As this folder walks through the tree in a post-order
	 * manner, children have already been processed when the current node is
	 * processed. Implementers must keep the state and context in the sub-class,
	 * modifying it for each node.
	 */
	protected abstract void doNode(Node n);

}
