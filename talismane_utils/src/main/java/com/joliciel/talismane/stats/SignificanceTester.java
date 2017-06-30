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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.utils.CSVFormatter;

public class SignificanceTester {
  private static CSVFormatter CSV = new CSVFormatter(4);
  @SuppressWarnings("unused")
  private static NumberFormat format = NumberFormat.getInstance(Locale.ENGLISH);
  private static final Log LOG = LogFactory.getLog(SignificanceTester.class);

  /**
   * @param args
   */
  public static void main(String[] args) throws Exception {
    Map<String, String> argMap = convertArgs(args);
    String refFilePath = null;
    String baseFilePath = null;
    String testFilePath = null;
    String spaceCharacter = "_";
    int sanityCheckIndex = 0;
    int[] testIndexes = null;
    Map<Integer, String> skipRules = new HashMap<Integer, String>();
    String suffix = "sig";
    for (String argName : argMap.keySet()) {
      String argValue = argMap.get(argName);
      if (argName.equals("refFile")) {
        refFilePath = argValue;
      } else if (argName.equals("baseFile")) {
        baseFilePath = argValue;
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
      } else if (argName.equals("skipRules")) {
        String[] parts = argValue.split(",");

        for (String part : parts) {
          String[] innerParts = part.split("=");
          skipRules.put(Integer.parseInt(innerParts[0]), innerParts[1]);
        }
      } else if (argName.equals("suffix")) {
        suffix = argValue;
      } else if (argName.equals("spaceCharacter")) {
        spaceCharacter = argValue;
      } else {
        throw new RuntimeException("Unknown option: " + argName);
      }
    }

    File refFile = new File(refFilePath);
    File baseFile = new File(baseFilePath);
    File testFile = new File(testFilePath);

    String baseName = testFile.getName();
    if (baseName.indexOf('.') >= 0) {
      baseName = baseName.substring(0, baseName.lastIndexOf('.'));
    }

    File outFile = new File(testFile.getParentFile(), baseName + "_" + suffix + ".csv");
    try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8"));
        Scanner refFileScanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(refFile), "UTF-8")));
        Scanner baseFileScanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(baseFile), "UTF-8")));
        Scanner testFileScanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(testFile), "UTF-8")));) {

      int lineCount = 0;

      int baseTrueTestTrue = 0;
      int baseTrueTestFalse = 0;
      int baseFalseTestTrue = 0;
      int baseFalseTestFalse = 0;

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
          continue;
        }

        String[] refParts = refLine.split("\t");
        String[] baseParts = baseLine.split("\t");
        String[] testParts = testLine.split("\t");

        String refSanityCheck = refParts[sanityCheckIndex];
        String baseSanityCheck = baseParts[sanityCheckIndex];
        String testSanityCheck = testParts[sanityCheckIndex];

        refSanityCheck = refSanityCheck.replace(spaceCharacter, " ");
        baseSanityCheck = baseSanityCheck.replace(spaceCharacter, " ");
        testSanityCheck = testSanityCheck.replace(spaceCharacter, " ");

        if (!refSanityCheck.equals(baseSanityCheck)) {
          throw new RuntimeException("Mismatch on base line: " + lineCount);
        }
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
          ;
        } else if (baseTrue && !testTrue) {
          baseTrueTestFalse++;
        } else if (!baseTrue && testTrue) {
          baseFalseTestTrue++;
        } else {
          baseFalseTestFalse++;
        }
      } // next line

      writer.write(CSV.format("Ref") + CSV.format(refFile.getName()) + "\n");
      writer.write(CSV.format("Base") + CSV.format(baseFile.getName()) + "\n");
      writer.write(CSV.format("Test") + CSV.format(testFile.getName()) + "\n");

      double mcNemarChi2 = Math.pow(baseTrueTestFalse - baseFalseTestTrue, 2) / ((double) baseTrueTestFalse + baseFalseTestTrue);
      writer.write(CSV.format("McNemar") + CSV.format(mcNemarChi2) + "\n");
      double pValue = 1;
      double cutoff = 0;
      if (mcNemarChi2 >= 7.789) {
        pValue = 0.005;
        cutoff = 7.789;
      } else if (mcNemarChi2 >= 6.635) {
        pValue = 0.01;
        cutoff = 6.635;
      } else if (mcNemarChi2 >= 5.024) {
        pValue = 0.025;
        cutoff = 5.024;
      } else if (mcNemarChi2 >= 3.841) {
        pValue = 0.05;
        cutoff = 3.841;
      } else if (mcNemarChi2 >= 2.706) {
        pValue = 0.1;
        cutoff = 2.706;
      }

      writer.write(CSV.format("McNemarPValue") + CSV.format(pValue) + CSV.format("cutoff") + CSV.format(cutoff) + "\n");

      if (baseTrueTestFalse + baseFalseTestTrue < 25) {
        BinomialTest binomialTest = new BinomialTest(baseTrueTestTrue, baseTrueTestFalse, baseFalseTestTrue, baseFalseTestFalse);
        writer.write(CSV.format("Binomial") + CSV.format(binomialTest.getPValue()) + "\n");
      } else {
        writer.write("\n");
      }

      writer.write(CSV.format("") + CSV.format("test true") + CSV.format("test false") + "\n");
      writer.write(CSV.format("base true") + CSV.format(baseTrueTestTrue) + CSV.format(baseTrueTestFalse) + "\n");
      writer.write(CSV.format("base false") + CSV.format(baseFalseTestTrue) + CSV.format(baseFalseTestFalse) + "\n");
      writer.flush();
      writer.close();
    }
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
