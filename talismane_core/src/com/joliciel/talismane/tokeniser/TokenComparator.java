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
package com.joliciel.talismane.tokeniser;

/**
 * An interface for comparing two tokenised corpora, one of which is considered a reference.
 * @author Assaf Urieli
 *
 */
public interface TokenComparator {
	/**
	 * Evaluate the evaluation corpus against the reference corpus.
	 * @param reader for reading manually tagged tokens from a corpus
	 */
	public void compare();

	public abstract void addObserver(TokenEvaluationObserver observer);

	/**
	 * If set, will limit the maximum number of sentences that will be evaluated.
	 * Default is 0 = all sentences.
	 * @param sentenceCount
	 */
	public abstract void setSentenceCount(int sentenceCount);
	public abstract int getSentenceCount();
}
