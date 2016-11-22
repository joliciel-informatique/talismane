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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.tokeniser.TokenAttribute;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

public class SentenceHolder {

	private static final Logger LOG = LoggerFactory.getLogger(SentenceHolder.class);
	private TreeSet<Integer> sentenceBoundaries = new TreeSet<Integer>();
	private static final Pattern duplicateWhiteSpacePattern = Pattern.compile("[" + Sentence.WHITE_SPACE + "\n\r]{2,}", Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern openingWhiteSpacePattern = Pattern.compile("\\A([" + Sentence.WHITE_SPACE + "\n\r]+)", Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern closingWhiteSpacePattern = Pattern.compile("([" + Sentence.WHITE_SPACE + "\n\r]+)\\z", Pattern.UNICODE_CHARACTER_CLASS);

	private TIntObjectMap<List<SentenceTag<?>>> tagStarts = new TIntObjectHashMap<List<SentenceTag<?>>>();
	private TIntObjectMap<List<SentenceTag<?>>> tagEnds = new TIntObjectHashMap<List<SentenceTag<?>>>();
	private String processedText;

	private final List<Integer> originalIndexes = new ArrayList<>();
	private TreeMap<Integer, String> originalTextSegments = new TreeMap<Integer, String>();
	private final TreeMap<Integer, Integer> newlines = new TreeMap<Integer, Integer>();
	private final List<SentenceTag<?>> sentenceTags = new ArrayList<SentenceTag<?>>();
	private String fileName = "";
	private File file = null;

	private final TalismaneSession session;

	public SentenceHolder(TalismaneSession session) {
		this.session = session;
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
		}

		List<Sentence> sentences = new ArrayList<Sentence>();

		int currentIndex = 0;
		boolean haveLeftOvers = this.processedText.length() > 0;
		if (this.sentenceBoundaries.size() > 0) {
			haveLeftOvers = false;
			int lastSentenceBoundary = this.sentenceBoundaries.descendingIterator().next();
			if (lastSentenceBoundary < this.processedText.length() - 1) {
				haveLeftOvers = true;
			}
			if (LOG.isTraceEnabled()) {
				LOG.trace("haveLeftOvers? " + lastSentenceBoundary + " < " + (this.processedText.length() - 1) + " = " + haveLeftOvers);
			}
		}

		int lastOriginalTextInsertionProcessed = -1;
		int lastTagStartProcessed = -1;
		int lastTagEndProcessed = -1;

		List<Integer> allBoundaries = new ArrayList<Integer>(this.sentenceBoundaries);
		if (haveLeftOvers)
			allBoundaries.add(this.processedText.length() - 1);

		Map<Integer, List<SentenceTag<?>>> sentenceTagPositions = new HashMap<Integer, List<SentenceTag<?>>>();
		for (SentenceTag<?> sentenceTag : this.sentenceTags) {
			List<SentenceTag<?>> sentenceTagsAtPos = sentenceTagPositions.get(sentenceTag.getStartIndex());
			if (sentenceTagsAtPos == null) {
				sentenceTagsAtPos = new ArrayList<SentenceTag<?>>();
				sentenceTagPositions.put(sentenceTag.getStartIndex(), sentenceTagsAtPos);
			}
			sentenceTagsAtPos.add(sentenceTag);
		}

		for (int sentenceBoundary : allBoundaries) {
			boolean isLeftover = haveLeftOvers && sentenceBoundary == this.processedText.length() - 1;

			int leftOverTextLength = 0;
			String text = "";
			List<Integer> originalIndexes = new ArrayList<>();
			TreeMap<Integer, String> originalTextSegments = new TreeMap<>();
			List<SentenceTag<?>> sentenceTags = new ArrayList<>();

			if (leftover != null) {
				leftOverTextLength = leftover.getText().length();
				text = leftover.getText() + this.processedText.subSequence(currentIndex, sentenceBoundary + 1).toString();
				originalIndexes.addAll(leftover.getOriginalIndexes());
				originalTextSegments.putAll(leftover.getOriginalTextSegments());
				sentenceTags.addAll(leftover.getSentenceTags());

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

					if (this.tagStarts.containsKey(i)) {
						for (SentenceTag<?> tag : this.tagStarts.get(i)) {
							sentenceTags.add(tag.clone(sb.length()));
						}
						lastTagStartProcessed = i;
					}

					if (this.tagEnds.containsKey(i)) {
						for (SentenceTag<?> tag : this.tagEnds.get(i)) {
							SentenceTag<?> myTag = null;
							for (int k = sentenceTags.size() - 1; k >= 0; k--) {
								SentenceTag<?> sentenceTag = sentenceTags.get(k);
								if (sentenceTag.getAttribute().equals(tag.getAttribute()) && sentenceTag.getValue().equals(tag.getValue())
										&& sentenceTag.getEndIndex() < 0) {
									myTag = sentenceTag;
									break;
								}
							}
							if (myTag == null) {
								myTag = tag.clone(0);
							}
							myTag.setEndIndex(sb.length());
						}
						lastTagEndProcessed = i;
					}

					i++;
				}

				if (appendLetter)
					sb.append(text.charAt(j));
			}

			for (SentenceTag<?> sentenceTag : sentenceTags) {
				if (sentenceTag.getEndIndex() < 0)
					sentenceTag.setEndIndex(sb.length());
			}

			Sentence sentence = new Sentence(sb.toString(), originalTextSegments, originalIndexes, !isLeftover, this.newlines, sentenceTags, fileName, file,
					session);

			if (LOG.isTraceEnabled()) {
				LOG.trace("sentence.setText |" + sentence.getText() + "|");
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

		TIntObjectMap<List<SentenceTag<?>>> leftoverTagStarts = new TIntObjectHashMap<List<SentenceTag<?>>>();
		TIntIterator iTagStarts = tagStarts.keySet().iterator();
		while (iTagStarts.hasNext()) {
			int i = iTagStarts.next();
			if (i > lastTagStartProcessed)
				leftoverTagStarts.put(i, tagStarts.get(i));
		}
		tagStarts = leftoverTagStarts;

		TIntObjectMap<List<SentenceTag<?>>> leftoverTagEnds = new TIntObjectHashMap<List<SentenceTag<?>>>();
		TIntIterator iTagEnds = tagEnds.keySet().iterator();
		while (iTagEnds.hasNext()) {
			int i = iTagEnds.next();
			if (i > lastTagEndProcessed)
				leftoverTagEnds.put(i, tagEnds.get(i));
		}
		tagEnds = leftoverTagEnds;

		return sentences;
	}

	/**
	 * Indicate that a tag starts at this position.
	 */
	public <T> void addTagStart(String attribute, TokenAttribute<T> value, int position) {
		List<SentenceTag<?>> tagStartsAtPos = this.tagStarts.get(position);
		if (tagStartsAtPos == null) {
			tagStartsAtPos = new ArrayList<SentenceTag<?>>();
			tagStarts.put(position, tagStartsAtPos);
		}
		tagStartsAtPos.add(new SentenceTag<T>(0, attribute, value));
	}

	/**
	 * Indicate that a tag ends at this position.
	 */
	public <T> void addTagEnd(String attribute, TokenAttribute<T> value, int position) {
		List<SentenceTag<?>> tagEndsAtPos = this.tagEnds.get(position);
		if (tagEndsAtPos == null) {
			tagEndsAtPos = new ArrayList<SentenceTag<?>>();
			tagEnds.put(position, tagEndsAtPos);
		}
		tagEndsAtPos.add(new SentenceTag<T>(0, attribute, value));
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

	public List<SentenceTag<?>> getSentenceTags() {
		return sentenceTags;
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
}
