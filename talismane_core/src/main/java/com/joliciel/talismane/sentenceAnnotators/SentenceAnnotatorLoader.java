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
package com.joliciel.talismane.sentenceAnnotators;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.utils.LogUtils;

public class SentenceAnnotatorLoader {
  public static final String SENTENCE_ANNOTATOR_DESCRIPTOR_KEY = "token_filter";

  private static final Logger LOG = LoggerFactory.getLogger(SentenceAnnotatorLoader.class);

  private final TalismaneSession talismaneSession;
  private final Set<String> registeredNames = new HashSet<>();
  private final Map<String, SentenceAnnotatorFactory<?>> registeredFactories = new HashMap<>();
  private final Map<String, Class<? extends TextReplacer>> registeredTextReplacers = new HashMap<>();

  private static final Map<String, SentenceAnnotatorLoader> instances = new HashMap<>();

  public static SentenceAnnotatorLoader getInstance(TalismaneSession talismaneSession) {
    SentenceAnnotatorLoader factory = instances.get(talismaneSession.getSessionId());
    if (factory == null) {
      factory = new SentenceAnnotatorLoader(talismaneSession);
      instances.put(talismaneSession.getSessionId(), factory);
    }
    return factory;
  }

  private SentenceAnnotatorLoader(TalismaneSession talismaneSession) {
    this.talismaneSession = talismaneSession;
    registeredNames.add(RegexAttributeAnnotator.class.getSimpleName());
    registeredNames.add(RegexTokenAnnotator.class.getSimpleName());
    registeredNames.add(TextReplaceFilter.class.getSimpleName());

    registeredTextReplacers.put(DiacriticRemover.class.getSimpleName(), DiacriticRemover.class);
    registeredTextReplacers.put(LowercaseFilter.class.getSimpleName(), LowercaseFilter.class);
    registeredTextReplacers.put(LowercaseKnownFirstWordFilter.class.getSimpleName(), LowercaseKnownFirstWordFilter.class);
    registeredTextReplacers.put(LowercaseKnownWordFilter.class.getSimpleName(), LowercaseKnownWordFilter.class);
    registeredTextReplacers.put(QuoteNormaliser.class.getSimpleName(), QuoteNormaliser.class);
    registeredTextReplacers.put(UppercaseSeriesFilter.class.getSimpleName(), UppercaseSeriesFilter.class);

  }

  /**
   * Reads a sequence of token filters from a scanner.
   * 
   * @throws SentenceAnnotatorLoadException
   */
  public List<SentenceAnnotator> loadSentenceAnnotators(Scanner scanner) throws SentenceAnnotatorLoadException {
    return this.loadSentenceAnnotators(scanner, null);
  }

  /**
   * Similar to {@link #loadSentenceAnnotators(Scanner)}, but keeps a
   * reference to the file, useful for finding the location of any descriptor
   * errors.
   * 
   * @param file
   *            the file to be read
   * @param charset
   *            the charset used to read the file
   * @throws SentenceAnnotatorLoadException
   * @throws IOException
   */
  public List<SentenceAnnotator> loadSentenceAnnotators(File file, Charset charset) throws SentenceAnnotatorLoadException, IOException {
    try (Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(file), charset)))) {
      return this.loadSentenceAnnotators(scanner, file.getCanonicalPath());
    }
  }

  /**
   * Reads a sequence of token filters from a scanner, with a path providing
   * clean error reporting.
   * 
   * @throws SentenceAnnotatorLoadException
   */
  public List<SentenceAnnotator> loadSentenceAnnotators(Scanner scanner, String path) throws SentenceAnnotatorLoadException {
    List<SentenceAnnotator> annotators = new ArrayList<>();
    Map<String, String> defaultParams = new HashMap<String, String>();
    int lineNumber = 0;
    while (scanner.hasNextLine()) {
      String descriptor = scanner.nextLine();
      lineNumber++;
      LOG.debug(descriptor);

      if (descriptor.trim().length() == 0 || descriptor.startsWith("#"))
        continue;
      if (descriptor.startsWith("DefaultParameters")) {
        defaultParams = new HashMap<String, String>();
        String[] tabs = descriptor.split("\t");
        for (int i = 1; i < tabs.length; i++) {
          String tab = tabs[i];
          int equalsPos = tab.indexOf('=');
          if (equalsPos < 0) {
            if (path != null)
              throw new SentenceAnnotatorLoadException(
                  "Unable to parse file " + path + ", line " + lineNumber + ": missing equals sign in " + descriptor);
            throw new SentenceAnnotatorLoadException("Unable to parse line " + lineNumber + ": missing equals sign in " + descriptor);
          }

          String paramName = tab.substring(0, tab.indexOf('='));
          String paramValue = tab.substring(tab.indexOf('=') + 1);
          defaultParams.put(paramName, paramValue);
        }
      } else {
        try {
          SentenceAnnotator annotator = this.loadSentenceAnnotator(descriptor, defaultParams);
          if (annotator != null)
            annotators.add(annotator);
        } catch (SentenceAnnotatorLoadException e) {
          if (path != null)
            throw new SentenceAnnotatorLoadException("Unable to parse file " + path + ", line " + lineNumber + ": " + descriptor, e);
          throw new SentenceAnnotatorLoadException("Unable to parse line " + lineNumber + ": " + descriptor, e);
        }
      }
    }

    return annotators;
  }

  /**
   * Gets an annotator corresponding to a given descriptor. The descriptor
   * should start with the registered annotator name, followed by a tab,
   * followed by whatever additional data is required.
   */
  public SentenceAnnotator loadSentenceAnnotator(String descriptor) throws SentenceAnnotatorLoadException {
    Map<String, String> parameterMap = new HashMap<String, String>();
    return this.loadSentenceAnnotator(descriptor, parameterMap);
  }

  public SentenceAnnotator loadSentenceAnnotator(String descriptor, Map<String, String> defaultParams) throws SentenceAnnotatorLoadException {
    try {
      String[] parts = descriptor.split("\t");
      String className = parts[0];

      SentenceAnnotator filter = null;
      if (className.equals(TextReplaceFilter.class.getSimpleName())) {
        filter = new TextReplaceFilter(registeredTextReplacers, descriptor, talismaneSession);
      } else if (className.equals(RegexTokenAnnotator.class.getSimpleName())) {
        filter = new RegexTokenAnnotator(descriptor, defaultParams, talismaneSession);
      } else if (className.equals(RegexAttributeAnnotator.class.getSimpleName())) {
        filter = new RegexAttributeAnnotator(descriptor, defaultParams, talismaneSession);
      } else if (this.registeredFactories.containsKey(className)) {
        filter = this.registeredFactories.get(className).construct(descriptor, defaultParams, talismaneSession);
      } else {
        throw new SentenceAnnotatorLoadException("Unknown sentence annotator class: " + className);
      }
      if (filter.isExcluded())
        return null;

      return filter;
    } catch (IllegalArgumentException e) {
      LogUtils.logError(LOG, e);
      throw new RuntimeException(e);
    }
  }

  /**
   * Registers a sentence annotator name, with a factory used to construct it.
   * 
   * @param name
   *            the name used to recognise this annotator
   * @param factory
   *            the factory to create this annotator
   * 
   */
  public <T extends SentenceAnnotator> void registerSentenceAnnotator(String name, SentenceAnnotatorFactory<T> factory) {
    this.registeredNames.add(name);
    this.registeredFactories.put(name, factory);
  }

}
