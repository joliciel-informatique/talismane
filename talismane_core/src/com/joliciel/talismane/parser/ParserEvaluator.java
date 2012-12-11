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

import com.joliciel.talismane.posTagger.PosTagger;
import com.joliciel.talismane.tokeniser.Tokeniser;

/**
 * Evaluate a parser.
 * @author Assaf Urieli
 *
 */
public interface ParserEvaluator {
	public void evaluate(ParserAnnotatedCorpusReader corpusReader);
	
	public Parser getParser();
	public void setParser(Parser parser);
	
	public void addObserver(ParseEvaluationObserver observer);

	/**
	 * If provided, will apply tokenisation as part of the evaluation.
	 * If provided, a pos-tagger must be provided as well.
	 * @param tokeniser
	 */
	public abstract void setTokeniser(Tokeniser tokeniser);
	public abstract Tokeniser getTokeniser();

	/**
	 * If provided, will apply pos-tagging as part of the evaluation.
	 * @param posTagger
	 */
	public abstract void setPosTagger(PosTagger posTagger);
	public abstract PosTagger getPosTagger();

	/**
	 * Should the beam be propagated from one module to the next,
	 * e.g. from the pos-tagger to the parser.
	 * @param propagateBeam
	 */
	public abstract void setPropagateBeam(boolean propagateBeam);
	public abstract boolean isPropagateBeam();

	/**
	 * The maximum number of sentences to evaluate. Default is 0, which means all.
	 * @param sentenceCount
	 */
	public abstract void setSentenceCount(int sentenceCount);
	public abstract int getSentenceCount();
}
