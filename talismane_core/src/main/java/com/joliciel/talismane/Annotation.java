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

package com.joliciel.talismane;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Annotation added to a continuous span of text. Examples could include
 * identifying a named entity, identifying token boundaries, or identifying
 * sentence boundaries.
 * 
 * Note: the Comparable methods only provide span ordering, and are inconsistent
 * with equals, which takes the data into account as well. Span ordering is by
 * start index, and for equal start indexes, from longest span to shortest.
 * 
 * @author Assaf Urieli
 *
 * @param <T>
 *          the data added by this annotation
 */
public final class Annotation<T extends Serializable> implements Comparable<Annotation<?>>, Serializable {
  private static final long serialVersionUID = 1L;
  private final int start;
  private final int end;
  private final T data;
  private final List<String> labels;
  private final Set<String> processingMarks;

  private Annotation(int start, int end, T data, List<String> labels, Set<String> processingMarks) {
    this.start = start;
    this.end = end;
    this.data = data;
    this.labels = labels;
    this.processingMarks = processingMarks;
  }

  public Annotation(int start, int end, T data, List<String> labels) {
    this(start, end, data, labels, new HashSet<>());
  }

  public Annotation(int start, int end, T data, String[] labels) {
    this(start, end, data, labels.length == 0 ? Collections.emptyList() : Arrays.asList(labels));
  }

  /**
   * The first character position to which the annotation applies.
   */
  public int getStart() {
    return start;
  }

  /**
   * The position immediately after the last character to which the annotation
   * applied.
   * 
   */
  public int getEnd() {
    return end;
  }

  /**
   * The annotation data.
   */
  public T getData() {
    return data;
  }

  /**
   * The labels added to this annotation.
   */
  public List<String> getLabels() {
    return labels;
  }

  /**
   * Return a new annotation representing the current annotation data with a
   * different start and end. All labels and processing marks are retained.
   */
  public Annotation<T> getAnnotation(int start, int end) {
    return new Annotation<T>(start, end, this.data, this.labels, this.processingMarks);
  }

  @Override
  public int compareTo(Annotation<?> that) {
    if (this.start != that.start)
      return this.start - that.start;
    return that.end - this.end;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((data == null) ? 0 : data.hashCode());
    result = prime * result + end;
    result = prime * result + start;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    @SuppressWarnings("rawtypes")
    Annotation other = (Annotation) obj;
    if (data == null) {
      if (other.data != null)
        return false;
    } else if (!data.equals(other.data))
      return false;
    if (end != other.end)
      return false;
    if (start != other.start)
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "Annotation [start=" + start + ", end=" + end + ", data=" + data + "]";
  }

  /**
   * Add a processing mark permanently to this annotation, typically indicating
   * that it has already been processed in a certain manner, and doesn't need to
   * be processed again.
   */
  public void addProcessingMark(String mark) {
    this.processingMarks.add(mark);
  }

  /**
   * Has the requested processing mark been added already?
   */
  public boolean hasProcessingMark(String mark) {
    return this.processingMarks.contains(mark);
  }
}
