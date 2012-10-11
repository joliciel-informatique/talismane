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
package com.joliciel.talismane.machineLearning;

import java.util.List;

/**
 * A solution to a particular NLP problem.
 * @type T the outcomes (or classifications) proposed for this NLP problem.
 * @author Assaf Urieli
 *
 */
public interface Solution<T extends Outcome> {
	/**
	 * This solution's total score.
	 */
	public double getScore();
	
	/**
	 * The decisions which were used to arrive at this solution.
	 * @return
	 */
	public List<Decision<T>> getDecisions();
	
	/**
	 * The solutions underlying this solution, in the case of several overlaid levels of abstraction,
	 * e.g. a syntax parse depends on an underlying pos-tagging solution, which itself depends on an underlying
	 * tokenising solution.
	 * @return
	 */
	public List<Solution<?>> getUnderlyingSolutions();
	
	/**
	 * Add a decision to this particular solution.
	 * @param decision
	 */
	public void addDecision(Decision<T> decision);
	
	/**
	 * Get the scoring strategy for this solution.
	 * @return
	 */
	public ScoringStrategy getScoringStrategy();
	public void setScoringStrategy(ScoringStrategy scoringStrategy);
}
