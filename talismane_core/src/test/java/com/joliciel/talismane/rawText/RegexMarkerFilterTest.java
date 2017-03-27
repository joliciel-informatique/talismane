package com.joliciel.talismane.rawText;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.AnnotatedText;
import com.joliciel.talismane.Annotation;
import com.joliciel.talismane.rawText.RawTextMarkType;
import com.joliciel.talismane.rawText.RawTextMarker;
import com.joliciel.talismane.rawText.RawTextRegexAnnotator;
import com.joliciel.talismane.rawText.RawTextMarker.RawTextReplaceMarker;
import com.joliciel.talismane.rawText.RawTextMarker.RawTextSkipMarker;
import com.joliciel.talismane.tokeniser.StringAttribute;

public class RegexMarkerFilterTest {
  private static final Logger LOG = LoggerFactory.getLogger(RegexMarkerFilterTest.class);

  @Test
  public void testApply() throws Exception {
    RawTextRegexAnnotator filter = new RawTextRegexAnnotator(RawTextMarkType.SKIP, "<skip>.*?</skip>", 0, 1000);

    AnnotatedText text = new AnnotatedText("J'ai du <skip>skip me</skip>mal à le croire.<skip>skip me</skip>");

    filter.annotate(text);
    LOG.debug(text.getAnnotations().toString());

    List<Annotation<RawTextSkipMarker>> skips = text.getAnnotations(RawTextSkipMarker.class);
    assertEquals(2, skips.size());

    int i = 0;
    for (Annotation<RawTextSkipMarker> skip : skips) {
      if (i == 0) {
        assertEquals("J'ai du ".length(), skip.getStart());
        assertEquals("J'ai du <skip>skip me</skip>".length(), skip.getEnd());
      } else if (i == 2) {
        assertEquals("J'ai du <skip>skip me</skip>mal à le croire.".length(), skip.getStart());
        assertEquals("J'ai du <skip>skip me</skip>mal à le croire.<skip>skip me</skip>".length(), skip.getEnd());
      }
      i++;
    }
  }

  @Test
  public void testApplyWithGroup() throws Exception {
    RawTextRegexAnnotator filter = new RawTextRegexAnnotator(RawTextMarkType.SKIP, "<skip>(.*?)</skip>", 1, 1000);

    AnnotatedText text = new AnnotatedText("J'ai du <skip>skip me</skip>mal à le croire.<skip>skip me</skip>");

    filter.annotate(text);
    LOG.debug(text.getAnnotations().toString());

    List<Annotation<RawTextSkipMarker>> skips = text.getAnnotations(RawTextSkipMarker.class);
    assertEquals(2, skips.size());

    int i = 0;
    for (Annotation<RawTextSkipMarker> skip : skips) {
      if (i == 0) {
        assertEquals("J'ai du <skip>".length(), skip.getStart());
        assertEquals("J'ai du <skip>skip me".length(), skip.getEnd());
      } else if (i == 2) {
        assertEquals("J'ai du <skip>skip me</skip>mal à le croire.<skip>".length(), skip.getStart());
        assertEquals("J'ai du <skip>skip me</skip>mal à le croire.<skip>skip me".length(), skip.getEnd());
      }
      i++;
    }
  }

  @Test
  public void testApplyWithReplacement() throws Exception {
    RawTextRegexAnnotator filter = new RawTextRegexAnnotator(RawTextMarkType.REPLACE, "<skip>(.*?)</skip>", 0, 1000);
    filter.setReplacement("Skipped:$1");

    AnnotatedText text = new AnnotatedText("J'ai du <skip>skip me</skip>mal à le croire.<skip>skip this</skip>");

    filter.annotate(text);
    LOG.debug(text.getAnnotations().toString());

    List<Annotation<RawTextReplaceMarker>> replaces = text.getAnnotations(RawTextReplaceMarker.class);
    assertEquals(2, replaces.size());

    int i = 0;
    for (Annotation<RawTextReplaceMarker> replace : replaces) {
      if (i == 0) {
        assertEquals("J'ai du ".length(), replace.getStart());
        assertEquals("J'ai du <skip>skip me</skip>".length(), replace.getEnd());
        assertEquals("Skipped:skip me", replace.getData().getInsertionText());
      } else if (i == 2) {
        assertEquals("J'ai du <skip>skip me</skip>mal à le croire.".length(), replace.getStart());
        assertEquals("J'ai du <skip>skip me</skip>mal à le croire.<skip>skip this</skip>".length(), replace.getEnd());
        assertEquals("Skipped:skip this", replace.getData().getInsertionText());
      }
      i++;
    }
  }

  @Test
  public void testTag() throws Exception {
    RawTextRegexAnnotator filter = new RawTextRegexAnnotator(RawTextMarkType.TAG, "<skip>(.*?)</skip>", 0, 1000);
    filter.setAttribute(new StringAttribute("TAG1", "x"));

    AnnotatedText text = new AnnotatedText("J'ai du <skip>skip me</skip>mal à le croire.<skip>skip this</skip>");

    filter.annotate(text);
    LOG.debug(text.getAnnotations().toString());

    List<Annotation<StringAttribute>> attributes = text.getAnnotations(StringAttribute.class);
    assertEquals(2, attributes.size());

    int i = 0;
    for (Annotation<StringAttribute> attribute : attributes) {
      if (i == 0) {
        assertEquals("J'ai du ".length(), attribute.getStart());
        assertEquals("J'ai du <skip>skip me</skip>".length(), attribute.getEnd());
        assertEquals("TAG1", attribute.getData().getKey());
        assertEquals("x", attribute.getData().getValue());
      } else if (i == 1) {
        assertEquals("J'ai du <skip>skip me</skip>mal à le croire.".length(), attribute.getStart());
        assertEquals("J'ai du <skip>skip me</skip>mal à le croire.<skip>skip this</skip>".length(), attribute.getEnd());
        assertEquals("TAG1", attribute.getData().getKey());
        assertEquals("x", attribute.getData().getValue());
      }
      i++;
    }
  }

  @Test
  public void testUnaryOperatorsStop() throws Exception {
    RawTextRegexAnnotator filter = new RawTextRegexAnnotator(RawTextMarkType.STOP, "<skip>", 0, 1000);

    AnnotatedText text = new AnnotatedText("J'ai du <skip>skip me</skip>mal à le croire.<skip>skip this</skip>");

    filter.annotate(text);
    LOG.debug(text.getAnnotations().toString());

    List<Annotation<RawTextMarker>> markers = text.getAnnotations(RawTextMarker.class);
    assertEquals(2, markers.size());

    int i = 0;
    for (Annotation<RawTextMarker> textMarker : markers) {
      if (i == 0) {
        assertEquals(RawTextMarkType.STOP, textMarker.getData().getType());
        assertEquals("J'ai du ".length(), textMarker.getStart());
        assertEquals("J'ai du <skip>".length(), textMarker.getEnd());
      } else if (i == 1) {
        assertEquals(RawTextMarkType.STOP, textMarker.getData().getType());
        assertEquals("J'ai du <skip>skip me</skip>mal à le croire.".length(), textMarker.getStart());
        assertEquals("J'ai du <skip>skip me</skip>mal à le croire.<skip>".length(), textMarker.getEnd());
      }
      i++;
    }
  }

  @Test
  public void testUnaryOperatorsStart() throws Exception {
    RawTextRegexAnnotator filter = new RawTextRegexAnnotator(RawTextMarkType.START, "<skip>", 0, 1000);

    AnnotatedText text = new AnnotatedText("J'ai du <skip>skip me</skip>mal à le croire.<skip>skip this</skip>");

    filter.annotate(text);
    LOG.debug(text.getAnnotations().toString());

    List<Annotation<RawTextMarker>> markers = text.getAnnotations(RawTextMarker.class);
    assertEquals(2, markers.size());

    int i = 0;
    for (Annotation<RawTextMarker> textMarker : markers) {
      if (i == 0) {
        assertEquals(RawTextMarkType.START, textMarker.getData().getType());
        assertEquals("J'ai du ".length(), textMarker.getStart());
        assertEquals("J'ai du <skip>".length(), textMarker.getEnd());
      } else if (i == 1) {
        assertEquals(RawTextMarkType.START, textMarker.getData().getType());
        assertEquals("J'ai du <skip>skip me</skip>mal à le croire.".length(), textMarker.getStart());
        assertEquals("J'ai du <skip>skip me</skip>mal à le croire.<skip>".length(), textMarker.getEnd());
      }
      i++;
    }
  }
}
