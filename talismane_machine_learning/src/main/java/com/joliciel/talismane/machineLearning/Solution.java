package com.joliciel.talismane.machineLearning;

import java.util.List;

public interface Solution {
  /**
   * This solution's total score.
   */
  public double getScore();

  /**
   * The solutions underlying this solution, in the case of several overlaid
   * levels of abstraction, e.g. a syntax parse depends on an underlying
   * pos-tagging solution, which itself depends on an underlying tokenising
   * solution.
   */
  public List<Solution> getUnderlyingSolutions();

  /**
   * Get the scoring strategy for this solution.
   */
  @SuppressWarnings("rawtypes")
  public ScoringStrategy getScoringStrategy();

  public void setScoringStrategy(@SuppressWarnings("rawtypes") ScoringStrategy scoringStrategy);
}
