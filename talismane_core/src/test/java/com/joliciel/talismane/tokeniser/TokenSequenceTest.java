///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2014 Joliciel Informatique
//
//This file is part of Talismane.
//
//Talismane is free software: you can redistribute it and/or modify
//it under the terms of the GNU Affero General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//Talismane is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Affero General Public License for more details.
//
//You should have received a copy of the GNU Affero General Public License
//along with Talismane.  If not, see <http://www.gnu.org/licenses/>.
//////////////////////////////////////////////////////////////////////////////
package com.joliciel.talismane.tokeniser;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.Annotation;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.filters.Sentence;
import com.joliciel.talismane.tokeniser.filters.TokenPlaceholder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class TokenSequenceTest {
	private static final Logger LOG = LoggerFactory.getLogger(TokenSequenceTest.class);

	@Test
	public void testTokeniseSentence() throws Exception {
		System.setProperty("config.file", "src/test/resources/test.conf");
		ConfigFactory.invalidateCaches();
		final Config config = ConfigFactory.load();

		final TalismaneSession talismaneSession = new TalismaneSession(config, "");
		final Sentence sentence = new Sentence("Je n'ai pas l'ourang-outan.", talismaneSession);

		TokenSequence tokenSequence = new TokenSequence(sentence, talismaneSession);
		tokenSequence.findDefaultTokens();

		assertEquals(14, tokenSequence.listWithWhiteSpace().size());
		assertEquals(11, tokenSequence.size());

		int i = 0;
		for (Token token : tokenSequence.listWithWhiteSpace()) {
			if (i == 0) {
				assertEquals("Je", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i == 1) {
				assertEquals(" ", token.getText());
				assertEquals(true, token.isSeparator());
			} else if (i == 2) {
				assertEquals("n", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i == 3) {
				assertEquals("'", token.getText());
				assertEquals(true, token.isSeparator());
			} else if (i == 4) {
				assertEquals("ai", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i == 5) {
				assertEquals(" ", token.getText());
				assertEquals(true, token.isSeparator());
			} else if (i == 6) {
				assertEquals("pas", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i == 7) {
				assertEquals(" ", token.getText());
				assertEquals(true, token.isSeparator());
			} else if (i == 8) {
				assertEquals("l", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i == 9) {
				assertEquals("'", token.getText());
				assertEquals(true, token.isSeparator());
			} else if (i == 10) {
				assertEquals("ourang", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i == 11) {
				assertEquals("-", token.getText());
				assertEquals(true, token.isSeparator());
			} else if (i == 12) {
				assertEquals("outan", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i == 13) {
				assertEquals(".", token.getText());
				assertEquals(true, token.isSeparator());
			}
			i++;
		}

		i = 0;
		for (Token token : tokenSequence) {
			if (i == 0) {
				assertEquals("Je", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i == 1) {
				assertEquals("n", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i == 2) {
				assertEquals("'", token.getText());
				assertEquals(true, token.isSeparator());
			} else if (i == 3) {
				assertEquals("ai", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i == 4) {
				assertEquals("pas", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i == 5) {
				assertEquals("l", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i == 6) {
				assertEquals("'", token.getText());
				assertEquals(true, token.isSeparator());
			} else if (i == 7) {
				assertEquals("ourang", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i == 8) {
				assertEquals("-", token.getText());
				assertEquals(true, token.isSeparator());
			} else if (i == 9) {
				assertEquals("outan", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i == 10) {
				assertEquals(".", token.getText());
				assertEquals(true, token.isSeparator());
			}
			i++;
		}
	}

	@Test
	public void testSimpleAddByIndex() throws Exception {
		System.setProperty("config.file", "src/test/resources/test.conf");
		ConfigFactory.invalidateCaches();
		final Config config = ConfigFactory.load();

		final TalismaneSession talismaneSession = new TalismaneSession(config, "");
		final Sentence sentence = new Sentence("The quick brown fox.", talismaneSession);

		TokenSequence tokenSequence = new TokenSequence(sentence, talismaneSession);
		tokenSequence.addToken(16, 19); // fox
		tokenSequence.addToken(4, 9); // quick
		tokenSequence.addToken(4, 9); // quick - should be ignored
		tokenSequence.addToken(0, 3); // The
		tokenSequence.addToken(19, 20); // .
		tokenSequence.addToken(10, 12); // br - should be removed by brown
		tokenSequence.addToken(12, 15); // own - should be removed by brown
		tokenSequence.addToken(10, 15); // brown
		tokenSequence.finalise();

		assertEquals(5, tokenSequence.size());
		int i = 0;
		for (Token token : tokenSequence) {
			LOG.debug(token.getText());
			if (i == 0) {
				assertEquals("The", token.getText());
			}
			assertEquals(i, token.getIndex());
			i++;
		}

		LOG.debug("Token splits:");
		for (int tokenSplit : tokenSequence.getTokenSplits()) {
			LOG.debug("" + tokenSplit);
		}

		assertEquals(9, tokenSequence.getTokenSplits().size());
	}

	@Test
	public void testTokeniseSentenceWithPlaceholders() throws Exception {
		System.setProperty("config.file", "src/test/resources/test.conf");
		ConfigFactory.invalidateCaches();
		final Config config = ConfigFactory.load();

		final TalismaneSession talismaneSession = new TalismaneSession(config, "");
		final Sentence sentence = new Sentence("Write to me at joe.schome@test.com, otherwise go to http://test.com.", talismaneSession);

		final List<Annotation<TokenPlaceholder>> placeholders = new ArrayList<>();
		Annotation<TokenPlaceholder> placeholder0 = new Annotation<>("Write to me at ".length(), "Write to me at joe.schome@test.com".length(),
				new TokenPlaceholder("Email", "blah"));
		placeholders.add(placeholder0);
		Annotation<TokenPlaceholder> placeholder1 = new Annotation<>("Write to me at joe.schome@test.com, otherwise go to ".length(),
				"Write to me at joe.schome@test.com, otherwise go to http://test.com".length(), new TokenPlaceholder("URL", "blah"));
		placeholders.add(placeholder1);

		sentence.addAnnotations(placeholders);

		TokenSequence tokenSequence = new TokenSequence(sentence, talismaneSession);
		tokenSequence.findDefaultTokens();

		assertEquals(19, tokenSequence.listWithWhiteSpace().size());
		assertEquals(11, tokenSequence.size());

		int i = 0;
		for (Token token : tokenSequence.listWithWhiteSpace()) {
			if (i == 0) {
				assertEquals("Write", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i == 1) {
				assertEquals(" ", token.getText());
				assertEquals(true, token.isSeparator());
			} else if (i == 2) {
				assertEquals("to", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i == 3) {
				assertEquals(" ", token.getText());
				assertEquals(true, token.isSeparator());
			} else if (i == 4) {
				assertEquals("me", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i == 5) {
				assertEquals(" ", token.getText());
				assertEquals(true, token.isSeparator());
			} else if (i == 6) {
				assertEquals("at", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i == 7) {
				assertEquals(" ", token.getText());
				assertEquals(true, token.isSeparator());
			} else if (i == 8) {
				assertEquals("Email", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i == 9) {
				assertEquals(",", token.getText());
				assertEquals(true, token.isSeparator());
			} else if (i == 10) {
				assertEquals(" ", token.getText());
				assertEquals(true, token.isSeparator());
			} else if (i == 11) {
				assertEquals("otherwise", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i == 12) {
				assertEquals(" ", token.getText());
				assertEquals(true, token.isSeparator());
			} else if (i == 13) {
				assertEquals("go", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i == 14) {
				assertEquals(" ", token.getText());
				assertEquals(true, token.isSeparator());
			} else if (i == 15) {
				assertEquals("to", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i == 16) {
				assertEquals(" ", token.getText());
				assertEquals(true, token.isSeparator());
			} else if (i == 17) {
				assertEquals("URL", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i == 18) {
				assertEquals(".", token.getText());
				assertEquals(true, token.isSeparator());
			}
			i++;
		}

		i = 0;
		for (Token token : tokenSequence) {
			if (i == 0) {
				assertEquals("Write", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i == 1) {
				assertEquals("to", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i == 2) {
				assertEquals("me", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i == 3) {
				assertEquals("at", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i == 4) {
				assertEquals("Email", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i == 5) {
				assertEquals(",", token.getText());
				assertEquals(true, token.isSeparator());
			} else if (i == 6) {
				assertEquals("otherwise", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i == 7) {
				assertEquals("go", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i == 8) {
				assertEquals("to", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i == 9) {
				assertEquals("URL", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i == 10) {
				assertEquals(".", token.getText());
				assertEquals(true, token.isSeparator());
			}
			i++;
		}
	}

	@Test
	public void testTokeniseSentenceWithPlaceholdersNoSeparators() throws Exception {
		System.setProperty("config.file", "src/test/resources/test.conf");
		ConfigFactory.invalidateCaches();
		final Config config = ConfigFactory.load();

		final TalismaneSession talismaneSession = new TalismaneSession(config, "");
		final Sentence sentence = new Sentence("Il t’aime.", talismaneSession);

		final List<Annotation<StringAttribute>> annotations = new ArrayList<>();
		Annotation<StringAttribute> annotation1 = new Annotation<>("Il ".length(), "Il t’aime".length(), new StringAttribute("phrase", "verbal"));
		annotations.add(annotation1);
		Annotation<StringAttribute> annotation2 = new Annotation<>("Il ".length(), "Il t’aime".length(), new StringAttribute("person", "3rd"));
		annotations.add(annotation2);

		Annotation<StringAttribute> annotation3 = new Annotation<>("Il ".length(), "Il t’".length(), new StringAttribute("type", "object"));
		annotations.add(annotation3);

		final List<Annotation<TokenPlaceholder>> placeholders = new ArrayList<>();

		Annotation<TokenPlaceholder> placeholder0 = new Annotation<>("Il t".length(), "Il t’".length(), new TokenPlaceholder("'", "blah"));

		placeholders.add(placeholder0);

		sentence.addAnnotations(annotations);
		sentence.addAnnotations(placeholders);

		TokenSequence tokenSequence = new TokenSequence(sentence, talismaneSession);
		tokenSequence.findDefaultTokens();

		LOG.debug(tokenSequence.listWithWhiteSpace().toString());
		LOG.debug(tokenSequence.toString());
		assertEquals(6, tokenSequence.listWithWhiteSpace().size());
		assertEquals(5, tokenSequence.size());

		int i = 0;
		for (Token token : tokenSequence.listWithWhiteSpace()) {
			if (i == 0) {
				assertEquals("Il", token.getText());
				assertEquals(false, token.isSeparator());
				assertEquals(0, token.getAttributes().size());
			} else if (i == 1) {
				assertEquals(" ", token.getText());
				assertEquals(true, token.isSeparator());
				assertEquals(0, token.getAttributes().size());
			} else if (i == 2) {
				assertEquals("t", token.getText());
				assertEquals(false, token.isSeparator());
				assertEquals(3, token.getAttributes().size());
				assertEquals("verbal", token.getAttributes().get("phrase").getValue());
				assertEquals("3rd", token.getAttributes().get("person").getValue());
				assertEquals("object", token.getAttributes().get("type").getValue());
			} else if (i == 3) {
				assertEquals("'", token.getText());
				assertEquals(true, token.isSeparator());
				assertEquals(3, token.getAttributes().size());
				assertEquals("verbal", token.getAttributes().get("phrase").getValue());
				assertEquals("3rd", token.getAttributes().get("person").getValue());
				assertEquals("object", token.getAttributes().get("type").getValue());
			} else if (i == 4) {
				assertEquals("aime", token.getText());
				assertEquals(false, token.isSeparator());
				assertEquals(2, token.getAttributes().size());
				assertEquals("verbal", token.getAttributes().get("phrase").getValue());
				assertEquals("3rd", token.getAttributes().get("person").getValue());
			} else if (i == 5) {
				assertEquals(".", token.getText());
				assertEquals(true, token.isSeparator());
				assertEquals(0, token.getAttributes().size());
			}
			i++;
		}

		i = 0;
		for (Token token : tokenSequence) {
			if (i == 0) {
				assertEquals("Il", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i == 1) {
				assertEquals("t", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i == 2) {
				assertEquals("'", token.getText());
				assertEquals(true, token.isSeparator());
			} else if (i == 3) {
				assertEquals("aime", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i == 4) {
				assertEquals(".", token.getText());
				assertEquals(true, token.isSeparator());
			}
			i++;
		}
	}

	@Test
	public void testOverlappingPlaceholders() throws Exception {
		System.setProperty("config.file", "src/test/resources/test.conf");
		ConfigFactory.invalidateCaches();
		final Config config = ConfigFactory.load();

		final TalismaneSession talismaneSession = new TalismaneSession(config, "");
		final Sentence sentence = new Sentence("Pakistan International Airlines Company", talismaneSession);

		final List<Annotation<StringAttribute>> annotations = new ArrayList<>();
		Annotation<StringAttribute> annotation1 = new Annotation<>("".length(), "Pakistan".length(), new StringAttribute("namedEntity", "place"));
		Annotation<StringAttribute> annotation1b = new Annotation<>("".length(), "Pakistan".length(), new StringAttribute("startsWithP", "true"));

		annotations.add(annotation1);
		annotations.add(annotation1b);

		Annotation<StringAttribute> annotation2 = new Annotation<>("".length(), "Pakistan International Airlines".length(),
				new StringAttribute("namedEntity", "company"));
		Annotation<StringAttribute> annotation2b = new Annotation<>("".length(), "Pakistan International Airlines".length(),
				new StringAttribute("asianCompany", "true"));

		annotations.add(annotation2);
		annotations.add(annotation2b);

		Annotation<StringAttribute> annotation3 = new Annotation<>("Pakistan ".length(), "Pakistan International Airlines Company".length(),
				new StringAttribute("namedEntity", "company"));
		Annotation<StringAttribute> annotation3b = new Annotation<>("Pakistan ".length(), "Pakistan International Airlines Company".length(),
				new StringAttribute("asianCompany", "false"));

		annotations.add(annotation3);
		annotations.add(annotation3b);

		Annotation<StringAttribute> annotation4 = new Annotation<>("Pakistan International Airlines ".length(),
				"Pakistan International Airlines Company".length(), new StringAttribute("startsWithC", "true"));

		annotations.add(annotation4);
		sentence.addAnnotations(annotations);

		TokenSequence tokenSequence = new TokenSequence(sentence, talismaneSession);
		tokenSequence.findDefaultTokens();

		LOG.debug(tokenSequence.listWithWhiteSpace().toString());
		LOG.debug(tokenSequence.toString());
		assertEquals(4, tokenSequence.size());

		int i = 0;
		for (Token token : tokenSequence) {
			LOG.debug(token.getAttributes().toString());
			if (i == 0) {
				assertEquals("Pakistan", token.getText());
				assertEquals(3, token.getAttributes().size());
				assertEquals("company", token.getAttributes().get("namedEntity").getValue());
				assertEquals("true", token.getAttributes().get("startsWithP").getValue());
				assertEquals("true", token.getAttributes().get("asianCompany").getValue());
			} else if (i == 1) {
				assertEquals("International", token.getText());
				assertEquals(2, token.getAttributes().size());
				assertEquals("company", token.getAttributes().get("namedEntity").getValue());
				assertEquals("true", token.getAttributes().get("asianCompany").getValue());
			} else if (i == 2) {
				assertEquals("Airlines", token.getText());
				assertEquals(2, token.getAttributes().size());
				assertEquals("company", token.getAttributes().get("namedEntity").getValue());
				assertEquals("true", token.getAttributes().get("asianCompany").getValue());
			} else if (i == 3) {
				assertEquals("Company", token.getText());
				assertEquals(1, token.getAttributes().size());
				assertEquals("true", token.getAttributes().get("startsWithC").getValue());
			}
			i++;
		}
	}
}
