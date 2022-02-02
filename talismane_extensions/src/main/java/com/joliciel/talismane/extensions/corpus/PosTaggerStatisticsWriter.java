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
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagOpenClassIndicator;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.posTagger.output.PosTagSequenceProcessor;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.utils.CSVFormatter;
import com.typesafe.config.Config;

/**
 * A class for gathering and writing pos-tagger statistics from a given corpus.
 * 
 * @author Assaf Urieli
 *
 */
public class PosTaggerStatisticsWriter implements PosTagSequenceProcessor {
  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(PosTaggerStatisticsWriter.class);
  private static final CSVFormatter CSV = new CSVFormatter();

  private final Pattern alphanumeric = Pattern.compile("[a-zA-Z0-9àáçéèëêïîíöôóòüûú]");

  private final Writer writer;
  private final File serializationFile;

  private final String sessionId;
  private final PosTaggerStatistics stats = new PosTaggerStatistics();
  private final PosTaggerStatistics referenceStats;

  /**
   * Statistics will be written to a file with the extension "_stats.csv".<br>
   * Statistics will be stored for future reference in "_stats.zip".
   * 
   * @throws ClassNotFoundException
   */
  public PosTaggerStatisticsWriter(File outDir, String sessionId) throws IOException, ClassNotFoundException {
    this.writer = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(new File(outDir, TalismaneSession.get(sessionId).getBaseName() + "_stats.csv"), false),
          TalismaneSession.get(sessionId).getOutputCharset()));

    this.sessionId = sessionId;
    this.serializationFile = new File(outDir, TalismaneSession.get(sessionId).getBaseName() + "_stats.zip");
    serializationFile.delete();

    Config config = ConfigFactory.load();
    if (config.hasPath("talismane.extensions." + sessionId + ".pos-tagger-statistics.reference-stats")) {
      String referenceStatsPath = config.getString("talismane.extensions." + sessionId + ".pos-tagger-statistics.reference-stats");
      File referenceStatsFile = new File(referenceStatsPath);
      this.referenceStats = PosTaggerStatistics.loadFromFile(referenceStatsFile);
    } else {
      this.referenceStats = null;
    }

  }

  @Override
  public void onNextPosTagSequence(PosTagSequence posTagSequence) throws TalismaneException {
    stats.sentenceCount++;
    stats.sentenceLengthStats.addValue(posTagSequence.size());

    for (PosTaggedToken posTaggedToken : posTagSequence) {
      if (posTaggedToken.getTag().equals(PosTag.ROOT_POS_TAG))
        continue;

      Token token = posTaggedToken.getToken();

      boolean knownInRefCorpus = false;
      boolean knownInLexicon = false;
      if (token.getPossiblePosTags().size() > 0)
        knownInLexicon = true;

      String word = token.getOriginalText();
      stats.words.add(word);

      if (referenceStats != null)
        if (referenceStats.words.contains(word))
          knownInRefCorpus = true;

      if (!knownInLexicon) {
        stats.unknownInLexiconCount++;
      }
      if (posTaggedToken.getTag().getOpenClassIndicator() == PosTagOpenClassIndicator.CLOSED) {
        stats.closedClassCount++;
        if (!knownInRefCorpus)
          stats.closedClassUnknownInRefCorpus++;
        if (!knownInLexicon)
          stats.closedClassUnknownInLexicon++;
      } else if (posTaggedToken.getTag().getOpenClassIndicator() == PosTagOpenClassIndicator.OPEN) {
        stats.openClassCount++;
        if (!knownInRefCorpus)
          stats.openClassUnknownInRefCorpus++;
        if (!knownInLexicon)
          stats.openClassUnknownInLexicon++;
      }

      if (!knownInRefCorpus)
        stats.unknownTokenCount++;

      if (alphanumeric.matcher(token.getOriginalText()).find()) {
        String lowercase = word.toLowerCase(TalismaneSession.get(sessionId).getLocale());
        stats.lowerCaseWords.add(lowercase);
        stats.alphanumericCount++;
        if (!knownInRefCorpus)
          stats.unknownAlphanumericCount++;

        if (!knownInLexicon)
          stats.unknownAlphaInLexiconCount++;
      }

      stats.tokenCount++;

      Integer countObj = stats.posTagCounts.get(posTaggedToken.getTag().getCode());
      int count = countObj == null ? 0 : countObj.intValue();
      count++;
      stats.posTagCounts.put(posTaggedToken.getTag().getCode(), count);
    }

  }

  @Override
  public void onCompleteAnalysis() throws IOException {
    if (writer != null) {
      PosTagSet posTagSet = TalismaneSession.get(sessionId).getPosTagSet();
      for (PosTag posTag : posTagSet.getTags()) {
        if (!stats.posTagCounts.containsKey(posTag.getCode())) {
          stats.posTagCounts.put(posTag.getCode(), 0);
        }
      }

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
      writer.write(CSV.format("lexiconSize") + CSV.format(stats.words.size()) + "\n");
      writer.write(CSV.format("lexiconUnknownInRefCorpus") + CSV.format(unknownLexiconPercent * 100.0) + "\n");
      writer.write(CSV.format("tokenCount") + CSV.format(stats.tokenCount) + "\n");

      double unknownTokenPercent = ((double) stats.unknownTokenCount / (double) stats.tokenCount) * 100.0;
      writer.write(CSV.format("tokenUnknownInRefCorpus") + CSV.format(unknownTokenPercent) + "\n");

      double unknownInLexiconPercent = ((double) stats.unknownInLexiconCount / (double) stats.tokenCount) * 100.0;
      writer.write(CSV.format("tokenUnknownInRefLexicon") + CSV.format(unknownInLexiconPercent) + "\n");

      writer.write(CSV.format("lowercaseLexiconSize") + CSV.format(stats.lowerCaseWords.size()) + "\n");
      writer.write(CSV.format("lowercaseLexiconUnknownInRefCorpus") + CSV.format(unknownLowercaseLexiconPercent * 100.0) + "\n");
      writer.write(CSV.format("alphanumericCount") + CSV.format(stats.alphanumericCount) + "\n");

      double unknownAlphanumericPercent = ((double) stats.unknownAlphanumericCount / (double) stats.alphanumericCount) * 100.0;
      writer.write(CSV.format("alphaUnknownInRefCorpus") + CSV.format(unknownAlphanumericPercent) + "\n");

      double unknownAlphaInLexiconPercent = ((double) stats.unknownAlphaInLexiconCount / (double) stats.alphanumericCount) * 100.0;
      writer.write(CSV.format("alphaUnknownInRefLexicon") + CSV.format(unknownAlphaInLexiconPercent) + "\n");

      writer.write(CSV.format("openClassCount") + CSV.format(stats.openClassCount) + "\n");

      double openClassUnknownPercent = ((double) stats.openClassUnknownInRefCorpus / (double) stats.openClassCount) * 100.0;
      writer.write(CSV.format("openClassUnknownInRefCorpus") + CSV.format(openClassUnknownPercent) + "\n");

      double openClassUnknownInLexiconPercent = ((double) stats.openClassUnknownInLexicon / (double) stats.openClassCount) * 100.0;
      writer.write(CSV.format("openClassUnknownInRefLexicon") + CSV.format(openClassUnknownInLexiconPercent) + "\n");

      writer.write(CSV.format("closedClassCount") + CSV.format(stats.closedClassCount) + "\n");

      double closedClassUnknownPercent = ((double) stats.closedClassUnknownInRefCorpus / (double) stats.closedClassCount) * 100.0;
      writer.write(CSV.format("closedClassUnknownInRefCorpus") + CSV.format(closedClassUnknownPercent) + "\n");

      double closedClassUnknownInLexiconPercent = ((double) stats.closedClassUnknownInLexicon / (double) stats.closedClassCount) * 100.0;
      writer.write(CSV.format("closedClassUnknownInRefLexicon") + CSV.format(closedClassUnknownInLexiconPercent) + "\n");

      for (String posTag : stats.posTagCounts.keySet()) {
        int count = stats.posTagCounts.get(posTag);
        writer.write(CSV.format(posTag) + CSV.format(count) + CSV.format(((double) count / (double) stats.tokenCount) * 100.0) + "\n");
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
