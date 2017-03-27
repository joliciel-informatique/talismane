package com.joliciel.talismane.machineLearning.features;

public interface FeatureParserInternal<T> extends FeatureParser<T> {
  /**
   * Inject any dependencies required by this feature to function correctly.
   */
  public void injectDependencies(@SuppressWarnings("rawtypes") Feature feature);
}
