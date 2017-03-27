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
package com.joliciel.talismane.tokeniser;

import java.util.ArrayList;

import com.joliciel.talismane.machineLearning.Decision;

/**
 * A sequence of tagged tokens with a score.
 * 
 * @author Assaf Urieli
 *
 */
public class TaggedTokenSequence<T extends TokenTag> extends ArrayList<TaggedToken<T>> {
  private static final long serialVersionUID = 1L;
  private String string = null;

  public TaggedTokenSequence() {
  }

  public TaggedTokenSequence(int initialCapacity) {
    super(initialCapacity);
  }

  /**
   * Create a tagged token sequence with space to one additional tagged token at
   * the end of an existing history.
   */
  public TaggedTokenSequence(TaggedTokenSequence<T> history) {
    super(history.size() + 1);
    this.addAll(history);
  }

  /**
   * Add a tagged token to the end of the current tagged token list.
   * 
   * @param token
   *          the token to be added
   * @param decision
   *          the decision attached to this token
   */
  public TaggedToken<T> addTaggedToken(Token token, Decision decision, T tag) {
    TaggedToken<T> taggedToken = new TaggedToken<T>(token, decision, tag);
    this.add(taggedToken);
    return taggedToken;
  }

  @Override
  public synchronized String toString() {
    if (string == null) {
      StringBuilder builder = new StringBuilder();
      builder.append("Sequence: ");
      for (TaggedToken<T> taggedToken : this) {
        builder.append(taggedToken.toString());
      }
      string = builder.toString();
    }
    return string;
  }

}
