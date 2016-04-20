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

import java.util.Arrays;

import com.joliciel.talismane.regex.Match;
import com.joliciel.talismane.regex.Matcher;

/**
 * @author Lucas Satabin
 *
 */
public class CompiledRegex {

	public final int nbSaved;
	public final Instruction[] instructions;

	public CompiledRegex(int nbSaved, Instruction[] instructions) {
		this.nbSaved = nbSaved;
		this.instructions = instructions;
	}

	public CompiledRegex or(CompiledRegex that) {
		int nbSaved = this.nbSaved + that.nbSaved;

		// split L1, L2
		// L1: codes for e1
		// L2: codes for e2
		// note: e1 and e2 MUST end with a MATCH
		Instruction[] newInstructions = new Instruction[this.instructions.length + that.instructions.length + 1];

		// rebase instruction index
		Instruction[] newThis = Arrays.stream(this.instructions).map(i -> i.resolved(1))
				.toArray(n -> new Instruction[n]);
		// rebase instruction and save indices
		Instruction[] newThat = Arrays.stream(that.instructions)
				.map(i -> i.rebaseCaptures(this.nbSaved).resolved(this.instructions.length + 1))
				.toArray(n -> new Instruction[n]);

		newInstructions[0] = new Split(1, 1 + newThis.length);
		System.arraycopy(newThis, 0, newInstructions, 1, newThis.length);
		System.arraycopy(newThat, 0, newInstructions, newThis.length + 1, newThat.length);

		return new CompiledRegex(nbSaved, newInstructions);
	}

	public Match findFirstIn(String input) {
		Matcher matcher = new Matcher(input, this);
		for (Match m : matcher) {
			return m;
		}
		return null;
	}

	public Matcher findIn(String input) {
		return new Matcher(input, this);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < instructions.length; i++) {
			sb.append(i).append(": ").append(instructions[i]).append('\n');
		}
		return sb.toString();
	}

}
