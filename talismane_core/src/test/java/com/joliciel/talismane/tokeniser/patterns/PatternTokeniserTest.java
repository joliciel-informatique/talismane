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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneServiceLocator;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.machineLearning.MachineLearningServiceLocator;
import com.joliciel.talismane.tokeniser.SeparatorDecision;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.TokeniserService;
import com.joliciel.talismane.tokeniser.TokeniserServiceLocator;

public class PatternTokeniserTest {
	private static final Logger LOG = LoggerFactory.getLogger(PatternTokeniserTest.class);

	@Test
	public void testTokenise() {
		TalismaneServiceLocator talismaneServiceLocator = TalismaneServiceLocator.getInstance("");
		TokeniserServiceLocator tokeniserServiceLocator = talismaneServiceLocator.getTokeniserServiceLocator();
		TokeniserService tokeniserService = tokeniserServiceLocator.getTokeniserService();
		final TalismaneSession talismaneSession = TalismaneServiceLocator.getInstance("").getTalismaneService().getTalismaneSession();

		MachineLearningServiceLocator machineLearningServiceLocator = MachineLearningServiceLocator.getInstance();
		MachineLearningService machineLearningService = machineLearningServiceLocator.getMachineLearningService();

		final String sentence = "Je n'ai pas l'ourang-outan.";
		final Map<SeparatorDecision, String> separatorDefaults = new HashMap<SeparatorDecision, String>();
		separatorDefaults.put(SeparatorDecision.IS_NOT_SEPARATOR, "-");
		separatorDefaults.put(SeparatorDecision.IS_SEPARATOR_AFTER, "'");

		List<String> tokeniserPatterns = new ArrayList<String>();
		TokeniserPatternManagerImpl patternManager = new TokeniserPatternManagerImpl(tokeniserPatterns);
		IntervalPatternTokeniser tokeniserImpl = new IntervalPatternTokeniser(patternManager, null, 1, talismaneSession);
		tokeniserImpl.setTokeniserService(tokeniserService);
		tokeniserImpl.setMachineLearningService(machineLearningService);
		patternManager.setSeparatorDefaults(separatorDefaults);
		List<TokenSequence> tokenSequences = tokeniserImpl.tokenise(sentence);
		TokenSequence tokenSequence = tokenSequences.get(0);
		LOG.debug(tokenSequence.toString());

		assertEquals(7, tokenSequence.size());

		int i = 0;
		for (Token token : tokenSequence) {
			if (i == 0) {
				assertEquals("Je", token.getText());
			} else if (i == 1) {
				assertEquals("n'", token.getText());
			} else if (i == 2) {
				assertEquals("ai", token.getText());
			} else if (i == 3) {
				assertEquals("pas", token.getText());
			} else if (i == 4) {
				assertEquals("l'", token.getText());
			} else if (i == 5) {
				assertEquals("ourang-outan", token.getText());
			} else if (i == 6) {
				assertEquals(".", token.getText());
			}
			i++;
		}
	}
}
