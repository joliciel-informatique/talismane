package com.joliciel.talismane.utils.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.utils.LogUtils;

/**
 * A writer which is linked to a CurrentFileProvider: when the current file
 * changes, the writer starts writing to a new file, while constructing the same
 * relative directory structure to contain the output files.
 * 
 * @author Assaf Urieli
 *
 */
public class DirectoryWriter extends Writer implements CurrentFileObserver {
  private static final Logger LOG = LoggerFactory.getLogger(DirectoryReader.class);
  private final File inDir;
  private final File outDir;
  private final String suffix;
  private final Charset charset;
  private Writer writer;

  /**
   * 
   * @param inDir
   *          the top directory from which we read input files
   * @param outDir
   *          the top directory to which we will write output files
   * @param suffix
   *          a suffix that will replace the file extension (or just get
   *          appended if there is no extension) in the output files
   * @param charset
   *          the character set to write in.
   */
  public DirectoryWriter(File inDir, File outDir, String suffix, Charset charset) {
    super();
    this.inDir = inDir;
    this.outDir = outDir;
    this.suffix = suffix;
    this.charset = charset;
  }

  @Override
  public void onNextFile(File file) {
    try {
      if (writer != null) {
        writer.flush();
        writer.close();
      }

      String fileName = file.getName();
      if (suffix != null) {
        if (fileName.indexOf('.') > 0) {
          fileName = fileName.substring(0, fileName.lastIndexOf('.'));
        }
        fileName += suffix;
      }
      if (LOG.isDebugEnabled())
        LOG.debug("Writing to " + fileName);

      // need to construct relative directory tree
      Stack<File> parents = new Stack<File>();
      File parent = file.getParentFile();
      while (parent != null && !parent.equals(inDir)) {
        parents.push(parent);
        parent = parent.getParentFile();
      }

      File outSubDir = outDir;
      while (!parents.isEmpty()) {
        parent = parents.pop();
        outSubDir = new File(outSubDir, parent.getName());
        outSubDir.mkdirs();
      }

      // path constructed, make the file
      File outFile = new File(outSubDir, fileName);
      outFile.delete();
      outFile.createNewFile();
      writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), charset));
    } catch (IOException e) {
      LogUtils.logError(LOG, e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void write(char[] cbuf, int off, int len) throws IOException {
    this.writer.write(cbuf, off, len);
  }

  @Override
  public void flush() throws IOException {
    if (this.writer != null)
      this.writer.flush();
  }

  @Override
  public void close() throws IOException {
    if (this.writer != null)
      this.writer.close();
  }

}
