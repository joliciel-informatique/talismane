///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2016 Joliciel Informatique
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
package com.joliciel.talismane.tokeniser;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * A TokenAttribute containing a String.
 * 
 * @author Assaf Urieli
 *
 */
public class StringAttribute extends TokenAttribute<String> {
	private static final long serialVersionUID = 1L;

	/*
	 * For deserialization only.
	 */
	public StringAttribute() {
	}

	public StringAttribute(String key, String value) {
		super(key, value);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		// just write the key and value to the stream
		out.writeUTF(key);
		out.writeUTF(value);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		// read the key and value from the stream
		this.key = in.readUTF();
		this.value = in.readUTF();

	}
}
