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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Special transition for ungoverned punctuation, so as not to consider it
 * explicitly as depending on root, and not to create non-projectivity. Reduce
 * the stack, by popping Stack[0], without creating a new dependency, but with
 * no preconditions on existing governors.
 * 
 * @author Assaf Urieli.
 *
 */
public class ForceReduceTransition extends AbstractTransition implements Transition {
  private static final Logger LOG = LoggerFactory.getLogger(ForceReduceTransition.class);
  private static String name = "ForceReduce";

  public ForceReduceTransition() {
    super();
  }

  @Override
  protected void applyInternal(ParseConfiguration configuration) {
    configuration.getStack().pop();
  }

  @Override
  public boolean checkPreconditions(ParseConfiguration configuration) {
    if (configuration.getStack().isEmpty()) {
      if (LOG.isTraceEnabled()) {
        LOG.trace("Cannot apply " + this.toString() + ": stack is empty");
      }
      return false;
    }

    return true;
  }

  public String getCode() {
    return name;
  }

  @Override
  public boolean doesReduce() {
    return true;
  }

}
