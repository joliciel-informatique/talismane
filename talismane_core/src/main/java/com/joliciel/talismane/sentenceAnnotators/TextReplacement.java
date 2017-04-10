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
package com.joliciel.talismane.sentenceAnnotators;

import java.io.Serializable;

/**
 * A marker to be used inside an annotation, indicating that the annotated
 * section should be replaced by another text for analysis purposes.
 * 
 * @author Assaf Urieli
 *
 */
public class TextReplacement implements Serializable {
  private static final long serialVersionUID = 1L;
  private final String replacement;

  public TextReplacement(String replacement) {
    this.replacement = replacement;
  }

  public String getReplacement() {
    return replacement;
  }

  @Override
  public String toString() {
    return "TextReplacement [replacement=" + replacement + "]";
  }
}
