package com.joliciel.talismane.tokeniser;

import static org.junit.Assert.assertEquals;

import com.joliciel.talismane.TalismaneTest;
import org.junit.Test;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.rawText.Sentence;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class PretokenisedSequenceTest extends TalismaneTest {

  @Test
  public void testAddTokenString() throws Exception {
    System.setProperty("config.file", "src/test/resources/test.conf");
    ConfigFactory.invalidateCaches();
    final Config config = ConfigFactory.load();

    final String sessionId = "test";
    final Sentence sentence = new Sentence("« Il est là.  »", sessionId);

    PretokenisedSequence sequence = new PretokenisedSequence(sentence, sessionId);
    sequence.addToken("« ");
    sequence.addToken("Il");
    sequence.addToken("est");
    sequence.addToken("là");
    sequence.addToken(".");
    sequence.addToken(" »");

    System.out.println(sequence.toString());

    assertEquals(6, sequence.size());
  }

}
