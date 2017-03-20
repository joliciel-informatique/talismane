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

import java.io.IOException;
import java.util.List;
import java.util.Set;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.machineLearning.ClassificationObserver;
import com.joliciel.talismane.parser.features.ParseConfigurationFeature;
import com.joliciel.talismane.parser.features.ParserRule;
import com.joliciel.talismane.posTagger.PosTagSequence;

/**
 * A syntax parser which takes a pos tag sequence and provides a parse
 * configuration.
 * 
 * @author Assaf Urieli
 *
 */
public interface Parser {

	public enum ParseComparisonStrategyType {
		/**
		 * Comparison based on number of transitions applied.
		 */
		transitionCount,
		/**
		 * Comparison based on number of elements remaining on the buffer.
		 */
		bufferSize,
		/**
		 * Comparison based on number of elements remaining on both the stack
		 * and the buffer.
		 */
		stackAndBufferSize,
		/**
		 * Comparison based on number of dependencies created.
		 */
		dependencyCount
	}

	public enum PredictTransitions {
		/**
		 * Always predict transitions.
		 */
		yes,
		/**
		 * Never predict transitions.
		 */
		no,
		/**
		 * Predict transitions if training, no otherwise.
		 */
		depends
	}

	/**
	 * Analyse a pos-tag sequence, and return the most likely parse
	 * configuration for the sentence.
	 * 
	 * @param posTagSequence
	 *            the likely pos-tag sequence for this sentence.
	 * @return the most likely parse configuration for this sentence
	 * @throws CircularDependencyException
	 * @throws InvalidTransitionException
	 * @throws UnknownTransitionException
	 * @throws UnknownDependencyLabelException
	 * @throws TalismaneException
	 * @throws IOException
	 */
	public abstract ParseConfiguration parseSentence(PosTagSequence posTagSequence) throws UnknownDependencyLabelException, UnknownTransitionException,
			InvalidTransitionException, CircularDependencyException, TalismaneException, IOException;

	public void addObserver(ClassificationObserver observer);

	/**
	 * The transition system used by this parser to make parse decisions.
	 */
	public TransitionSystem getTransitionSystem();

	/**
	 * The maximum time alloted per sentence for parse tree analysis, in
	 * seconds. Will be ignored if set to 0. If analysis jumps out because of
	 * time-out, there will be a parse-forest instead of a parse-tree, with
	 * several nodes left unattached.
	 */
	public int getMaxAnalysisTimePerSentence();

	/**
	 * The minimum amount of remaining free memory to continue a parse, in
	 * kilobytes. Will be ignored is set to 0. If analysis jumps out because of
	 * free memory descends below this limit, there will be a parse-forest
	 * instead of a parse-tree, with several nodes left unattached.
	 */
	public int getMinFreeMemory();

	/**
	 * Rules to apply while parsing (in place of the probablistic classifier).
	 */
	public List<ParserRule> getParserRules();

	public void setParserRules(List<ParserRule> parserRules);

	public ParseComparisonStrategy getParseComparisonStrategy();

	public void setParseComparisonStrategy(ParseComparisonStrategy parseComparisonStrategy);

	public Set<ParseConfigurationFeature<?>> getParseFeatures();

	public Parser cloneParser();
}
