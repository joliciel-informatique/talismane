package com.joliciel.talismane.posTagger.features;

import static org.junit.Assert.assertEquals;

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
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class CombinedLexicalAttributesTest extends TalismaneTest {

  @Test
  public void testCheckInternalMultipleEntries() throws Exception {
    System.setProperty("config.file", "src/test/resources/testWithLex.conf");
    ConfigFactory.invalidateCaches();
    final Config config = ConfigFactory.load();

    final String sessionId = "test";

    Sentence sentence = new Sentence("je demande", sessionId);
    TokenSequence tokenSequence = new TokenSequence(sentence, sessionId);
    Token token = new Token("demande", tokenSequence, 1, "je ".length(), "je demande".length(), sessionId);
    Decision decision = new Decision("V", 1.0);
    final PosTaggedToken posTaggedToken = new PosTaggedToken(token, decision, sessionId);

    PosTaggedTokenAddressFunction<PosTaggerContext> addressFunction = new AbstractPosTaggedTokenAddressFunction() {
      @Override
      protected FeatureResult<PosTaggedTokenWrapper> checkInternal(PosTaggerContext context, RuntimeEnvironment env) {
        return this.generateResult(posTaggedToken);
      }
    };

    StringLiteralFeature<PosTaggedTokenWrapper> person = new StringLiteralFeature<>(LexicalAttribute.Person.name());

    CombinedLexicalAttributesFeature<PosTaggerContext> feature = new CombinedLexicalAttributesFeature<>(addressFunction, person);

    PosTagSequence history = new PosTagSequence(tokenSequence);
    PosTaggerContext context = new PosTaggerContextImpl(token, history);
    RuntimeEnvironment env = new RuntimeEnvironment();

    FeatureResult<String> featureResult = feature.checkInternal(context, env);
    String outcome = featureResult.getOutcome();
    System.out.println(outcome);
    assertEquals("1;3", outcome);
  }

  @Test
  public void testCheckInternalMultipleAttributes() throws Exception {
    System.setProperty("config.file", "src/test/resources/testWithLex.conf");
    ConfigFactory.invalidateCaches();
    final Config config = ConfigFactory.load();

    final String sessionId = "test";

    Sentence sentence = new Sentence("blah", sessionId);
    TokenSequence tokenSequence = new TokenSequence(sentence, sessionId);
    Token token = new Token("blah", tokenSequence, 1, "".length(), "blah".length(), sessionId);
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

    CombinedLexicalAttributesFeature<PosTaggerContext> feature = new CombinedLexicalAttributesFeature<>(addressFunction, person, number);

    PosTagSequence history = new PosTagSequence(tokenSequence);
    PosTaggerContext context = new PosTaggerContextImpl(token, history);
    RuntimeEnvironment env = new RuntimeEnvironment();

    FeatureResult<String> featureResult = feature.checkInternal(context, env);
    String outcome = featureResult.getOutcome();
    System.out.println(outcome);
    assertEquals("1;3|p;s", outcome);
  }
}
