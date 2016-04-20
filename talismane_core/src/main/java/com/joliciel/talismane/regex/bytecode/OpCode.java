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
 * The available byte code operation opcodes
 * 
 * @author Lucas Satabin
 *
 */
public enum OpCode {

	SOME_CHAR(1), ANY_CHAR(2), CHAR_SET(3), JUMP(4), SPLIT(5), SAVE(6), MATCH(7), PREDICATE(8), NAMED_CLASS(9);

	public final byte id;

	OpCode(int i) {
		this.id = (byte) i;
	}

	public static OpCode valueOf(byte id) {
		switch (id) {
		case 1:
			return SOME_CHAR;
		case 2:
			return ANY_CHAR;
		case 3:
			return CHAR_SET;
		case 4:
			return JUMP;
		case 5:
			return SPLIT;
		case 6:
			return SAVE;
		case 7:
			return MATCH;
		case 8:
			return PREDICATE;
		case 9:
			return NAMED_CLASS;
		default:
			throw new IllegalArgumentException("Unknown opcode " + id);
		}
	}

}
