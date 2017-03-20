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

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * A machine learning model for classification.
 * 
 * @author Assaf Urieli
 *
 */
public interface ClassificationModel extends MachineLearningModel {

	/**
	 * Get the decision maker for this model.
	 */
	public DecisionMaker getDecisionMaker();

	/**
	 * An observer that will write low-level details of this model's analysis to
	 * a file.
	 * 
	 * @throws IOException
	 */
	public ClassificationObserver getDetailedAnalysisObserver(File file) throws IOException;

	/**
	 * A set of possible outcomes for this model.
	 */
	public Set<String> getOutcomeNames();
}
