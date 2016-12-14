package com.joliciel.talismane.sentenceAnnotators;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.AnnotatedText;
import com.joliciel.talismane.Annotation;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.resources.WordList;
import com.joliciel.talismane.sentenceAnnotators.AbstractRegexFilter;
import com.joliciel.talismane.sentenceAnnotators.TokenPlaceholder;
import com.joliciel.talismane.sentenceAnnotators.TokenRegexFilterImpl;
import com.joliciel.talismane.tokeniser.StringAttribute;
import com.joliciel.talismane.tokeniser.TokenAttribute;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class TokenRegexFilterImplTest {
	private static final Logger LOG = LoggerFactory.getLogger(TokenRegexFilterImplTest.class);

	@Before
	public void setup() {
	}

	@Test
	public void testApply() {
		TokenRegexFilterImpl filter = new TokenRegexFilterImpl();
		filter.setRegex("\\b[\\w.%-]+@[-.\\w]+\\.[A-Za-z]{2,4}\\b");
		filter.setReplacement("Email");
		AnnotatedText text = new AnnotatedText("My address is joe.schmoe@test.com.");
		filter.annotate(text);
		LOG.debug(text.getAnnotations().toString());
		List<Annotation<TokenPlaceholder>> placeholders = text.getAnnotations(TokenPlaceholder.class);
		assertEquals(1, placeholders.size());
		Annotation<TokenPlaceholder> placeholder = placeholders.get(0);
		assertEquals(14, placeholder.getStart());
		assertEquals(33, placeholder.getEnd());
		assertEquals("Email", placeholder.getData().getReplacement());
	}

	@Test
	public void testApplyWithDollars() {
		TokenRegexFilterImpl filter = new TokenRegexFilterImpl();
		filter.setRegex("\\b([\\w.%-]+)(@[-.\\w]+\\.[A-Za-z]{2,4})\\b");
		filter.setReplacement("\\$Email$2:$1");
		AnnotatedText text = new AnnotatedText("My address is joe.schmoe@test.com.");
		filter.annotate(text);
		List<Annotation<TokenPlaceholder>> placeholders = text.getAnnotations(TokenPlaceholder.class);

		LOG.debug(placeholders.toString());
		assertEquals(1, placeholders.size());
		Annotation<TokenPlaceholder> placeholder = placeholders.get(0);
		assertEquals(14, placeholder.getStart());
		assertEquals(33, placeholder.getEnd());
		assertEquals("$Email@test.com:joe.schmoe", placeholder.getData().getReplacement());

	}

	@Test
	public void testApplyWithConsecutiveDollars() {
		TokenRegexFilterImpl filter = new TokenRegexFilterImpl();
		filter.setRegex("\\b([\\w.%-]+)(@[-.\\w]+\\.[A-Za-z]{2,4})\\b");
		filter.setReplacement("\\$Email$2$1");
		AnnotatedText text = new AnnotatedText("My address is joe.schmoe@test.com.");
		filter.annotate(text);
		List<Annotation<TokenPlaceholder>> placeholders = text.getAnnotations(TokenPlaceholder.class);

		LOG.debug(placeholders.toString());
		assertEquals(1, placeholders.size());
		Annotation<TokenPlaceholder> placeholder = placeholders.get(0);
		assertEquals(14, placeholder.getStart());
		assertEquals(33, placeholder.getEnd());
		assertEquals("$Email@test.comjoe.schmoe", placeholder.getData().getReplacement());
	}

	@Test
	public void testApplyWithUnmatchingGroups() {
		TokenRegexFilterImpl filter = new TokenRegexFilterImpl();
		filter.setRegex("\\b(\\d)(\\d)?\\b");
		filter.setReplacement("Number$1$2");
		AnnotatedText text = new AnnotatedText("Two-digit number: 42. One-digit number: 7.");
		filter.annotate(text);
		List<Annotation<TokenPlaceholder>> placeholders = text.getAnnotations(TokenPlaceholder.class);
		LOG.debug(placeholders.toString());
		assertEquals(2, placeholders.size());

		Annotation<TokenPlaceholder> placeholder = placeholders.get(0);
		assertEquals("Two-digit number: ".length(), placeholder.getStart());
		assertEquals("Two-digit number: 42".length(), placeholder.getEnd());
		assertEquals("Number42", placeholder.getData().getReplacement());
		placeholder = placeholders.get(1);
		assertEquals("Two-digit number: 42. One-digit number: ".length(), placeholder.getStart());
		assertEquals("Two-digit number: 42. One-digit number: 7".length(), placeholder.getEnd());
		assertEquals("Number7", placeholder.getData().getReplacement());
	}

	@Test
	public void testWordList() throws Exception {
		System.setProperty("config.file", "src/test/resources/test.conf");
		ConfigFactory.invalidateCaches();
		final Config config = ConfigFactory.load();

		final TalismaneSession talismaneSession = new TalismaneSession(config, "");

		final List<String> wordList = new ArrayList<String>();
		wordList.add("Chloé");
		wordList.add("Marcel");
		wordList.add("Joëlle");
		wordList.add("Édouard");

		WordList nameList = new WordList("FirstNames", wordList);
		talismaneSession.getWordListFinder().addWordList(nameList);

		AbstractRegexFilter filter = new TokenRegexFilterImpl();
		filter.setTalismaneSession(talismaneSession);
		filter.setRegex("\\b(\\p{WordList(FirstNames)}) [A-Z]\\w+\\b");

		Pattern pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\b(Chloé|Marcel|Joëlle|Édouard) [A-Z]\\w+\\b", pattern.pattern());
	}

	@Test
	public void testWordListDiacriticsOptional() throws Exception {
		System.setProperty("config.file", "src/test/resources/test.conf");
		ConfigFactory.invalidateCaches();
		final Config config = ConfigFactory.load();

		final TalismaneSession talismaneSession = new TalismaneSession(config, "");

		final List<String> wordList = new ArrayList<String>();
		wordList.add("Chloé");
		wordList.add("Marcel");
		wordList.add("Joëlle");
		wordList.add("Édouard");

		WordList nameList = new WordList("FirstNames", wordList);
		talismaneSession.getWordListFinder().addWordList(nameList);

		AbstractRegexFilter filter = new TokenRegexFilterImpl();
		filter.setTalismaneSession(talismaneSession);
		filter.setRegex("\\b(\\p{WordList(FirstNames,diacriticsOptional)}) [A-Z]\\w+\\b");

		Pattern pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\b(Chlo[ée]|Marcel|Jo[ëe]lle|[ÉE]douard) [A-Z]\\w+\\b", pattern.pattern());
	}

	@Test
	public void testWordListUppercaseOptional() throws Exception {
		System.setProperty("config.file", "src/test/resources/test.conf");
		ConfigFactory.invalidateCaches();
		final Config config = ConfigFactory.load();

		final TalismaneSession talismaneSession = new TalismaneSession(config, "");

		final List<String> wordList = new ArrayList<String>();
		wordList.add("Chloé");
		wordList.add("Marcel");
		wordList.add("Joëlle");
		wordList.add("Édouard");

		WordList nameList = new WordList("FirstNames", wordList);
		talismaneSession.getWordListFinder().addWordList(nameList);

		AbstractRegexFilter filter = new TokenRegexFilterImpl();
		filter.setTalismaneSession(talismaneSession);
		filter.setRegex("\\b(\\p{WordList(FirstNames,uppercaseOptional)}) [A-Z]\\w+\\b");

		Pattern pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\b([Cc]hloé|[Mm]arcel|[Jj]oëlle|[Éé]douard) [A-Z]\\w+\\b", pattern.pattern());
	}

	@Test
	public void testWordListUppercaseDiacriticsOptional() throws Exception {
		System.setProperty("config.file", "src/test/resources/test.conf");
		ConfigFactory.invalidateCaches();
		final Config config = ConfigFactory.load();

		final TalismaneSession talismaneSession = new TalismaneSession(config, "");

		final List<String> wordList = new ArrayList<String>();
		wordList.add("Chloé");
		wordList.add("Marcel");
		wordList.add("Joëlle");
		wordList.add("Édouard");

		WordList nameList = new WordList("FirstNames", wordList);
		talismaneSession.getWordListFinder().addWordList(nameList);

		AbstractRegexFilter filter = new TokenRegexFilterImpl();
		filter.setTalismaneSession(talismaneSession);
		filter.setRegex("\\b(\\p{WordList(FirstNames,diacriticsOptional,uppercaseOptional)}) [A-Z]\\w+\\b");

		Pattern pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\b([Cc]hlo[ée]|[Mm]arcel|[Jj]o[ëe]lle|[ÉéEe]douard) [A-Z]\\w+\\b", pattern.pattern());
	}

	@Test
	public void testAutoWordBoundaries() throws Exception {
		System.setProperty("config.file", "src/test/resources/test.conf");
		ConfigFactory.invalidateCaches();
		final Config config = ConfigFactory.load();

		final TalismaneSession talismaneSession = new TalismaneSession(config, "");

		AbstractRegexFilter filter = new TokenRegexFilterImpl();
		filter.setRegex("hello 123");
		filter.setAutoWordBoundaries(true);

		Pattern pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\bhello 123\\b", pattern.pattern());

		filter = new TokenRegexFilterImpl();
		filter.setRegex("\\sabc");
		filter.setAutoWordBoundaries(true);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\sabc\\b", pattern.pattern());

		filter = new TokenRegexFilterImpl();
		filter.setRegex("\\bblah di blah\\b");
		filter.setAutoWordBoundaries(true);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\bblah di blah\\b", pattern.pattern());

		filter = new TokenRegexFilterImpl();
		filter.setRegex("helloe?");
		filter.setAutoWordBoundaries(true);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\bhelloe?\\b", pattern.pattern());

		filter = new TokenRegexFilterImpl();
		filter.setRegex("liste?s?");
		filter.setAutoWordBoundaries(true);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\bliste?s?\\b", pattern.pattern());

		filter = new TokenRegexFilterImpl();
		filter.setRegex("lis#e?s?");
		filter.setAutoWordBoundaries(true);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\blis#e?s?", pattern.pattern());

		filter = new TokenRegexFilterImpl();
		filter.setRegex("liste?\\d?");
		filter.setAutoWordBoundaries(true);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\bliste?\\d?\\b", pattern.pattern());

		filter = new TokenRegexFilterImpl();
		filter.setRegex("liste?\\s?");
		filter.setAutoWordBoundaries(true);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\bliste?\\s?", pattern.pattern());

		filter = new TokenRegexFilterImpl();
		filter.setRegex("a\\\\b");
		filter.setAutoWordBoundaries(true);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\ba\\\\b\\b", pattern.pattern());

		filter = new TokenRegexFilterImpl();
		filter.setRegex("\\d+ \\D+");
		filter.setAutoWordBoundaries(true);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\b\\d+ \\D+", pattern.pattern());

		filter = new TokenRegexFilterImpl();
		filter.setRegex("abc [A-Z]\\w+");
		filter.setAutoWordBoundaries(true);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\babc [A-Z]\\w+\\b", pattern.pattern());

		filter = new TokenRegexFilterImpl();
		filter.setRegex("(MLLE\\.|Mlle\\.)");
		filter.setAutoWordBoundaries(true);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\b(MLLE\\.|Mlle\\.)", pattern.pattern());

		final List<String> wordList = new ArrayList<String>();
		wordList.add("Chloé");
		wordList.add("Marcel");

		WordList nameList = new WordList("FirstNames", wordList);
		talismaneSession.getWordListFinder().addWordList(nameList);

		filter = new TokenRegexFilterImpl();
		filter.setTalismaneSession(talismaneSession);
		filter.setRegex("(\\p{WordList(FirstNames)})");
		filter.setAutoWordBoundaries(true);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\b(Chloé|Marcel)\\b", pattern.pattern());

		filter = new TokenRegexFilterImpl();
		filter.setRegex("(\\p{WordList(FirstNames,diacriticsOptional)}) +([A-Z]'\\p{Alpha}+)");
		filter.setTalismaneSession(talismaneSession);
		filter.setAutoWordBoundaries(true);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\b(Chlo[ée]|Marcel) +([A-Z]'\\p{Alpha}+)\\b", pattern.pattern());

	}

	@Test
	public void testCaseSensitive() throws Exception {
		System.setProperty("config.file", "src/test/resources/test.conf");
		ConfigFactory.invalidateCaches();
		final Config config = ConfigFactory.load();

		final TalismaneSession talismaneSession = new TalismaneSession(config, "");

		AbstractRegexFilter filter = new TokenRegexFilterImpl();
		filter.setRegex("hé");
		filter.setCaseSensitive(false);

		Pattern pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("[Hh][EÉé]", pattern.pattern());

		filter = new TokenRegexFilterImpl();
		filter.setRegex("hé");
		filter.setDiacriticSensitive(false);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("h[eé]", pattern.pattern());

		filter = new TokenRegexFilterImpl();
		filter.setRegex("hé");
		filter.setDiacriticSensitive(false);
		filter.setCaseSensitive(false);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("[Hh][EeÉé]", pattern.pattern());

		final List<String> wordList = new ArrayList<String>();
		wordList.add("apples");
		wordList.add("oranges");
		WordList fruitList = new WordList("Fruit", wordList);
		talismaneSession.getWordListFinder().addWordList(fruitList);

		filter = new TokenRegexFilterImpl();
		filter.setTalismaneSession(talismaneSession);
		filter.setRegex("(\\p{WordList(Fruit)})hé\\w+\\b");
		filter.setDiacriticSensitive(false);
		filter.setCaseSensitive(false);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("(apples|oranges)[Hh][EeÉé]\\w+\\b", pattern.pattern());

	}

	@Test
	public void testStartOfInput() {
		AbstractRegexFilter filter = new TokenRegexFilterImpl();
		filter.setRegex("^Résumé\\.");
		filter.addAttribute("TAG", new StringAttribute("TAG", "skip"));
		AnnotatedText text = new AnnotatedText("Résumé. Résumé des attaques");
		filter.annotate(text);
		@SuppressWarnings("rawtypes")
		List<Annotation<TokenAttribute>> annotations = text.getAnnotations(TokenAttribute.class);
		LOG.debug(annotations.toString());
		assertEquals(1, annotations.size());
		@SuppressWarnings("rawtypes")
		Annotation<TokenAttribute> placeholder = annotations.get(0);
		assertEquals(0, placeholder.getStart());
		assertEquals(7, placeholder.getEnd());
		assertEquals("TAG", placeholder.getData().getKey());
	}
}
