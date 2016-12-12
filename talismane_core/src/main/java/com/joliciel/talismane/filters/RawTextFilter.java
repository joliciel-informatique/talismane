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

import com.joliciel.talismane.AnnotatedText;
import com.joliciel.talismane.tokeniser.TokenAttribute;

/**
 * A filter applied to a block of raw text and producing a list of text markers.
 * 
 * @author Assaf Urieli
 *
 */
public interface RawTextFilter {
	/**
	 * Adds annotations of type {@link RawTextMarker} to the textBlock.
	 */
	public void annotate(AnnotatedText textBlock);

	/**
	 * If the filter includes text replacement, the replacement string.
	 */
	public String getReplacement();

	public void setReplacement(String replacement);

	/**
	 * The maximum size of text that this filter can match (without risking to
	 * add only the beginning and not the end, or vice versa). Bigger matches
	 * will throw an error.
	 */
	public int getBlockSize();

	/**
	 * If the filter adds a tag, the value to add.
	 */
	public TokenAttribute<?> getAttribute();

	/**
	 * Set the tag added by this filter.
	 */
	public void setAttribute(TokenAttribute<?> attribute);
}
