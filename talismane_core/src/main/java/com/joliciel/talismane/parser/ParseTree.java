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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.joliciel.talismane.posTagger.PosTaggedToken;

/**
 * A parse tree, giving a tree representation of a syntactic analysis, rooted at
 * the artificial root.
 * 
 * @author Assaf Urieli
 *
 */
public class ParseTree {
  private final List<PosTaggedToken> posTaggedTokens;
  private final Map<PosTaggedToken, ParseTreeNode> parseTreeMap;
  private final Map<PosTaggedToken, List<PosTaggedToken>> dependentMap;
  private final Map<PosTaggedToken, DependencyArc> governingDependencyMap;
  private final ParseTreeNode root;
  private List<Pair<ParseTreeNode, ParseTreeNode>> illNestedNodes = null;
  private List<ParseTreeNode> nonProjectiveNodes = null;
  private List<DependencyArc> nonProjectiveEdges = null;

  /**
   * Create a parse tree out of a given parse configuration.
   * 
   * @param parseConfiguration
   *          the base configuration
   * @param projective
   *          whether we should use the projective arcs (true) or the
   *          potentially non-projective arcs (false)
   */
  public ParseTree(ParseConfiguration parseConfiguration, boolean projective) {
    this.posTaggedTokens = parseConfiguration.getPosTagSequence();
    this.parseTreeMap = new HashMap<>();
    this.dependentMap = new HashMap<>();
    this.governingDependencyMap = new HashMap<>();

    Set<DependencyArc> arcs = parseConfiguration.getDependencies();
    if (!projective)
      arcs = parseConfiguration.getNonProjectiveDependencies();

    for (DependencyArc arc : arcs) {
      this.governingDependencyMap.put(arc.getDependent(), arc);
    }

    for (PosTaggedToken token : this.posTaggedTokens) {
      List<PosTaggedToken> dependents = new ArrayList<>();
      this.dependentMap.put(token, dependents);
    }

    for (PosTaggedToken token : this.posTaggedTokens) {
      DependencyArc arc = this.governingDependencyMap.get(token);
      if (arc != null) {
        List<PosTaggedToken> dependents = this.dependentMap.get(arc.getHead());
        dependents.add(token);
      }
    }

    root = new ParseTreeNode(this);

    this.populateMap(root);
  }

  private void populateMap(ParseTreeNode node) {
    this.parseTreeMap.put(node.getPosTaggedToken(), node);
    for (ParseTreeNode child : node.getChildren()) {
      this.populateMap(child);
    }
  }

  /**
   * This parse tree's root.
   */
  public ParseTreeNode getRoot() {
    return root;
  }

  /**
   * Retrieve the node corresponding to a given pos-tagged token.
   */
  public ParseTreeNode getNode(PosTaggedToken token) {
    return parseTreeMap.get(token);
  }

  /**
   * The sequence of pos-tagged tokens contained in this tree, preceded by an
   * artificial root.
   */
  public List<PosTaggedToken> getPosTaggedTokens() {
    return posTaggedTokens;
  }

  /**
   * Return all of the dependents of a given pos-tagged token.
   */
  public List<PosTaggedToken> getDependents(PosTaggedToken token) {
    return dependentMap.get(token);
  }

  /**
   * Get the dependency arc governing a given pos-tagged token.
   */
  public DependencyArc getGoverningArc(PosTaggedToken token) {
    return this.governingDependencyMap.get(token);
  }

  /**
   * Non-projectivity: returns the node with the maximum gap count in this tree,
   * where gap count is defined in {@link ParseTreeNode#getGapCount()}.
   */
  public Pair<ParseTreeNode, Integer> getGapDegree() {
    return this.getRoot().getGapDegree();
  }

  /**
   * Non-projectivity: returns the node with the maximum edge count in this
   * sub-tree, where edge count is defined in
   * {@link ParseTreeNode#getEdgeCount()}.
   */
  public Pair<ParseTreeNode, Integer> getEdgeDegree() {
    return this.getRoot().getEdgeDegree();
  }

  /**
   * Non-projectivity: are all nodes in this tree projective?
   * 
   * @return
   */
  public boolean isProjective() {
    return this.getNonProjectiveNodes().isEmpty();
  }

  /**
   * Non-projectivity: is this tree well nested?<br/>
   * Two sub-trees are called disjoint if neither of their heads dominates the
   * other.<br/>
   * They interleave if there exist leaves l1, r1 ∈ T1 and l2, r2 ∈ T2, such
   * that l1 &lt; l2 &lt; r1 &lt; r2.<br/>
   * A tree is well nested if none of its disjoint subtrees interleave.
   */
  public boolean isWellNested() {
    return this.getIllNestedNodes().isEmpty();
  }

  /**
   * A list of ill-nested node pairs, see {@link #isWellNested()}.
   */
  public List<Pair<ParseTreeNode, ParseTreeNode>> getIllNestedNodes() {
    if (illNestedNodes != null)
      return illNestedNodes;
    illNestedNodes = new ArrayList<>();
    List<ParseTreeNode> nonProjectiveNodes = this.getNonProjectiveNodes();
    for (int i = 0; i < nonProjectiveNodes.size() - 1; i++) {
      ParseTreeNode node1 = nonProjectiveNodes.get(i);
      for (int j = i + 1; j < nonProjectiveNodes.size(); j++) {
        ParseTreeNode node2 = nonProjectiveNodes.get(j);
        boolean disjoint = !(node1.getYield().contains(node2.getPosTaggedToken()) || node2.getYield().contains(node1.getPosTaggedToken()));
        if (disjoint) {
          int l1 = node1.getYield().first().getIndex();
          int r1 = node1.getYield().last().getIndex();
          int l2 = node2.getYield().first().getIndex();
          int r2 = node2.getYield().last().getIndex();

          if (l1 <= r2 && l2 <= r1) {
            illNestedNodes.add(new ImmutablePair<ParseTreeNode, ParseTreeNode>(node1, node2));
          }
        } // disjoint ?
      } // next test node
    } // next non-projective node

    return illNestedNodes;
  }

  /**
   * Return all non-projective nodes in this tree.
   */
  public List<ParseTreeNode> getNonProjectiveNodes() {
    if (nonProjectiveNodes != null)
      return nonProjectiveNodes;
    nonProjectiveNodes = new ArrayList<>();
    this.root.getNonProjectiveNodes(nonProjectiveNodes);
    return nonProjectiveNodes;
  }

  /**
   * Return all non-projective edges in this tree, where a non-projective edge
   * is one where node <i>i</i> governs node <i>j</i>, and there is some node
   * <i>k</i> where i&lt;k&lt;j such that node <i>i</i> does not dominate node
   * <i>k</i>.
   */
  public List<DependencyArc> getNonProjectiveEdges() {
    if (nonProjectiveEdges != null)
      return nonProjectiveEdges;
    nonProjectiveEdges = new ArrayList<>();
    this.root.getNonProjectiveEdges(nonProjectiveEdges);
    return nonProjectiveEdges;
  }

  @Override
  public String toString() {
    return this.root.toString();
  }
}
