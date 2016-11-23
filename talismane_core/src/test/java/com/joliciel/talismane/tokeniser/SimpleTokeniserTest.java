package com.joliciel.talismane.tokeniser;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.joliciel.talismane.Annotation;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.filters.Sentence;
import com.joliciel.talismane.tokeniser.filters.TokenPlaceholder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class SimpleTokeniserTest {

	@Test
	public void testTokenise() throws Exception {
		System.setProperty("config.file", "src/test/resources/test.conf");
		ConfigFactory.invalidateCaches();
		final Config config = ConfigFactory.load();

		final TalismaneSession session = new TalismaneSession(config, "");
		final Sentence sentence = new Sentence("Click http://www.blah-di-blah.com now", session);
		List<Annotation<TokenPlaceholder>> annotations = new ArrayList<>();
		Annotation<TokenPlaceholder> annotation = new Annotation<TokenPlaceholder>("Click ".length(), "Click http://www.blah-di-blah.com".length(),
				new TokenPlaceholder("URL", ""));
		annotations.add(annotation);
		sentence.addAnnotations(annotations);

		SimpleTokeniser simpleTokeniser = new SimpleTokeniser(session);
		TokenSequence tokenSequence = simpleTokeniser.tokeniseSentence(sentence);
		System.out.println(tokenSequence.toString());

		assertEquals(3, tokenSequence.size());

		assertEquals("Click", tokenSequence.get(0).getText());
		assertEquals("URL", tokenSequence.get(1).getText());
		assertEquals("now", tokenSequence.get(2).getText());
	}

}
