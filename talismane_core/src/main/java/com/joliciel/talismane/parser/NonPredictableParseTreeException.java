package com.joliciel.talismane.parser;

import com.joliciel.talismane.TalismaneException;

/**
 * Thrown when it was impossible to predict the current parse tree using the
 * current transition system.
 * 
 * @author Assaf Urieli
 *
 */
public class NonPredictableParseTreeException extends TalismaneException {
  private static final long serialVersionUID = 1L;

  public NonPredictableParseTreeException(String message) {
    super(message);
  }

}
