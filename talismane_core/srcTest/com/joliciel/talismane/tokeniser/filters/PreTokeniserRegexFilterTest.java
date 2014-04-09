package com.joliciel.talismane.tokeniser.filters;

import static org.junit.Assert.*;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

public class PreTokeniserRegexFilterTest {
	private static final Log LOG = LogFactory.getLog(PreTokeniserRegexFilterTest.class);

	@Test
	public void testApply() {
		TokenFilterServiceInternal tokeniserFilterService = new TokenFilterServiceImpl();
		TokenRegexFilterImpl filter = new TokenRegexFilterImpl("\\b[\\w.%-]+@[-.\\w]+\\.[A-Za-z]{2,4}\\b", "Email");
		filter.setTokeniserFilterService(tokeniserFilterService);
		String text = "My address is joe.schmoe@test.com.";
		Set<TokenPlaceholder> placeholders = filter.apply(text);
		LOG.debug(placeholders);
		assertEquals(1, placeholders.size());
		TokenPlaceholder placeholder = placeholders.iterator().next();
		assertEquals(14, placeholder.getStartIndex());
		assertEquals(33, placeholder.getEndIndex());
		assertEquals("Email", placeholder.getReplacement());
	}
	
	@Test
	public void testApplyWithDollars() {
		TokenFilterServiceInternal tokeniserFilterService = new TokenFilterServiceImpl();
		TokenRegexFilterImpl filter = new TokenRegexFilterImpl("\\b([\\w.%-]+)(@[-.\\w]+\\.[A-Za-z]{2,4})\\b", "\\$Email$2:$1");
		filter.setTokeniserFilterService(tokeniserFilterService);
		String text = "My address is joe.schmoe@test.com.";
		Set<TokenPlaceholder> placeholders = filter.apply(text);
		LOG.debug(placeholders);
		assertEquals(1, placeholders.size());
		TokenPlaceholder placeholder = placeholders.iterator().next();
		assertEquals(14, placeholder.getStartIndex());
		assertEquals(33, placeholder.getEndIndex());
		assertEquals("$Email@test.com:joe.schmoe", placeholder.getReplacement());
	}

}
