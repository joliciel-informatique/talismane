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
package com.joliciel.talismane.machineLearning;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class TextFileWordList implements ExternalWordList {
	private static final long serialVersionUID = 1L;
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(TextFileWordList.class);
	List<String> wordList = new ArrayList<String>();

	private String name;

	public TextFileWordList(String fileName, Scanner scanner) {
		this.name = fileName;

		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			if (line.length() > 0 && !line.startsWith("#")) {
				if (line.equals("Type: WordList"))
					continue;
				if (line.startsWith("Name: ")) {
					this.name = line.substring("Name: ".length());
					continue;
				}
				wordList.add(line);
			}
		}
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public List<String> getWordList() {
		return wordList;
	}

}
