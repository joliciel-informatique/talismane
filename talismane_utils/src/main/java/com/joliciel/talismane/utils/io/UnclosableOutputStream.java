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
import java.io.OutputStream;

public class UnclosableOutputStream extends OutputStream {
  OutputStream wrappedStream;

  public UnclosableOutputStream(OutputStream wrappedStream) {
    super();
    this.wrappedStream = wrappedStream;
  }

  @Override
  public void write(int b) throws IOException {
    this.wrappedStream.write(b);
  }

  @Override
  public void write(byte[] b) throws IOException {
    this.wrappedStream.write(b);
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    this.wrappedStream.write(b, off, len);
  }

  @Override
  public void flush() throws IOException {
    this.wrappedStream.flush();
  }

  @Override
  public void close() throws IOException {
    // do nothing
  }
}
