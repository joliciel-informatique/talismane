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
package com.joliciel.talismane.lexicon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.posTagger.UnknownPosTagException;

/**
 * <p>
 * Pos-tag mapper that maps lexical attributes to postags using a tab-delimited
 * file as input. File format is as follows:
 * </p>
 * <p>
 * Rows starting with # are comments
 * </p>
 * <p>
 * The first non-comment row needs to contain a list of tab delimited
 * attributes, e.g.
 * </p>
 * 
 * <pre>
 * Category Morphology
 * </pre>
 * <p>
 * The following rows each contain one tab per attribute, and a final tab for
 * the pos-tag, e.g.
 * </p>
 * 
 * <pre>
 * adj  Kms ADJ
 * </pre>
 * 
 * @author Assaf Urieli
 *
 */
public class SimplePosTagMapper implements PosTagMapper {
  private PosTagSet posTagSet;
  private Map<String, Set<PosTag>> posTagMap = new HashMap<String, Set<PosTag>>();
  private List<String> attributes = new ArrayList<String>();

  public SimplePosTagMapper(Scanner scanner, PosTagSet posTagSet) throws UnknownPosTagException {
    super();
    this.posTagSet = posTagSet;

    boolean firstLine = true;

    while (scanner.hasNextLine()) {
      String line = scanner.nextLine();
      if (line.startsWith("#"))
        continue;
      String[] parts = line.split("\t");
      if (firstLine) {
        for (String part : parts)
          attributes.add(part);
        firstLine = false;
      } else {
        String key = "";
        for (int i = 0; i < attributes.size(); i++)
          key += parts[i] + "|";

        String value = parts[attributes.size()];
        PosTag posTag = posTagSet.getPosTag(value);
        Set<PosTag> posTags = posTagMap.get(key);
        if (posTags == null) {
          posTags = new HashSet<PosTag>();
          posTagMap.put(key, posTags);
        }
        posTags.add(posTag);
      }
    }
  }

  @Override
  public PosTagSet getPosTagSet() {
    return this.posTagSet;
  }

  @Override
  public Set<PosTag> getPosTags(LexicalEntry lexicalEntry) {
    StringBuilder sb = new StringBuilder();
    for (String attribute : attributes)
      sb.append(lexicalEntry.getAttribute(attribute) + "|");
    String key = sb.toString();
    Set<PosTag> posTags = posTagMap.get(key);
    if (posTags == null)
      posTags = new HashSet<PosTag>();
    return posTags;
  }

}
