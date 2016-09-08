package com.joliciel.talismane.filters;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.tokeniser.StringAttribute;

public class RegexMarkerFilterTest {
	private static final Logger LOG = LoggerFactory.getLogger(RegexMarkerFilterTest.class);

	@Test
	public void testApply() {
		RegexMarkerFilter filter = new RegexMarkerFilter(MarkerFilterType.SKIP, "<skip>.*?</skip>", 0, 1000);

		String text = "J'ai du <skip>skip me</skip>mal à le croire.<skip>skip me</skip>";
		Set<TextMarker> textMarkers = filter.apply("", text, "");
		LOG.debug(textMarkers.toString());

		assertEquals(4, textMarkers.size());
		int i = 0;
		for (TextMarker textMarker : textMarkers) {
			if (i == 0) {
				assertEquals(TextMarkerType.PUSH_SKIP, textMarker.getType());
				assertEquals(8, textMarker.getPosition());
			} else if (i == 1) {
				assertEquals(TextMarkerType.POP_SKIP, textMarker.getType());
				assertEquals(28, textMarker.getPosition());
			} else if (i == 2) {
				assertEquals(TextMarkerType.PUSH_SKIP, textMarker.getType());
				assertEquals(44, textMarker.getPosition());
			} else if (i == 3) {
				assertEquals(TextMarkerType.POP_SKIP, textMarker.getType());
				assertEquals(64, textMarker.getPosition());
			}
			i++;
		}
	}

	@Test
	public void testApplyWithGroup() {
		RegexMarkerFilter filter = new RegexMarkerFilter(MarkerFilterType.SKIP, "<skip>(.*?)</skip>", 0, 1000);

		String text = "J'ai du <skip>skip me</skip>mal à le croire.<skip>skip me</skip>";
		Set<TextMarker> textMarkers = filter.apply("", text, "");
		LOG.debug(textMarkers.toString());

		int i = 0;
		for (TextMarker textMarker : textMarkers) {
			if (i == 0) {
				assertEquals(TextMarkerType.PUSH_SKIP, textMarker.getType());
				assertEquals(8, textMarker.getPosition());
			} else if (i == 1) {
				assertEquals(TextMarkerType.POP_SKIP, textMarker.getType());
				assertEquals(28, textMarker.getPosition());
			} else if (i == 2) {
				assertEquals(TextMarkerType.PUSH_SKIP, textMarker.getType());
				assertEquals(44, textMarker.getPosition());
			} else if (i == 3) {
				assertEquals(TextMarkerType.POP_SKIP, textMarker.getType());
				assertEquals(64, textMarker.getPosition());
			}
			i++;
		}

		assertEquals(4, textMarkers.size());
	}

	@Test
	public void testApplyWithReplacement() {
		RegexMarkerFilter filter = new RegexMarkerFilter(MarkerFilterType.REPLACE, "<skip>(.*?)</skip>", 0, 1000);
		filter.setReplacement("Skipped:$1");

		String text = "J'ai du <skip>skip me</skip>mal à le croire.<skip>skip this</skip>";
		Set<TextMarker> textMarkers = filter.apply("", text, "");
		LOG.debug(textMarkers.toString());

		assertEquals(6, textMarkers.size());
		int i = 0;
		for (TextMarker textMarker : textMarkers) {
			if (i == 0) {
				assertEquals(TextMarkerType.INSERT, textMarker.getType());
				assertEquals(8, textMarker.getPosition());
				assertEquals("Skipped:skip me", textMarker.getInsertionText());
			} else if (i == 1) {
				assertEquals(TextMarkerType.PUSH_SKIP, textMarker.getType());
				assertEquals(8, textMarker.getPosition());
			} else if (i == 2) {
				assertEquals(TextMarkerType.POP_SKIP, textMarker.getType());
				assertEquals(28, textMarker.getPosition());
			} else if (i == 3) {
				assertEquals(TextMarkerType.INSERT, textMarker.getType());
				assertEquals(44, textMarker.getPosition());
				assertEquals("Skipped:skip this", textMarker.getInsertionText());
			} else if (i == 4) {
				assertEquals(TextMarkerType.PUSH_SKIP, textMarker.getType());
				assertEquals(44, textMarker.getPosition());
			} else if (i == 5) {
				assertEquals(TextMarkerType.POP_SKIP, textMarker.getType());
				assertEquals(66, textMarker.getPosition());
			}
			i++;
		}
	}

	@Test
	public void testTag() {
		RegexMarkerFilter filter = new RegexMarkerFilter(MarkerFilterType.TAG, "<skip>(.*?)</skip>", 0, 1000);
		filter.setTag("TAG1", new StringAttribute("x"));

		String text = "J'ai du <skip>skip me</skip>mal à le croire.<skip>skip this</skip>";
		Set<TextMarker> textMarkers = filter.apply("", text, "");
		LOG.debug(textMarkers.toString());

		assertEquals(4, textMarkers.size());
		int i = 0;
		for (TextMarker textMarker : textMarkers) {
			if (i == 0) {
				assertEquals(TextMarkerType.TAG_START, textMarker.getType());
				assertEquals(8, textMarker.getPosition());
				assertEquals("TAG1", textMarker.getAttribute());
				assertEquals("x", textMarker.getValue().getValue());
			} else if (i == 1) {
				assertEquals(TextMarkerType.TAG_STOP, textMarker.getType());
				assertEquals(28, textMarker.getPosition());
				assertEquals("TAG1", textMarker.getAttribute());
				assertEquals("x", textMarker.getValue().getValue());
			} else if (i == 2) {
				assertEquals(TextMarkerType.TAG_START, textMarker.getType());
				assertEquals(44, textMarker.getPosition());
				assertEquals("TAG1", textMarker.getAttribute());
				assertEquals("x", textMarker.getValue().getValue());
			} else if (i == 3) {
				assertEquals(TextMarkerType.TAG_STOP, textMarker.getType());
				assertEquals(66, textMarker.getPosition());
				assertEquals("TAG1", textMarker.getAttribute());
				assertEquals("x", textMarker.getValue().getValue());
			}
			i++;
		}
	}

	@Test
	public void testUnaryOperatorsStop() {
		RegexMarkerFilter filter = new RegexMarkerFilter(MarkerFilterType.STOP, "<skip>", 0, 1000);

		String text = "J'ai du <skip>skip me</skip>mal à le croire.<skip>skip me</skip>";
		Set<TextMarker> textMarkers = filter.apply("", text, "");
		LOG.debug(textMarkers.toString());

		assertEquals(2, textMarkers.size());
		int i = 0;
		for (TextMarker textMarker : textMarkers) {
			if (i == 0) {
				assertEquals(TextMarkerType.STOP, textMarker.getType());
				assertEquals(8, textMarker.getPosition());
			} else if (i == 1) {
				assertEquals(TextMarkerType.STOP, textMarker.getType());
				assertEquals(44, textMarker.getPosition());
			}
			i++;
		}
	}

	@Test
	public void testUnaryOperatorsStart() {
		RegexMarkerFilter filter = new RegexMarkerFilter(MarkerFilterType.START, "</skip>", 0, 1000);

		String text = "J'ai du <skip>skip me</skip>mal à le croire.<skip>skip me</skip>!";
		Set<TextMarker> textMarkers = filter.apply("", text, "");
		LOG.debug(textMarkers.toString());

		assertEquals(2, textMarkers.size());
		int i = 0;
		for (TextMarker textMarker : textMarkers) {
			if (i == 0) {
				assertEquals(TextMarkerType.START, textMarker.getType());
				assertEquals(28, textMarker.getPosition());
			} else if (i == 1) {
				assertEquals(TextMarkerType.START, textMarker.getType());
				assertEquals(64, textMarker.getPosition());
			}
			i++;
		}
	}
}
