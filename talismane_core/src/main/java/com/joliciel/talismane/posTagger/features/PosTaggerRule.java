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
package com.joliciel.talismane.posTagger.features;

import com.joliciel.talismane.machineLearning.features.BooleanFeature;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTaggerContext;

/**
 * A PosTaggerRule is specified by a boolean feature and a PosTag.<br/>
 * If the boolean feature evaluates to true, the token will automatically be
 * assigned the PosTag in question, without taking any further decisions.<br/>
 * Negative rules are also possible: in this case, the PosTag in question is
 * eliminated from the set of possible PosTags (unless no other PosTags are
 * possible).
 * 
 * @author Assaf Urieli
 *
 */
public final class PosTaggerRule {
  private BooleanFeature<PosTaggerContext> condition;
  private PosTag tag;
  private boolean negative;

  public PosTaggerRule(BooleanFeature<PosTaggerContext> condition, PosTag tag) {
    this.condition = condition;
    this.tag = tag;
  }

  /**
   * The condition to test.
   */

  public BooleanFeature<PosTaggerContext> getCondition() {
    return condition;
  }

  /**
   * The tag to apply if the condition evaluates to true for a positive rule, or
   * to avoid if it is a negative rule.
   */
  public PosTag getTag() {
    return tag;
  }

  /**
   * Is this rule a negative rule or not.
   */
  public boolean isNegative() {
    return negative;
  }

  public void setNegative(boolean negative) {
    this.negative = negative;
  }

}
