///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2014 Joliciel Informatique
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

import static org.junit.Assert.assertEquals;

import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mockit.NonStrictExpectations;

public class RollingSentenceProcessorImplTest {
	private static final Logger LOG = LoggerFactory.getLogger(RollingSentenceProcessorImplTest.class);

	@Test
	public void testAddNextSegment() {
		FilterService filterService = new FilterServiceImpl();
		RollingSentenceProcessorImpl processor = new RollingSentenceProcessorImpl("");
		processor.setFilterService(filterService);

		final Set<TextMarker> textMarkers = new LinkedHashSet<TextMarker>();

		new NonStrictExpectations() {
			TextMarker textMarker1, textMarker2;
			{
				textMarker1.getType();
				returns(TextMarkerType.PUSH_SKIP);
				textMarker1.getPosition();
				returns(6);

				textMarker2.getType();
				returns(TextMarkerType.POP_SKIP);
				textMarker2.getPosition();
				returns(9);

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
				textMarker0.getType();
				returns(TextMarkerType.SPACE);
				textMarker0.getPosition();
				returns(5);
				textMarkers.add(textMarker0);

				textMarker1.getType();
				returns(TextMarkerType.PUSH_SKIP);
				textMarker1.getPosition();
				returns(5);
				textMarkers.add(textMarker1);

				textMarker2.getType();
				returns(TextMarkerType.POP_SKIP);
				textMarker2.getPosition();
				returns(8);
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
				textMarker1.getType();
				returns(TextMarkerType.PUSH_SKIP);
				textMarker1.getPosition();
				returns("Hello ".length());

				textMarkers1.add(textMarker1);

				textMarker2.getType();
				returns(TextMarkerType.POP_SKIP);
				textMarker2.getPosition();
				returns("</b>".length());
				textMarkers2.add(textMarker2);

				textMarker3.getType();
				returns(TextMarkerType.SENTENCE_BREAK);
				textMarker3.getPosition();
				returns("</b>. How are you?".length());
				textMarkers2.add(textMarker3);
			}
		};

		SentenceHolder holder = processor.addNextSegment("Hello <b>World", textMarkers1);
		assertEquals("Hello ", holder.getText());
		assertEquals(0, holder.getOriginalTextSegments().size());

		holder = processor.addNextSegment("</b>. How are you? Fine thanks.", textMarkers2);
		assertEquals(". How are you?  Fine thanks.", holder.getText());
		assertEquals(0, holder.getOriginalTextSegments().size());

		assertEquals("Hello <b>World</b>.".length(), holder.getOriginalIndex(".".length()));
		assertEquals("Hello <b>World</b>. How are you? F".length(), holder.getOriginalIndex(". How are you?  F".length()));
		assertEquals(". How are you?  Fine thanks.", holder.getText());
		assertEquals(". How are you?".length() - 1, holder.getSentenceBoundaries().iterator().next().intValue());
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
				textMarker1.getType();
				returns(TextMarkerType.PUSH_SKIP);
				textMarker1.getPosition();
				returns(6);
				textMarkers1.add(textMarker1);

				textMarker2.getType();
				returns(TextMarkerType.PUSH_OUTPUT);
				textMarker2.getPosition();
				returns(6);
				textMarkers1.add(textMarker2);

				textMarker3.getType();
				returns(TextMarkerType.POP_OUTPUT);
				textMarker3.getPosition();
				returns(4);
				textMarkers2.add(textMarker3);

				textMarker4.getType();
				returns(TextMarkerType.POP_SKIP);
				textMarker4.getPosition();
				returns(4);
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
				TextMarker[] textMarkers = new TextMarker[] { textMarker1, textMarker2, textMarker3, textMarker4, textMarker5, textMarker6, textMarker7,
						textMarker8, textMarker9, textMarker10 };

				TextMarkerType[] textMarkerTypes = new TextMarkerType[] { TextMarkerType.PUSH_SKIP, TextMarkerType.PUSH_OUTPUT, TextMarkerType.PUSH_INCLUDE,
						TextMarkerType.PUSH_SKIP, TextMarkerType.PUSH_OUTPUT, TextMarkerType.POP_OUTPUT, TextMarkerType.POP_SKIP, TextMarkerType.POP_INCLUDE,
						TextMarkerType.POP_OUTPUT, TextMarkerType.POP_SKIP, };

				int[] positions = new int[] { 7, 13, 21, 3, 3, 11, 11, 20, 28, 37 };

				for (int i = 0; i < 10; i++) {
					textMarkers[i].getType();
					returns(textMarkerTypes[i]);
					textMarkers[i].getPosition();
					returns(positions[i]);
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
		for (Entry<Integer, String> originalSegment : holder.getOriginalTextSegments().entrySet()) {
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
		LOG.debug(holder.getNewlines().toString());
		assertEquals(4, holder.getNewlines().size());

		int i = 1;
		for (Entry<Integer, Integer> newlineLocation : holder.getNewlines().entrySet()) {
			if (i == 1) {
				assertEquals(0, newlineLocation.getKey().intValue());
				assertEquals(i, newlineLocation.getValue().intValue());
			} else if (i == 2) {
				assertEquals("Hello World.\r\n".length(), newlineLocation.getKey().intValue());
				assertEquals(i, newlineLocation.getValue().intValue());
			} else if (i == 3) {
				assertEquals("Hello World.\r\nHow are you?\r\n".length(), newlineLocation.getKey().intValue());
				assertEquals(i, newlineLocation.getValue().intValue());
			} else if (i == 4) {
				assertEquals("Hello World.\r\nHow are you?\r\nFine thanks.\r\n".length(), newlineLocation.getKey().intValue());
				assertEquals(i, newlineLocation.getValue().intValue());
			}
			i++;
		}

		assertEquals(2, holder.getLineNumber("Hello World.\r\nHow".length() - 1));
		assertEquals(3, holder.getColumnNumber("Hello World.\r\nHow".length() - 1));

		holder = processor.addNextSegment("And you?\r\nGrand,", textMarkers1);
		LOG.debug(holder.getNewlines().toString());
		assertEquals(2, holder.getNewlines().size());

		i = 1;
		for (Entry<Integer, Integer> newlineLocation : holder.getNewlines().entrySet()) {
			if (i == 1) {
				assertEquals("Hello World.\r\nHow are you?\r\nFine thanks.\r\n".length(), newlineLocation.getKey().intValue());
				assertEquals(i + 3, newlineLocation.getValue().intValue());
			} else if (i == 2) {
				assertEquals("Hello World.\r\nHow are you?\r\nFine thanks.\r\nAnd you?\r\n".length(), newlineLocation.getKey().intValue());
				assertEquals(i + 3, newlineLocation.getValue().intValue());
			}
			i++;
		}

		holder = processor.addNextSegment(" and yourself?", textMarkers1);
		LOG.debug(holder.getNewlines().toString());
		assertEquals(1, holder.getNewlines().size());

		i = 1;
		for (Entry<Integer, Integer> newlineLocation : holder.getNewlines().entrySet()) {
			if (i == 1) {
				assertEquals("Hello World.\r\nHow are you?\r\nFine thanks.\r\nAnd you?\r\n".length(), newlineLocation.getKey().intValue());
				assertEquals(i + 4, newlineLocation.getValue().intValue());
			}
			i++;
		}

		assertEquals(5, holder.getLineNumber("Hello World.\r\nHow are you?\r\nFine thanks.\r\nAnd you?\r\nGrand, and".length() - 1));
		assertEquals(10, holder.getColumnNumber("Hello World.\r\nHow are you?\r\nFine thanks.\r\nAnd you?\r\nGrand, and".length() - 1));
	}

	@Test
	public void testUnaryMarkers() {
		FilterService filterService = new FilterServiceImpl();
		RollingSentenceProcessorImpl processor = new RollingSentenceProcessorImpl("");
		processor.setFilterService(filterService);

		final Set<TextMarker> textMarkers1 = new LinkedHashSet<TextMarker>();
		final Set<TextMarker> textMarkers2 = new LinkedHashSet<TextMarker>();

		new NonStrictExpectations() {
			TextMarker textMarker1, textMarker2, textMarker3, textMarker4;
			{
				TextMarker[] textMarkers = new TextMarker[] { textMarker1, textMarker2, textMarker3, textMarker4 };

				TextMarkerType[] textMarkerTypes = new TextMarkerType[] { TextMarkerType.STOP, TextMarkerType.START, TextMarkerType.STOP,
						TextMarkerType.START };

				int[] positions = new int[] { "Hello ".length(), "Hello <b>".length(), "ld".length(), "ld</b>".length() };

				for (int i = 0; i < 4; i++) {
					textMarkers[i].getType();
					returns(textMarkerTypes[i]);
					textMarkers[i].getPosition();
					returns(positions[i]);
				}
				textMarkers1.add(textMarkers[0]);
				textMarkers1.add(textMarkers[1]);
				textMarkers2.add(textMarkers[2]);
				textMarkers2.add(textMarkers[3]);
			}
		};

		SentenceHolder holder = processor.addNextSegment("Hello <b>Wor", textMarkers1);
		assertEquals("Hello Wor", holder.getText());

		holder = processor.addNextSegment("ld</b>.", textMarkers2);
		assertEquals("ld.", holder.getText());
	}

	@Test
	public void testReplace() {
		FilterService filterService = new FilterServiceImpl();
		RollingSentenceProcessorImpl processor = new RollingSentenceProcessorImpl("");
		processor.setFilterService(filterService);

		final Set<TextMarker> textMarkers1 = new LinkedHashSet<TextMarker>();
		final Set<TextMarker> textMarkers2 = new LinkedHashSet<TextMarker>();

		new NonStrictExpectations() {
			TextMarker textMarker1, textMarker2, textMarker3;
			{
				TextMarker[] textMarkers = new TextMarker[] { textMarker1, textMarker2, textMarker3 };

				TextMarkerType[] textMarkerTypes = new TextMarkerType[] { TextMarkerType.INSERT, TextMarkerType.PUSH_SKIP, TextMarkerType.POP_SKIP };

				int[] positions = new int[] { "Hello ".length(), "Hello ".length(), "ld</b>".length() };

				for (int i = 0; i < 3; i++) {
					textMarkers[i].getType();
					returns(textMarkerTypes[i]);
					textMarkers[i].getPosition();
					returns(positions[i]);
				}

				textMarkers1.add(textMarkers[0]);
				textMarkers1.add(textMarkers[1]);
				textMarkers2.add(textMarkers[2]);

				textMarker1.getInsertionText();
				returns("Jim");
			}
		};

		SentenceHolder holder = processor.addNextSegment("Hello <b>Wor", textMarkers1);
		assertEquals("Hello Jim", holder.getText());
		assertEquals("Hello ".length(), holder.getOriginalIndex("Hello J".length() - 1));
		assertEquals("Hello ".length(), holder.getOriginalIndex("Hello Ji".length() - 1));
		assertEquals("Hello ".length(), holder.getOriginalIndex("Hello Jim".length() - 1));

		holder = processor.addNextSegment("ld</b>.", textMarkers2);
		assertEquals(".", holder.getText());
		assertEquals("Hello <b>World</b>".length(), holder.getOriginalIndex(".".length() - 1));
	}

	@Test
	public void testNestedStackAndUnary() {
		FilterService filterService = new FilterServiceImpl();
		RollingSentenceProcessorImpl processor = new RollingSentenceProcessorImpl("");
		processor.setFilterService(filterService);

		final Set<TextMarker> textMarkers1 = new LinkedHashSet<TextMarker>();
		final Set<TextMarker> textMarkers2 = new LinkedHashSet<TextMarker>();

		new NonStrictExpectations() {
			TextMarker textMarker1, textMarker2, textMarker3, textMarker4, textMarker5, textMarker6, textMarker7, textMarker8, textMarker9, textMarker10;
			{
				TextMarker[] textMarkers = new TextMarker[] { textMarker1, textMarker2, textMarker3, textMarker4, textMarker5, textMarker6, textMarker7,
						textMarker8, textMarker9, textMarker10 };

				TextMarkerType[] textMarkerTypes = new TextMarkerType[] { TextMarkerType.PUSH_OUTPUT, TextMarkerType.PUSH_SKIP, TextMarkerType.PUSH_INCLUDE,
						TextMarkerType.STOP, TextMarkerType.STOP_OUTPUT, TextMarkerType.POP_INCLUDE, TextMarkerType.START_OUTPUT, TextMarkerType.START,
						TextMarkerType.POP_SKIP, TextMarkerType.POP_OUTPUT, };

				int[] positions = new int[] { "A".length(), "A[>O]".length(), "A[>O][>S]".length(), "A[>O][>S][>I]".length(), "".length(), "[O-]".length(),
						"[O-][I<]".length(), "[O-][I<][O+]".length(), "[O-][I<][O+][+]".length(), "[O-][I<][O+][+][S<]".length(), };

				for (int i = 0; i < 10; i++) {
					textMarkers[i].getType();
					returns(textMarkerTypes[i]);
					textMarkers[i].getPosition();
					returns(positions[i]);
				}
				textMarkers1.add(textMarkers[0]);
				textMarkers1.add(textMarkers[1]);
				textMarkers1.add(textMarkers[2]);
				textMarkers1.add(textMarkers[3]);

				textMarkers2.add(textMarkers[4]);
				textMarkers2.add(textMarkers[5]);
				textMarkers2.add(textMarkers[6]);
				textMarkers2.add(textMarkers[7]);
				textMarkers2.add(textMarkers[8]);
				textMarkers2.add(textMarkers[9]);

			}
		};

		SentenceHolder holder = processor.addNextSegment("A[>O][>S][>I][-]", textMarkers1);
		assertEquals("A[>O][>I]", holder.getText());
		for (Entry<Integer, String> originalSegment : holder.getOriginalTextSegments().entrySet()) {
			LOG.debug(originalSegment.getKey() + ": \"" + originalSegment.getValue() + "\"");
		}
		assertEquals("[>S]", holder.getOriginalTextSegments().get(5));

		holder = processor.addNextSegment("[O-][I<][O+][+][S<][O<]Z", textMarkers2);
		assertEquals("[+][S<][O<]Z", holder.getText());
		for (Entry<Integer, String> originalSegment : holder.getOriginalTextSegments().entrySet()) {
			LOG.debug(originalSegment.getKey() + ": \"" + originalSegment.getValue() + "\"");
		}
		assertEquals("[-][O+]", holder.getOriginalTextSegments().get(0));

	}
}
