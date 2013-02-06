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
	 * Remove the effect of the last skip marker.
	 * If two skip markers are nested, this has no effect.
	 * If a skip is nested in an include, this will start processing.
	 * Always matched to a PUSH_SKIP.
	 */
	POP_SKIP,
	/**
	 * Start processing again when you hit this marker (unless already started).
	 */
	PUSH_INCLUDE,
	/**
	 * Regardless of the current top-of-stack for processing, stops processing until either
	 * pop or end is reached.
	 */
	STOP,
	/**
	 * Insert a space when you hit this marker.
	 */
	SPACE,
	/**
	 * Insert text when you hit this marker.
	 */
	INSERT,
	/**
	 * Remove the effect of the last output marker.
	 * Always matched to a PUSH_OUTPUT.
	 */
	POP_OUTPUT,
	/**
	 * Remove the effect of the last include marker.
	 * If two include markers are nested, this has no effect.
	 * If an include is nested in an skip, this will stop processing.
	 * Always matched to a PUSH_INCLUDE.
	 */
	POP_INCLUDE,
	/**
	 * Regardless of the current top-of-stack for processing, starts processing until either
	 * pop or end is reached.
	 */
	START,
	/**
	 * Regardless of the current top-of-stack for output, starts output until either
	 * pop or end is reached (but only if not processing - will never output AND process).
	 */
	START_OUTPUT,
	/**
	 * Regardless of the current top-of-stack for output, stops processing until either
	 * pop or end is reached.
	 */
	STOP_OUTPUT,
	/**
	 * Output the raw contents in any output files produced by Talismane,
	 * assuming we're not processing.
	 */
	PUSH_OUTPUT,
	/**
	 * Stop processing when you hit this marker (unless already stopped).
	 */
	PUSH_SKIP,
}
