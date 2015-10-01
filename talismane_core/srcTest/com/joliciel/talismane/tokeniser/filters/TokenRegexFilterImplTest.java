package com.joliciel.talismane.tokeniser.filters;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
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
		TokenRegexFilterImpl filter = new TokenRegexFilterImpl("\\b[\\w.%-]+@[-.\\w]+\\.[A-Za-z]{2,4}\\b");
		filter.setReplacement("Email");
		filter.setTokeniserFilterService(tokeniserFilterService);
		String text = "My address is joe.schmoe@test.com.";
		List<TokenPlaceholder> placeholders = filter.apply(text);
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
		TokenRegexFilterImpl filter = new TokenRegexFilterImpl("\\b([\\w.%-]+)(@[-.\\w]+\\.[A-Za-z]{2,4})\\b");
		filter.setReplacement("\\$Email$2:$1");
		filter.setTokeniserFilterService(tokeniserFilterService);
		String text = "My address is joe.schmoe@test.com.";
		List<TokenPlaceholder> placeholders = filter.apply(text);
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
		TokenRegexFilterImpl filter = new TokenRegexFilterImpl("\\b(\\p{WordList(FirstNames,diacriticsOptional)}) [A-Z]\\w+\\b");
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
		TokenRegexFilterImpl filter = new TokenRegexFilterImpl("\\b(\\p{WordList(FirstNames,uppercaseOptional)}) [A-Z]\\w+\\b");
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
		TokenRegexFilterImpl filter = new TokenRegexFilterImpl("\\b(\\p{WordList(FirstNames,diacriticsOptional,uppercaseOptional)}) [A-Z]\\w+\\b");
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
	
	@Test
	public void testAutoWordBoundaries(@NonStrict final ExternalResourceFinder externalResourceFinder,
			@NonStrict final ExternalWordList externalWordList) {
		
		TokenFilterServiceInternal tokeniserFilterService = new TokenFilterServiceImpl();
		
		TokenRegexFilterImpl filter = new TokenRegexFilterImpl("hello 123");
		filter.setTokeniserFilterService(tokeniserFilterService);
		filter.setExternalResourceFinder(externalResourceFinder);
		filter.setAutoWordBoundaries(true);
		
		Pattern pattern = filter.getPattern();
		LOG.debug(pattern.pattern());
		
		assertEquals("\\bhello 123\\b", pattern.pattern());
		
		filter = new TokenRegexFilterImpl("\\sabc");
		filter.setTokeniserFilterService(tokeniserFilterService);
		filter.setExternalResourceFinder(externalResourceFinder);
		filter.setAutoWordBoundaries(true);
		
		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());
		
		assertEquals("\\sabc\\b", pattern.pattern());

		filter = new TokenRegexFilterImpl("\\bblah di blah\\b");
		filter.setTokeniserFilterService(tokeniserFilterService);
		filter.setExternalResourceFinder(externalResourceFinder);
		filter.setAutoWordBoundaries(true);
		
		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());
		
		assertEquals("\\bblah di blah\\b", pattern.pattern());

		filter = new TokenRegexFilterImpl("helloe?");
		filter.setTokeniserFilterService(tokeniserFilterService);
		filter.setExternalResourceFinder(externalResourceFinder);
		filter.setAutoWordBoundaries(true);
		
		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());
		
		assertEquals("\\bhelloe?\\b", pattern.pattern());

		filter = new TokenRegexFilterImpl("liste?s?");
		filter.setTokeniserFilterService(tokeniserFilterService);
		filter.setExternalResourceFinder(externalResourceFinder);
		filter.setAutoWordBoundaries(true);
		
		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());
		
		assertEquals("\\bliste?s?\\b", pattern.pattern());
		
		filter = new TokenRegexFilterImpl("lis#e?s?");
		filter.setTokeniserFilterService(tokeniserFilterService);
		filter.setExternalResourceFinder(externalResourceFinder);
		filter.setAutoWordBoundaries(true);
		
		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());
		
		assertEquals("\\blis#e?s?", pattern.pattern());

		filter = new TokenRegexFilterImpl("liste?\\d?");
		filter.setTokeniserFilterService(tokeniserFilterService);
		filter.setExternalResourceFinder(externalResourceFinder);
		filter.setAutoWordBoundaries(true);
		
		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());
		
		assertEquals("\\bliste?\\d?\\b", pattern.pattern());

		filter = new TokenRegexFilterImpl("liste?\\s?");
		filter.setTokeniserFilterService(tokeniserFilterService);
		filter.setExternalResourceFinder(externalResourceFinder);
		filter.setAutoWordBoundaries(true);
		
		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());
		
		assertEquals("\\bliste?\\s?", pattern.pattern());

		filter = new TokenRegexFilterImpl("a\\\\b");
		filter.setTokeniserFilterService(tokeniserFilterService);
		filter.setExternalResourceFinder(externalResourceFinder);
		filter.setAutoWordBoundaries(true);
		
		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());
		
		assertEquals("\\ba\\\\b\\b", pattern.pattern());

		filter = new TokenRegexFilterImpl("\\d+ \\D+");
		filter.setTokeniserFilterService(tokeniserFilterService);
		filter.setExternalResourceFinder(externalResourceFinder);
		filter.setAutoWordBoundaries(true);
		
		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());
		
		assertEquals("\\b\\d+ \\D+", pattern.pattern());

		filter = new TokenRegexFilterImpl("abc [A-Z]\\w+");
		filter.setTokeniserFilterService(tokeniserFilterService);
		filter.setExternalResourceFinder(externalResourceFinder);
		filter.setAutoWordBoundaries(true);
		
		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());
		
		assertEquals("\\babc [A-Z]\\w+\\b", pattern.pattern());
		
		filter = new TokenRegexFilterImpl("(MLLE\\.|Mlle\\.)");
		filter.setTokeniserFilterService(tokeniserFilterService);
		filter.setExternalResourceFinder(externalResourceFinder);
		filter.setAutoWordBoundaries(true);
		
		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());
		
		assertEquals("\\b(MLLE\\.|Mlle\\.)", pattern.pattern());
		
		final List<String> wordList = new ArrayList<String>();
		wordList.add("Chloé");
		wordList.add("Marcel");
		
		new NonStrictExpectations() {
			{
				externalResourceFinder.getExternalWordList("FirstNames"); returns(externalWordList);
				externalWordList.getWordList(); returns(wordList);
			}
		};
		
		filter = new TokenRegexFilterImpl("(\\p{WordList(FirstNames)})");
		filter.setTokeniserFilterService(tokeniserFilterService);
		filter.setExternalResourceFinder(externalResourceFinder);
		filter.setAutoWordBoundaries(true);
		
		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());
		
		assertEquals("\\b(Chloé|Marcel)\\b", pattern.pattern());
		
		                                                  
		filter = new TokenRegexFilterImpl("(\\p{WordList(FirstNames,diacriticsOptional)}) +([A-Z]'\\p{Alpha}+)");
		filter.setTokeniserFilterService(tokeniserFilterService);
		filter.setExternalResourceFinder(externalResourceFinder);
		filter.setAutoWordBoundaries(true);
		
		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());
		
		assertEquals("\\b(Chlo[ée]|Marcel) +([A-Z]'\\p{Alpha}+)\\b", pattern.pattern());

	}
	
	@Test
	public void testCaseSensitive(@NonStrict final ExternalResourceFinder externalResourceFinder,
			@NonStrict final ExternalWordList externalWordList) {
		TokenFilterServiceInternal tokeniserFilterService = new TokenFilterServiceImpl();
		
		TokenRegexFilterImpl filter = new TokenRegexFilterImpl("hé");
		filter.setTokeniserFilterService(tokeniserFilterService);
		filter.setCaseSensitive(false);
		
		Pattern pattern = filter.getPattern();
		LOG.debug(pattern.pattern());
		
		assertEquals("[Hh][EÉé]", pattern.pattern());
		
		filter = new TokenRegexFilterImpl("hé");
		filter.setTokeniserFilterService(tokeniserFilterService);
		filter.setDiacriticSensitive(false);
		
		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());
		
		assertEquals("h[eé]", pattern.pattern());

		filter = new TokenRegexFilterImpl("hé");
		filter.setTokeniserFilterService(tokeniserFilterService);
		filter.setDiacriticSensitive(false);
		filter.setCaseSensitive(false);
		
		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());
		
		assertEquals("[Hh][EeÉé]", pattern.pattern());

		final List<String> wordList = new ArrayList<String>();
		wordList.add("apples");
		wordList.add("oranges");
		
		new NonStrictExpectations() {
			{
				externalResourceFinder.getExternalWordList("Fruit"); returns(externalWordList);
				externalWordList.getWordList(); returns(wordList);
			}
		};
		
		filter = new TokenRegexFilterImpl("(\\p{WordList(Fruit)})hé\\w+\\b");
		filter.setTokeniserFilterService(tokeniserFilterService);
		filter.setExternalResourceFinder(externalResourceFinder);
		filter.setDiacriticSensitive(false);
		filter.setCaseSensitive(false);
		
		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());
		
		assertEquals("(apples|oranges)[Hh][EeÉé]\\w+\\b", pattern.pattern());

	}
}
