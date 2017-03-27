package com.joliciel.talismane.parser;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.posTagger.PosTaggedToken;

public class CircularDependencyException extends TalismaneException {
  private static final long serialVersionUID = 4444609621905354447L;
  private ParseConfiguration configuration;
  private PosTaggedToken head;
  private PosTaggedToken dependent;

  public CircularDependencyException(ParseConfiguration configuration, PosTaggedToken head, PosTaggedToken dependent) {
    super("Cannot add circular dependency, head = " + head + ", dependent = " + dependent);
    this.configuration = configuration;
    this.head = head;
    this.dependent = dependent;
  }

  public ParseConfiguration getConfiguration() {
    return configuration;
  }

  public PosTaggedToken getHead() {
    return head;
  }

  public PosTaggedToken getDependent() {
    return dependent;
  }

}
