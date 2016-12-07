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

/**
 * A marker added in an annotation by a TextMarkerFilter.
 * 
 * @author Assaf Urieli
 *
 */
public class RawTextMarker {
	private final RawTextMarkType type;
	private final String source;
	private final String insertionText;

	public static class RawTextSentenceBreakMarker extends RawTextMarker {
		public RawTextSentenceBreakMarker(String source) {
			super(RawTextMarkType.SENTENCE_BREAK, source);
		}
	}

	public static class RawTextSkipMarker extends RawTextMarker {
		public RawTextSkipMarker(String source) {
			super(RawTextMarkType.SKIP, source);
		}
	}

	public static class RawTextReplaceMarker extends RawTextMarker {
		public RawTextReplaceMarker(String source, String insertionText) {
			super(RawTextMarkType.REPLACE, source, insertionText);
		}
	}

	public RawTextMarker(RawTextMarkType type, String source) {
		this(type, source, null);
	}

	private RawTextMarker(RawTextMarkType type, String source, String insertionText) {
		this.type = type;
		this.source = source;
		this.insertionText = insertionText;
	}

	/**
	 * The marker type.
	 */
	public RawTextMarkType getType() {
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

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " [type=" + type + ", source=" + source + ", insertionText=" + insertionText + "]";
	}
}
