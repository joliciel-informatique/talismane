///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2013 Assaf Urieli
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

import com.joliciel.talismane.machineLearning.features.FeatureResult;

/**
 * The solution to an incremental ranking problem.
 * By incremental, we mean that the problem is solved in steps, and each step can have its own feature vector.
 * If the problem is not incremental, simply include a single item in the feature results list.
 * @author Assaf Urieli
 *
 */
public interface RankingSolution extends Solution {
	/**
	 * One feature vector for each incremental step in the ranking solution.
	 * @return
	 */
	public List<List<FeatureResult<?>>> getIncrementalFeatureResults();
	
	/**
	 * The list of incremental outcomes corresponding to each step.
	 * @return
	 */
	public List<String> getIncrementalOutcomes();
	
	/**
	 * Can this ranking solution be used to reach the correct solution?
	 * @param correctSolution
	 * @return
	 */
	public boolean canReach(RankingSolution correctSolution);
}
