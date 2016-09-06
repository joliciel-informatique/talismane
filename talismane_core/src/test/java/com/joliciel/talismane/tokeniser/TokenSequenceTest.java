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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.filters.Sentence;
import com.joliciel.talismane.filters.SentenceTag;
import com.joliciel.talismane.tokeniser.filters.TokenPlaceholder;

import mockit.NonStrict;
import mockit.NonStrictExpectations;

public class TokenSequenceTest {
	private static final Logger LOG = LoggerFactory.getLogger(TokenSequenceTest.class);

	@Test
	public void testTokeniseSentence(@NonStrict final Sentence sentence) {
		final TalismaneSession talismaneSession = TalismaneSession.getInstance("");
		final String separators = "[\\s\\p{Punct}]";
		Pattern separatorPattern = Pattern.compile(separators);

		new NonStrictExpectations() {
			{
				sentence.getText();
				returns("Je n'ai pas l'ourang-outan.");
				StringAttribute value = new StringAttribute("Singe");
				SentenceTag<String> sentenceTag = new SentenceTag<String>("Je n'ai pas l'".length(), "Animal", value);
				sentenceTag.setEndIndex("Je n'ai pas l'ourang-outan".length());
				List<SentenceTag<?>> sentenceTags = new ArrayList<SentenceTag<?>>();
				sentenceTags.add(sentenceTag);
				sentence.getSentenceTags();
				returns(sentenceTags);
			}
		};

		TokenSequence tokenSequence = new TokenSequence(sentence, separatorPattern, talismaneSession);

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
				assertTrue(token.getAttributes().containsKey("Animal"));
			} else if (i == 11) {
				assertEquals("-", token.getText());
				assertEquals(true, token.isSeparator());
				assertTrue(token.getAttributes().containsKey("Animal"));
			} else if (i == 12) {
				assertEquals("outan", token.getText());
				assertEquals(false, token.isSeparator());
				assertTrue(token.getAttributes().containsKey("Animal"));
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
	public void testSimpleAddByIndex(@NonStrict final Sentence sentence) {
		final TalismaneSession talismaneSession = TalismaneSession.getInstance("");

		new NonStrictExpectations() {
			{
				sentence.getText();
				returns("The quick brown fox.");
			}
		};
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
	public void testTokeniseSentenceWithPlaceholders(@NonStrict final Sentence sentence) {
		final TalismaneSession talismaneSession = TalismaneSession.getInstance("");

		final String separators = "[\\s\\p{Punct}]";
		Pattern separatorPattern = Pattern.compile(separators);

		final List<TokenPlaceholder> placeholders = new ArrayList<TokenPlaceholder>();
		TokenPlaceholder placeholder0 = new TokenPlaceholder("Write to me at ".length(), "Write to me at joe.schome@test.com".length(), "Email", "blah");
		placeholders.add(placeholder0);
		TokenPlaceholder placeholder1 = new TokenPlaceholder("Write to me at joe.schome@test.com, otherwise go to ".length(),
				"Write to me at joe.schome@test.com, otherwise go to http://test.com".length(), "URL", "blah");
		placeholders.add(placeholder1);

		new NonStrictExpectations() {

			{
				sentence.getText();
				returns("Write to me at joe.schome@test.com, otherwise go to http://test.com.");
			}
		};

		TokenSequence tokenSequence = new TokenSequence(sentence, separatorPattern, placeholders, talismaneSession);

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
	public void testTokeniseSentenceWithPlaceholdersNoSeparators(@NonStrict final Sentence sentence) {
		final TalismaneSession talismaneSession = TalismaneSession.getInstance("");

		final String separators = "[\\s\\p{Punct}]";
		Pattern separatorPattern = Pattern.compile(separators);

		final List<TokenPlaceholder> placeholders = new ArrayList<TokenPlaceholder>();
		TokenPlaceholder placeholder1 = new TokenPlaceholder("Il ".length(), "Il t’aime".length(), null, "blah");
		placeholder1.setSingleToken(false);
		placeholder1.addAttribute("phrase", new StringAttribute("verbal"));
		placeholder1.addAttribute("person", new StringAttribute("3rd"));
		placeholders.add(placeholder1);

		TokenPlaceholder placeholder2 = new TokenPlaceholder("Il ".length(), "Il t’".length(), null, "blah");
		placeholder2.setSingleToken(false);
		placeholder2.addAttribute("type", new StringAttribute("object"));
		placeholders.add(placeholder2);

		TokenPlaceholder placeholder0 = new TokenPlaceholder("Il t".length(), "Il t’".length(), "'", "blah");
		placeholder0.setSingleToken(true);

		placeholders.add(placeholder0);

		new NonStrictExpectations() {

			{
				sentence.getText();
				returns("Il t’aime.");

			}
		};

		TokenSequence tokenSequence = new TokenSequence(sentence, separatorPattern, placeholders, talismaneSession);
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
	public void testOverlappingPlaceholders(@NonStrict final Sentence sentence) {
		final TalismaneSession talismaneSession = TalismaneSession.getInstance("");

		final String separators = "[\\s\\p{Punct}]";
		Pattern separatorPattern = Pattern.compile(separators);

		final List<TokenPlaceholder> placeholders = new ArrayList<TokenPlaceholder>();
		TokenPlaceholder placeholder1 = new TokenPlaceholder("".length(), "Pakistan".length(), null, "blah");
		placeholder1.setSingleToken(false);
		placeholder1.addAttribute("namedEntity", new StringAttribute("place"));
		placeholder1.addAttribute("startsWithP", new StringAttribute("true"));
		placeholders.add(placeholder1);

		TokenPlaceholder placeholder2 = new TokenPlaceholder("".length(), "Pakistan International Airlines".length(), null, "blah");
		placeholder2.setSingleToken(false);
		placeholder2.addAttribute("namedEntity", new StringAttribute("company"));
		placeholder2.addAttribute("asianCompany", new StringAttribute("true"));
		placeholders.add(placeholder2);

		TokenPlaceholder placeholder3 = new TokenPlaceholder("Pakistan ".length(), "Pakistan International Airlines Company".length(), null, "blah");
		placeholder3.setSingleToken(false);
		placeholder3.addAttribute("namedEntity", new StringAttribute("company"));
		placeholder3.addAttribute("asianCompany", new StringAttribute("false"));
		placeholders.add(placeholder3);

		TokenPlaceholder placeholder4 = new TokenPlaceholder("Pakistan International Airlines ".length(), "Pakistan International Airlines Company".length(),
				null, "blah");
		placeholder4.setSingleToken(false);
		placeholder4.addAttribute("shouldNotBeSkipped", new StringAttribute("true"));
		placeholders.add(placeholder4);

		new NonStrictExpectations() {

			{
				sentence.getText();
				returns("Pakistan International Airlines Company");

			}
		};

		TokenSequence tokenSequence = new TokenSequence(sentence, separatorPattern, placeholders, talismaneSession);
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
				assertEquals("true", token.getAttributes().get("shouldNotBeSkipped").getValue());
			}
			i++;
		}
	}
}
