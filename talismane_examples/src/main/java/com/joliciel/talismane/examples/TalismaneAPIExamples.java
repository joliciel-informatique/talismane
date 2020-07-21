///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2015 Joliciel Informatique
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
package com.joliciel.talismane.examples;

import java.util.List;

import com.joliciel.talismane.AnnotatedText;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.parser.ParseTree;
import com.joliciel.talismane.parser.Parser;
import com.joliciel.talismane.parser.Parsers;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTagger;
import com.joliciel.talismane.posTagger.PosTaggers;
import com.joliciel.talismane.rawText.RawText;
import com.joliciel.talismane.rawText.RawTextAnnotator;
import com.joliciel.talismane.rawText.Sentence;
import com.joliciel.talismane.sentenceAnnotators.SentenceAnnotator;
import com.joliciel.talismane.sentenceDetector.SentenceDetector;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.Tokeniser;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

/**
 * A class showing how to analyse a sentence using the Talismane API and an
 * existing language pack.<br/>
 * 
 * Usage (barring the classpath, which must include Talismane jars):<br/>
 * 
 * <pre>
 * java -Dconfig.file=[languagePackConfigFile] com.joliciel.talismane.examples.TalismaneAPITest
 * </pre>
 */
public class TalismaneAPIExamples {

  public static void main(String[] args) throws Exception {
    OptionParser parser = new OptionParser();
    OptionSpec<Integer> exampleOption = parser.accepts("example", "which example to run").withRequiredArg().ofType(Integer.class);

    OptionSpec<String> sessionIdOption = parser.accepts("sessionId", "the current session id - configuration read as talismane.core.[sessionId]")
        .withRequiredArg().required().ofType(String.class);

    if (args.length <= 1) {
      parser.printHelpOn(System.out);
      return;
    }

    OptionSet options = parser.parse(args);

    int example = 1;
    if (options.has(exampleOption)) {
      example = options.valueOf(exampleOption);
    }

    String sessionId = options.valueOf(sessionIdOption);

    if (example == 1)
      example1(sessionId);
    else
      example2(sessionId);
  }

  /**
   * An example tokenising, pos-tagging and parsing a pre-existing sentence.
   */
  public static void example1(String sessionId) throws Exception {
    String text = "Les amoureux qui se bécotent sur les bancs publics ont des petites gueules bien sympathiques.";

    // tokenise the text
    Tokeniser tokeniser = Tokeniser.getInstance(sessionId);
    TokenSequence tokenSequence = tokeniser.tokeniseText(text);

    // pos-tag the token sequence
    PosTagger posTagger = PosTaggers.getPosTagger(sessionId);
    PosTagSequence posTagSequence = posTagger.tagSentence(tokenSequence);
    System.out.println(posTagSequence);

    // parse the pos-tag sequence
    Parser parser = Parsers.getParser(sessionId);
    ParseConfiguration parseConfiguration = parser.parseSentence(posTagSequence);
    ParseTree parseTree = new ParseTree(parseConfiguration, true);
    System.out.println(parseTree);

  }

  /**
   * Similar to example1, but begins with filtering and sentence detection.
   */
  public static void example2(String sessionId) throws Exception {
    String text = "Les gens qui voient de travers pensent que les bancs verts qu'on voit sur les trottoirs "
        + "sont faits pour les impotents ou les ventripotents. " + "Mais c'est une absurdité, car, à la vérité, ils sont là, c'est notoire, "
        + "pour accueillir quelque temps les amours débutants.";

    RawText rawText = new RawText(text, true, sessionId);

    // filter the text - in the case where filters are defined
    // to skip certain parts of the text (e.g. XML) or to fix encoding
    // issues (e.g. replace &quot; with ")
    for (RawTextAnnotator filter : TalismaneSession.get(sessionId).getTextAnnotators()) {
      filter.annotate(rawText);
    }

    // retrieve the processed text after filters have been applied
    AnnotatedText processedText = rawText.getProcessedText();

    // detect sentences
    SentenceDetector sentenceDetector = SentenceDetector.getInstance(sessionId);
    sentenceDetector.detectSentences(processedText);

    // the detected sentences can be retrieved directly from the raw text
    // this allows annotations made on the sentences to get reflected in the
    // raw text
    List<Sentence> sentences = rawText.getDetectedSentences();

    for (Sentence sentence : sentences) {
      // apply any sentence annotators to prepare the text for analysis
      // via deterministic rules (e.g. token boundaries or pos-tag
      // assignment for a given word)
      for (SentenceAnnotator annotator : TalismaneSession.get(sessionId).getSentenceAnnotators()) {
        annotator.annotate(sentence);
      }

      // tokenise the text
      Tokeniser tokeniser = Tokeniser.getInstance(sessionId);
      TokenSequence tokenSequence = tokeniser.tokeniseSentence(sentence);

      // pos-tag the token sequence
      PosTagger posTagger = PosTaggers.getPosTagger(sessionId);
      PosTagSequence posTagSequence = posTagger.tagSentence(tokenSequence);
      System.out.println(posTagSequence);

      // parse the pos-tag sequence
      Parser parser = Parsers.getParser(sessionId);
      ParseConfiguration parseConfiguration = parser.parseSentence(posTagSequence);
      System.out.println(parseConfiguration);

      ParseTree parseTree = new ParseTree(parseConfiguration, true);
      System.out.println(parseTree);
    }

  }
}
