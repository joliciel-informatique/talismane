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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneServiceLocator;
import com.joliciel.talismane.tokeniser.StringAttribute;
import com.joliciel.talismane.tokeniser.TokeniserService;

public class SentenceHolderImplTest {
	private static final Logger LOG = LoggerFactory.getLogger(SentenceHolderImplTest.class);

	@Before
	public void setup() {
		TalismaneServiceLocator.purgeInstance("");
	}

	@Test
	public void testGetDetectedSentences() {
		TalismaneServiceLocator talismaneServiceLocator = TalismaneServiceLocator.getInstance("");
		TokeniserService tokeniserService = talismaneServiceLocator.getTokeniserServiceLocator().getTokeniserService();

		FilterService filterService = new FilterServiceImpl();
		SentenceHolderImpl holder = new SentenceHolderImpl();
		holder.setFilterService(filterService);
		holder.setTokeniserService(tokeniserService);

		String originalText = "Hello  <b>World</b>. <o>Output this</o>How are you?  Fine<o>Output</o>,  ";
		holder.setText("Hello  World. How are you?  Fine,  ");

		for (int i = 0; i < holder.getText().length(); i++) {
			if (i < "Hello  ".length())
				holder.addOriginalIndex(i);
			else if (i < "Hello  World".length())
				holder.addOriginalIndex(i + "<b>".length());
			else if (i < "Hello  World. ".length())
				holder.addOriginalIndex(i + "<b></b>".length());
			else if (i < "Hello  World. How are you?  Fine".length())
				holder.addOriginalIndex(i + "<b></b><o>Output this</o>".length());
			else
				holder.addOriginalIndex(i + "<b></b><o>Output this</o><o>Output</o>".length());
		}
		holder.getOriginalTextSegments().put("Hello  World. ".length() - 1, "<o>Output this</o>");
		holder.getOriginalTextSegments().put("Hello  World. How are you?  Fine,".length() - 1, "<o>Output</o>");

		holder.addSentenceBoundary("Hello  World.".length() - 1);
		holder.addSentenceBoundary("Hello  World. How are you?".length() - 1);

		holder.addTagStart("State", new StringAttribute("OK"), "Hello  World. ".length());
		holder.addTagEnd("State", new StringAttribute("OK"), "Hello  World. How are you".length());

		holder.addTagStart("VerbIs", new StringAttribute(""), "Hello  World. How ".length());
		holder.addTagEnd("VerbIs", new StringAttribute(""), "Hello  World. How are".length());

		List<Sentence> sentences = holder.getDetectedSentences(null);
		for (Sentence sentence : sentences) {
			LOG.debug(sentence.getText());
		}
		assertEquals(3, sentences.size());

		Sentence sentence1 = sentences.get(0);
		assertEquals("Hello World.", sentence1.getText());
		assertEquals("Hello  <b>W".length(), sentence1.getOriginalIndex("Hello W".length()));
		assertEquals("Hello  <b>World</b>.".length() - 1, sentence1.getOriginalIndex("Hello World.".length() - 1));

		Sentence sentence2 = sentences.get(1);
		assertEquals("How are you?", sentence2.getText());
		assertEquals("Hello  <b>World</b>. <o>Output this</o>H".length(), sentence2.getOriginalIndex("H".length()));
		for (Entry<Integer, String> originalSegment : sentence2.getOriginalTextSegments().entrySet()) {
			LOG.debug(originalSegment.getKey() + ": \"" + originalSegment.getValue() + "\"");
		}
		assertEquals("<o>Output this</o>", sentence2.getOriginalTextSegments().get(0));

		for (SentenceTag<?> sentenceTag : sentence2.getSentenceTags()) {
			LOG.debug(sentenceTag.toString());
		}

		assertEquals(2, sentence2.getSentenceTags().size());
		assertEquals("State", sentence2.getSentenceTags().get(0).getAttribute());
		assertEquals("".length(), sentence2.getSentenceTags().get(0).getStartIndex());
		assertEquals("How are you".length(), sentence2.getSentenceTags().get(0).getEndIndex());
		assertEquals("VerbIs", sentence2.getSentenceTags().get(1).getAttribute());
		assertEquals("How ".length(), sentence2.getSentenceTags().get(1).getStartIndex());
		assertEquals("How are".length(), sentence2.getSentenceTags().get(1).getEndIndex());

		Sentence leftover = sentences.get(2);
		assertFalse(leftover.isComplete());
		assertEquals("Fine, ", leftover.getText());
		assertEquals("Hello  <b>World</b>. <o>Output this</o>How are you?  F".length(), leftover.getOriginalIndex("F".length()));
		for (Entry<Integer, String> originalSegment : leftover.getOriginalTextSegments().entrySet()) {
			LOG.debug(originalSegment.getKey() + ": \"" + originalSegment.getValue() + "\"");
		}
		assertEquals("<o>Output</o>", leftover.getOriginalTextSegments().get(4));

		SentenceHolderImpl holder2 = new SentenceHolderImpl();
		holder2.setFilterService(filterService);

		String originalText2 = "thanks, and you";
		holder2.setText("thanks, and you");
		for (int i = 0; i < holder2.getText().length(); i++) {
			holder2.addOriginalIndex(originalText.length() + i);
		}
		sentences = holder2.getDetectedSentences(leftover);
		assertEquals(1, sentences.size());

		leftover = sentences.get(0);
		assertEquals("Fine, thanks, and you", leftover.getText());
		assertEquals("Hello  <b>World</b>. <o>Output this</o>How are you?  F".length(), leftover.getOriginalIndex("F".length()));
		for (Entry<Integer, String> originalSegment : leftover.getOriginalTextSegments().entrySet()) {
			LOG.debug(originalSegment.getKey() + ": \"" + originalSegment.getValue() + "\"");
		}
		assertEquals("<o>Output</o>", leftover.getOriginalTextSegments().get(4));
		assertFalse(leftover.isComplete());

		SentenceHolderImpl holder3 = new SentenceHolderImpl();
		holder3.setFilterService(filterService);

		String originalText3 = "? Grand.";
		holder3.setText(originalText3);
		for (int i = 0; i < holder2.getText().length(); i++) {
			holder3.addOriginalIndex(originalText.length() + originalText2.length() + i);
		}
		holder3.addSentenceBoundary("?".length() - 1);
		holder3.addSentenceBoundary("? Grand.".length() - 1);
		sentences = holder3.getDetectedSentences(leftover);
		assertEquals(2, sentences.size());

		sentence1 = sentences.get(0);
		assertEquals("Fine, thanks, and you?", sentence1.getText());
		assertEquals("Hello  <b>World</b>. <o>Output this</o>How are you?  F".length(), sentence1.getOriginalIndex("F".length()));
		for (Entry<Integer, String> originalSegment : sentence1.getOriginalTextSegments().entrySet()) {
			LOG.debug(originalSegment.getKey() + ": \"" + originalSegment.getValue() + "\"");
		}
		assertEquals("<o>Output</o>", sentence1.getOriginalTextSegments().get(4));
		assertTrue(sentence1.isComplete());

		sentence2 = sentences.get(1);
		assertEquals("Grand.", sentence2.getText());
		assertEquals("Hello  <b>World</b>. <o>Output this</o>How are you?  Fine<o>Output</o>,  thanks, and you? G".length(),
				sentence2.getOriginalIndex("G".length()));
		assertTrue(sentence2.isComplete());
	}

	@Test
	public void testGetDetectedSentencesWithBoundaryAtEnd() {
		FilterService filterService = new FilterServiceImpl();
		SentenceHolderImpl holder = new SentenceHolderImpl();
		holder.setFilterService(filterService);

		holder.setText("Hello World.");

		for (int i = 0; i < holder.getText().length(); i++) {
			holder.addOriginalIndex(i);
		}

		holder.addSentenceBoundary("Hello World.".length() - 1);

		List<Sentence> sentences = holder.getDetectedSentences(null);
		for (Sentence sentence : sentences) {
			LOG.debug(sentence.getText());
		}
		assertEquals(1, sentences.size());

		assertEquals("Hello World.", sentences.iterator().next().getText());

	}

	@Test
	public void testGetDetectedSentencesWithNewlines() {
		FilterService filterService = new FilterServiceImpl();
		SentenceHolderImpl holder = new SentenceHolderImpl();
		holder.setFilterService(filterService);

		holder.setText("Hello World. How are you? Fine thanks.");

		for (int i = 0; i < holder.getText().length(); i++) {
			holder.addOriginalIndex(i);
		}

		holder.addSentenceBoundary("Hello World.".length() - 1);
		holder.addSentenceBoundary("Hello World.\nHow\nare you?".length() - 1);
		holder.addSentenceBoundary("Hello World.\nHow\nare you? Fine\nthanks.".length() - 1);
		holder.addNewline(0, 0);
		holder.addNewline("Hello World.\n".length(), 1);
		holder.addNewline("Hello World.\nHow\n".length(), 2);
		holder.addNewline("Hello World.\nHow\nare you? Fine\n".length(), 3);

		List<Sentence> sentences = holder.getDetectedSentences(null);
		for (Sentence sentence : sentences) {
			LOG.debug(sentence.getText());
		}
		assertEquals(3, sentences.size());

		for (int i = 0; i < sentences.size(); i++) {
			Sentence sentence = sentences.get(i);
			if (i == 0) {
				assertEquals("Hello World.", sentence.getText());
			} else if (i == 1) {
				assertEquals("How are you?", sentence.getText());
				assertEquals(1, sentence.getLineNumber(sentence.getOriginalIndex("How".length() - 1)));
				assertEquals(3, sentence.getColumnNumber(sentence.getOriginalIndex("How".length() - 1)));
				assertEquals(2, sentence.getLineNumber(sentence.getOriginalIndex("How a".length() - 1)));
				assertEquals(3, sentence.getColumnNumber(sentence.getOriginalIndex("How are".length() - 1)));
			}
		}

	}
}
