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

public class RollingTextBlockTest {

  @Test
  public void testRoll() throws Exception {
    System.setProperty("config.file", "src/test/resources/test.conf");
    ConfigFactory.invalidateCaches();
    final Config config = ConfigFactory.load();

    final TalismaneSession session = new TalismaneSession(config, "test");

    String[] labels = new String[0];

    RollingTextBlock textBlock = new RollingTextBlock(true, session);
    textBlock = textBlock.roll("One ");
    List<Annotation<String>> annotations = new ArrayList<>();
    annotations.add(new Annotation<String>(0, "One".length(), "1", labels));
    textBlock.addAnnotations(annotations);

    textBlock = textBlock.roll("Two ");
    annotations = new ArrayList<>();
    annotations.add(new Annotation<String>("One ".length(), "One Two".length(), "2", labels));
    textBlock.addAnnotations(annotations);

    textBlock = textBlock.roll("Three ");
    annotations = new ArrayList<>();
    annotations.add(new Annotation<String>("One Two ".length(), "One Two Three".length(), "3", labels));
    textBlock.addAnnotations(annotations);

    textBlock = textBlock.roll("Four ");
    annotations = new ArrayList<>();
    annotations.add(new Annotation<String>("One Two Three ".length(), "One Two Three Four".length(), "4", labels));
    textBlock.addAnnotations(annotations);

    textBlock = textBlock.roll("Five");

    assertEquals("Two Three Four Five", textBlock.getText());

    annotations = textBlock.getAnnotations(String.class);
    System.out.println(annotations.toString());

    assertEquals(3, annotations.size());
    int i = 0;

    // ensure annotations have been moved closer to start, since block 1 has
    // now been flushed
    for (Annotation<String> annotation : annotations) {
      if (i == 0) {
        assertEquals("2", annotation.getData());
        assertEquals(0, annotation.getStart());
        assertEquals("Two".length(), annotation.getEnd());
      } else if (i == 1) {
        assertEquals("3", annotation.getData());
        assertEquals("Two ".length(), annotation.getStart());
        assertEquals("Two Three".length(), annotation.getEnd());
      } else if (i == 2) {
        assertEquals("4", annotation.getData());
        assertEquals("Two Three ".length(), annotation.getStart());
        assertEquals("Two Three Four".length(), annotation.getEnd());
      }
      i++;
    }
  }

  @Test
  public void testGetRawTextBlock() throws Exception {
    System.setProperty("config.file", "src/test/resources/test.conf");
    ConfigFactory.invalidateCaches();
    final Config config = ConfigFactory.load();

    final TalismaneSession session = new TalismaneSession(config, "test");

    String[] labels = new String[0];

    RollingTextBlock textBlock = new RollingTextBlock(true, session);
    textBlock = textBlock.roll("1 ");
    textBlock = textBlock.roll("2 ");
    textBlock = textBlock.roll("3<skip>skip</skip> 4<sk");
    textBlock = textBlock.roll("ip>skip</skip> 5");

    // the rawTextBlock always contains the last two added sub-blocks
    // so annotations are relative to these sub-blocks
    AnnotatedText rawTextBlock = textBlock.getRawTextBlock();
    List<Annotation<RawTextSkipMarker>> skips = new ArrayList<>();
    skips.add(new Annotation<>("3".length(), "3<skip>skip</skip>".length(), new RawTextSkipMarker("me"), labels));
    skips.add(new Annotation<>("3<skip>skip</skip> 4".length(), "3<skip>skip</skip> 4<skip>skip</skip>".length(), new RawTextSkipMarker("me"), labels));
    rawTextBlock.addAnnotations(skips);

    List<Annotation<RawTextSkipMarker>> sourceSkips = textBlock.getAnnotations(RawTextSkipMarker.class);
    System.out.println(sourceSkips.toString());

    assertEquals(2, sourceSkips.size());
    int i = 0;

    // ensure annotations are placed correctly - they need to take into
    // account blocks 1 and 2
    for (Annotation<RawTextSkipMarker> skip : sourceSkips) {
      if (i == 0) {
        assertEquals("1 2 3".length(), skip.getStart());
        assertEquals("1 2 3<skip>skip</skip>".length(), skip.getEnd());
      } else if (i == 1) {
        assertEquals("1 2 3<skip>skip</skip> 4".length(), skip.getStart());
        assertEquals("1 2 3<skip>skip</skip> 4<skip>skip</skip>".length(), skip.getEnd());
      }
      i++;
    }
  }

  @Test
  public void testGetProcessedTextBlock() throws Exception {
    System.setProperty("config.file", "src/test/resources/test.conf");
    ConfigFactory.invalidateCaches();
    final Config config = ConfigFactory.load();

    final TalismaneSession session = new TalismaneSession(config, "test");

    String[] labels = new String[0];

    RollingTextBlock textBlock = new RollingTextBlock(true, session);
    textBlock = textBlock.roll("1 ");
    textBlock = textBlock.roll("2 ");
    textBlock = textBlock.roll("3<skip>skip</skip> 4<sk");
    textBlock = textBlock.roll("ip>skip</skip> five");

    // the rawTextBlock always contains the last two added sub-blocks
    // so annotations are relative to these sub-blocks
    AnnotatedText rawTextBlock = textBlock.getRawTextBlock();
    List<Annotation<RawTextSkipMarker>> skips = new ArrayList<>();
    skips.add(new Annotation<>("3".length(), "3<skip>skip</skip>".length(), new RawTextSkipMarker("me"), labels));
    skips.add(new Annotation<>("3<skip>skip</skip> 4".length(), "3<skip>skip</skip> 4<skip>skip</skip>".length(), new RawTextSkipMarker("me"), labels));
    rawTextBlock.addAnnotations(skips);

    textBlock = textBlock.roll(" 6");

    rawTextBlock = textBlock.getRawTextBlock();
    List<Annotation<RawTextReplaceMarker>> replaces = new ArrayList<>();
    replaces.add(new Annotation<>("ip>skip</skip> ".length(), "ip>skip</skip> five".length(), new RawTextReplaceMarker("me", "5"), labels));
    rawTextBlock.addAnnotations(replaces);

    AnnotatedText processedTextBlock = textBlock.getProcessedText();

    // the processed text always concerns sub-blocks 1, 2 and 3
    // at this point, sub-block 1 has already been flushed
    assertEquals("2 3 4 5", processedTextBlock.getText());
  }

  @Test
  public void testGetDetectedSentences() throws Exception {
    System.setProperty("config.file", "src/test/resources/test.conf");
    ConfigFactory.invalidateCaches();
    final Config config = ConfigFactory.load();

    final TalismaneSession session = new TalismaneSession(config, "test");

    String[] labels = new String[0];

    RollingTextBlock textBlock = new RollingTextBlock(true, session);
    textBlock = textBlock.roll("Sentence 1<sent/>Sentence 2. Sentence");
    textBlock = textBlock.roll(" 3.");

    // the rawTextBlock always contains the last two added sub-blocks
    // so annotations are relative to these sub-blocks
    AnnotatedText rawTextBlock = textBlock.getRawTextBlock();
    List<Annotation<RawTextSentenceBreakMarker>> sentenceBreaks = new ArrayList<>();

    // we add a sentence break annotation (as if it was added by a filter)
    System.out.println("we add a sentence break annotation (as if it was added by a filter)");
    sentenceBreaks.add(new Annotation<>("".length(), "Sentence 1<sent/>".length(), new RawTextSentenceBreakMarker("me"), labels));
    rawTextBlock.addAnnotations(sentenceBreaks);

    List<Annotation<RawTextSkipMarker>> skips = new ArrayList<>();
    skips.add(new Annotation<>("Sentence 1".length(), "Sentence 1<sent/>".length(), new RawTextSkipMarker("me"), labels));
    rawTextBlock.addAnnotations(skips);

    System.out.println("textBlock text: " + textBlock.getText());
    System.out.println("textBlock annotations: " + textBlock.getAnnotations().toString());

    textBlock = textBlock.roll(" Sentence 4.");

    AnnotatedText processedTextBlock = textBlock.getProcessedText();
    assertEquals("Sentence 1 Sentence 2. Sentence 3.", processedTextBlock.getText());

    // add sentence boundaries to the processed text (as if they were added
    // by a sentence detector)
    System.out.println("add sentence boundaries to the processed text (as if they were added by a sentence detector)");
    List<Annotation<SentenceBoundary>> sentenceBoundaries = new ArrayList<>();
    sentenceBoundaries.add(new Annotation<>("Sentence 1".length(), "Sentence 1 Sentence 2.".length(), new SentenceBoundary(), labels));
    processedTextBlock.addAnnotations(sentenceBoundaries);

    List<Sentence> sentences = textBlock.getDetectedSentences();
    System.out.println("sentences: " + sentences.toString());

    assertEquals(2, sentences.size());
    assertEquals("Sentence 1", sentences.get(0).getText());
    assertEquals("".length(), sentences.get(0).getOriginalIndex(0));
    assertEquals("Sentence 2.", sentences.get(1).getText());
    assertEquals("Sentence 1<sent/>".length(), sentences.get(1).getOriginalIndex(0));

    System.out.println("textBlock text: " + textBlock.getText());
    System.out.println("textBlock annotations: " + textBlock.getAnnotations().toString());

    textBlock = textBlock.roll("");

    // we have now rolled all text up until sentence 4 into the processed
    // area
    processedTextBlock = textBlock.getProcessedText();
    assertEquals("Sentence 1 Sentence 2. Sentence 3. Sentence 4.", processedTextBlock.getText());

    // add a sentence boundary for "Sentence 3"
    System.out.println("add a sentence boundary for \"Sentence 3\", this time inside the analysis range");
    sentenceBoundaries = new ArrayList<>();
    sentenceBoundaries.add(new Annotation<>("Sentence 1 Sentence 2.".length(), "Sentence 1 Sentence 2. Sentence 3.".length(), new SentenceBoundary(), labels));
    processedTextBlock.addAnnotations(sentenceBoundaries);

    sentences = textBlock.getDetectedSentences();
    System.out.println("sentences: " + sentences.toString());

    assertEquals(1, sentences.size());
    assertEquals("Sentence 3.", sentences.get(0).getText());
    assertEquals("Sentence 1<sent/>Sentence 2. ".length(), sentences.get(0).getOriginalIndex(0));

    System.out.println("textBlock text: " + textBlock.getText());
    System.out.println("textBlock annotations: " + textBlock.getAnnotations().toString());

    // ensure that the sentence boundary annotations in the original text
    // are in the right place
    assertEquals("Sentence 1<sent/>Sentence 2. Sentence 3. Sentence 4.", textBlock.getText());
    sentenceBoundaries = textBlock.getAnnotations(SentenceBoundary.class);
    System.out.println(sentenceBoundaries.toString());
    assertEquals(2, sentenceBoundaries.size());
    assertEquals("Sentence 1<sent/>".length(), sentenceBoundaries.get(0).getStart());
    assertEquals("Sentence 1<sent/>Sentence 2.".length(), sentenceBoundaries.get(0).getEnd());
    assertEquals("Sentence 1<sent/>Sentence 2.".length(), sentenceBoundaries.get(1).getStart());
    assertEquals("Sentence 1<sent/>Sentence 2. Sentence 3.".length(), sentenceBoundaries.get(1).getEnd());

    // roll in a final empty block - we now have an empty block at block 3,
    // so that any leftover in block 2 should be marked as complete
    // since sentences never overlap empty blocks.
    textBlock = textBlock.roll("");

    sentences = textBlock.getDetectedSentences();
    System.out.println("sentences: " + sentences.toString());
    assertEquals(1, sentences.size());
    assertEquals("Sentence 4.", sentences.get(0).getText());
    assertEquals("Sentence 1<sent/>Sentence 2. Sentence 3. ".length(), sentences.get(0).getOriginalIndex(0));

    // note: at this point the initial two blocks have been rolled out
    System.out.println("textBlock text: " + textBlock.getText());
    System.out.println("textBlock annotations: " + textBlock.getAnnotations().toString());

    // ensure that the sentence boundary annotations in the original text
    // are in the right place
    assertEquals(" 3. Sentence 4.", textBlock.getText());
    sentenceBoundaries = textBlock.getAnnotations(SentenceBoundary.class);
    assertEquals(1, sentenceBoundaries.size());
    assertEquals("".length(), sentenceBoundaries.get(0).getStart());
    assertEquals(" 3.".length(), sentenceBoundaries.get(0).getEnd());

    // test that sentence annotations get added to the original raw text
    Sentence sentence4 = sentences.get(0);
    List<Annotation<String>> annotations = new ArrayList<>();
    annotations.add(new Annotation<String>("Sentence ".length(), "Sentence 4".length(), "four", labels));
    sentence4.addAnnotations(annotations);

    System.out.println("textBlock text: " + textBlock.getText());
    System.out.println("textBlock annotations: " + textBlock.getAnnotations().toString());

    annotations = textBlock.getAnnotations(String.class);
    assertEquals(1, annotations.size());
    assertEquals(" 3. Sentence ".length(), annotations.get(0).getStart());
    assertEquals(" 3. Sentence 4".length(), annotations.get(0).getEnd());

    textBlock.getProcessedText();
  }

  @Test
  public void testNoSentenceAnnotationLocation() throws Exception {
    System.setProperty("config.file", "src/test/resources/test.conf");
    ConfigFactory.invalidateCaches();
    final Config config = ConfigFactory.load();

    final TalismaneSession session = new TalismaneSession(config, "test");

    String[] labels = new String[0];

    // String text = "I see Mr. Jones and <skip/>Mrs. Smith.";
    RollingTextBlock textBlock = new RollingTextBlock(true, session);
    textBlock = textBlock.roll("I see ");
    textBlock = textBlock.roll("Mr. Jones ");
    textBlock = textBlock.roll("and <sk");

    AnnotatedText rawText = textBlock.getRawTextBlock();
    System.out.println("rawText text: " + rawText.getText());

    List<Annotation<RawTextNoSentenceBreakMarker>> noSentenceBreaks = new ArrayList<>();

    System.out.println("we add no sentence break annotations (as if they were added by a filter)");
    noSentenceBreaks.add(new Annotation<>("".length(), "Mr.".length(), new RawTextNoSentenceBreakMarker("me"), labels));
    rawText.addAnnotations(noSentenceBreaks);

    System.out.println("textBlock text: " + textBlock.getText());
    System.out.println("textBlock annotations: " + textBlock.getAnnotations().toString());

    textBlock = textBlock.roll("ip/>Mrs.");

    rawText = textBlock.getRawTextBlock();
    System.out.println("rawText text: " + rawText.getText());
    List<Annotation<RawTextSkipMarker>> skips = new ArrayList<>();
    skips.add(new Annotation<>("and ".length(), "and <skip/>".length(), new RawTextSkipMarker("me"), labels));
    rawText.addAnnotations(skips);

    AnnotatedText processedTextBlock = textBlock.getProcessedText();
    assertEquals("I see Mr. Jones and ", processedTextBlock.getText());
    // ensure that the no sentence break text got added at the right place
    // in the processed text
    noSentenceBreaks = processedTextBlock.getAnnotations(RawTextNoSentenceBreakMarker.class);
    System.out.println("Processed annotations: " + noSentenceBreaks);

    assertEquals(1, noSentenceBreaks.size());
    assertEquals("I see ".length(), noSentenceBreaks.get(0).getStart());
    assertEquals("I see Mr.".length(), noSentenceBreaks.get(0).getEnd());

    textBlock = textBlock.roll(" Smith.");

    rawText = textBlock.getRawTextBlock();
    System.out.println("rawText text: " + rawText.getText());
    noSentenceBreaks = new ArrayList<>();
    noSentenceBreaks.add(new Annotation<>("ip/>".length(), "ip/>Mrs.".length(), new RawTextNoSentenceBreakMarker("me"), labels));
    rawText.addAnnotations(noSentenceBreaks);

    System.out.println("textBlock text: " + textBlock.getText());
    System.out.println("textBlock annotations: " + textBlock.getAnnotations().toString());

    textBlock = textBlock.roll("");

    System.out.println("textBlock text: " + textBlock.getText());
    System.out.println("textBlock annotations: " + textBlock.getAnnotations().toString());

    processedTextBlock = textBlock.getProcessedText();
    assertEquals("and Mrs. Smith.", processedTextBlock.getText());

    // ensure that the no sentence break text got added at the right place
    // in the processed text
    noSentenceBreaks = processedTextBlock.getAnnotations(RawTextNoSentenceBreakMarker.class);
    System.out.println("Processed annotations: " + noSentenceBreaks);

    assertEquals(1, noSentenceBreaks.size());
    assertEquals("and ".length(), noSentenceBreaks.get(0).getStart());
    assertEquals("and Mrs.".length(), noSentenceBreaks.get(0).getEnd());

  }
}
