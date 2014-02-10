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

import java.io.Serializable;

/**
 * A factory for generating a decision implementation of a particular type.
 * @author Assaf Urieli
 *
 */
public interface DecisionFactory<T extends Outcome> extends Serializable {
	/**
	 * Create the decision corresponding to a particular name.
	 * This decision will be considered statistical.
	 */
	public Decision<T> createDecision(String name, double probability);
	
	/**
	 * Create the decision corresponding to a particular name.
	 * This decision will be considered statistical.
	 */
	public Decision<T> createDecision(String name, double score, double probability);

	
	/**
	 * Create a default decision with a probability of 1.0, for a given outcome T.
	 * This decision will not be considered statistical.
	 * @param defaultDecision
	 * @return
	 */
	public Decision<T> createDefaultDecision(T outcome);
}
