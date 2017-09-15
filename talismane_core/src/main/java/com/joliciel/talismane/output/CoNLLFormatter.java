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
package com.joliciel.talismane.output;

public class CoNLLFormatter {
  private final boolean spacesToUnderscores;

  public CoNLLFormatter(boolean spacesToUnderscores) {
    this.spacesToUnderscores = spacesToUnderscores;
  }

  /**
   * Convert a string to CoNLL format.
   */
  public String toCoNLL(String text) {
    if (text == null)
      text = "";
    if (spacesToUnderscores) {
      String conllText = text.replace("_", "&und;");
      conllText = conllText.replace(' ', '_');
      if (conllText.length() == 0)
        conllText = "_";
      return conllText;
    } else {
      String conllText = text;
      if (conllText.equals("_"))
        conllText = "&und;";
      else if (conllText.length() == 0)
        conllText = "_";
      return conllText;
    }
  }

  /**
   * Convert a string from CoNLL format.
   */
  public String fromCoNLL(String conllText) {
    if (spacesToUnderscores) {
      String text = null;
      if (conllText.equals("_")) {
        text = "";
      } else {
        text = conllText.replace('_', ' ');
        text = text.replace("&und;", "_");
      }
      return text;
    } else {
      String text = conllText;
      if (conllText.equals("_"))
        text = "";
      else if (conllText.equals("&und;"))
        text = "_";
      return text;
    }
  }
}
