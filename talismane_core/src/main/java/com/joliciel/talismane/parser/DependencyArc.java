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

import com.joliciel.talismane.posTagger.PosTaggedToken;

/**
 * A dependency arc, indicating a dependency relationship between two pos-tagged
 * tokens, one of which is the head and one of which is the dependent, with a
 * particular functional label (e.g. subject, direct object, etc.).
 * 
 * @author Assaf Urieli
 *
 */
public class DependencyArc implements Comparable<DependencyArc> {
  private final PosTaggedToken head;
  private final PosTaggedToken dependent;
  private final String label;
  private String comment = "";
  private double probability;

  public DependencyArc(PosTaggedToken head, PosTaggedToken dependent, String label) {
    this.head = head;
    this.dependent = dependent;
    this.label = label;
  }

  /**
   * The head (or governor) of this dependency arc.
   */
  public PosTaggedToken getHead() {
    return this.head;
  }

  /**
   * The dependent of this dependency arc.
   */
  public PosTaggedToken getDependent() {
    return this.dependent;
  }

  /**
   * The functional label of this dependency arc.
   */
  public String getLabel() {
    return this.label;
  }

  @Override
  public int hashCode() {
    final int prime = 2;
    int result = 1;
    result = prime * result + ((dependent == null) ? 0 : dependent.hashCode());
    result = prime * result + ((head == null) ? 0 : head.hashCode());
    result = prime * result + ((label == null) ? 0 : label.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    DependencyArc other = (DependencyArc) obj;
    if (dependent == null) {
      if (other.dependent != null)
        return false;
    } else if (!dependent.equals(other.dependent))
      return false;
    if (head == null) {
      if (other.head != null)
        return false;
    } else if (!head.equals(other.head))
      return false;
    if (label == null) {
      if (other.label != null)
        return false;
    } else if (!label.equals(other.label))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "Dep[" + head + "," + label + "," + dependent + "]";
  }

  @Override
  public int compareTo(DependencyArc o) {
    return this.getDependent().getToken().getIndex() - o.getDependent().getToken().getIndex();
  }

  /**
   * The probability associated with this dependency arc.
   */
  public double getProbability() {
    return probability;
  }

  public void setProbability(double probability) {
    this.probability = probability;
  }

  /**
   * A comment regarding this depenency arc annotation.
   */
  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }
}
