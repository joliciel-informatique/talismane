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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

class SentenceImpl implements Sentence {
	private String text;
	private TreeMap<Integer, String> originalTextSegments = new TreeMap<Integer, String>();
	private List<Integer> originalIndexes = null;
	private boolean complete = true;
	TreeMap<Integer, Integer> newlines = new TreeMap<Integer, Integer>();
	private String fileName = "";
	
	@Override
	public String getText() {
		return this.text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public int getOriginalIndex(int index) {
		if (originalIndexes==null)
			return -1;
		if (index>=originalIndexes.size())
			return -1;
		return originalIndexes.get(index);
	}

	public int getIndex(int originalIndex) {
		if (originalIndexes==null)
			return -1;
		int index = -1;
		for (int i=0; i<originalIndexes.size(); i++) {
			if (originalIndexes.get(index)>=originalIndex) {
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

	public List<Integer> getOriginalIndexes() {
		return originalIndexes;
	}
	
	public void addOriginalIndex(int originalIndex) {
		if (this.originalIndexes==null)
			this.originalIndexes = new ArrayList<Integer>();
		this.originalIndexes.add(originalIndex);
	}

	public boolean isComplete() {
		return complete;
	}

	public void setComplete(boolean complete) {
		this.complete = complete;
	}

	@Override
	public int getLineNumber(int originalIndex) {
		Entry<Integer,Integer> lastLineEntry = this.newlines.floorEntry(originalIndex);
		if (lastLineEntry!=null)
			return lastLineEntry.getValue();
		return -1;
	}

	@Override
	public int getColumnNumber(int originalIndex) {
		Integer lastLineObj = this.newlines.floorKey(originalIndex);
		if (lastLineObj!=null)
			return originalIndex - lastLineObj.intValue();
		return -1;
	}

	@Override
	public void addNewline(int originalIndex, int lineNumber) {
		this.newlines.put(originalIndex, lineNumber);
	}

	public Map<Integer, Integer> getNewlines() {
		return newlines;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	@Override
	public Entry<Integer, String> getPrecedingOriginalTextSegment(int index) {
		return this.originalTextSegments.floorEntry(index);
	}

	@Override
	public String toString() {
		return "SentenceImpl [text=" + text + "]";
	}

}
