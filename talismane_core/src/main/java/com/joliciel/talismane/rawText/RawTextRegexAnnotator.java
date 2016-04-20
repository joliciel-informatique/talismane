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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.AnnotatedText;
import com.joliciel.talismane.Annotation;
import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.rawText.RawTextMarker.RawTextNoSentenceBreakMarker;
import com.joliciel.talismane.rawText.RawTextMarker.RawTextReplaceMarker;
import com.joliciel.talismane.rawText.RawTextMarker.RawTextSentenceBreakMarker;
import com.joliciel.talismane.rawText.RawTextMarker.RawTextSkipMarker;
import com.joliciel.talismane.tokeniser.TokenAttribute;
import com.joliciel.talismane.utils.RegexUtils;

/**
 * For a given regex, finds any matches within the text, and adds the
 * appropriate marker to these matches. If no group is provided, will match the
 * expression as a whole.
 * 
 * @author Assaf Urieli
 * @author Lucas Satabin
 *
 */
public class RawTextRegexAnnotator implements RawTextAnnotator {
	private static final Logger LOG = LoggerFactory.getLogger(RawTextRegexAnnotator.class);
	private final List<RawTextMarkType> filterTypes;
	private final Pattern pattern;
	private final String regex;
	private final int groupIndex;
	private String replacement;
	private TokenAttribute<?> attribute;
	private final int blockSize;

	public RawTextRegexAnnotator(List<RawTextMarkType> filterTypes, String regex, int groupIndex, int blockSize) {
		this.filterTypes = filterTypes;
		this.blockSize = blockSize;
		this.regex = regex;
		this.pattern = Pattern.compile(regex, Pattern.UNICODE_CHARACTER_CLASS);
		if (groupIndex < 0) {
			throw new TalismaneException("Cannot have a group index < 0: " + groupIndex);
		}
		this.groupIndex = groupIndex;
	}

	public RawTextRegexAnnotator(RawTextMarkType filterType, String regex, int groupIndex, int blockSize) {
		this(Arrays.asList(new RawTextMarkType[] { filterType }), regex, groupIndex, blockSize);
	}

	@Override
	public void annotate(AnnotatedText textBlock, String... labels) {
		if (LOG.isTraceEnabled()) {
			LOG.trace("Matching " + regex.replace('\n', '¶').replace('\r', '¶'));
		}

		List<Annotation<RawTextMarker>> rawTextMarkers = new ArrayList<>();
		List<Annotation<TokenAttribute<?>>> tokenAttributes = new ArrayList<>();

		Matcher matcher = pattern.matcher(textBlock.getText());
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

			CharSequence matchText = textBlock.getText().subSequence(matcher.start(), matcher.end());
			if (LOG.isTraceEnabled()) {
				LOG.trace("Next match: " + matchText.toString().replace('\n', '¶').replace('\r', '¶'));
				if (matcher.start() != matcherStart || matcher.end() != matcherEnd) {
					LOG.trace("But matching group: "
							+ textBlock.getText().subSequence(matcherStart, matcherEnd).toString().replace('\n', '¶').replace('\r', '¶'));
				}
				LOG.trace("matcher.start()=" + matcher.start() + ", matcher.end()=" + matcher.end() + ", matcherStart=" + matcherStart + ", matcherEnd="
						+ matcherEnd + ", analysisStart=" + textBlock.getAnalysisStart() + ", analysisEnd=" + textBlock.getAnalysisEnd());
			}

			if (matcherEnd - matcherStart > blockSize) {
				String errorString = "Match size (" + (matcherEnd - matcherStart) + ") bigger than block size (" + blockSize + "). "
						+ "Increase blockSize or change filter. "
						+ "Maybe you need to change a greedy quantifier (e.g. .*) to a reluctant quantifier (e.g. .*?)? " + "Regex: " + regex + ". Text: "
						+ matchText;
				throw new TalismaneException(errorString);
			}

			if (matcherStart >= textBlock.getAnalysisStart() && matcherStart < textBlock.getAnalysisEnd()) {
				if (LOG.isTraceEnabled()) {
					LOG.trace("Start in range: analysisStart " + textBlock.getAnalysisStart() + ">= matcherStart [[" + matcherStart + "]] < analysisEnd "
							+ textBlock.getAnalysisEnd());
				}

				for (RawTextMarkType filterType : filterTypes) {

					switch (filterType) {
					case REPLACE: {
						String insertionText = RegexUtils.getReplacement(replacement, textBlock.getText(), matcher);
						if (LOG.isTraceEnabled()) {
							LOG.trace("Setting replacement to: " + insertionText);
						}
						RawTextMarker marker = new RawTextReplaceMarker(this.toString(), insertionText);
						Annotation<RawTextMarker> annotation = new Annotation<>(matcherStart, matcherEnd, marker, labels);
						rawTextMarkers.add(annotation);
						break;
					}
					case SENTENCE_BREAK: {
						RawTextMarker marker = new RawTextSentenceBreakMarker(this.toString());
						Annotation<RawTextMarker> annotation = new Annotation<>(matcherStart, matcherEnd, marker, labels);
						rawTextMarkers.add(annotation);
						break;
					}
					case NO_SENTENCE_BREAK: {
						RawTextMarker marker = new RawTextNoSentenceBreakMarker(this.toString());
						Annotation<RawTextMarker> annotation = new Annotation<>(matcherStart, matcherEnd, marker, labels);
						rawTextMarkers.add(annotation);
						break;

					}
					case SKIP: {
						RawTextMarker marker = new RawTextSkipMarker(this.toString());
						Annotation<RawTextMarker> annotation = new Annotation<>(matcherStart, matcherEnd, marker, labels);
						rawTextMarkers.add(annotation);
						break;
					}
					case TAG: {
						Annotation<TokenAttribute<?>> annotation = new Annotation<TokenAttribute<?>>(matcherStart, matcherEnd, this.attribute, labels);
						tokenAttributes.add(annotation);
						break;
					}
					default: {
						RawTextMarker marker = new RawTextMarker(filterType, this.toString());
						Annotation<RawTextMarker> annotation = new Annotation<>(matcherStart, matcherEnd, marker, labels);
						rawTextMarkers.add(annotation);
						break;
					}
					}

				}
			} else {
				if (LOG.isTraceEnabled()) {
					LOG.trace("Start out of range: analysisStart " + textBlock.getAnalysisStart() + ">= matcherStart [[" + matcherStart + "]] < analysisEnd "
							+ textBlock.getAnalysisEnd());
				}
			}
		}

		if (rawTextMarkers.size() > 0) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("For regex: " + this.regex.replace('\n', '¶').replace('\r', '¶'));
				LOG.debug("Added annotations: " + rawTextMarkers);
			}
		}

		if (rawTextMarkers.size() > 0)
			textBlock.addAnnotations(rawTextMarkers);
		if (tokenAttributes.size() > 0)
			textBlock.addAnnotations(tokenAttributes);
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
	public TokenAttribute<?> getAttribute() {
		return attribute;
	}

	@Override
	public void setAttribute(TokenAttribute<?> attribute) {
		this.attribute = attribute;
	}

}
