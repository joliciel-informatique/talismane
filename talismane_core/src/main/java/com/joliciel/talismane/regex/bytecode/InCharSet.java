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

import java.util.List;
import java.util.SortedSet;
import java.util.Stack;

/**
 * @author Lucas Satabin
 *
 */
public class InCharSet extends Instruction {

	final List<CharRange> charset;

	public InCharSet(SortedSet<CharRange> charset) {
		super(OpCode.CHAR_SET);

		Stack<CharRange> stack = new Stack<>();
		for (CharRange range : charset) {
			if (stack.isEmpty() || !stack.peek().overlaps(range)) {
				stack.push(range);
			} else {
				CharRange prev = stack.pop();
				stack.push(prev.union(range));
			}
		}

		this.charset = stack.subList(0, stack.size());
	}

	// assume the ranges are non-overlapping and sorted
	// for internal use only, hence the package visibility
	InCharSet(List<CharRange> charset) {
		super(OpCode.CHAR_SET);
		this.charset = charset;
	}

	@Override
	public Instruction resolved(int base) {
		return this;
	}

	@Override
	public Instruction rebaseCaptures(int base) {
		return this;
	}

	public boolean contains(char c) {
		// implemented as a binary search because we know that the list of
		// ranges is sorted

		int l = 0;
		int r = charset.size() - 1;

		while (l <= r) {
			int m = (l + r) / 2;

			CharRange range = charset.get(m);
			if (range.contains(c)) {
				return true;
			} else if (range.before(c)) {
				l = m + 1;
			} else {
				r = m - 1;
			}
		}

		return false;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("in_charset [");
		for (CharRange range : charset) {
			builder.append(range.start);
			if (range.start < range.end) {
				builder.append('-').append(range.end);
			}
		}
		builder.append(']');
		return builder.toString();
	}

}
