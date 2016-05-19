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

import com.joliciel.talismane.tokeniser.TokenAttribute;
import com.joliciel.talismane.tokeniser.TokeniserService;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

class SentenceHolderImpl extends SentenceImpl implements SentenceHolder {
	private static final Logger LOG = LoggerFactory.getLogger(SentenceHolderImpl.class);
	private TreeSet<Integer> sentenceBoundaries = new TreeSet<Integer>();
	private static final Pattern duplicateWhiteSpacePattern = Pattern.compile("[" + Sentence.WHITE_SPACE + "\n\r]{2,}", Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern openingWhiteSpacePattern = Pattern.compile("\\A([" + Sentence.WHITE_SPACE + "\n\r]+)", Pattern.UNICODE_CHARACTER_CLASS);
	private static final Pattern closingWhiteSpacePattern = Pattern.compile("([" + Sentence.WHITE_SPACE + "\n\r]+)\\z", Pattern.UNICODE_CHARACTER_CLASS);

	private FilterService filterService;
	private TokeniserService tokeniserService;

	private TIntObjectMap<List<SentenceTag<?>>> tagStarts = new TIntObjectHashMap<List<SentenceTag<?>>>();
	private TIntObjectMap<List<SentenceTag<?>>> tagEnds = new TIntObjectHashMap<List<SentenceTag<?>>>();

	@Override
	public Set<Integer> getSentenceBoundaries() {
		return sentenceBoundaries;
	}

	@Override
	public void addSentenceBoundary(int boundary) {
		this.sentenceBoundaries.add(boundary);
	}

	@Override
	public List<Sentence> getDetectedSentences(Sentence leftover) {
		if (LOG.isTraceEnabled()) {
			LOG.trace("getDetectedSentences. leftover=" + leftover);
		}

		List<Sentence> sentences = new ArrayList<Sentence>();

		int currentIndex = 0;
		boolean haveLeftOvers = this.getText().length() > 0;
		if (this.sentenceBoundaries.size() > 0) {
			haveLeftOvers = false;
			int lastSentenceBoundary = this.sentenceBoundaries.descendingIterator().next();
			if (lastSentenceBoundary < this.getText().length() - 1) {
				haveLeftOvers = true;
			}
			if (LOG.isTraceEnabled()) {
				LOG.trace("haveLeftOvers? " + lastSentenceBoundary + " < " + (this.getText().length() - 1) + " = " + haveLeftOvers);
			}
		}

		int lastOriginalTextInsertionProcessed = -1;
		int lastTagStartProcessed = -1;
		int lastTagEndProcessed = -1;

		List<Integer> allBoundaries = new ArrayList<Integer>(this.sentenceBoundaries);
		if (haveLeftOvers)
			allBoundaries.add(this.getText().length() - 1);

		Map<Integer, List<SentenceTag<?>>> sentenceTagPositions = new HashMap<Integer, List<SentenceTag<?>>>();
		for (SentenceTag<?> sentenceTag : this.getSentenceTags()) {
			List<SentenceTag<?>> sentenceTagsAtPos = sentenceTagPositions.get(sentenceTag.getStartIndex());
			if (sentenceTagsAtPos == null) {
				sentenceTagsAtPos = new ArrayList<SentenceTag<?>>();
				sentenceTagPositions.put(sentenceTag.getStartIndex(), sentenceTagsAtPos);
			}
			sentenceTagsAtPos.add(sentenceTag);
		}

		for (int sentenceBoundary : allBoundaries) {
			boolean isLeftover = haveLeftOvers && sentenceBoundary == this.getText().length() - 1;

			Sentence sentence = filterService.getSentence();
			int leftOverTextLength = 0;
			String text = "";
			if (leftover != null) {
				sentence = leftover;
				leftOverTextLength = leftover.getText().length();
				text = leftover.getText() + this.getText().substring(currentIndex, sentenceBoundary + 1);
				leftover = null;
			} else {
				text = this.getText().substring(currentIndex, sentenceBoundary + 1);
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
						sentence.addOriginalIndex(this.getOriginalIndexes().get(i));

					if (this.getOriginalTextSegments().containsKey(i)) {
						sentence.addOriginalTextSegment(sb.length(), this.getOriginalTextSegments().get(i));
						lastOriginalTextInsertionProcessed = i;
					}

					if (this.tagStarts.containsKey(i)) {
						for (SentenceTag<?> tag : this.tagStarts.get(i)) {
							sentence.getSentenceTags().add(tag.clone(sb.length()));
						}
						lastTagStartProcessed = i;
					}

					if (this.tagEnds.containsKey(i)) {
						List<SentenceTag<?>> sentenceTags = sentence.getSentenceTags();
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

			for (SentenceTag<?> sentenceTag : sentence.getSentenceTags()) {
				if (sentenceTag.getEndIndex() < 0)
					sentenceTag.setEndIndex(sb.length());
			}

			sentence.setText(sb.toString());
			if (LOG.isTraceEnabled()) {
				LOG.trace("sentence.setText |" + sentence.getText() + "|");
			}

			sentence.setComplete(!isLeftover);

			for (Entry<Integer, Integer> newlineLocation : this.newlines.entrySet()) {
				sentence.addNewline(newlineLocation.getKey(), newlineLocation.getValue());
			}

			sentence.setFileName(this.getFileName());
			sentence.setFile(this.getFile());

			sentences.add(sentence);
			currentIndex = sentenceBoundary + 1;
		}

		// remove any original text segments already processed
		TreeMap<Integer, String> leftoverOriginalTextSegments = new TreeMap<Integer, String>();
		for (int i : this.getOriginalTextSegments().keySet()) {
			if (i > lastOriginalTextInsertionProcessed) {
				leftoverOriginalTextSegments.put(i, this.getOriginalTextSegments().get(i));
			}
		}
		this.setOriginalTextSegments(leftoverOriginalTextSegments);

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

	public FilterService getFilterService() {
		return filterService;
	}

	public void setFilterService(FilterService filterService) {
		this.filterService = filterService;
	}

	@Override
	public <T> void addTagStart(String attribute, TokenAttribute<T> value, int position) {
		List<SentenceTag<?>> tagStartsAtPos = this.tagStarts.get(position);
		if (tagStartsAtPos == null) {
			tagStartsAtPos = new ArrayList<SentenceTag<?>>();
			tagStarts.put(position, tagStartsAtPos);
		}
		tagStartsAtPos.add(new SentenceTag<T>(0, attribute, value));
	}

	@Override
	public <T> void addTagEnd(String attribute, TokenAttribute<T> value, int position) {
		List<SentenceTag<?>> tagEndsAtPos = this.tagEnds.get(position);
		if (tagEndsAtPos == null) {
			tagEndsAtPos = new ArrayList<SentenceTag<?>>();
			tagEnds.put(position, tagEndsAtPos);
		}
		tagEndsAtPos.add(new SentenceTag<T>(0, attribute, value));
	}

	public TokeniserService getTokeniserService() {
		return tokeniserService;
	}

	public void setTokeniserService(TokeniserService tokeniserService) {
		this.tokeniserService = tokeniserService;
	}

}
