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

import java.io.File;
import java.io.Reader;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import com.joliciel.talismane.machineLearning.CorpusEventStream;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.MachineLearningModel;
import com.joliciel.talismane.posTagger.features.PosTaggerFeature;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;

public interface PosTaggerService {
	/**
	 * 
	 * @param decisionMaker
	 * @param posTaggerFeatures
	 * @param beamWidth the maximum beamwidth to consider during the beam search
	 * @return
	 */
	public PosTagger getPosTagger(
			Set<PosTaggerFeature<?>> posTaggerFeatures,
			DecisionMaker<PosTag> decisionMaker,
			int beamWidth);
	
	/**
	 * Get a pos-tagger defined by a particular machine learning model.
	 * @param beamWidth the maximum beamwidth to consider during the beam search
	 * @return
	 */
	public PosTagger getPosTagger(
			MachineLearningModel<PosTag> model,
			int beamWidth);

	public PosTaggerEvaluator getPosTaggerEvaluator(PosTagger posTagger);
	public PosTagComparator getPosTagComparator();
	
	public PosTagSequence getPosTagSequence(
			PosTagSequence history);

	public PosTagSequence getPosTagSequence(
			TokenSequence tokenSequence,
			int initialCapacity);
	
	PosTaggedToken getPosTaggedToken(Token token, Decision<PosTag> decision);

	PosTag getPosTag(String code, String description,
			PosTagOpenClassIndicator openClassIndicator);
	
	public CorpusEventStream getPosTagEventStream(PosTagAnnotatedCorpusReader corpusReader,
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
}
