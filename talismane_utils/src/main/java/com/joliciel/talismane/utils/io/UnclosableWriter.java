///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2015 Joliciel Informatique
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
package com.joliciel.talismane.utils.io;

import java.io.IOException;
import java.io.Writer;

public class UnclosableWriter extends Writer {
	private Writer wrappedWriter;
	
	public UnclosableWriter(Writer wrappedWriter) {
		super();
		this.wrappedWriter = wrappedWriter;
	}

	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		wrappedWriter.write(cbuf, off, len);
	}

	@Override
	public void flush() throws IOException {
		wrappedWriter.flush();
	}

	@Override
	public void close() throws IOException {
		// ignore - never close the wrapped writer
	}

}
