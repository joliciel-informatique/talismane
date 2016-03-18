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

import java.util.List;

/**
 * A single probabilised decision used to construct a particular solution.
 * @author Assaf Urieli
 *
 */
public interface Decision extends Comparable<Decision> {
	/**
	 * A unique code representing this decision's outcome.
	 */
	public String getOutcome();
	
	/**
	 * The decision's raw score, for additive scoring systems (e.g. perceptrons).
	 */
	public double getScore();
	
	/**
	 * This decision's probability.
	 */
	public double getProbability();
	
	/**
	 * The log of this decision's probability.
	 * Avoids calculating the log multiple times.
	 */
	public double getProbabilityLog();
		
	/**
	 * Was this decision calculated by a statistical model, or was it made by default, based on rules, etc.
	 */
	public boolean isStatistical();

	/**
	 * A list of decision authorities which helped to make this decision.
	 * Useful when decisions are made by different authorities based on certain criteria -
	 * allows us to establish an f-score by authority, as well as analysing errors by authority. 
	 */
	public List<String> getAuthorities();
	
	/**
	 * Add an authority to this decision's list.
	 */
	public void addAuthority(String authority);


}
