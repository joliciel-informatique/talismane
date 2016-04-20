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
package com.joliciel.talismane.tokeniser.patterns;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.Annotation;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.rawText.Sentence;
import com.joliciel.talismane.sentenceAnnotators.TokenPlaceholder;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class PatternTokeniserTest {
	private static final Logger LOG = LoggerFactory.getLogger(PatternTokeniserTest.class);

	@Test
	public void testTokenise() throws Exception {
		System.setProperty("config.file", "src/test/resources/test.conf");
		ConfigFactory.invalidateCaches();
		final Config config = ConfigFactory.load();

		final TalismaneSession session = new TalismaneSession(config, "");

		String[] labels = new String[0];

		final Sentence sentence = new Sentence("Je n'ai pas l'ourang-outan sur www.google.com.", session);
		List<Annotation<TokenPlaceholder>> annotations = new ArrayList<>();
		Annotation<TokenPlaceholder> annotation = new Annotation<TokenPlaceholder>("Je n'ai pas l'ourang-outan sur ".length(),
				"Je n'ai pas l'ourang-outan sur www.google.com".length(), new TokenPlaceholder("URL"), labels);
		annotations.add(annotation);
		sentence.addAnnotations(annotations);

		List<String> tokeniserPatterns = new ArrayList<String>();
		tokeniserPatterns.add("IS_NOT_SEPARATOR -_");
		tokeniserPatterns.add("IS_SEPARATOR_AFTER '");

		TokeniserPatternManager patternManager = new TokeniserPatternManager(tokeniserPatterns, session);
		PatternTokeniser tokeniser = new PatternTokeniser(null, patternManager, null, 1, session);
		List<TokenSequence> tokenSequences = tokeniser.tokenise(sentence);
		TokenSequence tokenSequence = tokenSequences.get(0);
		LOG.debug(tokenSequence.toString());

		assertEquals(9, tokenSequence.size());

		int i = 0;
		for (Token token : tokenSequence) {
			if (i == 0) {
				assertEquals("Je", token.getAnalyisText());
			} else if (i == 1) {
				assertEquals("n'", token.getAnalyisText());
			} else if (i == 2) {
				assertEquals("ai", token.getAnalyisText());
			} else if (i == 3) {
				assertEquals("pas", token.getAnalyisText());
			} else if (i == 4) {
				assertEquals("l'", token.getAnalyisText());
			} else if (i == 5) {
				assertEquals("ourang-outan", token.getAnalyisText());
			} else if (i == 6) {
				assertEquals("sur", token.getAnalyisText());
			} else if (i == 7) {
				assertEquals("URL", token.getAnalyisText());
			} else if (i == 8) {
				assertEquals(".", token.getAnalyisText());
			}
			i++;
		}
	}
}