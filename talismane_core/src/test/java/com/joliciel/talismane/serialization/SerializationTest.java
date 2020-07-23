package com.joliciel.talismane.serialization;

import com.joliciel.talismane.TalismaneTest;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.parser.*;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.rawText.Sentence;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.Assert.assertEquals;

public class SerializationTest extends TalismaneTest {
  private static final Logger LOG = LoggerFactory.getLogger(SerializationTest.class);
  
  @Test
  public void testSerialize() throws Exception {
    System.setProperty("config.file", "src/test/resources/test.conf");
    ConfigFactory.invalidateCaches();

    String sessionId = "test";
    Sentence sentence = new Sentence("Il aime les pommes", sessionId);
    TokenSequence tokenSequence = new TokenSequence(sentence, sessionId);
    tokenSequence.addToken("".length(), "Il".length());
    tokenSequence.addToken("Il ".length(), "Il aime".length());
    tokenSequence.addToken("Il aime ".length(), "Il aime les".length());
    tokenSequence.addToken("Il aime les ".length(), "Il aime les pommes".length());
    PosTagSequence posTagSequence = new PosTagSequence(tokenSequence);
    posTagSequence.addPosTaggedToken(new PosTaggedToken(posTagSequence.getNextToken(), new Decision("CLS", 0.90), sessionId));
    posTagSequence.addPosTaggedToken(new PosTaggedToken(posTagSequence.getNextToken(), new Decision("V", 0.70), sessionId));
    posTagSequence.addPosTaggedToken(new PosTaggedToken(posTagSequence.getNextToken(), new Decision("DET", 0.60), sessionId));
    posTagSequence.addPosTaggedToken(new PosTaggedToken(posTagSequence.getNextToken(), new Decision("NC", 0.80), sessionId));

    posTagSequence.prependRoot();
    
    ParseConfiguration configuration = new ParseConfiguration(posTagSequence);
    LOG.debug(configuration.toString());
    new ShiftTransition().apply(configuration); // ROOT ... il
    LOG.debug("Shift -> " + configuration.toString());
    new LeftArcEagerTransition("suj").apply(configuration); // ROOT il <- aime
    LOG.debug("Left -> " + configuration.toString());
    new RightArcEagerTransition("root").apply(configuration); // ROOT -> aime
    LOG.debug("Right -> " + configuration.toString());
    new ShiftTransition().apply(configuration); // ROOT aime ... les
    LOG.debug("Shift -> " + configuration.toString());
    new LeftArcEagerTransition("det").apply(configuration); // ROOT aime les <- pommes
    LOG.debug("Left -> " + configuration.toString());
    new RightArcEagerTransition("obj").apply(configuration); // ROOT aime -> pommes
    LOG.debug("Right -> " + configuration.toString());
    
    ParseTree parseTree = new ParseTree(configuration, true);
    LOG.debug(parseTree.toString());

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(bos);
    oos.writeObject(sentence);
    oos.writeObject(tokenSequence);
    oos.writeObject(posTagSequence);
    oos.writeObject(configuration);
    oos.writeObject(parseTree);
    
    byte[] bytes = bos.toByteArray();

    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
    Sentence sentence2 = (Sentence) ois.readObject();
    TokenSequence tokenSequence2 = (TokenSequence) ois.readObject();
    PosTagSequence posTagSequence2 = (PosTagSequence) ois.readObject();
    ParseConfiguration configuration2 = (ParseConfiguration) ois.readObject(); 
    ParseTree parseTree2 = (ParseTree) ois.readObject();

    assertEquals(sentence, sentence2);
    assertEquals(tokenSequence, tokenSequence2);
    assertEquals(posTagSequence, posTagSequence2);
    assertEquals(configuration, configuration2);
    assertEquals(parseTree, parseTree2);
  }
}
