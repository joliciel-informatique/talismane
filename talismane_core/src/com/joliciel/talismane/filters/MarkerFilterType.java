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

import com.joliciel.talismane.tokeniser.Token;

/**
 * An action to take by a particular TextMarkerFilter.<br/>
 * These actions are either stack based or unary.<br/>
 * Stack-based markers can be nested.<br/>
 * If unary markers (e.g. start and end markers) are placed inside an
 * area marked by a stack-based marker, the will only affect this area.<br/>
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
	 * in any output file produced by Talismane (stack-based).</br>
	 * Actual raw output segments preceding each token can be retrieved by
	 * {@link Token#getPrecedingRawOutput()}.
	 */
	OUTPUT,
	/**
	 * Insert a sentence break.
	 */
	SENTENCE_BREAK,
	/**
	 * Replace the text with a space.
	 * Only applies if the current text is marked for processing.
	 */
	SPACE,
	/**
	 * Replace the text with another text. Should only be used for encoding
	 * replacements which don't change meaning - e.g. replace "&amp;eacute;" by "é".
	 * Only applies if the current text is marked for processing.
	 */
	REPLACE,
	/**
	 * Mark the beginning of a section to be skipped (without an explicit end).<br/>
	 * Note that the processing will stop at the beginning of the match.<br/>
	 * If this marker is placed inside an area marked by SKIP, INCLUDE or OUTPUT,
	 * it will only take effect within this area. It can be reversed by a START marker.
	 */
	STOP,
	/**
	 * Mark the beginning of a section to be processed (without an explicit end).<br/>
	 * Note that the processing will begin AFTER the end of the match.<br/>
	 * If this marker is placed inside an area marked by SKIP, INCLUDE or OUTPUT,
	 * it will only take effect within this area. It can be reversed by a START marker.
	 */
	START,
	/**
	 * Mark the beginning of a section to be outputted (without an explicit end).<br/>
	 * Will only actually output if processing is stopped.<br/>
	 * Stopping needs to be marked separately (via a STOP marker).<br/>
	 * Note that the output will begin at the beginning of the match.<br/>
	 * If this marker is placed inside an area marked by OUTPUT,
	 * it will only take effect within this area. It can be reversed by a OUTPUT_STOP marker.</br>
	 * Actual raw output segments preceding each token can be retrieved by
	 * {@link Token#getPrecedingRawOutput()}.
	 */
	OUTPUT_START,
	/**
	 * Mark the end of a section to be outputted (without an explicit beginning).<br/>
	 * Starting the processing needs to be marked separately.<br/>
	 * Note that the output will stop at the end of the match.<br/>
	 * If this marker is placed inside an area marked by OUTPUT,
	 * it will only take effect within this area. It can be reversed by a OUTPUT_START marker.
	 */
	OUTPUT_STOP,
	/**
	 * Does absolutely nothing - only used in code, useless in a filter.
	 */
	NONE,
}