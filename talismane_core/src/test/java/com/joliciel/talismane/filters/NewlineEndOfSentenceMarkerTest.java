package com.joliciel.talismane.filters;

import static org.junit.Assert.*;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

public class NewlineEndOfSentenceMarkerTest {
	private static final Log LOG = LogFactory.getLog(NewlineEndOfSentenceMarkerTest.class);

	@Test
	public void testApply() {
		FilterServiceInternal filterService = new FilterServiceImpl();
		NewlineEndOfSentenceMarker filter = new NewlineEndOfSentenceMarker();
		filter.setFilterService(filterService);
		
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
		FilterServiceInternal filterService = new FilterServiceImpl();
		NewlineEndOfSentenceMarker filter = new NewlineEndOfSentenceMarker();
		filter.setFilterService(filterService);
		
		String text = "Yet another test sentence.\n";
		Set<TextMarker> textMarkers = filter.apply("", text, "");
		LOG.debug(textMarkers);
		
		assertEquals(3, textMarkers.size());

	}
}
