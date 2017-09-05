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
package com.joliciel.talismane.extensions.corpus;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipInputStream;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 * A class for storing pos-tagger statistics from a given corpus.
 * 
 * @author Assaf Urieli
 *
 */
public class PosTaggerStatistics implements Serializable {
  private static final long serialVersionUID = 1L;

  public Set<String> words = new TreeSet<String>();
  public Set<String> lowerCaseWords = new TreeSet<String>();
  public Map<String, Integer> posTagCounts = new TreeMap<String, Integer>();
  public int tokenCount;
  public int unknownTokenCount;
  public int alphanumericCount;
  public int unknownAlphanumericCount;
  public int unknownInLexiconCount;
  public int unknownAlphaInLexiconCount;
  public int sentenceCount;
  public int openClassCount;
  public int openClassUnknownInRefCorpus;
  public int openClassUnknownInLexicon;
  public int closedClassCount;
  public int closedClassUnknownInRefCorpus;
  public int closedClassUnknownInLexicon;
  public DescriptiveStatistics sentenceLengthStats = new DescriptiveStatistics();

  public static PosTaggerStatistics loadFromFile(File inFile) throws IOException, ClassNotFoundException {
    ZipInputStream zis = new ZipInputStream(new FileInputStream(inFile));
    zis.getNextEntry();
    @SuppressWarnings("resource")
    ObjectInputStream in = new ObjectInputStream(zis);
    PosTaggerStatistics stats = (PosTaggerStatistics) in.readObject();
    return stats;
  }
}
