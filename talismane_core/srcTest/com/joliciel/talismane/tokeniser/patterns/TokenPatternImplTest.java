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

import java.util.List;
import java.util.regex.Pattern;

import junit.framework.Assert;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import com.joliciel.talismane.tokeniser.Tokeniser;

public class TokenPatternImplTest {
	private static final Log LOG = LogFactory.getLog(TokenPatternImplTest.class);

	@Test
	public void testParsePattern() {
		String regexp = "(?![cdCD]\\z|qu\\z|jusqu\\z).+'.+";
		TokenPatternImpl tokenPattern = new TokenPatternImpl(regexp, Tokeniser.SEPARATORS);
		List<Pattern> patterns = tokenPattern.getParsedPattern();
		Assert.assertEquals(3, patterns.size());
		int i = 0;
		for (Pattern pattern : patterns) {
			if (i==0) {
				Assert.assertEquals("(?![cdCD]\\z|[qQ]u\\z|[jJ]usqu\\z).+", pattern.pattern());
			} else if (i==1) {
				Assert.assertEquals("'", pattern.pattern());
			} else if (i==2) {
				Assert.assertEquals(".+", pattern.pattern());
			}
			i++;
		}
		
		regexp = "être (de|d)";
		tokenPattern = new TokenPatternImpl(regexp, Tokeniser.SEPARATORS);
		patterns = tokenPattern.getParsedPattern();
		Assert.assertEquals(3, patterns.size());
		i = 0;
		for (Pattern pattern : patterns) {
			if (i==0) {
				Assert.assertEquals("[êÊE]tre", pattern.pattern());
			} else if (i==1) {
				Assert.assertEquals(" ", pattern.pattern());
			} else if (i==2) {
				Assert.assertEquals("(de|d)", pattern.pattern());
			}
			i++;
		}
		
		regexp = ".+\\.\\p";
		tokenPattern = new TokenPatternImpl(regexp, Tokeniser.SEPARATORS);
		patterns = tokenPattern.getParsedPattern();
		Assert.assertEquals(3, patterns.size());
		i = 0;
		for (Pattern pattern : patterns) {
			if (i==0) {
				Assert.assertEquals(".+", pattern.pattern());
			} else if (i==1) {
				Assert.assertEquals("\\.", pattern.pattern());
			} else if (i==2) {
				Assert.assertEquals(Tokeniser.SEPARATORS.pattern(), pattern.pattern());
			}
			i++;
		}
		
		regexp = ".+qu'";
		tokenPattern = new TokenPatternImpl(regexp, Tokeniser.SEPARATORS);
		patterns = tokenPattern.getParsedPattern();
		Assert.assertEquals(2, patterns.size());
		i = 0;
		for (Pattern pattern : patterns) {
			if (i==0) {
				Assert.assertEquals(".+qu", pattern.pattern());
			} else if (i==1) {
				Assert.assertEquals("'", pattern.pattern());
			}
			i++;
		}
		
		regexp = "\\D+\\.a[ \\)]c[abc]";
		tokenPattern = new TokenPatternImpl(regexp, Tokeniser.SEPARATORS);
		patterns = tokenPattern.getParsedPattern();
		LOG.debug(patterns);
		Assert.assertEquals(5, patterns.size());
		i = 0;
		for (Pattern pattern : patterns) {
			if (i==0) {
				Assert.assertEquals("\\D+", pattern.pattern());
			} else if (i==1) {
				Assert.assertEquals("\\.", pattern.pattern());
			} else if (i==2) {
				Assert.assertEquals("a", pattern.pattern());
			} else if (i==3) {
				Assert.assertEquals("[ \\)]", pattern.pattern());
			} else if (i==4) {
				Assert.assertEquals("c[abc]", pattern.pattern());
			}
			i++;
		}
	}

}
