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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.utils.JolicielException;

/**
 * Finds the external wordlists corresponding to a given name.
 * 
 * @author Assaf Urieli
 *
 */
public class WordListFinder {
  private static final Logger LOG = LoggerFactory.getLogger(WordListFinder.class);
  private Map<String, WordList> wordListMap = new HashMap<>();

  /**
   * Add an external word list located in a scanner from a particular
   * filename.
   * 
   * @throws TalismaneException
   *             if unknown file type
   */
  public void addWordList(String fileName, Scanner scanner) throws TalismaneException {
    LOG.debug("Reading " + fileName);
    String typeLine = scanner.nextLine();

    if (!typeLine.startsWith("Type: "))
      throw new JolicielException("In file " + fileName + ", expected line starting with \"Type: \"");

    String type = typeLine.substring("Type: ".length());

    if ("WordList".equals(type)) {
      WordList textFileWordList = new WordList(fileName, scanner);
      this.addWordList(textFileWordList);
    } else {
      throw new TalismaneException("Unexpected type in file: " + fileName + ": " + type);
    }
  }

  public void addWordList(WordList wordList) {
    LOG.debug("Adding word list with name: " + wordList.getName());
    this.wordListMap.put(wordList.getName(), wordList);
  }

  public WordList getWordList(String name) {
    return this.wordListMap.get(name);
  }

  public Collection<WordList> getWordLists() {
    return wordListMap.values();
  }
}
