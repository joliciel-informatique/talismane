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

import java.io.File;
import java.io.Writer;
import java.util.Set;

import com.joliciel.talismane.machineLearning.ClassificationEventStream;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.MachineLearningModel;
import com.joliciel.talismane.parser.Parser.ParseComparisonStrategyType;
import com.joliciel.talismane.parser.features.ParseConfigurationFeature;

public interface ParserService {

	/**
	 * The arc-standard Shift-Reduce transition system, as described in Nivre
	 * 2009 Dependency Parsing, Chapter 3.
	 */
	public TransitionSystem getShiftReduceTransitionSystem();

	/**
	 * An arc-eager Shift-Reduce transition system, as described in Nivre 2009
	 * Dependency Parsing, Chapter 3, in which right-depedencies are attached as
	 * soon as possible, rather than waiting until all left-dependencies have
	 * been attached.
	 *
	 */
	public TransitionSystem getArcEagerTransitionSystem();

	public ClassificationEventStream getParseEventStream(ParserAnnotatedCorpusReader corpusReader, Set<ParseConfigurationFeature<?>> parseFeatures);

	/**
	 * Read a non-deterministic parser directly from a model.
	 */
	public NonDeterministicParser getTransitionBasedParser(MachineLearningModel machineLearningModel, int beamWidth, boolean dynamiseFeatures);

	/**
	 * A non-deterministic parser implementing transition based parsing, using a
	 * Shift-Reduce algorithm.<br/>
	 * See Nivre 2008 for details on the algorithm for the deterministic case.
	 */
	public NonDeterministicParser getTransitionBasedParser(DecisionMaker decisionMaker, TransitionSystem transitionSystem,
			Set<ParseConfigurationFeature<?>> parseFeatures, int beamWidth);

	/**
	 * Get the transition system corresponding to the model provided.
	 */
	public TransitionSystem getTransitionSystem(MachineLearningModel model);

	public ParserEvaluator getParserEvaluator();

	/**
	 * Compare two annotated corpora, one serving as reference and the other as
	 * an evaluation. Note: it is assumed that the corpora have an exact
	 * matching set of parse configurations!
	 */
	public ParseComparator getParseComparator();

	/**
	 * Writes the list of transitions that were actually applied, one at a time.
	 */
	public ParseConfigurationProcessor getTransitionLogWriter(Writer csvFileWriter);

	public ParseComparisonStrategy getParseComparisonStrategy(ParseComparisonStrategyType type);

	public ParseConfigurationProcessor getParseFeatureTester(Set<ParseConfigurationFeature<?>> parserFeatures, File file);
}
