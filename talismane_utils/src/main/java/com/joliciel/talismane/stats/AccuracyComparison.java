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
import java.util.Locale;
import java.util.Map;

import com.joliciel.talismane.utils.CSVFile;
import com.joliciel.talismane.utils.CSVFormatter;
import com.joliciel.talismane.utils.StringUtils;

/**
 * In the case where the FScoreCalculator is used to generate many confusion
 * matrices for different configurations, where each configuration has a
 * different file name but is located in the same directory, this class
 * generates a single CSV file per directory comparing the accuracy scores of
 * all of the different matrices.<br>
 * <br>
 * If rows are found indicating it is a parser f-score, an additional column is
 * added for the UAS.
 * 
 * @author Assaf Urieli
 *
 */
public class AccuracyComparison {
  private static CSVFormatter CSV = new CSVFormatter();
  private static NumberFormat format = NumberFormat.getInstance(Locale.ENGLISH);
  private static String fileNameBase = null;
  private static String fileNameSuffix = null;

  /**
   */
  public static void main(String[] args) throws Exception {
    Map<String, String> argMap = StringUtils.convertArgs(args);

    String dirPath = null;
    String csvSeparator = "\t";

    for (String argName : argMap.keySet()) {
      String argValue = argMap.get(argName);
      if (argName.equals("dir")) {
        dirPath = argValue;
      } else if (argName.equals("fileNameBase")) {
        fileNameBase = argValue;
      } else if (argName.equals("fileNameSuffix")) {
        fileNameSuffix = argValue;
      } else if (argName.equals("csvSeparator")) {
        csvSeparator = argValue;
      } else {
        throw new RuntimeException("Unknown option: " + argName);
      }
    }

    CSV.setCsvSeparator(csvSeparator);

    if (dirPath == null)
      throw new RuntimeException("dir is required");
    if (fileNameBase == null)
      throw new RuntimeException("fileNameBase is required");
    if (fileNameSuffix == null)
      throw new RuntimeException("fileNameSuffix is required");

    File directory = new File(dirPath);
    writeAccuracy(directory);
  }

  private static void writeAccuracy(File directory) throws Exception {
    File[] files = directory.listFiles(new FilenameFilter() {

      @Override
      public boolean accept(File dir, String name) {
        return (name.startsWith(fileNameBase) && name.endsWith(fileNameSuffix));
      }
    });

    Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(directory, fileNameBase + "_accuracy.csv")), "UTF-8"));
    writer.write(CSV.format("File") + CSV.format("Accuracy") + CSV.format("good") + CSV.format("bad") + CSV.format("total") + CSV.format("UAS"));

    writer.write("\n");

    for (File file : files) {
      CSVFile csvFile = new CSVFile(CSV, file, "UTF-8");

      writer.write(CSV.format(file.getName()));
      int truePosColumn = -1;
      int falseNegColumn = -1;
      int falsePosColumn = -1;

      for (int i = 0; i < csvFile.numColumns(0); i++) {
        String cell = csvFile.getValue(0, i);
        if (cell.equals("true+"))
          truePosColumn = i;
        else if (cell.equals("false-"))
          falseNegColumn = i;
        else if (cell.equals("false+"))
          falsePosColumn = i;
      }

      int totalRow = -1;
      int noHeadRow = -1;
      int wrongHeadRow = -1;
      int wrongHeadWrongLabelRow = -1;
      for (int i = 1; i < csvFile.numRows(); i++) {
        String cell = csvFile.getValue(i, 0);

        if (cell.equals("TOTAL")) {
          totalRow = i;
        } else if (cell.equals("noHead")) {
          noHeadRow = i;
        } else if (cell.equals("wrongHead")) {
          wrongHeadRow = i;
        } else if (cell.equals("wrongHeadWrongLabel")) {
          wrongHeadWrongLabelRow = i;
        }
      }

      double totalGood = format.parse(csvFile.getValue(totalRow, truePosColumn)).intValue();
      double totalBad = format.parse(csvFile.getValue(totalRow, falseNegColumn)).intValue();

      double accuracy = (totalGood / (totalGood + totalBad)) * 100.0;

      writer.write(CSV.format(accuracy));
      writer.write(CSV.format(totalGood));
      writer.write(CSV.format(totalBad));
      writer.write(CSV.format(totalGood + totalBad));

      if (noHeadRow >= 0) {
        int noHeadValue = format.parse(csvFile.getValue(noHeadRow, falsePosColumn)).intValue();
        int wrongHeadValue = format.parse(csvFile.getValue(wrongHeadRow, falsePosColumn)).intValue();
        int wrongHeadWrongLabelValue = format.parse(csvFile.getValue(wrongHeadWrongLabelRow, falsePosColumn)).intValue();

        double wrongHeadTotal = noHeadValue + wrongHeadValue + wrongHeadWrongLabelValue;
        double total = totalGood + totalBad;
        double uas = ((total - wrongHeadTotal) / total) * 100.0;
        writer.write(CSV.format(uas));
      }

      writer.write("\n");
      writer.flush();
    }
    writer.close();

  }
}
