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

import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

public class TokenSequenceImplTest {
	private static final Log LOG = LogFactory.getLog(TokenSequenceImplTest.class);

	@Test
	public void testTokeniseSentence() {
		TokeniserServiceInternal tokeniserServiceInternal = new TokeniserServiceImpl();
		final String sentence = "Je n'ai pas l'ourang-outan.";
		final String separators="[\\s\\p{Punct}]";
		Pattern separatorPattern = Pattern.compile(separators);
		
		TokenSequenceImpl tokenSequence = new TokenSequenceImpl(sentence, separatorPattern, tokeniserServiceInternal);
		
		assertEquals(14, tokenSequence.listWithWhiteSpace().size());
		assertEquals(11, tokenSequence.size());
		
		int i = 0;
		for (Token token : tokenSequence.listWithWhiteSpace()) {
			if (i==0) {
				assertEquals("Je", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i==1) {
				assertEquals(" ", token.getText());
				assertEquals(true, token.isSeparator());
			} else if (i==2) {
				assertEquals("n", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i==3) {
				assertEquals("'", token.getText());	
				assertEquals(true, token.isSeparator());
			} else if (i==4) {
				assertEquals("ai", token.getText());	
				assertEquals(false, token.isSeparator());
			} else if (i==5) {
				assertEquals(" ", token.getText());	
				assertEquals(true, token.isSeparator());
			} else if (i==6) {
				assertEquals("pas", token.getText());	
				assertEquals(false, token.isSeparator());
			} else if (i==7) {
				assertEquals(" ", token.getText());	
				assertEquals(true, token.isSeparator());
			} else if (i==8) {
				assertEquals("l", token.getText());	
				assertEquals(false, token.isSeparator());
			} else if (i==9) {
				assertEquals("'", token.getText());	
				assertEquals(true, token.isSeparator());
			} else if (i==10) {
				assertEquals("ourang", token.getText());	
				assertEquals(false, token.isSeparator());
			} else if (i==11) {
				assertEquals("-", token.getText());	
				assertEquals(true, token.isSeparator());
			} else if (i==12) {
				assertEquals("outan", token.getText());	
				assertEquals(false, token.isSeparator());
			} else if (i==13) {
				assertEquals(".", token.getText());	
				assertEquals(true, token.isSeparator());
			} 
			i++;
		}
		
		i = 0;
		for (Token token : tokenSequence) {
			if (i==0) {
				assertEquals("Je", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i==1) {
				assertEquals("n", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i==2) {
				assertEquals("'", token.getText());	
				assertEquals(true, token.isSeparator());
			} else if (i==3) {
				assertEquals("ai", token.getText());	
				assertEquals(false, token.isSeparator());
			} else if (i==4) {
				assertEquals("pas", token.getText());	
				assertEquals(false, token.isSeparator());
			} else if (i==5) {
				assertEquals("l", token.getText());	
				assertEquals(false, token.isSeparator());
			} else if (i==6) {
				assertEquals("'", token.getText());	
				assertEquals(true, token.isSeparator());
			} else if (i==7) {
				assertEquals("ourang", token.getText());	
				assertEquals(false, token.isSeparator());
			} else if (i==8) {
				assertEquals("-", token.getText());	
				assertEquals(true, token.isSeparator());
			} else if (i==9) {
				assertEquals("outan", token.getText());	
				assertEquals(false, token.isSeparator());
			} else if (i==10) {
				assertEquals(".", token.getText());	
				assertEquals(true, token.isSeparator());
			} 
			i++;
		}
	}
	
	@Test
	public void testSimpleAddByIndex() {
		TokeniserServiceInternal tokeniserServiceInternal = new TokeniserServiceImpl();
		String sentence = "The quick brown fox.";
		LOG.debug("Sentence length: " + sentence.length());
		
		TokenSequenceImpl tokenSequence = new TokenSequenceImpl(sentence);
		tokenSequence.setTokeniserServiceInternal(tokeniserServiceInternal);
		tokenSequence.addToken(16,19); // fox
		tokenSequence.addToken(4,9); // quick
		tokenSequence.addToken(4,9); // quick - should be ignored
		tokenSequence.addToken(0,3); // The
		tokenSequence.addToken(19,20); // .
		tokenSequence.addToken(10,12); // br - should be removed by brown
		tokenSequence.addToken(12,15); // own - should be removed by brown
		tokenSequence.addToken(10,15); // brown
		tokenSequence.finalise();
		
		assertEquals(5, tokenSequence.size());
		int i = 0;
		for (Token token : tokenSequence) {
			LOG.debug(token.getText());
			if (i==0) {
				assertEquals("The", token.getText());
			}
			assertEquals(i, token.getIndex());
			i++;
		}

		LOG.debug("Token splits:");
		for (int tokenSplit : tokenSequence.getTokenSplits()) {
			LOG.debug(tokenSplit);
		}
		
		assertEquals(9, tokenSequence.getTokenSplits().size());
	}

	
}
