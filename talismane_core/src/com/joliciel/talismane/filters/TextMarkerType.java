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

/**
 * A marker that can be applied at a certain character position in the text,
 * indicating whether processing should stop or start again, and whether
 * a hard sentence break should be applied.<br/>
 * Note that declaration order is important, since SENTENCE_BREAK needs to be processed prior to the others.
 * @author Assaf Urieli
 *
 */
public enum TextMarkerType {
	/**
	 * Insert a sentence break when you hit this marker.
	 */
	SENTENCE_BREAK,
	/**
	 * Insert a space when you hit this marker.
	 */
	SPACE,
	/**
	 * Stop processing when you hit this marker.
	 */
	STOP,
	/**
	 * Output the raw contents in any output files produced by Talismane,
	 * assuming we're not processing.
	 */
	OUTPUT,
	/**
	 * Start processing again when you hit this marker.
	 */
	START,
	/**
	 * Remove the effect of the last start/stop marker.
	 * If two stop markers or two start markers are imbricated, this has no effect.
	 * If a stop is imbricated in a start or vice versa, this will begin/stop processing.
	 */
	END_MARKER
}
