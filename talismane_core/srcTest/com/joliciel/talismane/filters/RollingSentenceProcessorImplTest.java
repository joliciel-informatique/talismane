///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
//
//This file is part of Talismane.
//
//Talismane is free software: you can redistribute it and/or modify
//it under the terms of the GNU Affero General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//Talismane is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Affero General Public License for more details.
//
//You should have received a copy of the GNU Affero General Public License
//along with Talismane.  If not, see <http://www.gnu.org/licenses/>.
//////////////////////////////////////////////////////////////////////////////
package com.joliciel.talismane.filters;

import static org.junit.Assert.*;

import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;

import mockit.NonStrictExpectations;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

public class RollingSentenceProcessorImplTest {
	private static final Log LOG = LogFactory.getLog(RollingSentenceProcessorImplTest.class);

	@Test
	public void testAddNextSegment() {
		FilterService filterService = new FilterServiceImpl();
		RollingSentenceProcessorImpl processor = new RollingSentenceProcessorImpl("");
		processor.setFilterService(filterService);
		
		final Set<TextMarker> textMarkers = new LinkedHashSet<TextMarker>();
		
		new NonStrictExpectations() {
			TextMarker textMarker1, textMarker2;
			{
				textMarker1.getType(); returns(TextMarkerType.STOP);
				textMarker1.getPosition(); returns(6);
				
				textMarker2.getType(); returns(TextMarkerType.END_MARKER);
				textMarker2.getPosition(); returns(9);
				
				textMarkers.add(textMarker1);
				textMarkers.add(textMarker2);
			}
		};

		SentenceHolder holder = processor.addNextSegment("Hello <b>World", textMarkers);
		assertEquals("Hello World", holder.getText());
		
		// up to World we have no text to skip
		assertEquals("Hello".length(), holder.getOriginalIndex("Hello".length()));
		// World starts at index 7
		assertEquals("Hello <b>W".length(), holder.getOriginalIndex("Hello W".length()));
		
		// no segments marked for output
		assertEquals(0, holder.getOriginalTextSegments().size());
	}

	@Test
	public void testAddNextSegmentWithSpace() {
		FilterService filterService = new FilterServiceImpl();
		RollingSentenceProcessorImpl processor = new RollingSentenceProcessorImpl("");
		processor.setFilterService(filterService);
		
		final Set<TextMarker> textMarkers = new LinkedHashSet<TextMarker>();
		
		new NonStrictExpectations() {
			TextMarker textMarker0, textMarker1, textMarker2;
			{
				textMarker0.getType(); returns(TextMarkerType.SPACE);
				textMarker0.getPosition(); returns(5);
				textMarkers.add(textMarker0);

				textMarker1.getType(); returns(TextMarkerType.STOP);
				textMarker1.getPosition(); returns(5);
				textMarkers.add(textMarker1);
				
				textMarker2.getType(); returns(TextMarkerType.END_MARKER);
				textMarker2.getPosition(); returns(8);
				textMarkers.add(textMarker2);
				
			}
		};

		SentenceHolder holder = processor.addNextSegment("Hello<b>World", textMarkers);
		assertEquals("Hello World", holder.getText());
		
		// up to World we have no text to skip
		assertEquals("Hello".length(), holder.getOriginalIndex("Hello".length()));
		// World starts at index 7
		assertEquals("Hello<b>W".length(), holder.getOriginalIndex("Hello W".length()));
		
		// no segments marked for output
		assertEquals(0, holder.getOriginalTextSegments().size());
	}
	
	@Test
	public void testAddNextSegmentWithLeftoverMarker() {
		FilterService filterService = new FilterServiceImpl();
		RollingSentenceProcessorImpl processor = new RollingSentenceProcessorImpl("");
		processor.setFilterService(filterService);
		
		final Set<TextMarker> textMarkers1 = new LinkedHashSet<TextMarker>();
		final Set<TextMarker> textMarkers2 = new LinkedHashSet<TextMarker>();
		
		new NonStrictExpectations() {
			TextMarker textMarker1, textMarker2, textMarker3;
			{
				textMarker1.getType(); returns(TextMarkerType.STOP);
				textMarker1.getPosition(); returns("Hello ".length());
								
				textMarkers1.add(textMarker1);
				
				textMarker2.getType(); returns(TextMarkerType.END_MARKER);
				textMarker2.getPosition(); returns("</b>".length());
				textMarkers2.add(textMarker2);
				
				textMarker3.getType(); returns(TextMarkerType.SENTENCE_BREAK);
				textMarker3.getPosition(); returns("</b>. How are you?".length());
				textMarkers2.add(textMarker3);
			}
		};

		SentenceHolder holder = processor.addNextSegment("Hello <b>World", textMarkers1);
		assertEquals("Hello ", holder.getText());
		assertEquals(0, holder.getOriginalTextSegments().size());
		
		holder = processor.addNextSegment("</b>. How are you? Fine thanks.", textMarkers2);
		assertEquals(". How are you? Fine thanks.", holder.getText());
		assertEquals(0, holder.getOriginalTextSegments().size());
		
		assertEquals("Hello <b>World</b>.".length(), holder.getOriginalIndex(".".length()));
		assertEquals("Hello <b>World</b>. How are you? F".length(), holder.getOriginalIndex(". How are you? F".length()));
		assertEquals(". How are you? Fine thanks.", holder.getText());
		assertEquals(". How are you?".length()-1, holder.getSentenceBoundaries().iterator().next().intValue());
	}
	
	@Test
	public void testAddNextSegmentWithLeftoverMarkerAndOutput() {
		FilterService filterService = new FilterServiceImpl();
		RollingSentenceProcessorImpl processor = new RollingSentenceProcessorImpl("");
		processor.setFilterService(filterService);
		
		final Set<TextMarker> textMarkers1 = new LinkedHashSet<TextMarker>();
		final Set<TextMarker> textMarkers2 = new LinkedHashSet<TextMarker>();
		
		new NonStrictExpectations() {
			TextMarker textMarker1, textMarker2, textMarker3, textMarker4;
			{
				textMarker1.getType(); returns(TextMarkerType.STOP);
				textMarker1.getPosition(); returns(6);								
				textMarkers1.add(textMarker1);

				textMarker2.getType(); returns(TextMarkerType.OUTPUT);
				textMarker2.getPosition(); returns(6);								
				textMarkers1.add(textMarker2);
				
				textMarker3.getType(); returns(TextMarkerType.END_MARKER);
				textMarker3.getPosition(); returns(4);
				textMarkers2.add(textMarker3);
				
				textMarker4.getType(); returns(TextMarkerType.END_MARKER);
				textMarker4.getPosition(); returns(4);
				textMarkers2.add(textMarker4);
			}
		};

		SentenceHolder holder = processor.addNextSegment("Hello <b>World", textMarkers1);
		assertEquals("Hello ", holder.getText());
		assertEquals(0, holder.getOriginalTextSegments().size());
		
		holder = processor.addNextSegment("</b>. How are you?", textMarkers2);
		assertEquals(". How are you?", holder.getText());
		assertEquals(1, holder.getOriginalTextSegments().size());
		assertEquals("<b>World</b>", holder.getOriginalTextSegments().get(0));
		
		assertEquals("Hello <b>World</b>.".length(), holder.getOriginalIndex(".".length()));
	}
	
	@Test
	public void testEmbeddedMarkers() {
		FilterService filterService = new FilterServiceImpl();
		RollingSentenceProcessorImpl processor = new RollingSentenceProcessorImpl("");
		processor.setFilterService(filterService);
		
		final Set<TextMarker> textMarkers1 = new LinkedHashSet<TextMarker>();
		final Set<TextMarker> textMarkers2 = new LinkedHashSet<TextMarker>();
		
		new NonStrictExpectations() {
			TextMarker textMarker1, textMarker2, textMarker3, textMarker4, textMarker5, textMarker6, textMarker7, textMarker8, textMarker9, textMarker10;
			{
				TextMarker[] textMarkers = new TextMarker[] {
					textMarker1, textMarker2, textMarker3, textMarker4, textMarker5, textMarker6, textMarker7, textMarker8, textMarker9, textMarker10
				};
				
				TextMarkerType[] textMarkerTypes = new TextMarkerType[] {
					TextMarkerType.STOP,
					TextMarkerType.OUTPUT,
					TextMarkerType.START,
					TextMarkerType.STOP,
					TextMarkerType.OUTPUT,
					TextMarkerType.END_MARKER,
					TextMarkerType.END_MARKER,
					TextMarkerType.END_MARKER,
					TextMarkerType.END_MARKER,
					TextMarkerType.END_MARKER,					
				};
				
				int[] positions = new int[] { 7, 13, 21, 3, 3, 11, 11, 20, 28, 37 };

				for (int i=0; i<10; i++) {
					textMarkers[i].getType(); returns(textMarkerTypes[i]);
					textMarkers[i].getPosition(); returns(positions[i]);
				}
				textMarkers1.add(textMarkers[0]);
				textMarkers1.add(textMarkers[1]);
				textMarkers1.add(textMarkers[2]);
				
				textMarkers2.add(textMarkers[3]);
				textMarkers2.add(textMarkers[4]);
				textMarkers2.add(textMarkers[5]);
				textMarkers2.add(textMarkers[6]);
				textMarkers2.add(textMarkers[7]);
				textMarkers2.add(textMarkers[8]);
				textMarkers2.add(textMarkers[9]);
			}
		};

		SentenceHolder holder = processor.addNextSegment("Normal [Stop [Output [Sta", textMarkers1);
		assertEquals("Normal [Sta", holder.getText());
		assertEquals(1, holder.getOriginalTextSegments().size());
		assertEquals("[Output ", holder.getOriginalTextSegments().get(7));
		
		holder = processor.addNextSegment("rt [Output2]continue]Output3]NoOutput] done", textMarkers2);
		assertEquals("rt ]continue] done", holder.getText());
		for (Entry<Integer,String> originalSegment : holder.getOriginalTextSegments().entrySet()) {
			LOG.debug(originalSegment.getKey() + ": \"" + originalSegment.getValue() + "\"");
		}
		assertEquals(2, holder.getOriginalTextSegments().size());
		assertEquals("[Output2", holder.getOriginalTextSegments().get(3));
		assertEquals("]Output3", holder.getOriginalTextSegments().get(12));
	}
	
	@Test
	public void testNewlines() {
		FilterService filterService = new FilterServiceImpl();
		RollingSentenceProcessorImpl processor = new RollingSentenceProcessorImpl("");
		processor.setFilterService(filterService);
		
		final Set<TextMarker> textMarkers1 = new LinkedHashSet<TextMarker>();
		
		SentenceHolder holder = processor.addNextSegment("Hello World.\r\nHow are you?\r\nFine thanks.\r\n", textMarkers1);
		LOG.debug(holder.getNewlines());
		assertEquals(4, holder.getNewlines().size());
		
		int i=0;
		for (Entry<Integer, Integer> newlineLocation : holder.getNewlines().entrySet()) {
			if (i==0) {
				assertEquals(0, newlineLocation.getKey().intValue());
				assertEquals(i, newlineLocation.getValue().intValue());
			} else if (i==1) {
				assertEquals("Hello World.\r\n".length(), newlineLocation.getKey().intValue());
				assertEquals(i, newlineLocation.getValue().intValue());
			} else if (i==2) {
				assertEquals("Hello World.\r\nHow are you?\r\n".length(), newlineLocation.getKey().intValue());
				assertEquals(i, newlineLocation.getValue().intValue());
			} else if (i==3) {
				assertEquals("Hello World.\r\nHow are you?\r\nFine thanks.\r\n".length(), newlineLocation.getKey().intValue());
				assertEquals(i, newlineLocation.getValue().intValue());
			} 
			i++;
		}
		
		assertEquals(1, holder.getLineNumber("Hello World.\r\nHow".length()-1));
		assertEquals(2, holder.getColumnNumber("Hello World.\r\nHow".length()-1));
		
		holder = processor.addNextSegment("And you?\r\nGrand,", textMarkers1);
		LOG.debug(holder.getNewlines());
		assertEquals(2, holder.getNewlines().size());
		
		i=0;
		for (Entry<Integer, Integer> newlineLocation : holder.getNewlines().entrySet()) {
			if (i==0) {
				assertEquals("Hello World.\r\nHow are you?\r\nFine thanks.\r\n".length(), newlineLocation.getKey().intValue());
				assertEquals(i+3, newlineLocation.getValue().intValue());
			} else if (i==1) {
				assertEquals("Hello World.\r\nHow are you?\r\nFine thanks.\r\nAnd you?\r\n".length(), newlineLocation.getKey().intValue());
				assertEquals(i+3, newlineLocation.getValue().intValue());
			} 
			i++;
		}
		
		holder = processor.addNextSegment(" and yourself?", textMarkers1);
		LOG.debug(holder.getNewlines());
		assertEquals(1, holder.getNewlines().size());
		
		i=0;
		for (Entry<Integer, Integer> newlineLocation : holder.getNewlines().entrySet()) {
			if (i==0) {
				assertEquals("Hello World.\r\nHow are you?\r\nFine thanks.\r\nAnd you?\r\n".length(), newlineLocation.getKey().intValue());
				assertEquals(i+4, newlineLocation.getValue().intValue());
			} 
			i++;
		}
		
		assertEquals(4, holder.getLineNumber("Hello World.\r\nHow are you?\r\nFine thanks.\r\nAnd you?\r\nGrand, and".length()-1));
		assertEquals(9, holder.getColumnNumber("Hello World.\r\nHow are you?\r\nFine thanks.\r\nAnd you?\r\nGrand, and".length()-1));

	}
}
