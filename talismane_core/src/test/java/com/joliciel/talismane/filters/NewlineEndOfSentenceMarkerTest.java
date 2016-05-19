package com.joliciel.talismane.filters;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NewlineEndOfSentenceMarkerTest {
	private static final Logger LOG = LoggerFactory.getLogger(NewlineEndOfSentenceMarkerTest.class);

	@Test
	public void testApply() {
		NewlineEndOfSentenceMarker filter = new NewlineEndOfSentenceMarker();

		String text = "1\r\n2\r\n";
		Set<TextMarker> textMarkers = filter.apply("", text, "");
		LOG.debug(textMarkers.toString());

		assertEquals(6, textMarkers.size());

		text = "1\r2\r";
		textMarkers = filter.apply("", text, "");
		LOG.debug(textMarkers.toString());

		assertEquals(6, textMarkers.size());

		text = "1\n2\n";
		textMarkers = filter.apply("", text, "");
		LOG.debug(textMarkers.toString());

		assertEquals(6, textMarkers.size());

	}

	@Test
	public void testApplyRealSentence() {
		NewlineEndOfSentenceMarker filter = new NewlineEndOfSentenceMarker();

		String text = "Yet another test sentence.\n";
		Set<TextMarker> textMarkers = filter.apply("", text, "");
		LOG.debug(textMarkers.toString());

		assertEquals(3, textMarkers.size());

	}
}
