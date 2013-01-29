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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.utils.RegexUtils;

/**
 * For a given regex, finds any matches within the text, and adds the appropriate marker to these matches.
 * If not group is provided, will match the expression as a whole.
 * @author Assaf Urieli
 *
 */
class RegexMarkerFilter implements TextMarkerFilter {
	private static final Log LOG = LogFactory.getLog(RegexMarkerFilter.class);
	private List<MarkerFilterType> filterTypes;
	private Pattern pattern;
	private String regex;
	private int groupIndex = 0;
	private String replacement;
	
	private FilterService filterService;
	
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
		this.pattern = Pattern.compile(regex);
		if (groupIndex<0) {
			throw new TalismaneException("Cannot have a group index < 0: " + groupIndex);
		}
		this.groupIndex = groupIndex;
	}

	@Override
	public Set<TextMarker> apply(String prevText, String text, String nextText) {
		if (LOG.isTraceEnabled()) {
			LOG.trace("Matching " + regex + "");
		}
		String context = prevText + text + nextText;
		
		int textStartPos = prevText.length();
		int textEndPos = prevText.length() + text.length();

		Matcher matcher = pattern.matcher(context);
		Set<TextMarker> textMarkers = new TreeSet<TextMarker>();
		while (matcher.find()) {
			int matcherStart = 0;
			int matcherEnd = 0;
			if (groupIndex==0) {
				matcherStart = matcher.start();
				matcherEnd = matcher.end();
			} else {
				matcherStart = matcher.start(groupIndex);
				matcherEnd = matcher.end(groupIndex);
			}
			
			if (matcherStart>=textStartPos && matcherStart<textEndPos) {
				if (LOG.isTraceEnabled()) {
					LOG.trace("Next match: " + context.substring(matcher.start(), matcher.end()));
					if (matcher.start()!=matcherStart || matcher.end()!=matcherEnd) {
						LOG.trace("But matching group: " + context.substring(matcherStart, matcherEnd));
					}
				}
				
				for (MarkerFilterType filterType : filterTypes) {
					switch (filterType) {
					case SKIP:
					{
						TextMarker textMarker = this.getFilterService().getTextMarker(TextMarkerType.PUSH_SKIP, matcherStart - prevText.length());
						textMarkers.add(textMarker);
						break;
					}
					case SENTENCE_BREAK:
					{
						TextMarker textMarker = this.getFilterService().getTextMarker(TextMarkerType.SENTENCE_BREAK, matcherStart - prevText.length());
						textMarkers.add(textMarker);
						break;
					}
					case SPACE:
					{
						TextMarker textMarker = this.getFilterService().getTextMarker(TextMarkerType.SPACE, matcherStart - prevText.length());
						textMarker.setInsertionText(" ");
						textMarkers.add(textMarker);
						TextMarker textMarker2 = this.getFilterService().getTextMarker(TextMarkerType.PUSH_SKIP, matcherStart - prevText.length());
						textMarkers.add(textMarker2);
						break;
					}
					case REPLACE:
					{
						TextMarker textMarker = this.getFilterService().getTextMarker(TextMarkerType.INSERT, matcherStart - prevText.length());
						String newText = RegexUtils.getReplacement(replacement, context, matcher);
						if (LOG.isTraceEnabled()) {
							LOG.trace("Setting replacement to: " + newText);
						}
						textMarker.setInsertionText(newText);
						textMarkers.add(textMarker);
						TextMarker textMarker2 = this.getFilterService().getTextMarker(TextMarkerType.PUSH_SKIP, matcherStart - prevText.length());
						textMarkers.add(textMarker2);
						break;
					}
					case OUTPUT:
					{
						TextMarker textMarker = this.getFilterService().getTextMarker(TextMarkerType.PUSH_OUTPUT, matcherStart - prevText.length());
						textMarkers.add(textMarker);
						TextMarker textMarker2 = this.getFilterService().getTextMarker(TextMarkerType.PUSH_SKIP, matcherStart - prevText.length());
						textMarkers.add(textMarker2);
						break;
					}
					case INCLUDE:
					{
						TextMarker textMarker = this.getFilterService().getTextMarker(TextMarkerType.PUSH_INCLUDE, matcherStart - prevText.length());
						textMarkers.add(textMarker);
						break;
					}
					case OUTPUT_START:
					{
						TextMarker textMarker = this.getFilterService().getTextMarker(TextMarkerType.START_OUTPUT, matcherStart - prevText.length());
						textMarkers.add(textMarker);
						break;
					}
					case STOP:
					{
						TextMarker textMarker = this.getFilterService().getTextMarker(TextMarkerType.STOP, matcherStart - prevText.length());
						textMarkers.add(textMarker);
						break;
					}
					}
				}
			}
			if (matcherEnd>=textStartPos && matcherEnd<textEndPos) {
				for (MarkerFilterType filterType : filterTypes) {
					switch (filterType) {
					case SKIP: case SPACE: case REPLACE:
					{
						TextMarker textMarker = this.getFilterService().getTextMarker(TextMarkerType.POP_SKIP, matcherEnd - prevText.length());
						textMarkers.add(textMarker);
						break;
					}
					case OUTPUT:
					{
						TextMarker textMarker = this.getFilterService().getTextMarker(TextMarkerType.POP_OUTPUT, matcherEnd - prevText.length());
						textMarkers.add(textMarker);
						TextMarker textMarker2 = this.getFilterService().getTextMarker(TextMarkerType.POP_SKIP, matcherEnd - prevText.length());
						textMarkers.add(textMarker2);
						break;
					}
					case INCLUDE:
					{
						TextMarker textMarker = this.getFilterService().getTextMarker(TextMarkerType.POP_INCLUDE, matcherEnd - prevText.length());
						textMarkers.add(textMarker);
						break;
					}
					case START:
					{
						TextMarker textMarker = this.getFilterService().getTextMarker(TextMarkerType.START, matcherEnd - prevText.length());
						textMarkers.add(textMarker);
						break;
					}
					case OUTPUT_STOP:
					{
						TextMarker textMarker = this.getFilterService().getTextMarker(TextMarkerType.STOP_OUTPUT, matcherEnd - prevText.length());
						textMarkers.add(textMarker);
						break;
					}
					}
				}
			}
		} // next match
		
		if (textMarkers.size()>0)
			LOG.debug("Added markers: " + textMarkers);
		return textMarkers;
	}
	
	public String getFind() {
		return regex;
	}
	
	public FilterService getFilterService() {
		return filterService;
	}

	public void setFilterService(FilterService filterService) {
		this.filterService = filterService;
	}

	public String getReplacement() {
		return replacement;
	}

	public void setReplacement(String replacement) {
		this.replacement = replacement;
	}


}
