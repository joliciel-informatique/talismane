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
package com.joliciel.talismane.machineLearning;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * A single probabilised decision used to construct a particular solution.
 * 
 * @author Assaf Urieli
 *
 */
public class Decision implements Comparable<Decision> {
  private static final DecimalFormat df = new DecimalFormat("0.0000");
  private final String outcome;
  private final double score;
  private final double probability;
  private double probabilityLog;
  private boolean probabilityLogCalculated = false;
  private List<String> authorities = new ArrayList<String>();
  private final boolean statistical;

  /**
   * Create the decision corresponding to a particular name. This decision will
   * be considered statistical.
   */
  public Decision(String outcome, double probability) {
    this.outcome = outcome;
    this.probability = probability;
    this.score = 0;
    this.statistical = true;
  }

  /**
   * Create a default decision with a probability of 1.0, for a given outcome.
   * This decision will not be considered statistical.
   */
  public Decision(String outcome) {
    this.outcome = outcome;
    this.probability = 1.0;
    this.score = 0;
    this.statistical = false;
  }

  /**
   * Create the decision corresponding to a particular name, with a score and
   * probability, for additive scoring systems (e.g. perceptrons). This decision
   * will be considered statistical.
   */
  public Decision(String outcome, double score, double probability) {
    this.outcome = outcome;
    this.score = score;
    this.probability = probability;
    this.statistical = true;
  }

  /**
   * A unique code representing this decision's outcome.
   */
  public String getOutcome() {
    return outcome;
  }

  /**
   * The decision's raw score, for additive scoring systems (e.g. perceptrons).
   */
  public double getScore() {
    return score;
  }

  /**
   * This decision's probability.
   */
  public double getProbability() {
    return probability;
  }

  /**
   * The log of this decision's probability. Avoids calculating the log multiple
   * times.
   */
  public double getProbabilityLog() {
    if (!probabilityLogCalculated) {
      probabilityLog = Math.log(probability);
      probabilityLogCalculated = true;
    }
    return probabilityLog;
  }

  @Override
  public int compareTo(Decision o) {
    if (this.getProbability() < o.getProbability()) {
      return 1;
    } else if (this.getProbability() > o.getProbability()) {
      return -1;
    } else {
      int nameCompare = this.getOutcome().compareTo(o.getOutcome());
      if (nameCompare != 0)
        return nameCompare;
      return this.hashCode() - o.hashCode();
    }
  }

  /**
   * A list of decision authorities which helped to make this decision. Useful
   * when decisions are made by different authorities based on certain criteria
   * - allows us to establish an f-score by authority, as well as analysing
   * errors by authority.
   */
  public List<String> getAuthorities() {
    return this.authorities;
  }

  /**
   * Add an authority to this decision's list.
   */
  public void addAuthority(String authority) {
    this.authorities.add(authority);
  }

  /**
   * Was this decision calculated by a statistical model, or was it made by
   * default, based on rules, etc.
   */
  public boolean isStatistical() {
    return statistical;
  }

  @Override
  public String toString() {
    return "Decision [" + outcome + "," + df.format(probability) + "]";
  }

}
