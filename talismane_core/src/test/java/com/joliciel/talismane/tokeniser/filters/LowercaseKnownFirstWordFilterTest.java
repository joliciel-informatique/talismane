package com.joliciel.talismane.tokeniser.filters;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;

import com.joliciel.talismane.TalismaneTest;
import org.junit.Test;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.lexicon.Diacriticizer;
import com.joliciel.talismane.rawText.Sentence;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LowercaseKnownFirstWordFilterTest extends TalismaneTest {

  @Test
  public void testReplace() throws Exception {
    Diacriticizer diacriticizer = mock(Diacriticizer.class);
    when(diacriticizer.diacriticize("J'")).thenReturn(new HashSet<>(Arrays.asList("j'")));
    when(diacriticizer.diacriticize("Il")).thenReturn(new HashSet<>(Arrays.asList("il")));

    System.setProperty("config.file", "src/test/resources/test.conf");
    ConfigFactory.invalidateCaches();
    final Config config = ConfigFactory.load();

    final String sessionId = "test";
    TalismaneSession.get(sessionId).setDiacriticizer(diacriticizer);

    LowercaseKnownFirstWordFilter filter = new LowercaseKnownFirstWordFilter(sessionId);

    String text = "J'avais oublié : Il est Malade.";
    Sentence sentence = new Sentence(text, sessionId);
    TokenSequence tokenSequence = new TokenSequence(sentence, sessionId);
    tokenSequence.addToken("".length(), "J'".length());
    tokenSequence.addToken("J'".length(), "J'avais".length());
    tokenSequence.addToken("J'avais ".length(), "J'avais oublié".length());
    tokenSequence.addToken("J'avais oublié ".length(), "J'avais oublié :".length());
    tokenSequence.addToken("J'avais oublié : ".length(), "J'avais oublié : Il".length());
    tokenSequence.addToken("J'avais oublié : Il ".length(), "J'avais oublié : Il est".length());
    tokenSequence.addToken("J'avais oublié : Il est ".length(), "J'avais oublié : Il est Malade".length());
    tokenSequence.addToken("J'avais oublié : Il est Malade".length(), "J'avais oublié : Il est Malade.".length());

    filter.apply(tokenSequence);
    System.out.println(tokenSequence);

    StringBuilder sb = new StringBuilder();
    for (Token token : tokenSequence) {
      sb.append(token.getText());
      sb.append('|');
    }

    assertEquals("j'|avais|oublié|:|il|est|Malade|.|", sb.toString());
  }

  @Test
  public void testReplaceLongWord() throws Exception {
    Diacriticizer diacriticizer = mock(Diacriticizer.class);
    when(diacriticizer.diacriticize("Aujourd'hui")).thenReturn(new HashSet<>(Arrays.asList("aujourd'hui")));
    when(diacriticizer.diacriticize("Parce que")).thenReturn(new HashSet<>(Arrays.asList("parce que")));

    
    System.setProperty("config.file", "src/test/resources/test.conf");
    ConfigFactory.invalidateCaches();
    final Config config = ConfigFactory.load();

    final String sessionId = "test";
    TalismaneSession.get(sessionId).setDiacriticizer(diacriticizer);

    LowercaseKnownFirstWordFilter filter = new LowercaseKnownFirstWordFilter(sessionId);

    String text = "Aujourd'hui il vient. Parce que...";
    Sentence sentence = new Sentence(text, sessionId);
    TokenSequence tokenSequence = new TokenSequence(sentence, sessionId);
    tokenSequence.addToken("".length(), "Aujourd'hui".length());
    tokenSequence.addToken("Aujourd'hui ".length(), "Aujourd'hui il".length());
    tokenSequence.addToken("Aujourd'hui il ".length(), "Aujourd'hui il vient".length());
    tokenSequence.addToken("Aujourd'hui il vient".length(), "Aujourd'hui il vient.".length());
    tokenSequence.addToken("Aujourd'hui il vient. ".length(), "Aujourd'hui il vient. Parce que".length());
    tokenSequence.addToken("Aujourd'hui il vient. Parce que".length(), "Aujourd'hui il vient. Parce que...".length());

    filter.apply(tokenSequence);
    System.out.println(tokenSequence);

    StringBuilder sb = new StringBuilder();
    for (Token token : tokenSequence) {
      sb.append(token.getText());
      sb.append('|');
    }

    assertEquals("aujourd'hui|il|vient|.|parce que|...|", sb.toString());
  }

  @Test
  public void testReplace3() throws Exception {
    Diacriticizer diacriticizer = mock(Diacriticizer.class);
 
    System.setProperty("config.file", "src/test/resources/test.conf");
    ConfigFactory.invalidateCaches();
    final Config config = ConfigFactory.load();

    final String sessionId = "test";
    TalismaneSession.get(sessionId).setDiacriticizer(diacriticizer);

    LowercaseKnownFirstWordFilter filter = new LowercaseKnownFirstWordFilter(sessionId);

    String text = "Georges est là.";
    Sentence sentence = new Sentence(text, sessionId);
    TokenSequence tokenSequence = new TokenSequence(sentence, sessionId);
    tokenSequence.addToken("".length(), "Georges".length());
    tokenSequence.addToken("Georges ".length(), "Georges est".length());
    tokenSequence.addToken("Georges est ".length(), "Georges est là".length());
    tokenSequence.addToken("Georges est là".length(), "Georges est là.".length());

    filter.apply(tokenSequence);
    System.out.println(tokenSequence);

    StringBuilder sb = new StringBuilder();
    for (Token token : tokenSequence) {
      sb.append(token.getText());
      sb.append('|');
    }

    assertEquals("Georges|est|là|.|", sb.toString());
  }

}
