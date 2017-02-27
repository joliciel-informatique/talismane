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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.LinkedList;
import java.util.List;

/**
 * This class is responsible for serializing and deserializing programs. The structure of a serialized
 * {@link CompiledRegex} is as follows:
 * <ul>
 * <li>number of capture groups (int32)</li>
 * <li>number of instructions (int32)</li>
 * <li>for each instruction, its serialized form. The serialized form of any instruction is composed of the opcode
 * (byte), followed by its arguments if any. The arguments of instructions are as follows:
 * <ul>
 * <li>{@code SOME_CHAR}: the character to match (char)</li>
 * <li>{@code ANY_CHAR}: no argument</li>
 * <li>{@code CHAR_SET}:
 * <ul>
 * <li>number of ranges (int32)</li>
 * <li>for each range, the start character and end character (char * 2)</li>
 * </ul>
 * </li>
 * <li>{@code JUMP}: the address where to jump (int32)</li>
 * <li>{@code MATCH}: no argument</li>
 * <li>{@code PREDICATE}: predicate identifier (int32)</li>
 * <li>{@code SAVE}: the register number (int32)</li>
 * <li>{@code SPLIT}: the addresses where to jump (int32 * 2)</li>
 * <li>{@code NAMED_CLASS}: the class name (utf8 string)</li>
 * </ul>
 * </li>
 * </ul>
 * 
 * @author Lucas Satabin
 *
 */
public class CompiledRegexSerializer {

	public static void serialize(ObjectOutput out, CompiledRegex program) throws IOException {
		// write number of capturing groups
		out.writeInt(program.nbSaved);
		// write number of instructions
		out.writeInt(program.instructions.length);
		// write instructions in order
		for (Instruction i : program.instructions) {
			// first write the instruction opcode
			out.writeByte(i.opcode.id);
			// then depending on the instruction, write the arguments
			switch (i.opcode) {
			case ANY_CHAR:
				// no parameter for this instruction
				break;
			case CHAR_SET:
				List<CharRange> ranges = ((InCharSet) i).charset;
				// write the number of ranges
				out.writeInt(ranges.size());
				// and then each lower-upper values in sequence
				for (CharRange r : ranges) {
					out.writeChar(r.start);
					out.writeChar(r.end);
				}
				break;
			case JUMP:
				// write the address where to jump
				out.writeInt(((Jump) i).next);
				break;
			case MATCH:
				// write the match identifier
				out.writeInt(((MatchFound) i).id);
				break;
			case PREDICATE:
				// write the predicate id
				out.writeByte(((Predicate) i).id);
				break;
			case SAVE:
				// write the register number
				out.writeInt(((Save) i).nb);
				break;
			case SOME_CHAR:
				// write the expected character
				out.writeChar(((SomeChar) i).c);
				break;
			case SPLIT:
				// write both addresses where to jump
				Split split = (Split) i;
				out.writeInt(split.next1);
				out.writeInt(split.next2);
				break;
			case NAMED_CLASS:
				// write the class name
				out.writeUTF(((InNamedClass) i).name);
				break;
			}
		}
		out.flush();
	}

	public static CompiledRegex deserialize(ObjectInput in) throws IOException {
		// read number of capturing groups
		int nbSaved = in.readInt();
		// read number of instructions
		int nbInstructions = in.readInt();
		Instruction[] instructions = new Instruction[nbInstructions];
		for (int i = 0; i < nbInstructions; i++) {
			// read the opcode
			OpCode opcode = OpCode.valueOf(in.readByte());
			// then depending on the opcode, read the arguments
			switch (opcode) {
			case ANY_CHAR:
				// no parameter for this instruction
				instructions[i] = AnyChar.INSTANCE;
				break;
			case CHAR_SET:
				// read the number of ranges
				int nbranges = in.readInt();
				List<CharRange> ranges = new LinkedList<>();
				for (int j = 0; j < nbranges; j++) {
					// read lower-upper values
					char start = in.readChar();
					char end = in.readChar();
					ranges.add(new CharRange(start, end));
				}
				instructions[i] = new InCharSet(ranges);
				break;
			case JUMP:
				// read the address where to jump
				instructions[i] = new Jump(in.readInt());
				break;
			case MATCH:
				// read the match identifier
				int id = in.readInt();
				if (id < 0) {
					instructions[i] = MatchFound.ANONYMOUS;
				} else {
					instructions[i] = new MatchFound(id);
				}
				break;
			case PREDICATE:
				// read the predicate id
				instructions[i] = Predicate.valueOf(in.readByte());
				break;
			case SAVE:
				// read the register number
				instructions[i] = new Save(in.readInt());
				break;
			case SOME_CHAR:
				// read the expected character
				instructions[i] = new SomeChar(in.readChar());
				break;
			case SPLIT:
				// read both addresses where to jump
				int next1 = in.readInt();
				int next2 = in.readInt();
				instructions[i] = new Split(next1, next2);
				break;
			case NAMED_CLASS:
				// read the class name
				String name = in.readUTF();
				instructions[i] = new InNamedClass(name);
				break;
			}
		}
		return new CompiledRegex(nbSaved, instructions);
	}

}
