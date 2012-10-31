package com.joliciel.talismane.filters;

import static org.junit.Assert.*;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

public class RegexMarkerFilterTest {
	private static final Log LOG = LogFactory.getLog(RegexMarkerFilterTest.class);

	@Test
	public void testApply() {
		FilterService filterService = new FilterServiceImpl();
		RegexMarkerFilter filter = new RegexMarkerFilter(MarkerFilterType.SKIP, "<skip>.*?</skip>");
		filter.setFilterService(filterService);
		
		String text = "J'ai du <skip>skip me</skip>mal à le croire.<skip>skip me</skip>";
		Set<TextMarker> textMarkers = filter.apply("", text, "");
		LOG.debug(textMarkers);
		
		assertEquals(3, textMarkers.size());
		int i = 0;
		for (TextMarker textMarker : textMarkers) {
			if (i==0) {
				assertEquals(TextMarkerType.STOP, textMarker.getType());
				assertEquals(8, textMarker.getPosition());
			} else if (i==1) {
				assertEquals(TextMarkerType.END_MARKER, textMarker.getType());
				assertEquals(28, textMarker.getPosition());
			} else if (i==2) {
				assertEquals(TextMarkerType.STOP, textMarker.getType());
				assertEquals(44, textMarker.getPosition());
			}
			i++;
		}
	}

	@Test
	public void testApplyWithGroup() {
		FilterService filterService = new FilterServiceImpl();
		RegexMarkerFilter filter = new RegexMarkerFilter(MarkerFilterType.SKIP, "<skip>(.*?)</skip>");
		filter.setFilterService(filterService);
		
		String text = "J'ai du <skip>skip me</skip>mal à le croire.<skip>skip me</skip>";
		Set<TextMarker> textMarkers = filter.apply("", text, "");
		LOG.debug(textMarkers);
		
		assertEquals(4, textMarkers.size());
		int i = 0;
		for (TextMarker textMarker : textMarkers) {
			if (i==0) {
				assertEquals(TextMarkerType.STOP, textMarker.getType());
				assertEquals(14, textMarker.getPosition());
			} else if (i==1) {
				assertEquals(TextMarkerType.END_MARKER, textMarker.getType());
				assertEquals(21, textMarker.getPosition());
			} else if (i==2) {
				assertEquals(TextMarkerType.STOP, textMarker.getType());
				assertEquals(50, textMarker.getPosition());
			} else if (i==3) {
				assertEquals(TextMarkerType.END_MARKER, textMarker.getType());
				assertEquals(57, textMarker.getPosition());
			}
			i++;
		}
	}

}
