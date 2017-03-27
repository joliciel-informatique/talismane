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
package com.joliciel.talismane.languageDetector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.corpus.AbstractAnnotatedCorpusReader;
import com.joliciel.talismane.utils.ConfigUtils;
import com.typesafe.config.Config;

/**
 * A default corpus reader which assumes one text per line.
 * 
 * @author Assaf Urieli
 *
 */
public class TextPerLineCorpusReader extends AbstractAnnotatedCorpusReader implements LanguageDetectorAnnotatedCorpusReader {
  private Scanner scanner;
  private Locale currentLocale;

  private int sentenceCount = 0;
  private String sentence = null;
  private Map<Locale, Reader> readerMap;
  private Iterator<Locale> localeIterator;

  public TextPerLineCorpusReader(Config config, TalismaneSession session) throws IOException {
    super(config, session);
    String configPath = "language-corpus-map";
    InputStream languageCorpusMapFile = ConfigUtils.getFileFromConfig(config, configPath);
    try (Scanner languageCorpusMapScanner = new Scanner(new BufferedReader(new InputStreamReader(languageCorpusMapFile, "UTF-8")))) {

      this.readerMap = new HashMap<>();
      while (languageCorpusMapScanner.hasNextLine()) {
        String line = languageCorpusMapScanner.nextLine();
        String[] parts = line.split("\t");
        Locale locale = Locale.forLanguageTag(parts[0]);
        String corpusPath = parts[1];
        InputStream corpusFile = new FileInputStream(new File(corpusPath));
        Reader corpusReader = new BufferedReader(new InputStreamReader(corpusFile, session.getInputCharset().name()));
        readerMap.put(locale, corpusReader);
      }
    }

    this.localeIterator = readerMap.keySet().iterator();
  }

  public TextPerLineCorpusReader(Map<Locale, Reader> readerMap, TalismaneSession session) {
    super(null, session);
    this.readerMap = readerMap;
    this.localeIterator = readerMap.keySet().iterator();
  }

  @Override
  public boolean hasNextText() {
    if (this.getMaxSentenceCount() > 0 && sentenceCount >= this.getMaxSentenceCount()) {
      // we've reached the end, do nothing
    } else {
      while (sentence == null) {
        if (scanner != null && !scanner.hasNextLine()) {
          scanner.close();
          scanner = null;
        }

        while (scanner == null) {
          if (localeIterator.hasNext()) {
            currentLocale = localeIterator.next();
            Reader reader = readerMap.get(currentLocale);
            scanner = new Scanner(reader);
            if (scanner.hasNextLine()) {
              break;
            }
            scanner.close();
            scanner = null;
          } else {
            break;
          }
        }
        if (scanner == null)
          break;

        sentence = scanner.nextLine().trim();
        sentence = sentence.toLowerCase(Locale.ENGLISH);
        sentence = Normalizer.normalize(sentence, Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

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
  public LanguageTaggedText nextText() {
    String currentSentence = sentence;
    sentence = null;
    LanguageTaggedText languageTaggedText = new LanguageTaggedText(currentSentence, currentLocale);
    return languageTaggedText;
  }

  @Override
  public Map<String, String> getCharacteristics() {
    Map<String, String> attributes = super.getCharacteristics();

    return attributes;
  }

  @Override
  public boolean hasNextSentence() {
    return this.hasNextText();
  }

}
