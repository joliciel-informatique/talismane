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
package com.joliciel.talismane.parser;

import java.util.List;

import com.joliciel.talismane.posTagger.PosTagSequence;

/**
 * A non-deterministic parser, which analyses multiple pos-tagging possibilities
 * for this sentence, and returns multiple parse configurations.
 * 
 * @author Assaf Urieli
 *
 */
public interface NonDeterministicParser extends Parser {
	/**
	 * Analyse a list of pos-tag sequences, each of which represents one
	 * possibility of tagging a given sentence, and return the n most likely
	 * parse configurations for the sentence.
	 * 
	 * @param posTagSequences
	 *            the n most likely pos-tag sequences for this sentence.
	 * @return the n most likely parse sequences for this sentence
	 */
	public List<ParseConfiguration> parseSentence(List<PosTagSequence> posTagSequences);

	/**
	 * The maximum size of the beam to be used during analysis.
	 */
	public int getBeamWidth();

	/**
	 * Should the full pos-tagger beam be propagated as input into the parser.
	 */
	public boolean isPropagatePosTaggerBeam();
}
