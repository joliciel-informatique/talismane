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
package com.joliciel.talismane.lexicon;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.sentenceAnnotators.SentenceAnnotatorLoadException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

/**
 * Used to deserialize a zip file containing an ordered set of lexicons
 * serialized by the {@link LexiconReader}.
 * 
 * @author Assaf Urieli
 *
 */
public class LexiconDeserializer {
  private static final Logger LOG = LoggerFactory.getLogger(LexiconDeserializer.class);

  public static void main(String[] args) throws IOException, SentenceAnnotatorLoadException, TalismaneException, ReflectiveOperationException {
    OptionParser parser = new OptionParser();
    parser.accepts("testLexicon", "test lexicon");

    OptionSpec<String> lexiconFilesOption = parser.accepts("lexicon", "lexicon(s), semi-colon delimited").withRequiredArg().ofType(String.class)
        .withValuesSeparatedBy(';');
    OptionSpec<String> wordsOption = parser.accepts("words", "comma-delimited list of words to test").withRequiredArg().required().ofType(String.class)
        .withValuesSeparatedBy(',');

    if (args.length <= 1) {
      parser.printHelpOn(System.out);
      return;
    }

    OptionSet options = parser.parse(args);

    Config config = null;
    if (options.has(lexiconFilesOption)) {
      List<String> lexiconFiles = options.valuesOf(lexiconFilesOption);

      Map<String, Object> values = new HashMap<>();
      values.put("talismane.core.lexicons", lexiconFiles);
      config = ConfigFactory.parseMap(values).withFallback(ConfigFactory.load());
    } else {
      config = ConfigFactory.load();
    }

    String sessionId = "";
    TalismaneSession talismaneSession = new TalismaneSession(config, sessionId);

    List<String> words = options.valuesOf(wordsOption);

    PosTaggerLexicon mergedLexicon = talismaneSession.getMergedLexicon();
    for (String word : words) {
      LOG.info("################");
      LOG.info("Word: " + word);
      List<LexicalEntry> entries = mergedLexicon.getEntries(word);
      for (LexicalEntry entry : entries) {
        LOG.info(entry + ", Full morph: " + entry.getMorphology() + ", PosTags: " + mergedLexicon.findPossiblePosTags(word));
      }
    }
  }
}
