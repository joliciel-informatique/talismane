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
package com.joliciel.talismane.sentenceDetector;

import java.io.Reader;
import java.util.Map;
import java.util.Scanner;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.corpus.AbstractAnnotatedCorpusReader;
import com.joliciel.talismane.rawText.Sentence;
import com.typesafe.config.Config;

/**
 * A default corpus reader which assumes one sentence per line.
 * 
 * @author Assaf Urieli
 *
 */
public class SentencePerLineCorpusReader extends AbstractAnnotatedCorpusReader implements SentenceDetectorAnnotatedCorpusReader {
  private final Scanner scanner;
  private int sentenceCount = 0;
  String sentence = null;
  private final TalismaneSession session;

  public SentencePerLineCorpusReader(Reader reader, Config config, TalismaneSession session) {
    super(config, session);
    this.session = session;
    this.scanner = new Scanner(reader);
  }

  @Override
  public boolean hasNextSentence() {
    if (this.getMaxSentenceCount() > 0 && sentenceCount >= this.getMaxSentenceCount()) {
      // we've reached the end, do nothing
    } else {

      while (sentence == null) {
        if (!scanner.hasNextLine()) {
          break;
        }

        sentence = scanner.nextLine().trim();
        if (sentence.length() == 0) {
          sentence = null;
          continue;
        }

        boolean includeMe = true;

        // check cross-validation
        if (this.getCrossValidationSize() > 0) {
          if (this.getIncludeIndex() >= 0) {
            if (sentenceCount % this.getCrossValidationSize() != this.getIncludeIndex()) {
              includeMe = false;
            }
          } else if (this.getExcludeIndex() >= 0) {
            if (sentenceCount % this.getCrossValidationSize() == this.getExcludeIndex()) {
              includeMe = false;
            }
          }
        }

        if (this.getStartSentence() > sentenceCount) {
          includeMe = false;
        }

        sentenceCount++;

        if (!includeMe) {
          sentence = null;
          continue;
        }

      }
    }
    return sentence != null;
  }

  @Override
  public Sentence nextSentence() {
    String currentSentence = sentence;
    sentence = null;
    return new Sentence(currentSentence, session);
  }

  @Override
  public Map<String, String> getCharacteristics() {
    Map<String, String> attributes = super.getCharacteristics();
    return attributes;
  }

  @Override
  public boolean isNewParagraph() {
    return false;
  }
}
