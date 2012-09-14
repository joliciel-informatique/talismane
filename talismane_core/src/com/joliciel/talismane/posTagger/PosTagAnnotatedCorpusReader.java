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
package com.joliciel.talismane.posTagger;

import com.joliciel.talismane.tokeniser.filters.TokenFilter;

/**
 * An interface for reading tokenized and tagged sentences from a corpus.
 * @author Assaf Urieli
 *
 */
public interface PosTagAnnotatedCorpusReader {
	/**
	 * Is there another sentence to be read?
	 * @return
	 */
	public boolean hasNextSentence();
	
	/**
	 * Read the list of tagged tokens from next sentence from the training corpus.
	 * @return
	 */
	public PosTagSequence nextSentence();
	
	public void addTokenFilter(TokenFilter tokenFilter);
}
