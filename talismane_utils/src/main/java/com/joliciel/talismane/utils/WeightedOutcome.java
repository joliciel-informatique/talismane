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
 * An outcome/value pair that orders automatically in descending order (from highest to lowest weight).
 * @author Assaf Urieli
 *
 */
public class WeightedOutcome<T> implements Comparable<WeightedOutcome<T>>, Serializable {
  private static final long serialVersionUID = 6970237630148498476L;
  private T outcome;
  private double weight;
  private transient double weightLog;
  private transient boolean weightLogCalculated = false;
  
  public WeightedOutcome(T outcome, double weight) {
    this.outcome = outcome;
    this.weight = weight;
  }
  
  
  public void setOutcome(T outcome) {
    this.outcome = outcome;
  }


  public void setWeight(double value) {
    this.weight = value;
    this.weightLogCalculated = false;
  }


  public T getOutcome() {
    return outcome;
  }


  public double getWeight() {
    return weight;
  }
  
  public double getWeightLog() {
    if (!weightLogCalculated) {
      weightLog = Math.log(weight);
      weightLogCalculated = true;
    }
    return weightLog;
  }


  @Override
  public int compareTo(WeightedOutcome<T> o) {
    if (this.getWeight()<o.getWeight()) {
      return 1;
    } else if (this.getWeight()>o.getWeight()) {
      return -1;
    } else {
      int nameCompare = this.getOutcome().toString().compareTo(o.getOutcome().toString());
      if (nameCompare!=0) return nameCompare;
      return this.hashCode()-o.hashCode();
    }
  }


  @Override
  public String toString() {
    return "[" + outcome + "," + weight
        + "]";
  }
  
  
}
