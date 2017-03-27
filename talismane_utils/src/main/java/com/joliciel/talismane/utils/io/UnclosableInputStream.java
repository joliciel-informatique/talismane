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
import java.io.InputStream;

public class UnclosableInputStream extends InputStream {
  InputStream wrappedStream;

  public UnclosableInputStream(InputStream wrappedStream) {
    super();
    this.wrappedStream = wrappedStream;
  }

  @Override
  public int read() throws IOException {
    return wrappedStream.read();
  }

  @Override
  public int read(byte[] b) throws IOException {
    return wrappedStream.read(b);
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    return wrappedStream.read(b, off, len);
  }

  @Override
  public long skip(long n) throws IOException {
    return wrappedStream.skip(n);
  }

  @Override
  public int available() throws IOException {
    return wrappedStream.available();
  }

  @Override
  public void close() throws IOException {
    // do nothing
  }

  @Override
  public synchronized void mark(int readlimit) {
    wrappedStream.mark(readlimit);
  }

  @Override
  public synchronized void reset() throws IOException {
    wrappedStream.reset();
  }

  @Override
  public boolean markSupported() {
    return wrappedStream.markSupported();
  }

}
