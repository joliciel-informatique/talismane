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
	private int lineNumber = 1;
	private Pattern newlinePattern = Pattern.compile("\r\n|[\r\n]");
	private int leftoverNewline = 0;
	private String fileName = "";
	
	private FilterService filterService;
	
	public RollingSentenceProcessorImpl(String fileName) {
		shouldProcessStack.push(true);
		shouldOutputStack.push(false);
		this.fileName = fileName;
	}
	
	@Override
	public SentenceHolder addNextSegment(String originalText, Set<TextMarker> textMarkers) {
		SentenceHolder sentenceHolder = filterService.getSentenceHolder();
		
		Map<Integer,Integer> insertionPoints = new TreeMap<Integer, Integer>();
		StringBuilder processedText = new StringBuilder();
		int currentPos = 0;
		int outputPos = 0;
		
		for (TextMarker textMarker : textMarkers) {
			LOG.debug(textMarker.getType() + ", " + textMarker.getPosition());
			boolean shouldProcess = shouldProcessStack.peek();
			boolean shouldOutput = shouldOutputStack.peek();
			
			switch (textMarker.getType()) {
			case STOP:
				if (shouldProcess) {
					insertionPoints.put(currentPos, processedText.length());
					processedText.append(originalText.substring(currentPos, textMarker.getPosition()));
					if (shouldOutput) {
						outputPos = textMarker.getPosition();
					}
				}
				shouldProcessStack.push(false);
				shouldOutputStack.push(shouldOutputStack.peek());
				break;
			case OUTPUT:
				if (!shouldOutput && !shouldProcess) {
					outputPos = textMarker.getPosition();
				}
				shouldProcessStack.push(shouldProcessStack.peek());
				shouldOutputStack.push(true);
				break;
			case START:
				if (!shouldProcess) {
					currentPos = textMarker.getPosition();
					if (shouldOutput) {
						String outputText = originalText.substring(outputPos, textMarker.getPosition());
						this.addOutputText(sentenceHolder, processedText.length(), outputText);
					}
				}				
				shouldProcessStack.push(true);
				shouldOutputStack.push(shouldOutputStack.peek());
				break;
			case SPACE:
				if (shouldProcess) {
					insertionPoints.put(currentPos, processedText.length());
					String textToInsert = originalText.substring(currentPos, textMarker.getPosition());
					processedText.append(textToInsert);
					currentPos = textMarker.getPosition();
					if (!textToInsert.endsWith(" ")) {
						insertionPoints.put(currentPos, processedText.length());
						processedText.append(" ");
					}
				}
				break;
			case SENTENCE_BREAK:
				if (shouldProcess) {
					insertionPoints.put(currentPos, processedText.length());
					processedText.append(originalText.substring(currentPos, textMarker.getPosition()));
					currentPos = textMarker.getPosition();
				}
				
				// add the sentence boundary on the last character that was actually added.
				sentenceHolder.addSentenceBoundary(processedText.length()-1);
				break;
			case END_MARKER:
				boolean wasProcessing = shouldProcess;
				boolean wasOutputting = shouldOutput && !shouldProcess;
				shouldProcessStack.pop();
				shouldOutputStack.pop();
				shouldProcess = shouldProcessStack.peek();
				shouldOutput = shouldOutputStack.peek();
					
				if (wasProcessing && !shouldProcess) {
					insertionPoints.put(currentPos, processedText.length());
					processedText.append(originalText.substring(currentPos, textMarker.getPosition()));
				} else if (!wasProcessing && shouldProcess){
					currentPos = textMarker.getPosition();
				} // shouldProcess?
				
				if (wasOutputting  && (!shouldOutput || !shouldProcess)) {
					String outputText = originalText.substring(outputPos, textMarker.getPosition());
					this.addOutputText(sentenceHolder, processedText.length(), outputText);
				} else if (!wasOutputting && (shouldOutput && !shouldProcess)) {
					outputPos = textMarker.getPosition();
				} // shouldOutput?
				break;
			} // marker type
		} // next text marker
		boolean shouldProcess = shouldProcessStack.peek();
		boolean shouldOutput = shouldOutputStack.peek();

		if (shouldProcess) {
			insertionPoints.put(currentPos, processedText.length());
			processedText.append(originalText.substring(currentPos));
		}
		if (shouldOutput && !shouldProcess) {
			leftoverOutput = leftoverOutput + originalText.substring(outputPos);
		}
		
		sentenceHolder.setText(processedText.toString());

		int lastValue = 0;
		int lastKey = 0;
		for (Entry<Integer,Integer> insertionPoint : insertionPoints.entrySet()) {
			int j=0;
			for (int i=lastValue; i<insertionPoint.getValue(); i++) {
				sentenceHolder.addOriginalIndex(originalTextIndex + lastKey + j);
				j++;
			}
			lastValue = insertionPoint.getValue();
			lastKey = insertionPoint.getKey();
		}
		if (lastValue<sentenceHolder.getText().length()) {
			int j=0;
			for (int i=lastValue; i<sentenceHolder.getText().length(); i++) {
				sentenceHolder.addOriginalIndex(originalTextIndex + lastKey + j);
				j++;
			}
		}
		
		// find any newlines
		sentenceHolder.addNewline(leftoverNewline, lineNumber-1);

		Matcher matcher = newlinePattern.matcher(originalText);
		while (matcher.find()) {
			sentenceHolder.addNewline(originalTextIndex + matcher.end(), lineNumber++);
			leftoverNewline = originalTextIndex + matcher.end();
		}

		originalTextIndex += originalText.length();
		
		sentenceHolder.setFileName(this.fileName);
		return sentenceHolder;
	}

	private void addOutputText(SentenceHolder holder, int position, String text) {
		String existingText = holder.getOriginalTextSegments().get(position);
		if (existingText==null) {
			holder.getOriginalTextSegments().put(position, leftoverOutput + text);
		} else {
			holder.getOriginalTextSegments().put(position, existingText + leftoverOutput + text);
		}
		leftoverOutput = "";
	}
	
	public FilterService getFilterService() {
		return filterService;
	}

	public void setFilterService(FilterService filterService) {
		this.filterService = filterService;
	}

	
	
}
