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
package com.joliciel.talismane.sentenceDetector;

import java.util.List;
import java.util.regex.Pattern;

import com.joliciel.talismane.tokeniser.filters.TokenFilter;

/**
 * Detect sentence boundaries within a textual block.
 * The linefeed character will always be considered to be a sentence boundary.
 * Therefore, filters prior to the sentence detector should remove any arbitrary linefeeds,
 * and insert linefeeds instead of any other patterns indicating a sentence boundary (e.g. XML tags).
 * @author Assaf Urieli
 *
 */
public interface SentenceDetector {
	/**
	 * A list of possible sentence-end boundaries.
	 */
	public static final Pattern POSSIBLE_BOUNDARIES = Pattern.compile("[\\.\\?\\!\"\\)\\]\\}»—―”″\n]");
	
	/**
	 * Detect sentences within a particular textual block, given the previous and next textual blocks.
	 * @param prevText the previous textual block
	 * @param text the current textual block
	 * @param moreText the following textual block
	 * @return a List of integers marking the index of the last character in each sentence within the current textual block. The index is relative to the current block only (text), not the full context (prevText + text + nextText).
	 */
	public List<Integer> detectSentences(String prevText, String text, String moreText);
	
	/**
	 * Token filters mark certain portions of the raw text as entire tokens -
	 * a sentence break will never be detected inside such a token.
	 * @return
	 */
	public List<TokenFilter> getTokenFilters();

	public void addTokenFilter(TokenFilter filter);
}
