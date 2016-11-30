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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.AnnotatedText;
import com.joliciel.talismane.Annotation;

/**
 * A block of text on which we attempt to detect sentences. Sentences are only
 * detected inside the text, never the prevText or nextText.
 * 
 * @author Assaf Urieli
 *
 */
public class RollingTextBlock extends AnnotatedText {
	private static final Logger LOG = LoggerFactory.getLogger(RollingTextBlock.class);

	private final String prevText;
	private final String currentText;
	private final String nextText;

	/**
	 * Creates a new RollingTextBlock with prev, current and next all set to
	 * empty strings.
	 */
	public RollingTextBlock() {
		this("", "", "", Collections.emptyList());
	}

	private RollingTextBlock(String prevText, String currentText, String nextText, List<Annotation<?>> annotations) {
		super(currentText + nextText, currentText.length(), annotations);
		this.prevText = prevText;
		this.currentText = currentText;
		this.nextText = nextText;
	}

	/**
	 * Creates a new RollingTextBlock.<br/>
	 * Moves next → current, current → prev, sets next<br/>
	 * All existing annotations have their start and end decremented by
	 * prev.length(). If the new start &lt; 0, start = 0, if new end &lt; 0,
	 * annotation dropped.
	 * 
	 * @param nextText
	 *            the next text segment to add onto this rolling text block
	 * @return a new text block as described above
	 */
	public RollingTextBlock roll(String nextText) {
		int currentLength = this.currentText.length();
		List<Annotation<?>> annotations = new ArrayList<>();
		for (Annotation<?> annotation : this.getAnnotations()) {
			int newEnd = annotation.getEnd() - currentLength;
			if (newEnd > 0) {
				int newStart = annotation.getStart() - currentLength;
				if (newStart < 0)
					newStart = 0;
				Annotation<?> newAnnotation = annotation.getAnnotation(newStart, newEnd);
				annotations.add(newAnnotation);
				if (LOG.isTraceEnabled()) {
					LOG.trace("Moved " + annotation + " to " + newStart + ", " + newEnd);
				}
			} else {
				if (LOG.isTraceEnabled()) {
					LOG.trace("Removed annotation " + annotation + ", newEnd = " + newEnd);
				}
			}
		}
		RollingTextBlock textBlock = new RollingTextBlock(this.currentText, this.nextText, nextText, annotations);

		return textBlock;
	}

	public String getPrevText() {
		return prevText;
	}

	public String getCurrentText() {
		return currentText;
	}

	public String getNextText() {
		return nextText;
	}
}
