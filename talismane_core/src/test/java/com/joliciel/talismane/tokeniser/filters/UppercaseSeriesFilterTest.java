package com.joliciel.talismane.tokeniser.filters;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.lexicon.Diacriticizer;
import com.joliciel.talismane.rawText.Sentence;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import mockit.NonStrict;
import mockit.NonStrictExpectations;

public class UppercaseSeriesFilterTest {

  @Test
  public void testReplace(@NonStrict final Diacriticizer diacriticizer) throws Exception {
    new NonStrictExpectations() {
      {
        diacriticizer.diacriticize("VEUX");
        returns(new HashSet<>(Arrays.asList("veux")));
        diacriticizer.diacriticize("SAVOIR");
        returns(new HashSet<>(Arrays.asList("savoir")));
        diacriticizer.diacriticize("L'");
        returns(new HashSet<>(Arrays.asList("l'")));
        diacriticizer.diacriticize("AMERIQUE");
        returns(new HashSet<>(Arrays.asList("Amérique")));
      }
    };

    System.setProperty("config.file", "src/test/resources/test.conf");
    ConfigFactory.invalidateCaches();
    final Config config = ConfigFactory.load();

    final TalismaneSession session = new TalismaneSession(config, "");
    session.setDiacriticizer(diacriticizer);

    UppercaseSeriesFilter filter = new UppercaseSeriesFilter(session);

    String text = "Je VEUX SAVOIR la VERITE, je VEUX SAVOIR LA VERITE sur L'AMERIQUE!";
    Sentence sentence = new Sentence(text, session);
    TokenSequence tokenSequence = new TokenSequence(sentence, session);
    tokenSequence.addToken("".length(), "Je".length());
    tokenSequence.addToken("Je ".length(), "Je VEUX".length());
    tokenSequence.addToken("Je VEUX ".length(), "Je VEUX SAVOIR".length());
    tokenSequence.addToken("Je VEUX SAVOIR ".length(), "Je VEUX SAVOIR la".length());
    tokenSequence.addToken("Je VEUX SAVOIR la ".length(), "Je VEUX SAVOIR la VERITE".length());
    tokenSequence.addToken("Je VEUX SAVOIR la VERITE".length(), "Je VEUX SAVOIR la VERITE,".length());
    tokenSequence.addToken("Je VEUX SAVOIR la VERITE, ".length(), "Je VEUX SAVOIR la VERITE, je".length());
    tokenSequence.addToken("Je VEUX SAVOIR la VERITE, je ".length(), "Je VEUX SAVOIR la VERITE, je VEUX".length());
    tokenSequence.addToken("Je VEUX SAVOIR la VERITE, je VEUX ".length(), "Je VEUX SAVOIR la VERITE, je VEUX SAVOIR".length());
    tokenSequence.addToken("Je VEUX SAVOIR la VERITE, je VEUX SAVOIR ".length(), "Je VEUX SAVOIR la VERITE, je VEUX SAVOIR LA".length());
    tokenSequence.addToken("Je VEUX SAVOIR la VERITE, je VEUX SAVOIR LA ".length(), "Je VEUX SAVOIR la VERITE, je VEUX SAVOIR LA VERITE".length());
    tokenSequence.addToken("Je VEUX SAVOIR la VERITE, je VEUX SAVOIR LA VERITE ".length(), "Je VEUX SAVOIR la VERITE, je VEUX SAVOIR LA VERITE sur".length());
    tokenSequence.addToken("Je VEUX SAVOIR la VERITE, je VEUX SAVOIR LA VERITE sur ".length(),
        "Je VEUX SAVOIR la VERITE, je VEUX SAVOIR LA VERITE sur L'".length());
    tokenSequence.addToken("Je VEUX SAVOIR la VERITE, je VEUX SAVOIR LA VERITE sur L'".length(),
        "Je VEUX SAVOIR la VERITE, je VEUX SAVOIR LA VERITE sur L'AMERIQUE".length());
    tokenSequence.addToken("Je VEUX SAVOIR la VERITE, je VEUX SAVOIR LA VERITE sur L'AMERIQUE".length(),
        "Je VEUX SAVOIR la VERITE, je VEUX SAVOIR LA VERITE sur L'AMERIQUE!".length());

    filter.apply(tokenSequence);
    System.out.println(tokenSequence);

    StringBuilder sb = new StringBuilder();
    for (Token token : tokenSequence) {
      sb.append(token.getText());
      sb.append('|');
    }

    assertEquals("Je|veux|savoir|la|VERITE|,|je|veux|savoir|La|Verite|sur|l'|Amérique|!|", sb.toString());
  }

}
