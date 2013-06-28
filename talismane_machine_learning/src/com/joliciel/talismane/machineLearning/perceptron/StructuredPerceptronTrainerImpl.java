///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2013 Assaf Urieli
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.machineLearning.CorpusEvent;
import com.joliciel.talismane.machineLearning.CorpusEventStream;
import com.joliciel.talismane.machineLearning.DecisionFactory;
import com.joliciel.talismane.machineLearning.MachineLearningModel;
import com.joliciel.talismane.machineLearning.Outcome;
import com.joliciel.talismane.utils.JolicielException;
import com.joliciel.talismane.utils.LogUtils;

class StructuredPerceptronTrainerImpl<T extends Outcome> implements PerceptronModelTrainer<T> {
	private static final Log LOG = LogFactory.getLog(StructuredPerceptronTrainerImpl.class);
	private int iterations = 100;
	private int cutoff = 0;
	private double tolerance = 0.0001;
	private boolean calculateLogLikelihood = false;
	
	private double[][] totalFeatureWeights;
	private PerceptronModelParameters params;
	private File eventFile;
	private PerceptronDecisionMaker<T> decisionMaker;
	private DecisionFactory<T> decisionFactory;
	
	public StructuredPerceptronTrainerImpl() {
	}
	
	void prepareData(CorpusEventStream eventStream) {
		try {
			eventFile = File.createTempFile("events","txt");
			Writer eventWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(eventFile), "UTF-8"));
			while (eventStream.hasNext()) {
				CorpusEvent corpusEvent = eventStream.next();
				PerceptronEvent event = new PerceptronEvent(corpusEvent, decisionMaker, params);
				event.write(eventWriter);
			}
			eventWriter.flush();
			eventWriter.close();
			
			if (cutoff>1) {
				params.initialiseCounts();
				Scanner scanner = new Scanner(eventFile, "UTF-8");
				while (scanner.hasNextLine()) {
					String line = scanner.nextLine();
					PerceptronEvent event = new PerceptronEvent(line);
					for (int featureIndex : event.getFeatureIndexes()) {
						params.getFeatureCounts()[featureIndex]++;
					}
				}
				scanner.close();
				
				PerceptronModelParameters cutoffParams = new PerceptronModelParameters();
				int[] newIndexes = cutoffParams.initialise(params, cutoff);
				decisionMaker = new PerceptronDecisionMaker<T>(cutoffParams, decisionFactory);
				scanner = new Scanner(eventFile, "UTF-8");
				eventFile = File.createTempFile("eventsCutoff","txt");
				Writer eventCutoffWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(eventFile), "UTF-8"));
				while (scanner.hasNextLine()) {
					String line = scanner.nextLine();
					PerceptronEvent oldEvent = new PerceptronEvent(line);
					PerceptronEvent newEvent = new PerceptronEvent(oldEvent, newIndexes);
					newEvent.write(eventCutoffWriter);
				}
				eventCutoffWriter.flush();
				eventCutoffWriter.close();
				params = cutoffParams;
			}
			
			params.initialiseWeights();
			totalFeatureWeights = new double[params.getFeatureCount()][params.getOutcomeCount()];
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
	
	void train() {
		try {
			double prevAccuracy1 = 0.0;
			double prevAccuracy2 = 0.0;
			double prevAccuracy3 = 0.0;
			int actualIterations = 1;
			for (int i = 0; i<iterations; i++) {
				LOG.debug("Iteration " + (i+1));
				int totalErrors = 0;
				int totalEvents = 0;
				double logLikelihood = 0.0;
				
				Scanner scanner = new Scanner(eventFile, "UTF-8");
				while (scanner.hasNextLine()) {
					String line = scanner.nextLine();
					PerceptronEvent event = new PerceptronEvent(line);
					totalEvents++;
					
					// don't normalise unless we calculate the log-likelihood, do avoid mathematical cost of normalising
					double[] results = decisionMaker.predict(event.getFeatureIndexes(), event.getFeatureValues(), calculateLogLikelihood);
					double maxValue = results[0];
					int predicted = 0;
					for (int j=1;j<results.length;j++) {
						if (results[j]>maxValue) {
							maxValue = results[j];
							predicted = j;
						}
					}
					
					int actual = event.getOutcomeIndex();
					if (calculateLogLikelihood)
						logLikelihood += Math.log(results[actual] + 0.0001);
					
					if (actual!=predicted) {
						for (int j=0; j<event.getFeatureIndexes().size(); j++) {
							double[] classWeights = params.getFeatureWeights()[event.getFeatureIndexes().get(j)];
							classWeights[actual] += event.getFeatureValues().get(j);
							classWeights[predicted] -= event.getFeatureValues().get(j);
						}
						totalErrors++;
					} // correct outcome?
				} // next event
				
				// Add feature weights for this iteration
				for (int j=0;j<params.getFeatureWeights().length;j++) {
					double[] totalClassWeights = totalFeatureWeights[j];
					double[] classWeights = params.getFeatureWeights()[j];
					for (int k=0;k<params.getOutcomeCount();k++) {
						totalClassWeights[k] += classWeights[k];
					}
				}
				
				double accuracy = (double) (totalEvents - totalErrors) / (double) totalEvents;
				LOG.debug("Accuracy: " + accuracy);
				if (calculateLogLikelihood)
					LOG.debug("LogLikelihood: " + logLikelihood);
				
				// exit if log-likelihood doesn't improve
				if (Math.abs(accuracy - prevAccuracy1)<tolerance
						&& Math.abs(accuracy - prevAccuracy2)<tolerance
						&& Math.abs(accuracy - prevAccuracy3)<tolerance) {
					LOG.info("Accuracy change < " + tolerance + " for 3 iterations: exiting after " + actualIterations + " iterations");
					break;
				}
				
				actualIterations++;
				prevAccuracy3 = prevAccuracy2;
				prevAccuracy2 = prevAccuracy1;
				prevAccuracy1 = accuracy;
			} // next iteration
			
			// average the final weights
			for (int j=0;j<params.getFeatureWeights().length;j++) {
				double[] totalClassWeights = totalFeatureWeights[j];
				double[] classWeights = params.getFeatureWeights()[j];
				for (int k=0;k<params.getOutcomeCount();k++) {	
					classWeights[k] = totalClassWeights[k] / actualIterations;
				}
			}
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
	
	private static final class PerceptronEvent {
		List<Integer> featureIndexes;
		List<Double> featureValues;
		int outcomeIndex;
		
		public PerceptronEvent(CorpusEvent corpusEvent, PerceptronDecisionMaker<?> decisionMaker, PerceptronModelParameters params) {
			featureIndexes = new ArrayList<Integer>();
			featureValues = new ArrayList<Double>();
			decisionMaker.prepareData(corpusEvent.getFeatureResults(), featureIndexes, featureValues, true);
			outcomeIndex = params.getOrCreateOutcomeIndex(corpusEvent.getClassification());
		}
		
		public PerceptronEvent(String line) {
			String[] parts = line.split(" ");
			this.outcomeIndex = Integer.parseInt(parts[0]);
			int featureCount = (parts.length - 1) / 2;
			featureIndexes = new ArrayList<Integer>(featureCount);
			featureValues = new ArrayList<Double>(featureCount);
			int j=1;
			for (int i=0; i<featureCount; i++) {
				featureIndexes.add(Integer.parseInt(parts[j++]));
				featureValues.add(Double.parseDouble(parts[j++]));
			}
		}
		

		public PerceptronEvent(PerceptronEvent oldEvent, int[] newIndexes) {
			featureIndexes = new ArrayList<Integer>();
			featureValues = new ArrayList<Double>();
			int i=0;
			for (int oldIndex : oldEvent.featureIndexes) {
				if (newIndexes[oldIndex]>=0) {
					featureIndexes.add(newIndexes[oldIndex]);
					featureValues.add(oldEvent.featureValues.get(i));
				}
				i++;
			}
			outcomeIndex = oldEvent.outcomeIndex;
		}

		public List<Integer> getFeatureIndexes() {
			return featureIndexes;
		}
		public List<Double> getFeatureValues() {
			return featureValues;
		}
		public int getOutcomeIndex() {
			return outcomeIndex;
		}
		
		public void write(Writer writer) throws IOException {
			writer.write("" + outcomeIndex);
			for (int i=0;i<featureIndexes.size();i++) {
				writer.write(" ");
				writer.write("" + featureIndexes.get(i));
				writer.write(" ");
				writer.write("" + featureValues.get(i));
			}
			writer.write("\n");
			writer.flush();
		}
		
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
	public MachineLearningModel<T> trainModel(
			CorpusEventStream corpusEventStream,
			DecisionFactory<T> decisionFactory, List<String> featureDescriptors) {
		Map<String,List<String>> descriptors = new HashMap<String, List<String>>();
		descriptors.put(MachineLearningModel.FEATURE_DESCRIPTOR_KEY, featureDescriptors);
		return this.trainModel(corpusEventStream, decisionFactory, descriptors);
	}

	@Override
	public MachineLearningModel<T> trainModel(
			CorpusEventStream corpusEventStream,
			DecisionFactory<T> decisionFactory,
			Map<String, List<String>> descriptors) {
		params = new PerceptronModelParameters();
		decisionMaker = new PerceptronDecisionMaker<T>(params, decisionFactory);
		this.decisionFactory = decisionFactory;
		this.prepareData(corpusEventStream);
		this.train();
		PerceptronModel<T> model = new PerceptronModel<T>(params, descriptors, decisionFactory);
		model.addModelAttribute("cutoff", this.getCutoff());
		model.addModelAttribute("iterations", this.getIterations());
		
		model.getModelAttributes().putAll(corpusEventStream.getAttributes());

		return model;		
	}


	@Override
	public void setParameters(Map<String, Object> parameters) {
		if (parameters!=null) {
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

	public boolean isCalculateLogLikelihood() {
		return calculateLogLikelihood;
	}

	public void setCalculateLogLikelihood(boolean calculateLogLikelihood) {
		this.calculateLogLikelihood = calculateLogLikelihood;
	}

}
