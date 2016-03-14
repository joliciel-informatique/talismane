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
package com.joliciel.talismane.posTagger;

import com.joliciel.talismane.tokeniser.Tokeniser;

/**
 * An interface for evaluating a given pos tagger.
 * @author Assaf Urieli
 *
 */
public interface PosTaggerEvaluator {
	/**
	 * Evaluate a given pos tagger.
	 * @param reader for reading manually tagged tokens from a corpus
	 */
	public void evaluate(PosTagAnnotatedCorpusReader reader);

	/**
	 * A tokeniser to tokenise the sentences brought back by the corpus reader,
	 * rather than automatically using their existing tokenisation.
	 * @return
	 */
	public abstract Tokeniser getTokeniser();
	public abstract void setTokeniser(Tokeniser tokeniser);

	/**
	 * Should the pos tagger take the tokeniser's full beam as it's input,
	 * or only the best guess.
	 * @return
	 */
	public abstract boolean isPropagateBeam();
	public abstract void setPropagateBeam(boolean propagateBeam);

	public abstract void addObserver(PosTagEvaluationObserver observer);

	/**
	 * If set, will limit the maximum number of sentences that will be evaluated.
	 * Default is 0 = all sentences.
	 * @param sentenceCount
	 */
	public abstract void setSentenceCount(int sentenceCount);
	public abstract int getSentenceCount();

	public PosTagger getPosTagger();
}
