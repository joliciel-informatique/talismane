///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
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
package com.joliciel.talismane.utils.stats;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Calculates the f-score for a given classification task.
 * @author Assaf Urieli
 *
 */
public class FScoreCalculator<E> {
	private static final Log LOG = LogFactory.getLog(FScoreCalculator.class);
	Map<E,Map<E,Integer>> falsePositives = new TreeMap<E,Map<E,Integer>>();
	Map<E,Map<E,Integer>> falseNegatives = new TreeMap<E,Map<E,Integer>>();
	Map<E,Integer> truePositiveCounts = new TreeMap<E, Integer>();
	Map<E,Integer> falsePositiveCounts = new TreeMap<E, Integer>();
	Map<E,Integer> falseNegativeCounts = new TreeMap<E, Integer>();
	
	Set<E> outcomeSet = new HashSet<E>();
	
	Map<E,Double> precisions = new TreeMap<E, Double>();
	Map<E,Double> recalls = new TreeMap<E, Double>();
	Map<E,Double> fScores = new TreeMap<E, Double>();
	
	int testCount = 0;
	double totalPrecision = 0.0;
	double totalRecall = 0.0;
	double totalFScore = 0.0;
	
	double totalTruePositiveCount = 0;
	double totalFalsePositiveCount = 0;
	double totalFalseNegativeCount = 0;
	
	boolean updatedSinceLastEval = false;

	public FScoreCalculator() {
		
	}
	
	/**
	 * Increment this f-score by a given expected value and guessed value.
	 * @param expected
	 * @param guessed
	 */
	public void increment(E expected, E guessed) {
		int pairCount = 1;
		Map<E,Integer> falsePositivesForGuessed = falsePositives.get(guessed);
		if (falsePositivesForGuessed==null) {
			falsePositivesForGuessed = new TreeMap<E, Integer>();
			falsePositives.put(guessed, falsePositivesForGuessed);
		}
		Integer pairCountObj = falsePositivesForGuessed.get(expected);
		if (pairCountObj!=null)
			pairCount = pairCountObj.intValue() + 1;
		falsePositivesForGuessed.put(expected, pairCount);
		
		pairCount = 1;
		Map<E,Integer> falseNegativesForExpected = falseNegatives.get(expected);
		if (falseNegativesForExpected==null) {
			falseNegativesForExpected = new TreeMap<E, Integer>();
			falseNegatives.put(expected, falseNegativesForExpected);
		}
		pairCountObj = falseNegativesForExpected.get(guessed);
		if (pairCountObj!=null)
			pairCount = pairCountObj.intValue() + 1;
		falseNegativesForExpected.put(guessed, pairCount);
		
		if (expected.equals(guessed)) {
			int truePositiveCount = 1;
			Integer truePositiveCountObj = truePositiveCounts.get(expected);
			if (truePositiveCountObj!=null)
				truePositiveCount = truePositiveCountObj.intValue() + 1;
			truePositiveCounts.put(expected, truePositiveCount);
		} else {
			// we didn't guess correctly that this was an X
			int falseNegativeCount = 1;
			Integer falseNegativeCountObj = falseNegativeCounts.get(expected);
			if (falseNegativeCountObj!=null)
				falseNegativeCount = falseNegativeCountObj.intValue() + 1;
			falseNegativeCounts.put(expected, falseNegativeCount);

			// we guessed that this was a Y, when it wasn't
			int falsePositiveCount = 1;
			Integer falsePositiveCountObj = falsePositiveCounts.get(guessed);
			if (falsePositiveCountObj!=null)
				falsePositiveCount = falsePositiveCountObj.intValue() + 1;
			falsePositiveCounts.put(guessed, falsePositiveCount);
		}
		outcomeSet.add(guessed);
		outcomeSet.add(expected);
		testCount++;
		updatedSinceLastEval = true;
	}
	
	void evaluate() {
		if (updatedSinceLastEval) {
			precisions = new TreeMap<E, Double>();
			recalls = new TreeMap<E, Double>();
			fScores = new TreeMap<E, Double>();

			for (E outcome : outcomeSet) {
				LOG.debug("Outcome: " + outcome);
				Integer truePositiveCountObj = truePositiveCounts.get(outcome);
				Integer falsePositiveCountObj = falsePositiveCounts.get(outcome);
				Integer falseNegativeCountObj = falseNegativeCounts.get(outcome);
				double truePositiveCount = truePositiveCountObj!=null ? truePositiveCountObj.doubleValue() : 0.0;
				double falsePositiveCount = falsePositiveCountObj!=null ? falsePositiveCountObj.doubleValue() : 0.0;
				double falseNegativeCount = falseNegativeCountObj!=null ? falseNegativeCountObj.doubleValue() : 0.0;
				LOG.debug("truePositiveCount: " + truePositiveCount);
				LOG.debug("falsePositiveCount: " + falsePositiveCount);
				if (LOG.isTraceEnabled()) {
					LOG.debug("False positives: ");
					Map<E,Integer> pairCounts = falsePositives.get(outcome);
					if (pairCounts != null) {
						for (E guessed : pairCounts.keySet()) {
							int pairCount = pairCounts.get(guessed);
							LOG.trace(outcome.toString() + " , " + guessed.toString() + ": " + pairCount);
						}
					}
				}
				
				LOG.debug("falseNegativeCount " + falseNegativeCount);
				if (LOG.isTraceEnabled()) {
					LOG.debug("False negatives: ");
					Map<E,Integer> pairCounts = falseNegatives.get(outcome);
					if (pairCounts != null) {
						for (E expected : pairCounts.keySet()) {
							int pairCount = pairCounts.get(expected);
							LOG.trace(outcome.toString() + " , " + expected.toString() + ": " + pairCount);
						}
					}
				}				
				
				double precision = 0;
				double recall = 0;
				double fScore = 0;
				
				if (truePositiveCount + falsePositiveCount > 0)
					precision = truePositiveCount / (truePositiveCount + falsePositiveCount);
				if (truePositiveCount + falseNegativeCount > 0)
					recall = truePositiveCount / (truePositiveCount + falseNegativeCount);
				if (precision + recall > 0)
					fScore = (2 * precision * recall) / (precision + recall);
				LOG.debug("Precision: " + precision);
				LOG.debug("Recall: " + recall);
				LOG.debug("F-score " + fScore);
				
				precisions.put(outcome, precision);
				recalls.put(outcome, recall);
				fScores.put(outcome, fScore);
				totalTruePositiveCount += truePositiveCount;
				totalFalsePositiveCount += falsePositiveCount;
				totalFalseNegativeCount += falseNegativeCount;
			}
			totalPrecision = totalTruePositiveCount / (totalTruePositiveCount + totalFalsePositiveCount);
			totalRecall = totalTruePositiveCount / (totalTruePositiveCount + totalFalseNegativeCount);
			totalFScore = (2 * totalPrecision * totalRecall) / (totalPrecision + totalRecall);
			LOG.info("Total tests: " + testCount);
			LOG.info("Total true positives: " + totalTruePositiveCount);
			LOG.info("Total false positives: " + totalFalsePositiveCount);
			LOG.info("Total false negatives: " + totalFalseNegativeCount);
			LOG.info("Total precision: " + totalPrecision);
			LOG.info("Total recall: " + totalRecall);
			LOG.info("Total f-score: " + totalFScore);
			
			updatedSinceLastEval = false;
		}
	}

	/**
	 * Get the count of false positives for a given outcome.
	 * @param outcome
	 * @return
	 */
	public int getFalsePositiveCount(E outcome) {
		int falsePositiveCount = 0;
		Integer falsePositiveCountObj = falsePositiveCounts.get(outcome);
		if (falsePositiveCountObj!=null)
			falsePositiveCount = falsePositiveCountObj.intValue();
		return falsePositiveCount;
	}
	
	/**
	 * Get the false positives for a given outcome.
	 * @param outcome
	 * @return
	 */
	public Map<E,Integer> getFalsePositives(E outcome) {
		return falsePositives.get(outcome);
	}
	
	/**
	 * False positives for all outcomes.
	 * @return
	 */
	public Map<E, Integer> getFalsePositiveCounts() {
		return falsePositiveCounts;
	}

	/**
	 * Get the count of false negatives for a given outcome.
	 * @param outcome
	 * @return
	 */
	public int getFalseNegativeCount(E outcome) {
		int falseNegativeCount = 0;
		Integer falseNegativeCountObj = falseNegativeCounts.get(outcome);
		if (falseNegativeCountObj!=null)
			falseNegativeCount = falseNegativeCountObj.intValue();
		return falseNegativeCount;
	}
	
	/**
	 * Get the false negatives for a given outcome.
	 * @param outcome
	 * @return
	 */
	public Map<E,Integer> getFalseNegatives(E outcome) {
		return falseNegatives.get(outcome);
	}

	/**
	 * False negatives for all outcomes.
	 * @return
	 */
	public Map<E, Integer> getFalseNegativeCounts() {
		return falseNegativeCounts;
	}

	/**
	 * Get the count of true positives for a given outcome.
	 * @param outcome
	 * @return
	 */
	public int getTruePositiveCount(E outcome) {
		int truePositiveCount = 0;
		Integer truePositiveCountObj = truePositiveCounts.get(outcome);
		if (truePositiveCountObj!=null)
			truePositiveCount = truePositiveCountObj.intValue();
		return truePositiveCount;
	}

	
	/**
	 * True positive counts for all outcomes.
	 * @return
	 */
	public Map<E, Integer> getTruePositiveCounts() {
		return truePositiveCounts;
	}
	
	/**
	 * The set of outcomes.
	 * @return
	 */
	public Set<E> getOutcomeSet() {
		return outcomeSet;
	}

	/**
	 * Get the precision for a particular outcome.
	 * @param outcome
	 * @return
	 */
	public double getPrecision(E outcome) {
		this.evaluate();
		return precisions.get(outcome);
	}
	
	/**
	 * Precisions for all outcomes.
	 * @return
	 */
	public Map<E, Double> getPrecisions() {
		this.evaluate();
		return precisions;
	}

	/**
	 * Get the recall for a particular outcome.
	 * @param outcome
	 * @return
	 */
	public double getRecall(E outcome) {
		this.evaluate();
		return recalls.get(outcome);
	}
	
	/**
	 * Recalls for all outcomes.
	 * @return
	 */
	public Map<E, Double> getRecalls() {
		this.evaluate();
		return recalls;
	}

	/**
	 * Get the f-score for a particular outcome.
	 * @param outcome
	 * @return
	 */
	public double getFScore(E outcome) {
		this.evaluate();
		return fScores.get(outcome);
	}
	
	
	/**
	 * F-scores for all outcomes.
	 * @return
	 */
	public Map<E, Double> getFScores() {
		this.evaluate();
		return fScores;
	}

	/**
	 * Total number of tests run.
	 * @return
	 */
	public int getTestCount() {
		return testCount;
	}

	/**
	 * Total precision.
	 * @return
	 */
	public double getTotalPrecision() {
		this.evaluate();
		return totalPrecision;
	}

	/**
	 * Total recall.
	 * @return
	 */
	public double getTotalRecall() {
		this.evaluate();
		return totalRecall;
	}

	/**
	 * Total f-score.
	 * @return
	 */
	public double getTotalFScore() {
		this.evaluate();
		return totalFScore;
	}

	public double getTotalTruePositiveCount() {
		return totalTruePositiveCount;
	}

	public double getTotalFalsePositiveCount() {
		return totalFalsePositiveCount;
	}

	public double getTotalFalseNegativeCount() {
		return totalFalseNegativeCount;
	}

	public void writeScoresToCSVFile(File fscoreFile) {
		try {
			fscoreFile.delete();
			fscoreFile.createNewFile();
			Writer fscoreFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fscoreFile, false),"UTF8"));
			try {
				this.writeScoresToCSV(fscoreFileWriter);
			} finally {
				fscoreFileWriter.flush();
				fscoreFileWriter.close();
			}
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}
	
	public void writeScoresToCSV(Writer fscoreFileWriter) {
		try {
			Set<E> outcomeSet = new TreeSet<E>();
			outcomeSet.addAll(this.getOutcomeSet());
			fscoreFileWriter.write("outcome,");
			for (E outcome : outcomeSet) {
				fscoreFileWriter.write(outcome.toString() + ",");
			}
			fscoreFileWriter.write("true+,false+,false-,precision,recall,f-score");
			fscoreFileWriter.write("\n");
			
			DecimalFormat df = (DecimalFormat) DecimalFormat.getNumberInstance(Locale.US);
			df.applyPattern("#.##");
			
			double totalPrecisionSum = 0;
			double totalRecallSum = 0;
			double totalFscoreSum = 0;
			for (E outcome : outcomeSet) {
				fscoreFileWriter.write(outcome.toString() + ",");
				for (E outcome2 : outcomeSet) {
					int falseNegativeCount = 0;
					Map<E,Integer> falseNegatives = this.getFalseNegatives(outcome);
					if (falseNegatives!=null&&falseNegatives.containsKey(outcome2)) {
						falseNegativeCount = this.getFalseNegatives(outcome).get(outcome2);
					}
					fscoreFileWriter.write(falseNegativeCount + ",");
				}
				fscoreFileWriter.write(df.format(this.getTruePositiveCount(outcome))+",");
				fscoreFileWriter.write(df.format(this.getFalsePositiveCount(outcome))+",");
				fscoreFileWriter.write(df.format(this.getFalseNegativeCount(outcome))+",");
				fscoreFileWriter.write(df.format(this.getPrecision(outcome)*100)+",");
				fscoreFileWriter.write(df.format(this.getRecall(outcome)*100)+",");
				fscoreFileWriter.write(df.format(this.getFScore(outcome)*100)+",");
				fscoreFileWriter.write("\n");
				
				totalPrecisionSum += this.getPrecision(outcome);
				totalRecallSum += this.getRecall(outcome);
				totalFscoreSum += this.getFScore(outcome);
			}
			
			fscoreFileWriter.write("TOTAL,");
			for (E outcome : outcomeSet) {
				outcome.hashCode();
				fscoreFileWriter.write(",");
			}
			fscoreFileWriter.write(df.format(this.getTotalTruePositiveCount())+",");
			fscoreFileWriter.write(df.format(this.getTotalFalsePositiveCount())+",");
			fscoreFileWriter.write(df.format(this.getTotalFalseNegativeCount())+",");
			fscoreFileWriter.write(df.format(this.getTotalPrecision()*100)+",");
			fscoreFileWriter.write(df.format(this.getTotalRecall()*100)+",");
			fscoreFileWriter.write(df.format(this.getTotalFScore()*100)+",");
			fscoreFileWriter.write("\n");
			
			fscoreFileWriter.write("AVERAGE,");
			for (E outcome : outcomeSet) {
				outcome.hashCode();
				fscoreFileWriter.write(",");
			}
			fscoreFileWriter.write(",");
			fscoreFileWriter.write(",");
			fscoreFileWriter.write(",");
			fscoreFileWriter.write(df.format((totalPrecisionSum/outcomeSet.size())*100)+",");
			fscoreFileWriter.write(df.format((totalRecallSum/outcomeSet.size())*100)+",");
			fscoreFileWriter.write(df.format((totalFscoreSum/outcomeSet.size())*100)+",");
			fscoreFileWriter.write("\n");
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}
}
