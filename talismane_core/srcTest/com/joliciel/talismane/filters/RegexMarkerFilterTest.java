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
		FilterServiceInternal filterService = new FilterServiceImpl();
		RegexMarkerFilter filter = new RegexMarkerFilter(MarkerFilterType.SKIP, "<skip>.*?</skip>");
		filter.setFilterService(filterService);
		
		String text = "J'ai du <skip>skip me</skip>mal à le croire.<skip>skip me</skip>";
		Set<TextMarker> textMarkers = filter.apply("", text, "");
		LOG.debug(textMarkers);
		
		assertEquals(4, textMarkers.size());
		int i = 0;
		for (TextMarker textMarker : textMarkers) {
			if (i==0) {
				assertEquals(TextMarkerType.PUSH_SKIP, textMarker.getType());
				assertEquals(8, textMarker.getPosition());
			} else if (i==1) {
				assertEquals(TextMarkerType.POP_SKIP, textMarker.getType());
				assertEquals(28, textMarker.getPosition());
			} else if (i==2) {
				assertEquals(TextMarkerType.PUSH_SKIP, textMarker.getType());
				assertEquals(44, textMarker.getPosition());
			} else if (i==3) {
				assertEquals(TextMarkerType.POP_SKIP, textMarker.getType());
				assertEquals(64, textMarker.getPosition());
			}
			i++;
		}
	}

	@Test
	public void testApplyWithGroup() {
		FilterServiceInternal filterService = new FilterServiceImpl();
		RegexMarkerFilter filter = new RegexMarkerFilter(MarkerFilterType.SKIP, "<skip>(.*?)</skip>");
		filter.setFilterService(filterService);
		
		String text = "J'ai du <skip>skip me</skip>mal à le croire.<skip>skip me</skip>";
		Set<TextMarker> textMarkers = filter.apply("", text, "");
		LOG.debug(textMarkers);
		
		int i = 0;
		for (TextMarker textMarker : textMarkers) {
			if (i==0) {
				assertEquals(TextMarkerType.PUSH_SKIP, textMarker.getType());
				assertEquals(8, textMarker.getPosition());
			} else if (i==1) {
				assertEquals(TextMarkerType.POP_SKIP, textMarker.getType());
				assertEquals(28, textMarker.getPosition());
			} else if (i==2) {
				assertEquals(TextMarkerType.PUSH_SKIP, textMarker.getType());
				assertEquals(44, textMarker.getPosition());
			} else if (i==3) {
				assertEquals(TextMarkerType.POP_SKIP, textMarker.getType());
				assertEquals(64, textMarker.getPosition());
			}
			i++;
		}
		
		assertEquals(4, textMarkers.size());
	}

	@Test
	public void testApplyWithReplacement() {
		FilterServiceInternal filterService = new FilterServiceImpl();
		RegexMarkerFilter filter = new RegexMarkerFilter(MarkerFilterType.REPLACE, "<skip>(.*?)</skip>", 0);
		filter.setFilterService(filterService);
		filter.setReplacement("Skipped:$1");
		
		String text = "J'ai du <skip>skip me</skip>mal à le croire.<skip>skip this</skip>";
		Set<TextMarker> textMarkers = filter.apply("", text, "");
		LOG.debug(textMarkers);
		
		assertEquals(6, textMarkers.size());
		int i = 0;
		for (TextMarker textMarker : textMarkers) {
			if (i==0) {
				assertEquals(TextMarkerType.INSERT, textMarker.getType());
				assertEquals(8, textMarker.getPosition());
				assertEquals("Skipped:skip me", textMarker.getInsertionText());
			} else if (i==1) {
				assertEquals(TextMarkerType.PUSH_SKIP, textMarker.getType());
				assertEquals(8, textMarker.getPosition());
			} else if (i==2) {
				assertEquals(TextMarkerType.POP_SKIP, textMarker.getType());
				assertEquals(28, textMarker.getPosition());
			} else if (i==3) {
				assertEquals(TextMarkerType.INSERT, textMarker.getType());
				assertEquals(44, textMarker.getPosition());
				assertEquals("Skipped:skip this", textMarker.getInsertionText());
			} else if (i==4) {
				assertEquals(TextMarkerType.PUSH_SKIP, textMarker.getType());
				assertEquals(44, textMarker.getPosition());
			} else if (i==5) {
				assertEquals(TextMarkerType.POP_SKIP, textMarker.getType());
				assertEquals(66, textMarker.getPosition());
			}
			i++;
		}
	}
	
	@Test
	public void testTag() {
		FilterServiceInternal filterService = new FilterServiceImpl();
		RegexMarkerFilter filter = new RegexMarkerFilter(MarkerFilterType.TAG, "<skip>(.*?)</skip>", 0);
		filter.setFilterService(filterService);
		filter.setTag("TAG1", "x");
		
		String text = "J'ai du <skip>skip me</skip>mal à le croire.<skip>skip this</skip>";
		Set<TextMarker> textMarkers = filter.apply("", text, "");
		LOG.debug(textMarkers);
		
		assertEquals(4, textMarkers.size());
		int i = 0;
		for (TextMarker textMarker : textMarkers) {
			if (i==0) {
				assertEquals(TextMarkerType.TAG_START, textMarker.getType());
				assertEquals(8, textMarker.getPosition());
				assertEquals("TAG1", textMarker.getAttribute());
				assertEquals("x", textMarker.getValue());
			} else if (i==1) {
				assertEquals(TextMarkerType.TAG_STOP, textMarker.getType());
				assertEquals(28, textMarker.getPosition());
				assertEquals("TAG1", textMarker.getAttribute());
				assertEquals("x", textMarker.getValue());
			} else if (i==2) {
				assertEquals(TextMarkerType.TAG_START, textMarker.getType());
				assertEquals(44, textMarker.getPosition());
				assertEquals("TAG1", textMarker.getAttribute());
				assertEquals("x", textMarker.getValue());
			} else if (i==3) {
				assertEquals(TextMarkerType.TAG_STOP, textMarker.getType());
				assertEquals(66, textMarker.getPosition());
				assertEquals("TAG1", textMarker.getAttribute());
				assertEquals("x", textMarker.getValue());
			}
			i++;
		}
	}
	
	@Test
	public void testUnaryOperatorsStop() {
		FilterServiceInternal filterService = new FilterServiceImpl();
		RegexMarkerFilter filter = new RegexMarkerFilter(MarkerFilterType.STOP, "<skip>");
		filter.setFilterService(filterService);
		
		String text = "J'ai du <skip>skip me</skip>mal à le croire.<skip>skip me</skip>";
		Set<TextMarker> textMarkers = filter.apply("", text, "");
		LOG.debug(textMarkers);
		
		assertEquals(2, textMarkers.size());
		int i = 0;
		for (TextMarker textMarker : textMarkers) {
			if (i==0) {
				assertEquals(TextMarkerType.STOP, textMarker.getType());
				assertEquals(8, textMarker.getPosition());
			} else if (i==1) {
				assertEquals(TextMarkerType.STOP, textMarker.getType());
				assertEquals(44, textMarker.getPosition());
			}
			i++;
		}
	}
	
	@Test
	public void testUnaryOperatorsStart() {
		FilterServiceInternal filterService = new FilterServiceImpl();
		RegexMarkerFilter filter = new RegexMarkerFilter(MarkerFilterType.START, "</skip>");
		filter.setFilterService(filterService);
		
		String text = "J'ai du <skip>skip me</skip>mal à le croire.<skip>skip me</skip>!";
		Set<TextMarker> textMarkers = filter.apply("", text, "");
		LOG.debug(textMarkers);
		
		assertEquals(2, textMarkers.size());
		int i = 0;
		for (TextMarker textMarker : textMarkers) {
			if (i==0) {
				assertEquals(TextMarkerType.START, textMarker.getType());
				assertEquals(28, textMarker.getPosition());
			} else if (i==1) {
				assertEquals(TextMarkerType.START, textMarker.getType());
				assertEquals(64, textMarker.getPosition());
			}
			i++;
		}
	}
}
