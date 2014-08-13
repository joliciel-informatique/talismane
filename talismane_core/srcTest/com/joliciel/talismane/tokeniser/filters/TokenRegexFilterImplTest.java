package com.joliciel.talismane.tokeniser.filters;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import mockit.NonStrict;
import mockit.NonStrictExpectations;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import com.joliciel.talismane.machineLearning.ExternalResourceFinder;
import com.joliciel.talismane.machineLearning.ExternalWordList;

public class TokenRegexFilterImplTest {
	private static final Log LOG = LogFactory.getLog(TokenRegexFilterImplTest.class);

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

	@Test
	public void testWordList(@NonStrict final ExternalResourceFinder externalResourceFinder,
			@NonStrict final ExternalWordList externalWordList) {
		
		TokenFilterServiceInternal tokeniserFilterService = new TokenFilterServiceImpl();
		TokenRegexFilterImpl filter = new TokenRegexFilterImpl("\\b(\\p{WordList(FirstNames)}) [A-Z]\\w+\\b");
		filter.setTokeniserFilterService(tokeniserFilterService);
		filter.setExternalResourceFinder(externalResourceFinder);
		
		final List<String> wordList = new ArrayList<String>();
		wordList.add("Chloé");
		wordList.add("Marcel");
		wordList.add("Joëlle");
		wordList.add("Édouard");
		
		new NonStrictExpectations() {
			{
				externalResourceFinder.getExternalWordList("FirstNames"); returns(externalWordList);
				externalWordList.getWordList(); returns(wordList);
			}
		};
		
		Pattern pattern = filter.getPattern();
		LOG.debug(pattern.pattern());
		
		assertEquals("\\b(Chloé|Marcel|Joëlle|Édouard) [A-Z]\\w+\\b", pattern.pattern());
	}
	
	@Test
	public void testWordListDiacriticsOptional(@NonStrict final ExternalResourceFinder externalResourceFinder,
			@NonStrict final ExternalWordList externalWordList) {
		
		TokenFilterServiceInternal tokeniserFilterService = new TokenFilterServiceImpl();
		TokenRegexFilterImpl filter = new TokenRegexFilterImpl("\\b(\\p{WordList(FirstNames,true)}) [A-Z]\\w+\\b");
		filter.setTokeniserFilterService(tokeniserFilterService);
		filter.setExternalResourceFinder(externalResourceFinder);
		
		final List<String> wordList = new ArrayList<String>();
		wordList.add("Chloé");
		wordList.add("Marcel");
		wordList.add("Joëlle");
		wordList.add("Édouard");
		
		new NonStrictExpectations() {
			{
				externalResourceFinder.getExternalWordList("FirstNames"); returns(externalWordList);
				externalWordList.getWordList(); returns(wordList);
			}
		};
		
		Pattern pattern = filter.getPattern();
		LOG.debug(pattern.pattern());
		
		assertEquals("\\b(Chlo[ée]|Marcel|Jo[ëe]lle|[ÉE]douard) [A-Z]\\w+\\b", pattern.pattern());
	}
	
	@Test
	public void testWordListUppercaseOptional(@NonStrict final ExternalResourceFinder externalResourceFinder,
			@NonStrict final ExternalWordList externalWordList) {
		
		TokenFilterServiceInternal tokeniserFilterService = new TokenFilterServiceImpl();
		TokenRegexFilterImpl filter = new TokenRegexFilterImpl("\\b(\\p{WordList(FirstNames,false,true)}) [A-Z]\\w+\\b");
		filter.setTokeniserFilterService(tokeniserFilterService);
		filter.setExternalResourceFinder(externalResourceFinder);
		
		final List<String> wordList = new ArrayList<String>();
		wordList.add("Chloé");
		wordList.add("Marcel");
		wordList.add("Joëlle");
		wordList.add("Édouard");
		
		new NonStrictExpectations() {
			{
				externalResourceFinder.getExternalWordList("FirstNames"); returns(externalWordList);
				externalWordList.getWordList(); returns(wordList);
			}
		};
		
		Pattern pattern = filter.getPattern();
		LOG.debug(pattern.pattern());
		
		assertEquals("\\b([Cc]hloé|[Mm]arcel|[Jj]oëlle|[Éé]douard) [A-Z]\\w+\\b", pattern.pattern());
	}
	
	
	@Test
	public void testWordListUppercaseDiacriticsOptional(@NonStrict final ExternalResourceFinder externalResourceFinder,
			@NonStrict final ExternalWordList externalWordList) {
		
		TokenFilterServiceInternal tokeniserFilterService = new TokenFilterServiceImpl();
		TokenRegexFilterImpl filter = new TokenRegexFilterImpl("\\b(\\p{WordList(FirstNames,true,true)}) [A-Z]\\w+\\b");
		filter.setTokeniserFilterService(tokeniserFilterService);
		filter.setExternalResourceFinder(externalResourceFinder);
		
		final List<String> wordList = new ArrayList<String>();
		wordList.add("Chloé");
		wordList.add("Marcel");
		wordList.add("Joëlle");
		wordList.add("Édouard");
		
		new NonStrictExpectations() {
			{
				externalResourceFinder.getExternalWordList("FirstNames"); returns(externalWordList);
				externalWordList.getWordList(); returns(wordList);
			}
		};
		
		Pattern pattern = filter.getPattern();
		LOG.debug(pattern.pattern());
		
		assertEquals("\\b([Cc]hlo[ée]|[Mm]arcel|[Jj]o[ëe]lle|[ÉéEe]douard) [A-Z]\\w+\\b", pattern.pattern());
	}
}
