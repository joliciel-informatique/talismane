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

import com.joliciel.talismane.machineLearning.MachineLearningModel;

/**
 * A set of transitions that can be applied to a configuration to give a
 * particular set of target dependencies.
 * 
 * @author Assaf
 *
 */
public interface TransitionSystem {
  /**
   * Predict the transitions required to generate the set of targe
   * dependencies for a given initial configuration, also transforms the
   * configuration so that it becomes a terminal configuration.
   * 
   * @throws UnknownDependencyLabelException
   *             if an unknown dependency label is encountered
   * @throws NonPredictableParseTreeException
   *             if its impossible to predict the current parse tree using
   *             this transition system.
   * @throws CircularDependencyException
   *             if parse tree contains a circular dependency
   */
  void predictTransitions(ParseConfiguration configuration, Set<DependencyArc> targetDependencies)
      throws UnknownDependencyLabelException, NonPredictableParseTreeException, CircularDependencyException;

  /**
   * Get the transition corresponding to a particular code.
   * 
   * @throws UnknownDependencyLabelException
   *             if the code includes an unknown dependency label
   * @throws UnknownTransitionException
   *             if the code includes an unknown transition
   */
  public Transition getTransitionForCode(String code) throws UnknownDependencyLabelException, UnknownTransitionException;

  /**
   * Get all possible transitions for this system.
   */
  public Set<Transition> getTransitions();

  /**
   * A set of dependency labels for this transition system.
   */
  public DependencyLabelSet getDependencyLabelSet();

  public void setDependencyLabelSet(DependencyLabelSet dependencyLabelSet);

  /**
   * The dependency labels allowed by the {@link DependencyLabelSet}, or an
   * empty set if no dependency label set has been set.
   * 
   * @return
   */
  public Set<String> getDependencyLabels();

  /**
   * Get the transition system corresponding to the model provided.
   */
  public static TransitionSystem getTransitionSystem(MachineLearningModel model) {
    TransitionSystem transitionSystem = null;
    String transitionSystemClassName = (String) model.getModelAttributes().get("transitionSystem");
    if (ShiftReduceTransitionSystem.class.getSimpleName().equalsIgnoreCase(transitionSystemClassName)) {
      transitionSystem = new ShiftReduceTransitionSystem();
    } else if (ArcEagerTransitionSystem.class.getSimpleName().equalsIgnoreCase(transitionSystemClassName)) {
      transitionSystem = new ArcEagerTransitionSystem();
    } else {
      throw new RuntimeException("Unknown transition system: " + transitionSystemClassName);
    }
    return transitionSystem;
  }
}
