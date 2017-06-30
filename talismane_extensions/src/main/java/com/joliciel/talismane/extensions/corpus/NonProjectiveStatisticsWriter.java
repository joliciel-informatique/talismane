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
package com.joliciel.talismane.extensions.corpus;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.commons.lang3.tuple.Pair;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.parser.ParseConfigurationProcessor;
import com.joliciel.talismane.parser.ParseTree;
import com.joliciel.talismane.parser.ParseTreeNode;
import com.joliciel.talismane.utils.CSVFormatter;

/**
 * A class for gathering statistics from a given corpus.
 * 
 * @author Assaf Urieli
 *
 */
public class NonProjectiveStatisticsWriter implements ParseConfigurationProcessor {
  private static final CSVFormatter CSV = new CSVFormatter();

  private final Writer writer;
  private final Writer writer2;

  @SuppressWarnings("unused")
  private final TalismaneSession session;
  private int totalCount = 0;
  private int nonProjectiveCount = 0;
  private int nonProjectiveNodeCount = 0;
  private int totalNodeCount = 0;
  private int illNestedCount = 0;
  private int[] gapDegreeCounts = new int[10];
  private int[] edgeDegreeCounts = new int[10];

  public NonProjectiveStatisticsWriter(File outDir, TalismaneSession session) throws IOException {
    this.session = session;
    File csvFile = new File(outDir, session.getBaseName() + "_nproj.csv");
    csvFile.delete();
    csvFile.createNewFile();
    writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false), "UTF8"));

    File csvFile2 = new File(outDir, session.getBaseName() + "_nprojnodes.csv");
    csvFile2.delete();
    csvFile2.createNewFile();
    writer2 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile2, false), "UTF8"));

    writer.write(CSV.format("Sentence"));
    writer.write(CSV.format("Gap degree"));
    writer.write(CSV.format("Max gap node"));
    writer.write(CSV.format("Edge degree"));
    writer.write(CSV.format("Max edge node"));
    writer.write(CSV.format("Well nested?"));
    writer.write(CSV.format("Ill nested nodes"));
    writer.write("\n");
    writer.flush();

    writer2.write(CSV.format("Sentence"));
    writer2.write(CSV.format("Non-proj node"));
    writer2.write(CSV.format("Gap degree"));
    writer2.write(CSV.format("Edge degree"));
    writer2.write("\n");
    writer2.flush();
  }

  @Override
  public void onNextParseConfiguration(ParseConfiguration parseConfiguration) throws IOException {
    ParseTree parseTree = new ParseTree(parseConfiguration, false);
    if (!parseTree.isProjective()) {
      writer.write(CSV.format(parseConfiguration.getSentence().getText().toString()));
      writer.write(CSV.format(parseTree.getGapDegree().getRight()));
      writer.write(CSV.format(parseTree.getGapDegree().getLeft().toString()));
      int gapDegree = parseTree.getGapDegree().getRight();
      if (gapDegree > 9)
        gapDegree = 9;
      gapDegreeCounts[gapDegree]++;
      writer.write(CSV.format(parseTree.getEdgeDegree().getRight()));
      writer.write(CSV.format(parseTree.getEdgeDegree().getLeft().toString()));
      int edgeDegree = parseTree.getEdgeDegree().getRight();
      if (edgeDegree > 9)
        edgeDegree = 9;
      edgeDegreeCounts[edgeDegree]++;
      writer.write(CSV.format(parseTree.isWellNested()));
      for (Pair<ParseTreeNode, ParseTreeNode> illNestedNodes : parseTree.getIllNestedNodes()) {
        writer.write(CSV.format(illNestedNodes.getLeft().toString()));
        writer.write(CSV.format(illNestedNodes.getRight().toString()));
      }
      if (!parseTree.isWellNested())
        illNestedCount++;
      writer.write("\n");
      writer.flush();
      nonProjectiveCount++;

      for (ParseTreeNode nonProjNode : parseTree.getNonProjectiveNodes()) {
        writer2.write(CSV.format(parseConfiguration.getSentence().getText().toString()));
        writer2.write(CSV.format(nonProjNode.toString()));
        writer2.write(CSV.format(nonProjNode.getGapCount()));
        writer2.write(CSV.format(nonProjNode.getEdgeCount()));
        writer2.write("\n");
        writer2.flush();
        nonProjectiveNodeCount++;
      }
      totalNodeCount += parseConfiguration.getPosTagSequence().size() - 1;
    } else {
      gapDegreeCounts[0]++;
      edgeDegreeCounts[0]++;
    }
    totalCount++;
  }

  @Override
  public void onCompleteParse() throws IOException {
    writer.write("ALL\n");
    writer.write(CSV.format("total") + CSV.format(totalCount) + CSV.format(100.0) + "\n");
    int projectiveCount = totalCount - nonProjectiveCount;
    writer.write(CSV.format("projective") + CSV.format(projectiveCount) + CSV.format(((double) projectiveCount / (double) totalCount) * 100.0) + "\n");
    int wellNestedCount = totalCount - illNestedCount;
    writer.write(CSV.format("wellNested") + CSV.format(wellNestedCount) + CSV.format((double) wellNestedCount / (double) totalCount * 100.0) + "\n");

    for (int i = 0; i < 10; i++) {
      int gapDegree = gapDegreeCounts[i];
      writer.write(CSV.format("gapDegree" + i) + CSV.format(gapDegree) + CSV.format((double) gapDegree / (double) totalCount * 100.0) + "\n");
    }

    for (int i = 0; i < 10; i++) {
      int edgeDegree = edgeDegreeCounts[i];
      writer.write(CSV.format("edgeDegree" + i) + CSV.format(edgeDegree) + CSV.format((double) edgeDegree / (double) totalCount * 100.0) + "\n");
    }

    writer.write("NON PROJECTIVE\n");
    writer.write(CSV.format("total") + CSV.format(nonProjectiveCount) + CSV.format(100.0) + "\n");

    int nprojWellNested = nonProjectiveCount - illNestedCount;
    writer.write(CSV.format("wellNested") + CSV.format(nprojWellNested) + CSV.format((double) nprojWellNested / (double) nonProjectiveCount * 100.0) + "\n");

    writer.write("NODES\n");
    writer.write(CSV.format("total") + CSV.format(totalNodeCount) + CSV.format(100.0) + "\n");
    int projectiveNodeCount = totalNodeCount - nonProjectiveNodeCount;
    writer.write(
        CSV.format("projective") + CSV.format(projectiveNodeCount) + CSV.format(((double) projectiveNodeCount / (double) totalNodeCount) * 100.0) + "\n");
    writer.write(CSV.format("non-projective") + CSV.format(nonProjectiveNodeCount)
        + CSV.format(((double) nonProjectiveNodeCount / (double) totalNodeCount) * 100.0) + "\n");

    writer.flush();
    writer.close();
  }

  @Override
  public void close() throws IOException {
    this.writer.close();
    this.writer2.close();
  }

}
