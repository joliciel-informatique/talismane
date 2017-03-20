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
package com.joliciel.talismane.machineLearning.maxent;

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

import opennlp.model.Context;
import opennlp.model.IndexHashTable;
import opennlp.model.MaxentModel;

/**
 * Writes a text file with a detailed analysis of what was calculated for each
 * event.
 * 
 * @author Assaf Urieli
 *
 */
class MaxentDetailedAnalysisWriter implements ClassificationObserver {
	private static DecimalFormat decFormat;

	private Writer writer;
	private MaxentModel maxentModel;
	private List<String> outcomeList = new ArrayList<String>();
	private String[] predicates;
	private Context[] modelParameters;
	private String[] outcomeNames;
	private IndexHashTable<String> predicateTable;

	static {
		decFormat = (DecimalFormat) DecimalFormat.getNumberInstance(Locale.US);
		decFormat.applyPattern("##0.00000000");
	}

	public MaxentDetailedAnalysisWriter(MaxentModel maxentModel, File file) throws IOException {
		this.maxentModel = maxentModel;
		file.delete();
		file.createNewFile();
		this.writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false), "UTF8"));
		this.initialise();
	}

	public MaxentDetailedAnalysisWriter(MaxentModel maxentModel, Writer outcomeFileWriter) {
		this.maxentModel = maxentModel;
		this.writer = outcomeFileWriter;
		this.initialise();
	}

	@SuppressWarnings("unchecked")
	private void initialise() {
		Object[] dataStructures = maxentModel.getDataStructures();
		outcomeNames = (String[]) dataStructures[2];
		TreeSet<String> outcomeSet = new TreeSet<String>();
		for (String outcome : outcomeNames)
			outcomeSet.add(outcome);
		outcomeList.addAll(outcomeSet);
		this.predicateTable = (IndexHashTable<String>) dataStructures[1];
		predicates = new String[predicateTable.size()];
		predicateTable.toArray(predicates);
		modelParameters = (Context[]) dataStructures[0];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.joliciel.talismane.maxent.MaxentObserver#onAnalyse(java.util.List,
	 * java.util.Collection)
	 */
	@Override
	public void onAnalyse(Object event, List<FeatureResult<?>> featureResults, Collection<Decision> outcomes) throws IOException {
		Map<String, Double> outcomeTotals = new TreeMap<String, Double>();
		double uniformPrior = Math.log(1 / (double) outcomeList.size());

		for (String outcome : outcomeList)
			outcomeTotals.put(outcome, uniformPrior);

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

		writer.append("### Outcome totals:\n");
		writer.append("# Uniform prior: " + uniformPrior + " (=1/" + outcomeList.size() + ")\n");

		double grandTotal = 0;
		for (String outcome : outcomeList) {
			double total = outcomeTotals.get(outcome);
			double expTotal = Math.exp(total);
			grandTotal += expTotal;
		}
		writer.append(String.format("%1$-30s", "outcome") + String.format("%1$#15s", "total(log)") + String.format("%1$#15s", "total")
				+ String.format("%1$#15s", "normalised") + "\n");

		for (String outcome : outcomeList) {
			double total = outcomeTotals.get(outcome);
			double expTotal = Math.exp(total);
			writer.append(String.format("%1$-30s", outcome) + String.format("%1$#15s", decFormat.format(total))
					+ String.format("%1$#15s", decFormat.format(expTotal)) + String.format("%1$#15s", decFormat.format(expTotal / grandTotal)) + "\n");
		}
		writer.append("\n");

		Map<String, Double> outcomeWeights = new TreeMap<String, Double>();
		for (Decision decision : outcomes) {
			outcomeWeights.put(decision.getOutcome(), decision.getProbability());
		}

		writer.append("### Outcome list:\n");
		Set<WeightedOutcome<String>> weightedOutcomes = new TreeSet<WeightedOutcome<String>>();
		for (String outcome : outcomeList) {
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

		writer.append(String.format("%1$-30s", "outcome") + String.format("%1$#15s", "weight") + String.format("%1$#15s", "total")
				+ String.format("%1$#15s", "exp") + "\n");
		int predicateIndex = predicateTable.get(featureName);
		if (predicateIndex >= 0) {
			Context context = modelParameters[predicateIndex];
			int[] outcomeIndexes = context.getOutcomes();
			double[] parameters = context.getParameters();
			for (String outcome : outcomeList) {
				int outcomeIndex = -1;
				for (int j = 0; j < outcomeNames.length; j++) {
					if (outcomeNames[j].equals(outcome)) {
						outcomeIndex = j;
						break;
					}
				}
				int paramIndex = -1;
				for (int k = 0; k < outcomeIndexes.length; k++) {
					if (outcomeIndexes[k] == outcomeIndex) {
						paramIndex = k;
						break;
					}
				}
				double weight = 0.0;
				if (paramIndex >= 0)
					weight = parameters[paramIndex];

				double total = value * weight;
				double exp = Math.exp(total);
				writer.append(String.format("%1$-30s", outcome) + String.format("%1$#15s", decFormat.format(weight))
						+ String.format("%1$#15s", decFormat.format(total)) + String.format("%1$#15s", decFormat.format(exp)) + "\n");

				double runningTotal = outcomeTotals.get(outcome);
				runningTotal += total;
				outcomeTotals.put(outcome, runningTotal);
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
