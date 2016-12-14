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
package com.joliciel.talismane.rawText;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneSession;

class SentenceHolder {

	private static final Logger LOG = LoggerFactory.getLogger(SentenceHolder.class);
	private TreeSet<Integer> sentenceBoundaries = new TreeSet<Integer>();
	private static final Pattern duplicateWhiteSpacePattern = Pattern.compile("[" + Sentence.WHITE_SPACE + "\n\r]{2,}", Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern openingWhiteSpacePattern = Pattern.compile("\\A([" + Sentence.WHITE_SPACE + "\n\r]+)", Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern closingWhiteSpacePattern = Pattern.compile("([" + Sentence.WHITE_SPACE + "\n\r]+)\\z", Pattern.UNICODE_CHARACTER_CLASS);
	private String processedText = "";

	private final int originalStartIndex;
	private final List<Integer> originalIndexes = new ArrayList<>();
	private TreeMap<Integer, String> originalTextSegments = new TreeMap<Integer, String>();
	private final TreeMap<Integer, Integer> newlines = new TreeMap<Integer, Integer>();
	private String fileName = "";
	private File file = null;
	private final boolean endOfBlock;

	private final TalismaneSession session;

	/**
	 * Construct a sentence holder.
	 * 
	 * @param session
	 * @param originalStartIndex
	 *            where does this sentence holder start in the original text
	 * @param endOfBlock
	 *            if this is an end-of-block, then any leftover text will be
	 *            added to a final complete sentence
	 */
	public SentenceHolder(TalismaneSession session, int originalStartIndex, boolean endOfBlock) {
		this.session = session;
		this.originalStartIndex = originalStartIndex;
		this.endOfBlock = endOfBlock;
	}

	public Set<Integer> getSentenceBoundaries() {
		return sentenceBoundaries;
	}

	/**
	 * Add a sentence boundary to this sentence holder.
	 */
	public void addSentenceBoundary(int boundary) {
		this.sentenceBoundaries.add(boundary);
	}

	/**
	 * Based on the sentence boundaries added, return all the sentences produced
	 * by this sentence holder. If there is any text left over, the last
	 * sentence will be marked as not complete. After this is called,
	 * {@link #getOriginalTextSegments()} will only return leftover original
	 * text segments that have not yet been assigned to sentences in the current
	 * list.
	 * 
	 * @param leftover
	 *            an incomplete sentence returned by the previous sentence
	 *            holder.
	 */
	public List<Sentence> getDetectedSentences(Sentence leftover) {
		if (LOG.isTraceEnabled()) {
			LOG.trace("getDetectedSentences. leftover=" + leftover);
			LOG.trace("processedText: " + processedText);
		}

		List<Sentence> sentences = new ArrayList<Sentence>();

		int currentIndex = 0;

		int lastOriginalTextInsertionProcessed = -1;

		List<Integer> allBoundaries = new ArrayList<Integer>();
		for (int sentenceBoundary : sentenceBoundaries) {
			if (sentenceBoundary < this.processedText.length())
				allBoundaries.add(sentenceBoundary);
		}

		// is there any text leftover, to construct an incomplete sentence after
		// the last proper sentence
		boolean haveLeftOvers = this.processedText.length() > 0;
		if (allBoundaries.size() > 0) {
			haveLeftOvers = false;
			int lastSentenceBoundary = allBoundaries.get(allBoundaries.size() - 1);
			if (lastSentenceBoundary < this.processedText.length() - 1) {
				haveLeftOvers = true;
			}
			if (LOG.isTraceEnabled()) {
				LOG.trace("haveLeftOvers? " + lastSentenceBoundary + " < " + (this.processedText.length() - 1) + " = " + haveLeftOvers);
			}
		}

		// if there is leftover text, we'll add a fake "boundary" to construct
		// the incomplete sentence
		if (haveLeftOvers)
			allBoundaries.add(this.processedText.length() - 1);

		for (int sentenceBoundary : allBoundaries) {
			// is this the final incomplete sentence?
			boolean isLeftover = (!endOfBlock) && (haveLeftOvers && sentenceBoundary == this.processedText.length() - 1);

			int leftOverTextLength = 0;
			String text = "";
			List<Integer> originalIndexes = new ArrayList<>();
			TreeMap<Integer, String> originalTextSegments = new TreeMap<>();

			if (leftover != null) {
				leftOverTextLength = leftover.getText().length();
				text = leftover.getText() + this.processedText.subSequence(currentIndex, sentenceBoundary + 1).toString();
				originalIndexes.addAll(leftover.getOriginalIndexes());
				originalTextSegments.putAll(leftover.getOriginalTextSegments());

				leftover = null;
			} else {
				text = this.processedText.subSequence(currentIndex, sentenceBoundary + 1).toString();
			}

			// handle trim & duplicate white space here
			Matcher matcherOpeningWhiteSpace = openingWhiteSpacePattern.matcher(text);
			int openingWhiteSpaceEnd = 0;
			if (matcherOpeningWhiteSpace.find()) {
				openingWhiteSpaceEnd = matcherOpeningWhiteSpace.end(1);
			}

			int closingWhiteSpaceStart = text.length();
			if (!isLeftover) {
				Matcher matcherClosingWhiteSpace = closingWhiteSpacePattern.matcher(text);
				if (matcherClosingWhiteSpace.find()) {
					closingWhiteSpaceStart = matcherClosingWhiteSpace.start(1);
				}
			}

			Matcher matcherDuplicateWhiteSpace = duplicateWhiteSpacePattern.matcher(text);
			Set<Integer> duplicateWhiteSpace = new HashSet<Integer>();
			while (matcherDuplicateWhiteSpace.find()) {
				// remove all white space barring the first
				for (int i = matcherDuplicateWhiteSpace.start() + 1; i < matcherDuplicateWhiteSpace.end(); i++) {
					duplicateWhiteSpace.add(i);
				}
			}

			StringBuilder sb = new StringBuilder();
			int i = currentIndex;
			for (int j = 0; j < text.length(); j++) {
				boolean appendLetter = false;
				if (j < openingWhiteSpaceEnd) {
					// do nothing
				} else if (j >= closingWhiteSpaceStart) {
					// do nothing
				} else if (duplicateWhiteSpace.contains(j)) {
					// do nothing
				} else {
					appendLetter = true;
				}

				if (j >= leftOverTextLength) {
					// if we're past the leftovers and onto the new stuff
					if (appendLetter)
						originalIndexes.add(this.originalIndexes.get(i));

					if (this.originalTextSegments.containsKey(i)) {
						originalTextSegments.put(sb.length(), this.originalTextSegments.get(i));
						lastOriginalTextInsertionProcessed = i;
					}

					i++;
				}

				if (appendLetter)
					sb.append(text.charAt(j));
			}

			Sentence sentence = new Sentence(sb.toString(), originalTextSegments, originalIndexes, !isLeftover, this.newlines, fileName, file, session);

			if (LOG.isTraceEnabled()) {
				LOG.trace("sentence.setText |" + sentence.getText() + "| complete? " + sentence.isComplete());
			}

			sentences.add(sentence);
			currentIndex = sentenceBoundary + 1;
		}

		// remove any original text segments already processed
		TreeMap<Integer, String> leftoverOriginalTextSegments = new TreeMap<Integer, String>();
		for (int i : this.originalTextSegments.keySet()) {
			if (i > lastOriginalTextInsertionProcessed) {
				leftoverOriginalTextSegments.put(i, this.originalTextSegments.get(i));
			}
		}
		this.originalTextSegments = leftoverOriginalTextSegments;

		return sentences;
	}

	public String getProcessedText() {
		return processedText;
	}

	public void setProcessedText(String processedText) {
		this.processedText = processedText;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public TreeMap<Integer, String> getOriginalTextSegments() {
		return originalTextSegments;
	}

	public TreeMap<Integer, Integer> getNewlines() {
		return newlines;
	}

	public void setSentenceBoundaries(TreeSet<Integer> sentenceBoundaries) {
		this.sentenceBoundaries = sentenceBoundaries;
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
			this.originalTextSegments.put(index, existingText + session.getOutputDivider() + segment);
		}
	}

	/**
	 * Indicate that a new line was found at a given original index in this
	 * sentence holder, and gives the line number.
	 */
	public void addNewline(int originalIndex, int lineNumber) {
		this.newlines.put(originalIndex, lineNumber);
	}

	public List<Integer> getOriginalIndexes() {
		return originalIndexes;
	}

	/**
	 * Add a new original index to the current sentence. These need to be added
	 * sequentially for every single index in the current sentence.
	 */
	public void addOriginalIndex(int originalIndex) {
		this.originalIndexes.add(originalIndex);
	}

	/**
	 * Get the original text index of any character index within this sentence.
	 */
	public int getOriginalIndex(int index) {
		if (originalIndexes.size() == 0)
			return index;
		if (index == originalIndexes.size())
			return originalIndexes.get(index - 1) + 1;
		if (index > originalIndexes.size())
			return -1;
		return originalIndexes.get(index);
	}

	/**
	 * Get the local index corresponding to a given original index.
	 * 
	 * @param originalIndex
	 * @return
	 */
	public int getIndex(int originalIndex) {
		int i = 0;
		for (; i < originalIndexes.size(); i++) {
			if (originalIndexes.get(i) >= originalIndex) {
				break;
			}
		}
		return i;
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
	 * The original index at which this sentence holder starts.
	 */
	public int getOriginalStartIndex() {
		return originalStartIndex;
	}
}
