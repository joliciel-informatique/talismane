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

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.joliciel.talismane.regex.bytecode.CompiledRegex;
import com.joliciel.talismane.regex.compiler.Compiler;
import com.joliciel.talismane.regex.vm.VM;
import com.joliciel.talismane.resources.WordListFinder;

/**
 * A lazy iterable over matching areas in an input string for a given regular expression computing matches as it goes.
 * 
 * @author Lucas Satabin
 *
 */
public class Matcher implements Iterable<Match> {

	private final CharSequence input;
	private final CompiledRegex program;

	public Matcher(CharSequence input, String regex) {
		this(input, regex, null, 0);
	}

	public Matcher(CharSequence input, String regex, WordListFinder wordListFinder, int flags) {
		this.input = input;
		this.program = new Compiler(flags, wordListFinder).compile(regex);
	}

	public Matcher(CharSequence input, String regex, int flags) {
		this(input, regex, null, flags);
	}

	public Matcher(CharSequence input, String regex, WordListFinder wordListFinder) {
		this(input, regex, wordListFinder, 0);
	}

	public Matcher(CharSequence input, CompiledRegex program) {
		this.input = input;
		this.program = program;
	}

	@Override
	public Iterator<Match> iterator() {
		return new Iterator<Match>() {

			private int position = 0;
			private Match nextMatch = null;

			@Override
			public boolean hasNext() {
				while (nextMatch == null && position <= input.length()) {
					nextMatch = VM.execute(program, input, position);
					position++;
				}
				return nextMatch != null;
			}

			@Override
			public Match next() {
				if (nextMatch == null) {
					throw new NoSuchElementException();
				}
				Match next = nextMatch;
				nextMatch = null;
				return next;
			}

		};
	}

}
