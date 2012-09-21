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
package com.joliciel.talismane.parser;

import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.Outcome;

/**
 * A single transition in a transition-based parsing system.
 * @author Assaf Urieli
 *
 */
public interface Transition extends Outcome {
	/**
	 * Check whether this transition is valid for the configuration provided.
	 * @param configuration
	 * @return
	 */
	public boolean checkPreconditions(ParseConfiguration configuration);
	
	/**
	 * Apply the transition to the configuration provided.
	 * @param configuration
	 * @return
	 */
	public void apply(ParseConfiguration configuration);
	
	/**
	 * The unique code for this transition.
	 * @return
	 */
	public String getCode();
	
	/**
	 * Returns true if this transition reduces the elements left to process,
	 * by removing an element permanently from either the stack or the buffer.
	 * @return
	 */
	public boolean doesReduce();
	
	/**
	 * The decision which generated this transition.
	 * @return
	 */
	public Decision<Transition> getDecision();
	public void setDecision(Decision<Transition> decision);
}
