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
