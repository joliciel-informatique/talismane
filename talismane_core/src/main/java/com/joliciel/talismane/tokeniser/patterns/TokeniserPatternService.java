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
package com.joliciel.talismane.tokeniser.patterns;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.joliciel.talismane.machineLearning.ClassificationEventStream;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.tokeniser.Tokeniser;
import com.joliciel.talismane.tokeniser.TokeniserAnnotatedCorpusReader;
import com.joliciel.talismane.tokeniser.features.TokenPatternMatchFeature;
import com.joliciel.talismane.tokeniser.features.TokeniserContextFeature;

public interface TokeniserPatternService {
	public enum PatternTokeniserType {
		Interval,
		Compound
	}
	
	public static final String PATTERN_DESCRIPTOR_KEY = "pattern";
	
	public TokeniserPatternManager getPatternManager(List<String> patternDescriptors);

	public TokenPattern getTokeniserPattern(String regexp, Pattern separatorPattern);

	/**
	 * The interval pattern tokeniser first splits the text into individual tokens based on a list of separators,
	 * each of which is assigned a default value for that separator.
	 * 
	 * The tokeniser then takes a list of patterns, and for each pattern in the list, tries to match it to a sequence of tokens within the sentence.
	 * If a match is found, the final decision for each token interval in this sequence is deferred to a TokeniserDecisionMaker.
	 * If not, the default values are retained.
	 * 
	 * Overlapping sequences are handled gracefully: if a given interval is 2nd in sequence A, but 1st in sequence B, it will receive the
	 * n-gram feature from sequence A and a bunch of contextual features from sequence B, and the final decision will be taken based on the
	 * combination of all features. However, this can result in a strange compound that doesn't exist in any pattern nor in the training corpus.
	 * 
	 * The motivation for this pattern tokeniser is to concentrate training and decisions on difficult cases, rather than blurring the
	 * training model with oodles of obvious cases.
	 */
	public Tokeniser getIntervalPatternTokeniser(TokeniserPatternManager patternManager,
			Set<TokeniserContextFeature<?>> features,
			DecisionMaker decisionMaker, int beamWidth);
	
	/**
	 * The compound pattern tokeniser first splits the text into individual tokens based on a list of separators,
	 * each of which is assigned a default value for that separator.
	 * 
	 * The tokeniser then takes a list of patterns, and for each pattern in the list, tries to match it to a sequence of tokens within the sentence.
	 * If a match is found, a join/separate decision is taken for the sequence as a whole. If not, the default values are retained.
	 * However, to allow for rare overlapping sequences, if the join/separate decision would result in default decisions for the entire sequence, we only mark the first interval
	 * in the sequence, and allow another pattern to match the remaining tokens.
	 * Otherwise, we skip all tokens in this sequence before trying to match.
	 * 
	 * The motivation for this pattern tokeniser is to concentrate training and decisions on difficult cases, rather than blurring the
	 * training model with oodles of obvious cases.
	 * Furthermore, we have virtually eliminated strange broken compounds, which was possible lower-down in the beam using the interval approach,
	 * because the n-gram features used in that approach generally contained no counter-examples, leading to the "missing category" phenomenon with a
	 * relatively high score for the missing category.
	 */
	public Tokeniser getCompoundPatternTokeniser(TokeniserPatternManager patternManager,
			Set<TokenPatternMatchFeature<?>> features,
			DecisionMaker decisionMaker, int beamWidth);
	
	/**
	 * Get a pattern tokeniser out of a machine-learning model,
	 * using all of the saved model attributes to construct it.
	 */
	public Tokeniser getPatternTokeniser(ClassificationModel model, int beamWidth);

	/**
	 * An event stream for tokenising, using patterns to identify intervals that need to be examined.
	 * An interval is simply the space between two tokens.
	 * This reduces the tokeniser decision to binary decision: separate or join.
	 * Unlike the Compound event stream, we create one event per token interval inside a pattern match.
	 * By convention, a feature being tested on a token is assumed to test the interval between the token and the one preceding it.
	 *
	 */
	public ClassificationEventStream getIntervalPatternEventStream(TokeniserAnnotatedCorpusReader corpusReader,
			Set<TokeniserContextFeature<?>> features, TokeniserPatternManager patternManager);

	/**
	 * An event stream for tokenising, using patterns to identify potential compounds that need to be examined.
	 * This reduces the tokeniser decision to binary decision: separate or join.
	 * Unlike the Interval stream, we generate one event per pattern match.
	 * The advantage is that inconsistent compounds become virtually impossible, even lower down on the beam.
	 */
	public ClassificationEventStream getCompoundPatternEventStream(TokeniserAnnotatedCorpusReader corpusReader,
			Set<TokenPatternMatchFeature<?>> features, TokeniserPatternManager patternManager);

}
