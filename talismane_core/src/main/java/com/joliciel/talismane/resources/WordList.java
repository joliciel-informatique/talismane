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
package com.joliciel.talismane.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * An external word list read from a text file.<br/>
 * The first line must be "Type: WordList". <br/>
 * The default name will be the filename.<br/>
 * If a line starts with the string "Name: ", the default name will be replaced
 * by this name.<br/>
 * All lines starting with # are skipped.<br/>
 * All other lines contain words.
 * 
 * @author Assaf Urieli
 *
 */
public class WordList {
  private final String name;
  private final List<String> wordList;

  public WordList(String name, List<String> wordList) {
    this.name = name;
    this.wordList = wordList;
  }

  public WordList(String fileName, Scanner scanner) {
    String name = fileName;
    this.wordList = new ArrayList<String>();
    while (scanner.hasNextLine()) {
      String line = scanner.nextLine();
      if (line.length() > 0 && !line.startsWith("#")) {
        if (line.equals("Type: WordList"))
          continue;
        if (line.startsWith("Name: ")) {
          name = line.substring("Name: ".length());
          continue;
        }
        wordList.add(line);
      }
    }

    this.name = name;
  }

  /**
   * A unique name for this resource.
   */
  public String getName() {
    return name;
  }

  /**
   * The word list itself.
   */
  public List<String> getWordList() {
    return wordList;
  }

}
