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

import com.joliciel.talismane.machineLearning.Decision;

/**
 * A single transition in a transition-based parsing system.
 * 
 * @author Assaf Urieli
 *
 */
public interface Transition extends Comparable<Transition> {
  /**
   * Check whether this transition is valid for the configuration provided.
   */
  public boolean checkPreconditions(ParseConfiguration configuration);

  /**
   * Apply the transition to the configuration provided.
   * 
   * @throws InvalidTransitionException
   *           if transition cannot be applied in current conditions
   * @throws CircularDependencyException
   *           if this would generate a ciruclar dependency
   */
  public void apply(ParseConfiguration configuration) throws InvalidTransitionException, CircularDependencyException;

  /**
   * The unique code for this transition.
   */
  public String getCode();

  /**
   * Returns true if this transition reduces the elements left to process, by
   * removing an element permanently from either the stack or the buffer.
   */
  public boolean doesReduce();

  /**
   * The decision which generated this transition.
   */
  public Decision getDecision();

  public void setDecision(Decision decision);
}
