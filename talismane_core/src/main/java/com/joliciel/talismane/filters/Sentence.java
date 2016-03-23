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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A sentence detected by the sentence detector, including information about the
 * original location of the sentence in the stream being processed, and about
 * any text in the original stream that has been marked for raw inclusion in
 * Talismane's output.
 * 
 * @author Assaf Urieli
 *
 */
public interface Sentence {
	public static final String WHITE_SPACE = "\\s\ufeff";

	/**
	 * The sentence text.
	 */
	String getText();

	void setText(String text);

	/**
	 * Get the original text index of any character index within this sentence.
	 */
	int getOriginalIndex(int index);

	/**
	 * Get the sentence text index corresponding to the first position following
	 * the original index provided.
	 */
	int getIndex(int originalIndex);

	/**
	 * A map giving any original text segments marked for output. The integer
	 * gives the index before which the segment needs to be inserted in the
	 * processed sentence text. The string gives the actual segment to be
	 * inserted.
	 */
	Map<Integer, String> getOriginalTextSegments();

	/**
	 * Add a raw text segment at a given position.
	 */
	void addOriginalTextSegment(int index, String segment);

	/**
	 * Get the closest original text segment preceding a certain index.
	 */
	Entry<Integer, String> getPrecedingOriginalTextSegment(int index);

	/**
	 * Get all raw input strictly after a given startIndex, and before or at a
	 * given end index, concatenated together.
	 */
	String getRawInput(int startIndex, int endIndex);

	/**
	 * Add a new original index to the current sentence. These need to be added
	 * sequentially for every single index in the current sentence.
	 */
	void addOriginalIndex(int originalIndex);

	/**
	 * Is this a complete sentence? Default is true.
	 */
	boolean isComplete();

	void setComplete(boolean complete);

	/**
	 * Returns the line number corresponding to a particular original index
	 * inside this sentence, starting at 1.
	 */
	int getLineNumber(int originalIndex);

	/**
	 * Returns the column number corresponding to a particular original index
	 * inside this sentence, starting at 1.
	 */
	int getColumnNumber(int originalIndex);

	/**
	 * Indicate that a new line was found at a given original index in this
	 * sentence holder, and gives the line number.
	 */
	void addNewline(int originalIndex, int lineNumber);

	/**
	 * A map giving original index to line number mappings, for all lines
	 * contained within this sentence.
	 */
	Map<Integer, Integer> getNewlines();

	/**
	 * The file name containing this sentence.
	 */
	String getFileName();

	void setFileName(String fileName);

	/**
	 * The file containing this sentence.
	 */
	File getFile();

	void setFile(File file);

	/**
	 * The line number on which this sentence started, when reading from a
	 * previously analysed corpus (one token per line).
	 */
	public int getStartLineNumber();

	public void setStartLineNumber(int startLineNumber);

	/**
	 * Set original text marked for output from a previous sentence holder.
	 */
	void setLeftoverOriginalText(String originalText);

	/**
	 * Is there any leftover original text?
	 */
	String getLeftoverOriginalText();

	/**
	 * A list of tags added to this sentence.
	 */
	public List<SentenceTag<?>> getSentenceTags();

	/**
	 * A string inserted between outputs (such as a newline).
	 */
	public String getOutputDivider();

	public void setOutputDivider(String outputDivider);
}
