package com.joliciel.talismane.tokeniser;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.joliciel.talismane.Annotation;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.rawText.Sentence;
import com.joliciel.talismane.sentenceAnnotators.TokenPlaceholder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class SimpleTokeniserTest {

	@Test
	public void testTokenise() throws Exception {
		System.setProperty("config.file", "src/test/resources/test.conf");
		ConfigFactory.invalidateCaches();
		final Config config = ConfigFactory.load();

		final TalismaneSession session = new TalismaneSession(config, "");

		String[] labels = new String[0];

		final Sentence sentence = new Sentence("Click http://www.blah-di-blah.com now", session);
		List<Annotation<TokenPlaceholder>> annotations = new ArrayList<>();
		Annotation<TokenPlaceholder> annotation = new Annotation<TokenPlaceholder>("Click ".length(), "Click http://www.blah-di-blah.com".length(),
				new TokenPlaceholder("URL", ""), labels);
		annotations.add(annotation);
		sentence.addAnnotations(annotations);

		SimpleTokeniser simpleTokeniser = new SimpleTokeniser(session);
		TokenSequence tokenSequence = simpleTokeniser.tokeniseSentence(sentence);
		System.out.println(tokenSequence.toString());

		assertEquals(3, tokenSequence.size());

		assertEquals("Click", tokenSequence.get(0).getAnalyisText());
		assertEquals("URL", tokenSequence.get(1).getAnalyisText());
		assertEquals("now", tokenSequence.get(2).getAnalyisText());

		List<Annotation<TokenBoundary>> tokenBoundaries = sentence.getAnnotations(TokenBoundary.class);
		assertEquals(3, tokenBoundaries.size());

		assertEquals("".length(), tokenBoundaries.get(0).getStart());
		assertEquals("Click".length(), tokenBoundaries.get(0).getEnd());
		assertEquals("Click", tokenBoundaries.get(0).getData().getAnalysisText());
		assertEquals("Click ".length(), tokenBoundaries.get(1).getStart());
		assertEquals("URL", tokenBoundaries.get(1).getData().getAnalysisText());
		assertEquals("Click http://www.blah-di-blah.com".length(), tokenBoundaries.get(1).getEnd());
		assertEquals("Click http://www.blah-di-blah.com ".length(), tokenBoundaries.get(2).getStart());
		assertEquals("Click http://www.blah-di-blah.com now".length(), tokenBoundaries.get(2).getEnd());
		assertEquals("now", tokenBoundaries.get(2).getData().getAnalysisText());

	}

}
