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
package com.joliciel.talismane.regex.parser;

import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

import com.joliciel.talismane.regex.bytecode.CharRange;

/**
 * @author Lucas Satabin
 *
 */
public class CharSet extends Node {

	public final SortedSet<CharRange> charset;

	public CharSet(SortedSet<CharRange> charset) {
		super(NodeType.CHARSET);
		this.charset = charset;
	}

	public CharSet(CharRange... ranges) {
		super(NodeType.CHARSET);
		this.charset = new TreeSet<>(Arrays.asList(ranges));
	}

	@Override
	public String toString() {
		return "CharSet [charset=" + charset + "]";
	}

}
