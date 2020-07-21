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
package com.joliciel.talismane.extensions.corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.parser.DependencyArc;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.parser.output.ParseConfigurationProcessor;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * Transforms a non-projective corpus to a projective corpus by attaching
 * non-projective arcs to a projective head.<br/>
 * Does not write any output directly - this needs to be taken care of by
 * another ParseConfigurationProcessor.<br/>
 * <br/>
 * The strategy for selecting the projective head is as follows:<br/>
 * <ul>
 * <li>A non-projective relationship involves two arcs which cross each other.
 * Let D<sub>1</sub> and D<sub>2</sub> be the dependents of these arcs.</li>
 * <li>We attempt to attach each dependent to its governor's governor. If the
 * non-projectivity is not resolved, we move on to the governor's governor's
 * governor, and so on, until either the non-projectivity has been resolved, or
 * we have reached the root.</li>
 * <li>We now potentially have two projectivised heads, H<sub>1</sub> and H
 * <sub>2</sub>.</li>
 * <li>If we have both heads, we select the one based on the
 * {@link ProjectivationStrategy}.</li>
 * </ul>
 * 
 * @author Assaf Urieli
 *
 */
public class CorpusProjectifier implements ParseConfigurationProcessor {
  /**
   * Strategy for selecting among two potential projective heads.
   * 
   * @author Assaf Urieli
   *
   */
  public enum ProjectivationStrategy {
    /**
     * Select the projective head which is closest to its dependent in terms of
     * linear distance. If both are at an equal distance, change to next
     * strategy.
     */
    LeastLinearDistance,
    /**
     * Select the projective head which is closest to its dependent, in terms of
     * depth. If both are at an equal distance, change to next strategy.
     */
    LeastDepthDifference,
    /**
     * Select the project head which is farthest from the root, in terms of
     * depth.
     */
    GreatestDepth,
  }

  private final String nonProjectiveArcSuffix;
  private final ProjectivationStrategy strategy;

  public CorpusProjectifier(String sessionId) throws IOException {
    Config config = ConfigFactory.load();
    nonProjectiveArcSuffix = config.getString("talismane.extensions." + sessionId + ".projectifier.non-projective-arc-suffix");
    strategy = ProjectivationStrategy.valueOf(config.getString("talismane.extensions." + sessionId + ".projectifier.strategy"));
  }

  @Override
  public void onNextParseConfiguration(ParseConfiguration parseConfiguration) throws TalismaneException {

    List<DependencyArc> arcs = new ArrayList<DependencyArc>(parseConfiguration.getNonProjectiveDependencies());

    NonProjectivePair pair = this.getNextPair(arcs);
    if (pair != null) {
      // have non projective dependencies - set up initial non-projective
      // set so that it stays untouched
      for (DependencyArc arc : arcs) {
        parseConfiguration.addManualNonProjectiveDependency(arc.getHead(), arc.getDependent(), arc.getLabel());
      }
    }
    while (pair != null) {
      PosTaggedToken newHead1 = null;
      PosTaggedToken parent1 = parseConfiguration.getHead(pair.arc1.getHead());
      int depIndex1 = pair.arc1.getDependent().getToken().getIndex();
      int depthDelta1 = 1;
      while (parent1 != null) {
        int headIndex = parent1.getToken().getIndex();
        int startIndex = headIndex < depIndex1 ? headIndex : depIndex1;
        int endIndex = headIndex >= depIndex1 ? headIndex : depIndex1;
        if (isProjective(startIndex, endIndex, pair.arc2)) {
          newHead1 = parent1;
          break;
        }
        parent1 = parseConfiguration.getHead(parent1);
        depthDelta1++;
      }
      PosTaggedToken newHead2 = null;
      PosTaggedToken parent2 = parseConfiguration.getHead(pair.arc2.getHead());
      int depIndex2 = pair.arc2.getDependent().getToken().getIndex();
      int depthDelta2 = 1;
      while (parent2 != null) {
        int headIndex = parent2.getToken().getIndex();
        int startIndex = headIndex < depIndex2 ? headIndex : depIndex2;
        int endIndex = headIndex >= depIndex2 ? headIndex : depIndex2;
        if (isProjective(startIndex, endIndex, pair.arc2)) {
          newHead2 = parent2;
          break;
        }
        parent2 = parseConfiguration.getHead(parent2);
        depthDelta2++;
      }
      if (newHead1 != null && newHead2 != null) {
        int linearDistance1 = Math.abs(newHead1.getIndex() - depIndex1);
        int linearDistance2 = Math.abs(newHead2.getIndex() - depIndex2);
        int rootDepthDelta1 = 0;
        PosTaggedToken parent = parseConfiguration.getHead(newHead1);
        while (parent != null) {
          rootDepthDelta1++;
          parent = parseConfiguration.getHead(parent);
        }
        int rootDepthDelta2 = 0;
        parent = parseConfiguration.getHead(newHead2);
        while (parent != null) {
          rootDepthDelta2++;
          parent = parseConfiguration.getHead(parent);
        }
        switch (strategy) {
        case LeastLinearDistance:
          if (linearDistance1 < linearDistance2) {
            newHead2 = null;
            break;
          } else if (linearDistance2 < linearDistance1) {
            newHead1 = null;
            break;
          }
          // break left out on purpose
        case LeastDepthDifference:
          if (depthDelta1 < depthDelta2) {
            newHead2 = null;
            break;
          } else if (depthDelta2 < depthDelta1) {
            newHead1 = null;
            break;
          }
          // break left out on purpose
        case GreatestDepth:
          if (rootDepthDelta1 < rootDepthDelta2) {
            newHead1 = null;
            break;
          } else {
            newHead2 = null;
            break;
          }
        }
      }
      if (newHead1 != null && newHead2 == null) {
        parseConfiguration.removeDependency(pair.arc1);
        String newLabel = pair.arc1.getLabel();
        if (this.nonProjectiveArcSuffix.length() > 0 && !newLabel.endsWith(this.nonProjectiveArcSuffix))
          newLabel += this.nonProjectiveArcSuffix;
        parseConfiguration.addDependency(newHead1, pair.arc1.getDependent(), newLabel, null);

        // for the other arc, copy the non-projective version, in case
        // there is an attempt at manual projectivisation
        DependencyArc otherProjArc = parseConfiguration.getGoverningDependency(pair.arc2.getDependent());
        parseConfiguration.removeDependency(otherProjArc);
        parseConfiguration.addDependency(pair.arc2.getHead(), pair.arc2.getDependent(), pair.arc2.getLabel(), null);

      } else if (newHead1 == null && newHead2 != null) {
        parseConfiguration.removeDependency(pair.arc2);
        String newLabel = pair.arc2.getLabel();
        if (this.nonProjectiveArcSuffix.length() > 0 && !newLabel.endsWith(this.nonProjectiveArcSuffix))
          newLabel += this.nonProjectiveArcSuffix;
        parseConfiguration.addDependency(newHead2, pair.arc2.getDependent(), newLabel, null);

        // for the other arc, copy the non-projective version, in case
        // there is an attempt at manual projectivisation
        DependencyArc otherProjArc = parseConfiguration.getGoverningDependency(pair.arc1.getDependent());
        parseConfiguration.removeDependency(otherProjArc);
        parseConfiguration.addDependency(pair.arc1.getHead(), pair.arc1.getDependent(), pair.arc1.getLabel(), null);

      } else {
        throw new TalismaneException("Cannot deprojectify " + pair + ". Could not find projective parents.");
      }
      parseConfiguration.clearMemory();

      arcs = new ArrayList<DependencyArc>(parseConfiguration.getDependencies());
      pair = this.getNextPair(arcs);
    }
  }

  private NonProjectivePair getNextPair(List<DependencyArc> arcs) {
    NonProjectivePair pair = null;
    DependencyArc arc = null;
    DependencyArc otherArc = null;
    for (int i = 0; i < arcs.size(); i++) {
      arc = arcs.get(i);
      if (arc.getHead().getTag().equals(PosTag.ROOT_POS_TAG) && (arc.getLabel() == null || arc.getLabel().length() == 0))
        continue;
      int headIndex = arc.getHead().getToken().getIndex();
      int depIndex = arc.getDependent().getToken().getIndex();
      int startIndex = headIndex < depIndex ? headIndex : depIndex;
      int endIndex = headIndex >= depIndex ? headIndex : depIndex;

      for (int j = i + 1; j < arcs.size(); j++) {
        otherArc = arcs.get(j);
        if (otherArc.getHead().getTag().equals(PosTag.ROOT_POS_TAG) && (otherArc.getLabel() == null || otherArc.getLabel().length() == 0))
          continue;
        if (!isProjective(startIndex, endIndex, otherArc)) {
          pair = new NonProjectivePair(arc, otherArc);
          break;
        }

      }
      if (pair != null)
        break;
    }
    return pair;
  }

  boolean isProjective(int startIndex, int endIndex, DependencyArc otherArc) {
    boolean projective = true;

    int headIndex2 = otherArc.getHead().getToken().getIndex();
    int depIndex2 = otherArc.getDependent().getToken().getIndex();
    int startIndex2 = headIndex2 < depIndex2 ? headIndex2 : depIndex2;
    int endIndex2 = headIndex2 >= depIndex2 ? headIndex2 : depIndex2;
    if (startIndex2 < startIndex && endIndex2 > startIndex && endIndex2 < endIndex) {
      projective = false;
    } else if (startIndex2 > startIndex && startIndex2 < endIndex && endIndex2 > endIndex) {
      projective = false;
    }
    return projective;
  }

  @Override
  public void onCompleteParse() {
  }

  private static final class NonProjectivePair {
    DependencyArc arc1;
    DependencyArc arc2;

    public NonProjectivePair(DependencyArc arc1, DependencyArc arc2) {
      super();
      this.arc1 = arc1;
      this.arc2 = arc2;
    }

    @Override
    public String toString() {
      return "NonProjectivePair [arc1=" + arc1 + ", arc2=" + arc2 + "]";
    }

  }

  @Override
  public void close() throws IOException {
  }
}
