///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
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
package com.joliciel.talismane.parser.features;

import java.util.Set;

import com.joliciel.talismane.machineLearning.features.BooleanFeature;
import com.joliciel.talismane.parser.Transition;

/**
 * A ParserRule is specified by a boolean feature and a Transition.<br/>
 * If the boolean feature evaluates to true, the configuration will automatically be assigned the Transition in question,
 * without taking any further decisions.<br/>
 * Negative rules are also possible: in this case, the Transition in question is eliminated from the set
 * of possible Transitions (unless no other Transitions are possible).
 * @author Assaf Urieli
 *
 */
public interface ParserRule {
	/**
	 * The condition to test.
	 * @return
	 */
	public BooleanFeature<ParseConfigurationWrapper> getCondition();
	
	/**
	 * The transition to apply (or to eliminate, for negative rules) if the condition evaluates to true.
	 * @return
	 */
	public Transition getTransition();
	
	/**
	 * The transitions to eliminate if the condition evaluates to true.
	 * @return
	 */
	public Set<Transition> getTransitions();
	
	/**
	 * Is this rule a negative rule or not.
	 * @return
	 */
	public boolean isNegative();
	public void setNegative(boolean negative);
	
}
