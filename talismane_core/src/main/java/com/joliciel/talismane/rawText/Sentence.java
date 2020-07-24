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
package com.joliciel.talismane.rawText;

import java.io.File;
import java.io.Serializable;
import java.net.URI;
import java.util.*;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.AnnotatedText;
import com.joliciel.talismane.TalismaneSession;

/**
 * A sentence detected by the sentence detector, including information about the
 * original location of the sentence in the stream being processed, and about
 * any text in the original stream that has been marked for raw inclusion in
 * Talismane's output.
 * 
 * @author Assaf Urieli
 *
 */
public class Sentence extends AnnotatedText implements Serializable {
  private static final long serialVersionUID = 1L;
  
  public static final String WHITE_SPACE = "\\s\ufeff";

  private static final Logger LOG = LoggerFactory.getLogger(Sentence.class);

  private final TreeMap<Integer, String> originalTextSegments;
  private final List<Integer> originalIndexes;
  private final boolean complete;
  private final TreeMap<Integer, Integer> newlines;
  private final URI fileURI;
  private final String filePath;
  private transient File file;
  private int startLineNumber = -1;
  private String leftoverOriginalText = "";

  protected final String sessionId;

  public Sentence(CharSequence text, TreeMap<Integer, String> originalTextSegments, List<Integer> originalIndexes, boolean complete,
      TreeMap<Integer, Integer> newlines, File file, String sessionId) {
    super(text);
    this.originalTextSegments = originalTextSegments;
    this.originalIndexes = originalIndexes;
    this.complete = complete;
    this.newlines = newlines;
    this.file = file;
    this.fileURI = file==null ? null: file.toURI();
    this.filePath = file==null ? "": file.getPath();
    this.sessionId = sessionId;

    if (LOG.isTraceEnabled()) {
      LOG.trace("Constructed sentence: |" + text + "|");
      LOG.trace("Original text segments: " + originalTextSegments);
    }
  }

  public Sentence(CharSequence text, File file, String sessionId) {
    this(text, new TreeMap<>(), new ArrayList<>(), true, new TreeMap<>(), file, sessionId);
  }

  public Sentence(CharSequence text, String sessionId) {
    this(text, null, sessionId);
  }

  /**
   * Get the original text index of any character index within this sentence.
   */

  public int getOriginalIndex(int index) {
    if (originalIndexes.size() == 0)
      return index;
    if (index == originalIndexes.size())
      return originalIndexes.get(index - 1) + 1;
    if (index > originalIndexes.size())
      return -1;
    return originalIndexes.get(index);
  }

  /**
   * Get the sentence text index corresponding to the first position following
   * the original index provided.
   */

  public int getIndex(int originalIndex) {
    if (originalIndexes.size() == 0)
      return originalIndex;
    int index = -1;
    for (int i = 0; i < originalIndexes.size(); i++) {
      if (originalIndexes.get(index) >= originalIndex) {
        index = i;
        break;
      }
    }
    return index;
  }

  /**
   * A map giving any original text segments marked for output. The integer
   * gives the index before which the segment needs to be inserted in the
   * processed sentence text. The string gives the actual segment to be
   * inserted.
   */

  public Map<Integer, String> getOriginalTextSegments() {
    return this.originalTextSegments;
  }

  public List<Integer> getOriginalIndexes() {
    return originalIndexes;
  }

  /**
   * Is this a complete sentence? Default is true.
   */

  public boolean isComplete() {
    return complete;
  }

  /**
   * Returns the line number corresponding to a particular original index inside
   * this sentence, starting at 1.
   */

  public int getLineNumber(int originalIndex) {
    Entry<Integer, Integer> lastLineEntry = this.newlines.floorEntry(originalIndex);
    if (lastLineEntry != null)
      return lastLineEntry.getValue();
    return -1;
  }

  /**
   * Returns the column number corresponding to a particular original index
   * inside this sentence, starting at 0.
   */

  public int getColumnNumber(int originalIndex) {
    Integer lastLineObj = this.newlines.floorKey(originalIndex);
    if (lastLineObj != null)
      return (originalIndex - lastLineObj.intValue());
    return -1;
  }

  /**
   * A map giving original index to line number mappings, for all lines
   * contained within this sentence.
   */

  public Map<Integer, Integer> getNewlines() {
    return newlines;
  }

  /**
   * The file name containing this sentence.
   */

  public String getFileName() {
    return filePath;
  }

  /**
   * Get the closest original text segment preceding a certain index.
   */

  public Entry<Integer, String> getPrecedingOriginalTextSegment(int index) {
    return this.originalTextSegments.floorEntry(index);
  }

  /**
   * Get all raw input strictly after a given startIndex, and before or at a
   * given end index, concatenated together.
   */

  public String getRawInput(int startIndex, int endIndex) {
    SortedMap<Integer, String> containedSegments = originalTextSegments.subMap(startIndex + 1, endIndex + 1);
    String rawInput = null;
    if (containedSegments.size() > 0) {
      StringBuilder sb = new StringBuilder();
      boolean firstSegment = true;
      for (String segment : containedSegments.values()) {
        if (!firstSegment)
          sb.append(TalismaneSession.get(sessionId).getOutputDivider());
        sb.append(segment);
        firstSegment = false;
      }
      rawInput = sb.toString();
    }
    return rawInput;
  }

  @Override
  public String toString() {
    return "SentenceImpl [text=" + this.getText() + "]";
  }

  /**
   * The line number on which this sentence started, when reading from a
   * previously analysed corpus (one token per line).
   */
  public int getStartLineNumber() {
    return startLineNumber;
  }

  public void setStartLineNumber(int startLineNumber) {
    this.startLineNumber = startLineNumber;
  }

  /**
   * The file containing this sentence, or null if none was indicated.
   */

  public File getFile() {
    if (fileURI==null)
      return null;
    if (file==null)
      file = new File(this.fileURI);
    return file;
  }

  /** The URI of the file containing this sentence, or null if none was indicated. */
  public URI getFileURI() {
    return fileURI;
  }

  /**
   * Is there any leftover original text?
   */

  public String getLeftoverOriginalText() {
    return leftoverOriginalText;
  }

  /**
   * Set original text marked for output from a previous sentence holder.
   */

  public void setLeftoverOriginalText(String leftoverOriginalText) {
    if (LOG.isTraceEnabled())
      LOG.trace("setLeftoverOriginalText: " + leftoverOriginalText);
    this.leftoverOriginalText = leftoverOriginalText;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Sentence sentence = (Sentence) o;
    return complete == sentence.complete &&
      startLineNumber == sentence.startLineNumber &&
      originalTextSegments.equals(sentence.originalTextSegments) &&
      originalIndexes.equals(sentence.originalIndexes) &&
      newlines.equals(sentence.newlines) &&
      Objects.equals(fileURI, sentence.fileURI) &&
      filePath.equals(sentence.filePath) &&
      Objects.equals(leftoverOriginalText, sentence.leftoverOriginalText) &&
      sessionId.equals(sentence.sessionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(originalTextSegments, originalIndexes, complete, newlines, fileURI, filePath, startLineNumber, leftoverOriginalText, sessionId);
  }
}
