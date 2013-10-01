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

import com.joliciel.talismane.machineLearning.perceptron.PerceptronService.PerceptronScoring;

/**
 * A class storing session-wide reference data.
 * @author Assaf Urieli
 *
 */
public class MachineLearningSession {
	private static ThreadLocal<PerceptronScoring> perceptronScoringHolder = new ThreadLocal<PerceptronScoring>();
	
	public static PerceptronScoring getPerceptronScoring() {
		PerceptronScoring scoring = perceptronScoringHolder.get();
		if (scoring==null) {
			scoring = PerceptronScoring.normalisedExponential;
			setPerceptronScoring(scoring);
		}
		return scoring;
	}
	
	public static void setPerceptronScoring(PerceptronScoring scoring) {
		perceptronScoringHolder.set(scoring);
	}
	
}
