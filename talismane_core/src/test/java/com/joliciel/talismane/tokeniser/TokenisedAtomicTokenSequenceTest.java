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

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.rawText.Sentence;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class TokenisedAtomicTokenSequenceTest {
	private static final Logger LOG = LoggerFactory.getLogger(TokenisedAtomicTokenSequenceTest.class);

	@Test
	public void testGetTokenSequence() throws Exception {
		System.setProperty("config.file", "src/test/resources/test.conf");
		ConfigFactory.invalidateCaches();
		final Config config = ConfigFactory.load();

		final TalismaneSession talismaneSession = new TalismaneSession(config, "");
		final Sentence sentence = new Sentence("Je n'ai pas encore l'ourang-outan.", talismaneSession);

		TokeniserOutcome[] tokeniserOutcomeArray = new TokeniserOutcome[] { TokeniserOutcome.SEPARATE, // Je
				TokeniserOutcome.SEPARATE, // _
				TokeniserOutcome.SEPARATE, // n
				TokeniserOutcome.JOIN, // '
				TokeniserOutcome.SEPARATE, // ai
				TokeniserOutcome.SEPARATE, // _
				TokeniserOutcome.SEPARATE, // pas
				TokeniserOutcome.JOIN, // _
				TokeniserOutcome.JOIN, // encore
				TokeniserOutcome.SEPARATE, // _
				TokeniserOutcome.SEPARATE, // l
				TokeniserOutcome.JOIN, // '
				TokeniserOutcome.SEPARATE, // ourang
				TokeniserOutcome.JOIN, // -
				TokeniserOutcome.JOIN, // outan
				TokeniserOutcome.SEPARATE // .
		};

		TokenisedAtomicTokenSequence atomicTokenSequence = new TokenisedAtomicTokenSequence(sentence, talismaneSession);

		TokenSequence tokenSequence = new TokenSequence(sentence, talismaneSession);
		tokenSequence.findDefaultTokens();

		int i = 0;
		for (Token token : tokenSequence.listWithWhiteSpace()) {
			Decision decision = new Decision(tokeniserOutcomeArray[i++].name());
			TaggedToken<TokeniserOutcome> taggedToken = new TaggedToken<>(token, decision, TokeniserOutcome.valueOf(decision.getOutcome()));

			atomicTokenSequence.add(taggedToken);
		}

		TokenSequence newTokenSequence = atomicTokenSequence.inferTokenSequence();
		LOG.debug(newTokenSequence.toString());

		i = 0;
		for (Token token : newTokenSequence) {
			if (i == 0) {
				assertEquals("Je", token.getAnalyisText());
			} else if (i == 1) {
				assertEquals("n'", token.getAnalyisText());
			} else if (i == 2) {
				assertEquals("ai", token.getAnalyisText());
			} else if (i == 3) {
				assertEquals("pas encore", token.getAnalyisText());
			} else if (i == 4) {
				assertEquals("l'", token.getAnalyisText());
			} else if (i == 5) {
				assertEquals("ourang-outan", token.getAnalyisText());
			} else if (i == 6) {
				assertEquals(".", token.getAnalyisText());
			}
			i++;
		}
		assertEquals(7, newTokenSequence.size());
	}
}
