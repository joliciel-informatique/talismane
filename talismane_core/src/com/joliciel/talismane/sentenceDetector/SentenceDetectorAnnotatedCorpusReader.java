///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
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

import java.util.Map;

/**
 * An interface for reading sentence splits from a training corpus.
 * @author Assaf Urieli
 *
 */
public interface SentenceDetectorAnnotatedCorpusReader {
	/**
	 * Is there another sentence to be read?
	 * @return
	 */
	public boolean hasNextSentence();
	
	/**
	 * Reads the next sentence from the corpus.
	 * @return
	 */
	public String nextSentence();
	
	/**
	 * Characteristics describing this corpus reader.
	 * @return
	 */
	public Map<String,String> getCharacteristics();
	
	/**
	 * Is the last sentence read the start of a new paragraph?
	 * @return
	 */
	public boolean isNewParagraph();

}
