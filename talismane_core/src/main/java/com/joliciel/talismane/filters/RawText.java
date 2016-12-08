///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2016 Joliciel Informatique
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

import java.util.List;

import com.joliciel.talismane.AnnotatedText;
import com.joliciel.talismane.TalismaneSession;

/**
 * An annotated text which can be filtered and sentence-detected, after which we
 * can retrieve the resulting sentences. <br/>
 * <br/>
 * Typical usage:
 * 
 * <pre>
 * String text = ...;
 * RawText rawText = new RawText(text, processByDefault, session);
 * 
 * // annotate  with raw text filters
 * for (RawTextFilter textMarkerFilter : session.getTextFilters()) {
 *   textMarkerFilter.annotate(rawText);
 * }
 * 
 * // detect sentences on processed text using the sentence detector
 * AnnotatedText processedText = rawText.getProcessedText();
 * sentenceDetector.detectSentences(processedText);
 * 
 * // get the sentences detected
 * List&lt;Sentence&gt; sentences = rawText.getDetectedSentences();
 * </pre>
 * 
 * @author Assaf Urieli
 *
 */
public class RawText extends RawTextProcessor {
	private final CharSequence text;
	private final TalismaneSession session;

	/**
	 * Constructor
	 * 
	 * @param text
	 *            the text to analyse
	 * @param processByDefault
	 *            whether the text should be analysed from the start, or should
	 *            wait for a filter to indicate that analysis starts (e.g. when
	 *            processing XML)
	 * @param session
	 */
	public RawText(CharSequence text, boolean processByDefault, TalismaneSession session) {
		super(text, processByDefault, session);
		this.text = text;
		this.session = session;
	}

	@Override
	public AnnotatedText getProcessedText() {
		SentenceHolder prevHolder = new SentenceHolder(session, 0, false);
		SentenceHolder sentenceHolder = this.processText(0, text.length(), text, true);
		SentenceHolder nextHolder = new SentenceHolder(session, text.length(), true);
		return this.getProcessedTextBlock(0, text.length(), prevHolder, sentenceHolder, nextHolder);
	}

	/**
	 * Get a list of sentences currently detected.
	 * 
	 * @return
	 */
	public List<Sentence> getDetectedSentences() {
		SentenceHolder sentenceHolder = this.processText(0, text.length(), text, true);
		SentenceHolder prevHolder = new SentenceHolder(session, 0, false);
		this.addDetectedSentences(prevHolder, sentenceHolder);
		List<Sentence> sentences = sentenceHolder.getDetectedSentences(null);
		return sentences;
	}
}
