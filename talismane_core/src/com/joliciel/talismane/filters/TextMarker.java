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
 * A text marker, indicating a position within a block of text and the action to take.
 * @author Assaf Urieli
 *
 */
public interface TextMarker extends Comparable<TextMarker> {
	/**
	 * The marker type.
	 * @return
	 */
	public TextMarkerType getType();
	/**
	 * The marker position in the original text.
	 * @return
	 */
	public int getPosition();
	public void setPosition(int position);
	
	/**
	 * The text that should be inserted at this marker position.
	 * @return
	 */
	public String getInsertionText();
	public void setInsertionText(String insertionText);
}
