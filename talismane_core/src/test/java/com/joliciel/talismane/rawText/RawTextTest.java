package com.joliciel.talismane.rawText;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.joliciel.talismane.AnnotatedText;
import com.joliciel.talismane.Annotation;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.rawText.RawTextMarker.RawTextNoSentenceBreakMarker;
import com.joliciel.talismane.rawText.RawTextMarker.RawTextReplaceMarker;
import com.joliciel.talismane.rawText.RawTextMarker.RawTextSentenceBreakMarker;
import com.joliciel.talismane.rawText.RawTextMarker.RawTextSkipMarker;
import com.joliciel.talismane.sentenceDetector.SentenceBoundary;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class RawTextTest {

  @Test
  public void testGetProcessedText() throws Exception {
    System.setProperty("config.file", "src/test/resources/test.conf");
    ConfigFactory.invalidateCaches();
    final Config config = ConfigFactory.load();

    final TalismaneSession session = new TalismaneSession(config, "test");

    String[] labels = new String[0];
    String text = "1 2 3<skip>skip</skip> 4<skip>skip</skip> five";
    RawText rawText = new RawText(text, true, session);

    List<Annotation<RawTextSkipMarker>> skips = new ArrayList<>();
    skips.add(new Annotation<>("1 2 3".length(), "1 2 3<skip>skip</skip>".length(), new RawTextSkipMarker("me"), labels));
    skips.add(new Annotation<>("1 2 3<skip>skip</skip> 4".length(), "1 2 3<skip>skip</skip> 4<skip>skip</skip>".length(), new RawTextSkipMarker("me"), labels));
    rawText.addAnnotations(skips);

    List<Annotation<RawTextReplaceMarker>> replaces = new ArrayList<>();
    replaces.add(new Annotation<>("1 2 3<skip>skip</skip> 4<skip>skip</skip> ".length(), "1 2 3<skip>skip</skip> 4<skip>skip</skip> five".length(),
        new RawTextReplaceMarker("me", "5"), labels));
    rawText.addAnnotations(replaces);

    AnnotatedText processedTextBlock = rawText.getProcessedText();

    assertEquals("1 2 3 4 5", processedTextBlock.getText());
  }

  @Test
  public void testGetDetectedSentences() throws Exception {
    System.setProperty("config.file", "src/test/resources/test.conf");
    ConfigFactory.invalidateCaches();
    final Config config = ConfigFactory.load();

    final TalismaneSession session = new TalismaneSession(config, "test");

    String[] labels = new String[0];
    String text = "Sentence 1<sent/>Sentence 2. Sentence 3. Sentence 4.";
    RawText textBlock = new RawText(text, true, session);

    // we add a sentence break annotation to the raw text (as if it was
    // added by a filter)
    System.out.println("we add a sentence break annotation (as if it was added by a filter)");
    List<Annotation<RawTextSentenceBreakMarker>> sentenceBreaks = new ArrayList<>();
    sentenceBreaks.add(new Annotation<>("Sentence 1".length(), "Sentence 1<sent/>".length(), new RawTextSentenceBreakMarker("me"), labels));
    textBlock.addAnnotations(sentenceBreaks);

    List<Annotation<RawTextSkipMarker>> skips = new ArrayList<>();
    skips.add(new Annotation<>("Sentence 1".length(), "Sentence 1<sent/>".length(), new RawTextSkipMarker("me"), labels));
    textBlock.addAnnotations(skips);

    System.out.println("textBlock text: " + textBlock.getText());
    System.out.println("textBlock annotations: " + textBlock.getAnnotations().toString());

    AnnotatedText processedTextBlock = textBlock.getProcessedText();
    assertEquals("Sentence 1 Sentence 2. Sentence 3. Sentence 4.", processedTextBlock.getText());

    // add sentence boundaries to the processed text (as if they were added
    // by a sentence detector)
    System.out.println("add sentence boundaries to the processed text (as if they were added by a sentence detector)");
    List<Annotation<SentenceBoundary>> sentenceBoundaries = new ArrayList<>();
    sentenceBoundaries.add(new Annotation<>("Sentence 1".length(), "Sentence 1 Sentence 2.".length(), new SentenceBoundary(), labels));
    sentenceBoundaries.add(new Annotation<>("Sentence 1 Sentence 2.".length(), "Sentence 1 Sentence 2. Sentence 3.".length(), new SentenceBoundary(), labels));
    processedTextBlock.addAnnotations(sentenceBoundaries);

    assertEquals("Sentence 1 Sentence 2. Sentence 3. Sentence 4.", processedTextBlock.getText());

    System.out.println("textBlock text: " + textBlock.getText());
    System.out.println("textBlock annotations: " + textBlock.getAnnotations().toString());

    // ensure that the sentence boundary annotations in the original text
    // are in the right place
    assertEquals("Sentence 1<sent/>Sentence 2. Sentence 3. Sentence 4.", textBlock.getText());
    sentenceBoundaries = textBlock.getAnnotations(SentenceBoundary.class);
    assertEquals(2, sentenceBoundaries.size());
    assertEquals("Sentence 1<sent/>".length(), sentenceBoundaries.get(0).getStart());
    assertEquals("Sentence 1<sent/>Sentence 2.".length(), sentenceBoundaries.get(0).getEnd());
    assertEquals("Sentence 1<sent/>Sentence 2.".length(), sentenceBoundaries.get(1).getStart());
    assertEquals("Sentence 1<sent/>Sentence 2. Sentence 3.".length(), sentenceBoundaries.get(1).getEnd());

    List<Sentence> sentences = textBlock.getDetectedSentences();
    System.out.println("sentences: " + sentences.toString());

    assertEquals(4, sentences.size());
    assertEquals("Sentence 1", sentences.get(0).getText());
    assertEquals("".length(), sentences.get(0).getOriginalIndex(0));

    assertEquals("Sentence 2.", sentences.get(1).getText());
    assertEquals("Sentence 1<sent/>".length(), sentences.get(1).getOriginalIndex(0));

    assertEquals("Sentence 3.", sentences.get(2).getText());
    assertEquals("Sentence 1<sent/>Sentence 2. ".length(), sentences.get(2).getOriginalIndex(0));

    assertEquals("Sentence 4.", sentences.get(3).getText());
    assertEquals("Sentence 1<sent/>Sentence 2. Sentence 3. ".length(), sentences.get(3).getOriginalIndex(0));

    // test that sentence annotations get added to the original raw text
    Sentence sentence4 = sentences.get(3);
    List<Annotation<String>> annotations = new ArrayList<>();
    annotations.add(new Annotation<String>("Sentence ".length(), "Sentence 4".length(), "four", labels));
    sentence4.addAnnotations(annotations);

    annotations = textBlock.getAnnotations(String.class);
    assertEquals(1, annotations.size());
    assertEquals("Sentence 1<sent/>Sentence 2. Sentence 3. Sentence ".length(), annotations.get(0).getStart());
    assertEquals("Sentence 1<sent/>Sentence 2. Sentence 3. Sentence 4".length(), annotations.get(0).getEnd());
  }

  @Test
  public void testNoSentenceAnnotationLocation() throws Exception {
    System.setProperty("config.file", "src/test/resources/test.conf");
    ConfigFactory.invalidateCaches();
    final Config config = ConfigFactory.load();

    final TalismaneSession session = new TalismaneSession(config, "test");

    String[] labels = new String[0];
    String text = "Mr. Jones and <skip/>Mrs. Smith.";
    RawText textBlock = new RawText(text, true, session);

    List<Annotation<RawTextNoSentenceBreakMarker>> noSentenceBreaks = new ArrayList<>();

    System.out.println("we add no sentence break annotations (as if they were added by a filter)");
    noSentenceBreaks.add(new Annotation<>("".length(), "Mr.".length(), new RawTextNoSentenceBreakMarker("me"), labels));
    noSentenceBreaks
        .add(new Annotation<>("Mr. Jones and <skip/>".length(), "Mr. Jones and <skip/>Mrs.".length(), new RawTextNoSentenceBreakMarker("me"), labels));
    textBlock.addAnnotations(noSentenceBreaks);

    List<Annotation<RawTextSkipMarker>> skips = new ArrayList<>();
    skips.add(new Annotation<>("Mr. Jones and ".length(), "Mr. Jones and <skip/>".length(), new RawTextSkipMarker("me"), labels));
    textBlock.addAnnotations(skips);

    System.out.println("textBlock text: " + textBlock.getText());
    System.out.println("textBlock annotations: " + textBlock.getAnnotations().toString());

    AnnotatedText processedTextBlock = textBlock.getProcessedText();
    assertEquals("Mr. Jones and Mrs. Smith.", processedTextBlock.getText());

    // ensure that the no sentence break text got added at the right place
    // in the processed text
    noSentenceBreaks = processedTextBlock.getAnnotations(RawTextNoSentenceBreakMarker.class);
    System.out.println("Processed annotations: " + noSentenceBreaks);

    assertEquals(2, noSentenceBreaks.size());
    assertEquals("".length(), noSentenceBreaks.get(0).getStart());
    assertEquals("Mr.".length(), noSentenceBreaks.get(0).getEnd());
    assertEquals("Mr. Jones and ".length(), noSentenceBreaks.get(1).getStart());
    assertEquals("Mr. Jones and Mrs.".length(), noSentenceBreaks.get(1).getEnd());

  }

}
