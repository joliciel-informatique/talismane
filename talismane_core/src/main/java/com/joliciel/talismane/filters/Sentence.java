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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneSession;

/**
 * A sentence detected by the sentence detector, including information about the
 * original location of the sentence in the stream being processed, and about
 * any text in the original stream that has been marked for raw inclusion in
 * Talismane's output.
 * 
 * @author Assaf Urieli
 *
 */
public class Sentence {
	public static final String WHITE_SPACE = "\\s\ufeff";

	private static final Logger LOG = LoggerFactory.getLogger(Sentence.class);

	private String text;
	private TreeMap<Integer, String> originalTextSegments = new TreeMap<Integer, String>();
	private List<Integer> originalIndexes = null;
	private boolean complete = true;
	TreeMap<Integer, Integer> newlines = new TreeMap<Integer, Integer>();
	private String fileName = "";
	private File file = null;
	private int startLineNumber = -1;
	private String leftoverOriginalText;

	protected final TalismaneSession talismaneSession;

	/**
	 * Create a sentence with an empty string for text.
	 * 
	 * @param talismaneSession
	 */
	public Sentence(TalismaneSession talismaneSession) {
		this("", talismaneSession);
	}

	public Sentence(String text, TalismaneSession talismaneSession) {
		this.talismaneSession = talismaneSession;
		this.text = text;
	}

	private List<SentenceTag<?>> sentenceTags = new ArrayList<SentenceTag<?>>();

	/**
	 * The sentence text.
	 */

	public String getText() {
		return this.text;
	}

	public void setText(String text) {
		this.text = text;
	}

	/**
	 * Get the original text index of any character index within this sentence.
	 */

	public int getOriginalIndex(int index) {
		if (originalIndexes == null)
			return index;
		if (index == originalIndexes.size())
			return originalIndexes.get(index - 1) + 1;
		if (index > originalIndexes.size())
			return -1;
		return originalIndexes.get(index);
	}

	/**
	 * Get the sentence text index corresponding to the first position following
	 * the original index provided.
	 */

	public int getIndex(int originalIndex) {
		if (originalIndexes == null)
			return originalIndex;
		int index = -1;
		for (int i = 0; i < originalIndexes.size(); i++) {
			if (originalIndexes.get(index) >= originalIndex) {
				index = i;
				break;
			}
		}
		return index;
	}

	/**
	 * A map giving any original text segments marked for output. The integer
	 * gives the index before which the segment needs to be inserted in the
	 * processed sentence text. The string gives the actual segment to be
	 * inserted.
	 */

	public Map<Integer, String> getOriginalTextSegments() {
		return this.originalTextSegments;
	}

	/**
	 * Add a raw text segment at a given position.
	 */

	public void addOriginalTextSegment(int index, String segment) {
		if (LOG.isTraceEnabled())
			LOG.trace("Adding raw segment at index " + index + ": " + segment);
		String existingText = this.originalTextSegments.get(index);
		if (existingText == null) {
			this.originalTextSegments.put(index, segment);
		} else {
			this.originalTextSegments.put(index, existingText + talismaneSession.getOutputDivider() + segment);
		}
	}

	void setOriginalTextSegments(TreeMap<Integer, String> originalTextSegments) {
		this.originalTextSegments = originalTextSegments;
	}

	public List<Integer> getOriginalIndexes() {
		return originalIndexes;
	}

	/**
	 * Add a new original index to the current sentence. These need to be added
	 * sequentially for every single index in the current sentence.
	 */

	public void addOriginalIndex(int originalIndex) {
		if (this.originalIndexes == null)
			this.originalIndexes = new ArrayList<Integer>();
		this.originalIndexes.add(originalIndex);
	}

	/**
	 * Is this a complete sentence? Default is true.
	 */

	public boolean isComplete() {
		return complete;
	}

	public void setComplete(boolean complete) {
		this.complete = complete;
	}

	/**
	 * Returns the line number corresponding to a particular original index
	 * inside this sentence, starting at 1.
	 */

	public int getLineNumber(int originalIndex) {
		Entry<Integer, Integer> lastLineEntry = this.newlines.floorEntry(originalIndex);
		if (lastLineEntry != null)
			return lastLineEntry.getValue();
		return -1;
	}

	/**
	 * Returns the column number corresponding to a particular original index
	 * inside this sentence, starting at 1.
	 */

	public int getColumnNumber(int originalIndex) {
		Integer lastLineObj = this.newlines.floorKey(originalIndex);
		if (lastLineObj != null)
			return (originalIndex - lastLineObj.intValue()) + 1;
		return -1;
	}

	/**
	 * Indicate that a new line was found at a given original index in this
	 * sentence holder, and gives the line number.
	 */

	public void addNewline(int originalIndex, int lineNumber) {
		this.newlines.put(originalIndex, lineNumber);
	}

	/**
	 * A map giving original index to line number mappings, for all lines
	 * contained within this sentence.
	 */

	public Map<Integer, Integer> getNewlines() {
		return newlines;
	}

	/**
	 * The file name containing this sentence.
	 */

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * Get the closest original text segment preceding a certain index.
	 */

	public Entry<Integer, String> getPrecedingOriginalTextSegment(int index) {
		return this.originalTextSegments.floorEntry(index);
	}

	/**
	 * Get all raw input strictly after a given startIndex, and before or at a
	 * given end index, concatenated together.
	 */

	public String getRawInput(int startIndex, int endIndex) {
		SortedMap<Integer, String> containedSegments = originalTextSegments.subMap(startIndex + 1, endIndex + 1);
		String rawInput = null;
		if (containedSegments.size() > 0) {
			StringBuilder sb = new StringBuilder();
			boolean firstSegment = true;
			for (String segment : containedSegments.values()) {
				if (!firstSegment)
					sb.append(talismaneSession.getOutputDivider());
				sb.append(segment);
				firstSegment = false;
			}
			rawInput = sb.toString();
		}
		return rawInput;
	}

	@Override
	public String toString() {
		return "SentenceImpl [text=" + text + "]";
	}

	/**
	 * The line number on which this sentence started, when reading from a
	 * previously analysed corpus (one token per line).
	 */

	public int getStartLineNumber() {
		return startLineNumber;
	}

	public void setStartLineNumber(int startLineNumber) {
		this.startLineNumber = startLineNumber;
	}

	/**
	 * The file containing this sentence.
	 */

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	/**
	 * Is there any leftover original text?
	 */

	public String getLeftoverOriginalText() {
		return leftoverOriginalText;
	}

	/**
	 * Set original text marked for output from a previous sentence holder.
	 */

	public void setLeftoverOriginalText(String leftoverOriginalText) {
		if (LOG.isTraceEnabled())
			LOG.trace("setLeftoverOriginalText: " + leftoverOriginalText);
		this.leftoverOriginalText = leftoverOriginalText;
	}

	/**
	 * A list of tags added to this sentence.
	 */

	public List<SentenceTag<?>> getSentenceTags() {
		return sentenceTags;
	}

}
