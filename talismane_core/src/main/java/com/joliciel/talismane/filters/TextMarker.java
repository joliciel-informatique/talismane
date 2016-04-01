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
public class TextMarker implements Comparable<TextMarker> {
	private TextMarkerType type;
	private int position;
	private String insertionText;
	private TextMarkerFilter source;
	private String matchText;
	private String attribute;
	private TokenAttribute<?> value;

	public TextMarker(TextMarkerType type, int position) {
		super();
		this.type = type;
		this.position = position;
	}

	public TextMarker(TextMarkerType type, int position, TextMarkerFilter source) {
		super();
		this.type = type;
		this.position = position;
		this.source = source;
	}

	public TextMarker(TextMarkerType type, int position, TextMarkerFilter source, String matchText) {
		super();
		this.type = type;
		this.position = position;
		this.source = source;
		this.matchText = matchText;
	}

	/**
	 * The marker type.
	 */
	public TextMarkerType getType() {
		return type;
	}

	/**
	 * The marker position in the original text.
	 */
	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	@Override
	public int compareTo(TextMarker o) {
		if (this == o)
			return 0;
		if (this.position != o.getPosition())
			return this.position - o.getPosition();
		if (!this.getType().equals(o.getType()))
			return this.getType().compareTo(o.getType());
		return 1;
	}

	@Override
	public String toString() {
		return "TextMarkerImpl [type=" + type + ", position=" + position + "]";
	}

	/**
	 * The text that should be inserted at this marker position.
	 */
	public String getInsertionText() {
		return insertionText;
	}

	public void setInsertionText(String insertionText) {
		this.insertionText = insertionText;
	}

	/**
	 * The filter which generated this text marker.
	 */
	public TextMarkerFilter getSource() {
		return source;
	}

	public void setSource(TextMarkerFilter source) {
		this.source = source;
	}

	/**
	 * The text that was matched.
	 */
	public String getMatchText() {
		return matchText;
	}

	public void setMatchText(String matchText) {
		this.matchText = matchText;
	}

	/**
	 * The marker's attribute and value tag.
	 */
	public <T> void setTag(String attribute, TokenAttribute<T> value) {
		this.attribute = attribute;
		this.value = value;
	}

	/**
	 * The tag attribute to add with this marker.
	 */
	public String getAttribute() {
		return attribute;
	}

	/**
	 * The tag value to add with this marker.
	 */
	public TokenAttribute<?> getValue() {
		return value;
	}
}
