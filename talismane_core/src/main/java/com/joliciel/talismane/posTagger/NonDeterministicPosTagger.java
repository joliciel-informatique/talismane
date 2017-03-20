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

import java.util.List;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.tokeniser.TokenSequence;

/**
 * A non-deterministic pos tagger, which analyses multiple tokenising
 * possibilities for this sentence, and returns multiple postagging
 * possibilities.
 * 
 * @author Assaf Urieli
 *
 */
public interface NonDeterministicPosTagger extends PosTagger {
	/**
	 * Analyse a list of token sequences, each of which represents one
	 * possibility of tokenising a given sentence, and return the n most likely
	 * pos tag sequences for the sentence. The number of token sequences
	 * provided as input can be different from the number of pos-tagging
	 * possibilities returned as output.
	 * 
	 * @param tokenSequences
	 *            the n most likely token sequences for this sentence.
	 * @return the n most likely postag sequences for this sentence
	 * @throws UnknownPosTagException
	 *             if an unknown pos-tag is guessed
	 * @throws TalismaneException
	 */
	public abstract List<PosTagSequence> tagSentence(List<TokenSequence> tokenSequences) throws UnknownPosTagException, TalismaneException;

	/**
	 * The maximum number of possible sequences returned by the pos-tagger.
	 */
	public abstract int getBeamWidth();

	/**
	 * Should the full tokeniser beam be propagated as input into the
	 * pos-tagger.
	 */
	public boolean isPropagateTokeniserBeam();
}
