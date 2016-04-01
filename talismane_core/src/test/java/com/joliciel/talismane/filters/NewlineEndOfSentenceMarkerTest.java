package com.joliciel.talismane.filters;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

public class NewlineEndOfSentenceMarkerTest {
	private static final Log LOG = LogFactory.getLog(NewlineEndOfSentenceMarkerTest.class);

	@Test
	public void testApply() {
		NewlineEndOfSentenceMarker filter = new NewlineEndOfSentenceMarker();

		String text = "1\r\n2\r\n";
		Set<TextMarker> textMarkers = filter.apply("", text, "");
		LOG.debug(textMarkers);

		assertEquals(6, textMarkers.size());

		text = "1\r2\r";
		textMarkers = filter.apply("", text, "");
		LOG.debug(textMarkers);

		assertEquals(6, textMarkers.size());

		text = "1\n2\n";
		textMarkers = filter.apply("", text, "");
		LOG.debug(textMarkers);

		assertEquals(6, textMarkers.size());

	}

	@Test
	public void testApplyRealSentence() {
		NewlineEndOfSentenceMarker filter = new NewlineEndOfSentenceMarker();

		String text = "Yet another test sentence.\n";
		Set<TextMarker> textMarkers = filter.apply("", text, "");
		LOG.debug(textMarkers);

		assertEquals(3, textMarkers.size());

	}
}
