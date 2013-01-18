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
package com.joliciel.talismane.parser;

/**
 * Compare two annotated corpora, one serving as reference and the other as
 * an evaluation.
 * Note: it is assumed that the corpora have an exact matching set of parse configurations!
 * @author Assaf Urieli
 *
 */
public interface ParseComparator {
	public void evaluate(ParserAnnotatedCorpusReader referenceCorpusReader,
			ParserAnnotatedCorpusReader evaluationCorpusReader);
	
	public void addObserver(ParseEvaluationObserver observer);

	/**
	 * The maximum number of sentences to evaluate. Default is 0, which means all.
	 * @param sentenceCount
	 */
	public abstract void setSentenceCount(int sentenceCount);
	public abstract int getSentenceCount();
}
