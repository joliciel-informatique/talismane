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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Takes the geometric mean of the individual decision scores, and multiplies it
 * by the scores of underlying solutions.
 * 
 * @author Assaf Urieli
 *
 */
public class GeometricMeanScoringStrategy implements ScoringStrategy<ClassificationSolution> {
  private static final Logger LOG = LoggerFactory.getLogger(GeometricMeanScoringStrategy.class);

  @Override
  public double calculateScore(ClassificationSolution solution) {
    double score = 0;
    if (solution != null && solution.getDecisions().size() > 0) {
      for (Decision decision : solution.getDecisions())
        score += decision.getProbabilityLog();

      score = score / solution.getDecisions().size();
    }
    score = Math.exp(score);

    if (LOG.isTraceEnabled()) {
      LOG.trace("Score for solution: " + solution.getClass().getSimpleName());
      LOG.trace(solution.toString());
      StringBuilder sb = new StringBuilder();
      for (Decision decision : solution.getDecisions()) {
        sb.append(" * ");
        sb.append(decision.getProbability());
      }
      sb.append(" root ");
      sb.append(solution.getDecisions().size());
      sb.append(" = ");
      sb.append(score);

      LOG.trace(sb.toString());
    }

    for (Solution underlyingSolution : solution.getUnderlyingSolutions()) {
      if (!underlyingSolution.getScoringStrategy().isAdditive())
        score = score * underlyingSolution.getScore();
    }

    if (LOG.isTraceEnabled()) {
      for (Solution underlyingSolution : solution.getUnderlyingSolutions()) {
        if (!underlyingSolution.getScoringStrategy().isAdditive())
          LOG.trace(" * " + underlyingSolution.getScore() + " (" + underlyingSolution.getClass().getSimpleName() + ")");
      }
      LOG.trace(" = " + score);
    }

    return score;
  }

  @Override
  public boolean isAdditive() {
    return false;
  }

}
