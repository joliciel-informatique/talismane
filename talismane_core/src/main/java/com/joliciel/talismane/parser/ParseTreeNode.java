///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2017 Joliciel Informatique
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.posTagger.PosTaggedTokenLeftToRightComparator;

/**
 * A node inside a parse tree.
 * 
 * @author Assaf Urieli
 *
 */
public class ParseTreeNode implements Comparable<ParseTreeNode> {
  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(ParseTreeNode.class);
  private final ParseTree parseTree;
  private final PosTaggedToken token;
  private final String label;
  private final ParseTreeNode parent;
  private final Set<ParseTreeNode> children;
  private NavigableSet<PosTaggedToken> yield = null;
  private final int depth;

  private String string = null;

  /**
   * Create the root node of a parse configuration and populate all of its
   * dependents.
   */
  ParseTreeNode(ParseTree parseTree) {
    this.parseTree = parseTree;
    // set the token to the root token
    PosTaggedToken root = null;
    for (PosTaggedToken token : parseTree.getPosTaggedTokens()) {
      if (token.getTag().equals(PosTag.ROOT_POS_TAG)) {
        root = token;
        break;
      }
    }
    if (root == null) {
      throw new RuntimeException("No root found in parse tree");
    }
    this.token = root;
    this.label = "";
    this.parent = null;
    this.depth = 0;
    this.children = new TreeSet<>();
    for (PosTaggedToken child : parseTree.getDependents(this.token)) {
      ParseTreeNode childNode = new ParseTreeNode(child, this);
      this.children.add(childNode);
    }
  }

  private ParseTreeNode(PosTaggedToken token, ParseTreeNode parent) {
    this.parseTree = parent.parseTree;
    this.token = token;
    DependencyArc arc = parseTree.getGoverningDependency(token);
    if (arc != null)
      this.label = arc.getLabel();
    else
      this.label = "";

    this.parent = parent;

    this.depth = parent.depth + 1;
    this.children = new TreeSet<>();
    for (PosTaggedToken child : parseTree.getDependents(this.token)) {
      ParseTreeNode childNode = new ParseTreeNode(child, this);
      this.children.add(childNode);
    }
  }

  /**
   * The actual pos-tagged token in this node.
   */
  public PosTaggedToken getPosTaggedToken() {
    return token;
  }

  /**
   * The dependency label tying this node to its parent.
   */
  public String getLabel() {
    return label;
  }

  /**
   * This node's parent.
   */
  public ParseTreeNode getParent() {
    return parent;
  }

  /**
   * This node's children.
   */
  public Set<ParseTreeNode> getChildren() {
    return children;
  }

  /**
   * The node's depth with respect to the root, where the root's depth is
   * considered 0.
   */
  public int getDepth() {
    return depth;
  }

  @Override
  public int compareTo(ParseTreeNode o) {
    return this.getPosTaggedToken().compareTo(o.getPosTaggedToken());
  }

  @Override
  public String toString() {
    if (string == null) {
      StringBuilder sb = new StringBuilder();
      this.toString(sb);
      this.string = sb.toString();
    }
    return string;
  }

  /**
   * The index of this node's token.
   */
  public int getIndex() {
    return this.getPosTaggedToken().getIndex();
  }

  private void toString(StringBuilder sb) {
    sb.append("(");

    for (ParseTreeNode child : this.children)
      if (child.getIndex() < this.getIndex())
        child.toString(sb);

    sb.append(this.getPosTaggedToken().getToken().getOriginalText() + "|" + this.getIndex() + " ");

    for (ParseTreeNode child : this.children)
      if (child.getIndex() > this.getIndex())
        child.toString(sb);

    sb.append(")");
  }

  /**
   * Is this node's yield projective - meaning is it a continuous interval with
   * no gaps.
   */
  public boolean isProjective() {
    Set<PosTaggedToken> yield = this.getYield();
    int currentIndex = -1;
    boolean contiguous = true;
    for (PosTaggedToken token : yield) {
      if (currentIndex < 0) {
        currentIndex = token.getIndex();
      } else if (token.getIndex() == currentIndex + 1) {
        currentIndex++;
      } else {
        contiguous = false;
        break;
      }
    }

    return contiguous;
  }

  /**
   * Collect all non-projective sub-nodes for this node, including this node if
   * non-projective.
   */
  public void getNonProjectiveNodes(List<ParseTreeNode> nonProjectiveNodes) {
    if (!this.isProjective())
      nonProjectiveNodes.add(this);
    for (ParseTreeNode child : this.getChildren()) {
      child.getNonProjectiveNodes(nonProjectiveNodes);
    }
  }

  /**
   * The current node's yield, an ordered set of the current node and all of its
   * descendants.
   * 
   * @return
   */
  public NavigableSet<PosTaggedToken> getYield() {
    if (yield == null) {
      yield = new TreeSet<PosTaggedToken>(new PosTaggedTokenLeftToRightComparator());
      this.getAllNodes(yield);
    }
    return yield;
  }

  private void getAllNodes(Set<PosTaggedToken> posTaggedTokens) {
    posTaggedTokens.add(this.token);
    for (ParseTreeNode dependent : this.getChildren()) {
      dependent.getAllNodes(posTaggedTokens);
    }
  }

  /**
   * Non-projectivity: gives the number of "gaps" in the node's yield. Each gap
   * is defined as a series of one or more tokens which are not members of the
   * node's yield, but which are contained between two tokens which are members
   * of the node's yield.
   */
  public int getGapCount() {
    Set<PosTaggedToken> yield = this.getYield();
    int currentIndex = -1;
    int gapCount = 0;
    for (PosTaggedToken token : yield) {
      if (currentIndex < 0) {
        // do nothing: first token
      } else if (token.getIndex() == currentIndex + 1) {
        // do nothing: not a gap
      } else {
        gapCount++;
      }
      currentIndex = token.getIndex();
    }

    return gapCount;
  }

  /**
   * Non-projectivity: returns the node with the maximum gap count in this
   * sub-tree.
   */
  public Pair<ParseTreeNode, Integer> getGapDegree() {
    int maxGapCount = this.getGapCount();
    Pair<ParseTreeNode, Integer> gapDegree = null;
    for (ParseTreeNode childNode : this.children) {
      Pair<ParseTreeNode, Integer> childGapDegree = childNode.getGapDegree();
      if (childGapDegree.getRight() > maxGapCount) {
        maxGapCount = childGapDegree.getRight();
        gapDegree = childGapDegree;
      }
    }
    if (gapDegree == null) {
      gapDegree = new ImmutablePair<ParseTreeNode, Integer>(this, maxGapCount);
    }
    return gapDegree;
  }

  /**
   * Non-projectivity: returns the number of connected components contained in
   * the interval defined by the start and end of this node's yield, which are
   * not themselves dominated by this node, i.e. not themselves contained in the
   * yield. A connected component is a connected subtree.
   */
  public int getEdgeCount() {
    NavigableSet<PosTaggedToken> yield = this.getYield();
    int currentIndex = -1;
    int edgeCount = 0;
    for (PosTaggedToken token : yield) {
      if (currentIndex < 0) {
        // do nothing: first token
      } else if (token.getIndex() == currentIndex + 1) {
        // do nothing: not a gap
      } else {
        List<ParseTreeNode> connectedComponents = this.getConnectedComponents(currentIndex, token.getIndex());
        edgeCount += connectedComponents.size();
      }
      currentIndex = token.getIndex();
    }

    return edgeCount;
  }

  /**
   * Non-projectivity: returns the node with the maximum edge count in this
   * sub-tree.
   */
  public Pair<ParseTreeNode, Integer> getEdgeDegree() {
    int maxEdgeCount = this.getEdgeCount();
    Pair<ParseTreeNode, Integer> edgeDegree = null;
    for (ParseTreeNode childNode : this.children) {
      Pair<ParseTreeNode, Integer> childGapDegree = childNode.getEdgeDegree();
      if (childGapDegree.getRight() > maxEdgeCount) {
        maxEdgeCount = childGapDegree.getRight();
        edgeDegree = childGapDegree;
      }
    }
    if (edgeDegree == null) {
      edgeDegree = new ImmutablePair<ParseTreeNode, Integer>(this, maxEdgeCount);
    }
    return edgeDegree;
  }

  /**
   * Non-projectivity: returns a list of connected components found between the
   * tokens at indexes start and end, not including start and end.
   */
  private List<ParseTreeNode> getConnectedComponents(int start, int end) {
    if (end - start <= 1)
      return Collections.emptyList();

    List<ParseTreeNode> nodes = new ArrayList<>();
    for (int i = start + 1; i < end; i++) {
      PosTaggedToken token = parseTree.getPosTaggedTokens().get(i);
      boolean foundParent = false;
      for (ParseTreeNode parentNode : nodes) {
        if (parentNode.getYield().contains(token)) {
          foundParent = true;
          break;
        }
      }
      if (!foundParent) {
        ParseTreeNode node = parseTree.getNode(token);
        nodes.add(node);
      }
    }

    return nodes;
  }
}
