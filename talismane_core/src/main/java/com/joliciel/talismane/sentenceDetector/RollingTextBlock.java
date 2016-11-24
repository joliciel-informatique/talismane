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
package com.joliciel.talismane.sentenceDetector;

import com.joliciel.talismane.AnnotatedText;

/**
 * A block of text on which we attempt to detect sentences. Sentences are only
 * detected inside the text, never the prevText or nextText.
 * 
 * @author Assaf Urieli
 *
 */
public class RollingTextBlock extends AnnotatedText {
	private final String prevText;
	private final String currentText;
	private final String nextText;

	public RollingTextBlock(String prevText, String currentText, String nextText) {
		super(prevText + currentText + nextText);
		this.prevText = prevText;
		this.currentText = currentText;
		this.nextText = nextText;
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
