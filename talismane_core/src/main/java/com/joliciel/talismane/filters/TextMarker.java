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

import com.joliciel.talismane.tokeniser.TokenAttribute;

/**
 * A text marker, indicating a position within a block of text and the action to
 * take.
 * 
 * @author Assaf Urieli
 *
 */
public interface TextMarker extends Comparable<TextMarker> {
	/**
	 * The marker type.
	 */
	public TextMarkerType getType();

	/**
	 * The marker position in the original text.
	 */
	public int getPosition();

	public void setPosition(int position);

	/**
	 * The text that should be inserted at this marker position.
	 */
	public String getInsertionText();

	public void setInsertionText(String insertionText);

	/**
	 * The filter which generated this text marker.
	 */
	public TextMarkerFilter getSource();

	public void setSource(TextMarkerFilter source);

	/**
	 * The text that was matched.
	 */
	public String getMatchText();

	public void setMatchText(String matchText);

	/**
	 * The tag value to add with this marker.
	 */
	public TokenAttribute<?> getValue();

	/**
	 * The tag attribute to add with this marker.
	 */
	public String getAttribute();

	/**
	 * The marker's attribute and value tag.
	 */
	public <T> void setTag(String attribute, TokenAttribute<T> value);
}
