package com.joliciel.talismane.utils.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class SingleFileReader extends BufferedReader implements CurrentFileProvider {
  private final File file;
  private boolean fileReported = false;
  private final List<CurrentFileObserver> observers = new ArrayList<CurrentFileObserver>();
  
  public SingleFileReader(File file, Reader in) {
    super(in);
    this.file = file;
  }
  private void reportFile() {
    if (!fileReported) {
      for (CurrentFileObserver observer: observers) {
        observer.onNextFile(file);
      }
      fileReported = true;
    }
  }

  @Override
  public int read() throws IOException {
    this.reportFile();
    return super.read();
  }

  @Override
  public String readLine() throws IOException {
    this.reportFile();
    return super.readLine();
  }

  @Override
  public int read(char[] cbuf, int off, int len) throws IOException {
    this.reportFile();
    return super.read(cbuf, off, len);
  }

  @Override
  public void addCurrentFileObserver(CurrentFileObserver observer) {
    this.observers.add(observer);
  }
}
