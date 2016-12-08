///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2016 Joliciel Informatique
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
import com.joliciel.talismane.AnnotationObserver;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.sentenceDetector.SentenceBoundary;
import com.joliciel.talismane.utils.io.CurrentFileObserver;

/**
 * A class capable of processing a set of RawTextFilters so that a list of
 * sentences can be extracted.
 * 
 * @author Assaf Urieli
 *
 */
public abstract class RawTextProcessor extends AnnotatedText implements CurrentFileObserver {
	private static final Logger LOG = LoggerFactory.getLogger(RawTextProcessor.class);
	private SentenceHolder sentenceHolder;
	private List<Annotation<SentenceBoundary>> sentenceBoundaries = new ArrayList<>();
	private final TalismaneSession session;
	private static Pattern newlinePattern = Pattern.compile("\r\n|[\r\n]");
	private final Stack<Boolean> shouldProcessStack;
	private final Stack<Boolean> shouldOutputStack;
	private int originalTextIndex = 0;
	private String leftoverOutput = "";

	private int lineNumber = 2; // this is so that the line following the first
	// newline character will be given 2 (and the
	// initial line 1)
	private int leftoverNewline = 0;

	private static final int NUM_CHARS = 30;

	protected RawTextProcessor(CharSequence text, boolean processByDefault, TalismaneSession session) {
		super(text, 0, text.length(), Collections.emptyList());
		this.session = session;

		this.shouldProcessStack = new Stack<>();
		this.shouldProcessStack.push(processByDefault);
		this.shouldOutputStack = new Stack<>();
		this.shouldOutputStack.push(false);
	}

	protected RawTextProcessor(RawTextProcessor predecessor, CharSequence text, int analysisStart, int analysisEnd, List<Annotation<?>> annotations) {
		super(text, analysisStart, analysisEnd, annotations);
		this.session = predecessor.session;

		this.shouldOutputStack = predecessor.shouldOutputStack;
		this.shouldProcessStack = predecessor.shouldProcessStack;
		this.originalTextIndex = predecessor.originalTextIndex;
		this.leftoverOutput = predecessor.leftoverOutput;
		this.lineNumber = predecessor.lineNumber;
		this.leftoverNewline = predecessor.leftoverNewline;
	}

	/**
	 * Processes the current text based on annotations added to block 3, and
	 * returns a SentenceHolder.
	 * 
	 * @return SentenceHolder to retrieve the sentences.
	 */
	protected SentenceHolder processText(int textStartPos, int textEndPos, CharSequence rawText, boolean finalBlock) {
		if (this.sentenceHolder != null)
			return this.sentenceHolder;

		LOG.debug("processText");
		List<Annotation<RawTextMarker>> annotations = this.getAnnotations(RawTextMarker.class);
		if (LOG.isTraceEnabled())
			LOG.trace("annotations: " + annotations.toString());

		Map<Integer, List<Pair<Boolean, Annotation<RawTextMarker>>>> markMap = new TreeMap<>();

		for (Annotation<RawTextMarker> annotation : annotations) {
			if (LOG.isTraceEnabled())
				LOG.trace("Annotation: " + annotation.toString());

			if (annotation.getStart() >= textStartPos && annotation.getStart() < textEndPos) {
				if (LOG.isTraceEnabled()) {
					LOG.trace("Start in range: textStartPos " + textStartPos + ">= matcherStart [[" + annotation.getStart() + "]] < textEndPos " + textEndPos);
				}
				List<Pair<Boolean, Annotation<RawTextMarker>>> startMarks = markMap.get(annotation.getStart());
				if (startMarks == null) {
					startMarks = new ArrayList<>();
					markMap.put(annotation.getStart(), startMarks);
				}
				startMarks.add(new ImmutablePair<Boolean, Annotation<RawTextMarker>>(true, annotation));
			} else {
				if (LOG.isTraceEnabled()) {
					LOG.trace("Start out of range: textStartPos " + textStartPos + ">= matcherStart [[" + annotation.getStart() + "]] < textEndPos "
							+ textEndPos);
				}
			}

			// if the matcher ends within the textblock
			// or if the matcher ends exactly on the textblock, and the
			// following text block is empty
			// we add the end match
			// the 2nd condition is to ensure we add the end match, since empty
			// blocks can never match anything
			if (annotation.getEnd() >= textStartPos
					&& (annotation.getEnd() < textEndPos || (annotation.getEnd() == textEndPos && rawText.length() > 0 && finalBlock))) {
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
			LOG.trace("currentText: " + rawText.toString().replace('\n', '¶').replace('\r', '¶'));
			LOG.trace("marks: " + marks.toString());
		}

		SentenceHolder sentenceHolder = new SentenceHolder(session, originalTextIndex, finalBlock);

		// find any newlines
		sentenceHolder.addNewline(leftoverNewline, lineNumber - 1);

		Matcher matcher = newlinePattern.matcher(rawText);
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
			int relativePosition = position - textStartPos;
			if (LOG.isTraceEnabled()) {
				LOG.trace((isStart ? "Start " : "Stop ") + marker.getType() + " at " + position + ", relative pos: " + relativePosition);
				LOG.trace("Stack before: " + shouldProcessStack);
				LOG.trace("Text before: " + processedText.toString());
				LOG.trace("Added by filter: " + marker.getSource());
				LOG.trace("Match text: "
						+ this.getText().subSequence(annotation.getStart(), annotation.getEnd()).toString().replace('\n', '¶').replace('\r', '¶'));
			}

			boolean shouldProcess = shouldProcessStack.peek();
			boolean shouldOutput = shouldOutputStack.peek();

			List<RawTextInstruction> actions = new ArrayList<>();
			if (isStart) {
				switch (marker.getType()) {
				case SKIP:
					actions.add(RawTextInstruction.PUSH_SKIP);
					break;
				case SENTENCE_BREAK:
					actions.add(RawTextInstruction.SENTENCE_BREAK);
					break;
				case SPACE:
					actions.add(RawTextInstruction.INSERT);
					actions.add(RawTextInstruction.PUSH_SKIP);
					break;
				case REPLACE:
					actions.add(RawTextInstruction.INSERT);
					actions.add(RawTextInstruction.PUSH_SKIP);
					break;
				case OUTPUT:
					actions.add(RawTextInstruction.PUSH_OUTPUT);
					actions.add(RawTextInstruction.PUSH_SKIP);
					break;
				case INCLUDE:
					actions.add(RawTextInstruction.PUSH_INCLUDE);
					break;
				case OUTPUT_START:
					actions.add(RawTextInstruction.PUSH_OUTPUT);
					break;
				case STOP:
					actions.add(RawTextInstruction.STOP);
					break;
				case NO_SENTENCE_BREAK:
				case NONE:
				case OUTPUT_STOP:
				case START:
				case TAG:
					break;
				}
			} else {
				// end of annotation
				switch (marker.getType()) {
				case SKIP:
				case SPACE:
				case REPLACE:
					actions.add(RawTextInstruction.POP_SKIP);
					break;
				case OUTPUT:
					actions.add(RawTextInstruction.STOP_OUTPUT);
					actions.add(RawTextInstruction.POP_SKIP);
					break;
				case INCLUDE:
					actions.add(RawTextInstruction.POP_INCLUDE);
					break;
				case START:
					actions.add(RawTextInstruction.START);
					break;
				case OUTPUT_STOP:
					actions.add(RawTextInstruction.STOP_OUTPUT);
					break;
				case NO_SENTENCE_BREAK:
				case NONE:
				case OUTPUT_START:
				case SENTENCE_BREAK:
				case STOP:
				case TAG:
					break;
				}
			}

			for (RawTextInstruction action : actions) {
				switch (action) {
				case PUSH_SKIP:
					if (shouldProcess) {
						insertionPoints.put(processedText.length(), currentPos);
						processedText.append(rawText.subSequence(currentPos, relativePosition));
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
							CharSequence outputText = rawText.subSequence(outputPos, relativePosition);
							this.addOutputText(sentenceHolder, processedText.length(), outputText);
							outputPos = relativePosition;
						}
					}
					shouldProcessStack.push(true);
					break;
				case SPACE:
					if (shouldProcess) {
						insertionPoints.put(processedText.length(), currentPos);
						CharSequence leftoverText = rawText.subSequence(currentPos, relativePosition);
						processedText.append(leftoverText);
						currentPos = relativePosition;
						if (leftoverText.length() > 0 && leftoverText.charAt(leftoverText.length() - 1) != ' ') {
							insertionPoints.put(processedText.length(), currentPos);
							processedText.append(" ");
						}
					}
					break;
				case INSERT:
					if (shouldProcess) {
						insertionPoints.put(processedText.length(), currentPos);
						CharSequence leftoverText = rawText.subSequence(currentPos, relativePosition);
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
					CharSequence leftoverText = null;
					if (shouldProcess) {
						insertionPoints.put(processedText.length(), currentPos);
						leftoverText = rawText.subSequence(currentPos, relativePosition);
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
							String startString = processedText.subSequence(start1, boundary).toString();

							String middleString = "" + processedText.charAt(boundary);

							string = startString + "[" + middleString + "]";
							string = string.replace('\n', '¶');
							LOG.trace("Adding sentence break at position " + boundary + ": " + string);
						}
					}
					if (shouldProcess) {
						if (leftoverText.length() > 0 && leftoverText.charAt(leftoverText.length() - 1) != ' ') {
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
					if (action == RawTextInstruction.POP_SKIP || action == RawTextInstruction.POP_INCLUDE) {
						shouldProcessStack.pop();
					} else if (action == RawTextInstruction.STOP) {
						shouldProcessStack.pop();
						shouldProcessStack.push(false);
					} else if (action == RawTextInstruction.START) {
						shouldProcessStack.pop();
						shouldProcessStack.push(true);
					}
					shouldProcess = shouldProcessStack.peek();
					shouldOutput = shouldOutput && !shouldProcess;

					if (wasProcessing && !shouldProcess) {
						insertionPoints.put(processedText.length(), currentPos);
						processedText.append(rawText.subSequence(currentPos, relativePosition));
					} else if (!wasProcessing && shouldProcess) {
						currentPos = relativePosition;
					} // shouldProcess?

					if (wasOutputting && (!shouldOutput || !shouldProcess)) {
						CharSequence outputText = rawText.subSequence(outputPos, relativePosition);
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
					if (action == RawTextInstruction.POP_OUTPUT) {
						shouldOutputStack.pop();
					} else if (action == RawTextInstruction.STOP_OUTPUT) {
						shouldOutputStack.pop();
						shouldOutputStack.push(false);
					} else if (action == RawTextInstruction.START_OUTPUT) {
						shouldOutputStack.pop();
						shouldOutputStack.push(true);
					}
					shouldOutput = shouldOutputStack.peek();

					if (wasOutputting && (!shouldOutput || !shouldProcess)) {
						CharSequence outputText = rawText.subSequence(outputPos, relativePosition);
						this.addOutputText(sentenceHolder, processedText.length(), outputText);
						outputPos = relativePosition;
					} else if (!wasOutputting && (shouldOutput && !shouldProcess)) {
						outputPos = relativePosition;
					} // shouldOutput?
					break;
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
			processedText.append(rawText.subSequence(currentPos, rawText.length()));
		}

		if (shouldOutput && !shouldProcess) {
			leftoverOutput = leftoverOutput + rawText.subSequence(outputPos, rawText.length());
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

		originalTextIndex += rawText.length();

		this.sentenceHolder = sentenceHolder;
		return sentenceHolder;

	}

	private void addOutputText(SentenceHolder holder, int position, CharSequence text) {
		holder.addOriginalTextSegment(position, leftoverOutput + text);
		leftoverOutput = "";
	}

	/**
	 * Return processed text ready for sentence detection.
	 * 
	 * @return
	 */
	public abstract AnnotatedText getProcessedText();

	protected AnnotatedText getProcessedTextBlock(int textStartPos, int textEndPos, SentenceHolder prevHolder, SentenceHolder currentHolder,
			SentenceHolder nextHolder) {
		LOG.trace("getProcessedTextBlock");

		StringBuilder sb = new StringBuilder();
		String processedText1 = prevHolder.getProcessedText();
		String processedText2 = currentHolder.getProcessedText();
		String processedText3 = nextHolder.getProcessedText();
		sb.append(processedText1);
		sb.append(processedText2);
		sb.append(processedText3);
		String processedText = sb.toString();

		List<Annotation<RawTextMarker>> myAnnotations = this.getAnnotations(RawTextMarker.class);
		List<Annotation<RawTextMarker>> hisAnnotations = new ArrayList<>();

		int prevHolderOriginalIndex = prevHolder.getOriginalStartIndex();
		for (Annotation<RawTextMarker> myAnnotation : myAnnotations) {
			if ((myAnnotation.getStart() >= textStartPos && myAnnotation.getStart() < textEndPos)
					|| ((myAnnotation.getEnd() >= textStartPos && myAnnotation.getEnd() < textEndPos))) {
				int originalStart = prevHolderOriginalIndex + myAnnotation.getStart();
				int originalEnd = prevHolderOriginalIndex + myAnnotation.getEnd();
				int localStart = processedText1.length();
				if (originalStart >= currentHolder.getOriginalStartIndex())
					localStart += currentHolder.getIndex(originalStart);
				int localEnd = processedText1.length() + currentHolder.getIndex(originalEnd);

				Annotation<RawTextMarker> hisAnnotation = myAnnotation.getAnnotation(localStart, localEnd);
				hisAnnotations.add(hisAnnotation);
			}
		}

		if (LOG.isTraceEnabled()) {
			LOG.trace("raw annotations: " + myAnnotations);
			LOG.trace("processed annotations: " + hisAnnotations);
		}

		AnnotatedText processedTextBlock = new AnnotatedText(processedText, processedText1.length(), processedText1.length() + processedText2.length());
		processedTextBlock.addAnnotations(hisAnnotations);

		processedTextBlock.addObserver(new AnnotationObserver() {
			// an observer which adds any annotations added to the
			// processedTextBlock back to myself, at the correct position
			@Override
			public <T> void beforeAddAnnotations(AnnotatedText subject, List<Annotation<T>> annotations) {
				int offset = textStartPos;

				int length1 = prevHolder.getProcessedText().length();
				int length2 = currentHolder.getProcessedText().length();

				int sentence2HolderStart = currentHolder.getOriginalStartIndex();
				List<Annotation<T>> newAnnotations = new ArrayList<>();
				for (Annotation<T> annotation : annotations) {
					if (annotation.getStart() >= length1 && annotation.getStart() < length1 + length2) {
						int originalStart = currentHolder.getOriginalIndex(annotation.getStart() - length1);
						int originalEnd = -1;
						if (annotation.getEnd() <= length1 + length2)
							originalEnd = currentHolder.getOriginalIndex(annotation.getEnd() - length1);
						else
							originalEnd = nextHolder.getOriginalIndex(annotation.getEnd() - (length1 + length2));

						if (originalEnd >= 0) {
							Annotation<T> newAnnotation = annotation.getAnnotation(originalStart - sentence2HolderStart + offset,
									originalEnd - sentence2HolderStart + offset);
							newAnnotations.add(newAnnotation);

							if (annotation.getData() instanceof SentenceBoundary) {
								@SuppressWarnings("unchecked")
								Annotation<SentenceBoundary> sentenceBoundary = (Annotation<SentenceBoundary>) annotation;
								sentenceBoundaries.add(sentenceBoundary);
							}
						}
					}
				}
				RawTextProcessor.this.addAnnotations(newAnnotations);

				if (LOG.isTraceEnabled()) {
					LOG.trace("ProcessedTextBlock Annotations received: " + annotations);
					LOG.trace("ProcessedTextBlock Annotations added: " + newAnnotations);
				}
			}

			@Override
			public <T> void afterAddAnnotations(AnnotatedText subject) {
			}
		});

		return processedTextBlock;
	}

	public void addDetectedSentences(SentenceHolder prevHolder, SentenceHolder currentHolder) {
		for (Annotation<SentenceBoundary> sentenceBoundary : sentenceBoundaries) {
			currentHolder.addSentenceBoundary(sentenceBoundary.getStart() - prevHolder.getProcessedText().length());
		}
	}

	@Override
	public void onNextFile(File file) {
		this.lineNumber = 2;
		this.originalTextIndex = 0;
		this.leftoverNewline = 0;
	}
}
