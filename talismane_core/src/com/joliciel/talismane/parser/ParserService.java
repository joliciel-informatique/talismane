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

import java.io.File;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Set;

import com.joliciel.talismane.machineLearning.ClassificationEventStream;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.FeatureWeightVector;
import com.joliciel.talismane.machineLearning.MachineLearningModel;
import com.joliciel.talismane.machineLearning.Ranker;
import com.joliciel.talismane.machineLearning.RankingEventStream;
import com.joliciel.talismane.parser.Parser.ParseComparisonStrategyType;
import com.joliciel.talismane.parser.features.ParseConfigurationFeature;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTaggedToken;

public interface ParserService {
	/**
	 * Gets the initial configuration for a particular pos-tagged token sequence.
	 * @param posTagSequence
	 * @return
	 */
	public ParseConfiguration getInitialConfiguration(PosTagSequence posTagSequence);

	/**
	 * The arc-standard Shift-Reduce transition system, as described in Nivre 2009 Dependency Parsing, Chapter 3.
	 */
	public TransitionSystem getShiftReduceTransitionSystem();
	
	/**
	 * An arc-eager Shift-Reduce transition system, as described in Nivre 2009 Dependency Parsing, Chapter 3,
	 * in which right-depedencies are attached as soon as possible, rather than waiting until all left-dependencies
	 * have been attached.
	 *
	 */
	public TransitionSystem getArcEagerTransitionSystem();
	
	public DependencyArc getDependencyArc(PosTaggedToken head, PosTaggedToken dependent,
			String label);

	public ClassificationEventStream getParseEventStream(ParserAnnotatedCorpusReader corpusReader, Set<ParseConfigurationFeature<?>> parseFeatures);
	public RankingEventStream<PosTagSequence> getGlobalParseEventStream(ParserAnnotatedCorpusReader corpusReader, Set<ParseConfigurationFeature<?>> parseFeatures);
	
	/**
	 * Return a parsing ranker.
	 * @param parsingConstrainer used to constrain the parse solutions given the training corpus
	 * @param parseFeatures features to use
	 * @param beamWidth beam width to use when ranking
	 * @return
	 */
	public Ranker<PosTagSequence> getRanker(ParsingConstrainer parsingConstrainer, Set<ParseConfigurationFeature<?>> parseFeatures, int beamWidth);
	
	/**
	 * A non-deterministic parser implementing transition based parsing,
	 * using a Shift-Reduce algorithm, but applying global learning.</br>
	 * The features are thus used to rank parse configurations
	 * after all valid transitions have been applied, rather than being used
	 * to select the next transition for an existing configuration.
	 * @param featureWeightVector
	 * @param parsingConstrainer
	 * @param parseFeatures
	 * @param beamWidth
	 * @return
	 */
	public NonDeterministicParser getTransitionBasedGlobalLearningParser(FeatureWeightVector featureWeightVector, ParsingConstrainer parsingConstrainer, Set<ParseConfigurationFeature<?>> parseFeatures, int beamWidth);
	
	/**
	 * Read a non-deterministic parser directly from a model.
	 * @param jolicielMaxentModel
	 * @param beamWidth
	 * @param dynamiseFeatures
	 * @return
	 */
	public NonDeterministicParser getTransitionBasedParser(MachineLearningModel machineLearningModel, int beamWidth, boolean dynamiseFeatures);
	
	/**
	 * A non-deterministic parser implementing transition based parsing,
	 * using a Shift-Reduce algorithm.<br/>
	 * See Nivre 2008 for details on the algorithm for the deterministic case.
	 */
	public NonDeterministicParser getTransitionBasedParser(DecisionMaker<Transition> decisionMaker, TransitionSystem transitionSystem, Set<ParseConfigurationFeature<?>> parseFeatures, int beamWidth);

	public ParserEvaluator getParserEvaluator();
	
	/**
	 * Compare two annotated corpora, one serving as reference and the other as
	 * an evaluation.
	 * Note: it is assumed that the corpora have an exact matching set of parse configurations!
	 */
	public ParseComparator getParseComparator();

	public ParserRegexBasedCorpusReader getRegexBasedCorpusReader(Reader reader);
	public ParserRegexBasedCorpusReader getRegexBasedCorpusReader(File file, Charset charset);
	
	/**
	 * Get a brand new parsing constrainer.
	 * @return
	 */
	public ParsingConstrainer getParsingConstrainer();
	
	/**
	 * Get a parsing constrainer from a file where it was previously serialised.
	 * @param file
	 * @return
	 */
	public ParsingConstrainer getParsingConstrainer(File file);
	
	public ParseComparisonStrategy getParseComparisonStrategy(ParseComparisonStrategyType type);

	public ParseConfigurationProcessor getParseFeatureTester(
			Set<ParseConfigurationFeature<?>> parserFeatures, File file);
}
