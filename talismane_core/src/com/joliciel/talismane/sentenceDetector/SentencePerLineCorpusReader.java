///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2013 Assaf Urieli
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
package com.joliciel.talismane.sentenceDetector;

import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * A default corpus reader which assumes one sentence per line.
 * @author Assaf Urieli
 *
 */
class SentencePerLineCorpusReader implements SentenceDetectorAnnotatedCorpusReader {
	private Scanner scanner;
	
	public SentencePerLineCorpusReader(Reader reader) {
		scanner = new Scanner(reader);
	}
	
	@Override
	public boolean hasNextSentence() {
		return scanner.hasNextLine();
	}

	@Override
	public String nextSentence() {
		return scanner.nextLine();
	}

	@Override
	public Map<String, String> getCharacteristics() {
		Map<String, String> characteristics = new LinkedHashMap<String, String>();
		return characteristics;
	}

	@Override
	public boolean isNewParagraph() {
		return false;
	}

}
