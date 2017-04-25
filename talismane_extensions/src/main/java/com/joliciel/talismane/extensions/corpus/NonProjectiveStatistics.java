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

import java.io.IOException;
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
public class NonProjectiveStatistics implements ParseConfigurationProcessor {
  private static final CSVFormatter CSV = new CSVFormatter();

  private final Writer writer;

  @SuppressWarnings("unused")
  private final TalismaneSession session;
  private int totalCount = 0;
  private int nonProjectiveCount = 0;
  private int illNestedCount = 0;
  private int[] gapDegreeCounts = new int[10];
  private int[] edgeDegreeCounts = new int[10];

  public NonProjectiveStatistics(TalismaneSession session, Writer writer) throws IOException {
    this.session = session;
    this.writer = writer;
    writer.write(CSV.format("Sentence"));
    writer.write(CSV.format("Gap degree"));
    writer.write(CSV.format("Max gap node"));
    writer.write(CSV.format("Edge degree"));
    writer.write(CSV.format("Max edge node"));
    writer.write(CSV.format("Well nested?"));
    writer.write(CSV.format("Ill nested nodes"));
    writer.write("\n");
    writer.flush();
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
    } else {
      gapDegreeCounts[0]++;
      edgeDegreeCounts[0]++;
    }
    totalCount++;
  }

  @Override
  public void onCompleteParse() throws IOException {
    if (writer != null) {

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

      writer.flush();
      writer.close();
    }
  }

  @Override
  public void close() throws IOException {
    this.writer.close();
  }

}
