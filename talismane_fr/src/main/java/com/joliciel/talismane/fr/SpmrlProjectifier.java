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
package com.joliciel.talismane.fr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.StringUtils;

/**
 * Transforms a non-projective corpus to a projective corpus by attaching
 * non-projective arcs to the nearest projective head.
 * 
 * @author Assaf Urieli
 *
 */
public class SpmrlProjectifier {

  public static void main(String[] args) throws Exception {
    SpmrlProjectifier projectifier = new SpmrlProjectifier();
    Map<String, String> argMap = StringUtils.convertArgs(args);
    projectifier.run(argMap);
  }

  public void run(Map<String, String> argMap) throws Exception {
    String logConfigPath = argMap.get("logConfigFile");
    argMap.remove("logConfigFile");
    LogUtils.configureLogging(logConfigPath);

    String refFilePath = null;
    String suffix = "proj";

    for (String argName : argMap.keySet()) {
      String argValue = argMap.get(argName);
      if (argName.equals("refFile")) {
        refFilePath = argValue;
      } else if (argName.equals("suffix")) {
        suffix = argValue;
      } else {
        throw new RuntimeException("Unknown option: " + argName);
      }
    }

    File refFile = new File(refFilePath);

    String baseName = refFile.getName();
    if (baseName.indexOf('.') >= 0) {
      baseName = baseName.substring(0, baseName.lastIndexOf('.'));
    }

    String extension = "";
    if (refFile.getName().indexOf('.') >= 0) {
      extension = refFile.getName().substring(refFile.getName().lastIndexOf('.'));
    }

    String outFileName = baseName + "_" + suffix + extension;

    File outFile = new File(refFile.getParentFile(), outFileName);
    try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8"));
        Scanner refFileScanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(refFile), "UTF-8")))) {

      int lineNumber = 0;

      ConllLine rootLine = new ConllLine("0\tROOT\tROOT\tROOT\troot\troot\t-1\troot\t-1\troot", -1);
      List<ConllLine> refLines = new ArrayList<ConllLine>();
      refLines.add(rootLine);

      while (refFileScanner.hasNextLine()) {
        lineNumber++;

        String nextRefLine = refFileScanner.nextLine();

        if (nextRefLine.length() > 0) {
          ConllLine refLine = new ConllLine(nextRefLine, lineNumber);

          refLines.add(refLine);
        }

        if (nextRefLine.length() == 0 || !refFileScanner.hasNextLine()) {
          // construct dependency map

          NonProjectivePair pair = this.getNextPair(refLines);
          while (pair != null) {
            ConllLine newHead1 = null;
            ConllLine parent1 = refLines.get(pair.arc1.projGov);
            int depIndex1 = pair.arc1.index;
            while (parent1.index != 0) {
              parent1 = refLines.get(parent1.projGov);
              int headIndex = parent1.index;
              int startIndex = headIndex < depIndex1 ? headIndex : depIndex1;
              int endIndex = headIndex >= depIndex1 ? headIndex : depIndex1;
              if (isProjective(startIndex, endIndex, pair.arc2)) {
                newHead1 = parent1;
                break;
              }
            }
            ConllLine newHead2 = null;
            ConllLine parent2 = refLines.get(pair.arc2.projGov);
            int depIndex2 = pair.arc2.index;
            while (parent2.index != 0) {
              parent2 = refLines.get(parent2.projGov);
              int headIndex = parent2.index;
              int startIndex = headIndex < depIndex2 ? headIndex : depIndex2;
              int endIndex = headIndex >= depIndex2 ? headIndex : depIndex2;
              if (isProjective(startIndex, endIndex, pair.arc2)) {
                newHead2 = parent2;
                break;
              }
            }
            if (newHead1 != null && newHead2 != null) {
              int distance1 = 0;
              ConllLine parent = newHead1;
              while (parent.index != 0) {
                distance1++;
                parent = refLines.get(parent.projGov);
              }
              int distance2 = 0;
              parent = newHead2;
              while (parent.index != 0) {
                distance2++;
                parent = refLines.get(parent.projGov);
              }
              // we want the new arc to be as far as possible from
              // root
              if (distance1 < distance2) {
                newHead1 = null;
              } else {
                newHead2 = null;
              }
            }
            if (newHead1 != null && newHead2 == null) {
              pair.arc1.projGov = newHead1.index;
            } else if (newHead1 == null && newHead2 != null) {
              pair.arc2.projGov = newHead2.index;
            } else {
              throw new RuntimeException("Cannot deprojectify " + pair);
            }

            pair = this.getNextPair(refLines);
          }

          for (ConllLine refLine : refLines) {
            if (refLine.index == 0)
              continue;
            writer.write(refLine.toString() + "\n");
          }
          writer.write("\n");
          writer.flush();

          // prepare next sentence
          refLines = new ArrayList<ConllLine>();
          refLines.add(rootLine);
        } // next sentence

      } // next line
      writer.flush();
    }
  }

  private NonProjectivePair getNextPair(List<ConllLine> arcs) {
    NonProjectivePair pair = null;
    ConllLine arc = null;
    ConllLine otherArc = null;
    for (int i = 0; i < arcs.size(); i++) {
      arc = arcs.get(i);
      if (arc.projGov == 0 && (arc.label == null || arc.label.length() == 0))
        continue;
      int headIndex = arc.projGov;
      int depIndex = arc.index;
      int startIndex = headIndex < depIndex ? headIndex : depIndex;
      int endIndex = headIndex >= depIndex ? headIndex : depIndex;

      for (int j = i + 1; j < arcs.size(); j++) {
        otherArc = arcs.get(j);
        if (otherArc.projGov == 0 && (otherArc.label == null || otherArc.label.length() == 0))
          continue;
        if (!isProjective(startIndex, endIndex, otherArc)) {
          pair = new NonProjectivePair(arc, otherArc);
          break;
        }

      }
      if (pair != null)
        break;
    }
    return pair;
  }

  boolean isProjective(int startIndex, int endIndex, ConllLine otherArc) {
    boolean projective = true;

    int headIndex2 = otherArc.projGov;
    int depIndex2 = otherArc.index;
    int startIndex2 = headIndex2 < depIndex2 ? headIndex2 : depIndex2;
    int endIndex2 = headIndex2 >= depIndex2 ? headIndex2 : depIndex2;
    if (startIndex2 < startIndex && endIndex2 > startIndex && endIndex2 < endIndex) {
      projective = false;
    } else if (startIndex2 > startIndex && startIndex2 < endIndex && endIndex2 > endIndex) {
      projective = false;
    }
    return projective;
  }

  private static final class NonProjectivePair {
    ConllLine arc1;
    ConllLine arc2;

    public NonProjectivePair(ConllLine arc1, ConllLine arc2) {
      super();
      this.arc1 = arc1;
      this.arc2 = arc2;
    }

    @Override
    public String toString() {
      return "NonProjectivePair [arc1=" + arc1.index + ", arc2=" + arc2.index + "]";
    }
  }

  private static final class ConllLine {
    public ConllLine(String line, int lineNumber) {
      String[] parts = line.split("\t");
      index = Integer.parseInt(parts[0]);
      word = parts[1];
      lemma = parts[2];

      if (word.equals("-LRB-")) {
        word = "(";
        lemma = "(";
      } else if (word.equals("-RRB-")) {
        word = ")";
        lemma = ")";
      } else if (word.equals("-LSB-")) {
        word = "[";
        lemma = "[";
      } else if (word.equals("-RSB-")) {
        word = "]";
        lemma = "]";
      } else if (word.equals("-LRB-...-RRB-")) {
        word = "(...)";
        lemma = "(...)";
      }

      posTag = parts[3];
      posTag2 = parts[4];

      if (posTag2.equals("ADJWH")) {
        posTag2 = "DETWH";
        posTag = "D";
      }

      morph = parts[5];
      if (morph.contains("mwehead")) {
        int mweheadPos = morph.indexOf("mwehead");
        int nextPart = morph.indexOf('|', mweheadPos);
        compPosTag = morph.substring(mweheadPos + "mwehead".length() + 1, nextPart - 1);
        if (compPosTag.equals("ADJWH"))
          compPosTag = "DETWH";
      }

      governor = Integer.parseInt(parts[6]);
      label = parts[7].replace('.', '_');
      if (label.equals("obj_cpl"))
        label = "sub";
      else if (label.equals("obj_p"))
        label = "prep";

      projGov = Integer.parseInt(parts[8]);
      projLabel = parts[9].replace('.', '_');
      if (projLabel.equals("obj_cpl"))
        projLabel = "sub";
      else if (projLabel.equals("obj_p"))
        projLabel = "prep";

      this.lineNumber = lineNumber;
    }

    int index;
    String word;
    String lemma;
    String posTag;
    String posTag2;
    String compPosTag = null;
    String morph;
    int governor;
    String label;
    int projGov;
    String projLabel;

    @SuppressWarnings("unused")
    int lineNumber;

    @Override
    public String toString() {
      String string = this.index + "\t" + this.word + "\t" + this.lemma + "\t" + this.posTag + "\t" + this.posTag2 + "\t" + this.morph + "\t" + this.governor
          + "\t" + this.label + "\t" + this.projGov + "\t" + this.projLabel;
      return string;
    }
  }

}
