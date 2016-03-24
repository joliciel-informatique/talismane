package com.joliciel.talismane.tokeniser.filters;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;

import com.joliciel.talismane.TalismaneServiceLocator;
import com.joliciel.talismane.machineLearning.ExternalResourceFinder;
import com.joliciel.talismane.machineLearning.ExternalWordList;
import com.joliciel.talismane.tokeniser.StringAttribute;

import mockit.NonStrict;
import mockit.NonStrictExpectations;

public class TokenRegexFilterImplTest {
	private static final Log LOG = LogFactory.getLog(TokenRegexFilterImplTest.class);

	@Before
	public void setup() {
		TalismaneServiceLocator.purgeInstance("");
	}

	@Test
	public void testApply() {
		TokenRegexFilterWithReplacement filter = new TokenRegexFilterWithReplacement();
		filter.setRegex("\\b[\\w.%-]+@[-.\\w]+\\.[A-Za-z]{2,4}\\b");
		filter.setReplacement("Email");
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
		TokenRegexFilterWithReplacement filter = new TokenRegexFilterWithReplacement();
		filter.setRegex("\\b([\\w.%-]+)(@[-.\\w]+\\.[A-Za-z]{2,4})\\b");
		filter.setReplacement("\\$Email$2:$1");
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
	public void testWordList(@NonStrict final ExternalResourceFinder externalResourceFinder, @NonStrict final ExternalWordList externalWordList) {

		AbstractRegexFilter filter = new TokenRegexFilterWithReplacement();
		filter.setRegex("\\b(\\p{WordList(FirstNames)}) [A-Z]\\w+\\b");
		filter.setExternalResourceFinder(externalResourceFinder);

		final List<String> wordList = new ArrayList<String>();
		wordList.add("Chloé");
		wordList.add("Marcel");
		wordList.add("Joëlle");
		wordList.add("Édouard");

		new NonStrictExpectations() {
			{
				externalResourceFinder.getExternalWordList("FirstNames");
				returns(externalWordList);
				externalWordList.getWordList();
				returns(wordList);
			}
		};

		Pattern pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\b(Chloé|Marcel|Joëlle|Édouard) [A-Z]\\w+\\b", pattern.pattern());
	}

	@Test
	public void testWordListDiacriticsOptional(@NonStrict final ExternalResourceFinder externalResourceFinder,
			@NonStrict final ExternalWordList externalWordList) {
		AbstractRegexFilter filter = new TokenRegexFilterWithReplacement();
		filter.setRegex("\\b(\\p{WordList(FirstNames,diacriticsOptional)}) [A-Z]\\w+\\b");
		filter.setExternalResourceFinder(externalResourceFinder);

		final List<String> wordList = new ArrayList<String>();
		wordList.add("Chloé");
		wordList.add("Marcel");
		wordList.add("Joëlle");
		wordList.add("Édouard");

		new NonStrictExpectations() {
			{
				externalResourceFinder.getExternalWordList("FirstNames");
				returns(externalWordList);
				externalWordList.getWordList();
				returns(wordList);
			}
		};

		Pattern pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\b(Chlo[ée]|Marcel|Jo[ëe]lle|[ÉE]douard) [A-Z]\\w+\\b", pattern.pattern());
	}

	@Test
	public void testWordListUppercaseOptional(@NonStrict final ExternalResourceFinder externalResourceFinder,
			@NonStrict final ExternalWordList externalWordList) {

		AbstractRegexFilter filter = new TokenRegexFilterWithReplacement();
		filter.setRegex("\\b(\\p{WordList(FirstNames,uppercaseOptional)}) [A-Z]\\w+\\b");
		filter.setExternalResourceFinder(externalResourceFinder);

		final List<String> wordList = new ArrayList<String>();
		wordList.add("Chloé");
		wordList.add("Marcel");
		wordList.add("Joëlle");
		wordList.add("Édouard");

		new NonStrictExpectations() {
			{
				externalResourceFinder.getExternalWordList("FirstNames");
				returns(externalWordList);
				externalWordList.getWordList();
				returns(wordList);
			}
		};

		Pattern pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\b([Cc]hloé|[Mm]arcel|[Jj]oëlle|[Éé]douard) [A-Z]\\w+\\b", pattern.pattern());
	}

	@Test
	public void testWordListUppercaseDiacriticsOptional(@NonStrict final ExternalResourceFinder externalResourceFinder,
			@NonStrict final ExternalWordList externalWordList) {

		AbstractRegexFilter filter = new TokenRegexFilterWithReplacement();
		filter.setRegex("\\b(\\p{WordList(FirstNames,diacriticsOptional,uppercaseOptional)}) [A-Z]\\w+\\b");
		filter.setExternalResourceFinder(externalResourceFinder);

		final List<String> wordList = new ArrayList<String>();
		wordList.add("Chloé");
		wordList.add("Marcel");
		wordList.add("Joëlle");
		wordList.add("Édouard");

		new NonStrictExpectations() {
			{
				externalResourceFinder.getExternalWordList("FirstNames");
				returns(externalWordList);
				externalWordList.getWordList();
				returns(wordList);
			}
		};

		Pattern pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\b([Cc]hlo[ée]|[Mm]arcel|[Jj]o[ëe]lle|[ÉéEe]douard) [A-Z]\\w+\\b", pattern.pattern());
	}

	@Test
	public void testAutoWordBoundaries(@NonStrict final ExternalResourceFinder externalResourceFinder, @NonStrict final ExternalWordList externalWordList) {

		AbstractRegexFilter filter = new TokenRegexFilterWithReplacement();
		filter.setRegex("hello 123");
		filter.setExternalResourceFinder(externalResourceFinder);
		filter.setAutoWordBoundaries(true);

		Pattern pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\bhello 123\\b", pattern.pattern());

		filter = new TokenRegexFilterWithReplacement();
		filter.setRegex("\\sabc");
		filter.setExternalResourceFinder(externalResourceFinder);
		filter.setAutoWordBoundaries(true);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\sabc\\b", pattern.pattern());

		filter = new TokenRegexFilterWithReplacement();
		filter.setRegex("\\bblah di blah\\b");
		filter.setExternalResourceFinder(externalResourceFinder);
		filter.setAutoWordBoundaries(true);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\bblah di blah\\b", pattern.pattern());

		filter = new TokenRegexFilterWithReplacement();
		filter.setRegex("helloe?");
		filter.setExternalResourceFinder(externalResourceFinder);
		filter.setAutoWordBoundaries(true);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\bhelloe?\\b", pattern.pattern());

		filter = new TokenRegexFilterWithReplacement();
		filter.setRegex("liste?s?");
		filter.setExternalResourceFinder(externalResourceFinder);
		filter.setAutoWordBoundaries(true);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\bliste?s?\\b", pattern.pattern());

		filter = new TokenRegexFilterWithReplacement();
		filter.setRegex("lis#e?s?");
		filter.setExternalResourceFinder(externalResourceFinder);
		filter.setAutoWordBoundaries(true);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\blis#e?s?", pattern.pattern());

		filter = new TokenRegexFilterWithReplacement();
		filter.setRegex("liste?\\d?");
		filter.setExternalResourceFinder(externalResourceFinder);
		filter.setAutoWordBoundaries(true);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\bliste?\\d?\\b", pattern.pattern());

		filter = new TokenRegexFilterWithReplacement();
		filter.setRegex("liste?\\s?");
		filter.setExternalResourceFinder(externalResourceFinder);
		filter.setAutoWordBoundaries(true);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\bliste?\\s?", pattern.pattern());

		filter = new TokenRegexFilterWithReplacement();
		filter.setRegex("a\\\\b");
		filter.setExternalResourceFinder(externalResourceFinder);
		filter.setAutoWordBoundaries(true);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\ba\\\\b\\b", pattern.pattern());

		filter = new TokenRegexFilterWithReplacement();
		filter.setRegex("\\d+ \\D+");
		filter.setExternalResourceFinder(externalResourceFinder);
		filter.setAutoWordBoundaries(true);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\b\\d+ \\D+", pattern.pattern());

		filter = new TokenRegexFilterWithReplacement();
		filter.setRegex("abc [A-Z]\\w+");
		filter.setExternalResourceFinder(externalResourceFinder);
		filter.setAutoWordBoundaries(true);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\babc [A-Z]\\w+\\b", pattern.pattern());

		filter = new TokenRegexFilterWithReplacement();
		filter.setRegex("(MLLE\\.|Mlle\\.)");
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
				externalResourceFinder.getExternalWordList("FirstNames");
				returns(externalWordList);
				externalWordList.getWordList();
				returns(wordList);
			}
		};

		filter = new TokenRegexFilterWithReplacement();
		filter.setRegex("(\\p{WordList(FirstNames)})");
		filter.setExternalResourceFinder(externalResourceFinder);
		filter.setAutoWordBoundaries(true);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\b(Chloé|Marcel)\\b", pattern.pattern());

		filter = new TokenRegexFilterWithReplacement();
		filter.setRegex("(\\p{WordList(FirstNames,diacriticsOptional)}) +([A-Z]'\\p{Alpha}+)");
		filter.setExternalResourceFinder(externalResourceFinder);
		filter.setAutoWordBoundaries(true);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\b(Chlo[ée]|Marcel) +([A-Z]'\\p{Alpha}+)\\b", pattern.pattern());

	}

	@Test
	public void testCaseSensitive(@NonStrict final ExternalResourceFinder externalResourceFinder, @NonStrict final ExternalWordList externalWordList) {

		AbstractRegexFilter filter = new TokenRegexFilterWithReplacement();
		filter.setRegex("hé");
		filter.setCaseSensitive(false);

		Pattern pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("[Hh][EÉé]", pattern.pattern());

		filter = new TokenRegexFilterWithReplacement();
		filter.setRegex("hé");
		filter.setDiacriticSensitive(false);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("h[eé]", pattern.pattern());

		filter = new TokenRegexFilterWithReplacement();
		filter.setRegex("hé");
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
				externalResourceFinder.getExternalWordList("Fruit");
				returns(externalWordList);
				externalWordList.getWordList();
				returns(wordList);
			}
		};

		filter = new TokenRegexFilterWithReplacement();
		filter.setRegex("(\\p{WordList(Fruit)})hé\\w+\\b");
		filter.setExternalResourceFinder(externalResourceFinder);
		filter.setDiacriticSensitive(false);
		filter.setCaseSensitive(false);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("(apples|oranges)[Hh][EeÉé]\\w+\\b", pattern.pattern());

	}

	@Test
	public void testStartOfInput() {
		AbstractRegexFilter filter = new TokenRegexFilterWithReplacement();
		filter.setRegex("^Résumé\\.");
		filter.addAttribute("TAG", new StringAttribute("skip"));
		String text = "Résumé. Résumé des attaques";
		List<TokenPlaceholder> placeholders = filter.apply(text);
		LOG.debug(placeholders);
		assertEquals(1, placeholders.size());
		TokenPlaceholder placeholder = placeholders.iterator().next();
		assertEquals(0, placeholder.getStartIndex());
		assertEquals(7, placeholder.getEndIndex());
		assertEquals(1, placeholder.getAttributes().size());
		assertEquals("TAG", placeholder.getAttributes().keySet().iterator().next());
	}
}
