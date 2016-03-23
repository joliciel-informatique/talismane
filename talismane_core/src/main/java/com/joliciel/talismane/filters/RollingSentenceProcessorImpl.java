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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class RollingSentenceProcessorImpl implements RollingSentenceProcessor {
	private static final Log LOG = LogFactory.getLog(RollingSentenceProcessorImpl.class);
	private Stack<Boolean> shouldProcessStack = new Stack<Boolean>();
	private Stack<Boolean> shouldOutputStack = new Stack<Boolean>();
	private int originalTextIndex = 0;
	private String leftoverOutput = "";
	private int lineNumber = 2; // this is so that the line following the first
								// newline character will be given 2 (and the
								// initial line 1)
	private Pattern newlinePattern = Pattern.compile("\r\n|[\r\n]");
	private int leftoverNewline = 0;
	private String fileName = "";
	private File file = null;
	private static final int NUM_CHARS = 30;

	private FilterService filterService;

	public RollingSentenceProcessorImpl(String fileName, boolean processByDefault) {
		shouldProcessStack.push(processByDefault);
		shouldOutputStack.push(false);
		this.fileName = fileName;
	}

	public RollingSentenceProcessorImpl(String fileName) {
		this(fileName, true);
	}

	@Override
	public SentenceHolder addNextSegment(String originalText, Set<TextMarker> textMarkers) {
		if (LOG.isTraceEnabled()) {
			LOG.trace("addNextSegment: " + originalText.replace('\n', '¶').replace('\r', '¶'));
		}
		SentenceHolder sentenceHolder = filterService.getSentenceHolder();

		// find any newlines
		sentenceHolder.addNewline(leftoverNewline, lineNumber - 1);

		Matcher matcher = newlinePattern.matcher(originalText);
		while (matcher.find()) {
			sentenceHolder.addNewline(originalTextIndex + matcher.end(), lineNumber++);
			leftoverNewline = originalTextIndex + matcher.end();
		}

		Map<Integer, Integer> insertionPoints = new TreeMap<Integer, Integer>();
		StringBuilder processedText = new StringBuilder();
		int currentPos = 0;
		int outputPos = 0;

		for (TextMarker textMarker : textMarkers) {
			if (LOG.isTraceEnabled()) {
				LOG.trace(textMarker.getType() + ", " + textMarker.getPosition());
				LOG.trace("Stack before: " + shouldProcessStack);
				LOG.trace("Text before: " + processedText.toString());
				LOG.trace("Added by filter: " + textMarker.getSource().toString().replace('\n', '¶').replace('\r', '¶'));
				LOG.trace("Match text: " + textMarker.getMatchText().replace('\n', '¶').replace('\r', '¶'));
			}

			boolean shouldProcess = shouldProcessStack.peek();
			boolean shouldOutput = shouldOutputStack.peek();

			switch (textMarker.getType()) {
			case PUSH_SKIP:
				if (shouldProcess) {
					insertionPoints.put(processedText.length(), currentPos);
					processedText.append(originalText.substring(currentPos, textMarker.getPosition()));
					if (shouldOutput) {
						outputPos = textMarker.getPosition();
					}
				}
				shouldProcessStack.push(false);
				break;
			case PUSH_OUTPUT:
				if (!shouldOutput && !shouldProcess) {
					outputPos = textMarker.getPosition();
				}
				shouldOutputStack.push(true);
				break;
			case PUSH_INCLUDE:
				if (!shouldProcess) {
					currentPos = textMarker.getPosition();
					if (shouldOutput) {
						String outputText = originalText.substring(outputPos, textMarker.getPosition());
						this.addOutputText(sentenceHolder, processedText.length(), outputText);
						outputPos = textMarker.getPosition();
					}
				}
				shouldProcessStack.push(true);
				break;
			case SPACE:
				if (shouldProcess) {
					insertionPoints.put(processedText.length(), currentPos);
					String leftoverText = originalText.substring(currentPos, textMarker.getPosition());
					processedText.append(leftoverText);
					currentPos = textMarker.getPosition();
					if (!leftoverText.endsWith(" ")) {
						insertionPoints.put(processedText.length(), currentPos);
						processedText.append(" ");
					}
				}
				break;
			case INSERT:
				if (shouldProcess) {
					insertionPoints.put(processedText.length(), currentPos);
					String leftoverText = originalText.substring(currentPos, textMarker.getPosition());
					processedText.append(leftoverText);
					currentPos = textMarker.getPosition();
					for (int i = 0; i < textMarker.getInsertionText().length(); i++) {
						insertionPoints.put(processedText.length() + i, currentPos);
					}
					if (LOG.isTraceEnabled())
						LOG.trace("Inserting: " + textMarker.getInsertionText());
					processedText.append(textMarker.getInsertionText());
				}
				break;
			case SENTENCE_BREAK: {
				String leftoverText = null;
				if (shouldProcess) {
					insertionPoints.put(processedText.length(), currentPos);
					leftoverText = originalText.substring(currentPos, textMarker.getPosition());
					processedText.append(leftoverText);
					currentPos = textMarker.getPosition();
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
				if (textMarker.getType().equals(TextMarkerType.POP_SKIP) || textMarker.getType().equals(TextMarkerType.POP_INCLUDE)) {
					shouldProcessStack.pop();
				} else if (textMarker.getType().equals(TextMarkerType.STOP)) {
					shouldProcessStack.pop();
					shouldProcessStack.push(false);
				} else if (textMarker.getType().equals(TextMarkerType.START)) {
					shouldProcessStack.pop();
					shouldProcessStack.push(true);
				}
				shouldProcess = shouldProcessStack.peek();
				shouldOutput = shouldOutput && !shouldProcess;

				if (wasProcessing && !shouldProcess) {
					insertionPoints.put(processedText.length(), currentPos);
					processedText.append(originalText.substring(currentPos, textMarker.getPosition()));
				} else if (!wasProcessing && shouldProcess) {
					currentPos = textMarker.getPosition();
				} // shouldProcess?

				if (wasOutputting && (!shouldOutput || !shouldProcess)) {
					String outputText = originalText.substring(outputPos, textMarker.getPosition());
					this.addOutputText(sentenceHolder, processedText.length(), outputText);
					outputPos = textMarker.getPosition();
				} else if (!wasOutputting && (shouldOutput && !shouldProcess)) {
					outputPos = textMarker.getPosition();
				} // shouldOutput?
				break;
			}
			case POP_OUTPUT:
			case STOP_OUTPUT:
			case START_OUTPUT: {
				boolean wasOutputting = shouldOutput && !shouldProcess;
				if (textMarker.getType().equals(TextMarkerType.POP_OUTPUT)) {
					shouldOutputStack.pop();
				} else if (textMarker.getType().equals(TextMarkerType.STOP_OUTPUT)) {
					shouldOutputStack.pop();
					shouldOutputStack.push(false);
				} else if (textMarker.getType().equals(TextMarkerType.START_OUTPUT)) {
					shouldOutputStack.pop();
					shouldOutputStack.push(true);
				}
				shouldOutput = shouldOutputStack.peek();

				if (wasOutputting && (!shouldOutput || !shouldProcess)) {
					String outputText = originalText.substring(outputPos, textMarker.getPosition());
					this.addOutputText(sentenceHolder, processedText.length(), outputText);
					outputPos = textMarker.getPosition();
				} else if (!wasOutputting && (shouldOutput && !shouldProcess)) {
					outputPos = textMarker.getPosition();
				} // shouldOutput?
				break;
			}
			case TAG_START:
			case TAG_STOP: {
				if (shouldProcess) {
					insertionPoints.put(processedText.length(), currentPos);
					String leftoverText = originalText.substring(currentPos, textMarker.getPosition());
					processedText.append(leftoverText);
					currentPos = textMarker.getPosition();
				}
				if (textMarker.getType().equals(TextMarkerType.TAG_START))
					sentenceHolder.addTagStart(textMarker.getAttribute(), textMarker.getValue(), processedText.length());
				else
					sentenceHolder.addTagEnd(textMarker.getAttribute(), textMarker.getValue(), processedText.length());
			}
			} // marker type

			if (LOG.isTraceEnabled()) {
				LOG.trace("Stack after: " + shouldProcessStack);
				LOG.trace("Text after: " + processedText.toString());
			}

		} // next text marker
		boolean shouldProcess = shouldProcessStack.peek();
		boolean shouldOutput = shouldOutputStack.peek();

		if (shouldProcess) {
			insertionPoints.put(processedText.length(), currentPos);
			processedText.append(originalText.substring(currentPos));
		}
		if (shouldOutput && !shouldProcess) {
			leftoverOutput = leftoverOutput + originalText.substring(outputPos);
		}

		String finalProcessedText = processedText.toString();
		if (LOG.isTraceEnabled())
			LOG.trace("Text after processing: " + finalProcessedText);
		sentenceHolder.setText(finalProcessedText);

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
		if (lastIndex < sentenceHolder.getText().length()) {
			int j = 0;
			for (int i = lastIndex; i < sentenceHolder.getText().length(); i++) {
				sentenceHolder.addOriginalIndex(originalTextIndex + lastOriginalIndex + j);
				j++;
			}
		}

		originalTextIndex += originalText.length();

		sentenceHolder.setFileName(this.fileName);
		sentenceHolder.setFile(this.file);
		return sentenceHolder;
	}

	private void addOutputText(SentenceHolder holder, int position, String text) {
		holder.addOriginalTextSegment(position, leftoverOutput + text);
		leftoverOutput = "";
	}

	public FilterService getFilterService() {
		return filterService;
	}

	public void setFilterService(FilterService filterService) {
		this.filterService = filterService;
	}

	@Override
	public void onNextFile(File file) {
		this.file = file;
		this.fileName = file.getPath();
		this.lineNumber = 2;
		this.originalTextIndex = 0;
		this.leftoverNewline = 0;
	}

}
