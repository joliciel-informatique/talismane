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

import java.util.Map;
import java.util.Map.Entry;

/**
 * A sentence detected by the sentence detector,
 * including information about the original location of the sentence in the
 * stream being processed, and about any text in the original stream
 * that has been marked for raw inclusion in Talismane's output.
 * @author Assaf Urieli
 *
 */
public interface Sentence {
	public static final String WHITE_SPACE = " \t\\x0B\f\u00a0\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2007\u2008\u2009\u200a\u200b\u202f\u205f\u3000\ufeff";
	
	/**
	 * The sentence text.
	 * @return
	 */
	String getText();
	void setText(String text);
	
	/**
	 * Get the original text index of any character index within this sentence.
	 * @param index
	 * @return
	 */
	int getOriginalIndex(int index);
	
	/**
	 * Get the sentence text index corresponding to the first position following
	 * the original index provided.
	 * @param originalIndex
	 * @return
	 */
	int getIndex(int originalIndex);
	
	/**
	 * A map giving any original text segments marked for output.
	 * The integer gives the index before which the segment needs to be inserted in the processed sentence text.
	 * The string gives the actual segment to be inserted.
	 * @return
	 */
	Map<Integer, String> getOriginalTextSegments();
	
	/**
	 * Get the closest original text segment preceding a certain index.
	 * @param index
	 * @return
	 */
	Entry<Integer,String> getPrecedingOriginalTextSegment(int index);
	
	/**
	 * Add a new original index to the current sentence.
	 * These need to be added sequentially for every single index in the current sentence.
	 */
	void addOriginalIndex(int originalIndex);
	
	/**
	 * Is this a complete sentence? Default is true.
	 * @return
	 */
	boolean isComplete();
	void setComplete(boolean complete);

	/**
	 * Returns the line number corresponding to a particular original index inside this sentence,
	 * starting at 1.
	 * @param originalIndex
	 * @return
	 */
	int getLineNumber(int originalIndex);
	
	/**
	 * Returns the column number corresponding to a particular original index inside this sentence,
	 * starting at 1.
	 * @param originalIndex
	 * @return
	 */
	int getColumnNumber(int originalIndex);
	
	/**
	 * Indicate that a new line was found at a given original index in this sentence holder,
	 * and gives the line number.
	 * @param originalIndex
	 * @param lineNumber
	 */
	void addNewline(int originalIndex, int lineNumber);
	
	/**
	 * A map giving original index to line number mappings, for all lines contained within this sentence.
	 * @return
	 */
	Map<Integer, Integer> getNewlines();
	
	/**
	 * The file name containing this sentence.
	 * @return
	 */
	String getFileName();
	void setFileName(String fileName);
}
