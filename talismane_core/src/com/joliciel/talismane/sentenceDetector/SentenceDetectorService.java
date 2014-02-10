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
package com.joliciel.talismane.sentenceDetector;

import java.io.Reader;
import java.util.Set;

import com.joliciel.talismane.machineLearning.ClassificationEventStream;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.DecisionFactory;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.sentenceDetector.features.SentenceDetectorFeature;

public interface SentenceDetectorService {
	
	public SentenceDetector getSentenceDetector(DecisionMaker<SentenceDetectorOutcome> decisionMaker,
			Set<SentenceDetectorFeature<?>> features);
	public SentenceDetector getSentenceDetector(ClassificationModel<SentenceDetectorOutcome> model);
	
	public SentenceDetectorEvaluator getEvaluator(SentenceDetector sentenceDetector);
	
	public PossibleSentenceBoundary getPossibleSentenceBoundary(String text, int index);
	
	public ClassificationEventStream getSentenceDetectorEventStream(SentenceDetectorAnnotatedCorpusReader corpusReader,
			Set<SentenceDetectorFeature<?>> features);

	public DecisionFactory<SentenceDetectorOutcome> getDecisionFactory();
	
	/**
	 * A default reader which assumes one sentence per line.
	 * @param reader
	 * @return
	 */
	public SentenceDetectorAnnotatedCorpusReader getDefaultReader(Reader reader);
}
