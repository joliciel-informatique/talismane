///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2015 Joliciel Informatique
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import com.joliciel.talismane.utils.CSVFile;
import com.joliciel.talismane.utils.CSVFormatter;
import com.joliciel.talismane.utils.StringUtils;

/**
 * In the case where the FScoreCalculator is used to generate many confusion
 * matrices for different configurations, where each configuration has a
 * different file name but is located in the same directory, this class
 * generates a single CSV file per directory comparing the accuracy scores of
 * all of the different matrices.
 * 
 * @author Assaf Urieli
 *
 */
public class AccuracyComparison {
  private static CSVFormatter CSV = new CSVFormatter();
  private static NumberFormat format = NumberFormat.getInstance(Locale.ENGLISH);
  private static String fileExtension = null;
  private static String fileExtensionBase = null;
  private static String fileExtensionSuffix = null;

  private static Map<String, AccuracyStats> statsMap = new TreeMap<String, AccuracyStats>();

  private static Map<String, Map<String, Map<String, Integer>>> confusionMatrixes = new TreeMap<String, Map<String, Map<String, Integer>>>();

  /**
   */
  public static void main(String[] args) throws Exception {
    Map<String, String> argMap = StringUtils.convertArgs(args);

    String dirPath = null;

    for (String argName : argMap.keySet()) {
      String argValue = argMap.get(argName);
      if (argName.equals("dir")) {
        dirPath = argValue;
      } else if (argName.equals("extension")) {
        fileExtension = argValue;
      } else {
        throw new RuntimeException("Unknown option: " + argName);
      }
    }

    if (dirPath == null)
      throw new RuntimeException("dir is required");
    if (fileExtension == null)
      throw new RuntimeException("extension is required");

    if (fileExtension.indexOf('.') > 0) {
      fileExtensionBase = fileExtension.substring(0, fileExtension.lastIndexOf('.'));
      fileExtensionSuffix = fileExtension.substring(fileExtension.lastIndexOf('.'));
    } else {
      fileExtensionBase = fileExtension;
      fileExtensionSuffix = ".csv";
    }

    File directory = new File(dirPath);
    writeAccuracy(directory);

    File[] subdirs = directory.listFiles();

    for (File subdir : subdirs) {
      if (subdir.isDirectory()) {
        writeAccuracy(subdir);
      }
    }

    Writer writer = new BufferedWriter(new OutputStreamWriter(
        new FileOutputStream(new File(directory, directory.getName() + fileExtensionBase + "_totalAccuracy" + fileExtensionSuffix)), "UTF-8"));
    writer.write(CSV.format("File") + CSV.format("Accuracy") + CSV.format("good") + CSV.format("bad") + CSV.format("total"));

    writer.write("\n");

    for (String suffix : statsMap.keySet()) {
      writer.write(CSV.format(suffix));
      AccuracyStats stats = statsMap.get(suffix);
      double LAS = ((double) stats.good / (double) (stats.good + stats.bad)) * 100.0;

      writer.write(CSV.format(LAS));
      writer.write(CSV.format(stats.good));
      writer.write(CSV.format(stats.bad));
      writer.write(CSV.format(stats.good + stats.bad));

      writer.write("\n");
    }
    writer.flush();
    writer.close();

    for (String fileSuffix : confusionMatrixes.keySet()) {
      Map<String, Map<String, Integer>> confusionMatrix = confusionMatrixes.get(fileSuffix);
      Map<String, FScoreStats> fscoreMap = new HashMap<String, AccuracyComparison.FScoreStats>();

      for (String rowName : confusionMatrix.keySet()) {
        FScoreStats fscoreStats = new FScoreStats();
        fscoreMap.put(rowName, fscoreStats);
      }

      for (String rowName : confusionMatrix.keySet()) {
        Map<String, Integer> rowMap = confusionMatrix.get(rowName);
        FScoreStats fscoreStats = fscoreMap.get(rowName);
        fscoreStats.truePos = rowMap.get(rowName);

        for (String columnName : rowMap.keySet()) {
          if (!rowName.equals(columnName)) {
            fscoreStats.falseNeg += rowMap.get(columnName);
            FScoreStats badGuyStats = fscoreMap.get(columnName);
            badGuyStats.falsePos += rowMap.get(columnName);
          }
        }
      }

      Writer matrixWriter = new BufferedWriter(new OutputStreamWriter(
          new FileOutputStream(new File(directory, directory.getName() + fileExtensionBase + fileSuffix + fileExtensionSuffix)), "UTF-8"));

      matrixWriter.write(CSV.format("outcome"));
      for (String rowName : confusionMatrix.keySet()) {
        matrixWriter.write(CSV.format(rowName));
      }
      matrixWriter.write(CSV.format("true+") + CSV.format("false+") + CSV.format("false-"));
      matrixWriter.write(CSV.format("precision") + CSV.format("recall") + CSV.format("f-score"));

      matrixWriter.write("\n");

      FScoreStats totalStats = new FScoreStats();
      for (String rowName : confusionMatrix.keySet()) {
        Map<String, Integer> rowMap = confusionMatrix.get(rowName);
        FScoreStats fscoreStats = fscoreMap.get(rowName);
        matrixWriter.write(CSV.format(rowName));
        for (String columnName : rowMap.keySet()) {
          matrixWriter.write(CSV.format(rowMap.get(columnName)));
        }
        matrixWriter.write(CSV.format(fscoreStats.truePos));
        matrixWriter.write(CSV.format(fscoreStats.falsePos));
        matrixWriter.write(CSV.format(fscoreStats.falseNeg));
        matrixWriter.write(CSV.format(fscoreStats.getPrecision() * 100.0));
        matrixWriter.write(CSV.format(fscoreStats.getRecall() * 100.0));
        matrixWriter.write(CSV.format(fscoreStats.getFScore() * 100.0));
        matrixWriter.write("\n");

        totalStats.truePos += fscoreStats.truePos;
        totalStats.falsePos += fscoreStats.falsePos;
        totalStats.falseNeg += fscoreStats.falseNeg;

      }
      matrixWriter.write(CSV.format("TOTAL"));
      for (@SuppressWarnings("unused")
      String rowName : confusionMatrix.keySet()) {
        matrixWriter.write(CSV.format(""));
      }
      matrixWriter.write(CSV.format(totalStats.truePos));
      matrixWriter.write(CSV.format(totalStats.falsePos));
      matrixWriter.write(CSV.format(totalStats.falseNeg));
      matrixWriter.write(CSV.format(totalStats.getPrecision() * 100.0));
      matrixWriter.write(CSV.format(totalStats.getRecall() * 100.0));
      matrixWriter.write(CSV.format(totalStats.getFScore() * 100.0));
      matrixWriter.write("\n");

      matrixWriter.flush();
      matrixWriter.close();
    }
  }

  private static void writeAccuracy(File directory) throws Exception {
    File[] files = directory.listFiles(new FilenameFilter() {

      @Override
      public boolean accept(File dir, String name) {
        return name.endsWith(fileExtension);
      }
    });

    Writer writer = new BufferedWriter(new OutputStreamWriter(
        new FileOutputStream(new File(directory, directory.getName() + fileExtensionBase + "_accuracy" + fileExtensionSuffix)), "UTF-8"));
    writer.write(CSV.format("File") + CSV.format("Accuracy") + CSV.format("good") + CSV.format("bad") + CSV.format("total"));

    writer.write("\n");

    for (File file : files) {
      CSVFile csvFile = new CSVFile(file, "UTF-8");

      String fileSuffix = file.getName().substring(directory.getName().length() + 1);
      AccuracyStats stats = statsMap.get(fileSuffix);
      if (stats == null) {
        stats = new AccuracyStats();
        statsMap.put(fileSuffix, stats);
      }

      writer.write(CSV.format(file.getName()));
      int truePosColumn = -1;
      int falseNegColumn = -1;
      for (int i = 0; i < csvFile.numColumns(0); i++) {
        String cell = csvFile.getValue(0, i);
        if (cell.equals("true+"))
          truePosColumn = i;
        else if (cell.equals("false-"))
          falseNegColumn = i;
      }
      int totalRow = -1;

      for (int i = 1; i < csvFile.numRows(); i++) {
        String cell = csvFile.getValue(i, 0);

        if (cell.equals("TOTAL")) {
          totalRow = i;
          break;
        }
      }

      double totalGood = format.parse(csvFile.getValue(totalRow, truePosColumn)).intValue();
      double totalBad = format.parse(csvFile.getValue(totalRow, falseNegColumn)).intValue();

      double accuracy = (totalGood / (totalGood + totalBad)) * 100.0;

      writer.write(CSV.format(accuracy));
      writer.write(CSV.format(totalGood));
      writer.write(CSV.format(totalBad));
      writer.write(CSV.format(totalGood + totalBad));

      writer.write("\n");
      writer.flush();

      stats.good += totalGood;
      stats.bad += totalBad;

      Map<String, Map<String, Integer>> confusionMatrix = confusionMatrixes.get(fileSuffix);
      if (confusionMatrix == null) {
        confusionMatrix = new TreeMap<String, Map<String, Integer>>();
        confusionMatrixes.put(fileSuffix, confusionMatrix);
      }
      for (int i = 1; i < totalRow; i++) {
        String rowName = csvFile.getValue(i, 0);
        Map<String, Integer> rowMap = confusionMatrix.get(rowName);
        if (rowMap == null) {
          rowMap = new TreeMap<String, Integer>();
          confusionMatrix.put(rowName, rowMap);
        }
        for (int j = 1; j < truePosColumn; j++) {
          String columnName = csvFile.getValue(0, j);
          Integer countObj = rowMap.get(columnName);
          int count = countObj == null ? 0 : countObj.intValue();
          try {
            count += Integer.parseInt(csvFile.getValue(i, j));
          } catch (NumberFormatException e) {
            // do nothing
          }
          rowMap.put(columnName, count);
        }
      }
    }
    writer.close();

  }

  private static final class AccuracyStats {
    public int good;
    public int bad;
  }

  private static final class FScoreStats {
    public int truePos;
    public int falsePos;
    public int falseNeg;

    public double getPrecision() {
      return (double) truePos / ((double) truePos + falsePos);
    }

    public double getRecall() {
      return (double) truePos / ((double) truePos + falseNeg);
    }

    public double getFScore() {
      return (2 * getPrecision() * getRecall()) / (getPrecision() + getRecall());
    }

  }
}
