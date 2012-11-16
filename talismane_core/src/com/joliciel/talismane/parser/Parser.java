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

import com.joliciel.talismane.machineLearning.AnalysisObserver;
import com.joliciel.talismane.posTagger.PosTagSequence;

/**
 * A syntax parser which takes a pos tag sequence and provides a parse configuration.
 * @author Assaf Urieli
 *
 */
public interface Parser {
	/**
	 * Analyse a pos-tag sequence,
	 * and return the most likely parse configuration for the sentence.
	 * @param posTagSequences the n most likely pos-tag sequences for this sentence.
	 * @return the n most likely parse sequences for this sentence
	 */
	public abstract ParseConfiguration parseSentence(PosTagSequence posTagSequence);
	

	/**
	 * The maximum size of the beam to be used during analysis.
	 * @return
	 */
	public abstract int getBeamWidth();
	
	public void addObserver(AnalysisObserver observer);
	
	/**
	 * The transition system used by this parser to make parse decisions.
	 * @return
	 */
	public TransitionSystem getTransitionSystem();
	
	/**
	 * The maximum time alloted per sentence for parse tree analysis, in seconds.
	 * Will be ignored if set to 0.
	 * If analysis jumps out because of time-out, there will be a parse-forest instead of a parse-tree,
	 * with several nodes left unattached.
	 * @return
	 */
	public int getMaxAnalysisTimePerSentence();
	public void setMaxAnalysisTimePerSentence(int maxAnalysisTimePerSentence);
}