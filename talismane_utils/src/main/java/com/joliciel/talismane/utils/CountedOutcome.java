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
package com.joliciel.talismane.utils;

import java.io.Serializable;

/**
 * An outcome/value pair that orders automatically in descending order (from
 * highest to lowest count).
 * 
 * @author Assaf Urieli
 *
 */
public class CountedOutcome<T> implements Comparable<CountedOutcome<T>>, Serializable {
  private static final long serialVersionUID = 1L;
  private T outcome;
  private int count;

  public CountedOutcome(T outcome, int weight) {
    this.outcome = outcome;
    this.count = weight;
  }

  public void setOutcome(T outcome) {
    this.outcome = outcome;
  }

  public void setCount(int value) {
    this.count = value;
  }

  public T getOutcome() {
    return outcome;
  }

  public int getCount() {
    return count;
  }

  @Override
  public int compareTo(CountedOutcome<T> o) {
    if (this.getCount() < o.getCount()) {
      return 1;
    } else if (this.getCount() > o.getCount()) {
      return -1;
    } else {
      int nameCompare = this.getOutcome().toString().compareTo(o.getOutcome().toString());
      if (nameCompare != 0)
        return nameCompare;
      return this.hashCode() - o.hashCode();
    }
  }

  @Override
  public String toString() {
    return "[" + outcome + "," + count + "]";
  }

}
