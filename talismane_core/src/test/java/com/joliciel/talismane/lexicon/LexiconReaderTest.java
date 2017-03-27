package com.joliciel.talismane.lexicon;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class LexiconReaderTest {

  @Test
  public void testReadLexicons() throws Exception {
    System.setProperty("config.file", "src/test/resources/test.conf");
    ConfigFactory.invalidateCaches();
    final Config config = ConfigFactory.load();

    final TalismaneSession session = new TalismaneSession(config, "");
    File lexiconProperties = new File("src/test/resources/lexicons/lexicons_fr.txt");
    LexiconReader lexiconReader = new LexiconReader(session);

    List<PosTaggerLexicon> lexicons = lexiconReader.readLexicons(lexiconProperties);
    assertEquals(3, lexicons.size());

    LexiconChain lexicon = new LexiconChain(lexicons);
    List<LexicalEntry> entries = lexicon.getEntries("dame");
    for (LexicalEntry entry : entries) {
      System.out.println(entry);
    }
    assertEquals(9, entries.size());

    PosTagSet posTagSet = session.getPosTagSet();

    entries = lexicon.findLexicalEntries("dame", posTagSet.getPosTag("NC"));
    for (LexicalEntry entry : entries) {
      System.out.println(entry);
    }
    assertEquals(2, entries.size());

    Set<PosTag> posTags = lexicon.findPossiblePosTags("dame");
    System.out.println(posTags);

    assertEquals(4, posTags.size());

  }

}
