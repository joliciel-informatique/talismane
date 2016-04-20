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
package com.joliciel.talismane.regex;

import java.util.regex.MatchResult;

/**
 * A matching area from an input string.
 * 
 * @author Lucas Satabin
 *
 */
public class Match implements MatchResult, Comparable<Match> {

	private final CharSequence input;
	private final int start;
	private final int end;
	private final int[] groups;
	private final int id;

	public Match(int start, int end, CharSequence input, int[] groups, int id) {
		this.start = start;
		this.end = end;
		this.input = input;
		this.groups = groups;
		this.id = id;
	}

	/**
	 * Returns the content of the given group number, or <code>null</code> if it does not exist. group '0' returns the
	 * entire matching region.
	 */
	@Override
	public String group(int nb) {
		if (nb == 0) {
			return input.subSequence(start, end).toString();
		} else if (nb > 0 && nb <= groups.length / 2) {
			if (groups[2 * (nb - 1)] < 0 || groups[2 * (nb - 1) + 1] < 0) {
				return null;
			} else {
				return input.subSequence(groups[2 * (nb - 1)], groups[2 * (nb - 1) + 1]).toString();
			}
		} else {
			return null;
		}
	}

	@Override
	public int start() {
		return start;
	}

	@Override
	public int start(int nb) {
		if (nb == 0) {
			return start;
		} else if (nb > 0 && nb <= groups.length / 2) {
			return groups[2 * (nb - 1)];
		} else {
			return -1;
		}
	}

	@Override
	public int end() {
		return end;
	}

	@Override
	public int end(int nb) {
		if (nb == 0) {
			return end;
		} else if (nb > 0 && nb <= groups.length / 2) {
			return groups[2 * (nb - 1) + 1];
		} else {
			return -1;
		}
	}

	@Override
	public String toString() {
		return "Match [start=" + start + ", end=" + end + "]";
	}

	@Override
	public String group() {
		return group(0);
	}

	@Override
	public int groupCount() {
		return groups.length / 2;
	}

	@Override
	public int compareTo(Match that) {
		return this.start - that.start;
	}

	public int getId() {
		return id;
	}

}
