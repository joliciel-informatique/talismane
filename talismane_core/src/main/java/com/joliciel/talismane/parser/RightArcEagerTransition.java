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

import com.joliciel.talismane.posTagger.PosTaggedToken;

import java.util.Objects;

/**
 * Create a dependency where Buffer[0] depends on Stack[0], and push Buffer[0]
 * to the top of the stack. Example: in "eat apple", create a "object"
 * dependency obj(eat,apple), and pushes "apple" to the top of stack.
 * 
 * @author Assaf Urieli
 *
 */
public class RightArcEagerTransition extends AbstractTransition implements Transition {
  private static final long serialVersionUID = 1L;
  private static final Logger LOG = LoggerFactory.getLogger(RightArcEagerTransition.class);
  private String label;
  private String name;

  public RightArcEagerTransition(String label) {
    super();
    this.label = label;
  }

  @Override
  protected void applyInternal(ParseConfiguration configuration) throws CircularDependencyException {
    PosTaggedToken head = configuration.getStack().peek();
    PosTaggedToken dependent = configuration.getBuffer().pollFirst();
    configuration.getStack().push(dependent);
    configuration.addDependency(head, dependent, label, this);
  }

  @Override
  public boolean checkPreconditions(ParseConfiguration configuration) {
    if (configuration.getBuffer().isEmpty() || configuration.getStack().isEmpty()) {
      if (LOG.isTraceEnabled()) {
        LOG.trace("Cannot apply " + this.toString() + ": buffer or stack is empty");
      }
      return false;
    }

    PosTaggedToken topOfBuffer = configuration.getBuffer().peekFirst();

    // the top-of-buffer must not yet have a governor
    PosTaggedToken governor = configuration.getHead(topOfBuffer);
    if (governor != null) {
      if (LOG.isTraceEnabled()) {
        LOG.trace("Cannot apply " + this.toString() + ": top of buffer " + topOfBuffer + " already has governor " + governor);
      }
      return false;
    }

    return true;
  }

  @Override
  public String getCode() {
    if (this.name == null) {
      this.name = "RightArc";
      if (this.label != null && this.label.length() > 0)
        this.name += "[" + this.label + "]";
    }

    return this.name;
  }

  @Override
  public boolean doesReduce() {
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    RightArcEagerTransition that = (RightArcEagerTransition) o;
    return label.equals(that.label);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), label);
  }
}
