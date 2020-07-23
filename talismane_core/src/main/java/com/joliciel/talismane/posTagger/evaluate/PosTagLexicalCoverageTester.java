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
package com.joliciel.talismane.posTagger.evaluate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.stats.FScoreCalculator;
import com.joliciel.talismane.tokeniser.TaggedToken;
import com.joliciel.talismane.utils.CSVFormatter;

/**
 * An observer for testing lexicon coverage of the corpus.
 * 
 * @author Assaf Urieli
 *
 */
public class PosTagLexicalCoverageTester implements PosTagEvaluationObserver {
  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(PosTagLexicalCoverageTester.class);
  private static final CSVFormatter CSV = new CSVFormatter();
  private FScoreCalculator<String> fscoreUnknownInLexicon = new FScoreCalculator<String>();

  private final Map<String, Integer> unknownWords = new TreeMap<String, Integer>();
  private final Set<String> knownWords = new HashSet<String>();
  private final Set<String> closedCategoryMismatches = new HashSet<String>();

  int knownWordCount;
  int unknownWordCount;

  private final File fScoreFile;

  public PosTagLexicalCoverageTester(File outDir, String sessionId) {
    this.fScoreFile = new File(outDir, TalismaneSession.get(sessionId).getBaseName() + ".lexiconCoverage.csv");
  }

  @Override
  public void onNextPosTagSequence(PosTagSequence realSequence, List<PosTagSequence> guessedSequences) throws TalismaneException {
    PosTagSequence guessedSequence = guessedSequences.get(0);

    for (int i = 0; i < realSequence.size(); i++) {
      TaggedToken<PosTag> realToken = realSequence.get(i);
      TaggedToken<PosTag> testToken = guessedSequence.get(i);

      boolean tokenUnknown = realToken.getToken().getPossiblePosTags() != null && realToken.getToken().getPossiblePosTags().size() == 0;
      if (tokenUnknown) {
        fscoreUnknownInLexicon.increment(realToken.getTag().getCode(), testToken.getTag().getCode());
        unknownWordCount++;
        Integer countObj = unknownWords.get(realToken.getTag() + "|" + realToken.getToken().getAnalyisText());
        int count = countObj == null ? 0 : countObj.intValue();
        unknownWords.put(realToken.getTag() + "|" + realToken.getToken().getAnalyisText(), count + 1);
      } else {
        knownWordCount++;
        knownWords.add(realToken.getToken().getAnalyisText());
      }

      if (realToken.getTag().getOpenClassIndicator().isClosed() && !realToken.getToken().getPossiblePosTags().contains(realToken.getTag())) {
        closedCategoryMismatches.add(realToken.getTag() + "|" + realToken.getToken().getAnalyisText());
      }
    }
  }

  @Override
  public void onEvaluationComplete() throws IOException {
    Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fScoreFile), "UTF-8"));
    fscoreUnknownInLexicon.writeScoresToCSV(writer);

    writer.write("\n");
    writer.write(
        CSV.format("Known") + CSV.format(knownWordCount) + CSV.format((double) knownWordCount / (double) (knownWordCount + unknownWordCount) * 100.0) + "\n");
    writer.write(CSV.format("Unknown") + CSV.format(unknownWordCount)
        + CSV.format((double) unknownWordCount / (double) (knownWordCount + unknownWordCount) * 100.0) + "\n");
    writer.write(CSV.format("Unique known") + CSV.format(knownWords.size())
        + CSV.format((double) knownWords.size() / (double) (knownWords.size() + unknownWords.size()) * 100.0) + "\n");
    writer.write(CSV.format("Unique unknown") + CSV.format(unknownWords.size())
        + CSV.format((double) unknownWords.size() / (double) (knownWords.size() + unknownWords.size()) * 100.0) + "\n");
    writer.write("\n");
    writer.write("Missing closed tags\n");
    for (String closedTagMismatch : closedCategoryMismatches) {
      writer.write(CSV.format(closedTagMismatch) + "\n");
    }
    writer.write("\n");
    writer.write("Unknown words\n");
    for (String unknownWord : unknownWords.keySet()) {
      writer.write(CSV.format(unknownWord) + CSV.format(unknownWords.get(unknownWord)) + "\n");
    }
    writer.flush();
    writer.close();
  }

}
