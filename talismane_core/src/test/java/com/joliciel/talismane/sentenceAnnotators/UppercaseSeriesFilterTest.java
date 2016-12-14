package com.joliciel.talismane.sentenceAnnotators;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.lexicon.Diacriticizer;
import com.joliciel.talismane.sentenceAnnotators.UppercaseSeriesFilter;
import com.joliciel.talismane.tokeniser.Tokeniser;
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

		UppercaseSeriesFilter filter = new UppercaseSeriesFilter();
		filter.setTalismaneSession(session);

		String text = "Je VEUX SAVOIR la VERITE, je VEUX SAVOIR LA VERITE sur L'AMERIQUE!";
		List<String> tokens = Tokeniser.bruteForceTokenise(text, session);

		filter.replace(tokens);
		System.out.println(tokens);

		StringBuilder sb = new StringBuilder();
		for (String token : tokens)
			sb.append(token);

		assertEquals("Je veux savoir la VERITE, je veux savoir La Verite sur l'Amérique!", sb.toString());
	}

}
