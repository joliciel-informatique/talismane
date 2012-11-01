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

import java.util.List;

public interface FilterService {
	/**
	 * For a given regex, finds any matches within the text,
	 * and adds the appropriate marker to these matches.
	 * The markers will automatically be added to the last group indicated in the regex,
	 * or to the entire regex if it contains no groups.
	 */
	public TextMarkerFilter getRegexMarkerFilter(List<MarkerFilterType> types, String regex);
	
	public TextMarkerFilter getDuplicateWhiteSpaceFilter();
	public TextMarkerFilter getNewlineEndOfSentenceMarker();
	public TextMarkerFilter getNewlineSpaceMarker();

	/**
	 * For a given regex, finds any matches within the text, and adds the appropriate marker to these matches.
	 * The markers will be added for the group indicated by the groupIndex.
	 */
	public TextMarkerFilter getRegexMarkerFilter(List<MarkerFilterType> types, String regex, int groupIndex);

	public RollingSentenceProcessor getRollingSentenceProcessor(String fileName, boolean processByDefault);
	
	public TextMarker getTextMarker(TextMarkerType type, int position);

	public Sentence getSentence(String text);
	public Sentence getSentence();
	
	public SentenceHolder getSentenceHolder();
	
	public TextMarkerFilter getTextMarkerFilter(String descriptor);
}
