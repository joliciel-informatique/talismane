///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
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

import static org.junit.Assert.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

public class TokeniserDecisionTagSequenceImplTest {
	private static final Log LOG = LogFactory.getLog(TokeniserDecisionTagSequenceImplTest.class);

	@Test
	public void testGetTokenSequence() {
		final String sentence = "Je n'ai pas encore l'ourang-outan.";
		
		TokeniserDecision[] tokeniserDecisionArray = new TokeniserDecision[] {
				TokeniserDecision.DOES_SEPARATE, // Je
				TokeniserDecision.DOES_SEPARATE, // _
				TokeniserDecision.DOES_SEPARATE, // n
				TokeniserDecision.DOES_NOT_SEPARATE, // '
				TokeniserDecision.DOES_SEPARATE, // ai
				TokeniserDecision.DOES_SEPARATE, // _
				TokeniserDecision.DOES_SEPARATE, // pas
				TokeniserDecision.DOES_NOT_SEPARATE, // _
				TokeniserDecision.DOES_NOT_SEPARATE, // encore
				TokeniserDecision.DOES_SEPARATE, // _
				TokeniserDecision.DOES_SEPARATE, // l
				TokeniserDecision.DOES_NOT_SEPARATE, // '
				TokeniserDecision.DOES_SEPARATE, // ourang
				TokeniserDecision.DOES_NOT_SEPARATE, // -
				TokeniserDecision.DOES_NOT_SEPARATE, // outan
				TokeniserDecision.DOES_SEPARATE // .
		};

		TokeniserServiceInternal tokeniserService = new TokeniserServiceImpl();

		TokeniserDecisionTagSequenceImpl tokeniserDecisionTagSequence = new TokeniserDecisionTagSequenceImpl(sentence);
		tokeniserDecisionTagSequence.setTokeniserServiceInternal(tokeniserService);
		
		TokenSequence tokenSequence = tokeniserService.getTokenSequence(sentence, Tokeniser.SEPARATORS);
		
		int i = 0;
		for (Token token : tokenSequence.listWithWhiteSpace()) {
			TaggedToken<TokeniserDecision> decision = tokeniserService.getTaggedToken(token, tokeniserDecisionArray[i++], 1.0);
			tokeniserDecisionTagSequence.add(decision);
		}
		
			
		TokenSequence newTokenSequence = tokeniserDecisionTagSequence.getTokenSequence();
		LOG.debug(newTokenSequence);
		
		i = 0;
		for (Token token : newTokenSequence) {
			if (i==0) {
				assertEquals("Je", token.getText());
			} else if (i==1) {
				assertEquals("n'", token.getText());
			} else if (i==2) {
				assertEquals("ai", token.getText());	
			} else if (i==3) {
				assertEquals("pas encore", token.getText());	
			} else if (i==4) {
				assertEquals("l'", token.getText());	
			} else if (i==5) {
				assertEquals("ourang-outan", token.getText());	
			} else if (i==6) {
				assertEquals(".", token.getText());	
			} 
			i++;
		}
		assertEquals(7, newTokenSequence.size());
	}
}
