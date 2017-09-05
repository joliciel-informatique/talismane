package com.joliciel.talismane.stats;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.utils.CSVFormatter;

public class SignificanceTesterDir {
  private static CSVFormatter CSV = new CSVFormatter(4);
  @SuppressWarnings("unused")
  private static NumberFormat format = NumberFormat.getInstance(Locale.ENGLISH);
  private static final Log LOG = LogFactory.getLog(SignificanceTesterDir.class);

  /**
   * @param args
   */
  public static void main(String[] args) throws Exception {
    Map<String, String> argMap = convertArgs(args);
    String refDirPath = null;
    String testDirPath = null;
    String baseFileSuffix = null;
    String testFileSuffix = null;
    int sanityCheckIndex = 0;
    int filterIndex = 0;
    int[] testIndexes = null;
    int[] diffIndexes = null;
    List<Integer> skipIndexes = new ArrayList<Integer>();
    List<String> skipValues = new ArrayList<String>();
    Set<String> filters = null;
    String filterString = null;
    boolean posTaggerDiff = false;
    String suffix = "";

    String refFileSuffix = ".conll";
    for (String argName : argMap.keySet()) {
      String argValue = argMap.get(argName);
      if (argName.equals("refDir")) {
        refDirPath = argValue;
      } else if (argName.equals("testDir")) {
        testDirPath = argValue;
      } else if (argName.equals("baseSuffix")) {
        baseFileSuffix = argValue;
      } else if (argName.equals("testSuffix")) {
        testFileSuffix = argValue;
      } else if (argName.equals("refSuffix")) {
        refFileSuffix = argValue;
      } else if (argName.equals("suffix")) {
        suffix = argValue;
      } else if (argName.equals("testIndexes")) {
        String[] parts = argValue.split(",");
        testIndexes = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
          testIndexes[i] = Integer.parseInt(parts[i]);
        }
      } else if (argName.equals("diffIndexes")) {
        String[] parts = argValue.split(",");
        diffIndexes = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
          diffIndexes[i] = Integer.parseInt(parts[i]);
        }
      } else if (argName.equals("sanityCheckIndex")) {
        sanityCheckIndex = Integer.parseInt(argValue);
      } else if (argName.equals("filterIndex")) {
        filterIndex = Integer.parseInt(argValue);
      } else if (argName.equals("posTaggerDiffs")) {
        posTaggerDiff = argValue.equalsIgnoreCase("true");
      } else if (argName.equals("filters")) {
        filterString = argValue;
        String[] parts = argValue.split(";");
        filters = new HashSet<String>();
        for (String part : parts)
          filters.add(part);
      } else if (argName.equals("skipRules")) {
        String[] parts = argValue.split(",");

        for (String part : parts) {
          String[] innerParts = part.split("=");
          skipIndexes.add(Integer.parseInt(innerParts[0]));
          skipValues.add(innerParts[1]);
        }
      } else {
        throw new RuntimeException("Unknown option: " + argName);
      }
    }

    File refDir = new File(refDirPath);
    File testDir = new File(testDirPath);
    File[] subDirs = testDir.listFiles(new FileFilter() {

      @Override
      public boolean accept(File pathname) {
        return pathname.isDirectory();
      }
    });

    File outFile = new File(testDir, "sig" + testFileSuffix + "_" + filterString.replace(';', '_').replace("'", "") + suffix + ".csv");
    Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8"));

    File diffOutFile = new File(testDir, "diff" + testFileSuffix + suffix + ".txt");
    Writer diffWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(diffOutFile), "UTF-8"));

    int baseTrueTestTrueAll = 0;
    int baseTrueTestFalseAll = 0;
    int baseFalseTestTrueAll = 0;
    int baseFalseTestFalseAll = 0;

    int baseTrueTestTrueFilteredAll = 0;
    int baseTrueTestFalseFilteredAll = 0;
    int baseFalseTestTrueFilteredAll = 0;
    int baseFalseTestFalseFilteredAll = 0;

    for (File subDir : subDirs) {
      diffWriter.write("\n#######" + subDir.getName() + "\n");
      File refFile = new File(refDir, subDir.getName() + refFileSuffix);
      File baseFile = new File(subDir, subDir.getName() + baseFileSuffix);
      File testFile = new File(subDir, subDir.getName() + testFileSuffix);

      LOG.info("refFile: " + refFile.getName());
      LOG.info("baseFile: " + baseFile.getName());
      LOG.info("testFile: " + testFile.getName());

      try (Scanner refFileScanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(refFile), "UTF-8")));
          Scanner baseFileScanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(baseFile), "UTF-8")));
          Scanner testFileScanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(testFile), "UTF-8")));) {

        int lineCount = 0;

        int baseTrueTestTrue = 0;
        int baseTrueTestFalse = 0;
        int baseFalseTestTrue = 0;
        int baseFalseTestFalse = 0;

        int baseTrueTestTrueFiltered = 0;
        int baseTrueTestFalseFiltered = 0;
        int baseFalseTestTrueFiltered = 0;
        int baseFalseTestFalseFiltered = 0;

        StringBuilder diffs = new StringBuilder();
        boolean haveDiffs = false;
        int errorCount = 0;
        int fixCount = 0;
        int diffCount = 0;
        StringBuilder baseDiffs = new StringBuilder();
        StringBuilder testDiffs = new StringBuilder();

        while (refFileScanner.hasNextLine()) {
          lineCount++;
          if (!baseFileScanner.hasNextLine()) {
            throw new RuntimeException("Not enough lines on baseFile: line " + lineCount);
          }
          if (!testFileScanner.hasNextLine()) {
            throw new RuntimeException("Not enough lines on testFile: line " + lineCount);
          }

          String refLine = refFileScanner.nextLine();
          String baseLine = baseFileScanner.nextLine();
          String testLine = testFileScanner.nextLine();

          if (refLine.length() == 0) {
            if (baseLine.length() != 0) {
              throw new RuntimeException("Mismatch on base line: " + lineCount);
            }
            if (testLine.length() != 0) {
              throw new RuntimeException("Mismatch on test line: " + lineCount);
            }

            if (haveDiffs) {
              if (posTaggerDiff) {
                diffWriter.write("\n");
                diffWriter.write("Diffs " + diffCount + ". Errors " + errorCount + ". Fixes " + fixCount + "\n");
                diffWriter.write(baseDiffs.toString() + "\n");
                diffWriter.write(testDiffs.toString() + "\n");
              } else {
                diffWriter.write("\n");
                diffWriter.write(diffs.toString());
                diffWriter.write("\n");
                diffWriter.flush();
              }
            }
            diffs = new StringBuilder();
            baseDiffs = new StringBuilder();
            testDiffs = new StringBuilder();
            diffCount = 0;
            errorCount = 0;
            fixCount = 0;
            haveDiffs = false;
            continue;
          }

          String[] refParts = refLine.split("\t");
          String[] baseParts = baseLine.split("\t");
          String[] testParts = testLine.split("\t");

          String refSanityCheck = refParts[sanityCheckIndex].replace('_', ' ');
          String baseSanityCheck = baseParts[sanityCheckIndex].replace('_', ' ');
          String testSanityCheck = testParts[sanityCheckIndex].replace('_', ' ');

          if (!refSanityCheck.equals(baseSanityCheck)) {
            throw new RuntimeException("Mismatch on base line: " + lineCount + ". Ref: " + refSanityCheck + ". Base: " + baseSanityCheck);
          }
          if (!refSanityCheck.equals(testSanityCheck)) {
            throw new RuntimeException("Mismatch on test line: " + lineCount + ". Ref: " + refSanityCheck + ". Test: " + testSanityCheck);
          }

          boolean shouldSkip = false;
          for (int i = 0; i < skipIndexes.size(); i++) {
            int skipIndex = skipIndexes.get(i);
            String skipValue = skipValues.get(i);
            if (refParts[skipIndex].equals(skipValue)) {
              LOG.debug("Skipping line " + lineCount + ": " + refLine);
              shouldSkip = true;
              break;
            }
          }
          if (shouldSkip) {
            diffs.append(baseLine + "\n");
            continue;
          }

          boolean baseTrue = true;
          for (int testIndex : testIndexes) {
            if (!refParts[testIndex].equals(baseParts[testIndex])) {
              baseTrue = false;
              break;
            }
          }

          boolean testTrue = true;
          for (int testIndex : testIndexes) {
            if (!refParts[testIndex].equals(testParts[testIndex])) {
              testTrue = false;
              break;
            }
          }

          if (baseTrue && testTrue) {
            baseTrueTestTrue++;
          } else if (baseTrue && !testTrue) {
            baseTrueTestFalse++;
            errorCount++;
            haveDiffs = true;
          } else if (!baseTrue && testTrue) {
            baseFalseTestTrue++;
            fixCount++;
            haveDiffs = true;
          } else {
            baseFalseTestFalse++;
          }

          String filter = refParts[filterIndex].toLowerCase();
          if (filters != null && filters.contains(filter)) {
            if (baseTrue && testTrue) {
              baseTrueTestTrueFiltered++;
            } else if (baseTrue && !testTrue) {
              baseTrueTestFalseFiltered++;
            } else if (!baseTrue && testTrue) {
              baseFalseTestTrueFiltered++;
            } else {
              baseFalseTestFalseFiltered++;
            }
          }

          boolean haveDiff = false;
          for (int testIndex : testIndexes) {
            if (!baseParts[testIndex].equals(testParts[testIndex])) {
              haveDiff = true;
              diffCount++;
              break;
            }
          }
          if (!haveDiff) {
            for (int diffIndex : diffIndexes) {
              if (!baseParts[diffIndex].equals(testParts[diffIndex])) {
                haveDiff = true;
                diffCount++;
                break;
              }
            }
          }

          if (haveDiff) {
            if (posTaggerDiff) {
              baseDiffs.append(" #" + baseParts[1] + "/" + baseParts[3]);
              testDiffs.append(" #" + testParts[1] + "/" + testParts[3]);
            } else {
              if (baseTrue && !testTrue) {
                diffs.append("#ERROR\n");
              } else if (!baseTrue && testTrue) {
                diffs.append("#FIX\n");
              } else {
                diffs.append("#DIFF\n");
              }
              diffs.append(refLine + "\n");
              diffs.append(baseLine + "\n");
              diffs.append(testLine + "\n");
              if (baseTrue && !testTrue) {
                diffs.append("/ERROR\n");
              } else if (!baseTrue && testTrue) {
                diffs.append("/FIX\n");
              } else {
                diffs.append("/DIFF\n");
              }
            }
          } else {
            if (posTaggerDiff) {
              baseDiffs.append(" " + baseParts[1]);
              testDiffs.append(" " + testParts[1]);
            } else {
              diffs.append(baseLine + "\n");
            }
          }

        } // next line

        writer.write(CSV.format("Ref") + CSV.format(refFile.getName()) + "\n");
        writer.write(CSV.format("Base") + CSV.format(baseFile.getName()) + "\n");
        writer.write(CSV.format("Test") + CSV.format(testFile.getName()) + "\n");

        double mcNemarChi2 = Math.pow(baseTrueTestFalse - baseFalseTestTrue, 2) / ((double) baseTrueTestFalse + baseFalseTestTrue);
        writer.write(CSV.format("McNemar") + CSV.format(mcNemarChi2) + "\n");

        if (baseTrueTestFalse + baseFalseTestTrue < 25) {
          BinomialTest binomialTest = new BinomialTest(baseTrueTestTrue, baseTrueTestFalse, baseFalseTestTrue, baseFalseTestFalse);
          writer.write(CSV.format("Binomial") + CSV.format(binomialTest.getPValue()) + "\n");
        } else {
          writer.write("\n");
        }

        writer.write(CSV.format("") + CSV.format("test true") + CSV.format("test false") + CSV.format("total") + "\n");
        writer.write(
            CSV.format("base true") + CSV.format(baseTrueTestTrue) + CSV.format(baseTrueTestFalse) + CSV.format(baseTrueTestTrue + baseTrueTestFalse) + "\n");
        writer.write(CSV.format("base false") + CSV.format(baseFalseTestTrue) + CSV.format(baseFalseTestFalse)
            + CSV.format(baseFalseTestTrue + baseFalseTestFalse) + "\n");
        writer.write(CSV.format("total") + CSV.format(baseTrueTestTrue + baseFalseTestTrue) + CSV.format(baseTrueTestFalse + baseFalseTestFalse)
            + CSV.format(baseTrueTestTrue + baseTrueTestFalse + baseFalseTestTrue + baseFalseTestFalse) + "\n");
        writer.write("\n");

        writer.flush();

        writer.write("\n");

        writer.write(CSV.format("Filter") + CSV.format(filterString) + "\n");
        writer.write(CSV.format("Ref") + CSV.format(refFile.getName()) + "\n");
        writer.write(CSV.format("Base") + CSV.format(baseFile.getName()) + "\n");
        writer.write(CSV.format("Test") + CSV.format(testFile.getName()) + "\n");

        double mcNemarChi2Filtered = Math.pow(baseTrueTestFalseFiltered - baseFalseTestTrueFiltered, 2)
            / ((double) baseTrueTestFalseFiltered + baseFalseTestTrueFiltered);
        writer.write(CSV.format("McNemar") + CSV.format(mcNemarChi2Filtered) + "\n");

        if (baseTrueTestFalseFiltered + baseFalseTestTrueFiltered < 25) {
          BinomialTest binomialTest = new BinomialTest(baseTrueTestTrueFiltered, baseTrueTestFalseFiltered, baseFalseTestTrueFiltered,
              baseFalseTestFalseFiltered);
          writer.write(CSV.format("Binomial") + CSV.format(binomialTest.getPValue()) + "\n");
        } else {
          writer.write("\n");
        }

        writer.write(CSV.format("") + CSV.format("test true") + CSV.format("test false") + CSV.format("total") + "\n");
        writer.write(CSV.format("base true") + CSV.format(baseTrueTestTrueFiltered) + CSV.format(baseTrueTestFalseFiltered)
            + CSV.format(baseTrueTestTrueFiltered + baseTrueTestFalseFiltered) + "\n");
        writer.write(CSV.format("base false") + CSV.format(baseFalseTestTrueFiltered) + CSV.format(baseFalseTestFalseFiltered)
            + CSV.format(baseFalseTestTrueFiltered + baseFalseTestFalseFiltered) + "\n");
        writer.write(CSV.format("total") + CSV.format(baseTrueTestTrueFiltered + baseFalseTestTrueFiltered)
            + CSV.format(baseTrueTestFalseFiltered + baseFalseTestFalseFiltered)
            + CSV.format(baseTrueTestTrueFiltered + baseTrueTestFalseFiltered + baseFalseTestTrueFiltered + baseFalseTestFalseFiltered) + "\n");

        writer.write("\n");

        writer.flush();

        baseTrueTestTrueAll += baseTrueTestTrue;
        baseTrueTestFalseAll += baseTrueTestFalse;
        baseFalseTestTrueAll += baseFalseTestTrue;
        baseFalseTestFalseAll += baseFalseTestFalse;

        baseTrueTestTrueFilteredAll += baseTrueTestTrueFiltered;
        baseTrueTestFalseFilteredAll += baseTrueTestFalseFiltered;
        baseFalseTestTrueFilteredAll += baseFalseTestTrueFiltered;
        baseFalseTestFalseFilteredAll += baseFalseTestFalseFiltered;
      }
    }

    writer.write(CSV.format("Base") + CSV.format(baseFileSuffix) + "\n");
    writer.write(CSV.format("Test") + CSV.format(testFileSuffix) + "\n");

    double mcNemarChi2 = Math.pow(baseTrueTestFalseAll - baseFalseTestTrueAll, 2) / ((double) baseTrueTestFalseAll + baseFalseTestTrueAll);
    writer.write(CSV.format("McNemar") + CSV.format(mcNemarChi2) + "\n");

    if (baseTrueTestFalseAll + baseFalseTestTrueAll < 25) {
      BinomialTest binomialTest = new BinomialTest(baseTrueTestTrueAll, baseTrueTestFalseAll, baseFalseTestTrueAll, baseFalseTestFalseAll);
      writer.write(CSV.format("Binomial") + CSV.format(1 - binomialTest.getPValue()) + "\n");
    } else {
      writer.write("\n");
    }

    writer.write(CSV.format("") + CSV.format("test true") + CSV.format("test false") + CSV.format("total") + "\n");
    writer.write(CSV.format("base true") + CSV.format(baseTrueTestTrueAll) + CSV.format(baseTrueTestFalseAll)
        + CSV.format(baseTrueTestTrueAll + baseTrueTestFalseAll) + "\n");
    writer.write(CSV.format("base false") + CSV.format(baseFalseTestTrueAll) + CSV.format(baseFalseTestFalseAll)
        + CSV.format(baseFalseTestTrueAll + baseFalseTestFalseAll) + "\n");
    writer.write(CSV.format("total") + CSV.format(baseTrueTestTrueAll + baseFalseTestTrueAll) + CSV.format(baseTrueTestFalseAll + baseFalseTestFalseAll)
        + CSV.format(baseTrueTestTrueAll + baseTrueTestFalseAll + baseFalseTestTrueAll + baseFalseTestFalseAll) + "\n");
    writer.flush();

    writer.write("\n");

    writer.write(CSV.format("Filter") + CSV.format(filterString) + "\n");
    writer.write(CSV.format("Base") + CSV.format(baseFileSuffix) + "\n");
    writer.write(CSV.format("Test") + CSV.format(testFileSuffix) + "\n");

    double mcNemarChi2Filtered = Math.pow(baseTrueTestFalseFilteredAll - baseFalseTestTrueFilteredAll, 2)
        / ((double) baseTrueTestFalseFilteredAll + baseFalseTestTrueFilteredAll);
    writer.write(CSV.format("McNemar") + CSV.format(mcNemarChi2Filtered) + "\n");

    if (baseTrueTestFalseFilteredAll + baseFalseTestTrueFilteredAll < 25) {
      BinomialTest binomialTest = new BinomialTest(baseTrueTestTrueFilteredAll, baseTrueTestFalseFilteredAll, baseFalseTestTrueFilteredAll,
          baseFalseTestFalseFilteredAll);
      writer.write(CSV.format("Binomial") + CSV.format(binomialTest.getPValue()) + "\n");
    } else {
      writer.write("\n");
    }

    writer.write(CSV.format("") + CSV.format("test true") + CSV.format("test false") + CSV.format("total") + "\n");
    writer.write(CSV.format("base true") + CSV.format(baseTrueTestTrueFilteredAll) + CSV.format(baseTrueTestFalseFilteredAll)
        + CSV.format(baseTrueTestTrueFilteredAll + baseTrueTestFalseFilteredAll) + "\n");
    writer.write(CSV.format("base false") + CSV.format(baseFalseTestTrueFilteredAll) + CSV.format(baseFalseTestFalseFilteredAll)
        + CSV.format(baseFalseTestTrueFilteredAll + baseFalseTestFalseFilteredAll) + "\n");
    writer.write(CSV.format("total") + CSV.format(baseTrueTestTrueFilteredAll + baseFalseTestTrueFilteredAll)
        + CSV.format(baseTrueTestFalseFilteredAll + baseFalseTestFalseFilteredAll)
        + CSV.format(baseTrueTestTrueFilteredAll + baseTrueTestFalseFilteredAll + baseFalseTestTrueFilteredAll + baseFalseTestFalseFilteredAll) + "\n");
    writer.flush();

    writer.close();

    diffWriter.close();
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
