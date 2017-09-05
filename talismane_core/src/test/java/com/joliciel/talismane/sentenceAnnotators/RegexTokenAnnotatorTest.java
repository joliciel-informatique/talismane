package com.joliciel.talismane.sentenceAnnotators;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.Annotation;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.rawText.Sentence;
import com.joliciel.talismane.resources.WordList;
import com.joliciel.talismane.tokeniser.StringAttribute;
import com.joliciel.talismane.tokeniser.TokenAttribute;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class RegexTokenAnnotatorTest {
  private static final Logger LOG = LoggerFactory.getLogger(RegexTokenAnnotatorTest.class);

  @Test
  public void testApply() throws Exception {
    System.setProperty("config.file", "src/test/resources/test.conf");
    ConfigFactory.invalidateCaches();
    final Config config = ConfigFactory.load();
    final TalismaneSession session = new TalismaneSession(config, "test");

    String regex = "\\b[\\w.%-]+@[-.\\w]+\\.[A-Za-z]{2,4}\\b";
    String replacement = "Email";
    RegexTokenAnnotator filter = new RegexTokenAnnotator(regex, replacement, null, session);

    Sentence text = new Sentence("My address is joe.schmoe@test.com.", session);
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
  public void testApplyWithDollars() throws Exception {
    System.setProperty("config.file", "src/test/resources/test.conf");
    ConfigFactory.invalidateCaches();
    final Config config = ConfigFactory.load();
    final TalismaneSession session = new TalismaneSession(config, "test");

    String regex = "\\b([\\w.%-]+)(@[-.\\w]+\\.[A-Za-z]{2,4})\\b";
    String replacement = "\\$Email$2:$1";
    RegexTokenAnnotator filter = new RegexTokenAnnotator(regex, replacement, null, session);

    Sentence text = new Sentence("My address is joe.schmoe@test.com.", session);
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
  public void testApplyWithConsecutiveDollars() throws Exception {
    System.setProperty("config.file", "src/test/resources/test.conf");
    ConfigFactory.invalidateCaches();
    final Config config = ConfigFactory.load();
    final TalismaneSession session = new TalismaneSession(config, "test");

    String regex = "\\b([\\w.%-]+)(@[-.\\w]+\\.[A-Za-z]{2,4})\\b";
    String replacement = "\\$Email$2$1";
    RegexTokenAnnotator filter = new RegexTokenAnnotator(regex, replacement, null, session);

    Sentence text = new Sentence("My address is joe.schmoe@test.com.", session);
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
  public void testApplyWithUnmatchingGroups() throws Exception {
    System.setProperty("config.file", "src/test/resources/test.conf");
    ConfigFactory.invalidateCaches();
    final Config config = ConfigFactory.load();
    final TalismaneSession session = new TalismaneSession(config, "test");

    String regex = "\\b(\\d)(\\d)?\\b";
    String replacement = "Number$1$2";
    RegexTokenAnnotator filter = new RegexTokenAnnotator(regex, replacement, null, session);

    Sentence text = new Sentence("Two-digit number: 42. One-digit number: 7.", session);
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
    final TalismaneSession session = new TalismaneSession(config, "test");

    final List<String> wordList = new ArrayList<String>();
    wordList.add("Chloé");
    wordList.add("Marcel");
    wordList.add("Joëlle");
    wordList.add("Édouard");

    WordList nameList = new WordList("FirstNames", wordList);
    session.getWordListFinder().addWordList(nameList);

    String regex = "\\b(\\p{WordList(FirstNames)}) [A-Z]\\w+\\b";
    String replacement = null;
    RegexTokenAnnotator filter = new RegexTokenAnnotator(regex, replacement, null, session);

    Pattern pattern = filter.getPattern();
    LOG.debug(pattern.pattern());

    assertEquals("\\b(Chloé|Marcel|Joëlle|Édouard) [A-Z]\\w+\\b", pattern.pattern());
  }

  @Test
  public void testWordListDiacriticsOptional() throws Exception {
    System.setProperty("config.file", "src/test/resources/test.conf");
    ConfigFactory.invalidateCaches();
    final Config config = ConfigFactory.load();
    final TalismaneSession session = new TalismaneSession(config, "test");

    final List<String> wordList = new ArrayList<String>();
    wordList.add("Chloé");
    wordList.add("Marcel");
    wordList.add("Joëlle");
    wordList.add("Édouard");

    WordList nameList = new WordList("FirstNames", wordList);
    session.getWordListFinder().addWordList(nameList);

    String regex = "\\b(\\p{WordList(FirstNames,diacriticsOptional)}) [A-Z]\\w+\\b";
    String replacement = null;
    RegexTokenAnnotator filter = new RegexTokenAnnotator(regex, replacement, null, session);

    Pattern pattern = filter.getPattern();
    LOG.debug(pattern.pattern());

    assertEquals("\\b(Chlo[ée]|Marcel|Jo[ëe]lle|[ÉE]douard) [A-Z]\\w+\\b", pattern.pattern());
  }

  @Test
  public void testWordListUppercaseOptional() throws Exception {
    System.setProperty("config.file", "src/test/resources/test.conf");
    ConfigFactory.invalidateCaches();
    final Config config = ConfigFactory.load();
    final TalismaneSession session = new TalismaneSession(config, "test");

    final List<String> wordList = new ArrayList<String>();
    wordList.add("Chloé");
    wordList.add("Marcel");
    wordList.add("Joëlle");
    wordList.add("Édouard");

    WordList nameList = new WordList("FirstNames", wordList);
    session.getWordListFinder().addWordList(nameList);

    String regex = "\\b(\\p{WordList(FirstNames,uppercaseOptional)}) [A-Z]\\w+\\b";
    String replacement = null;
    RegexTokenAnnotator filter = new RegexTokenAnnotator(regex, replacement, null, session);

    Pattern pattern = filter.getPattern();
    LOG.debug(pattern.pattern());

    assertEquals("\\b([Cc]hloé|[Mm]arcel|[Jj]oëlle|[Éé]douard) [A-Z]\\w+\\b", pattern.pattern());
  }

  @Test
  public void testWordListUppercaseDiacriticsOptional() throws Exception {
    System.setProperty("config.file", "src/test/resources/test.conf");
    ConfigFactory.invalidateCaches();
    final Config config = ConfigFactory.load();
    final TalismaneSession session = new TalismaneSession(config, "test");

    final List<String> wordList = new ArrayList<String>();
    wordList.add("Chloé");
    wordList.add("Marcel");
    wordList.add("Joëlle");
    wordList.add("Édouard");

    WordList nameList = new WordList("FirstNames", wordList);
    session.getWordListFinder().addWordList(nameList);

    String regex = "\\b(\\p{WordList(FirstNames,diacriticsOptional,uppercaseOptional)}) [A-Z]\\w+\\b";
    String replacement = null;
    RegexTokenAnnotator filter = new RegexTokenAnnotator(regex, replacement, null, session);

    Pattern pattern = filter.getPattern();
    LOG.debug(pattern.pattern());

    assertEquals("\\b([Cc]hlo[ée]|[Mm]arcel|[Jj]o[ëe]lle|[ÉéEe]douard) [A-Z]\\w+\\b", pattern.pattern());
  }

  @Test
  public void testAutoWordBoundaries() throws Exception {
    System.setProperty("config.file", "src/test/resources/test.conf");
    ConfigFactory.invalidateCaches();
    final Config config = ConfigFactory.load();
    final TalismaneSession session = new TalismaneSession(config, "test");

    String regex = "hello 123";
    String replacement = null;
    RegexTokenAnnotator filter = new RegexTokenAnnotator(regex, replacement, null, 0, true, true, true, session);

    Pattern pattern = filter.getPattern();
    LOG.debug(pattern.pattern());

    assertEquals("\\bhello 123\\b", pattern.pattern());

    regex = "\\sabc";
    filter = new RegexTokenAnnotator(regex, replacement, null, 0, true, true, true, session);

    pattern = filter.getPattern();
    LOG.debug(pattern.pattern());

    assertEquals("\\sabc\\b", pattern.pattern());

    regex = "\\bblah di blah\\b";
    filter = new RegexTokenAnnotator(regex, replacement, null, 0, true, true, true, session);

    pattern = filter.getPattern();
    LOG.debug(pattern.pattern());

    assertEquals("\\bblah di blah\\b", pattern.pattern());

    regex = "helloe?";
    filter = new RegexTokenAnnotator(regex, replacement, null, 0, true, true, true, session);

    pattern = filter.getPattern();
    LOG.debug(pattern.pattern());

    assertEquals("\\bhelloe?\\b", pattern.pattern());

    regex = "liste?s?";
    filter = new RegexTokenAnnotator(regex, replacement, null, 0, true, true, true, session);

    pattern = filter.getPattern();
    LOG.debug(pattern.pattern());

    assertEquals("\\bliste?s?\\b", pattern.pattern());

    regex = "lis#e?s?";
    filter = new RegexTokenAnnotator(regex, replacement, null, 0, true, true, true, session);

    pattern = filter.getPattern();
    LOG.debug(pattern.pattern());

    assertEquals("\\blis#e?s?", pattern.pattern());

    regex = "liste?\\d?";
    filter = new RegexTokenAnnotator(regex, replacement, null, 0, true, true, true, session);

    pattern = filter.getPattern();
    LOG.debug(pattern.pattern());

    assertEquals("\\bliste?\\d?\\b", pattern.pattern());

    regex = "liste?\\s?";
    filter = new RegexTokenAnnotator(regex, replacement, null, 0, true, true, true, session);

    pattern = filter.getPattern();
    LOG.debug(pattern.pattern());

    assertEquals("\\bliste?\\s?", pattern.pattern());

    regex = "a\\\\b";
    filter = new RegexTokenAnnotator(regex, replacement, null, 0, true, true, true, session);

    pattern = filter.getPattern();
    LOG.debug(pattern.pattern());

    assertEquals("\\ba\\\\b\\b", pattern.pattern());

    regex = "\\d+ \\D+";
    filter = new RegexTokenAnnotator(regex, replacement, null, 0, true, true, true, session);

    pattern = filter.getPattern();
    LOG.debug(pattern.pattern());

    assertEquals("\\b\\d+ \\D+", pattern.pattern());

    regex = "abc [A-Z]\\w+";
    filter = new RegexTokenAnnotator(regex, replacement, null, 0, true, true, true, session);

    pattern = filter.getPattern();
    LOG.debug(pattern.pattern());

    assertEquals("\\babc [A-Z]\\w+\\b", pattern.pattern());

    regex = "(MLLE\\.|Mlle\\.)";
    filter = new RegexTokenAnnotator(regex, replacement, null, 0, true, true, true, session);

    pattern = filter.getPattern();
    LOG.debug(pattern.pattern());

    assertEquals("\\b(MLLE\\.|Mlle\\.)", pattern.pattern());

    final List<String> wordList = new ArrayList<String>();
    wordList.add("Chloé");
    wordList.add("Marcel");

    WordList nameList = new WordList("FirstNames", wordList);
    session.getWordListFinder().addWordList(nameList);

    regex = "(\\p{WordList(FirstNames)})";
    filter = new RegexTokenAnnotator(regex, replacement, null, 0, true, true, true, session);

    pattern = filter.getPattern();
    LOG.debug(pattern.pattern());

    assertEquals("\\b(Chloé|Marcel)\\b", pattern.pattern());

    regex = "(\\p{WordList(FirstNames,diacriticsOptional)}) +([A-Z]'\\p{Alpha}+)";
    filter = new RegexTokenAnnotator(regex, replacement, null, 0, true, true, true, session);

    pattern = filter.getPattern();
    LOG.debug(pattern.pattern());

    assertEquals("\\b(Chlo[ée]|Marcel) +([A-Z]'\\p{Alpha}+)\\b", pattern.pattern());
  }

  @Test
  public void testCaseSensitive() throws Exception {
    System.setProperty("config.file", "src/test/resources/test.conf");
    ConfigFactory.invalidateCaches();
    final Config config = ConfigFactory.load();

    final TalismaneSession session = new TalismaneSession(config, "test");

    String regex = "hé";
    String replacement = null;
    RegexTokenAnnotator filter = new RegexTokenAnnotator(regex, replacement, null, 0, false, true, false, session);

    Pattern pattern = filter.getPattern();
    LOG.debug(pattern.pattern());

    assertEquals("[Hh][EÉé]", pattern.pattern());

    regex = "hé";
    filter = new RegexTokenAnnotator(regex, replacement, null, 0, true, false, false, session);

    pattern = filter.getPattern();
    LOG.debug(pattern.pattern());

    assertEquals("h[eé]", pattern.pattern());

    regex = "hé";
    filter = new RegexTokenAnnotator(regex, replacement, null, 0, false, false, false, session);

    pattern = filter.getPattern();
    LOG.debug(pattern.pattern());

    assertEquals("[Hh][EeÉé]", pattern.pattern());

    final List<String> wordList = new ArrayList<String>();
    wordList.add("apples");
    wordList.add("oranges");
    WordList fruitList = new WordList("Fruit", wordList);
    session.getWordListFinder().addWordList(fruitList);

    regex = "(\\p{WordList(Fruit)})hé\\w+\\b";
    filter = new RegexTokenAnnotator(regex, replacement, null, 0, false, false, false, session);

    pattern = filter.getPattern();
    LOG.debug(pattern.pattern());

    assertEquals("(apples|oranges)[Hh][EeÉé]\\w+\\b", pattern.pattern());
  }

  @Test
  public void testStartOfInput() throws Exception {
    System.setProperty("config.file", "src/test/resources/test.conf");
    ConfigFactory.invalidateCaches();
    final Config config = ConfigFactory.load();
    final TalismaneSession session = new TalismaneSession(config, "test");

    String regex = "^Résumé\\.";
    String replacement = null;
    RegexTokenAnnotator filter = new RegexTokenAnnotator(regex, replacement, null, session);

    filter.addAttribute("TAG", new StringAttribute("TAG", "skip"));
    Sentence text = new Sentence("Résumé. Résumé des attaques", session);
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

  @Test
  public void testPuctuation() throws Exception {
    System.setProperty("config.file", "src/test/resources/test.conf");
    ConfigFactory.invalidateCaches();
    final Config config = ConfigFactory.load();
    final TalismaneSession session = new TalismaneSession(config, "test");

    String regex = "[\\p{IsPunctuation}&&[^%$#@§¶‰‱]]+";
    String replacement = null;
    RegexTokenAnnotator filter = new RegexTokenAnnotator(regex, replacement, null, session);

    filter.addAttribute("featureType", new StringAttribute("featureType", "punctuation"));
    Sentence text = new Sentence("Bonjour. Comment ça va?", session);
    filter.annotate(text);
    @SuppressWarnings("rawtypes")
    List<Annotation<TokenAttribute>> annotations = text.getAnnotations(TokenAttribute.class);
    LOG.debug(annotations.toString());
    assertEquals(2, annotations.size());
    @SuppressWarnings("rawtypes")
    Annotation<TokenAttribute> placeholder = annotations.get(0);
    assertEquals("Bonjour".length(), placeholder.getStart());
    assertEquals("Bonjour.".length(), placeholder.getEnd());
    assertEquals("featureType", placeholder.getData().getKey());
    assertEquals("punctuation", placeholder.getData().getValue());

  }
}
