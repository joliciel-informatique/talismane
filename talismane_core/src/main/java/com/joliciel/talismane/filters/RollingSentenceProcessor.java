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

import java.util.Set;

import com.joliciel.talismane.utils.io.CurrentFileObserver;

/**
 * A raw text processor which processes text block by block, applies text markers,
 * and returns SentenceHolders. These sentence holders will then be used
 * to detect the actual sentences.
 * @author Assaf Urieli
 */
public interface RollingSentenceProcessor extends CurrentFileObserver {
	/**
	 * Adds a segment of original text, and the textMarkers corresponding it.
	 * @param segment the next segment of original text
	 * @param textMarkers the text markers corresponding this segment
	 * @return a SentenceHolder corresponding to this segment of original text
	 */
	public SentenceHolder addNextSegment(String segment, Set<TextMarker> textMarkers);

}