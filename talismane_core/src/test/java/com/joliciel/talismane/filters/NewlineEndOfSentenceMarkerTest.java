package com.joliciel.talismane.filters;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneSession;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class NewlineEndOfSentenceMarkerTest {
	private static final Logger LOG = LoggerFactory.getLogger(NewlineEndOfSentenceMarkerTest.class);

	@Test
	public void testApply() throws Exception {
		System.setProperty("config.file", "src/test/resources/test.conf");
		ConfigFactory.invalidateCaches();
		final Config config = ConfigFactory.load();

		final TalismaneSession session = new TalismaneSession(config, "");

		NewlineEndOfSentenceMarker filter = new NewlineEndOfSentenceMarker(1000);

		RollingTextBlock textBlock = new RollingTextBlock(session, true);
		String text = "1\r\n2\r\n";
		textBlock = textBlock.roll(text);
		textBlock = textBlock.roll("");

		Set<TextMarker> textMarkers = filter.apply(textBlock);
		LOG.debug(textMarkers.toString());

		assertEquals(6, textMarkers.size());

		textBlock = new RollingTextBlock(session, true);
		text = "1\r2\r";
		textBlock = textBlock.roll(text);
		textBlock = textBlock.roll("");

		textMarkers = filter.apply(textBlock);
		LOG.debug(textMarkers.toString());

		assertEquals(6, textMarkers.size());

		textBlock = new RollingTextBlock(session, true);
		text = "1\n2\n";
		textBlock = textBlock.roll(text);
		textBlock = textBlock.roll("");

		textMarkers = filter.apply(textBlock);
		LOG.debug(textMarkers.toString());

		assertEquals(6, textMarkers.size());

	}

	@Test
	public void testApplyRealSentence() throws Exception {
		System.setProperty("config.file", "src/test/resources/test.conf");
		ConfigFactory.invalidateCaches();
		final Config config = ConfigFactory.load();

		final TalismaneSession session = new TalismaneSession(config, "");

		NewlineEndOfSentenceMarker filter = new NewlineEndOfSentenceMarker(1000);

		RollingTextBlock textBlock = new RollingTextBlock(session, true);
		String text = "Yet another test sentence.\n";
		textBlock = textBlock.roll(text);
		textBlock = textBlock.roll("");

		Set<TextMarker> textMarkers = filter.apply(textBlock);
		LOG.debug(textMarkers.toString());

		assertEquals(3, textMarkers.size());

	}
}
