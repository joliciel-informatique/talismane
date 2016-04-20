///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2016 Safety Data -CFH
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
package com.joliciel.talismane.regex.bytecode;

/**
 * An inclusive character ranger.
 * 
 * @author Lucas Satabin
 *
 */
public class CharRange implements Comparable<CharRange> {

	public final char start;
	public final char end;

	public CharRange(char start, char end) {
		assert start <= end;
		this.start = start;
		this.end = end;
	}

	public CharRange(char c) {
		this.start = c;
		this.end = c;
	}

	public boolean before(char c) {
		return end < c;
	}

	public boolean overlaps(CharRange that) {
		return this.start <= that.end && this.end >= that.start;
	}

	public CharRange union(CharRange that) {
		assert this.overlaps(that);
		// these are min/max, but it avoids conversion as standard min/max
		// functions return integers
		char start1 = this.start <= that.start ? this.start : that.start;
		char end1 = this.end >= that.end ? this.end : that.end;
		return new CharRange(start1, end1);
	}

	public boolean contains(char c) {
		return this.start <= c && c <= this.end;
	}

	@Override
	public int compareTo(CharRange that) {
		return this.start - that.start;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
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
		CharRange other = (CharRange) obj;
		if (end != other.end)
			return false;
		if (start != other.start)
			return false;
		return true;
	}

	@Override
	public String toString() {
		if (start == end) {
			return "" + start;
		} else {
			return "[" + start + "-" + end + "]";
		}
	}

	public boolean hasAnyLetter() {
		// TODO this can probably be improved a lot by using unicode properties
		for (char c = start; c <= end; c++) {
			if (Character.isLetter(c)) {
				return true;
			}
		}
		return false;
	}

}
