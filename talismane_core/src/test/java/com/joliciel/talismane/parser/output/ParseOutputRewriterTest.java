package com.joliciel.talismane.parser.output;

import static org.junit.Assert.assertEquals;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.corpus.CorpusLine;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.parser.ParserRegexBasedCorpusReader;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class ParseOutputRewriterTest {
  private static final Logger LOG = LoggerFactory.getLogger(ParseOutputRewriterTest.class);

  @Test
  public void testGetCorpusLines() throws Exception {
    System.setProperty("config.file", "src/test/resources/testWithOutputRules.conf");
    ConfigFactory.invalidateCaches();
    final Config config = ConfigFactory.load();

    final TalismaneSession session = new TalismaneSession(config, "test");

    String input = "";
    input += "1\tAu\tau\tADP+DET\t0\troot\n";
    input += "2\tsein\tsein\tNOUN\t1\tfixed\n";
    input += "3\tmême\tmême\tADV\t1\tadvmod\n";
    input += "4\tdu\tdu\tADP+DET\t5\tcase\n";
    input += "5\tParti\tParti\tPROPN\t1\tnmod\n";
    input += "6\tsocialiste\tsocialiste\tADJ\t5\tfixed\n";
    input += "7\tauquel\tauquel\tADP+PRON\t8\tobl\n";
    input += "8\tappartient\tappartenir\tVERB\t5\tacl:relcl\n";
    input += "9\tM.\tmonsieur\tNOUN\t8\tnsubj\n";
    input += "10\tDupont\tDupont\tPROPN\t9\tflat:name\n";

    StringReader stringReader = new StringReader(input);
    ParserRegexBasedCorpusReader reader = new ParserRegexBasedCorpusReader(stringReader, config.getConfig("talismane.core.test.parser.input"), session);

    ParseConfiguration parseConfiguration = reader.nextConfiguration();
    final StringWriter writer = new StringWriter();
    try (ParseOutputRewriter rewriter = new ParseOutputRewriter(writer, session)) {
      List<CorpusLine> corpusLines = rewriter.getCorpusLines(parseConfiguration);
      int i = 1;
      for (CorpusLine corpusLine : corpusLines) {
        LOG.debug("line " + corpusLine.getIndex() + ": " + corpusLine.getElements());
        if (i == 1) {
          assertEquals(1, corpusLine.getIndex());
          assertEquals("à", corpusLine.getToken());
          assertEquals("à", corpusLine.getLemma());
          assertEquals("ADP", corpusLine.getPosTag());
          assertEquals(0, corpusLine.getGovernorIndex());
          assertEquals("root", corpusLine.getLabel());
        } else if (i == 2) {
          assertEquals(2, corpusLine.getIndex());
          assertEquals("le", corpusLine.getToken());
          assertEquals("le", corpusLine.getLemma());
          assertEquals("DET", corpusLine.getPosTag());
          assertEquals(1, corpusLine.getGovernorIndex());
          assertEquals("fixed", corpusLine.getLabel());
        } else if (i == 3) {
          assertEquals(3, corpusLine.getIndex());
          assertEquals("sein", corpusLine.getToken());
          assertEquals(1, corpusLine.getGovernorIndex());
          assertEquals("fixed", corpusLine.getLabel());
        } else if (i == 4) {
          assertEquals(4, corpusLine.getIndex());
          assertEquals("même", corpusLine.getToken());
          assertEquals(1, corpusLine.getGovernorIndex());
          assertEquals("advmod", corpusLine.getLabel());
        } else if (i == 5) {
          assertEquals(5, corpusLine.getIndex());
          assertEquals("de", corpusLine.getToken());
          assertEquals("de", corpusLine.getLemma());
          assertEquals("ADP", corpusLine.getPosTag());
          assertEquals(7, corpusLine.getGovernorIndex());
          assertEquals("case", corpusLine.getLabel());
        } else if (i == 6) {
          assertEquals(6, corpusLine.getIndex());
          assertEquals("le", corpusLine.getToken());
          assertEquals("le", corpusLine.getLemma());
          assertEquals("DET", corpusLine.getPosTag());
          assertEquals(7, corpusLine.getGovernorIndex());
          assertEquals("det", corpusLine.getLabel());
        } else if (i == 7) {
          assertEquals(7, corpusLine.getIndex());
          assertEquals("Parti", corpusLine.getToken());
          assertEquals(1, corpusLine.getGovernorIndex());
          assertEquals("nmod", corpusLine.getLabel());
        } else if (i == 8) {
          assertEquals(8, corpusLine.getIndex());
          assertEquals("socialiste", corpusLine.getToken());
          assertEquals(7, corpusLine.getGovernorIndex());
          assertEquals("fixed", corpusLine.getLabel());
        } else if (i == 9) {
          assertEquals(9, corpusLine.getIndex());
          assertEquals("à", corpusLine.getToken());
          assertEquals("à", corpusLine.getLemma());
          assertEquals("ADP", corpusLine.getPosTag());
          assertEquals(10, corpusLine.getGovernorIndex());
          assertEquals("case", corpusLine.getLabel());
        } else if (i == 10) {
          assertEquals(10, corpusLine.getIndex());
          assertEquals("lequel", corpusLine.getToken());
          assertEquals("lequel", corpusLine.getLemma());
          assertEquals("PRON", corpusLine.getPosTag());
          assertEquals(11, corpusLine.getGovernorIndex());
          assertEquals("obl", corpusLine.getLabel());
        } else if (i == 11) {
          assertEquals(11, corpusLine.getIndex());
          assertEquals("appartient", corpusLine.getToken());
          assertEquals("VERB", corpusLine.getPosTag());
          assertEquals(7, corpusLine.getGovernorIndex());
          assertEquals("acl:relcl", corpusLine.getLabel());
        } else if (i == 12) {
          assertEquals(12, corpusLine.getIndex());
          assertEquals("M.", corpusLine.getToken());
          assertEquals("NOUN", corpusLine.getPosTag());
          assertEquals(11, corpusLine.getGovernorIndex());
          assertEquals("nsubj", corpusLine.getLabel());
        } else if (i == 13) {
          assertEquals(13, corpusLine.getIndex());
          assertEquals("Dupont", corpusLine.getToken());
          assertEquals("PROPN", corpusLine.getPosTag());
          assertEquals(12, corpusLine.getGovernorIndex());
          assertEquals("flat:name", corpusLine.getLabel());
        }
        i++;
      }

      assertEquals(13, corpusLines.size());
    }
  }

}
