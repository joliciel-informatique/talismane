package com.joliciel.talismane.tokeniser.filters;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.lexicon.Diacriticizer;
import com.joliciel.talismane.tokeniser.Tokeniser;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import mockit.NonStrict;
import mockit.NonStrictExpectations;

public class LowercaseKnownFirstWordFilterTest {

	@Test
	public void testReplace(@NonStrict final Diacriticizer diacriticizer) throws Exception {
		new NonStrictExpectations() {
			{
				diacriticizer.diacriticize("J'");
				returns(new HashSet<>(Arrays.asList("j'")));
				diacriticizer.diacriticize("Il");
				returns(new HashSet<>(Arrays.asList("il")));
			}
		};

		System.setProperty("config.file", "src/test/resources/test.conf");
		ConfigFactory.invalidateCaches();
		final Config config = ConfigFactory.load();

		final TalismaneSession session = new TalismaneSession(config, "");
		session.setDiacriticizer(diacriticizer);

		LowercaseKnownFirstWordFilter filter = new LowercaseKnownFirstWordFilter();
		filter.setTalismaneSession(session);

		String text = "J'avais oublié : Il est Malade.";
		List<String> tokens = Tokeniser.bruteForceTokenise(text, session);

		filter.replace(tokens);
		System.out.println(tokens);

		StringBuilder sb = new StringBuilder();
		for (String token : tokens)
			sb.append(token);

		assertEquals("j'avais oublié : il est Malade.", sb.toString());
	}

	@Test
	public void testReplaceLongWord(@NonStrict final Diacriticizer diacriticizer) throws Exception {
		new NonStrictExpectations() {
			{
				diacriticizer.diacriticize("Aujourd'hui");
				returns(new HashSet<>(Arrays.asList("aujourd'hui")));
				diacriticizer.diacriticize("Parce que");
				returns(new HashSet<>(Arrays.asList("parce que")));
			}
		};

		System.setProperty("config.file", "src/test/resources/test.conf");
		ConfigFactory.invalidateCaches();
		final Config config = ConfigFactory.load();

		final TalismaneSession session = new TalismaneSession(config, "");
		session.setDiacriticizer(diacriticizer);

		LowercaseKnownFirstWordFilter filter = new LowercaseKnownFirstWordFilter();
		filter.setTalismaneSession(session);

		String text = "Aujourd'hui il vient. Parce que...";
		List<String> tokens = Tokeniser.bruteForceTokenise(text, session);

		filter.replace(tokens);
		System.out.println(tokens);

		StringBuilder sb = new StringBuilder();
		for (String token : tokens)
			sb.append(token);

		assertEquals("aujourd'hui il vient. parce que...", sb.toString());
	}

	@Test
	public void testReplace3(@NonStrict final Diacriticizer diacriticizer) throws Exception {
		new NonStrictExpectations() {
			{
			}
		};

		System.setProperty("config.file", "src/test/resources/test.conf");
		ConfigFactory.invalidateCaches();
		final Config config = ConfigFactory.load();

		final TalismaneSession session = new TalismaneSession(config, "");
		session.setDiacriticizer(diacriticizer);

		LowercaseKnownFirstWordFilter filter = new LowercaseKnownFirstWordFilter();
		filter.setTalismaneSession(session);

		String text = "Georges est là.";
		List<String> tokens = Tokeniser.bruteForceTokenise(text, session);

		filter.replace(tokens);
		System.out.println(tokens);

		StringBuilder sb = new StringBuilder();
		for (String token : tokens)
			sb.append(token);

		assertEquals("Georges est là.", sb.toString());
	}

}
