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

package com.joliciel.talismane;

/**
 * Annotation added to a continuous span of text. Examples could include
 * identifying a named entity, identifying token boundaries, or identifying
 * sentence boundaries.
 * 
 * Note: the Comparable methods only provide span ordering, and are inconsistent
 * with equals, which takes the data into account as well. Span ordering is by
 * start index, and for equal start indexes, from longest span to shortest.
 * 
 * @author Assaf Urieli
 *
 * @param <T>
 *            the data added by this annotation
 */
public final class Annotation<T> implements Comparable<Annotation<?>> {
	private final int start;
	private final int end;
	private final T data;

	public Annotation(int start, int end, T data) {
		super();
		this.start = start;
		this.end = end;
		this.data = data;
	}

	/**
	 * The first character position to which the annotation applies.
	 */
	public int getStart() {
		return start;
	}

	/**
	 * The position immediately after the last character to which the annotation
	 * applied.
	 * 
	 */
	public int getEnd() {
		return end;
	}

	/**
	 * The annotation data.
	 */
	public T getData() {
		return data;
	}

	@Override
	public int compareTo(Annotation<?> that) {
		if (this.start != that.start)
			return this.start - that.start;
		return that.end - this.end;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((data == null) ? 0 : data.hashCode());
		result = prime * result + end;
		result = prime * result + start;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		@SuppressWarnings("rawtypes")
		Annotation other = (Annotation) obj;
		if (data == null) {
			if (other.data != null)
				return false;
		} else if (!data.equals(other.data))
			return false;
		if (end != other.end)
			return false;
		if (start != other.start)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Annotation [start=" + start + ", end=" + end + ", data=" + data + "]";
	}
}
