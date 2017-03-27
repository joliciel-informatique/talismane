///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2014 Joliciel Informatique
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
package com.joliciel.talismane.utils.compiler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import javax.tools.SimpleJavaFileObject;

/**
 * A JavaFileObject that contains either the source code or the compiled class,
 * but not both, depending on how it was instantiated.
 */
final class InMemoryJavaFileObject extends SimpleJavaFileObject {
  private final CharSequence sourceCode;
  private ByteArrayOutputStream compiledClass;

  InMemoryJavaFileObject(final String baseName, final CharSequence source) {
    super(getURI(baseName + ".java"), Kind.SOURCE);
    this.sourceCode = source;
  }

  InMemoryJavaFileObject(final String name) {
    super(getURI(name), Kind.CLASS);
    sourceCode = null;
  }

  /**
   * Return the source code.
   */
  @Override
  public CharSequence getCharContent(final boolean ignoreEncodingErrors) throws UnsupportedOperationException {
    if (sourceCode == null)
      throw new UnsupportedOperationException("getCharContent()");
    return sourceCode;
  }

  /**
   * Return the compiled class for reading.
   */
  @Override
  public InputStream openInputStream() {
    byte[] byteArray = compiledClass.toByteArray();
    return new ByteArrayInputStream(byteArray);
  }

  /**
   * Return the compiled class for writing.
   */
  @Override
  public OutputStream openOutputStream() {
    compiledClass = new ByteArrayOutputStream();
    return compiledClass;
  }

  public byte[] getBytes() {
    return compiledClass.toByteArray();
  }

  static URI getURI(String name) {
    try {
      return new URI(name);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
