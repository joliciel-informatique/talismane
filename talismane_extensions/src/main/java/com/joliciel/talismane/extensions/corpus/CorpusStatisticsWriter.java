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
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.typesafe.config.ConfigFactory;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.parser.DependencyArc;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.parser.output.ParseConfigurationProcessor;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.utils.CSVFormatter;
import com.typesafe.config.Config;

/**
 * A class for gathering and writing statistics from a given corpus.
 * 
 * @author Assaf Urieli
 *
 */
public class CorpusStatisticsWriter implements ParseConfigurationProcessor {
  private static final Logger LOG = LoggerFactory.getLogger(CorpusStatisticsWriter.class);
  private static final CSVFormatter CSV = new CSVFormatter();

  private final Pattern alphanumeric = Pattern.compile("[a-zA-Z0-9àáçéèëêïîíöôóòüûú]");

  private final Writer writer;
  private final File serializationFile;

  private final String sessionId;
  private final CorpusStatistics stats = new CorpusStatistics();
  private final CorpusStatistics referenceStats;

  /**
   * Statistics will be written to a file with the extension "_stats.csv".<br/>
   * Statistics will be stored for future reference in "_stats.zip".
   * 
   * @throws ClassNotFoundException
   */
  public CorpusStatisticsWriter(File outDir, String sessionId) throws IOException, ClassNotFoundException {
    this.writer = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(new File(outDir, TalismaneSession.get(sessionId).getBaseName() + "_stats.csv"), false),
          TalismaneSession.get(sessionId).getOutputCharset()));

    this.sessionId = sessionId;
    this.serializationFile = new File(outDir, TalismaneSession.get(sessionId).getBaseName() + "_stats.zip");
    serializationFile.delete();

    Config config = ConfigFactory.load();
    if (config.hasPath("talismane.extensions." + sessionId + ".corpus-statistics.reference-stats")) {
      String referenceStatsPath = config.getString("talismane.extensions." + sessionId + ".corpus-statistics.reference-stats");
      File referenceStatsFile = new File(referenceStatsPath);
      this.referenceStats = CorpusStatistics.loadFromFile(referenceStatsFile);
    } else {
      this.referenceStats = null;
    }

  }

  @Override
  public void onNextParseConfiguration(ParseConfiguration parseConfiguration) {
    stats.sentenceCount++;
    stats.sentenceLengthStats.addValue(parseConfiguration.getPosTagSequence().size());

    for (PosTaggedToken posTaggedToken : parseConfiguration.getPosTagSequence()) {
      if (posTaggedToken.getTag().equals(PosTag.ROOT_POS_TAG))
        continue;

      Token token = posTaggedToken.getToken();

      String word = token.getOriginalText();
      stats.words.add(word);
      if (referenceStats != null) {
        if (!referenceStats.words.contains(word))
          stats.unknownTokenCount++;
      }
      if (alphanumeric.matcher(token.getOriginalText()).find()) {
        String lowercase = word.toLowerCase(TalismaneSession.get(sessionId).getLocale());
        stats.lowerCaseWords.add(lowercase);
        stats.alphanumericCount++;
        if (referenceStats != null) {
          if (!referenceStats.lowerCaseWords.contains(lowercase))
            stats.unknownAlphanumericCount++;
        }
      }

      stats.tokenCount++;

      Integer countObj = stats.posTagCounts.get(posTaggedToken.getTag().getCode());
      int count = countObj == null ? 0 : countObj.intValue();
      count++;
      stats.posTagCounts.put(posTaggedToken.getTag().getCode(), count);
    }

    int maxDepth = 0;
    DescriptiveStatistics avgSyntaxDepthForSentenceStats = new DescriptiveStatistics();
    for (DependencyArc arc : parseConfiguration.getNonProjectiveDependencies()) {
      Integer countObj = stats.depLabelCounts.get(arc.getLabel());
      int count = countObj == null ? 0 : countObj.intValue();
      count++;
      stats.depLabelCounts.put(arc.getLabel(), count);
      stats.totalDepCount++;

      if (arc.getHead().getTag().equals(PosTag.ROOT_POS_TAG) && (arc.getLabel() == null || arc.getLabel().length() == 0)) {
        // do nothing for unattached stuff (e.g. punctuation)
      } else if (arc.getLabel().equals("ponct")) {
        // do nothing for punctuation
      } else {
        int depth = 0;
        DependencyArc theArc = arc;
        while (theArc != null && !theArc.getHead().getTag().equals(PosTag.ROOT_POS_TAG)) {
          theArc = parseConfiguration.getGoverningDependency(theArc.getHead());
          depth++;
        }
        if (depth > maxDepth)
          maxDepth = depth;

        stats.syntaxDepthStats.addValue(depth);
        avgSyntaxDepthForSentenceStats.addValue(depth);

        int distance = Math.abs(arc.getHead().getToken().getIndex() - arc.getDependent().getToken().getIndex());
        stats.syntaxDistanceStats.addValue(distance);
      }
    }

    stats.maxSyntaxDepthStats.addValue(maxDepth);
    if (avgSyntaxDepthForSentenceStats.getN() > 0)
      stats.avgSyntaxDepthStats.addValue(avgSyntaxDepthForSentenceStats.getMean());

    if (maxDepth > stats.maxDepthCorpus)
      stats.maxDepthCorpus = maxDepth;

    // we cheat a little bit by only allowing each arc to count once
    // there could be a situation where there are two independent
    // non-projective arcs
    // crossing the same mother arc, but we prefer here to underestimate,
    // as this phenomenon is quite rare.
    Set<DependencyArc> nonProjectiveArcs = new HashSet<DependencyArc>();
    int i = 0;
    for (DependencyArc arc : parseConfiguration.getNonProjectiveDependencies()) {
      i++;
      if (arc.getHead().getTag().equals(PosTag.ROOT_POS_TAG) && (arc.getLabel() == null || arc.getLabel().length() == 0))
        continue;
      if (nonProjectiveArcs.contains(arc))
        continue;

      int headIndex = arc.getHead().getToken().getIndex();
      int depIndex = arc.getDependent().getToken().getIndex();
      int startIndex = headIndex < depIndex ? headIndex : depIndex;
      int endIndex = headIndex >= depIndex ? headIndex : depIndex;
      int j = 0;
      for (DependencyArc otherArc : parseConfiguration.getNonProjectiveDependencies()) {
        j++;
        if (j <= i)
          continue;
        if (otherArc.getHead().getTag().equals(PosTag.ROOT_POS_TAG) && (otherArc.getLabel() == null || otherArc.getLabel().length() == 0))
          continue;
        if (nonProjectiveArcs.contains(otherArc))
          continue;

        int headIndex2 = otherArc.getHead().getToken().getIndex();
        int depIndex2 = otherArc.getDependent().getToken().getIndex();
        int startIndex2 = headIndex2 < depIndex2 ? headIndex2 : depIndex2;
        int endIndex2 = headIndex2 >= depIndex2 ? headIndex2 : depIndex2;
        boolean nonProjective = false;
        if (startIndex2 < startIndex && endIndex2 > startIndex && endIndex2 < endIndex) {
          nonProjective = true;
        } else if (startIndex2 > startIndex && startIndex2 < endIndex && endIndex2 > endIndex) {
          nonProjective = true;
        }
        if (nonProjective) {
          nonProjectiveArcs.add(arc);
          nonProjectiveArcs.add(otherArc);
          stats.nonProjectiveCount++;
          LOG.debug("Non-projective arcs in sentence: " + parseConfiguration.getSentence().getText());
          LOG.debug(arc.toString());
          LOG.debug(otherArc.toString());
          break;
        }
      }
    }
  }

  @Override
  public void onCompleteParse() throws IOException {
    if (writer != null) {
      double unknownLexiconPercent = 1;
      if (referenceStats != null) {
        int unknownLexiconCount = 0;
        for (String word : stats.words) {
          if (!referenceStats.words.contains(word))
            unknownLexiconCount++;
        }
        unknownLexiconPercent = (double) unknownLexiconCount / (double) stats.words.size();
      }
      double unknownLowercaseLexiconPercent = 1;
      if (referenceStats != null) {
        int unknownLowercaseLexiconCount = 0;
        for (String lowercase : stats.lowerCaseWords) {
          if (!referenceStats.lowerCaseWords.contains(lowercase))
            unknownLowercaseLexiconCount++;
        }
        unknownLowercaseLexiconPercent = (double) unknownLowercaseLexiconCount / (double) stats.lowerCaseWords.size();
      }

      writer.write(CSV.format("sentenceCount") + CSV.format(stats.sentenceCount) + "\n");
      writer.write(CSV.format("sentenceLengthMean") + CSV.format(stats.sentenceLengthStats.getMean()) + "\n");
      writer.write(CSV.format("sentenceLengthStdDev") + CSV.format(stats.sentenceLengthStats.getStandardDeviation()) + "\n");
      writer.write(CSV.format("tokenLexiconSize") + CSV.format(stats.words.size()) + "\n");
      writer.write(CSV.format("tokenLexiconUnknown") + CSV.format(unknownLexiconPercent * 100.0) + "\n");
      writer.write(CSV.format("tokenCount") + CSV.format(stats.tokenCount) + "\n");

      double unknownTokenPercent = ((double) stats.unknownTokenCount / (double) stats.tokenCount) * 100.0;
      writer.write(CSV.format("tokenUnknown") + CSV.format(unknownTokenPercent) + "\n");

      writer.write(CSV.format("lowercaseLexiconSize") + CSV.format(stats.lowerCaseWords.size()) + "\n");
      writer.write(CSV.format("lowercaseLexiconUnknown") + CSV.format(unknownLowercaseLexiconPercent * 100.0) + "\n");
      writer.write(CSV.format("alphanumericCount") + CSV.format(stats.alphanumericCount) + "\n");

      double unknownAlphanumericPercent = ((double) stats.unknownAlphanumericCount / (double) stats.alphanumericCount) * 100.0;
      writer.write(CSV.format("alphanumericUnknown") + CSV.format(unknownAlphanumericPercent) + "\n");

      writer.write(CSV.format("syntaxDepthMean") + CSV.format(stats.syntaxDepthStats.getMean()) + "\n");
      writer.write(CSV.format("syntaxDepthStdDev") + CSV.format(stats.syntaxDepthStats.getStandardDeviation()) + "\n");
      writer.write(CSV.format("maxSyntaxDepth") + CSV.format(stats.maxDepthCorpus) + "\n");
      writer.write(CSV.format("maxSyntaxDepthMean") + CSV.format(stats.maxSyntaxDepthStats.getMean()) + "\n");
      writer.write(CSV.format("maxSyntaxDepthStdDev") + CSV.format(stats.maxSyntaxDepthStats.getStandardDeviation()) + "\n");
      writer.write(CSV.format("sentAvgSyntaxDepthMean") + CSV.format(stats.avgSyntaxDepthStats.getMean()) + "\n");
      writer.write(CSV.format("sentAvgSyntaxDepthStdDev") + CSV.format(stats.avgSyntaxDepthStats.getStandardDeviation()) + "\n");
      writer.write(CSV.format("syntaxDistanceMean") + CSV.format(stats.syntaxDistanceStats.getMean()) + "\n");
      writer.write(CSV.format("syntaxDistanceStdDev") + CSV.format(stats.syntaxDistanceStats.getStandardDeviation()) + "\n");

      double nonProjectivePercent = ((double) stats.nonProjectiveCount / (double) stats.totalDepCount) * 100.0;
      writer.write(CSV.format("nonProjectiveCount") + CSV.format(stats.nonProjectiveCount) + "\n");
      writer.write(CSV.format("nonProjectivePercent") + CSV.format(nonProjectivePercent) + "\n");
      writer.write(CSV.format("PosTagCounts") + "\n");

      for (String posTag : stats.posTagCounts.keySet()) {
        int count = stats.posTagCounts.get(posTag);
        writer.write(CSV.format(posTag) + CSV.format(count) + CSV.format(((double) count / (double) stats.tokenCount) * 100.0) + "\n");
      }

      writer.write(CSV.format("DepLabelCounts") + "\n");
      for (String depLabel : stats.depLabelCounts.keySet()) {
        int count = stats.depLabelCounts.get(depLabel);
        writer.write(CSV.format(depLabel) + CSV.format(count) + CSV.format(((double) count / (double) stats.totalDepCount) * 100.0) + "\n");
      }
      writer.flush();
      writer.close();
    }

    if (this.serializationFile != null) {
      ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(serializationFile, false));
      zos.putNextEntry(new ZipEntry("Contents.obj"));
      ObjectOutputStream oos = new ObjectOutputStream(zos);
      try {
        oos.writeObject(stats);
      } finally {
        oos.flush();
      }
      zos.flush();
      zos.close();
    }
  }

  @Override
  public void close() throws IOException {
  }

}
