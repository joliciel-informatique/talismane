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

import com.joliciel.talismane.TalismaneException;

/**
 * Thrown when the transition does not exist in the current transition system.
 * 
 * @author Assaf Urieli
 *
 */
public class UnknownTransitionException extends TalismaneException {
  private static final long serialVersionUID = 1L;
  private String transition = "";
  private int index;

  public UnknownTransitionException(int index, String transition) {
    super("Unknown transition: " + transition + " on index " + index);
    this.transition = transition;
    this.index = index;
  }

  public UnknownTransitionException(String transition) {
    super("Unknown transition: " + transition);
    this.transition = transition;
  }

  public String getTransition() {
    return transition;
  }

  public int getIndex() {
    return index;
  }

}
