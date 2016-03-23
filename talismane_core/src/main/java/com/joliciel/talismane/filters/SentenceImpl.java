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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class SentenceImpl implements Sentence {
	private static final Log LOG = LogFactory.getLog(SentenceImpl.class);

	private String text;
	private TreeMap<Integer, String> originalTextSegments = new TreeMap<Integer, String>();
	private List<Integer> originalIndexes = null;
	private boolean complete = true;
	TreeMap<Integer, Integer> newlines = new TreeMap<Integer, Integer>();
	private String fileName = "";
	private File file = null;
	private int startLineNumber = -1;
	private String leftoverOriginalText;
	private String outputDivider = "";

	private List<SentenceTag<?>> sentenceTags = new ArrayList<SentenceTag<?>>();

	@Override
	public String getText() {
		return this.text;
	}

	@Override
	public void setText(String text) {
		this.text = text;
	}

	@Override
	public int getOriginalIndex(int index) {
		if (originalIndexes == null)
			return index;
		if (index == originalIndexes.size())
			return originalIndexes.get(index - 1) + 1;
		if (index > originalIndexes.size())
			return -1;
		return originalIndexes.get(index);
	}

	@Override
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

	@Override
	public Map<Integer, String> getOriginalTextSegments() {
		return this.originalTextSegments;
	}

	@Override
	public void addOriginalTextSegment(int index, String segment) {
		if (LOG.isTraceEnabled())
			LOG.trace("Adding raw segment at index " + index + ": " + segment);
		String existingText = this.originalTextSegments.get(index);
		if (existingText == null) {
			this.originalTextSegments.put(index, segment);
		} else {
			this.originalTextSegments.put(index, existingText + outputDivider + segment);
		}
	}

	void setOriginalTextSegments(TreeMap<Integer, String> originalTextSegments) {
		this.originalTextSegments = originalTextSegments;
	}

	public List<Integer> getOriginalIndexes() {
		return originalIndexes;
	}

	@Override
	public void addOriginalIndex(int originalIndex) {
		if (this.originalIndexes == null)
			this.originalIndexes = new ArrayList<Integer>();
		this.originalIndexes.add(originalIndex);
	}

	@Override
	public boolean isComplete() {
		return complete;
	}

	@Override
	public void setComplete(boolean complete) {
		this.complete = complete;
	}

	@Override
	public int getLineNumber(int originalIndex) {
		Entry<Integer, Integer> lastLineEntry = this.newlines.floorEntry(originalIndex);
		if (lastLineEntry != null)
			return lastLineEntry.getValue();
		return -1;
	}

	@Override
	public int getColumnNumber(int originalIndex) {
		Integer lastLineObj = this.newlines.floorKey(originalIndex);
		if (lastLineObj != null)
			return (originalIndex - lastLineObj.intValue()) + 1;
		return -1;
	}

	@Override
	public void addNewline(int originalIndex, int lineNumber) {
		this.newlines.put(originalIndex, lineNumber);
	}

	@Override
	public Map<Integer, Integer> getNewlines() {
		return newlines;
	}

	@Override
	public String getFileName() {
		return fileName;
	}

	@Override
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	@Override
	public Entry<Integer, String> getPrecedingOriginalTextSegment(int index) {
		return this.originalTextSegments.floorEntry(index);
	}

	@Override
	public String getRawInput(int startIndex, int endIndex) {
		SortedMap<Integer, String> containedSegments = originalTextSegments.subMap(startIndex + 1, endIndex + 1);
		String rawInput = null;
		if (containedSegments.size() > 0) {
			StringBuilder sb = new StringBuilder();
			boolean firstSegment = true;
			for (String segment : containedSegments.values()) {
				if (!firstSegment)
					sb.append(outputDivider);
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

	@Override
	public int getStartLineNumber() {
		return startLineNumber;
	}

	@Override
	public void setStartLineNumber(int startLineNumber) {
		this.startLineNumber = startLineNumber;
	}

	@Override
	public File getFile() {
		return file;
	}

	@Override
	public void setFile(File file) {
		this.file = file;
	}

	@Override
	public String getLeftoverOriginalText() {
		return leftoverOriginalText;
	}

	@Override
	public void setLeftoverOriginalText(String leftoverOriginalText) {
		if (LOG.isTraceEnabled())
			LOG.trace("setLeftoverOriginalText: " + leftoverOriginalText);
		this.leftoverOriginalText = leftoverOriginalText;
	}

	@Override
	public List<SentenceTag<?>> getSentenceTags() {
		return sentenceTags;
	}

	@Override
	public String getOutputDivider() {
		return outputDivider;
	}

	@Override
	public void setOutputDivider(String outputDivider) {
		this.outputDivider = outputDivider;
	}
}
