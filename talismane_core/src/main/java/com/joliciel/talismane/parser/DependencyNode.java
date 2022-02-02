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

import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.posTagger.PosTaggedTokenLeftToRightComparator;

/**
 * A partial sub-tree of a syntax analysis, which might not contain all of the
 * dependents of the parent node.<br>
 * 
 * It is is constructed little by little, adding dependents using the
 * {@link #addDependent(DependencyNode)} or
 * {@link #addDependent(PosTaggedToken)} methods - typical usage would be the
 * construction of noun phrases for terminology extraction.<br>
 * 
 * @author Assaf Urieli
 *
 */
public class DependencyNode implements Comparable<DependencyNode> {
  private static final Logger LOG = LoggerFactory.getLogger(DependencyNode.class);
  private final PosTaggedToken token;
  private final String label;
  private DependencyNode parent;
  private final Set<DependencyNode> dependents = new TreeSet<>();
  private final ParseConfiguration parseConfiguration;
  private Set<PosTaggedToken> yield = null;

  private String string = null;

  DependencyNode(PosTaggedToken token, String label, ParseConfiguration parseConfiguration) {
    this.token = token;
    this.label = label;
    this.parseConfiguration = parseConfiguration;
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
  public DependencyNode getParent() {
    return parent;
  }

  public void setParent(DependencyNode parent) {
    this.parent = parent;
  }

  /**
   * This node's dependents.
   */
  public Set<DependencyNode> getDependents() {
    return dependents;
  }

  /**
   * Add an existing dependent of the current node's token to the current
   * dependency node. The dependent must already exist in the parse
   * configuration. This is useful for constructing sub-trees out of the
   * existing parse tree.
   * 
   * @throws TalismaneException
   *           if this dependent does not reflect the parse configuration
   */
  public DependencyNode addDependent(PosTaggedToken dependent) throws TalismaneException {
    DependencyArc arc = parseConfiguration.getGoverningDependency(dependent);
    if (arc == null) {
      throw new TalismaneException("Can only add a dependent to a dependency node if it is a true dependent in the parse configuration.");
    }
    DependencyNode node = new DependencyNode(dependent, arc.getLabel(), this.parseConfiguration);
    this.getDependents().add(node);
    node.setParent(this);
    this.setDirty();
    return node;
  }

  /**
   * Add a dependent in the form of an existing dependency node.
   */
  public void addDependent(DependencyNode dependent) {
    this.getDependents().add(dependent);
    this.setDirty();
  }

  /**
   * Populate this node's dependents directly from the parse configuration.
   */
  public void autoPopulate() {
    for (PosTaggedToken dependent : parseConfiguration.getDependents(this.token)) {
      DependencyNode childNode;
      try {
        childNode = this.addDependent(dependent);
      } catch (TalismaneException e) {
        // should never happen
        LOG.error(e.getMessage(), e);
        throw new RuntimeException(e);
      }
      childNode.autoPopulate();
    }
  }

  @Override
  public int compareTo(DependencyNode o) {
    return this.getPosTaggedToken().compareTo(o.getPosTaggedToken());
  }

  /**
   * The parse configuration from which this node was derived.
   */
  public ParseConfiguration getParseConfiguration() {
    return parseConfiguration;
  }

  /**
   * Clone the current dependency node.
   */
  public DependencyNode cloneNode() {
    DependencyNode node = new DependencyNode(this.token, this.label, this.parseConfiguration);
    for (DependencyNode dependent : this.dependents) {
      DependencyNode clone = dependent.cloneNode();
      node.addDependent(clone);
    }
    return node;
  }

  @Override
  public int hashCode() {
    return this.toString().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return this.toString().equals(obj.toString());
  }

  @Override
  public String toString() {
    if (string == null) {
      string = this.getPosTaggedToken().getToken().getOriginalText();

      boolean firstDependent = true;
      if (this.dependents.size() > 0) {
        string += "(";
        for (DependencyNode dependent : this.dependents) {
          if (firstDependent) {
            firstDependent = false;
          } else {
            string += ", ";
          }
          string += dependent.toString();
        }
        string += ")";
      }
    }
    return string;
  }

  /**
   * Return the depth of this tree starting at its head (1).
   */
  public int getDepth() {
    int depth = 1;
    int maxDepth = 0;
    for (DependencyNode dependent : this.getDependents()) {
      int dependentDepth = dependent.getDepth();
      if (dependentDepth > maxDepth) {
        maxDepth = dependentDepth;
      }
    }
    depth += maxDepth;
    return depth;
  }

  /**
   * Language-specific depth as a typical user would expect to see it.<br>
   * In French, for example, a preposition and its object would thus be a single
   * layer of depth, equivalent to a single adjective when modifying a noun. A
   * determinent would not add to its governor's depth.
   * 
   * @param zeroDepthLabels
   *          labels which shouldn't be counted for depth calculation
   */
  public int getPerceivedDepth(Set<String> zeroDepthLabels) {
    int depth = 1;

    if (zeroDepthLabels.contains(this.getLabel()))
      depth = 0;
    int maxDepth = 0;
    for (DependencyNode dependent : this.getDependents()) {
      int dependentDepth = dependent.getPerceivedDepth(zeroDepthLabels);
      if (dependentDepth > maxDepth) {
        maxDepth = dependentDepth;
      }
    }
    depth += maxDepth;
    return depth;
  }

  /**
   * Removes a node (and all of its children) from this dependency node's tree,
   * wherever it may be located.
   * 
   * @return true if the node was removed.
   */
  public boolean removeNode(DependencyNode node) {
    if (this.getDependents().contains(node)) {
      this.getDependents().remove(node);
      this.setDirty();
      return true;
    }
    for (DependencyNode dependent : this.getDependents()) {
      boolean removed = dependent.removeNode(node);
      if (removed)
        return true;
    }
    return false;
  }

  /**
   * The first token comprising this dependency node, if viewed in linear order
   * within a sentence.
   */
  public PosTaggedToken getFirstToken() {
    PosTaggedToken firstToken = this.getPosTaggedToken();
    for (DependencyNode dependent : this.getDependents()) {
      PosTaggedToken firstDepToken = dependent.getFirstToken();
      if (firstDepToken.getToken().getStartIndex() < firstToken.getToken().getStartIndex()) {
        firstToken = firstDepToken;
      }
    }
    return firstToken;
  }

  /**
   * The last token comprising this dependency node, if viewed in linear order
   * within a sentence.
   */
  public PosTaggedToken getLastToken() {
    PosTaggedToken lastToken = this.getPosTaggedToken();
    for (DependencyNode dependent : this.getDependents()) {
      PosTaggedToken lastDepToken = dependent.getLastToken();
      if (lastDepToken.getToken().getEndIndex() > lastToken.getToken().getEndIndex()) {
        lastToken = lastDepToken;
      }
    }
    return lastToken;
  }

  /**
   * Return true if this node is contiguous, false if it contains gaps.
   */
  public boolean isContiguous() {
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
   * The current node's yield, an ordered set of the current node and all of its
   * descendants, that have actually been added.
   * 
   * @return
   */
  public Set<PosTaggedToken> getYield() {
    if (yield == null) {
      yield = new TreeSet<PosTaggedToken>(new PosTaggedTokenLeftToRightComparator());
      this.getAllNodes(yield);
    }
    return yield;
  }

  /**
   * @deprecated use {@link #getYield()} instead
   * @param posTaggedTokens
   */
  @Deprecated
  public void getAllNodes(Set<PosTaggedToken> posTaggedTokens) {
    posTaggedTokens.add(this.token);
    for (DependencyNode dependent : this.getDependents()) {
      dependent.getAllNodes(posTaggedTokens);
    }
  }

  void setDirty() {
    this.string = null;
    this.yield = null;
    if (this.parent != null) {
      this.parent.setDirty();
    }
  }

}
