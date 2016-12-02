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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.AnnotatedText;
import com.joliciel.talismane.Annotation;
import com.joliciel.talismane.TalismaneSession;

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
	private final String context;

	private final Stack<Boolean> shouldProcessStack;
	private final Stack<Boolean> shouldOutputStack;
	private int originalTextIndex = 0;
	private String leftoverOutput = "";
	private int lineNumber = 2; // this is so that the line following the first
								// newline character will be given 2 (and the
								// initial line 1)

	private static Pattern newlinePattern = Pattern.compile("\r\n|[\r\n]");
	private int leftoverNewline = 0;
	private String fileName = "";
	private File file = null;
	private static final int NUM_CHARS = 30;

	private final TalismaneSession talismaneSession;

	/**
	 * Creates a new RollingTextBlock with prev, current and next all set to
	 * empty strings.
	 */
	public RollingTextBlock(TalismaneSession talismaneSession, boolean processByDefault) {
		super("", 0, Collections.emptyList());
		this.talismaneSession = talismaneSession;
		this.prevText = "";
		this.currentText = "";
		this.nextText = "";
		this.context = "";
		this.shouldProcessStack = new Stack<>();
		this.shouldProcessStack.push(processByDefault);
		this.shouldOutputStack = new Stack<>();
		this.shouldOutputStack.push(false);
	}

	private RollingTextBlock(RollingTextBlock predecessor, String nextText, List<Annotation<?>> annotations) {
		super(predecessor.nextText + nextText, predecessor.nextText.length(), annotations);
		this.prevText = predecessor.currentText;
		this.currentText = predecessor.nextText;
		this.nextText = nextText;
		this.context = this.prevText + this.currentText + this.nextText;

		this.talismaneSession = predecessor.talismaneSession;
		this.shouldOutputStack = predecessor.shouldOutputStack;
		this.shouldProcessStack = predecessor.shouldProcessStack;
		this.originalTextIndex = predecessor.originalTextIndex;
		this.leftoverOutput = predecessor.leftoverOutput;
		this.lineNumber = predecessor.lineNumber;
		this.leftoverNewline = predecessor.leftoverNewline;
		this.fileName = predecessor.fileName;
		this.file = predecessor.file;

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
		int prevLength = this.prevText.length();
		List<Annotation<?>> annotations = new ArrayList<>();
		for (Annotation<?> annotation : this.getAnnotations()) {
			int newStart = annotation.getStart() - prevLength;
			int newEnd = annotation.getEnd() - prevLength;
			if (newEnd > 0 || (newStart == 0 && newEnd == 0)) {
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
		RollingTextBlock textBlock = new RollingTextBlock(this, nextText, annotations);

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

	/**
	 * Processes the current text based on markers, and returns a
	 * SentenceHolder.
	 * 
	 * @return SentenceHolder to retrieve the sentences.
	 */
	public SentenceHolder processText() {
		LOG.debug("processText");
		List<Annotation<RawTextMarker>> annotations = this.getAnnotations(RawTextMarker.class);
		Map<Integer, List<Pair<Boolean, Annotation<RawTextMarker>>>> markMap = new TreeMap<>();
		int textStartPos = this.prevText.length();
		int textEndPos = this.prevText.length() + this.currentText.length();

		for (Annotation<RawTextMarker> annotation : annotations) {
			if (annotation.getStart() >= textStartPos && annotation.getStart() < textEndPos) {
				List<Pair<Boolean, Annotation<RawTextMarker>>> startMarks = markMap.get(annotation.getStart());
				if (startMarks == null) {
					startMarks = new ArrayList<>();
					markMap.put(annotation.getEnd(), startMarks);
				}
				startMarks.add(new ImmutablePair<Boolean, Annotation<RawTextMarker>>(true, annotation));
			}

			// if the matcher ends within the textblock
			// or if the matcher ends exactly on the textblock, and the
			// following text block is empty
			// we add the end match
			// the 2nd condition is to ensure we add the end match, since empty
			// blocks can never match anything
			if (annotation.getEnd() >= textStartPos && (annotation.getEnd() < textEndPos
					|| (annotation.getEnd() == textEndPos && this.currentText.length() > 0 && this.nextText.length() == 0))) {
				if (LOG.isTraceEnabled()) {
					LOG.trace("End in range: textStartPos " + textStartPos + ">= matcherEnd [[" + annotation.getEnd() + "]] < textEndPos " + textEndPos);
				}
				List<Pair<Boolean, Annotation<RawTextMarker>>> endMarks = markMap.get(annotation.getEnd());
				if (endMarks == null) {
					endMarks = new ArrayList<>();
					markMap.put(annotation.getEnd(), endMarks);
				}
				endMarks.add(new ImmutablePair<Boolean, Annotation<RawTextMarker>>(false, annotation));
			} else {
				if (LOG.isTraceEnabled()) {
					LOG.trace("End out of range: textStartPos " + textStartPos + ">= matcherEnd [[" + annotation.getEnd() + "]] < textEndPos " + textEndPos);
				}
			}
		}
		List<Pair<Boolean, Annotation<RawTextMarker>>> marks = new ArrayList<>();
		for (int key : markMap.keySet())
			marks.addAll(markMap.get(key));

		if (LOG.isTraceEnabled()) {
			LOG.trace("currentText: " + currentText.replace('\n', '¶').replace('\r', '¶'));
		}

		SentenceHolder sentenceHolder = new SentenceHolder(talismaneSession);

		// find any newlines
		sentenceHolder.addNewline(leftoverNewline, lineNumber - 1);

		Matcher matcher = newlinePattern.matcher(currentText);
		while (matcher.find()) {
			sentenceHolder.addNewline(originalTextIndex + matcher.end(), lineNumber++);
			leftoverNewline = originalTextIndex + matcher.end();
		}

		Map<Integer, Integer> insertionPoints = new TreeMap<Integer, Integer>();
		StringBuilder processedText = new StringBuilder();
		int currentPos = 0;
		int outputPos = 0;

		for (Pair<Boolean, Annotation<RawTextMarker>> mark : marks) {
			boolean isStart = mark.getLeft();
			Annotation<RawTextMarker> annotation = mark.getRight();
			RawTextMarker marker = annotation.getData();
			int position = isStart ? annotation.getStart() : annotation.getEnd();
			int relativePosition = position - this.prevText.length();
			if (LOG.isTraceEnabled()) {
				LOG.trace((isStart ? "Start " : "Stop ") + marker.getType() + " at " + position + ", relative pos: " + relativePosition);
				LOG.trace("Stack before: " + shouldProcessStack);
				LOG.trace("Text before: " + processedText.toString());
				LOG.trace("Added by filter: " + marker.getSource());
				LOG.trace("Match text: " + this.context.substring(annotation.getStart(), annotation.getEnd()).replace('\n', '¶').replace('\r', '¶'));
			}

			boolean shouldProcess = shouldProcessStack.peek();
			boolean shouldOutput = shouldOutputStack.peek();

			List<TextMarkerType> actions = new ArrayList<>();
			if (isStart) {
				switch (marker.getType()) {
				case SKIP:
					actions.add(TextMarkerType.PUSH_SKIP);
					break;
				case SENTENCE_BREAK:
					actions.add(TextMarkerType.SENTENCE_BREAK);
					break;
				case SPACE:
					actions.add(TextMarkerType.INSERT);
					actions.add(TextMarkerType.PUSH_SKIP);
					break;
				case REPLACE:
					actions.add(TextMarkerType.INSERT);
					actions.add(TextMarkerType.PUSH_SKIP);
					break;
				case OUTPUT:
					actions.add(TextMarkerType.PUSH_OUTPUT);
					actions.add(TextMarkerType.PUSH_SKIP);
					break;
				case INCLUDE:
					actions.add(TextMarkerType.PUSH_INCLUDE);
					break;
				case OUTPUT_START:
					actions.add(TextMarkerType.PUSH_OUTPUT);
					break;
				case STOP:
					actions.add(TextMarkerType.STOP);
					break;
				case NONE:
				case OUTPUT_STOP:
				case START:
					break;
				case TAG:
					actions.add(TextMarkerType.TAG_START);
					break;
				}
			} else {
				// end of annotation
				switch (marker.getType()) {
				case SKIP:
				case SPACE:
				case REPLACE:
					actions.add(TextMarkerType.POP_SKIP);
					break;
				case OUTPUT:
					actions.add(TextMarkerType.STOP_OUTPUT);
					actions.add(TextMarkerType.POP_SKIP);
					break;
				case INCLUDE:
					actions.add(TextMarkerType.POP_INCLUDE);
					break;
				case START:
					actions.add(TextMarkerType.START);
					break;
				case OUTPUT_STOP:
					actions.add(TextMarkerType.STOP_OUTPUT);
					break;
				case NONE:
				case OUTPUT_START:
				case SENTENCE_BREAK:
				case STOP:
					break;
				case TAG:
					actions.add(TextMarkerType.TAG_STOP);
					break;
				}
			}

			for (TextMarkerType action : actions) {
				switch (action) {
				case PUSH_SKIP:
					if (shouldProcess) {
						insertionPoints.put(processedText.length(), currentPos);
						processedText.append(currentText.substring(currentPos, relativePosition));
						if (shouldOutput) {
							outputPos = relativePosition;
						}
					}
					shouldProcessStack.push(false);
					break;
				case PUSH_OUTPUT:
					if (!shouldOutput && !shouldProcess) {
						outputPos = relativePosition;
					}
					shouldOutputStack.push(true);
					break;
				case PUSH_INCLUDE:
					if (!shouldProcess) {
						currentPos = relativePosition;
						if (shouldOutput) {
							String outputText = currentText.substring(outputPos, relativePosition);
							this.addOutputText(sentenceHolder, processedText.length(), outputText);
							outputPos = relativePosition;
						}
					}
					shouldProcessStack.push(true);
					break;
				case SPACE:
					if (shouldProcess) {
						insertionPoints.put(processedText.length(), currentPos);
						String leftoverText = currentText.substring(currentPos, relativePosition);
						processedText.append(leftoverText);
						currentPos = relativePosition;
						if (!leftoverText.endsWith(" ")) {
							insertionPoints.put(processedText.length(), currentPos);
							processedText.append(" ");
						}
					}
					break;
				case INSERT:
					if (shouldProcess) {
						insertionPoints.put(processedText.length(), currentPos);
						String leftoverText = currentText.substring(currentPos, relativePosition);
						processedText.append(leftoverText);
						currentPos = relativePosition;
						for (int i = 0; i < marker.getInsertionText().length(); i++) {
							insertionPoints.put(processedText.length() + i, currentPos);
						}
						if (LOG.isTraceEnabled())
							LOG.trace("Inserting: " + marker.getInsertionText());
						processedText.append(marker.getInsertionText());
					}
					break;
				case SENTENCE_BREAK: {
					String leftoverText = null;
					if (shouldProcess) {
						insertionPoints.put(processedText.length(), currentPos);
						leftoverText = currentText.substring(currentPos, relativePosition);
						processedText.append(leftoverText);
						currentPos = relativePosition;
					}

					// add the sentence boundary on the last character that was
					// actually added.
					sentenceHolder.addSentenceBoundary(processedText.length() - 1);
					if (LOG.isTraceEnabled()) {
						int boundary = processedText.length() - 1;
						if (boundary >= 0) {
							String string = null;
							int start1 = boundary - NUM_CHARS;

							if (start1 < 0)
								start1 = 0;
							String startString = processedText.substring(start1, boundary);

							String middleString = "" + processedText.charAt(boundary);

							string = startString + "[" + middleString + "]";
							string = string.replace('\n', '¶');
							LOG.trace("Adding sentence break at position " + boundary + ": " + string);
						}
					}
					if (shouldProcess) {
						if (!leftoverText.endsWith(" ")) {
							insertionPoints.put(processedText.length(), currentPos);
							processedText.append(" ");
						}
					}
					break;
				}
				case POP_SKIP:
				case POP_INCLUDE:
				case STOP:
				case START: {
					boolean wasProcessing = shouldProcess;
					boolean wasOutputting = shouldOutput && !shouldProcess;
					if (action == TextMarkerType.POP_SKIP || action == TextMarkerType.POP_INCLUDE) {
						shouldProcessStack.pop();
					} else if (action == TextMarkerType.STOP) {
						shouldProcessStack.pop();
						shouldProcessStack.push(false);
					} else if (action == TextMarkerType.START) {
						shouldProcessStack.pop();
						shouldProcessStack.push(true);
					}
					shouldProcess = shouldProcessStack.peek();
					shouldOutput = shouldOutput && !shouldProcess;

					if (wasProcessing && !shouldProcess) {
						insertionPoints.put(processedText.length(), currentPos);
						processedText.append(currentText.substring(currentPos, relativePosition));
					} else if (!wasProcessing && shouldProcess) {
						currentPos = relativePosition;
					} // shouldProcess?

					if (wasOutputting && (!shouldOutput || !shouldProcess)) {
						String outputText = currentText.substring(outputPos, relativePosition);
						this.addOutputText(sentenceHolder, processedText.length(), outputText);
						outputPos = relativePosition;
					} else if (!wasOutputting && (shouldOutput && !shouldProcess)) {
						outputPos = relativePosition;
					} // shouldOutput?
					break;
				}
				case POP_OUTPUT:
				case STOP_OUTPUT:
				case START_OUTPUT: {
					boolean wasOutputting = shouldOutput && !shouldProcess;
					if (action == TextMarkerType.POP_OUTPUT) {
						shouldOutputStack.pop();
					} else if (action == TextMarkerType.STOP_OUTPUT) {
						shouldOutputStack.pop();
						shouldOutputStack.push(false);
					} else if (action == TextMarkerType.START_OUTPUT) {
						shouldOutputStack.pop();
						shouldOutputStack.push(true);
					}
					shouldOutput = shouldOutputStack.peek();

					if (wasOutputting && (!shouldOutput || !shouldProcess)) {
						String outputText = currentText.substring(outputPos, relativePosition);
						this.addOutputText(sentenceHolder, processedText.length(), outputText);
						outputPos = relativePosition;
					} else if (!wasOutputting && (shouldOutput && !shouldProcess)) {
						outputPos = relativePosition;
					} // shouldOutput?
					break;
				}
				case TAG_START:
				case TAG_STOP: {
					if (shouldProcess) {
						insertionPoints.put(processedText.length(), currentPos);
						String leftoverText = currentText.substring(currentPos, relativePosition);
						processedText.append(leftoverText);
						currentPos = relativePosition;
					}
					if (action == TextMarkerType.TAG_START)
						sentenceHolder.addTagStart(marker.getAttribute(), marker.getValue(), processedText.length());
					else
						sentenceHolder.addTagEnd(marker.getAttribute(), marker.getValue(), processedText.length());
				}
				} // marker type

				if (LOG.isTraceEnabled()) {
					LOG.trace("Stack after: " + shouldProcessStack);
					LOG.trace("Text after: " + processedText.toString());
				}
			} // next action
		} // next text marker

		boolean shouldProcess = shouldProcessStack.peek();
		boolean shouldOutput = shouldOutputStack.peek();

		if (shouldProcess) {
			insertionPoints.put(processedText.length(), currentPos);
			processedText.append(currentText.substring(currentPos));
		}

		if (shouldOutput && !shouldProcess) {
			leftoverOutput = leftoverOutput + currentText.substring(outputPos);
		}

		String finalProcessedText = processedText.toString();
		if (LOG.isTraceEnabled())
			LOG.trace("Text after processing: " + finalProcessedText);
		sentenceHolder.setProcessedText(finalProcessedText);

		int lastIndex = 0;
		int lastOriginalIndex = 0;
		for (Entry<Integer, Integer> insertionPoint : insertionPoints.entrySet()) {
			int j = 0;
			for (int i = lastIndex; i < insertionPoint.getKey(); i++) {
				sentenceHolder.addOriginalIndex(originalTextIndex + lastOriginalIndex + j);
				j++;
			}
			lastIndex = insertionPoint.getKey();
			lastOriginalIndex = insertionPoint.getValue();
		}

		if (lastIndex < sentenceHolder.getProcessedText().length()) {
			int j = 0;
			for (int i = lastIndex; i < sentenceHolder.getProcessedText().length(); i++) {
				sentenceHolder.addOriginalIndex(originalTextIndex + lastOriginalIndex + j);
				j++;
			}
		}

		originalTextIndex += currentText.length();

		sentenceHolder.setFileName(this.fileName);
		sentenceHolder.setFile(this.file);
		return sentenceHolder;

	}

	private void addOutputText(SentenceHolder holder, int position, String text) {
		holder.addOriginalTextSegment(position, leftoverOutput + text);
		leftoverOutput = "";
	}

}
