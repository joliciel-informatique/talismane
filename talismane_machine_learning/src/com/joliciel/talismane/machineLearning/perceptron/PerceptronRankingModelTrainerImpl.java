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
package com.joliciel.talismane.machineLearning.perceptron;

import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.procedure.TIntDoubleProcedure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.machineLearning.MachineLearningModel;
import com.joliciel.talismane.machineLearning.Ranker;
import com.joliciel.talismane.machineLearning.RankingEvent;
import com.joliciel.talismane.machineLearning.RankingEventStream;
import com.joliciel.talismane.machineLearning.RankingModel;
import com.joliciel.talismane.machineLearning.RankingSolution;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.utils.JolicielException;

class PerceptronRankingModelTrainerImpl<T> implements PerceptronRankingModelTrainer<T> {
	private static final Log LOG = LogFactory.getLog(PerceptronRankingModelTrainerImpl.class);
	private int iterations = 100;
	private int cutoff = 0;
	private double tolerance = 0.0001;
	
	private final TIntDoubleMap totalFeatureWeights = new TIntDoubleHashMap(1000, 0.7f, -1, 0.0);
	private PerceptronRankingModelParameters params = new PerceptronRankingModelParameters();

	private Map<String,Object> trainingParameters = new HashMap<String, Object>();
	
	public PerceptronRankingModelTrainerImpl() {
	}
	
	void prepareData(RankingEventStream<T> eventStream) {
		Map<Integer, Integer> featureCounts = new HashMap<Integer, Integer>();
		
		eventStream.rewind();
		while (eventStream.hasNext()) {
			RankingEvent<T> corpusEvent = eventStream.next();
			RankingSolution solution = corpusEvent.getSolution();
			for (List<FeatureResult<?>> featureResults : solution.getIncrementalFeatureResults()) {
				List<Integer> featureIndexes = new ArrayList<Integer>();
				List<Double> featureValues = new ArrayList<Double>();
				params.prepareData(featureResults, featureIndexes, featureValues, true);
				for (int featureIndex : featureIndexes) {
					Integer featureCountObj = featureCounts.get(featureIndex);
					int featureCount = featureCountObj==null ? 0 : featureCountObj.intValue();
					featureCount++;
					featureCounts.put(featureIndex, featureCount);
				}
			}
		}
		
		params.initialiseVector();
	}
	
	void train(RankingEventStream<T> eventStream, Ranker<T> ranker) {
		double prevAccuracy1 = 0.0;
		double prevAccuracy2 = 0.0;
		double prevAccuracy3 = 0.0;
		int actualIterations = 1;
		for (int i = 0; i<iterations; i++) {
			LOG.info("Iteration " + (i+1));
			int totalErrors = 0;
			int totalEvents = 0;
			int totalIncrementalErrors = 0;
			int totalIncrementalOutcomes = 0;
			
			eventStream.rewind();
			while (eventStream.hasNext()) {
				RankingEvent<T> corpusEvent = eventStream.next();
				RankingSolution solution = corpusEvent.getSolution();
				
				totalEvents++;
				
				List<RankingSolution> guesses = ranker.rank(corpusEvent.getInput(), params, solution);
				
				RankingSolution bestGuess = guesses.get(0);
				if (!bestGuess.canReach(solution)) {
					int incrementCount = 0;
					for (List<FeatureResult<?>> featureResults : bestGuess.getIncrementalFeatureResults()) {
						params.subtractVector(featureResults);
						incrementCount++;
					}
					for (int j=0; j<incrementCount && j<solution.getIncrementalFeatureResults().size(); j++) {
						List<FeatureResult<?>> featureResults = solution.getIncrementalFeatureResults().get(j);
						params.addVector(featureResults);
					}
					
					for (int j=0; j<bestGuess.getIncrementalOutcomes().size() && j<solution.getIncrementalOutcomes().size(); j++) {
						totalIncrementalOutcomes++;
						String guessedOutcome = bestGuess.getIncrementalOutcomes().get(j);
						String solutionOutcome = solution.getIncrementalOutcomes().get(j);
						if (!solutionOutcome.equals(guessedOutcome))
							totalIncrementalErrors++;
					}
					totalErrors++;
					
				} else {
					totalIncrementalOutcomes += bestGuess.getIncrementalOutcomes().size();
				} // correct outcome?
			} // next event
			
			// Add feature weights for this iteration
			params.getFeatureWeights().forEachEntry(
				new TIntDoubleProcedure() {
					
					@Override
					public boolean execute(int key, double value) {
						totalFeatureWeights.put(key, totalFeatureWeights.get(key) + value);
						return true;
					}
				});
			
			double accuracy = (double) (totalEvents - totalErrors) / (double) totalEvents;
			LOG.info("Solution accuracy: " + accuracy);
			double incrementalAccuracy = (double) (totalIncrementalOutcomes - totalIncrementalErrors) / (double) totalIncrementalOutcomes;
			LOG.info("Incremental step accuracy: " + incrementalAccuracy);
			LOG.debug("Feature count: " + params.getFeatureCount());
			
			// exit if incremental accuracy hasn't significantly improved in 3 iterations
			if (Math.abs(incrementalAccuracy - prevAccuracy1)<tolerance
					&& Math.abs(incrementalAccuracy - prevAccuracy2)<tolerance
					&& Math.abs(incrementalAccuracy - prevAccuracy3)<tolerance) {
				LOG.info("Incremental accuracy change < " + tolerance + " for 3 iterations: exiting after " + actualIterations + " iterations");
				break;
			}
			
			actualIterations++;
			prevAccuracy3 = prevAccuracy2;
			prevAccuracy2 = prevAccuracy1;
			prevAccuracy1 = accuracy;
		} // next iteration
		
		// average the final weights
		final double finalIterations = actualIterations;
		for (int key : params.getFeatureWeights().keys()) {
			params.getFeatureWeights().adjustValue(key, totalFeatureWeights.get(key)/ finalIterations);
		}
		params.finaliseVector();
	}

	public int getIterations() {
		return iterations;
	}

	public void setIterations(int iterations) {
		this.iterations = iterations;
	}

	public int getCutoff() {
		return cutoff;
	}

	public void setCutoff(int cutoff) {
		this.cutoff = cutoff;
	}

	public double getTolerance() {
		return tolerance;
	}

	public void setTolerance(double tolerance) {
		this.tolerance = tolerance;
	}

	@Override
	public RankingModel trainModel(
			RankingEventStream<T> corpusEventStream,
			Ranker<T> ranker,
			List<String> featureDescriptors) {
		Map<String,List<String>> descriptors = new HashMap<String, List<String>>();
		descriptors.put(MachineLearningModel.FEATURE_DESCRIPTOR_KEY, featureDescriptors);
		return this.trainModel(corpusEventStream, ranker, descriptors);
	}

	@Override
	public RankingModel trainModel(
			RankingEventStream<T> corpusEventStream,
			Ranker<T> ranker,
			Map<String, List<String>> descriptors) {
		this.prepareData(corpusEventStream);
		this.train(corpusEventStream, ranker);
		PerceptronRankingModel model = new PerceptronRankingModel(params, descriptors, this.trainingParameters);
		model.addModelAttribute("cutoff", "" + this.getCutoff());
		model.addModelAttribute("iterations", "" + this.getIterations());
		
		model.getModelAttributes().putAll(corpusEventStream.getAttributes());

		return model;
	}


	@Override
	public void setParameters(Map<String, Object> parameters) {
		if (parameters!=null) {
			this.trainingParameters = parameters;
			for (String parameter : parameters.keySet()) {
				PerceptronModelParameter modelParameter = PerceptronModelParameter.valueOf(parameter);
				Object value = parameters.get(parameter);
				if (!modelParameter.getParameterType().isAssignableFrom(value.getClass())) {
					throw new JolicielException("Parameter of wrong type: " + parameter + ". Expected: " + modelParameter.getParameterType().getSimpleName());
				}
				switch (modelParameter) {
				case Iterations:
					this.setIterations((Integer)value);
					break;
				case Cutoff:
					this.setCutoff((Integer)value);
					break;
				case Tolerance:
					this.setTolerance((Double)value);
					break;
				default:
					throw new JolicielException("Unknown parameter type: " + modelParameter);
				}
			}
		}
	}
}
