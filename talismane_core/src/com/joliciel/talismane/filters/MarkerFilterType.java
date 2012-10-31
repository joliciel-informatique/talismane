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
 * An action to take by a particular TextMarkerFilter.
 * @author Assaf Urieli
 *
 */
public enum MarkerFilterType {
	/**
	 * Skip any text matching this filter.
	 */
	SKIP,
	/**
	 * Include any text matching this filter
	 * (if imbricated inside a skip filter).
	 */
	INCLUDE,
	/**
	 * Insert a sentence break.
	 */
	SENTENCE_BREAK,
	/**
	 * Insert a space.
	 */
	SPACE,
	/**
	 * Skip any text matching this filter, and output its raw content
	 * in any output file produced by Talismane.
	 */
	OUTPUT,
	/**
	 * No marking for this filter - only used in code, useless in a filter.
	 */
	NONE,
}