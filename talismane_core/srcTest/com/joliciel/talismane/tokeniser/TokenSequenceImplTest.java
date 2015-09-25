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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import mockit.NonStrict;
import mockit.NonStrictExpectations;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import com.joliciel.talismane.filters.Sentence;
import com.joliciel.talismane.filters.SentenceTag;
import com.joliciel.talismane.tokeniser.filters.TokenPlaceholder;

public class TokenSequenceImplTest {
	private static final Log LOG = LogFactory.getLog(TokenSequenceImplTest.class);

	@Test
	public void testTokeniseSentence(@NonStrict final Sentence sentence) {
		TokeniserServiceInternal tokeniserServiceInternal = new TokeniserServiceImpl();
		final String separators="[\\s\\p{Punct}]";
		Pattern separatorPattern = Pattern.compile(separators);
		
		new NonStrictExpectations() {
			{
				sentence.getText(); returns("Je n'ai pas l'ourang-outan.");
				SentenceTag sentenceTag = new SentenceTag("Je n'ai pas l'".length(), "Animal", "Singe");
				sentenceTag.setEndIndex("Je n'ai pas l'ourang-outan".length());
				List<SentenceTag> sentenceTags = new ArrayList<SentenceTag>();
				sentenceTags.add(sentenceTag);
				sentence.getSentenceTags(); returns(sentenceTags);
			}
		};
		
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
				assertTrue(token.getAttributes().containsKey("Animal"));
			} else if (i==11) {
				assertEquals("-", token.getText());	
				assertEquals(true, token.isSeparator());
				assertTrue(token.getAttributes().containsKey("Animal"));
			} else if (i==12) {
				assertEquals("outan", token.getText());	
				assertEquals(false, token.isSeparator());
				assertTrue(token.getAttributes().containsKey("Animal"));
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
	public void testSimpleAddByIndex(@NonStrict final Sentence sentence) {
		TokeniserServiceInternal tokeniserServiceInternal = new TokeniserServiceImpl();
		
		new NonStrictExpectations() {
			{
				sentence.getText(); returns("The quick brown fox.");
			}
		};
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

	@Test
	public void testTokeniseSentenceWithPlaceholders(@NonStrict final Sentence sentence) {
		TokeniserServiceInternal tokeniserServiceInternal = new TokeniserServiceImpl();
		
		final String separators="[\\s\\p{Punct}]";
		Pattern separatorPattern = Pattern.compile(separators);
		
		final Set<TokenPlaceholder> placeholders = new LinkedHashSet<TokenPlaceholder>();
		
		new NonStrictExpectations() {
			TokenPlaceholder placeholder0, placeholder1;
			{
				sentence.getText(); returns("Write to me at joe.schome@test.com, otherwise go to http://test.com.");
				
				placeholder0.getStartIndex(); returns(15);
				placeholder0.getEndIndex(); returns(34);
				placeholder0.getReplacement(); returns("Email");
				placeholders.add(placeholder0);
				
				placeholder1.getStartIndex(); returns(52);
				placeholder1.getEndIndex(); returns(67);
				placeholder1.getReplacement(); returns("URL");
				placeholders.add(placeholder1);
				
			}
		};

		TokenSequenceImpl tokenSequence = new TokenSequenceImpl(sentence, separatorPattern, placeholders, tokeniserServiceInternal);
		
		assertEquals(19, tokenSequence.listWithWhiteSpace().size());
		assertEquals(11, tokenSequence.size());
		
		int i = 0;
		for (Token token : tokenSequence.listWithWhiteSpace()) {
			if (i==0) {
				assertEquals("Write", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i==1) {
				assertEquals(" ", token.getText());
				assertEquals(true, token.isSeparator());
			} else if (i==2) {
				assertEquals("to", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i==3) {
				assertEquals(" ", token.getText());	
				assertEquals(true, token.isSeparator());
			} else if (i==4) {
				assertEquals("me", token.getText());	
				assertEquals(false, token.isSeparator());
			} else if (i==5) {
				assertEquals(" ", token.getText());	
				assertEquals(true, token.isSeparator());
			} else if (i==6) {
				assertEquals("at", token.getText());	
				assertEquals(false, token.isSeparator());
			} else if (i==7) {
				assertEquals(" ", token.getText());	
				assertEquals(true, token.isSeparator());
			} else if (i==8) {
				assertEquals("Email", token.getText());	
				assertEquals(false, token.isSeparator());
			} else if (i==9) {
				assertEquals(",", token.getText());	
				assertEquals(true, token.isSeparator());
			} else if (i==10) {
				assertEquals(" ", token.getText());	
				assertEquals(true, token.isSeparator());
			} else if (i==11) {
				assertEquals("otherwise", token.getText());	
				assertEquals(false, token.isSeparator());
			} else if (i==12) {
				assertEquals(" ", token.getText());	
				assertEquals(true, token.isSeparator());
			} else if (i==13) {
				assertEquals("go", token.getText());	
				assertEquals(false, token.isSeparator());
			} else if (i==14) {
				assertEquals(" ", token.getText());	
				assertEquals(true, token.isSeparator());
			} else if (i==15) {
				assertEquals("to", token.getText());	
				assertEquals(false, token.isSeparator());
			} else if (i==16) {
				assertEquals(" ", token.getText());	
				assertEquals(true, token.isSeparator());
			} else if (i==17) {
				assertEquals("URL", token.getText());	
				assertEquals(false, token.isSeparator());
			} else if (i==18) {
				assertEquals(".", token.getText());	
				assertEquals(true, token.isSeparator());
			}
			i++;
		}
		
		i = 0;
		for (Token token : tokenSequence) {
			if (i==0) {
				assertEquals("Write", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i==1) {
				assertEquals("to", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i==2) {
				assertEquals("me", token.getText());	
				assertEquals(false, token.isSeparator());
			} else if (i==3) {
				assertEquals("at", token.getText());	
				assertEquals(false, token.isSeparator());
			} else if (i==4) {
				assertEquals("Email", token.getText());	
				assertEquals(false, token.isSeparator());
			} else if (i==5) {
				assertEquals(",", token.getText());	
				assertEquals(true, token.isSeparator());
			} else if (i==6) {
				assertEquals("otherwise", token.getText());	
				assertEquals(false, token.isSeparator());
			} else if (i==7) {
				assertEquals("go", token.getText());	
				assertEquals(false, token.isSeparator());
			} else if (i==8) {
				assertEquals("to", token.getText());	
				assertEquals(false, token.isSeparator());
			} else if (i==9) {
				assertEquals("URL", token.getText());	
				assertEquals(false, token.isSeparator());
			} else if (i==10) {
				assertEquals(".", token.getText());	
				assertEquals(true, token.isSeparator());
			} 
			i++;
		}
	}
	
	@Test
	public void testTokeniseSentenceWithPlaceholdersNoSeparators(@NonStrict final Sentence sentence) {
		TokeniserServiceInternal tokeniserServiceInternal = new TokeniserServiceImpl();
		
		final String separators="[\\s\\p{Punct}]";
		Pattern separatorPattern = Pattern.compile(separators);
		
		final Set<TokenPlaceholder> placeholders = new LinkedHashSet<TokenPlaceholder>();
		
		new NonStrictExpectations() {
			TokenPlaceholder placeholder0;
			{
				sentence.getText(); returns("Il t’aime.");
				
				placeholder0.getStartIndex(); returns("Il t".length());
				placeholder0.getEndIndex(); returns("Il t’".length());
				placeholder0.getReplacement(); returns("'");
				placeholders.add(placeholder0);
			}
		};

		TokenSequenceImpl tokenSequence = new TokenSequenceImpl(sentence, separatorPattern, placeholders, tokeniserServiceInternal);
		LOG.debug(tokenSequence.listWithWhiteSpace());
		LOG.debug(tokenSequence);
		assertEquals(6, tokenSequence.listWithWhiteSpace().size());
		assertEquals(5, tokenSequence.size());
		
		int i = 0;
		for (Token token : tokenSequence.listWithWhiteSpace()) {
			if (i==0) {
				assertEquals("Il", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i==1) {
				assertEquals(" ", token.getText());
				assertEquals(true, token.isSeparator());
			} else if (i==2) {
				assertEquals("t", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i==3) {
				assertEquals("'", token.getText());	
				assertEquals(true, token.isSeparator());
			} else if (i==4) {
				assertEquals("aime", token.getText());	
				assertEquals(false, token.isSeparator());
			} else if (i==5) {
				assertEquals(".", token.getText());	
				assertEquals(true, token.isSeparator());
			}
			i++;
		}
		
		i = 0;
		for (Token token : tokenSequence) {
			if (i==0) {
				assertEquals("Il", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i==1) {
				assertEquals("t", token.getText());
				assertEquals(false, token.isSeparator());
			} else if (i==2) {
				assertEquals("'", token.getText());	
				assertEquals(true, token.isSeparator());
			} else if (i==3) {
				assertEquals("aime", token.getText());	
				assertEquals(false, token.isSeparator());
			} else if (i==4) {
				assertEquals(".", token.getText());	
				assertEquals(true, token.isSeparator());
			}
			i++;
		}
	}
}
