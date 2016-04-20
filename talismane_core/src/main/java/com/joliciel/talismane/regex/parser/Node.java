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

/**
 * A regular expression node.
 * 
 * @author Lucas Satabin
 *
 */
public abstract class Node {

	public final NodeType type;
	public final Node left;
	public final Node right;

	public Node(NodeType type, Node left, Node right) {
		this.type = type;
		this.left = left;
		this.right = right;
	}

	public Node(NodeType type, Node left) {
		this.type = type;
		this.left = left;
		this.right = null;
	}

	public Node(NodeType type) {
		this.type = type;
		this.left = null;
		this.right = null;
	}

	public boolean isLeaf() {
		return left == null && right == null;
	}

}
