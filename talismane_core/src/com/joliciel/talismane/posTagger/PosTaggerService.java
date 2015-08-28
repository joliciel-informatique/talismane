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
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import com.joliciel.talismane.machineLearning.ClassificationEventStream;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.posTagger.features.PosTaggerFeature;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;

/**
 * Interface for retrieving implementations of various pos-tagger interfaces.
 * @author Assaf Urieli
 *
 */
public interface PosTaggerService {
	/**
	 * Get a pos-tagger.
	 * @param decisionMaker the decision maker used to make pos-tagging decisions
	 * @param posTaggerFeatures the features on which the decision maker's decisions are made.
	 * @param beamWidth the maximum beamwidth to consider during the beam search
	 * @return the pos-tagger
	 */
	public PosTagger getPosTagger(
			Set<PosTaggerFeature<?>> posTaggerFeatures,
			DecisionMaker decisionMaker,
			int beamWidth);
	
	/**
	 * Get a pos-tagger defined by a particular machine learning model.
	 * @param beamWidth the maximum beamwidth to consider during the beam search
	 * @return the pos-tagger
	 */
	public PosTagger getPosTagger(
			ClassificationModel model,
			int beamWidth);

	/**
	 * Get a pos-tagger evaluator.
	 * @param posTagger the pos-tagger to evaluate.
	 */
	public PosTaggerEvaluator getPosTaggerEvaluator(PosTagger posTagger);
	
	/**
	 * Get a pos-tagger comparator.
	 */
	public PosTagComparator getPosTagComparator();
	
	/**
	 * Construct an empty pos-tag sequence, based on a given {@link TokenSequence} that needs to be pos-tagged.
	 * @param tokenSequence the token sequence to be pos-tagged.
	 */
	public PosTagSequence getPosTagSequence(
			TokenSequence tokenSequence);
	
	/**
	 * Construct a pos-tagged token for a given token and given decision - the {@link Decision#getOutcome()} must
	 * be a valid {@link PosTag#getCode()} from the current {@link PosTagSet}.
	 * @param token the token to be tagged
	 * @param decision the decision used to tag it
	 */
	PosTaggedToken getPosTaggedToken(Token token, Decision decision);

	/**
	 * Construct a pos-tag for a given code, desciption and open-class indicator.
	 * @param code the pos-tag's code
	 * @param description the pos-tag's description
	 * @param openClassIndicator the pos-tag's open class indicator.
	 */
	PosTag getPosTag(String code, String description,
			PosTagOpenClassIndicator openClassIndicator);
	
	/**
	 * Create a classification event stream for a given corpus reader and set of features.
	 * @param corpusReader the corpus reader used to read the training corpus
	 * @param posTaggerFeatures the features used to describe events in the training corpus
	 */
	public ClassificationEventStream getPosTagEventStream(PosTagAnnotatedCorpusReader corpusReader,
			Set<PosTaggerFeature<?>> posTaggerFeatures);

	/**
	 * Loads a PosTagSet from a file or list of strings.
	 * The file has the following format: <BR/>
	 * First row: PosTagSet name<BR/>
	 * Second row: PosTagSet ISO 2 letter language code<BR/>
	 * Remaining rows:<BR/>
	 * PosTagCode tab description tab OPEN/CLOSED<BR/>
	 * e.g.<BR/>
	 * ADJ	adjectif	OPEN<BR/>
	 *
	 */
	PosTagSet getPosTagSet(File file);
	
	/**
	 * Same as getPosTageSet(file), but replaces file with a scanner.
	 * @param scanner
	 * @return
	 */
	PosTagSet getPosTagSet(Scanner scanner);

	/**
	 * Same as getPosTagSet(File), but replaces the file with a List of Strings.
	 * @param descriptors
	 * @return
	 */
	PosTagSet getPosTagSet(List<String> descriptors);
	
	/**
	 * Returns a corpus reader based on the use of Regex.
	 * See class description for details.
	 * @param reader
	 * @return
	 */
	PosTagRegexBasedCorpusReader getRegexBasedCorpusReader(Reader reader);
	
	/**
	 * A feature tester, which outputs results of applying features to the items encountered
	 * in a given corpus.
	 * @param posTaggerFeatures the features to test
	 * @param testWords limit the test to certain words only
	 * @param file the file where the test results should be written
	 * @return
	 */
	PosTagSequenceProcessor getPosTagFeatureTester(Set<PosTaggerFeature<?>> posTaggerFeatures,
			Set<String> testWords, File file);
}
