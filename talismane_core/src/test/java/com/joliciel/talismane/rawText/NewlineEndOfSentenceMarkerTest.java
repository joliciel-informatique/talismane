package com.joliciel.talismane.rawText;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.AnnotatedText;
import com.joliciel.talismane.Annotation;
import com.joliciel.talismane.rawText.NewlineEndOfSentenceMarker;
import com.joliciel.talismane.rawText.RawTextMarker.RawTextSentenceBreakMarker;
import com.joliciel.talismane.rawText.RawTextMarker.RawTextSkipMarker;

public class NewlineEndOfSentenceMarkerTest {
  private static final Logger LOG = LoggerFactory.getLogger(NewlineEndOfSentenceMarkerTest.class);

  @Test
  public void testApply() throws Exception {
    NewlineEndOfSentenceMarker filter = new NewlineEndOfSentenceMarker(1000);

    AnnotatedText text = new AnnotatedText("1\r\n2\r\n");

    filter.annotate(text);
    LOG.debug(text.getAnnotations().toString());

    List<Annotation<RawTextSentenceBreakMarker>> sentenceBreaks = text.getAnnotations(RawTextSentenceBreakMarker.class);
    assertEquals(2, sentenceBreaks.size());

    List<Annotation<RawTextSkipMarker>> skips = text.getAnnotations(RawTextSkipMarker.class);
    assertEquals(2, skips.size());

    assertEquals(1, sentenceBreaks.get(0).getStart());
    assertEquals(3, sentenceBreaks.get(0).getEnd());
    assertEquals(1, skips.get(0).getStart());
    assertEquals(3, skips.get(0).getEnd());
    assertEquals(4, sentenceBreaks.get(1).getStart());
    assertEquals(6, sentenceBreaks.get(1).getEnd());
    assertEquals(4, skips.get(1).getStart());
    assertEquals(6, skips.get(1).getEnd());

    text = new AnnotatedText("1\r2\r");

    filter.annotate(text);
    LOG.debug(text.getAnnotations().toString());

    sentenceBreaks = text.getAnnotations(RawTextSentenceBreakMarker.class);
    assertEquals(2, sentenceBreaks.size());

    skips = text.getAnnotations(RawTextSkipMarker.class);
    assertEquals(2, skips.size());

    assertEquals(1, sentenceBreaks.get(0).getStart());
    assertEquals(2, sentenceBreaks.get(0).getEnd());
    assertEquals(1, skips.get(0).getStart());
    assertEquals(2, skips.get(0).getEnd());
    assertEquals(3, sentenceBreaks.get(1).getStart());
    assertEquals(4, sentenceBreaks.get(1).getEnd());
    assertEquals(3, skips.get(1).getStart());
    assertEquals(4, skips.get(1).getEnd());

    text = new AnnotatedText("1\r2\r");

    filter.annotate(text);
    LOG.debug(text.getAnnotations().toString());

    sentenceBreaks = text.getAnnotations(RawTextSentenceBreakMarker.class);
    assertEquals(2, sentenceBreaks.size());

    skips = text.getAnnotations(RawTextSkipMarker.class);
    assertEquals(2, skips.size());

    assertEquals(1, sentenceBreaks.get(0).getStart());
    assertEquals(2, sentenceBreaks.get(0).getEnd());
    assertEquals(1, skips.get(0).getStart());
    assertEquals(2, skips.get(0).getEnd());
    assertEquals(3, sentenceBreaks.get(1).getStart());
    assertEquals(4, sentenceBreaks.get(1).getEnd());
    assertEquals(3, skips.get(1).getStart());
    assertEquals(4, skips.get(1).getEnd());
  }
}
