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
package com.joliciel.talismane.tokeniser.patterns;

import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

import mockit.NonStrict;
import mockit.NonStrictExpectations;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.patterns.TokenMatch;
import com.joliciel.talismane.tokeniser.patterns.TokenPatternImpl;
import com.joliciel.talismane.tokeniser.patterns.TokenPatternMatch;

public class TokeniserPatternImplTest {
	private static final Log LOG = LogFactory.getLog(TokeniserPatternImplTest.class);

	@Test
	public void testGetParsedPattern() {
		final String separators="[\\s\\p{Punct}]";
		Pattern separatorPattern = Pattern.compile(separators);
		
		List<String> testPatterns = new ArrayList<String>();
		testPatterns.add(".+'.+");
		testPatterns.add(".+-t-elle");
		testPatterns.add(".+\\..+");
		testPatterns.add("\\d+ \\d+");
		testPatterns.add("lors (de|du|des|d)");
		testPatterns.add("([^cdjlmnst]|..+)'.+");
		testPatterns.add(".+-t{-}elle");
		
		
		int i = 0;
		for (String testPattern : testPatterns) {
			LOG.debug("Test Pattern = " + testPatterns.get(i));
			
			TokenPatternImpl tokeniserPattern = new TokenPatternImpl(testPattern, separatorPattern);
			
			List<Pattern> parsedPattern = tokeniserPattern.getParsedPattern();
			LOG.debug("Parsed Pattern = " + parsedPattern);
			if (i==0) {
				assertEquals(".+", parsedPattern.get(0).pattern());
				assertEquals("'", parsedPattern.get(1).pattern());
				assertEquals(".+", parsedPattern.get(2).pattern());
			} else if (i==1) {
				assertEquals(".+", parsedPattern.get(0).pattern());
				assertEquals("-", parsedPattern.get(1).pattern());
				assertEquals("t", parsedPattern.get(2).pattern());
				assertEquals("-", parsedPattern.get(3).pattern());
				assertEquals("elle", parsedPattern.get(4).pattern());
				assertEquals(4, tokeniserPattern.getIndexesToTest().size());
				assertEquals(1, tokeniserPattern.getIndexesToTest().get(0).intValue());
				assertEquals(2, tokeniserPattern.getIndexesToTest().get(1).intValue());
				assertEquals(3, tokeniserPattern.getIndexesToTest().get(2).intValue());
				assertEquals(4, tokeniserPattern.getIndexesToTest().get(3).intValue());
			} else if (i==2) {
				assertEquals(".+", parsedPattern.get(0).pattern());
				assertEquals("\\.", parsedPattern.get(1).pattern());
				assertEquals(".+", parsedPattern.get(2).pattern());
			} else if (i==3) {
				assertEquals("\\d+", parsedPattern.get(0).pattern());
				assertEquals(" ", parsedPattern.get(1).pattern());
				assertEquals("\\d+", parsedPattern.get(2).pattern());
			} else if (i==4) {
				assertEquals("[lL]ors", parsedPattern.get(0).pattern());
				assertEquals(" ", parsedPattern.get(1).pattern());
				assertEquals("(de|du|des|d)", parsedPattern.get(2).pattern());
			} else if (i==5) {
				assertEquals("([^cdjlmnst]|..+)", parsedPattern.get(0).pattern());
				assertEquals("'", parsedPattern.get(1).pattern());
				assertEquals(".+", parsedPattern.get(2).pattern());
			} else if (i==6) {
				assertEquals(".+", parsedPattern.get(0).pattern());
				assertEquals("-", parsedPattern.get(1).pattern());
				assertEquals("t", parsedPattern.get(2).pattern());
				assertEquals("-", parsedPattern.get(3).pattern());
				assertEquals("elle", parsedPattern.get(4).pattern());
				assertEquals(3, tokeniserPattern.getIndexesToTest().size());
				assertEquals(1, tokeniserPattern.getIndexesToTest().get(0).intValue());
				assertEquals(2, tokeniserPattern.getIndexesToTest().get(1).intValue());
				assertEquals(4, tokeniserPattern.getIndexesToTest().get(2).intValue());
			} 
			i++;
		}
	}
	
	@Test
	public void testMatch(@NonStrict final TokenSequence tokenSequence) {
		final String separators="[\\s\\p{Punct}]";
		final List<TokenMatch> matches3 = new ArrayList<TokenMatch>();
		final List<TokenMatch> matches4 = new ArrayList<TokenMatch>();
		final List<TokenMatch> matches5 = new ArrayList<TokenMatch>();
		final List<TokenMatch> matches6 = new ArrayList<TokenMatch>();
		final List<TokenMatch> matches7 = new ArrayList<TokenMatch>();
		
		new NonStrictExpectations() {
			Iterator<Token> i;
			List<Token> listWithWhiteSpaces;
			Token token0, token1, token2, token3, token4, token5, token6, token7;

			{
				tokenSequence.listWithWhiteSpace(); returns(listWithWhiteSpaces);
				
				listWithWhiteSpaces.get(0); returns(token0);
				token0.getText(); returns("Moi");
				token0.isSeparator(); returns(false);
				token0.getIndex(); returns(0);

				listWithWhiteSpaces.get(1); returns(token1);
				token1.getText(); returns(",");
				token1.isSeparator(); returns(true);
				token1.getIndex(); returns(1);

				listWithWhiteSpaces.get(2); returns(token2);
				token2.getText(); returns(" ");
				token2.isSeparator(); returns(true);
				token2.getIndex(); returns(2);

				listWithWhiteSpaces.get(3); returns(token3);
				token3.getText(); returns("j");
				token3.isSeparator(); returns(false);
				token3.getIndex(); returns(3);
				token3.getMatches(); returns(matches3);
	
				listWithWhiteSpaces.get(4); returns(token4);
				token4.getText(); returns("'");
				token4.isSeparator(); returns(true);
				token4.getIndex(); returns(4);
				token4.getMatches(); returns(matches4);
				
				listWithWhiteSpaces.get(5); returns(token5);
				token5.getText(); returns("aim");
				token5.isSeparator(); returns(false);
				token5.getIndex(); returns(5);
				token5.getMatches(); returns(matches5);
				
				listWithWhiteSpaces.get(6); returns(token6);
				token6.getText(); returns("'");
				token6.isSeparator(); returns(true);
				token6.getIndex(); returns(6);
				token6.getMatches(); returns(matches6);
				
				listWithWhiteSpaces.get(7); returns(token7);
				token7.getText(); returns("rais");
				token7.isSeparator(); returns(false);
				token7.getIndex(); returns(7);
				token7.getMatches(); returns(matches7);

				listWithWhiteSpaces.size(); returns(8);
				listWithWhiteSpaces.iterator(); returns(i);
				i.hasNext(); returns(true, true, true, true, true, true, true, true, false);
				i.next(); returns(token0, token1, token2, token3, token4, token5, token6, token7);
			}
		};
		
		Pattern separatorPattern = Pattern.compile(separators);
		TokenPatternImpl tokeniserPatternImpl = new TokenPatternImpl(".+'.+", separatorPattern);
		
		List<TokenPatternMatch> patternMatches = tokeniserPatternImpl.match(tokenSequence);
		assertEquals(2, patternMatches.size());
		
		List<Token> patternMatch = patternMatches.get(0).getTokenSequence();
		
		assertEquals(3, patternMatch.size());
		for (int i=0;i<3;i++) {
			Token token = patternMatch.get(i);
			if (i==0) {
				assertEquals(3, token.getIndex());
				assertEquals("j", token.getText());
				assertEquals(1, token.getMatches().size());
				assertEquals(0, token.getMatches().get(0).getIndex());
			}
			if (i==1) {
				assertEquals(4, token.getIndex());
				assertEquals("'", token.getText());
				assertEquals(1, token.getMatches().size());
				assertEquals(1, token.getMatches().get(0).getIndex());
			}
			if (i==2) {
				assertEquals(5, token.getIndex());
				assertEquals("aim", token.getText());
				assertEquals(2, token.getMatches().size());
				assertEquals(2, token.getMatches().get(0).getIndex());
			}
		}
		
		patternMatch = patternMatches.get(1).getTokenSequence();
		
		assertEquals(3, patternMatch.size());
		for (int i=0;i<3;i++) {
			Token token = patternMatch.get(i);
			if (i==0) {
				assertEquals(5, token.getIndex());
				assertEquals("aim", token.getText());
				assertEquals(2, token.getMatches().size());
				assertEquals(0, token.getMatches().get(1).getIndex());
			}
			if (i==1) {
				assertEquals(6, token.getIndex());
				assertEquals("'", token.getText());
				assertEquals(1, token.getMatches().size());
				assertEquals(1, token.getMatches().get(0).getIndex());
			}
			if (i==2) {
				assertEquals(7, token.getIndex());
				assertEquals("rais", token.getText());
				assertEquals(1, token.getMatches().size());
				assertEquals(2, token.getMatches().get(0).getIndex());
			}
		}
		
	}

}
