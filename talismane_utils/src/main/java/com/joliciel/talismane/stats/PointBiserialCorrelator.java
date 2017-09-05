package com.joliciel.talismane.stats;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.joliciel.talismane.utils.CSVFormatter;

public class PointBiserialCorrelator {
  private static CSVFormatter CSV = new CSVFormatter(4);
  private static NumberFormat format = NumberFormat.getInstance(Locale.ENGLISH);
  private static final Log LOG = LogFactory.getLog(PointBiserialCorrelator.class);

  /**
   * @param args
   */
  public static void main(String[] args) throws Exception {
    Map<String, String> argMap = convertArgs(args);
    String refFilePath = null;
    String testFilePath = null;
    int sanityCheckIndex = 0;
    int measurementIndex = 0;
    int labelIndex = -1;
    int[] testIndexes = null;
    Map<Integer, String> skipRules = new HashMap<Integer, String>();
    for (String argName : argMap.keySet()) {
      String argValue = argMap.get(argName);
      if (argName.equals("refFile")) {
        refFilePath = argValue;
      } else if (argName.equals("testFile")) {
        testFilePath = argValue;
      } else if (argName.equals("testIndexes")) {
        String[] parts = argValue.split(",");
        testIndexes = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
          testIndexes[i] = Integer.parseInt(parts[i]);
        }
      } else if (argName.equals("sanityCheckIndex")) {
        sanityCheckIndex = Integer.parseInt(argValue);
      } else if (argName.equals("measurementIndex")) {
        measurementIndex = Integer.parseInt(argValue);
      } else if (argName.equals("labelIndex")) {
        labelIndex = Integer.parseInt(argValue);
      } else if (argName.equals("skipRules")) {
        String[] parts = argValue.split(",");

        for (String part : parts) {
          String[] innerParts = part.split("=");
          skipRules.put(Integer.parseInt(innerParts[0]), innerParts[1]);
        }
      }
    }

    File refFile = new File(refFilePath);
    File testFile = new File(testFilePath);

    String baseName = testFile.getName();
    if (baseName.indexOf('.') >= 0) {
      baseName = baseName.substring(0, baseName.lastIndexOf('.'));
    }

    File outFile = new File(testFile.getParentFile(), baseName + "_correlation.csv");
    try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile, false), "UTF-8"));
        Scanner refFileScanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(refFile), "UTF-8")));
        Scanner testFileScanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(testFile), "UTF-8")));) {

      int lineCount = 0;

      int trueCount = 0;
      int falseCount = 0;
      int totalCount = 0;
      DescriptiveStatistics trueStats = new DescriptiveStatistics();
      DescriptiveStatistics falseStats = new DescriptiveStatistics();
      DescriptiveStatistics totalStats = new DescriptiveStatistics();

      Map<String, PointBiserialStatistic> statMap = new TreeMap<String, PointBiserialCorrelator.PointBiserialStatistic>();

      while (refFileScanner.hasNextLine()) {
        lineCount++;
        LOG.debug("line: " + lineCount);
        if (!testFileScanner.hasNextLine()) {
          throw new RuntimeException("Not enough lines on testFile: line " + lineCount);
        }

        String refLine = refFileScanner.nextLine();
        String testLine = testFileScanner.nextLine();

        if (refLine.length() == 0) {
          if (testLine.length() != 0) {
            throw new RuntimeException("Mismatch on test line: " + lineCount);
          }
          continue;
        }

        String[] refParts = refLine.split("\t");
        String[] testParts = testLine.split("\t");

        String refSanityCheck = refParts[sanityCheckIndex];
        String testSanityCheck = testParts[sanityCheckIndex];

        if (!refSanityCheck.equals(testSanityCheck)) {
          throw new RuntimeException("Mismatch on test line: " + lineCount);
        }

        boolean shouldSkip = false;
        for (int skipIndex : skipRules.keySet()) {
          String skipValue = skipRules.get(skipIndex);
          if (refParts[skipIndex].equals(skipValue)) {
            LOG.debug("Skipping line " + lineCount + ": " + refLine);
            shouldSkip = true;
            break;
          }
        }
        if (shouldSkip)
          continue;

        boolean testTrue = true;
        for (int testIndex : testIndexes) {
          if (!refParts[testIndex].equals(testParts[testIndex])) {
            testTrue = false;
            break;
          }
        }

        double measurement = 0;
        String measurementString = testParts[measurementIndex];
        if (!measurementString.equals("_"))
          measurement = format.parse(measurementString).doubleValue();

        if (measurement == 0)
          continue;

        if (testTrue) {
          trueCount++;
          trueStats.addValue(measurement);
        } else {
          falseCount++;
          falseStats.addValue(measurement);
        }
        totalCount++;
        totalStats.addValue(measurement);

        if (labelIndex >= 0) {
          String label = refParts[labelIndex];
          PointBiserialStatistic stat = statMap.get(label);
          if (stat == null) {
            stat = new PointBiserialStatistic();
            statMap.put(label, stat);
          }

          if (testTrue) {
            stat.trueStats.addValue(measurement);
          } else {
            stat.falseStats.addValue(measurement);
          }
          stat.totalStats.addValue(measurement);
        }

      } // next line

      writer.write(CSV.format("Ref") + CSV.format(refFile.getName()) + "\n");
      writer.write(CSV.format("Test") + CSV.format(testFile.getName()) + "\n");
      writer.write(CSV.format("ALL") + CSV.format("totalCount") + CSV.format(totalCount) + "\n");
      writer.write(CSV.format("ALL") + CSV.format("trueCount") + CSV.format(trueCount) + "\n");
      writer.write(CSV.format("ALL") + CSV.format("trueMean") + CSV.format(trueStats.getMean()) + "\n");
      writer.write(CSV.format("ALL") + CSV.format("trueStdDev") + CSV.format(trueStats.getStandardDeviation()) + "\n");
      writer.write(CSV.format("ALL") + CSV.format("falseCount") + CSV.format(falseCount) + "\n");
      writer.write(CSV.format("ALL") + CSV.format("falseMean") + CSV.format(falseStats.getMean()) + "\n");
      writer.write(CSV.format("ALL") + CSV.format("falseStdDev") + CSV.format(falseStats.getStandardDeviation()) + "\n");
      writer.write(CSV.format("ALL") + CSV.format("mean") + CSV.format(totalStats.getMean()) + "\n");
      writer.write(CSV.format("ALL") + CSV.format("stdDev") + CSV.format(totalStats.getStandardDeviation()) + "\n");

      double correlation = (trueStats.getMean() - falseStats.getMean()) / totalStats.getStandardDeviation();
      correlation *= Math.sqrt(((double) trueCount * (double) falseCount) / ((double) totalCount * (double) totalCount));
      writer.write(CSV.format("ALL") + CSV.format("correlation") + CSV.format(correlation) + "\n");

      if (labelIndex > 0) {
        for (String label : statMap.keySet()) {
          PointBiserialStatistic stat = statMap.get(label);
          writer.write(CSV.format(label) + CSV.format("#########") + "\n");
          writer.write(CSV.format(label) + CSV.format("totalCount") + CSV.format(stat.totalStats.getN()) + "\n");
          writer.write(CSV.format(label) + CSV.format("trueCount") + CSV.format(stat.trueStats.getN()) + "\n");
          writer.write(CSV.format(label) + CSV.format("trueMean") + CSV.format(stat.trueStats.getMean()) + "\n");
          writer.write(CSV.format(label) + CSV.format("trueStdDev") + CSV.format(stat.trueStats.getStandardDeviation()) + "\n");
          writer.write(CSV.format(label) + CSV.format("falseCount") + CSV.format(stat.falseStats.getN()) + "\n");
          writer.write(CSV.format(label) + CSV.format("falseMean") + CSV.format(stat.falseStats.getMean()) + "\n");
          writer.write(CSV.format(label) + CSV.format("falseStdDev") + CSV.format(stat.falseStats.getStandardDeviation()) + "\n");
          writer.write(CSV.format(label) + CSV.format("mean") + CSV.format(stat.totalStats.getMean()) + "\n");
          writer.write(CSV.format(label) + CSV.format("stdDev") + CSV.format(stat.totalStats.getStandardDeviation()) + "\n");

          double labelCorrelation = (stat.trueStats.getMean() - stat.falseStats.getMean()) / stat.totalStats.getStandardDeviation();
          labelCorrelation *= Math
              .sqrt(((double) stat.trueStats.getN() * (double) stat.falseStats.getN()) / ((double) stat.totalStats.getN() * (double) stat.totalStats.getN()));
          writer.write(CSV.format(label) + CSV.format("correlation") + CSV.format(labelCorrelation) + "\n");
        }
      }
      writer.flush();
      writer.close();
    }
  }

  private static final class PointBiserialStatistic {
    DescriptiveStatistics trueStats = new DescriptiveStatistics();
    DescriptiveStatistics falseStats = new DescriptiveStatistics();
    DescriptiveStatistics totalStats = new DescriptiveStatistics();
  }

  public static Map<String, String> convertArgs(String[] args) {
    Map<String, String> argMap = new HashMap<String, String>();
    for (String arg : args) {
      int equalsPos = arg.indexOf('=');
      String argName = arg.substring(0, equalsPos);
      String argValue = arg.substring(equalsPos + 1);
      argMap.put(argName, argValue);
    }
    return argMap;
  }
}
