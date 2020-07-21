package com.joliciel.talismane.posTagger.features;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import com.joliciel.talismane.TalismaneTest;
import org.junit.Test;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.lexicon.LexicalAttribute;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.machineLearning.features.StringLiteralFeature;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.posTagger.PosTaggerContext;
import com.joliciel.talismane.posTagger.PosTaggerContextImpl;
import com.joliciel.talismane.rawText.Sentence;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.utils.WeightedOutcome;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class LexicalAttributeFeatureTest extends TalismaneTest {

  @Test
  public void testCheckInternal() throws Exception {
    System.setProperty("config.file", "src/test/resources/testWithLex.conf");
    ConfigFactory.invalidateCaches();
    final Config config = ConfigFactory.load();

    final String sessionId = "test";

    Sentence sentence = new Sentence("une dame", sessionId);
    TokenSequence tokenSequence = new TokenSequence(sentence, sessionId);
    Token token = new Token("dame", tokenSequence, 1, "une ".length(), "une dame".length(), TalismaneSession.get(sessionId).getMergedLexicon(), sessionId);
    Decision decision = new Decision("NC", 1.0);
    final PosTaggedToken posTaggedToken = new PosTaggedToken(token, decision, sessionId);

    PosTaggedTokenAddressFunction<PosTaggerContext> addressFunction = new AbstractPosTaggedTokenAddressFunction() {
      @Override
      protected FeatureResult<PosTaggedTokenWrapper> checkInternal(PosTaggerContext context, RuntimeEnvironment env) {
        return this.generateResult(posTaggedToken);
      }
    };

    StringLiteralFeature<PosTaggedTokenWrapper> gender = new StringLiteralFeature<>(LexicalAttribute.Gender.name());

    LexicalAttributeFeature<PosTaggerContext> feature = new LexicalAttributeFeature<>(addressFunction, gender);

    PosTagSequence history = new PosTagSequence(tokenSequence);
    PosTaggerContext context = new PosTaggerContextImpl(token, history);
    RuntimeEnvironment env = new RuntimeEnvironment();

    FeatureResult<List<WeightedOutcome<String>>> featureResult = feature.checkInternal(context, env);
    List<WeightedOutcome<String>> outcomes = featureResult.getOutcome();
    System.out.println(outcomes);
    assertEquals("f", outcomes.get(0).getOutcome());
    assertEquals(1, outcomes.size());
  }

  @Test
  public void testCheckInternalMultipleEntries() throws Exception {
    System.setProperty("config.file", "src/test/resources/testWithLex.conf");
    ConfigFactory.invalidateCaches();
    final Config config = ConfigFactory.load();

    final String sessionId = "test";

    Sentence sentence = new Sentence("je demande", sessionId);
    TokenSequence tokenSequence = new TokenSequence(sentence, sessionId);
    Token token = new Token("demande", tokenSequence, 1, "je ".length(), "je demande".length(), TalismaneSession.get(sessionId).getMergedLexicon(), sessionId);
    Decision decision = new Decision("V", 1.0);
    final PosTaggedToken posTaggedToken = new PosTaggedToken(token, decision, sessionId);

    PosTaggedTokenAddressFunction<PosTaggerContext> addressFunction = new AbstractPosTaggedTokenAddressFunction() {
      @Override
      protected FeatureResult<PosTaggedTokenWrapper> checkInternal(PosTaggerContext context, RuntimeEnvironment env) {
        return this.generateResult(posTaggedToken);
      }
    };

    StringLiteralFeature<PosTaggedTokenWrapper> person = new StringLiteralFeature<>(LexicalAttribute.Person.name());

    LexicalAttributeFeature<PosTaggerContext> feature = new LexicalAttributeFeature<>(addressFunction, person);

    PosTagSequence history = new PosTagSequence(tokenSequence);
    PosTaggerContext context = new PosTaggerContextImpl(token, history);
    RuntimeEnvironment env = new RuntimeEnvironment();

    FeatureResult<List<WeightedOutcome<String>>> featureResult = feature.checkInternal(context, env);
    List<WeightedOutcome<String>> outcomes = featureResult.getOutcome();
    System.out.println(outcomes);
    for (WeightedOutcome<String> outcome : outcomes) {
      assertTrue("1".equals(outcome.getOutcome()) || "3".equals(outcome.getOutcome()));
    }
    assertEquals(2, outcomes.size());
  }

  @Test
  public void testCheckInternalMultipleAttributes() throws Exception {
    System.setProperty("config.file", "src/test/resources/testWithLex.conf");
    ConfigFactory.invalidateCaches();
    final Config config = ConfigFactory.load();

    final String sessionId = "test";

    Sentence sentence = new Sentence("blah", sessionId);
    TokenSequence tokenSequence = new TokenSequence(sentence, sessionId);
    Token token = new Token("blah", tokenSequence, 1, "".length(), "blah".length(), TalismaneSession.get(sessionId).getMergedLexicon(), sessionId);
    Decision decision = new Decision("V", 1.0);
    final PosTaggedToken posTaggedToken = new PosTaggedToken(token, decision, sessionId);

    PosTaggedTokenAddressFunction<PosTaggerContext> addressFunction = new AbstractPosTaggedTokenAddressFunction() {
      @Override
      protected FeatureResult<PosTaggedTokenWrapper> checkInternal(PosTaggerContext context, RuntimeEnvironment env) {
        return this.generateResult(posTaggedToken);
      }
    };

    StringLiteralFeature<PosTaggedTokenWrapper> person = new StringLiteralFeature<>(LexicalAttribute.Person.name());
    StringLiteralFeature<PosTaggedTokenWrapper> number = new StringLiteralFeature<>(LexicalAttribute.Number.name());

    LexicalAttributeFeature<PosTaggerContext> feature = new LexicalAttributeFeature<>(addressFunction, person, number);

    PosTagSequence history = new PosTagSequence(tokenSequence);
    PosTaggerContext context = new PosTaggerContextImpl(token, history);
    RuntimeEnvironment env = new RuntimeEnvironment();

    FeatureResult<List<WeightedOutcome<String>>> featureResult = feature.checkInternal(context, env);
    List<WeightedOutcome<String>> outcomes = featureResult.getOutcome();
    System.out.println(outcomes);
    for (WeightedOutcome<String> outcome : outcomes) {
      assertTrue("3|p".equals(outcome.getOutcome()) || "1|s".equals(outcome.getOutcome()) || "3|s".equals(outcome.getOutcome()));
    }
    assertEquals(3, outcomes.size());
  }
}
