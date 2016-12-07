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
import com.joliciel.talismane.AnnotationObserver;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.sentenceDetector.SentenceBoundary;
import com.joliciel.talismane.utils.io.CurrentFileObserver;

/**
 * A block of raw text, always containing four sub-blocks, which are rolled in
 * from right to left. The reasoning behind this is that filters and features
 * cannot be applied to a block without knowing its context to the left and
 * right. If we attempt to apply filters to a single block only, we might not
 * match a regex that crosses the block boundaries at its start or end. We
 * therefore apply filters to a single block at a time, but provide context to
 * the left and right.<br/>
 * <br/>
 * In the case of raw text filters, e.g. XML filters which tell the system which
 * parts of the file to analyse, or which correct XML encoding issues (e.g. &lt;
 * becomes &amp;lt;), we always apply these filters to the 3rd sub-block, with
 * block 4 as the right-hand context. Since blocks are rolled from right to
 * left, and since we begin with four empty blocks, any filters crossing the
 * border between blocks 2 and 3 have already been added by the predecessor. The
 * filters should be applied to the AnnotatedText returned by
 * {@link RollingTextBlock#getRawTextBlock()}, which encapsulates block 3 and 4
 * with analysis ending at the end of block 3. Annotations added to this object
 * will automatically get added to the parent RollingTextBlock. This system
 * ensures that blocks 1, 2 and 3 have always been "processed" (with block 4
 * serving only as context for correct processing of block 3). <br/>
 * <br/>
 * Sentence detection has to be performed on processed text, since the training
 * corpus is of course a simple text corpus and we cannot apply probabilistic
 * decisions on a formatted file, such as XML. But sentence detection also needs
 * a context to the right and left, since some features may need to look beyond
 * a processed text block boundary. Therefore, sentence detection is always
 * performed on block 2 of processed text, with blocks 1 and 3 as the left and
 * right context. The object required for sentence detection can be requested
 * through {@link #getProcessedTextBlock()}. Annotations added to this object
 * will automatically get added to the parent RollingTextBlock, hence enabling
 * sentence extraction.<br/>
 * <br/>
 * <br/>
 * 
 * 
 * @author Assaf Urieli
 *
 */
public class RollingTextBlock extends AnnotatedText implements CurrentFileObserver {
	private static final Logger LOG = LoggerFactory.getLogger(RollingTextBlock.class);

	private final String block1;
	private final String block2;
	private final String block3;
	private final String block4;

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

	private final SentenceHolder sentenceHolder1;
	private final SentenceHolder sentenceHolder2;
	private SentenceHolder sentenceHolder3 = null;
	private List<Annotation<SentenceBoundary>> sentenceBoundaries = new ArrayList<>();
	private Sentence leftover;

	private static final int NUM_CHARS = 30;

	private final TalismaneSession session;

	/**
	 * Creates a new RollingTextBlock with prev, current and next all set to
	 * empty strings.
	 */
	public RollingTextBlock(TalismaneSession session, boolean processByDefault) {
		super("", 0, 0, Collections.emptyList());
		this.session = session;
		this.block1 = "";
		this.block2 = "";
		this.block3 = "";
		this.block4 = "";
		this.shouldProcessStack = new Stack<>();
		this.shouldProcessStack.push(processByDefault);
		this.shouldOutputStack = new Stack<>();
		this.shouldOutputStack.push(false);

		this.sentenceHolder1 = new SentenceHolder(session, 0, true);
		this.sentenceHolder1.setProcessedText("");
		this.sentenceHolder2 = new SentenceHolder(session, 0, true);
		this.sentenceHolder2.setProcessedText("");
	}

	private RollingTextBlock(RollingTextBlock predecessor, String nextText, List<Annotation<?>> annotations) {
		super(predecessor.block2 + predecessor.block3 + predecessor.block4 + nextText, predecessor.block2.length() + predecessor.block3.length(),
				predecessor.block2.length() + predecessor.block3.length() + predecessor.block4.length(), annotations);
		this.block1 = predecessor.block2;
		this.block2 = predecessor.block3;
		this.block3 = predecessor.block4;
		this.block4 = nextText;

		this.session = predecessor.session;
		this.shouldOutputStack = predecessor.shouldOutputStack;
		this.shouldProcessStack = predecessor.shouldProcessStack;
		this.originalTextIndex = predecessor.originalTextIndex;
		this.leftoverOutput = predecessor.leftoverOutput;
		this.lineNumber = predecessor.lineNumber;
		this.leftoverNewline = predecessor.leftoverNewline;
		this.fileName = predecessor.fileName;
		this.file = predecessor.file;

		this.sentenceHolder1 = predecessor.sentenceHolder2;
		this.sentenceHolder2 = predecessor.sentenceHolder3;
		this.leftover = predecessor.leftover;

		if (LOG.isTraceEnabled()) {
			LOG.trace("After roll: ");
			LOG.trace("block1: " + block1.replace('\n', '¶').replace('\r', '¶'));
			LOG.trace("block2: " + block2.replace('\n', '¶').replace('\r', '¶'));
			LOG.trace("block3: " + block3.replace('\n', '¶').replace('\r', '¶'));
			LOG.trace("block4: " + block4.replace('\n', '¶').replace('\r', '¶'));
		}
	}

	/**
	 * Creates a new RollingTextBlock.<br/>
	 * Moves block2 → block1, block3 → block2, block4 → block3, and nextText →
	 * block4.<br/>
	 * <br/>
	 * All existing annotations have their start and end decremented by
	 * block1.length(). If the new start &lt; 0, start = 0, if new end &lt; 0,
	 * annotation dropped.<br/>
	 * <br/>
	 * If the current block3 has not yet been processed, it is processed when
	 * rolling, thus ensuring that we always have blocks 1, 2 and 3 processed.
	 * <br/>
	 * 
	 * @param nextText
	 *            the next text segment to add onto this rolling text block
	 * @return a new text block as described above
	 */
	public RollingTextBlock roll(String nextText) {
		this.processText();

		int prevLength = this.block1.length();
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

	/**
	 * Get a raw text block for annotation by filters. This covers blocks 3 and
	 * 4 only of the current RollingTextBlock, with analysis end at the end of
	 * block3. It is assumed that annotations crossing block 2 and 3 were
	 * already added by a predecessor.
	 */
	public AnnotatedText getRawTextBlock() {
		AnnotatedText rawTextBlock = new AnnotatedText(this.block3 + this.block4, 0, this.block3.length());
		rawTextBlock.addObserver(new AnnotationObserver() {

			@Override
			public <T> void beforeAddAnnotations(AnnotatedText subject, List<Annotation<T>> annotations) {
				int offset = RollingTextBlock.this.block1.length() + RollingTextBlock.this.block2.length();
				List<Annotation<T>> newAnnotations = new ArrayList<>();
				for (Annotation<T> annotation : annotations) {
					Annotation<T> newAnnotation = annotation.getAnnotation(annotation.getStart() + offset, annotation.getEnd() + offset);
					newAnnotations.add(newAnnotation);
				}
				RollingTextBlock.this.addAnnotations(newAnnotations);

				if (LOG.isTraceEnabled()) {
					LOG.trace("Annotations received: " + annotations);
					LOG.trace("Annotations added: " + newAnnotations);
				}
			}

			@Override
			public <T> void afterAddAnnotations(AnnotatedText subject) {
			}
		});
		return rawTextBlock;
	}

	/**
	 * Processes the current text based on annotations added to block 3, and
	 * returns a SentenceHolder.
	 * 
	 * @return SentenceHolder to retrieve the sentences.
	 */
	private SentenceHolder processText() {
		if (this.sentenceHolder3 != null)
			return this.sentenceHolder3;

		LOG.debug("processText");
		List<Annotation<RawTextMarker>> annotations = this.getAnnotations(RawTextMarker.class);
		Map<Integer, List<Pair<Boolean, Annotation<RawTextMarker>>>> markMap = new TreeMap<>();
		int textStartPos = this.block1.length() + this.block2.length();
		int textEndPos = this.block1.length() + this.block2.length() + this.block3.length();

		for (Annotation<RawTextMarker> annotation : annotations) {
			if (LOG.isTraceEnabled())
				LOG.trace("Annotation: " + annotation.toString());

			if (annotation.getStart() >= textStartPos && annotation.getStart() < textEndPos) {
				List<Pair<Boolean, Annotation<RawTextMarker>>> startMarks = markMap.get(annotation.getStart());
				if (startMarks == null) {
					startMarks = new ArrayList<>();
					markMap.put(annotation.getStart(), startMarks);
				}
				startMarks.add(new ImmutablePair<Boolean, Annotation<RawTextMarker>>(true, annotation));
			}

			// if the matcher ends within the textblock
			// or if the matcher ends exactly on the textblock, and the
			// following text block is empty
			// we add the end match
			// the 2nd condition is to ensure we add the end match, since empty
			// blocks can never match anything
			if (annotation.getEnd() >= textStartPos
					&& (annotation.getEnd() < textEndPos || (annotation.getEnd() == textEndPos && this.block3.length() > 0 && this.block4.length() == 0))) {
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
			LOG.trace("currentText: " + block3.replace('\n', '¶').replace('\r', '¶'));
			LOG.trace("marks: " + marks.toString());
		}

		boolean endOfBlock = block4.length() == 0;
		SentenceHolder sentenceHolder = new SentenceHolder(session, originalTextIndex, endOfBlock);

		// find any newlines
		sentenceHolder.addNewline(leftoverNewline, lineNumber - 1);

		Matcher matcher = newlinePattern.matcher(block3);
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
						processedText.append(block3.substring(currentPos, relativePosition));
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
							String outputText = block3.substring(outputPos, relativePosition);
							this.addOutputText(sentenceHolder, processedText.length(), outputText);
							outputPos = relativePosition;
						}
					}
					shouldProcessStack.push(true);
					break;
				case SPACE:
					if (shouldProcess) {
						insertionPoints.put(processedText.length(), currentPos);
						String leftoverText = block3.substring(currentPos, relativePosition);
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
						String leftoverText = block3.substring(currentPos, relativePosition);
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
						leftoverText = block3.substring(currentPos, relativePosition);
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
						processedText.append(block3.substring(currentPos, relativePosition));
					} else if (!wasProcessing && shouldProcess) {
						currentPos = relativePosition;
					} // shouldProcess?

					if (wasOutputting && (!shouldOutput || !shouldProcess)) {
						String outputText = block3.substring(outputPos, relativePosition);
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
						String outputText = block3.substring(outputPos, relativePosition);
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
			processedText.append(block3.substring(currentPos));
		}

		if (shouldOutput && !shouldProcess) {
			leftoverOutput = leftoverOutput + block3.substring(outputPos);
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

		originalTextIndex += block3.length();

		sentenceHolder.setFileName(this.fileName);
		sentenceHolder.setFile(this.file);
		this.sentenceHolder3 = sentenceHolder;
		return sentenceHolder;

	}

	private void addOutputText(SentenceHolder holder, int position, String text) {
		holder.addOriginalTextSegment(position, leftoverOutput + text);
		leftoverOutput = "";
	}

	/**
	 * Returns annotated text whose text is a combination of blocks 1, 2 and 3
	 * of the current RollingTextBlock, but after processing via raw text
	 * filters to convert the text to processed text, and with analysis start
	 * and end set so that only block 2 is analysed. This annotated text is
	 * ready to be submitted to sentence detection. It has sentence break and
	 * non-sentence-break annotations inherited from the present
	 * RollingTextBlock. Any sentence-break annotations added will automatically
	 * get reflected in the current RollingTextBlock.
	 * 
	 * @return
	 */
	public AnnotatedText getProcessedTextBlock() {
		SentenceHolder sentenceHolder = this.processText();
		StringBuilder sb = new StringBuilder();
		String processedText1 = sentenceHolder1.getProcessedText();
		String processedText2 = sentenceHolder2.getProcessedText();
		String processedText3 = sentenceHolder.getProcessedText();
		sb.append(processedText1);
		sb.append(processedText2);
		sb.append(processedText3);
		String processedText = sb.toString();

		List<Annotation<RawTextMarker>> myAnnotations = this.getAnnotations(RawTextMarker.class);
		List<Annotation<RawTextMarker>> hisAnnotations = new ArrayList<>();

		int block2OriginalIndex = sentenceHolder2.getOriginalStartIndex();
		for (Annotation<RawTextMarker> myAnnotation : myAnnotations) {
			if ((myAnnotation.getStart() >= block1.length() && myAnnotation.getStart() < block1.length() + block2.length())
					|| ((myAnnotation.getEnd() >= block1.length() && myAnnotation.getEnd() < block1.length() + block2.length()))) {
				int originalStart = block2OriginalIndex + myAnnotation.getStart();
				int originalEnd = block2OriginalIndex + myAnnotation.getEnd();
				int localStart = sentenceHolder2.getIndex(originalStart);
				int localEnd = sentenceHolder2.getIndex(originalEnd);
				Annotation<RawTextMarker> hisAnnotation = myAnnotation.getAnnotation(localStart, localEnd);
				hisAnnotations.add(hisAnnotation);
			}
		}
		AnnotatedText processedTextBlock = new AnnotatedText(processedText, processedText1.length(), processedText1.length() + processedText2.length());
		processedTextBlock.addAnnotations(hisAnnotations);

		processedTextBlock.addObserver(new AnnotationObserver() {
			// an observer which adds any annotations added to the
			// processedTextBlock back to myself, at the correct position
			@Override
			public <T> void beforeAddAnnotations(AnnotatedText subject, List<Annotation<T>> annotations) {
				int offset = RollingTextBlock.this.block1.length();
				SentenceHolder sentenceHolder1 = RollingTextBlock.this.sentenceHolder1;
				SentenceHolder sentenceHolder2 = RollingTextBlock.this.sentenceHolder2;
				SentenceHolder sentenceHolder3 = RollingTextBlock.this.sentenceHolder3;
				int length1 = sentenceHolder1.getProcessedText().length();
				int length2 = sentenceHolder2.getProcessedText().length();

				int sentence2HolderStart = sentenceHolder2.getOriginalStartIndex();
				List<Annotation<T>> newAnnotations = new ArrayList<>();
				for (Annotation<T> annotation : annotations) {
					if (annotation.getStart() >= length1 && annotation.getStart() < length1 + length2) {
						int originalStart = sentenceHolder2.getOriginalIndex(annotation.getStart() - length1);
						int originalEnd = -1;
						if (annotation.getEnd() <= length1 + length2)
							originalEnd = sentenceHolder2.getOriginalIndex(annotation.getEnd() - length1);
						else
							originalEnd = sentenceHolder3.getOriginalIndex(annotation.getEnd() - (length1 + length2));

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
				RollingTextBlock.this.addAnnotations(newAnnotations);

				if (LOG.isTraceEnabled()) {
					LOG.trace("Annotations received: " + annotations);
					LOG.trace("Annotations added: " + newAnnotations);
				}
			}

			@Override
			public <T> void afterAddAnnotations(AnnotatedText subject) {
			}
		});

		return processedTextBlock;
	}

	/**
	 * Get a list of sentences currently detected in block 2. If block 3 is
	 * empty, any leftover text after the final detected sentence will
	 * automatically be returned as an additional final sentence. If not, it
	 * will be kept as a "leftover" to be added to the first sentence detected
	 * in block 3.
	 * 
	 * @return
	 */
	public List<Sentence> getDetectedSentences() {
		for (Annotation<SentenceBoundary> sentenceBoundary : sentenceBoundaries) {
			sentenceHolder2.addSentenceBoundary(sentenceBoundary.getStart() - sentenceHolder1.getProcessedText().length());
		}
		List<Sentence> sentences = sentenceHolder2.getDetectedSentences(leftover);
		leftover = null;
		if (sentences.size() > 0) {
			Sentence lastSentence = sentences.get(sentences.size() - 1);
			if (!lastSentence.isComplete()) {
				leftover = lastSentence;
				if (LOG.isTraceEnabled())
					LOG.trace("Set leftover to: " + leftover.toString());
				sentences.remove(sentences.size() - 1);
			}
		}

		// If we have any leftover original text segments,
		// copy them over
		// they are necessarily at position 0 - since
		// otherwise they would
		// have gotten added to the leftover sentence. The
		// only case where
		// there isn't a leftover sentence is the case where
		// the sentenceHolder
		// boundary happens to be a sentence boundary, hence
		// position 0.
		if (sentenceHolder2.getOriginalTextSegments().size() > 0) {
			String fileName = "";
			File file = null;

			if (leftover == null) {
				leftover = new Sentence("", fileName, file, session);
			}
			StringBuilder segmentsToInsert = new StringBuilder();

			if (leftover.getLeftoverOriginalText().length() > 0)
				segmentsToInsert.append(session.getOutputDivider());

			for (String originalTextSegment : sentenceHolder2.getOriginalTextSegments().values()) {
				segmentsToInsert.append(originalTextSegment);
			}

			leftover.setLeftoverOriginalText(leftover.getLeftoverOriginalText() + segmentsToInsert.toString());
		}

		return sentences;
	}

	@Override
	public void onNextFile(File file) {
		this.file = file;
		this.fileName = file.getPath();
		this.lineNumber = 2;
		this.originalTextIndex = 0;
		this.leftoverNewline = 0;
	}

	public String getLeftoverOriginalText() {
		if (leftover != null) {
			return leftover.getLeftoverOriginalText();
		} else {
			return "";
		}
	}
}
