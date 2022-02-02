/**
 * Top-level package for machine-learning. The central interface is the
 * {@link com.joliciel.talismane.machineLearning.MachineLearningModel}, usually
 * a {@link com.joliciel.talismane.machineLearning.ClassificationModel} trained
 * using a
 * {@link com.joliciel.talismane.machineLearning.ClassificationModelTrainer},
 * based on a
 * {@link com.joliciel.talismane.machineLearning.ClassificationEventStream}
 * which consists of a stream of
 * {@link com.joliciel.talismane.machineLearning.ClassificationEvent}.<br>
 * This model provides a
 * {@link com.joliciel.talismane.machineLearning.DecisionMaker} capable of
 * returning a list of {@link com.joliciel.talismane.machineLearning.Decision}
 * ordered by descending probability.
 */
package com.joliciel.talismane.machineLearning;
