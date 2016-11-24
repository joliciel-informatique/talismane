package com.joliciel.talismane.tokeniser;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.filters.Sentence;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class PretokenisedSequenceTest {

	@Test
	public void testAddTokenString() throws Exception {
		System.setProperty("config.file", "src/test/resources/test.conf");
		ConfigFactory.invalidateCaches();
		final Config config = ConfigFactory.load();

		final TalismaneSession session = new TalismaneSession(config, "");
		final Sentence sentence = new Sentence("« Il est là.  »", session);

		PretokenisedSequence sequence = new PretokenisedSequence(sentence, session);
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
