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
package com.joliciel.talismane.tokeniser;

import java.util.Map;

import com.joliciel.talismane.tokeniser.filters.TokenFilter;

/**
 * An interface for reading tokenized sentences from a corpus.
 * @author Assaf Urieli
 *
 */
public interface TokeniserAnnotatedCorpusReader {
	/**
	 * Is there another sentence to be read?
	 * @return
	 */
	public boolean hasNextTokenSequence();
	
	/**
	 * Read the next sentence from the training corpus, along with its token splits.
	 * Any whitespace (including several whitespaces in a row) should be reduced to a single space
	 * in the sentence that's returned.
	 * The token splits returned indicate the position of the first symbol following any token split,
	 * with splits occurring on both sides of any white space if it is not part of a compound word.
	 * @return
	 */
//	public String nextSentence(List<Integer> tokenSplits);
	
	/***
	 * Reads the next token sequence from the corpus.
	 * @return
	 */
	public TokenSequence nextTokenSequence();
	
	/**
	 * Characteristics describing this corpus reader.
	 * @return
	 */
	public Map<String,String> getCharacteristics();
	
	public void addTokenFilter(TokenFilter tokenFilter);


}
