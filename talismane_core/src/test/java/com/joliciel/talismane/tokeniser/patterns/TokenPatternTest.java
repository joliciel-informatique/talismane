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

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneTest;
import com.joliciel.talismane.rawText.Sentence;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.Tokeniser;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TokenPatternTest extends TalismaneTest {
  private static final Logger LOG = LoggerFactory.getLogger(TokenPatternTest.class);

  @Test
  public void testParsePattern() throws Exception {
    System.setProperty("config.file", "src/test/resources/test.conf");
    ConfigFactory.invalidateCaches();
    final Config config = ConfigFactory.load();

    final String sessionId = "test";

    String regexp = "(?![cdCD]\\z|qu\\z|jusqu\\z).+'.+";
    TokenPattern tokenPattern = new TokenPattern(regexp, Tokeniser.getTokenSeparators(sessionId));
    List<Pattern> patterns = tokenPattern.getParsedPattern();
    Assert.assertEquals(3, patterns.size());
    int i = 0;
    for (Pattern pattern : patterns) {
      if (i == 0) {
        Assert.assertEquals("(?![cdCD]\\z|[qQ]u\\z|[jJ]usqu\\z).+", pattern.pattern());
      } else if (i == 1) {
        Assert.assertEquals("'", pattern.pattern());
      } else if (i == 2) {
        Assert.assertEquals(".+", pattern.pattern());
      }
      i++;
    }

    regexp = "être (de|d)";
    tokenPattern = new TokenPattern(regexp, Tokeniser.getTokenSeparators(sessionId));
    patterns = tokenPattern.getParsedPattern();
    Assert.assertEquals(3, patterns.size());
    i = 0;
    for (Pattern pattern : patterns) {
      if (i == 0) {
        Assert.assertEquals("[êÊE]tre", pattern.pattern());
      } else if (i == 1) {
        Assert.assertEquals(" ", pattern.pattern());
      } else if (i == 2) {
        Assert.assertEquals("(de|d)", pattern.pattern());
      }
      i++;
    }

    regexp = ".+\\.\\p";
    tokenPattern = new TokenPattern(regexp, Tokeniser.getTokenSeparators(sessionId));
    patterns = tokenPattern.getParsedPattern();
    Assert.assertEquals(3, patterns.size());
    i = 0;
    for (Pattern pattern : patterns) {
      if (i == 0) {
        Assert.assertEquals(".+", pattern.pattern());
      } else if (i == 1) {
        Assert.assertEquals("\\.", pattern.pattern());
      } else if (i == 2) {
        Assert.assertEquals(Tokeniser.getTokenSeparators(sessionId).pattern(), pattern.pattern());
      }
      i++;
    }

    regexp = ".+qu'";
    tokenPattern = new TokenPattern(regexp, Tokeniser.getTokenSeparators(sessionId));
    patterns = tokenPattern.getParsedPattern();
    Assert.assertEquals(2, patterns.size());
    i = 0;
    for (Pattern pattern : patterns) {
      if (i == 0) {
        Assert.assertEquals(".+qu", pattern.pattern());
      } else if (i == 1) {
        Assert.assertEquals("'", pattern.pattern());
      }
      i++;
    }

    regexp = "\\D+\\.a[ \\)]c[abc]";
    tokenPattern = new TokenPattern(regexp, Tokeniser.getTokenSeparators(sessionId));
    patterns = tokenPattern.getParsedPattern();
    LOG.debug(patterns.toString());
    Assert.assertEquals(5, patterns.size());
    i = 0;
    for (Pattern pattern : patterns) {
      if (i == 0) {
        Assert.assertEquals("\\D+", pattern.pattern());
      } else if (i == 1) {
        Assert.assertEquals("\\.", pattern.pattern());
      } else if (i == 2) {
        Assert.assertEquals("a", pattern.pattern());
      } else if (i == 3) {
        Assert.assertEquals("[ \\)]", pattern.pattern());
      } else if (i == 4) {
        Assert.assertEquals("c[abc]", pattern.pattern());
      }
      i++;
    }
  }

  @Test
  public void testGetParsedPattern() throws TalismaneException {
    final String separators = "[\\s\\p{Punct}]";
    Pattern separatorPattern = Pattern.compile(separators, Pattern.UNICODE_CHARACTER_CLASS);

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

      TokenPattern tokeniserPattern = new TokenPattern(testPattern, separatorPattern);

      List<Pattern> parsedPattern = tokeniserPattern.getParsedPattern();
      LOG.debug("Parsed Pattern = " + parsedPattern);
      if (i == 0) {
        assertEquals(".+", parsedPattern.get(0).pattern());
        assertEquals("'", parsedPattern.get(1).pattern());
        assertEquals(".+", parsedPattern.get(2).pattern());
      } else if (i == 1) {
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
      } else if (i == 2) {
        assertEquals(".+", parsedPattern.get(0).pattern());
        assertEquals("\\.", parsedPattern.get(1).pattern());
        assertEquals(".+", parsedPattern.get(2).pattern());
      } else if (i == 3) {
        assertEquals("\\d+", parsedPattern.get(0).pattern());
        assertEquals(" ", parsedPattern.get(1).pattern());
        assertEquals("\\d+", parsedPattern.get(2).pattern());
      } else if (i == 4) {
        assertEquals("[lL]ors", parsedPattern.get(0).pattern());
        assertEquals(" ", parsedPattern.get(1).pattern());
        assertEquals("(de|du|des|d)", parsedPattern.get(2).pattern());
      } else if (i == 5) {
        assertEquals("([^cdjlmnst]|..+)", parsedPattern.get(0).pattern());
        assertEquals("'", parsedPattern.get(1).pattern());
        assertEquals(".+", parsedPattern.get(2).pattern());
      } else if (i == 6) {
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
  public void testMatch() throws TalismaneException {
    final String separators = "[\\s\\p{Punct}]";
    final List<TokenPatternMatch> matches3 = new ArrayList<TokenPatternMatch>();
    final List<TokenPatternMatch> matches4 = new ArrayList<TokenPatternMatch>();
    final List<TokenPatternMatch> matches5 = new ArrayList<TokenPatternMatch>();
    final List<TokenPatternMatch> matches6 = new ArrayList<TokenPatternMatch>();
    final List<TokenPatternMatch> matches7 = new ArrayList<TokenPatternMatch>();

    final TokenSequence tokenSequence = mock(TokenSequence.class);
    final Token token0 = mock(Token.class);
    final Token token1 = mock(Token.class);
    final Token token2 = mock(Token.class);
    final Token token3 = mock(Token.class);
    final Token token4 = mock(Token.class);
    final Token token5 = mock(Token.class);
    final Token token6 = mock(Token.class);
    final Token token7 = mock(Token.class);
    
    final List<Token> listWithWhiteSpaces = Arrays.asList(token0, token1, token2, token3, token4, token5, token6, token7);
    
    when (tokenSequence.listWithWhiteSpace()).thenReturn(listWithWhiteSpaces);
   
    when (token0.getAnalyisText()).thenReturn("Moi");
    when (token0.isSeparator()).thenReturn(false);
    when (token0.getIndex()).thenReturn(0);

    when (token1.getAnalyisText()).thenReturn(",");
    when (token1.isSeparator()).thenReturn(true);
    when (token1.getIndex()).thenReturn(1);

    when (token2.getAnalyisText()).thenReturn(" ");
    when (token2.isSeparator()).thenReturn(true);
    when (token2.getIndex()).thenReturn(2);

    when (token3.getAnalyisText()).thenReturn("j");
    when (token3.isSeparator()).thenReturn(false);
    when (token3.getIndex()).thenReturn(3);
    when (token3.getMatches()).thenReturn(matches3);

    when (token4.getAnalyisText()).thenReturn("'");
    when (token4.isSeparator()).thenReturn(true);
    when (token4.getIndex()).thenReturn(4);
    when (token4.getMatches()).thenReturn(matches4);
    
    when (token5.getAnalyisText()).thenReturn("aim");
    when (token5.isSeparator()).thenReturn(false);
    when (token5.getIndex()).thenReturn(5);
    when (token5.getMatches()).thenReturn(matches5);
    
    when (token6.getAnalyisText()).thenReturn("'");
    when (token6.isSeparator()).thenReturn(true);
    when (token6.getIndex()).thenReturn(6);
    when (token6.getMatches()).thenReturn(matches6);
    
    when (token7.getAnalyisText()).thenReturn("rais");
    when (token7.isSeparator()).thenReturn(false);
    when (token7.getIndex()).thenReturn(7);
    when (token7.getMatches()).thenReturn(matches7);

    Pattern separatorPattern = Pattern.compile(separators, Pattern.UNICODE_CHARACTER_CLASS);
    TokenPattern tokeniserPatternImpl = new TokenPattern(".+'.+", separatorPattern);

    List<TokenPatternMatchSequence> patternMatches = tokeniserPatternImpl.match(tokenSequence);
    assertEquals(2, patternMatches.size());

    List<Token> patternMatch = patternMatches.get(0).getTokenSequence();

    assertEquals(3, patternMatch.size());
    for (int i = 0; i < 3; i++) {
      Token token = patternMatch.get(i);
      if (i == 0) {
        assertEquals(3, token.getIndex());
        assertEquals("j", token.getAnalyisText());
        assertEquals(1, token.getMatches().size());
        assertEquals(0, token.getMatches().get(0).getIndex());
      }
      if (i == 1) {
        assertEquals(4, token.getIndex());
        assertEquals("'", token.getAnalyisText());
        assertEquals(1, token.getMatches().size());
        assertEquals(1, token.getMatches().get(0).getIndex());
      }
      if (i == 2) {
        assertEquals(5, token.getIndex());
        assertEquals("aim", token.getAnalyisText());
        assertEquals(2, token.getMatches().size());
        assertEquals(2, token.getMatches().get(0).getIndex());
      }
    }

    patternMatch = patternMatches.get(1).getTokenSequence();

    assertEquals(3, patternMatch.size());
    for (int i = 0; i < 3; i++) {
      Token token = patternMatch.get(i);
      if (i == 0) {
        assertEquals(5, token.getIndex());
        assertEquals("aim", token.getAnalyisText());
        assertEquals(2, token.getMatches().size());
        assertEquals(0, token.getMatches().get(1).getIndex());
      }
      if (i == 1) {
        assertEquals(6, token.getIndex());
        assertEquals("'", token.getAnalyisText());
        assertEquals(1, token.getMatches().size());
        assertEquals(1, token.getMatches().get(0).getIndex());
      }
      if (i == 2) {
        assertEquals(7, token.getIndex());
        assertEquals("rais", token.getAnalyisText());
        assertEquals(1, token.getMatches().size());
        assertEquals(2, token.getMatches().get(0).getIndex());
      }
    }

  }

  @Test
  public void testMatch2() throws Exception {
    System.setProperty("config.file", "src/test/resources/test.conf");
    ConfigFactory.invalidateCaches();
    final Config config = ConfigFactory.load();

    final String sessionId = "test";
    final Sentence sentence = new Sentence("Qu'ensuite il aille...", sessionId);

    TokenSequence tokenSequence = new TokenSequence(sentence, sessionId);
    tokenSequence.findDefaultTokens();

    TokenPattern tokenPattern = new TokenPattern("{(?![cdjlmnstCDJLMNST]\\z|qu\\z|jusqu\\z|puisqu\\z|lorsqu\\z|aujourd\\z|prud\\z|quelqu\\z|quoiqu\\z).+'}.+",
        Tokeniser.getTokenSeparators(sessionId));

    List<TokenPatternMatchSequence> patternMatches = tokenPattern.match(tokenSequence);
    assertEquals(0, patternMatches.size());

  }

  @Test
  public void testMatch3() throws Exception {
    System.setProperty("config.file", "src/test/resources/test.conf");
    ConfigFactory.invalidateCaches();
    final Config config = ConfigFactory.load();

    final String sessionId = "test";
    final Sentence sentence = new Sentence("Z'ensuite il aille...", sessionId);

    TokenSequence tokenSequence = new TokenSequence(sentence, sessionId);
    tokenSequence.findDefaultTokens();

    TokenPattern tokenPattern = new TokenPattern("{(?![cdjlmnstCDJLMNST]\\z|qu\\z|jusqu\\z|puisqu\\z|lorsqu\\z|aujourd\\z|prud\\z|quelqu\\z|quoiqu\\z).+'}.+",
        Tokeniser.getTokenSeparators(sessionId));

    List<TokenPatternMatchSequence> patternMatches = tokenPattern.match(tokenSequence);
    assertEquals(1, patternMatches.size());

    TokenPatternMatchSequence matchSequence = patternMatches.get(0);
    assertEquals(3, matchSequence.getTokenSequence().size());
    assertEquals("Z", matchSequence.getTokenSequence().get(0).getOriginalText());

  }

  @Test
  public void testMatch4() throws Exception {
    System.setProperty("config.file", "src/test/resources/test.conf");
    ConfigFactory.invalidateCaches();
    final Config config = ConfigFactory.load();

    final String sessionId = "test";
    final Sentence sentence = new Sentence("Aix-les-Bains", sessionId);

    TokenSequence tokenSequence = new TokenSequence(sentence, sessionId);
    tokenSequence.findDefaultTokens();

    TokenPattern tokenPattern = new TokenPattern(".+-{(ce|je|la|le|les|leur|lui|moi|nous|toi|tu)[^-]}", Tokeniser.getTokenSeparators(sessionId));

    List<TokenPatternMatchSequence> patternMatches = tokenPattern.match(tokenSequence);
    assertEquals(0, patternMatches.size());

  }
}
