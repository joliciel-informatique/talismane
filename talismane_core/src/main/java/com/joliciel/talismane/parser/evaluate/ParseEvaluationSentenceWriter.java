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
package com.joliciel.talismane.parser.evaluate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.Talismane.Module;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.parser.DependencyArc;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.parser.Transition;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.utils.CSVFormatter;
import com.typesafe.config.Config;

/**
 * Writes each sentence to a CSV file as it gets parsed, along with the guesses
 * made and their probability.
 * 
 * @author Assaf Urieli
 *
 */
public class ParseEvaluationSentenceWriter implements ParseEvaluationObserver {
  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(ParseEvaluationSentenceWriter.class);
  private static final CSVFormatter CSV = new CSVFormatter();
  private final Writer csvFileWriter;
  private final int guessCount;
  private final boolean hasTokeniser;
  private final boolean hasPosTagger;

  public ParseEvaluationSentenceWriter(File outDir, String sessionId) throws FileNotFoundException {
    Config config = ConfigFactory.load();
    Config parserConfig = config.getConfig("talismane.core." + sessionId + ".parser");
    Config evalConfig = parserConfig.getConfig("evaluate");

    File csvFile = new File(outDir, TalismaneSession.get(sessionId).getBaseName() + "_sentences.csv");
    this.csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false), TalismaneSession.get(sessionId).getCsvCharset()));
    this.guessCount = evalConfig.getInt("output-guess-count");

    Module startModule = Module.valueOf(evalConfig.getString("start-module"));
    this.hasTokeniser = (startModule == Module.tokeniser);
    this.hasPosTagger = (startModule == Module.tokeniser || startModule == Module.posTagger);
  }

  @Override
  public void onParseEnd(ParseConfiguration realConfiguration, List<ParseConfiguration> guessedConfigurations) throws IOException {
    TreeSet<Integer> startIndexes = new TreeSet<Integer>();
    for (PosTaggedToken posTaggedToken : realConfiguration.getPosTagSequence()) {
      if (!posTaggedToken.getTag().equals(PosTag.ROOT_POS_TAG)) {
        Token token = posTaggedToken.getToken();
        startIndexes.add(token.getStartIndex());
      }
    }
    if (hasTokeniser || hasPosTagger) {
      int i = 0;
      for (ParseConfiguration guessedConfiguration : guessedConfigurations) {
        for (PosTaggedToken posTaggedToken : guessedConfiguration.getPosTagSequence()) {
          if (!posTaggedToken.getTag().equals(PosTag.ROOT_POS_TAG)) {
            Token token = posTaggedToken.getToken();
            startIndexes.add(token.getStartIndex());
          }
        }
        i++;
        if (i == guessCount)
          break;
      }
    }
    Map<Integer, Integer> startIndexMap = new HashMap<Integer, Integer>();
    int j = 0;
    for (int startIndex : startIndexes) {
      startIndexMap.put(startIndex, j++);
    }

    PosTagSequence posTagSequence = realConfiguration.getPosTagSequence();
    PosTaggedToken[] realTokens = new PosTaggedToken[startIndexes.size()];
    for (PosTaggedToken posTaggedToken : posTagSequence) {
      if (!posTaggedToken.getTag().equals(PosTag.ROOT_POS_TAG)) {
        realTokens[startIndexMap.get(posTaggedToken.getToken().getStartIndex())] = posTaggedToken;
      }
    }

    for (PosTaggedToken posTaggedToken : realTokens) {
      if (posTaggedToken != null) {
        csvFileWriter.write(CSV.format(posTaggedToken.getToken().getOriginalText()));
      } else {
        csvFileWriter.write(CSV.getCsvSeparator());
      }
    }

    csvFileWriter.write("\n");
    for (PosTaggedToken posTaggedToken : realTokens) {
      if (posTaggedToken != null) {
        csvFileWriter.write(CSV.format(posTaggedToken.getTag().getCode()));
      } else {
        csvFileWriter.write(CSV.getCsvSeparator());
      }
    }
    csvFileWriter.write("\n");
    for (PosTaggedToken posTaggedToken : realTokens) {
      if (posTaggedToken != null) {
        DependencyArc realArc = realConfiguration.getGoverningDependency(posTaggedToken);
        String realLabel = realArc.getLabel() == null ? "null" : realArc.getLabel();
        csvFileWriter.write(CSV.format(realLabel));
      } else {
        csvFileWriter.write(CSV.getCsvSeparator());
      }
    }
    csvFileWriter.write("\n");
    for (PosTaggedToken posTaggedToken : realTokens) {
      if (posTaggedToken != null) {
        DependencyArc realArc = realConfiguration.getGoverningDependency(posTaggedToken);
        int startIndex = -1;
        if (realArc != null) {
          PosTaggedToken head = realArc.getHead();
          if (!head.getTag().equals(PosTag.ROOT_POS_TAG)) {
            startIndex = head.getToken().getStartIndex();
          }
        }
        if (startIndex < 0)
          csvFileWriter.write(CSV.format("ROOT"));
        else
          csvFileWriter.write(CSV.getColumnLabel(startIndexMap.get(startIndex)) + CSV.getCsvSeparator());
      } else {
        csvFileWriter.write(CSV.getCsvSeparator());
      }
    }
    csvFileWriter.write("\n");

    for (int i = 0; i < guessCount; i++) {
      if (i < guessedConfigurations.size()) {
        ParseConfiguration guessedConfiguration = guessedConfigurations.get(i);
        PosTaggedToken[] guessedTokens = new PosTaggedToken[startIndexes.size()];
        for (PosTaggedToken posTaggedToken : guessedConfiguration.getPosTagSequence()) {
          if (!posTaggedToken.getTag().equals(PosTag.ROOT_POS_TAG)) {
            guessedTokens[startIndexMap.get(posTaggedToken.getToken().getStartIndex())] = posTaggedToken;
          }
        }

        if (hasTokeniser) {
          for (PosTaggedToken posTaggedToken : guessedTokens) {
            if (posTaggedToken != null) {
              csvFileWriter.write(CSV.format(posTaggedToken.getToken().getOriginalText()));
            } else {
              csvFileWriter.write(CSV.getCsvSeparator());
            }
          }

          csvFileWriter.write("\n");
        }

        if (hasPosTagger) {
          for (PosTaggedToken posTaggedToken : guessedTokens) {
            if (posTaggedToken != null) {
              csvFileWriter.write(CSV.format(posTaggedToken.getTag().getCode()));
            } else {
              csvFileWriter.write(CSV.getCsvSeparator());
            }
          }
          csvFileWriter.write("\n");
        }

        for (PosTaggedToken posTaggedToken : guessedTokens) {
          if (posTaggedToken != null) {
            DependencyArc guessedArc = guessedConfiguration.getGoverningDependency(posTaggedToken);
            String guessedLabel = "";
            if (guessedArc != null) {
              guessedLabel = guessedArc.getLabel() == null ? "null" : guessedArc.getLabel();
            }
            csvFileWriter.write(CSV.format(guessedLabel));
          } else {
            csvFileWriter.write(CSV.getCsvSeparator());
          }
        }
        csvFileWriter.write("\n");
        for (PosTaggedToken posTaggedToken : guessedTokens) {
          if (posTaggedToken != null) {
            DependencyArc guessedArc = guessedConfiguration.getGoverningDependency(posTaggedToken);
            int startIndex = -1;
            if (guessedArc != null) {
              PosTaggedToken head = guessedArc.getHead();
              if (!head.getTag().equals(PosTag.ROOT_POS_TAG)) {
                startIndex = head.getToken().getStartIndex();
              }
            }
            if (startIndex < 0)
              csvFileWriter.write(CSV.format("ROOT"));
            else
              csvFileWriter.write(CSV.getColumnLabel(startIndexMap.get(startIndex)) + CSV.getCsvSeparator());
          } else {
            csvFileWriter.write(CSV.getCsvSeparator());
          }
        }
        csvFileWriter.write("\n");
        for (PosTaggedToken posTaggedToken : guessedTokens) {
          if (posTaggedToken != null) {
            DependencyArc guessedArc = guessedConfiguration.getGoverningDependency(posTaggedToken);
            double prob = 1.0;
            if (guessedArc != null) {
              Transition transition = guessedConfiguration.getTransition(guessedArc);
              if (transition != null)
                prob = transition.getDecision().getProbability();
            }
            csvFileWriter.write(CSV.format(prob));
          } else {
            csvFileWriter.write(CSV.getCsvSeparator());
          }
        }
        csvFileWriter.write("\n");

      } else {
        csvFileWriter.write("\n");
        csvFileWriter.write("\n");
      } // have more configurations
    } // next guessed configuration
    csvFileWriter.flush();
  }

  @Override
  public void onEvaluationComplete() throws IOException {
    csvFileWriter.flush();
    csvFileWriter.close();

  }

  @Override
  public void onParseStart(ParseConfiguration realConfiguration, List<PosTagSequence> posTagSequences) {
  }
}
