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
 * @author Lucas Satabin
 *
 */
public abstract class Predicate extends Instruction {

	final byte id;

	public Predicate(int id) {
		super(OpCode.PREDICATE);
		this.id = (byte) id;
	}

	public abstract boolean apply(CharSequence input, int idx);

	public static Predicate valueOf(byte id) {
		switch (id) {
		case 1:
			return WordBoundaryPredicate.INSTANCE;
		default:
			throw new IllegalArgumentException("Unknown predicate " + id);
		}
	}

}
