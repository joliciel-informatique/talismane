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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.joliciel.talismane.machineLearning.ClassificationObserver;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.features.DoubleFeature;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.utils.WeightedOutcome;

/**
 * Writes a text file with a detailed analysis of what was calculated for each
 * event.
 * 
 * @author Assaf Urieli
 *
 */
class PerceptronDetailedAnalysisWriter implements ClassificationObserver {
	private static DecimalFormat decFormat;

	private Writer writer;
	private PerceptronModelParameters modelParams;
	private PerceptronDecisionMaker decisionMaker;

	static {
		decFormat = (DecimalFormat) DecimalFormat.getNumberInstance(Locale.US);
		decFormat.applyPattern("##0.0000");
	}

	public PerceptronDetailedAnalysisWriter(PerceptronDecisionMaker decisionMaker, File file) throws IOException {
		this.decisionMaker = decisionMaker;
		this.modelParams = decisionMaker.getModelParameters();
		file.delete();
		file.createNewFile();
		this.writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false), "UTF8"));
		this.initialise();
	}

	public PerceptronDetailedAnalysisWriter(PerceptronDecisionMaker decisionMaker, Writer outcomeFileWriter) {
		this.decisionMaker = decisionMaker;
		this.modelParams = decisionMaker.getModelParameters();
		this.writer = outcomeFileWriter;
		this.initialise();
	}

	private void initialise() {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.joliciel.talismane.maxent.MaxentObserver#onAnalyse(java.util.List,
	 * java.util.Collection)
	 */
	@Override
	public void onAnalyse(Object event, List<FeatureResult<?>> featureResults, Collection<Decision> decisions) throws IOException {
		Map<String, Double> outcomeTotals = new TreeMap<String, Double>();
		for (String outcome : modelParams.getOutcomes())
			outcomeTotals.put(outcome, 0.0);

		writer.append("####### Event: " + event.toString() + "\n");

		writer.append("### Feature results:\n");
		for (FeatureResult<?> featureResult : featureResults) {
			if (featureResult.getOutcome() instanceof List) {
				@SuppressWarnings("unchecked")
				FeatureResult<List<WeightedOutcome<String>>> stringCollectionResult = (FeatureResult<List<WeightedOutcome<String>>>) featureResult;
				for (WeightedOutcome<String> stringOutcome : stringCollectionResult.getOutcome()) {
					String featureName = featureResult.getTrainingName() + "|" + featureResult.getTrainingOutcome(stringOutcome.getOutcome());
					String featureOutcome = stringOutcome.getOutcome();
					double value = stringOutcome.getWeight();
					this.writeFeatureResult(featureName, featureOutcome, value, outcomeTotals);
				}

			} else {
				double value = 1.0;
				if (featureResult.getFeature() instanceof DoubleFeature) {
					value = (Double) featureResult.getOutcome();
				}
				this.writeFeatureResult(featureResult.getTrainingName(), featureResult.getOutcome().toString(), value, outcomeTotals);
			}
		}

		List<Integer> featureIndexList = new ArrayList<Integer>();
		List<Double> featureValueList = new ArrayList<Double>();
		modelParams.prepareData(featureResults, featureIndexList, featureValueList);
		double[] results = decisionMaker.predict(featureIndexList, featureValueList);

		writer.append("### Outcome totals:\n");

		writer.append(String.format("%1$-30s", "outcome") + String.format("%1$#15s", "total") + String.format("%1$#15s", "normalised") + "\n");

		int j = 0;
		for (String outcome : modelParams.getOutcomes()) {
			double total = outcomeTotals.get(outcome);
			double normalised = results[j++];
			writer.append(String.format("%1$-30s", outcome) + String.format("%1$#15s", decFormat.format(total))
					+ String.format("%1$#15s", decFormat.format(normalised)) + "\n");
		}
		writer.append("\n");

		Map<String, Double> outcomeWeights = new TreeMap<String, Double>();
		for (Decision decision : decisions) {
			outcomeWeights.put(decision.getOutcome(), decision.getProbability());
		}

		writer.append("### Outcome list:\n");
		Set<WeightedOutcome<String>> weightedOutcomes = new TreeSet<WeightedOutcome<String>>();
		for (String outcome : modelParams.getOutcomes()) {
			Double weightObj = outcomeWeights.get(outcome);
			double weight = (weightObj == null ? 0.0 : weightObj.doubleValue());
			WeightedOutcome<String> weightedOutcome = new WeightedOutcome<String>(outcome, weight);
			weightedOutcomes.add(weightedOutcome);
		}
		for (WeightedOutcome<String> weightedOutcome : weightedOutcomes) {
			writer.append(
					String.format("%1$-30s", weightedOutcome.getOutcome()) + String.format("%1$#15s", decFormat.format(weightedOutcome.getWeight())) + "\n");
		}
		writer.append("\n");
		writer.flush();
	}

	private void writeFeatureResult(String featureName, String featureOutcome, double value, Map<String, Double> outcomeTotals) throws IOException {
		writer.append("#" + featureName + "\t");
		writer.append("outcome=" + featureOutcome + "\n");
		writer.append("value=" + String.format("%1$-30s", value) + "\n");

		writer.append(String.format("%1$-30s", "outcome") + String.format("%1$#15s", "weight") + String.format("%1$#15s", "total") + "\n");
		int featureIndex = modelParams.getFeatureIndex(featureName);
		if (featureIndex >= 0) {
			double[] classWeights = modelParams.getFeatureWeights()[featureIndex];
			int j = 0;
			for (String outcome : modelParams.getOutcomes()) {
				double weight = classWeights[j];

				double total = value * weight;
				writer.append(String.format("%1$-30s", outcome) + String.format("%1$#15s", decFormat.format(weight))
						+ String.format("%1$#15s", decFormat.format(total)) + "\n");

				double runningTotal = outcomeTotals.get(outcome);
				runningTotal += total;
				outcomeTotals.put(outcome, runningTotal);
				j++;
			}
		}
		writer.append("\n");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.joliciel.talismane.maxent.MaxentObserver#onTerminate()
	 */
	@Override
	public void onTerminate() throws IOException {
		this.writer.flush();
		this.writer.close();
	}
}
