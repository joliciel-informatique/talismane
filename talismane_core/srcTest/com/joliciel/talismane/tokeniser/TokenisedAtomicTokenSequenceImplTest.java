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

import mockit.NonStrict;
import mockit.NonStrictExpectations;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import com.joliciel.talismane.filters.Sentence;
import com.joliciel.talismane.machineLearning.Decision;

public class TokenisedAtomicTokenSequenceImplTest {
	private static final Log LOG = LogFactory.getLog(TokenisedAtomicTokenSequenceImplTest.class);

	@Test
	public void testGetTokenSequence(@NonStrict final Sentence sentence) {
		new NonStrictExpectations() {
			{
				sentence.getText(); returns("Je n'ai pas encore l'ourang-outan.");
			}
		};
		
		TokeniserOutcome[] tokeniserOutcomeArray = new TokeniserOutcome[] {
				TokeniserOutcome.DOES_SEPARATE, // Je
				TokeniserOutcome.DOES_SEPARATE, // _
				TokeniserOutcome.DOES_SEPARATE, // n
				TokeniserOutcome.DOES_NOT_SEPARATE, // '
				TokeniserOutcome.DOES_SEPARATE, // ai
				TokeniserOutcome.DOES_SEPARATE, // _
				TokeniserOutcome.DOES_SEPARATE, // pas
				TokeniserOutcome.DOES_NOT_SEPARATE, // _
				TokeniserOutcome.DOES_NOT_SEPARATE, // encore
				TokeniserOutcome.DOES_SEPARATE, // _
				TokeniserOutcome.DOES_SEPARATE, // l
				TokeniserOutcome.DOES_NOT_SEPARATE, // '
				TokeniserOutcome.DOES_SEPARATE, // ourang
				TokeniserOutcome.DOES_NOT_SEPARATE, // -
				TokeniserOutcome.DOES_NOT_SEPARATE, // outan
				TokeniserOutcome.DOES_SEPARATE // .
		};

		TokeniserServiceInternal tokeniserService = new TokeniserServiceImpl();
		TokeniserDecisionFactory tokeniserDecisionFactory = new TokeniserDecisionFactory();

		TokenisedAtomicTokenSequenceImpl atomicTokenSequence = new TokenisedAtomicTokenSequenceImpl(sentence);
		atomicTokenSequence.setTokeniserServiceInternal(tokeniserService);
		
		TokenSequence tokenSequence = tokeniserService.getTokenSequence(sentence, Tokeniser.SEPARATORS);
		
		int i = 0;
		for (Token token : tokenSequence.listWithWhiteSpace()) {
			Decision<TokeniserOutcome> decision = tokeniserDecisionFactory.createDefaultDecision(tokeniserOutcomeArray[i++]);
			TaggedToken<TokeniserOutcome> taggedToken = tokeniserService.getTaggedToken(token, decision);
			atomicTokenSequence.add(taggedToken);
		}
		
			
		TokenSequence newTokenSequence = atomicTokenSequence.inferTokenSequence();
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
