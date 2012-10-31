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

import java.util.List;
import java.util.Map;

import com.joliciel.talismane.tokeniser.filters.TokenFilter;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;

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
	
	/**
	 * These filters will be applied to each token sequence returned by the corpus prior to being returned.
	 * @param tokenFilter
	 */
	public void addTokenSequenceFilter(TokenSequenceFilter tokenSequenceFilter);

	/**
	 * These filters will not be used to detect tokens, as token boundaries are provided by the corpus.
	 * They will, on the other hand, be used to replace token text.
	 * @param tokenFilter
	 */
	public void addTokenFilter(TokenFilter tokenFilter);
	
	/**
	 * @see #addTokenSequenceFilter(TokenSequenceFilter)
	 * @return
	 */
	public List<TokenSequenceFilter> getTokenSequenceFilters();
	
	/**
	 * #see {@link #addTokenFilter(TokenFilter)}
	 * @return
	 */
	public List<TokenFilter> getTokenFilters();
}
