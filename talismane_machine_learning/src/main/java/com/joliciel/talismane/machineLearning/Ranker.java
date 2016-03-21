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
 * An algorithm which, given an input, will produce a set of ranked outputs in the form
 * of feature results.
 * @author Assaf Urieli
 *
 */
public interface Ranker<T> {
	/**
	 * Return <i>n</i> guesses, ordered by decreasing score, for a given input, given
	 * a feature weight vector. Stop solving as soon as none of the guesses can lead
	 * to the correct solution provided.
	 * @param input the input to be solved
	 * @param featureWeightVector the weights for each feature
	 * @param correctSolution the correct solution for this input
	 */
	public List<RankingSolution> rank(T input, FeatureWeightVector featureWeightVector, RankingSolution correctSolution);	
}
