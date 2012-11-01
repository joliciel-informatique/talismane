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
 * An action to take by a particular TextMarkerFilter.<br/>
 * These actions are either stack based or unary.<br/>
 * Stack-based actions can be nested.<br/>
 * Unary actions (e.g. start and end markers) only affect the current top of stack.<br/>
 * For maximum robustness, the best strategy is to reserve stack-based markers for
 * very short segments, and use unary markers instead of excessive nesting.<br/>
 * @author Assaf Urieli
 *
 */
public enum MarkerFilterType {
	/**
	 * Skip any text matching this filter (stack-based).
	 */
	SKIP,
	/**
	 * Include any text matching this filter (stack-based).
	 */
	INCLUDE,
	/**
	 * Skip any text matching this filter, and output its raw content
	 * in any output file produced by Talismane (stack-based).
	 */
	OUTPUT,
	/**
	 * Insert a sentence break.
	 */
	SENTENCE_BREAK,
	/**
	 * Replace the text with a space.
	 */
	SPACE,
	/**
	 * Replace the text with another text. Should only be used for encoding
	 * replacements which don't change meaning - e.g. replace "&amp;eacute;" by "é".
	 */
	REPLACE,
	/**
	 * Mark the beginning of a section to be skipped (without an explicit end).
	 */
	STOP,
	/**
	 * Mark the beginning of a section to be processed (without an explicit end).
	 */
	START,
	/**
	 * Mark the beginning of a section to be outputted (without an explicit end).
	 */
	OUTPUT_START,
	/**
	 * Mark the end of a section to be outputted (without an explicit beginning).
	 */
	OUTPUT_STOP,
	/**
	 * No marking for this filter - only used in code, useless in a filter.
	 */
	NONE,
}