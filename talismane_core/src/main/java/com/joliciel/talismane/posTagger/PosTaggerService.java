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

import java.io.File;
import java.io.Reader;
import java.util.Set;

import com.joliciel.talismane.machineLearning.ClassificationEventStream;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.posTagger.features.PosTaggerFeature;
import com.joliciel.talismane.tokeniser.TokenSequence;

/**
 * Interface for retrieving implementations of various pos-tagger interfaces.
 * 
 * @author Assaf Urieli
 *
 */
public interface PosTaggerService {
	/**
	 * Get a pos-tagger.
	 * 
	 * @param decisionMaker
	 *            the decision maker used to make pos-tagging decisions
	 * @param posTaggerFeatures
	 *            the features on which the decision maker's decisions are made.
	 * @param beamWidth
	 *            the maximum beamwidth to consider during the beam search
	 * @return the pos-tagger
	 */
	public PosTagger getPosTagger(Set<PosTaggerFeature<?>> posTaggerFeatures, DecisionMaker decisionMaker, int beamWidth);

	/**
	 * Get a pos-tagger defined by a particular machine learning model.
	 * 
	 * @param beamWidth
	 *            the maximum beamwidth to consider during the beam search
	 * @return the pos-tagger
	 */
	public PosTagger getPosTagger(ClassificationModel model, int beamWidth);

	/**
	 * Get a pos-tagger evaluator.
	 * 
	 * @param posTagger
	 *            the pos-tagger to evaluate.
	 */
	public PosTaggerEvaluator getPosTaggerEvaluator(PosTagger posTagger);

	/**
	 * Get a pos-tagger comparator.
	 */
	public PosTagComparator getPosTagComparator();

	/**
	 * Construct an empty pos-tag sequence, based on a given
	 * {@link TokenSequence} that needs to be pos-tagged.
	 * 
	 * @param tokenSequence
	 *            the token sequence to be pos-tagged.
	 */
	public PosTagSequence getPosTagSequence(TokenSequence tokenSequence);

	/**
	 * Create a classification event stream for a given corpus reader and set of
	 * features.
	 * 
	 * @param corpusReader
	 *            the corpus reader used to read the training corpus
	 * @param posTaggerFeatures
	 *            the features used to describe events in the training corpus
	 */
	public ClassificationEventStream getPosTagEventStream(PosTagAnnotatedCorpusReader corpusReader, Set<PosTaggerFeature<?>> posTaggerFeatures);

	/**
	 * Returns a corpus reader based on the use of Regex. See class description
	 * for details.
	 */
	PosTagRegexBasedCorpusReader getRegexBasedCorpusReader(Reader reader);

	/**
	 * A feature tester, which outputs results of applying features to the items
	 * encountered in a given corpus.
	 * 
	 * @param posTaggerFeatures
	 *            the features to test
	 * @param testWords
	 *            limit the test to certain words only
	 * @param file
	 *            the file where the test results should be written
	 */
	PosTagSequenceProcessor getPosTagFeatureTester(Set<PosTaggerFeature<?>> posTaggerFeatures, Set<String> testWords, File file);
}
