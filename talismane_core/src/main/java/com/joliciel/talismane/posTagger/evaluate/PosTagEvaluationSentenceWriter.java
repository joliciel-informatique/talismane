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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.tokeniser.TaggedToken;
import com.joliciel.talismane.utils.CSVFormatter;
import com.typesafe.config.Config;

/**
 * Writes each sentence to a CSV file as it gets parsed, along with the guesses
 * made and their probability.
 * 
 * @author Assaf Urieli
 *
 */
public class PosTagEvaluationSentenceWriter implements PosTagEvaluationObserver {
  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(PosTagEvaluationSentenceWriter.class);
  private static final CSVFormatter CSV = new CSVFormatter();
  private final Writer writer;
  private final int guessCount;

  public PosTagEvaluationSentenceWriter(File outDir, TalismaneSession session) throws FileNotFoundException {
    Config config = session.getConfig();
    Config posTaggerConfig = config.getConfig("talismane.core.pos-tagger");
    Config evalConfig = posTaggerConfig.getConfig("evaluate");

    File csvFile = new File(outDir, session.getBaseName() + "_sentences.csv");
    this.writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false), session.getCsvCharset()));
    this.guessCount = evalConfig.getInt("output-guess-count");
  }

  @Override
  public void onNextPosTagSequence(PosTagSequence realSequence, List<PosTagSequence> guessedSequences) throws IOException {
    for (int i = 0; i < realSequence.size(); i++) {
      String token = realSequence.get(i).getToken().getAnalyisText();
      writer.write(CSV.format(token));
    }
    writer.write("\n");
    for (int i = 0; i < realSequence.size(); i++)
      writer.write(CSV.format(realSequence.get(i).getTag().getCode()));
    writer.write("\n");

    for (int k = 0; k < guessCount; k++) {
      PosTagSequence posTagSequence = null;
      if (k < guessedSequences.size()) {
        posTagSequence = guessedSequences.get(k);
      } else {
        writer.write("\n");
        writer.write("\n");
        continue;
      }
      int j = 0;
      String probs = "";
      for (int i = 0; i < realSequence.size(); i++) {
        TaggedToken<PosTag> realToken = realSequence.get(i);
        TaggedToken<PosTag> testToken = posTagSequence.get(j);
        boolean tokenError = false;
        if (realToken.getToken().getStartIndex() == testToken.getToken().getStartIndex()
            && realToken.getToken().getEndIndex() == testToken.getToken().getEndIndex()) {
          // no token error
          j++;
          if (j == posTagSequence.size()) {
            j--;
          }
        } else {
          tokenError = true;
          while (realToken.getToken().getEndIndex() >= testToken.getToken().getEndIndex()) {
            j++;
            if (j == posTagSequence.size()) {
              j--;
              break;
            }
            testToken = posTagSequence.get(j);
          }
        }
        if (tokenError) {
          writer.write(CSV.format("BAD_TOKEN"));
        } else {
          writer.write(CSV.format(testToken.getTag().getCode()));
        }
        probs += CSV.format(testToken.getDecision().getProbability());
      }
      writer.write("\n");
      writer.write(probs + "\n");
    }
    writer.flush();
  }

  @Override
  public void onEvaluationComplete() throws IOException {
    writer.flush();
    writer.close();
  }

}
