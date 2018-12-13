package com.joliciel.talismane.corpus;

import static org.junit.Assert.assertEquals;

import java.io.StringReader;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.corpus.CorpusLine.CorpusElement;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class TokenPerLineCorpusReaderTest {
  private static final Logger LOG = LoggerFactory.getLogger(TokenPerLineCorpusReaderTest.class);

  @Test
  public void testReadCorpusLines() throws Exception {
    System.setProperty("config.file", "src/test/resources/testWithCorpusRules.conf");
    ConfigFactory.invalidateCaches();
    final Config config = ConfigFactory.load();

    final TalismaneSession session = new TalismaneSession(config, "test");

    String input = "# Skip me\n";
    input += "1-2\tAu\t_\t_\t_\t_\n";
    input += "1\tÀ\tà\tADP\t0\troot\n";
    input += "2\tle\tle\tDET\t1\tfixed\n";
    input += "3\tsein\tsein\tNOUN\t1\tfixed\n";
    input += "4\tmême\tmême\tADV\t1\tadvmod\n";
    input += "5-6\tdu\t_\t_\t_\t_\n";
    input += "5\tde\tde\tADP\t7\tcase\n";
    input += "6\tle\tle\tDET\t7\tdet\n";
    input += "7\tParti\tParti\tPROPN\t1\tnmod\n";
    input += "8\tsocialiste\tsocialiste\tADJ\t7\tfixed\n";
    input += "# skip me too\n";
    input += "9-10\tauquel\t_\t_\t_\t_\n";
    input += "9\tà\tà\tADP\t10\tcase\n";
    input += "10\tlequel\tlequel\tPRON\t11\tobl\n";
    input += "11\tappartient\tappartenir\tVERB\t7\tacl:relcl\n";
    input += "12\tM.\tmonsieur\tNOUN\t11\tnsubj\n";
    input += "13\tDupont\tDupont\tPROPN\t12\tflat:name\n";

    StringReader reader = new StringReader(input);
    Config readerConfig = config.getConfig("talismane.core.test.input");
    TokenPerLineCorpusReader corpusReader = new TokenPerLineCorpusReader(reader, readerConfig, session);
    List<CorpusLine> corpusLines = corpusReader.nextCorpusLineList();
    int i = 0;
    for (CorpusLine corpusLine : corpusLines) {
      LOG.debug(corpusLine.getElements().toString());
      if (i == 0) {
        assertEquals(1, corpusLine.getIndex());
        assertEquals("Au", corpusLine.getElement(CorpusElement.TOKEN));
        assertEquals("ADP+DET", corpusLine.getElement(CorpusElement.POSTAG));
        assertEquals(0, corpusLine.getGovernorIndex());
        assertEquals("root", corpusLine.getElement(CorpusElement.LABEL));
      } else if (i == 1) {
        assertEquals(2, corpusLine.getIndex());
        assertEquals("sein", corpusLine.getElement(CorpusElement.TOKEN));
        assertEquals("NOUN", corpusLine.getElement(CorpusElement.POSTAG));
        assertEquals(1, corpusLine.getGovernorIndex());
        assertEquals("fixed", corpusLine.getElement(CorpusElement.LABEL));
      } else if (i == 3) {
        assertEquals(4, corpusLine.getIndex());
        assertEquals("du", corpusLine.getElement(CorpusElement.TOKEN));
        assertEquals("ADP+DET", corpusLine.getElement(CorpusElement.POSTAG));
        assertEquals(5, corpusLine.getGovernorIndex());
        assertEquals("case", corpusLine.getElement(CorpusElement.LABEL));
      } else if (i == 6) {
        assertEquals(7, corpusLine.getIndex());
        assertEquals("auquel", corpusLine.getElement(CorpusElement.TOKEN));
        assertEquals("ADP+PRON", corpusLine.getElement(CorpusElement.POSTAG));
        assertEquals(8, corpusLine.getGovernorIndex());
        assertEquals("obl", corpusLine.getElement(CorpusElement.LABEL));
      }
      i++;
    }
    assertEquals(10, corpusLines.size());
  }

}
