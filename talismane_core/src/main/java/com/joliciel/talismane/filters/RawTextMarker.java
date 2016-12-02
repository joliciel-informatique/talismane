///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2016 Joliciel Informatique
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
 * A marker added in an annotation by a TextMarkerFilter.
 * 
 * @author Assaf Urieli
 *
 */
public class RawTextMarker {
	private final TextMarkType type;
	private final String source;
	private final String insertionText;
	private final String attribute;
	private final TokenAttribute<?> value;

	public RawTextMarker(TextMarkType type, String source) {
		this(type, source, null, null, null);
	}

	public RawTextMarker(TextMarkType type, String source, String insertionText) {
		this(type, source, insertionText, null, null);
	}

	public RawTextMarker(TextMarkType type, String source, String attribute, TokenAttribute<?> value) {
		this(type, source, null, attribute, value);
	}

	private RawTextMarker(TextMarkType type, String source, String insertionText, String attribute, TokenAttribute<?> value) {
		this.type = type;
		this.source = source;
		this.insertionText = insertionText;
		this.attribute = attribute;
		this.value = value;
	}

	/**
	 * The marker type.
	 */
	public TextMarkType getType() {
		return type;
	}

	/**
	 * The source which generated this text marker.
	 */
	public String getSource() {
		return source;
	}

	/**
	 * The text that should be inserted before the span marked by this marker.
	 */
	public String getInsertionText() {
		return insertionText;
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

	@Override
	public String toString() {
		return "RawTextMarker [type=" + type + ", source=" + source + ", insertionText=" + insertionText + ", attribute=" + attribute + ", value=" + value
				+ "]";
	}
}
