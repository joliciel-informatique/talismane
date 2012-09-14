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

import java.io.Writer;
import java.util.Set;

import com.joliciel.talismane.tokeniser.Tokeniser;
import com.joliciel.talismane.utils.stats.FScoreCalculator;

/**
 * An interface for evaluating a given pos tagger.
 * @author Assaf Urieli
 *
 */
public interface PosTaggerEvaluator {

	/**
	 * Evaluate a given pos tagger.
	 * @param reader for reading manually tagged tokens from a corpus
	 * @return an f-score calculator for this postagger
	 */
	public FScoreCalculator<String> evaluate(PosTagAnnotatedCorpusReader reader);

	/**
	 * A list of unknown words, for evaluating f-scores for unknown words in the corpus.
	 * @return
	 */
	public abstract Set<String> getUnknownWords();
	public abstract void setUnknownWords(Set<String> unknownWords);

	/**
	 * An f-score calculator for unknown words in the lexicon.
	 * @return
	 */
	public abstract FScoreCalculator<String> getFscoreUnknownInLexicon();

	/**
	 * An f-score calculator for unknown words in the corpus.
	 * @return
	 */
	public abstract FScoreCalculator<String> getFscoreUnknownInCorpus();

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
	
	/**
	 * If provided, will write the evaluation of each sentence
	 * to a csv file.
	 * @param csvFileWriter
	 */
	public void setCsvFileWriter(Writer csvFileWriter);
}
