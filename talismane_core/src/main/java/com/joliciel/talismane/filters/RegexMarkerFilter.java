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
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.tokeniser.TokenAttribute;
import com.joliciel.talismane.utils.RegexUtils;

/**
 * For a given regex, finds any matches within the text, and adds the
 * appropriate marker to these matches. If not group is provided, will match the
 * expression as a whole.
 * 
 * @author Assaf Urieli
 *
 */
class RegexMarkerFilter implements TextMarkerFilter {
	private static final Logger LOG = LoggerFactory.getLogger(RegexMarkerFilter.class);
	private List<MarkerFilterType> filterTypes;
	private Pattern pattern;
	private String regex;
	private int groupIndex = 0;
	private String replacement;
	private String attribute;
	private TokenAttribute<?> value;
	private int blockSize = 1000;

	public RegexMarkerFilter(List<MarkerFilterType> filterTypes, String regex) {
		this(filterTypes, regex, 0);
	}

	public RegexMarkerFilter(MarkerFilterType[] filterTypeArray, String regex) {
		this(filterTypeArray, regex, 0);
	}

	public RegexMarkerFilter(MarkerFilterType filterType, String regex) {
		this(filterType, regex, 0);
	}

	public RegexMarkerFilter(List<MarkerFilterType> filterTypes, String regex, int groupIndex) {
		this.filterTypes = filterTypes;
		this.initialise(regex, groupIndex);
	}

	public RegexMarkerFilter(MarkerFilterType[] filterTypeArray, String regex, int groupIndex) {
		this.filterTypes = new ArrayList<MarkerFilterType>(filterTypeArray.length);
		for (MarkerFilterType filterType : filterTypeArray)
			this.filterTypes.add(filterType);
		this.initialise(regex, groupIndex);
	}

	public RegexMarkerFilter(MarkerFilterType filterType, String regex, int groupIndex) {
		this.filterTypes = new ArrayList<MarkerFilterType>(1);
		this.filterTypes.add(filterType);
		this.initialise(regex, groupIndex);
	}

	private void initialise(String regex, int groupIndex) {
		this.regex = regex;
		this.pattern = Pattern.compile(regex, Pattern.UNICODE_CHARACTER_CLASS);
		if (groupIndex < 0) {
			throw new TalismaneException("Cannot have a group index < 0: " + groupIndex);
		}
		this.groupIndex = groupIndex;
	}

	@Override
	public Set<TextMarker> apply(String prevText, String text, String nextText) {
		if (LOG.isTraceEnabled()) {
			LOG.trace("Matching " + regex.replace('\n', '¶').replace('\r', '¶'));
		}
		String context = prevText + text + nextText;

		int textStartPos = prevText.length();
		int textEndPos = prevText.length() + text.length();

		Matcher matcher = pattern.matcher(context);
		Set<TextMarker> textMarkers = new TreeSet<TextMarker>();
		while (matcher.find()) {
			int matcherStart = 0;
			int matcherEnd = 0;
			if (groupIndex == 0) {
				matcherStart = matcher.start();
				matcherEnd = matcher.end();
			} else {
				matcherStart = matcher.start(groupIndex);
				matcherEnd = matcher.end(groupIndex);
			}

			String matchText = context.substring(matcher.start(), matcher.end());
			if (LOG.isTraceEnabled()) {
				LOG.trace("Next match: " + matchText.replace('\n', '¶').replace('\r', '¶'));
				if (matcher.start() != matcherStart || matcher.end() != matcherEnd) {
					LOG.trace("But matching group: " + context.substring(matcherStart, matcherEnd).replace('\n', '¶').replace('\r', '¶'));
				}
				LOG.trace("matcher.start()=" + matcher.start() + ", matcher.end()=" + matcher.end() + ", matcherStart=" + matcherStart + ", matcherEnd="
						+ matcherEnd + ", textStartPos=" + textStartPos + ", textEndPos=" + textEndPos);
			}

			if (matcherEnd - matcherStart > blockSize) {
				String errorString = "Match size (" + (matcherEnd - matcherStart) + ") bigger than block size (" + blockSize + "). "
						+ "Increase blockSize or change filter. "
						+ "Maybe you need to change a greedy quantifier (e.g. .*) to a reluctant quantifier (e.g. .*?)? " + "Regex: " + regex + ". Text: "
						+ matchText;
				throw new TalismaneException(errorString);
			}

			if (matcherStart >= textStartPos && matcherStart < textEndPos) {
				if (LOG.isTraceEnabled()) {
					LOG.trace("Start in range: textStartPos " + textStartPos + ">= matcherStart [[" + matcherStart + "]] < textEndPos " + textEndPos);
				}
				for (MarkerFilterType filterType : filterTypes) {
					switch (filterType) {
					case SKIP: {
						TextMarker textMarker = new TextMarker(TextMarkerType.PUSH_SKIP, matcherStart - prevText.length(), this, matchText);
						textMarkers.add(textMarker);
						break;
					}
					case SENTENCE_BREAK: {
						TextMarker textMarker = new TextMarker(TextMarkerType.SENTENCE_BREAK, matcherStart - prevText.length(), this, matchText);
						textMarkers.add(textMarker);
						break;
					}
					case SPACE: {
						TextMarker textMarker = new TextMarker(TextMarkerType.SPACE, matcherStart - prevText.length(), this, matchText);
						textMarker.setInsertionText(" ");
						textMarkers.add(textMarker);
						TextMarker textMarker2 = new TextMarker(TextMarkerType.PUSH_SKIP, matcherStart - prevText.length(), this, matchText);
						textMarkers.add(textMarker2);
						break;
					}
					case REPLACE: {
						TextMarker textMarker = new TextMarker(TextMarkerType.INSERT, matcherStart - prevText.length(), this, matchText);
						String newText = RegexUtils.getReplacement(replacement, context, matcher);
						if (LOG.isTraceEnabled()) {
							LOG.trace("Setting replacement to: " + newText);
						}
						textMarker.setInsertionText(newText);
						textMarkers.add(textMarker);
						TextMarker textMarker2 = new TextMarker(TextMarkerType.PUSH_SKIP, matcherStart - prevText.length(), this, matchText);
						textMarkers.add(textMarker2);
						break;
					}
					case OUTPUT: {
						TextMarker textMarker = new TextMarker(TextMarkerType.PUSH_OUTPUT, matcherStart - prevText.length(), this, matchText);
						textMarkers.add(textMarker);
						TextMarker textMarker2 = new TextMarker(TextMarkerType.PUSH_SKIP, matcherStart - prevText.length(), this, matchText);
						textMarkers.add(textMarker2);
						break;
					}
					case INCLUDE: {
						TextMarker textMarker = new TextMarker(TextMarkerType.PUSH_INCLUDE, matcherStart - prevText.length(), this, matchText);
						textMarkers.add(textMarker);
						break;
					}
					case OUTPUT_START: {
						TextMarker textMarker = new TextMarker(TextMarkerType.START_OUTPUT, matcherStart - prevText.length(), this, matchText);
						textMarkers.add(textMarker);
						break;
					}
					case STOP: {
						TextMarker textMarker = new TextMarker(TextMarkerType.STOP, matcherStart - prevText.length(), this, matchText);
						textMarkers.add(textMarker);
						break;
					}
					case NONE:
						break;
					case OUTPUT_STOP:
						break;
					case START:
						break;
					case TAG: {
						TextMarker textMarker = new TextMarker(TextMarkerType.TAG_START, matcherStart - prevText.length(), this, matchText);
						textMarker.setTag(attribute, value);
						textMarkers.add(textMarker);
						break;
					}
					default:
						break;
					}
				}
			}
			// if the matcher ends within the textblock
			// or if the matcher ends exactly on the textblock, and the
			// following text block is empty
			// we add the end match
			// the 2nd condition is to ensure we add the end match, since empty
			// blocks can never match anything
			if (matcherEnd >= textStartPos && (matcherEnd < textEndPos || (matcherEnd == textEndPos && text.length() > 0 && nextText.length() == 0))) {
				if (LOG.isTraceEnabled()) {
					LOG.trace("End in range: textStartPos " + textStartPos + ">= matcherEnd [[" + matcherEnd + "]] < textEndPos " + textEndPos);
				}
				for (MarkerFilterType filterType : filterTypes) {
					switch (filterType) {
					case SKIP:
					case SPACE:
					case REPLACE: {
						TextMarker textMarker = new TextMarker(TextMarkerType.POP_SKIP, matcherEnd - prevText.length(), this, matchText);
						textMarkers.add(textMarker);
						break;
					}
					case OUTPUT: {
						TextMarker textMarker = new TextMarker(TextMarkerType.POP_OUTPUT, matcherEnd - prevText.length(), this, matchText);
						textMarkers.add(textMarker);
						TextMarker textMarker2 = new TextMarker(TextMarkerType.POP_SKIP, matcherEnd - prevText.length(), this, matchText);
						textMarkers.add(textMarker2);
						break;
					}
					case INCLUDE: {
						TextMarker textMarker = new TextMarker(TextMarkerType.POP_INCLUDE, matcherEnd - prevText.length(), this, matchText);
						textMarkers.add(textMarker);
						break;
					}
					case START: {
						TextMarker textMarker = new TextMarker(TextMarkerType.START, matcherEnd - prevText.length(), this, matchText);
						textMarkers.add(textMarker);
						break;
					}
					case OUTPUT_STOP: {
						TextMarker textMarker = new TextMarker(TextMarkerType.STOP_OUTPUT, matcherEnd - prevText.length(), this, matchText);
						textMarkers.add(textMarker);
						break;
					}
					case NONE:
						break;
					case OUTPUT_START:
						break;
					case SENTENCE_BREAK:
						break;
					case STOP:
						break;
					case TAG: {
						TextMarker textMarker = new TextMarker(TextMarkerType.TAG_STOP, matcherEnd - prevText.length(), this, matchText);
						textMarker.setTag(attribute, value);
						textMarkers.add(textMarker);
						break;
					}

					default:
						break;
					}
				}
			}
		} // next match

		if (textMarkers.size() > 0) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("For regex: " + this.regex.replace('\n', '¶').replace('\r', '¶'));
				LOG.debug("Added markers: " + textMarkers);
			}
		}
		return textMarkers;
	}

	public String getFind() {
		return regex;
	}

	@Override
	public String getReplacement() {
		return replacement;
	}

	@Override
	public void setReplacement(String replacement) {
		this.replacement = replacement;
	}

	@Override
	public String toString() {
		return "RegexMarkerFilter [filterTypes=" + filterTypes + ", regex=" + regex + ", groupIndex=" + groupIndex + ", replacement=" + replacement + "]";
	}

	@Override
	public int getBlockSize() {
		return blockSize;
	}

	@Override
	public void setBlockSize(int blockSize) {
		this.blockSize = blockSize;
	}

	@Override
	public String getAttribute() {
		return attribute;
	}

	@Override
	public TokenAttribute<?> getValue() {
		return value;
	}

	@Override
	public void setTag(String attribute, TokenAttribute<?> value) {
		this.attribute = attribute;
		this.value = value;
	}
}
