///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2017 Joliciel Informatique
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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.posTagger.UnknownPosTagException;

/**
 * Pos-tag mapper which assumes the entry's category is already a pos-tag.
 * 
 * @author Assaf Urieli
 *
 */
public class DefaultPosTagMapper implements PosTagMapper {
  private final PosTagSet posTagSet;

  private Map<String, Set<PosTag>> posTagsPerCategory = new HashMap<>();

  public DefaultPosTagMapper(PosTagSet posTagSet) {
    this.posTagSet = posTagSet;
  }

  @Override
  public PosTagSet getPosTagSet() {
    return this.posTagSet;
  }

  @Override
  public Set<PosTag> getPosTags(LexicalEntry lexicalEntry) {
    if (lexicalEntry.getCategory() == null)
      return Collections.emptySet();
    Set<PosTag> posTags = posTagsPerCategory.get(lexicalEntry.getCategory());

    if (posTags == null) {
      PosTag posTag = null;
      try {
        posTag = posTagSet.getPosTag(lexicalEntry.getCategory());
      } catch (UnknownPosTagException e) {
        // unknown posTag, do nothing
      }
      if (posTag == null)
        posTags = Collections.emptySet();
      else {
        posTags = new HashSet<>();
        posTags.add(posTag);
      }
      posTagsPerCategory.put(lexicalEntry.getCategory(), posTags);
    }
    return posTags;
  }

}
