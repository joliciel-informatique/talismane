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
package com.joliciel.talismane.posTagger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A tag set to be used for pos tagging. The default format for reading a
 * PosTagSet from a file is as follows:
 * <br>
 * All lines starting with # are ignored. The first line read is the PosTagSet
 * name. The second line read is the PosTagSet locale.
 * <br>
 * All further lines are postags, in a tab delimited format shown below:
 * <br>
 * 
 * <pre>
 * PosTag description PosTagOpenClassIndicator
 * </pre>
 * <br>
 * For example:
 * <br>
 * 
 * <pre>
 * # Example of a PosTagSet file
 * Talismane 2013
 * fr
 * ADJ  adjectif  OPEN
 * ADV  adverbe OPEN
 * ADVWH  adverbe intérrogatif CLOSED
 * CC conjonction de coordination CLOSED
 * PONCT  ponctuation PUNCTUATION
 * </pre>
 * 
 * @see PosTag
 * @see PosTagOpenClassIndicator
 * @author Assaf Urieli
 *
 */
public class PosTagSet implements Serializable {
  private static final long serialVersionUID = 1L;
  private static final Logger LOG = LoggerFactory.getLogger(PosTagSet.class);

  private String name;
  private Locale locale;
  private Set<PosTag> tags = new TreeSet<>();
  private Map<String, PosTag> tagMap = null;

  /**
   * Loads a PosTagSet from a file or list of strings. The file has the
   * following format: <br>
   * First row: PosTagSet name<br>
   * Second row: PosTagSet ISO 2 letter language code<br>
   * Remaining rows:<br>
   * PosTagCode tab description tab OPEN/CLOSED<br>
   * e.g.<br>
   * ADJ adjectif OPEN<br>
   */
  public PosTagSet(File file) throws IOException {
    try (Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8")))) {
      this.load(scanner);
    }
  }

  /**
   * Same as getPosTageSet(file), but replaces file with a scanner.
   */
  public PosTagSet(Scanner scanner) {
    this.load(scanner);
  }

  /**
   * Same as getPosTagSet(File), but replaces the file with a List of Strings.
   */
  public PosTagSet(List<String> descriptors) {
    this.load(descriptors);
  }

  void load(Scanner scanner) {
    List<String> descriptors = new ArrayList<String>();
    while (scanner.hasNextLine()) {
      String line = scanner.nextLine();
      descriptors.add(line);
    }
    this.load(descriptors);
  }

  void load(List<String> descriptors) {
    boolean nameFound = false;
    boolean localeFound = false;
    for (String descriptor : descriptors) {
      LOG.debug(descriptor);
      if (descriptor.startsWith("#")) {
        continue;
      }

      if (!nameFound) {
        this.name = descriptor;
        nameFound = true;
      } else if (!localeFound) {
        this.locale = new Locale(descriptor);
        localeFound = true;
      } else {
        String[] parts = descriptor.split("\t");
        tags.add(new PosTag(parts[0], parts[1], PosTagOpenClassIndicator.valueOf(parts[2])));
      }
    }
  }

  /**
   * Name of this posTagSet.
   */
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  /**
   * The locale to which this PosTagSet applies.
   */
  public Locale getLocale() {
    return locale;
  }

  public void setLocale(Locale locale) {
    this.locale = locale;
  }

  /**
   * Returns the full tagset.
   */
  public Set<PosTag> getTags() {
    return tags;
  }

  /**
   * Return the PosTag corresponding to a given code.
   * 
   * @throws UnknownPosTagException
   */
  public PosTag getPosTag(String code) throws UnknownPosTagException {
    if (tagMap == null) {
      tagMap = new HashMap<>();
      for (PosTag posTag : this.getTags()) {
        tagMap.put(posTag.getCode(), posTag);
      }
      tagMap.put(PosTag.ROOT_POS_TAG_CODE, PosTag.ROOT_POS_TAG);
    }
    PosTag posTag = tagMap.get(code);
    if (posTag == null) {
      throw new UnknownPosTagException("Unknown PosTag: " + code);
    }
    return posTag;
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    PosTagSet other = (PosTagSet) obj;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    return true;
  }
}
