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
public class Split extends Instruction {

	public final int next1;
	public final int next2;

	public Split(int next1, int next2) {
		super(OpCode.SPLIT);
		this.next1 = next1;
		this.next2 = next2;
	}

	@Override
	public String toString() {
		return "split " + next1 + ", " + next2;
	}

	@Override
	public Split resolved(int base) {
		return new Split(next1 + base, next2 + base);
	}

	@Override
	public Instruction rebaseCaptures(int base) {
		return this;
	}

}
