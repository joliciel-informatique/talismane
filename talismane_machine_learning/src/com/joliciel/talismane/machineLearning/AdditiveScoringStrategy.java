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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * For perceptrons and other additive score methods.
 * Simply adds all of the scores for individual decisions,
 * and divides by the number of decisions.
 * @author Assaf Urieli
 *
 */
public class AdditiveScoringStrategy<T extends Outcome> implements ScoringStrategy<ClassificationSolution<T>> {
	private static final Log LOG = LogFactory.getLog(AdditiveScoringStrategy.class);

	@Override
	public double calculateScore(ClassificationSolution<T> solution) {
		double score = 0;
		if (solution!=null && solution.getDecisions().size()>0) {
			for (Decision<?> decision : solution.getDecisions())
				score += decision.getScore();
			score /= solution.getDecisions().size();
		}
		
		if (LOG.isTraceEnabled()) {
			if (solution!=null) {
				LOG.trace("Score for solution: " + solution.getClass().getSimpleName());
				LOG.trace(solution.toString());
				StringBuilder sb = new StringBuilder();
				for (Decision<?> decision : solution.getDecisions()) {
					sb.append(" + ");
					sb.append(decision.getScore());
				}
				sb.append(" / ");
				sb.append(solution.getDecisions().size());
				sb.append(" = ");
				sb.append(score);
	
				LOG.trace(sb.toString());
			}
		}
		
		for (Solution underlyingSolution : solution.getUnderlyingSolutions()) {
			if (solution.getScoringStrategy().isAdditive())
				score += underlyingSolution.getScore();
		}
		
		if (LOG.isTraceEnabled()) {
			for (Solution underlyingSolution : solution.getUnderlyingSolutions()) {
				if (solution.getScoringStrategy().isAdditive())
					LOG.trace(" + " + underlyingSolution.getScore() + " (" + underlyingSolution.getClass().getSimpleName() + ")");
			}
			LOG.trace(" = " + score);
		}

		return score;
	}

	@Override
	public boolean isAdditive() {
		return true;
	}

}
