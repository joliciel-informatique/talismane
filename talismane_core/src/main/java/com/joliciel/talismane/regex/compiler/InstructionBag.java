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
package com.joliciel.talismane.regex.compiler;

import java.util.LinkedList;

import com.joliciel.talismane.regex.bytecode.Instruction;
import com.joliciel.talismane.regex.bytecode.MatchFound;
import com.joliciel.talismane.regex.bytecode.OpCode;

/**
 * A bag of instruction used in the compiler.
 * 
 * @author Lucas Satabin
 *
 */
class InstructionBag {

	final LinkedList<Instruction> insts;

	InstructionBag() {
		this.insts = new LinkedList<>();
	}

	InstructionBag(Instruction inst) {
		this.insts = new LinkedList<>();
		insts.add(inst);
	}

	public void concat(InstructionBag that) {
		this.insts.addAll(that.insts);
	}

	public void prepend(Instruction inst) {
		insts.addFirst(inst);
	}

	public void append(Instruction inst) {
		insts.addLast(inst);
	}

	public int size() {
		return insts.size();
	}

	public InstructionBag copy() {
		InstructionBag result = new InstructionBag();
		for (Instruction i : insts) {
			result.insts.add(i);
		}
		return result;
	}

	@Override
	public String toString() {
		int i = 0;
		StringBuilder builder = new StringBuilder();
		for (Instruction inst : insts) {
			builder.append(i).append('\t').append(inst).append('\n');
			i++;
		}
		return builder.toString();
	}

	public Instruction[] compile(int id) {
		final Instruction[] compiled;
		// add potentially missing match in the end
		if (insts.size() == 0 || insts.peekLast().opcode != OpCode.MATCH) {
			compiled = new Instruction[size() + 1];
			if (id == -1) {
				compiled[compiled.length - 1] = MatchFound.ANONYMOUS;
			} else {
				compiled[compiled.length - 1] = new MatchFound(id);
			}
		} else {
			compiled = new Instruction[size()];
		}
		// resolve jumps as absolute indices
		int idx = 0;
		for (Instruction inst : insts) {
			compiled[idx] = inst.resolved(idx);
			idx++;
		}
		return compiled;
	}

}
