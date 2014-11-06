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
package com.joliciel.talismane.stats;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.joliciel.talismane.utils.CSVFormatter;
import com.joliciel.talismane.utils.JolicielException;

/**
 * Calculates the f-score for a given classification task.
 * @author Assaf Urieli
 *
 */
public class FScoreCalculator<E> {
	private static final Log LOG = LogFactory.getLog(FScoreCalculator.class);
	private static final CSVFormatter CSV = new CSVFormatter();
	Map<E,Map<E,Integer>> falsePositives = new HashMap<E,Map<E,Integer>>();
	Map<E,Map<E,Integer>> falseNegatives = new HashMap<E,Map<E,Integer>>();
	Map<E,Integer> truePositiveCounts = new HashMap<E, Integer>();
	Map<E,Integer> falsePositiveCounts = new HashMap<E, Integer>();
	Map<E,Integer> falseNegativeCounts = new HashMap<E, Integer>();
	
	Set<E> outcomeSet = new TreeSet<E>();
	
	Map<E,Double> precisions = new HashMap<E, Double>();
	Map<E,Double> recalls = new HashMap<E, Double>();
	Map<E,Double> fScores = new HashMap<E, Double>();
	
	int testCount = 0;
	double totalPrecision = 0.0;
	double totalRecall = 0.0;
	double totalFScore = 0.0;
	
	int totalTruePositiveCount = 0;
	int totalFalsePositiveCount = 0;
	int totalFalseNegativeCount = 0;
	
	boolean updatedSinceLastEval = true;
	
	Object label = null;
	
	public FScoreCalculator(Object label) {
		this.label = label;
	}
	
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
			falsePositivesForGuessed = new HashMap<E, Integer>();
			falsePositives.put(guessed, falsePositivesForGuessed);
		}
		Integer pairCountObj = falsePositivesForGuessed.get(expected);
		if (pairCountObj!=null)
			pairCount = pairCountObj.intValue() + 1;
		falsePositivesForGuessed.put(expected, pairCount);
		
		pairCount = 1;
		Map<E,Integer> falseNegativesForExpected = falseNegatives.get(expected);
		if (falseNegativesForExpected==null) {
			falseNegativesForExpected = new HashMap<E, Integer>();
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
			LOG.info("###F-score calculations " + (label==null? "" : " for " + label.toString()));
			precisions = new HashMap<E, Double>();
			recalls = new HashMap<E, Double>();
			fScores = new HashMap<E, Double>();

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
			totalPrecision = (double) totalTruePositiveCount / ((double) totalTruePositiveCount + (double) totalFalsePositiveCount);
			totalRecall = (double) totalTruePositiveCount / ((double) totalTruePositiveCount + (double) totalFalseNegativeCount);
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
		if (precisions.containsKey(outcome))
			return precisions.get(outcome);
		else
			return 0;
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
		if (recalls.containsKey(outcome))
			return recalls.get(outcome);
		else
			return 0;
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
		if (fScores.containsKey(outcome))
			return fScores.get(outcome);
		else
			return 0;
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

	public int getTotalTruePositiveCount() {
		this.evaluate();
		return totalTruePositiveCount;
	}

	public int getTotalFalsePositiveCount() {
		this.evaluate();
		return totalFalsePositiveCount;
	}

	public int getTotalFalseNegativeCount() {
		this.evaluate();
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
	/**
	 * The total accuracy for this confusion matrix.
	 * @return
	 */
	public double getAccuracy() {
		this.evaluate();
		double totalCount = (double) testCount;
		double totalAccuracy = (double) totalTruePositiveCount / totalCount;
		return totalAccuracy;
	}
	
	/**
	 * Return Cohen's kappa for this confusion matrix.
	 * @return
	 */
	public double getKappa() {
		this.evaluate();
		double totalCount = (double) testCount;
		double totalAccuracy = (double) totalTruePositiveCount / totalCount;
		
		// random accuracy is the sum of products for marginal accuracies for each label
		double randomAccuracy = 0.0;
		for (E outcome : outcomeSet) {
			Integer truePositiveCountObj = truePositiveCounts.get(outcome);
			Integer falsePositiveCountObj = falsePositiveCounts.get(outcome);
			Integer falseNegativeCountObj = falseNegativeCounts.get(outcome);
			double truePositiveCount = truePositiveCountObj!=null ? truePositiveCountObj.doubleValue() : 0.0;
			double falsePositiveCount = falsePositiveCountObj!=null ? falsePositiveCountObj.doubleValue() : 0.0;
			double falseNegativeCount = falseNegativeCountObj!=null ? falseNegativeCountObj.doubleValue() : 0.0;
			double marginalRandomAccuracy = ((truePositiveCount + falsePositiveCount) / totalCount)
				* ((truePositiveCount + falseNegativeCount) / totalCount);
			randomAccuracy += marginalRandomAccuracy;
		}
		
		double kappa = (totalAccuracy - randomAccuracy) / (1 - randomAccuracy);
		return kappa;
	}
	
	public void writeScoresToCSV(Writer fscoreFileWriter) {
		try {
			Set<E> outcomeSet = new TreeSet<E>();
			outcomeSet.addAll(this.getOutcomeSet());
			fscoreFileWriter.write(CSV.format("outcome"));
			for (E outcome : outcomeSet) {
				fscoreFileWriter.write(CSV.format(outcome.toString()));
			}
			fscoreFileWriter.write(CSV.format("true+")
					+ CSV.format("false+")
					+ CSV.format("false-")
					+ CSV.format("precision")
					+ CSV.format("recall")
					+ CSV.format("f-score"));
			fscoreFileWriter.write("\n");
			
			double totalPrecisionSum = 0;
			double totalRecallSum = 0;
			double totalFscoreSum = 0;
			for (E outcome : outcomeSet) {
				fscoreFileWriter.write(CSV.format(outcome.toString()));
				for (E outcome2 : outcomeSet) {
					int falseNegativeCount = 0;
					Map<E,Integer> falseNegatives = this.getFalseNegatives(outcome);
					if (falseNegatives!=null&&falseNegatives.containsKey(outcome2)) {
						falseNegativeCount = this.getFalseNegatives(outcome).get(outcome2);
					}
					fscoreFileWriter.write(CSV.format(falseNegativeCount));
				}
				fscoreFileWriter.write(CSV.format(this.getTruePositiveCount(outcome)));
				fscoreFileWriter.write(CSV.format(this.getFalsePositiveCount(outcome)));
				fscoreFileWriter.write(CSV.format(this.getFalseNegativeCount(outcome)));
				fscoreFileWriter.write(CSV.format(this.getPrecision(outcome)*100));
				fscoreFileWriter.write(CSV.format(this.getRecall(outcome)*100));
				fscoreFileWriter.write(CSV.format(this.getFScore(outcome)*100));
				fscoreFileWriter.write("\n");
				
				totalPrecisionSum += this.getPrecision(outcome);
				totalRecallSum += this.getRecall(outcome);
				totalFscoreSum += this.getFScore(outcome);
			}
			
			fscoreFileWriter.write(CSV.format("TOTAL"));
			for (E outcome : outcomeSet) {
				outcome.hashCode();
				fscoreFileWriter.write(CSV.format(""));
			}
			fscoreFileWriter.write(CSV.format(this.getTotalTruePositiveCount()));
			fscoreFileWriter.write(CSV.format(this.getTotalFalsePositiveCount()));
			fscoreFileWriter.write(CSV.format(this.getTotalFalseNegativeCount()));
			fscoreFileWriter.write(CSV.format(this.getTotalPrecision()*100));
			fscoreFileWriter.write(CSV.format(this.getTotalRecall()*100));
			fscoreFileWriter.write(CSV.format(this.getTotalFScore()*100));
			fscoreFileWriter.write("\n");
			
			fscoreFileWriter.write(CSV.format("AVERAGE"));
			for (E outcome : outcomeSet) {
				outcome.hashCode();
				fscoreFileWriter.write(CSV.format(""));
			}
			fscoreFileWriter.write(CSV.format(""));
			fscoreFileWriter.write(CSV.format(""));
			fscoreFileWriter.write(CSV.format(""));
			fscoreFileWriter.write(CSV.format((totalPrecisionSum/outcomeSet.size())*100));
			fscoreFileWriter.write(CSV.format((totalRecallSum/outcomeSet.size())*100));
			fscoreFileWriter.write(CSV.format((totalFscoreSum/outcomeSet.size())*100));
			fscoreFileWriter.write("\n");
			
			fscoreFileWriter.write(CSV.format("ACCURACY"));
			for (E outcome : outcomeSet) {
				outcome.hashCode();
				fscoreFileWriter.write(CSV.format(""));
			}
			fscoreFileWriter.write(CSV.format(""));
			fscoreFileWriter.write(CSV.format(""));
			fscoreFileWriter.write(CSV.format(""));
			fscoreFileWriter.write(CSV.format(this.getAccuracy()*100));
			fscoreFileWriter.write("\n");

			fscoreFileWriter.write(CSV.format("KAPPA"));
			for (E outcome : outcomeSet) {
				outcome.hashCode();
				fscoreFileWriter.write(CSV.format(""));
			}
			fscoreFileWriter.write(CSV.format(""));
			fscoreFileWriter.write(CSV.format(""));
			fscoreFileWriter.write(CSV.format(""));
			fscoreFileWriter.write(CSV.format(this.getKappa()*100));
			fscoreFileWriter.write("\n");

		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}
	
	public static void main(String[] args) throws Exception {
		File directory = new File(args[0]);
		String prefix = args[1];
		String suffix = args[2];
		Writer csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(directory, prefix + "A" + suffix + ".csv"), false),"UTF8"));
		combineCrossValidationResults(directory, prefix, suffix, csvFileWriter);
	}
	
	/**
	 * Combine the results of n cross validation results into a single f-score file.
	 * @param directory
	 * @param prefix
	 * @param suffix
	 * @param csvFileWriter
	 */
	static void combineCrossValidationResults(File directory, String prefix, String suffix, Writer csvFileWriter) {
		try {
			File[] files = directory.listFiles();
			Map<Integer,Map<String,FScoreStats>> fileStatsMap = new HashMap<Integer, Map<String,FScoreStats>>();
			for (File file : files) {
				if (file.getName().startsWith(prefix) && file.getName().endsWith(suffix)) {
					int index = Integer.parseInt(file.getName().substring(prefix.length(),prefix.length()+1));
					Map<String,FScoreStats> statsMap = new HashMap<String, FScoreCalculator.FScoreStats>();
					fileStatsMap.put(index, statsMap);
					Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8")));

					boolean firstLine = true;
					int truePositivePos = -1;
					
					while (scanner.hasNextLine()) {
						String line = scanner.nextLine();
						List<String> cells = CSV.getCSVCells(line);
						if (firstLine) {
							int i = 0;
							for (String cell : cells) {
								if (cell.equals("true+")) {
									truePositivePos = i;
									break;
								}
								i++;
							}
							if (truePositivePos<0) {
								throw new JolicielException("Couldn't find true+ on first line");
							}
							firstLine = false;
						} else {
							FScoreStats stats = new FScoreStats();
							String outcome = cells.get(0);
							stats.outcome = outcome;
							if (outcome.equals("AVERAGE"))
								break;
							stats.truePos = Integer.parseInt(cells.get(truePositivePos));
							stats.falsePos = Integer.parseInt(cells.get(truePositivePos+1));
							stats.falseNeg = Integer.parseInt(cells.get(truePositivePos+2));
							stats.precision = Double.parseDouble(cells.get(truePositivePos+3));
							stats.recall = Double.parseDouble(cells.get(truePositivePos+4));
							stats.fScore = Double.parseDouble(cells.get(truePositivePos+5));
							statsMap.put(outcome, stats);
						} // firstLine?
					} // has more lines
					scanner.close();
				} // file in current series
			} // next file
			
			int numFiles = fileStatsMap.size();
			if (numFiles==0) {
				throw new JolicielException("No files found matching prefix and suffix provided");
			}
			Map<String,DescriptiveStatistics> descriptiveStatsMap = new HashMap<String, DescriptiveStatistics>();
			Map<String,FScoreStats> outcomeStats = new HashMap<String, FScoreCalculator.FScoreStats>();
			Set<String> outcomes = new TreeSet<String>();
			for (Map<String,FScoreStats> statsMap : fileStatsMap.values()) {
				for (FScoreStats stats : statsMap.values()) {
					DescriptiveStatistics fScoreStats = descriptiveStatsMap.get(stats.outcome + "fScore");
					if (fScoreStats==null) {
						fScoreStats = new DescriptiveStatistics();
						descriptiveStatsMap.put(stats.outcome + "fScore", fScoreStats);
					}
					fScoreStats.addValue(stats.fScore);
					DescriptiveStatistics precisionStats = descriptiveStatsMap.get(stats.outcome + "precision");
					if (precisionStats==null) {
						precisionStats = new DescriptiveStatistics();
						descriptiveStatsMap.put(stats.outcome + "precision", precisionStats);
					}
					precisionStats.addValue(stats.precision);
					DescriptiveStatistics recallStats = descriptiveStatsMap.get(stats.outcome + "recall");
					if (recallStats==null) {
						recallStats = new DescriptiveStatistics();
						descriptiveStatsMap.put(stats.outcome + "recall", recallStats);
					}
					recallStats.addValue(stats.recall);
					
					FScoreStats outcomeStat = outcomeStats.get(stats.outcome);
					if (outcomeStat==null) {
						outcomeStat = new FScoreStats();
						outcomeStat.outcome = stats.outcome;
						outcomeStats.put(stats.outcome, outcomeStat);
					}
					outcomeStat.truePos += stats.truePos;
					outcomeStat.falsePos += stats.falsePos;
					outcomeStat.falseNeg += stats.falseNeg;
					
					outcomes.add(stats.outcome);
				}
			}

			csvFileWriter.write(CSV.format(prefix+suffix));
			csvFileWriter.write("\n");
			csvFileWriter.write(CSV.format("outcome"));
			csvFileWriter.write(CSV.format("true+")
					+ CSV.format("false+")
					+ CSV.format("false-")
					+ CSV.format("tot precision")
					+ CSV.format("avg precision")
					+ CSV.format("dev precision")
					+ CSV.format("tot recall")
					+ CSV.format("avg recall")
					+ CSV.format("dev recall")
					+ CSV.format("tot f-score")
					+ CSV.format("avg f-score")
					+ CSV.format("dev f-score")
					+ "\n"
			);
			
			for (String outcome : outcomes) {
				csvFileWriter.write(CSV.format(outcome));
				FScoreStats outcomeStat = outcomeStats.get(outcome);
				DescriptiveStatistics fScoreStats = descriptiveStatsMap.get(outcome + "fScore");
				DescriptiveStatistics precisionStats = descriptiveStatsMap.get(outcome + "precision");
				DescriptiveStatistics recallStats = descriptiveStatsMap.get(outcome + "recall");
				outcomeStat.calculate();
				csvFileWriter.write(CSV.format(outcomeStat.truePos));
				csvFileWriter.write(CSV.format(outcomeStat.falsePos));
				csvFileWriter.write(CSV.format(outcomeStat.falseNeg));
				csvFileWriter.write(CSV.format(outcomeStat.precision * 100));
				csvFileWriter.write(CSV.format(precisionStats.getMean()));
				csvFileWriter.write(CSV.format(precisionStats.getStandardDeviation()));
				csvFileWriter.write(CSV.format(outcomeStat.recall * 100));
				csvFileWriter.write(CSV.format(recallStats.getMean()));
				csvFileWriter.write(CSV.format(recallStats.getStandardDeviation()));
				csvFileWriter.write(CSV.format(outcomeStat.fScore * 100));
				csvFileWriter.write(CSV.format(fScoreStats.getMean()));
				csvFileWriter.write(CSV.format(fScoreStats.getStandardDeviation()));
				csvFileWriter.write("\n");
				csvFileWriter.flush();
			}
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}
	
	private static final class FScoreStats {
		String outcome;
		int truePos;
		int falsePos;
		int falseNeg;
		double precision;
		double recall;
		double fScore;
		
		public void calculate() {
			if (truePos + falsePos > 0)
				precision = (double) truePos / (double) (truePos + falsePos);
			if (truePos + falseNeg > 0)
				recall = (double) truePos / (double) (truePos + falseNeg);
			if (precision + recall > 0)
				fScore = (2 * precision * recall) / (precision + recall);
		}
	}
}
